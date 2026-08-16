package com.example.expensetracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.domain.SmsParserEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    private val parserEngine = SmsParserEngine()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return

            for (smsMessage in messages) {
                val sender = smsMessage.displayOriginatingAddress ?: smsMessage.originatingAddress ?: ""
                val body = smsMessage.displayMessageBody ?: smsMessage.messageBody ?: ""

                if (parserEngine.isBankSender(sender)) {
                    val pendingResult = goAsync()
                    val timestamp = smsMessage.timestampMillis.takeIf { it > 0 } ?: System.currentTimeMillis()

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = AppDatabase.getDatabase(context)
                            val transaction = parserEngine.parseAndBuildTransaction(
                                smsBody = body,
                                senderId = sender,
                                timestamp = timestamp,
                                categoryRuleDao = db.categoryRuleDao(),
                                categoryDao = db.categoryDao(),
                                accountDao = db.accountDao()
                            )
                            if (transaction != null) {
                                val dao = db.transactionDao()
                                dao.insertTransaction(transaction)
                                Log.d("SmsReceiver", "Transaction saved successfully: $transaction")
                            }
                        } catch (e: Exception) {
                            Log.e("SmsReceiver", "Error saving SMS transaction", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }
}
