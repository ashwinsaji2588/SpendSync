package com.example.expensetracker.service

import android.app.Notification
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.expensetracker.data.AccountType
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.Category
import com.example.expensetracker.data.TransactionEntity
import com.example.expensetracker.data.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class GPayNotificationListener : NotificationListenerService() {

    private val amountRegex = Regex("""(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
    private val splitPeerRegex = Regex("""(?:from|with|to|by|request\s+from)\s+([A-Za-z\s]+?)(?=\s*(?:for|on|via|ref|\.|\,|$))""", RegexOption.IGNORE_CASE)

    private val supportedPackages = setOf(
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "com.phonepe.app",                       // PhonePe
        "net.one97.paytm",                        // Paytm
        "in.org.npci.upiapp",                     // BHIM
        "com.cred.club"                           // CRED
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val packageName = sbn.packageName ?: return

        if (!supportedPackages.contains(packageName)) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val combinedContent = "$title $text $bigText"
        val lowerContent = combinedContent.lowercase(Locale.ROOT)

        // Check if this is a Split / Reimbursement / Request notification
        val isSplitOrRequest = lowerContent.contains("split") ||
                lowerContent.contains("requested") ||
                lowerContent.contains("bill request") ||
                lowerContent.contains("share")

        if (isSplitOrRequest) {
            val amountMatch = amountRegex.find(combinedContent)
            val amountStr = amountMatch?.groupValues?.get(1)?.replace(",", "")
            val amount = amountStr?.toDoubleOrNull()

            if (amount != null && amount > 0.0) {
                // Extract peer name
                val peerMatch = splitPeerRegex.find(combinedContent)
                val peerName = peerMatch?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() } ?: "Friend"

                val timestamp = sbn.postTime.takeIf { it > 0 } ?: System.currentTimeMillis()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(applicationContext)
                        AppDatabase.seedInitialData(db)

                        val categoryDao = db.categoryDao()
                        val accountDao = db.accountDao()
                        val transactionDao = db.transactionDao()

                        val categoryId = categoryDao.getCategoryByName("General")?.id
                            ?: categoryDao.insertCategory(Category(name = "General"))

                        val accountId = accountDao.getAccountByName("Primary Bank Account")?.id
                            ?: (accountDao.getAllAccounts() as? List<*>)?.firstOrNull()?.let { 1L } ?: 1L

                        val splitTransaction = TransactionEntity(
                            amount = amount,
                            merchantName = "Split: $peerName",
                            timestamp = timestamp,
                            transactionType = TransactionType.EXPENSE,
                            accountId = accountId,
                            categoryId = categoryId,
                            isSplit = true,
                            reimbursementAmount = amount,
                            settled = false,
                            peerName = peerName,
                            notes = "Auto-detected split from $title"
                        )

                        transactionDao.insertTransaction(splitTransaction)
                        Log.d(TAG, "Logged pending split from $packageName: $peerName owes ₹$amount")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing UPI split notification", e)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "GPayNotificationListener"

        fun isNotificationServiceEnabled(context: Context): Boolean {
            val pkgName = context.packageName
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            return flat != null && flat.contains(pkgName)
        }
    }
}
