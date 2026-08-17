package com.example.expensetracker.domain

import com.example.expensetracker.data.TransactionType
import com.example.expensetracker.data.TransactionWithDetails
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class DetectedSubscription(
    val id: String,
    val merchantName: String,
    val categoryName: String,
    val averageAmount: Double, // Active recurring billing amount
    val lastPaidTimestamp: Long,
    val nextBillingTimestamp: Long,
    val cadenceDays: Int,
    val occurrences: Int
)

class SubscriptionDetector {

    private val knownSubscriptionKeywords = listOf(
        "netflix", "spotify", "prime", "amazon prime", "hotstar", "disney", "youtube", "apple", "google storage", "google one",
        "jio", "airtel", "vi", "vodafone", "bsnl", "act fibernet", "broadband", "wifi", "internet",
        "electricity", "bescom", "kseb", "tneb", "mseb", "uppcl", "water", "gas", "indane", "hpcl", "adani gas",
        "rent", "society", "maintenance", "gym", "cult.fit", "cult fit", "insurance", "lic", "hdfc life", "max life", "star health",
        "emi", "loan", "sip", "mutual fund", "groww", "zerodha", "ksfe", "chitty"
    )

    fun detectSubscriptions(transactions: List<TransactionWithDetails>): List<DetectedSubscription> {
        val expenseTxns = transactions.filter { it.transaction.transactionType == TransactionType.EXPENSE }
        val groupedByMerchant = expenseTxns.groupBy {
            it.transaction.merchantName.trim().lowercase(Locale.ROOT)
        }

        val subscriptions = mutableListOf<DetectedSubscription>()

        for ((normalizedMerchant, items) in groupedByMerchant) {
            if (items.isEmpty()) continue

            val sortedItems = items.sortedBy { it.transaction.timestamp }
            val isKnownProvider = knownSubscriptionKeywords.any { normalizedMerchant.contains(it) }

            // If it's a known subscription service with at least 1 txn, or any merchant with >= 2 occurrences
            if (sortedItems.size == 1 && isKnownProvider) {
                val single = sortedItems.first()
                val lastPaid = single.transaction.timestamp
                val nextBilling = lastPaid + TimeUnit.DAYS.toMillis(30)
                subscriptions.add(
                    DetectedSubscription(
                        id = normalizedMerchant,
                        merchantName = single.transaction.merchantName,
                        categoryName = single.category?.name ?: "Bills & Utilities",
                        averageAmount = single.transaction.amount,
                        lastPaidTimestamp = lastPaid,
                        nextBillingTimestamp = nextBilling,
                        cadenceDays = 30,
                        occurrences = 1
                    )
                )
                continue
            }

            if (sortedItems.size < 2) continue

            val intervals = mutableListOf<Long>()
            val consecutiveMatches = mutableListOf<Double>()

            for (i in 1 until sortedItems.size) {
                val prev = sortedItems[i - 1].transaction
                val curr = sortedItems[i].transaction
                val diffDays = TimeUnit.MILLISECONDS.toDays(curr.timestamp - prev.timestamp)

                // Match monthly interval (24 to 36 days) or quarterly interval (80 to 95 days)
                val isMonthly = diffDays in 24..36
                val isQuarterly = diffDays in 80..95
                val isWeekly = diffDays in 6..8

                val amountDiffPercent = if (prev.amount > 0) abs(curr.amount - prev.amount) / prev.amount else 1.0

                // Strict amount check (within 15% variation) OR known utility/subscription keyword
                if ((isMonthly || isQuarterly || isWeekly) && (amountDiffPercent <= 0.15 || isKnownProvider)) {
                    intervals.add(diffDays)
                    consecutiveMatches.add(curr.amount)
                }
            }

            if (intervals.isNotEmpty() || (isKnownProvider && sortedItems.size >= 2)) {
                val avgInterval = if (intervals.isNotEmpty()) intervals.average().toInt() else 30
                val lastItem = sortedItems.last()
                val lastPaid = lastItem.transaction.timestamp
                val nextBilling = lastPaid + TimeUnit.DAYS.toMillis(avgInterval.toLong())

                // Use the latest recurring bill amount (the active plan price) rather than a noisy multi-month average
                val latestBillAmount = lastItem.transaction.amount
                val displayMerchant = lastItem.transaction.merchantName
                val category = lastItem.category?.name ?: "Bills & Utilities"

                subscriptions.add(
                    DetectedSubscription(
                        id = normalizedMerchant,
                        merchantName = displayMerchant,
                        categoryName = category,
                        averageAmount = latestBillAmount,
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
