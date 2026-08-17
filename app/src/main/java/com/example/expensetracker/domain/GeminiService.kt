package com.example.expensetracker.domain

import android.content.Context
import com.example.expensetracker.data.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

data class AiParsedTransaction(
    val merchantName: String,
    val amount: Double,
    val transactionType: TransactionType,
    val category: String
)

class GeminiService(private val context: Context) {

    private val prefs = context.getSharedPreferences("expense_tracker_prefs", Context.MODE_PRIVATE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    fun getApiKey(): String {
        val saved = prefs.getString(PREF_GEMINI_API_KEY, "") ?: ""
        if (saved.isNotBlank()) return saved
        return com.example.expensetracker.BuildConfig.GEMINI_API_KEY
    }

    fun setApiKey(apiKey: String) {
        prefs.edit().putString(PREF_GEMINI_API_KEY, apiKey.trim()).apply()
    }

    /**
     * Direct HTTP REST call to Google Gemini endpoint.
     * POST https://generativelanguage.googleapis.com/v1beta/models/{modelName}:generateContent?key={apiKey}
     */
    private fun callGeminiRestApi(prompt: String, apiKey: String, modelName: String): String? {
        val cleanModel = modelName.removePrefix("models/")
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey"

        // Build standard JSON payload: contents -> parts -> text
        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray()
            val contentObj = JSONObject().apply {
                val partsArray = JSONArray()
                partsArray.put(JSONObject().apply {
                    put("text", prompt)
                })
                put("parts", partsArray)
            }
            contentsArray.put(contentObj)
            put("contents", contentsArray)
        }

        val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: return null
            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(responseBody).optJSONObject("error")
                    errJson?.optString("message") ?: "HTTP ${response.code}"
                } catch (e: Exception) {
                    "HTTP ${response.code}"
                }
                throw RuntimeException(errorMsg)
            }

            return parseGeneratedText(responseBody)
        }
    }

    /**
     * Extracts text from candidates[0].content.parts[0].text
     */
    private fun parseGeneratedText(responseBody: String): String? {
        val root = JSONObject(responseBody)
        val candidates = root.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null
        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null
        val firstPart = parts.getJSONObject(0)
        val text = firstPart.optString("text", "")
        return text.ifBlank { null }
    }

    /**
     * AI-Powered Fallback SMS Parser using direct REST API.
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

        val modelsToTry = listOf("gemini-1.5-flash", "gemini-2.0-flash", "gemini-1.5-pro", "gemini-2.5-flash")
        for (modelName in modelsToTry) {
            try {
                val rawText = callGeminiRestApi(prompt, apiKey, modelName) ?: continue
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
     * Financial Chat Assistant answering user questions using direct REST API.
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

        val modelsToTry = listOf("gemini-1.5-flash", "gemini-2.0-flash", "gemini-1.5-pro", "gemini-2.5-flash")
        var lastError: String? = null
        for (modelName in modelsToTry) {
            try {
                val text = callGeminiRestApi(prompt, apiKey, modelName)
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
