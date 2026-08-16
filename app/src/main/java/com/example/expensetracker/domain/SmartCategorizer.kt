package com.example.expensetracker.domain

import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Interface for pluggable machine learning / on-device TFLite text classification engines.
 */
interface TextClassificationEngine {
    fun classify(merchantText: String): ClassificationResult?
}

data class ClassificationResult(
    val categoryName: String,
    val confidence: Float
)

/**
 * Smart heuristic string-similarity and cluster-based categorizer.
 * Evaluates token overlap, Levenshtein edit distance, and domain vendor clusters.
 */
class SmartCategorizer(
    private val mlEngine: TextClassificationEngine? = null
) {

    private val categoryClusters = mapOf(
        "Food" to listOf(
            "cafe", "bistro", "bakery", "kitchen", "diner", "pizza", "burger",
            "biryani", "dhaba", "barbeque", "canteen", "chai", "coffee", "roast",
            "grill", "sweets", "tiffin", "momos", "pastry", "shawarma"
        ),
        "Grocery" to listOf(
            "mart", "supermarket", "hypermarket", "provisions", "bazaar",
            "kirana", "dairy", "fruits", "vegetables", "organic", "spices",
            "fresh", "retail", "general store", "whole foods"
        ),
        "Shopping" to listOf(
            "apparel", "clothing", "fashion", "footwear", "couture", "jewels",
            "jewellers", "boutique", "trends", "wear", "electronics", "gadgets",
            "appliances", "opticals", "mall", "outlet", "tailor", "garments"
        ),
        "Entertainment" to listOf(
            "multiplex", "theatre", "gaming", "playstation", "amusement",
            "concert", "event", "club", "pub", "lounge", "bowling", "arcade",
            "tickets", "show", "movies", "live"
        ),
        "Travel" to listOf(
            "cabs", "taxis", "travels", "toll", "metro", "rail", "aviation",
            "airline", "airways", "petrol", "fuel", "diesel", "cng", "parking",
            "hotel", "resort", "inn", "stay", "lodge", "fastag"
        ),
        "Bills & Utilities" to listOf(
            "broadband", "fiber", "telecom", "recharge", "electric", "power",
            "water supply", "gas", "dth", "insurance", "premium", "hospital",
            "pharmacy", "medicos", "druggist", "clinic", "diagnostic", "lab"
        )
    )

    fun predictCategory(merchant: String): String {
        if (merchant.isBlank()) return "General"

        // 1. Check ML / TFLite engine if available
        mlEngine?.classify(merchant)?.let { result ->
            if (result.confidence >= 0.65f) {
                return result.categoryName
            }
        }

        val cleaned = merchant.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\s]"), " ")
        val tokens = cleaned.split("\\s+".toRegex()).filter { it.length > 2 }

        var bestCategory = "General"
        var highestScore = 0.0

        for ((category, keywords) in categoryClusters) {
            var score = 0.0
            for (token in tokens) {
                for (keyword in keywords) {
                    if (token == keyword) {
                        score += 1.0
                    } else if (token.contains(keyword) || keyword.contains(token)) {
                        score += 0.7
                    } else {
                        val similarity = calculateSimilarity(token, keyword)
                        if (similarity > 0.75) {
                            score += similarity * 0.6
                        }
                    }
                }
            }

            if (score > highestScore && score >= 0.6) {
                highestScore = score
                bestCategory = category
            }
        }

        return bestCategory
    }

    /**
     * Normalized Levenshtein similarity (0.0 to 1.0).
     */
    private fun calculateSimilarity(s1: String, s2: String): Double {
        val maxLen = max(s1.length, s2.length)
        if (maxLen == 0) return 1.0
        val dist = levenshteinDistance(s1, s2)
        return (maxLen - dist).toDouble() / maxLen
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}
