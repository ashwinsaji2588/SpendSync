package com.example.expensetracker.domain

import android.content.Context
import android.net.Uri
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.Category
import com.example.expensetracker.data.TransactionEntity
import com.example.expensetracker.data.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale

data class CsvImportResult(
    val totalRows: Int,
    val importedCount: Int,
    val duplicateCount: Int,
    val errorCount: Int,
    val message: String
)

class CsvStatementParser(
    private val context: Context,
    private val database: AppDatabase
) {

    private val transactionDao = database.transactionDao()
    private val categoryDao = database.categoryDao()
    private val categoryRuleDao = database.categoryRuleDao()
    private val accountDao = database.accountDao()

    private val smsParserEngine = SmsParserEngine()
    private val smartCategorizer = SmartCategorizer()

    private val dateFormats = listOf(
        SimpleDateFormat("dd/MM/yyyy", Locale.US),
        SimpleDateFormat("dd-MM-yyyy", Locale.US),
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("dd/MM/yy", Locale.US),
        SimpleDateFormat("dd-MM-yy", Locale.US),
        SimpleDateFormat("dd MMM yyyy", Locale.US),
        SimpleDateFormat("dd-MMM-yyyy", Locale.US),
        SimpleDateFormat("dd-MMM-yy", Locale.US),
        SimpleDateFormat("MM/dd/yyyy", Locale.US),
        SimpleDateFormat("yyyy/MM/dd", Locale.US),
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    )

    suspend fun importCsvFromUri(uri: Uri, targetAccountId: Long? = null): CsvImportResult = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return@withContext CsvImportResult(0, 0, 0, 0, "Could not open selected file")

        val reader = BufferedReader(InputStreamReader(inputStream))
        val lines = reader.readLines()
        reader.close()

        if (lines.isEmpty()) {
            return@withContext CsvImportResult(0, 0, 0, 0, "Selected file is empty")
        }

        // Determine delimiter (, or ; or \t)
        val sampleLine = lines.firstOrNull { it.isNotBlank() } ?: ""
        val delimiter = when {
            sampleLine.count { it == ',' } >= sampleLine.count { it == ';' } && sampleLine.count { it == ',' } >= sampleLine.count { it == '\t' } -> ","
            sampleLine.count { it == ';' } > sampleLine.count { it == ',' } -> ";"
            else -> "\t"
        }

        // Find header line
        var headerIndex = -1
        var dateCol = -1
        var descCol = -1
        var amountCol = -1
        var debitCol = -1
        var creditCol = -1
        var typeCol = -1

        for (i in 0 until minOf(lines.size, 10)) {
            val tokens = parseCsvLine(lines[i], delimiter).map { it.lowercase(Locale.ROOT).trim() }
            val dIndex = tokens.indexOfFirst { it.contains("date") || it.contains("txn date") || it.contains("value date") }
            val descIndex = tokens.indexOfFirst { it.contains("desc") || it.contains("narration") || it.contains("particular") || it.contains("merchant") || it.contains("remark") || it.contains("detail") }
            val aIndex = tokens.indexOfFirst { it == "amount" || it.contains("amount (inr)") || it.contains("txn amount") }
            val drIndex = tokens.indexOfFirst { it.contains("debit") || it.contains("withdrawal") || it.contains("dr") }
            val crIndex = tokens.indexOfFirst { it.contains("credit") || it.contains("deposit") || it.contains("cr") }
            val tIndex = tokens.indexOfFirst { it.contains("type") || it.contains("dr/cr") }

            if (dIndex != -1 && (descIndex != -1 || aIndex != -1 || drIndex != -1)) {
                headerIndex = i
                dateCol = dIndex
                descCol = if (descIndex != -1) descIndex else 1
                amountCol = aIndex
                debitCol = drIndex
                creditCol = crIndex
                typeCol = tIndex
                break
            }
        }

        if (headerIndex == -1) {
            // Default fallback assumption: Col 0 = Date, Col 1 = Description, Col 2 = Amount
            dateCol = 0
            descCol = 1
            amountCol = 2
            headerIndex = 0
        }

        val allCategories = categoryDao.getAllCategoriesList()
        val defaultAccountId = targetAccountId ?: accountDao.getDefaultAccountId() ?: 1L

        var importedCount = 0
        var duplicateCount = 0
        var errorCount = 0
        val transactionsToInsert = mutableListOf<TransactionEntity>()

        for (i in (headerIndex + 1) until lines.size) {
            val line = lines[i].trim()
            if (line.isBlank()) continue

            try {
                val columns = parseCsvLine(line, delimiter)
                if (columns.size <= dateCol) {
                    errorCount++
                    continue
                }

                val dateStr = columns.getOrNull(dateCol)?.trim() ?: ""
                val descStr = columns.getOrNull(descCol)?.trim()?.ifEmpty { "Bank Transaction" } ?: "Bank Transaction"
                
                // Parse Amount & Transaction Type
                var amount = 0.0
                var type = TransactionType.EXPENSE

                if (debitCol != -1 && columns.getOrNull(debitCol)?.isNotBlank() == true) {
                    val debitVal = parseAmount(columns[debitCol])
                    if (debitVal > 0.0) {
                        amount = debitVal
                        type = TransactionType.EXPENSE
                    }
                }
                
                if (amount == 0.0 && creditCol != -1 && columns.getOrNull(creditCol)?.isNotBlank() == true) {
                    val creditVal = parseAmount(columns[creditCol])
                    if (creditVal > 0.0) {
                        amount = creditVal
                        type = TransactionType.INCOME
                    }
                }

                if (amount == 0.0 && amountCol != -1 && columns.getOrNull(amountCol)?.isNotBlank() == true) {
                    val rawAmt = parseAmount(columns[amountCol])
                    amount = Math.abs(rawAmt)
                    if (rawAmt < 0.0) {
                        type = TransactionType.EXPENSE
                    } else if (typeCol != -1) {
                        val typeStr = columns.getOrNull(typeCol)?.lowercase(Locale.ROOT) ?: ""
                        type = if (typeStr.contains("cr") || typeStr.contains("credit") || typeStr.contains("dep")) {
                            TransactionType.INCOME
                        } else {
                            TransactionType.EXPENSE
                        }
                    }
                }

                if (amount <= 0.0) {
                    errorCount++
                    continue
                }

                val timestamp = parseDate(dateStr)

                // Clean description
                val merchantClean = descStr.trim().trimEnd { it == '.' || it == ',' || it == ' ' || it == ':' }

                // Categorize
                val customRule = categoryRuleDao.findMatchingRuleForMerchant(merchantClean)
                val categoryId: Long = if (customRule != null) {
                    customRule.targetCategoryId
                } else {
                    val keywordCategory = smsParserEngine.categorizeMerchantKeywords(merchantClean)
                    val finalCategoryName = keywordCategory ?: smartCategorizer.predictCategory(merchantClean)
                    val existing = allCategories.find { it.name.equals(finalCategoryName, ignoreCase = true) }
                    existing?.id ?: (categoryDao.getCategoryByName(finalCategoryName)?.id ?: 7L)
                }

                transactionsToInsert.add(
                    TransactionEntity(
                        amount = amount,
                        merchantName = merchantClean,
                        timestamp = timestamp,
                        transactionType = type,
                        accountId = defaultAccountId,
                        categoryId = categoryId,
                        isSplit = false,
                        reimbursementAmount = 0.0,
                        settled = false,
                        notes = "Imported from statement ($descStr)"
                    )
                )
            } catch (e: Exception) {
                errorCount++
            }
        }

        // Insert into Room with duplicate filtering
        for (txn in transactionsToInsert) {
            val insertedId = transactionDao.insertTransaction(txn)
            if (insertedId > 0) {
                importedCount++
            } else {
                duplicateCount++
            }
        }

        val totalProcessed = lines.size - (headerIndex + 1)
        val msg = "Imported $importedCount transaction${if (importedCount != 1) "s" else ""}." +
                (if (duplicateCount > 0) " Skipped $duplicateCount duplicate(s)." else "") +
                (if (errorCount > 0) " Ignored $errorCount unreadable row(s)." else "")

        CsvImportResult(
            totalRows = totalProcessed,
            importedCount = importedCount,
            duplicateCount = duplicateCount,
            errorCount = errorCount,
            message = msg
        )
    }

    private fun parseDate(dateStr: String): Long {
        val clean = dateStr.replace("\"", "").trim()
        for (format in dateFormats) {
            try {
                val parsed = format.parse(clean)
                if (parsed != null) {
                    return parsed.time
                }
            } catch (e: Exception) {
                // Try next format
            }
        }
        return System.currentTimeMillis()
    }

    private fun parseAmount(amountStr: String): Double {
        val clean = amountStr.replace("\"", "")
            .replace(",", "")
            .replace("₹", "")
            .replace("INR", "")
            .replace("$", "")
            .trim()
        return clean.toDoubleOrNull() ?: 0.0
    }

    private fun parseCsvLine(line: String, delimiter: String): List<String> {
        val tokens = mutableListOf<String>()
        var inQuotes = false
        val sb = StringBuilder()

        for (char in line) {
            when {
                char == '\"' -> inQuotes = !inQuotes
                char.toString() == delimiter && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                }
                else -> sb.append(char)
            }
        }
        tokens.add(sb.toString().trim())
        return tokens
    }
}
