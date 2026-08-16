package com.example.expensetracker.domain

import com.example.expensetracker.data.TransactionType
import com.example.expensetracker.data.TransactionWithDetails
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DetectedSubscription(
    val id: String,
    val merchantName: String,
    val categoryName: String,
    val averageAmount: Double,
    val lastPaidTimestamp: Long,
    val nextBillingTimestamp: Long,
    val cadenceDays: Int,
    val occurrences: Int
)

class SubscriptionDetector {

    fun detectSubscriptions(transactions: List<TransactionWithDetails>): List<DetectedSubscription> {
        val expenseTxns = transactions.filter { it.transaction.transactionType == TransactionType.EXPENSE }
        val groupedByMerchant = expenseTxns.groupBy {
            it.transaction.merchantName.trim().lowercase(Locale.ROOT)
        }

        val subscriptions = mutableListOf<DetectedSubscription>()

        for ((normalizedMerchant, items) in groupedByMerchant) {
            if (items.size < 2) continue

            val sortedItems = items.sortedBy { it.transaction.timestamp }
            val intervals = mutableListOf<Long>()
            val amounts = mutableListOf<Double>()

            for (i in 1 until sortedItems.size) {
                val prev = sortedItems[i - 1].transaction
                val curr = sortedItems[i].transaction
                val diffDays = TimeUnit.MILLISECONDS.toDays(curr.timestamp - prev.timestamp)

                // Check if interval is ~1 month (20 to 38 days) or consistent interval
                if (diffDays in 20..38) {
                    intervals.add(diffDays)
                    amounts.add(curr.amount)
                }
            }

            if (intervals.isNotEmpty() || items.size >= 3) {
                val avgInterval = if (intervals.isNotEmpty()) intervals.average().toInt() else 30
                val avgAmount = sortedItems.map { it.transaction.amount }.average()
                val lastItem = sortedItems.last()
                val lastPaid = lastItem.transaction.timestamp
                val nextBilling = lastPaid + TimeUnit.DAYS.toMillis(avgInterval.toLong())

                val displayMerchant = lastItem.transaction.merchantName
                val category = lastItem.category?.name ?: "General"

                subscriptions.add(
                    DetectedSubscription(
                        id = normalizedMerchant,
                        merchantName = displayMerchant,
                        categoryName = category,
                        averageAmount = avgAmount,
                        lastPaidTimestamp = lastPaid,
                        nextBillingTimestamp = nextBilling,
                        cadenceDays = avgInterval,
                        occurrences = sortedItems.size
                    )
                )
            }
        }

        return subscriptions.sortedBy { it.nextBillingTimestamp }
    }
}
