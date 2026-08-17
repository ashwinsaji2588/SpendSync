package com.example.expensetracker.service

import android.app.Notification
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.expensetracker.data.Account
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
        // Check if this is an incoming UPI payment / credit / reimbursement
        val isIncomeReceived = lowerContent.contains("received") ||
                lowerContent.contains("paid you") ||
                lowerContent.contains("sent you") ||
                lowerContent.contains("credited")

        if (isIncomeReceived) {
            val amountMatch = amountRegex.find(combinedContent)
            val amountStr = amountMatch?.groupValues?.get(1)?.replace(",", "")
            val amount = amountStr?.toDoubleOrNull()

            if (amount != null && amount > 0.0) {
                // Extract peer/sender name
                val peerMatch = splitPeerRegex.find(combinedContent)
                val peerName = peerMatch?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() } ?: "UPI Contact"
                val timestamp = sbn.postTime.takeIf { it > 0 } ?: System.currentTimeMillis()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(applicationContext)
                        AppDatabase.seedInitialData(db)

                        val categoryDao = db.categoryDao()
                        val accountDao = db.accountDao()
                        val transactionDao = db.transactionDao()

                        val categoryId = categoryDao.getCategoryByName("Reimbursements")?.id
                            ?: categoryDao.insertCategory(Category(name = "Reimbursements", iconName = "reimbursements", colorHex = "#00897B"))

                        val accountId = accountDao.getAllAccountsDirect().firstOrNull()?.id
                            ?: accountDao.insertAccount(Account(name = "UPI Bank Account", type = AccountType.BANK_ACCOUNT))

                        val reimbursementTxn = TransactionEntity(
                            amount = amount,
                            merchantName = "From: $peerName",
                            timestamp = timestamp,
                            transactionType = TransactionType.INCOME,
                            accountId = accountId,
                            categoryId = categoryId,
                            notes = "Auto-detected UPI reimbursement from $title"
                        )

                        transactionDao.insertTransaction(reimbursementTxn)
                        Log.d(TAG, "Logged reimbursement from $packageName: Received ₹$amount from $peerName")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing UPI reimbursement notification", e)
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
