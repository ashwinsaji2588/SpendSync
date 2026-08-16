package com.example.expensetracker.domain

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.expensetracker.MainActivity
import com.example.expensetracker.R
import java.util.Locale

class BudgetNotificationHelper(private val context: Context) {

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SpendSync Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when spending approaches or exceeds category budget limits"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun checkAndNotifyBudget(
        categoryName: String,
        spentAmount: Double,
        budgetLimit: Double
    ) {
        if (budgetLimit <= 0.0) return

        val percentage = (spentAmount / budgetLimit) * 100
        val indianLocale = Locale.Builder().setLanguage("en").setRegion("IN").build()
        val spentFormatted = "₹${String.format(indianLocale, "%.0f", spentAmount)}"
        val limitFormatted = "₹${String.format(indianLocale, "%.0f", budgetLimit)}"

        val (title, message, notificationId) = when {
            percentage >= 100.0 -> {
                Triple(
                    "⚠️ Budget Exceeded: $categoryName",
                    "You have spent $spentFormatted, which exceeds your $limitFormatted budget (${String.format(indianLocale, "%.0f", percentage)}%)!",
                    (categoryName.hashCode() + 1000)
                )
            }
            percentage >= 80.0 -> {
                Triple(
                    "⚡ Budget Warning: $categoryName",
                    "You have reached ${String.format(indianLocale, "%.0f", percentage)}% of your $limitFormatted limit ($spentFormatted spent).",
                    (categoryName.hashCode() + 2000)
                )
            }
            else -> return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_spendsync_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Notifications permission not granted on Android 13+
        }
    }

    companion object {
        const val CHANNEL_ID = "spendsync_budget_alerts"
    }
}
