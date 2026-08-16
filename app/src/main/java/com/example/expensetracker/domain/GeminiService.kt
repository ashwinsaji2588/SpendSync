package com.example.expensetracker.domain

import android.content.Context
import com.example.expensetracker.data.TransactionType
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class AiParsedTransaction(
    val merchantName: String,
    val amount: Double,
    val transactionType: TransactionType,
    val category: String
)

class GeminiService(private val context: Context) {

    private val prefs = context.getSharedPreferences("expense_tracker_prefs", Context.MODE_PRIVATE)

    fun getApiKey(): String {
        val saved = prefs.getString(PREF_GEMINI_API_KEY, "") ?: ""
        if (saved.isNotBlank()) return saved
        return com.example.expensetracker.BuildConfig.GEMINI_API_KEY
    }

    fun setApiKey(apiKey: String) {
        prefs.edit().putString(PREF_GEMINI_API_KEY, apiKey.trim()).apply()
    }

    private fun getModel(): GenerativeModel? {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) return null
        return GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey
        )
    }

    /**
     * AI-Powered Fallback SMS Parser when regex/keyword categorization needs deeper understanding.
     */
    suspend fun parseSmsWithAi(smsText: String): AiParsedTransaction? = withContext(Dispatchers.IO) {
        val model = getModel() ?: return@withContext null

        val prompt = """
            Analyze the following Indian Bank SMS and extract transaction details.
            Return ONLY a valid JSON object without markdown formatting or codeblocks:
            {
              "merchantName": "Clean Merchant or Recipient Name (e.g. Swiggy, Groww, KSFE, Uber, Landlord)",
              "amount": 1000.0,
              "transactionType": "EXPENSE or INCOME or TRANSFER",
              "category": "Food, Grocery, Shopping, Bills & Utilities, Travel, Entertainment, Health, Investment, Salary, Freelance, or a custom suitable Category"
            }

            SMS: "$smsText"
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            val text = response.text?.trim() ?: return@withContext null
            val cleanJson = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

            val json = JSONObject(cleanJson)
            val merchant = json.optString("merchantName", "Unknown Merchant")
            val amount = json.optDouble("amount", 0.0)
            val typeStr = json.optString("transactionType", "EXPENSE")
            val category = json.optString("category", "General")

            val type = when (typeStr.uppercase()) {
                "INCOME", "CREDIT" -> TransactionType.INCOME
                "TRANSFER" -> TransactionType.TRANSFER
                else -> TransactionType.EXPENSE
            }

            if (amount > 0) {
                AiParsedTransaction(
                    merchantName = merchant,
                    amount = amount,
                    transactionType = type,
                    category = category
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Financial Chat Assistant answering user questions using aggregated spending data.
     */
    suspend fun answerFinancialQuery(
        financialSummaryContext: String,
        userQuery: String
    ): String = withContext(Dispatchers.IO) {
        val model = getModel()
            ?: return@withContext "Please configure your free Gemini API Key in AI Insights Settings to enable smart financial assistance."

        val prompt = """
            You are SpendSync AI, a smart, insightful, and friendly personal finance assistant.
            The user is asking questions about their financial habits, budgets, and expenses.
            
            Financial Context for the current period:
            $financialSummaryContext

            User Question:
            "$userQuery"

            Provide a concise, helpful, and friendly response with clear actionable tips. Format amounts in Indian Rupees (₹).
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            response.text?.trim() ?: "I couldn't analyze that right now. Please try again."
        } catch (e: Exception) {
            "Error querying Gemini: ${e.localizedMessage ?: "Please check your network and API Key."}"
        }
    }

    companion object {
        const val PREF_GEMINI_API_KEY = "pref_gemini_api_key"
    }
}
