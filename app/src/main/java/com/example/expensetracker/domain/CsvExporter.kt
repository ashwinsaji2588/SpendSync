package com.example.expensetracker.domain

import android.content.Context
import android.net.Uri
import com.example.expensetracker.data.TransactionWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvExporter(private val context: Context) {

    suspend fun exportTransactionsToUri(
        uri: Uri,
        transactions: List<TransactionWithDetails>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val outputStream = context.contentResolver.openOutputStream(uri) ?: return@withContext false
            val writer = OutputStreamWriter(outputStream)

            // Header line
            writer.append("ID,Date,Time,Type,Merchant,Category,Account,Amount,Notes\n")

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

            for (item in transactions) {
                val t = item.transaction
                val date = dateFormat.format(Date(t.timestamp))
                val time = timeFormat.format(Date(t.timestamp))
                val type = t.transactionType.name
                val merchant = escapeCsv(t.merchantName)
                val category = escapeCsv(item.category?.name ?: "General")
                val account = escapeCsv(item.account?.nickname ?: item.account?.name ?: "Account")
                val amount = String.format(Locale.US, "%.2f", t.amount)
                val notes = escapeCsv(t.notes ?: "")

                writer.append("${t.id},$date,$time,$type,$merchant,$category,$account,$amount,$notes\n")
            }

            writer.flush()
            writer.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }
}
