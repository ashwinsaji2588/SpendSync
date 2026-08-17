package com.example.expensetracker.domain

import android.content.Context
import com.example.expensetracker.data.TransactionType
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

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

    private fun getModel(modelName: String = "gemini-2.5-flash"): GenerativeModel? {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) return null
        val cleanModelName = modelName.removePrefix("models/")
        return GenerativeModel(
            modelName = cleanModelName,
            apiKey = apiKey
        )
    }

    /**
     * AI-Powered Fallback SMS Parser when regex/keyword categorization needs deeper understanding.
     */
    suspend fun parseSmsWithAi(smsText: String): AiParsedTransaction? = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) return@withContext null

        val prompt = """
            Analyze the following Indian Bank SMS and extract transaction details.
            Return ONLY a valid JSON object without markdown formatting or codeblocks:
            {
              "merchantName": "Clean Merchant or Recipient Name (e.g. Swiggy, Groww, KSFE, Uber, Landlord)",
              "amount": 1000.0,
              "transactionType": "EXPENSE or INCOME or TRANSFER",
              "category": "Food, Grocery, Shopping, Bills & Utilities, Travel, Entertainment, Health, Investment, Salary, Freelance, Reimbursements, or a custom suitable Category"
            }

            SMS: "$smsText"
        """.trimIndent()

        val modelsToTry = listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-pro", "gemini-2.0-flash-lite", "gemini-1.5-flash")
        for (modelName in modelsToTry) {
            try {
                val cleanModelName = modelName.removePrefix("models/")
                val model = GenerativeModel(modelName = cleanModelName, apiKey = apiKey)
                val response = model.generateContent(prompt)
                val rawText = response.text?.trim() ?: continue
                val cleanJson = rawText
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val json = JSONObject(cleanJson)
                val merchant = json.optString("merchantName", "Unknown")
                val amount = json.optDouble("amount", 0.0)
                val typeStr = json.optString("transactionType", "EXPENSE")
                val category = json.optString("category", "General")

                val type = when (typeStr.uppercase(Locale.ROOT)) {
                    "INCOME" -> TransactionType.INCOME
                    "TRANSFER" -> TransactionType.TRANSFER
                    else -> TransactionType.EXPENSE
                }

                if (amount > 0.0) {
                    return@withContext AiParsedTransaction(
                        merchantName = merchant,
                        amount = amount,
                        transactionType = type,
                        category = category
                    )
                }
            } catch (e: Exception) {
                // Try next fallback model
            }
        }
        null
    }

    /**
     * Financial Chat Assistant answering user questions using aggregated spending data.
     */
    suspend fun answerFinancialQuery(financialContext: String, userQuery: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext "Google Gemini API Key is not configured. Please ensure GEMINI_API_KEY is provided in local.properties or app settings."
        }

        val prompt = """
            You are SpendSync's AI Financial Advisor for an Indian user.
            All monetary amounts are in Indian Rupees (₹).
            
            Current User Financial Context from Room Database:
            $financialContext
            
            User Question:
            $userQuery
            
            Provide a helpful, actionable, concise, and friendly answer. Keep bullet points crisp.
        """.trimIndent()

        val modelsToTry = listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-pro", "gemini-2.0-flash-lite", "gemini-1.5-flash")
        var lastError: String? = null
        for (modelName in modelsToTry) {
            try {
                val cleanModelName = modelName.removePrefix("models/")
                val model = GenerativeModel(modelName = cleanModelName, apiKey = apiKey)
                val response = model.generateContent(prompt)
                val text = response.text?.trim()
                if (!text.isNullOrBlank()) {
                    return@withContext text
                }
            } catch (e: Exception) {
                lastError = e.localizedMessage
            }
        }
        "I was unable to analyze your financial query right now: ${lastError ?: "Please verify network connection"}"
    }

    companion object {
        const val PREF_GEMINI_API_KEY = "pref_gemini_api_key"
    }
}
