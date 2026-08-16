package com.example.expensetracker.domain

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HistoricalSmsScanner(
    private val context: Context,
    private val db: AppDatabase = AppDatabase.getDatabase(context),
    private val parserEngine: SmsParserEngine = SmsParserEngine(),
    private val geminiService: GeminiService = GeminiService(context)
) {

    /**
     * Scans the device SMS inbox for bank transactions and backfills the Room database.
     * Requires Manifest.permission.READ_SMS permission.
     *
     * @return The number of parsed transactions inserted.
     */
    suspend fun scanInbox(): Int = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_SMS permission not granted. Cannot scan historical SMS.")
            return@withContext 0
        }

        // Ensure default seed accounts & categories exist
        AppDatabase.seedInitialData(db)

        val transactionDao = db.transactionDao()
        val categoryRuleDao = db.categoryRuleDao()
        val categoryDao = db.categoryDao()
        val accountDao = db.accountDao()

        val parsedTransactions = mutableListOf<TransactionEntity>()
        val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.Inbox.ADDRESS,
            Telephony.Sms.Inbox.BODY,
            Telephony.Sms.Inbox.DATE
        )

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Sms.Inbox.DATE} DESC"
            )

            if (cursor != null && cursor.moveToFirst()) {
                val addressIdx = cursor.getColumnIndex(Telephony.Sms.Inbox.ADDRESS)
                val bodyIdx = cursor.getColumnIndex(Telephony.Sms.Inbox.BODY)
                val dateIdx = cursor.getColumnIndex(Telephony.Sms.Inbox.DATE)

                do {
                    val address = if (addressIdx != -1) cursor.getString(addressIdx) ?: "" else ""
                    val body = if (bodyIdx != -1) cursor.getString(bodyIdx) ?: "" else ""
                    val date = if (dateIdx != -1) cursor.getLong(dateIdx) else System.currentTimeMillis()

                    if (parserEngine.isBankSender(address)) {
                        var transaction = parserEngine.parseAndBuildTransaction(
                            smsBody = body,
                            senderId = address,
                            timestamp = date,
                            categoryRuleDao = categoryRuleDao,
                            categoryDao = categoryDao,
                            accountDao = accountDao
                        )

                        // AI-Powered Fallback Parser for unclassified or obscure transactions
                        if (transaction == null || transaction.merchantName.equals("Unknown Merchant", ignoreCase = true)) {
                            val aiResult = geminiService.parseSmsWithAi(body)
                            if (aiResult != null && aiResult.amount > 0) {
                                val cat = categoryDao.getCategoryByName(aiResult.category)
                                val catId = cat?.id ?: categoryDao.insertCategory(
                                    com.example.expensetracker.data.Category(name = aiResult.category, colorHex = "#7E57C2")
                                )
                                val accId = accountDao.getAccountByName("Primary Bank Account")?.id
                                    ?: (accountDao.getAllAccountsDirect().firstOrNull()?.id ?: 1L)

                                transaction = TransactionEntity(
                                    amount = aiResult.amount,
                                    merchantName = aiResult.merchantName,
                                    timestamp = date,
                                    transactionType = aiResult.transactionType,
                                    accountId = accId,
                                    categoryId = if (catId > 0) catId else 7L,
                                    isSplit = false,
                                    reimbursementAmount = 0.0,
                                    settled = false
                                )
                            }
                        }

                        if (transaction != null) {
                            parsedTransactions.add(transaction)
                        }
                    }
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying device SMS inbox", e)
        } finally {
            cursor?.close()
        }

        if (parsedTransactions.isNotEmpty()) {
            val insertedRowIds = transactionDao.insertTransactions(parsedTransactions)
            val newlyInsertedCount = insertedRowIds.count { it != -1L }
            Log.d(TAG, "Historical scan completed. Found ${parsedTransactions.size} transactions, inserted $newlyInsertedCount new.")
            return@withContext newlyInsertedCount
        }

        0
    }

    companion object {
        private const val TAG = "HistoricalSmsScanner"
    }
}
