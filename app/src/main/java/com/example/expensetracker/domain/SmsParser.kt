package com.example.expensetracker.domain

import com.example.expensetracker.data.Account
import com.example.expensetracker.data.AccountDao
import com.example.expensetracker.data.AccountType
import com.example.expensetracker.data.Category
import com.example.expensetracker.data.CategoryDao
import com.example.expensetracker.data.CategoryRuleDao
import com.example.expensetracker.data.TransactionEntity
import com.example.expensetracker.data.TransactionType
import java.util.Locale

data class RawSmsTransaction(
    val amount: Double,
    val merchantName: String,
    val timestamp: Long,
    val transactionType: TransactionType,
    val accountIdentifier: String?,
    val detectedAccountType: AccountType = AccountType.BANK_ACCOUNT,
    val detectedBankName: String? = null
)

data class DetectedAccountInfo(
    val last4: String?,
    val accountType: AccountType,
    val bankName: String?
)

class SmsParserEngine(
    private val smartCategorizer: SmartCategorizer = SmartCategorizer()
) {

    // Regex to extract amount (looks for 'Rs.', 'INR', '₹', etc.)
    private val amountRegex = Regex("""(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)

    // Regex to extract merchant/recipient name (looks for 'at ', 'to ', 'Info:', 'VPA', 'towards', 'spent on')
    private val merchantRegex = Regex("""(?:at|to|Info:|towards|spent\s+on|vpa)\s+(?:Info:\s*)?([A-Za-z0-9\s&-]+?)(?=\s*(?:on|ref|via|val|Bal|avail|A/c|account|card|date|\.|\,|$))""", RegexOption.IGNORE_CASE)

    // Bank SMS Sender ID headers & substrings commonly used in India
    val bankHeaders = listOf(
        "HDFCBK", "SBINB", "ICICIB", "AXISBK", "KOTAKB",
        "PNBSMS", "BOISMS", "CANBNK", "YESBNK", "IDFCFB",
        "INDUSB", "UNIONB", "PAYTM", "CENTBK", "IOBBNK",
        "RBLBNK", "FEDBNK", "SCISMS", "CITIBK", "HSBCIN",
        "AUBNK", "BANDHN", "BARB0", "UBIN", "SBIINB",
        "SIBSTM", "CSBLTD", "DLBSMS", "KVBANK", "TMBANK"
    )

    fun isBankSender(sender: String): Boolean {
        if (sender.isBlank()) return false
        val upperSender = sender.uppercase(Locale.ROOT)
        if (bankHeaders.any { upperSender.contains(it) }) return true
        return upperSender.contains("BK") ||
               upperSender.contains("BNK") ||
               upperSender.contains("BANK") ||
               upperSender.contains("PAYTM") ||
               upperSender.contains("GPAY") ||
               upperSender.contains("CRED")
    }

    fun extractBankName(senderId: String, smsBody: String): String? {
        val upper = "${senderId.uppercase(Locale.ROOT)} ${smsBody.uppercase(Locale.ROOT)}"
        return when {
            upper.contains("HDFC") -> "HDFC Bank"
            upper.contains("AXIS") -> "Axis Bank"
            upper.contains("ICICI") -> "ICICI Bank"
            upper.contains("SBI") || upper.contains("SBIN") -> "SBI"
            upper.contains("KOTAK") -> "Kotak Bank"
            upper.contains("PNB") || upper.contains("PUNJAB") -> "PNB"
            upper.contains("BOI") || upper.contains("BANK OF INDIA") -> "Bank of India"
            upper.contains("CANARA") || upper.contains("CANBNK") -> "Canara Bank"
            upper.contains("YES") || upper.contains("YESBK") -> "Yes Bank"
            upper.contains("IDFC") -> "IDFC FIRST Bank"
            upper.contains("INDUS") || upper.contains("INDUSB") -> "IndusInd Bank"
            upper.contains("UNION") || upper.contains("UNIONB") -> "Union Bank"
            upper.contains("PAYTM") -> "Paytm Bank"
            upper.contains("RBL") -> "RBL Bank"
            upper.contains("FEDERAL") || upper.contains("FEDBNK") -> "Federal Bank"
            upper.contains("CITI") -> "Citi"
            upper.contains("HSBC") -> "HSBC"
            upper.contains("AU") && (upper.contains("AUBNK") || upper.contains("AU BANK") || upper.contains("AU SMALL")) -> "AU Small Finance Bank"
            upper.contains("BANDHAN") -> "Bandhan Bank"
            upper.contains("BARODA") || upper.contains("BOB") || upper.contains("BARB0") -> "Bank of Baroda"
            upper.contains("STANDARD CHARTERED") || upper.contains("SCISMS") || upper.contains("STANCHAR") -> "Standard Chartered"
            upper.contains("SOUTH INDIAN") || upper.contains("SIBSTM") -> "South Indian Bank"
            else -> null
        }
    }

    fun detectAccountTypeAndDetails(smsBody: String, senderId: String): DetectedAccountInfo {
        val lowerBody = smsBody.lowercase(Locale.ROOT)
        val bankName = extractBankName(senderId, smsBody)

        // Specific regex patterns for cards:
        // "Axis Bank Card no. XX3102", "HDFC Bank Card 7009", "Credit Card ending in 1234", "Card *1234"
        val cardRegex = Regex("""(?:credit\s*card|debit\s*card|card\s*no\.?|card\s*num\.?|card|cc)\s*(?:no\.?|num\.?)?\s*(?:ending\s*(?:in|with)?\s*)?(?:[xX]+|\*+)?([0-9]{4})""", RegexOption.IGNORE_CASE)

        // Specific regex patterns for bank accounts:
        // "HDFC Bank A/c XX2452", "A/C *1234", "Account ending in 4321", "acct 1234"
        val accountRegex = Regex("""(?:a/c|account|acct|savings\s*a/c|current\s*a/c)\s*(?:no\.?|num\.?)?\s*(?:ending\s*(?:in|with)?\s*)?(?:[xX]+|\*+)?([0-9]{4})""", RegexOption.IGNORE_CASE)

        // Generic fallback digit extractor
        val genericDigitRegex = Regex("""(?:[xX]{2,}|\*{2,})([0-9]{4})""")

        val isCreditCardKeywords = lowerBody.contains("credit card") ||
                lowerBody.contains("creditcard") ||
                lowerBody.contains("card no") ||
                lowerBody.contains("card ending") ||
                lowerBody.contains("card num") ||
                lowerBody.contains("cc ending") ||
                lowerBody.contains("cc no") ||
                lowerBody.contains(" cc ") ||
                lowerBody.contains(" cc:") ||
                Regex("""\b(?:axis|hdfc|icici|sbi|kotak|rbl|citi|sc|idfc)\s+bank\s+card\b""", RegexOption.IGNORE_CASE).containsMatchIn(smsBody) ||
                Regex("""\bcard\s+(?:no\.?\s*)?(?:xx+|\*+)?[0-9]{4}\b""", RegexOption.IGNORE_CASE).containsMatchIn(smsBody)

        val isDebitCardKeywords = lowerBody.contains("debit card") ||
                lowerBody.contains("debitcard") ||
                lowerBody.contains(" dc ") ||
                lowerBody.contains("dc ending")

        val isExplicitBankAccountKeywords = lowerBody.contains("a/c") ||
                lowerBody.contains("account") ||
                lowerBody.contains("acct")

        // Determine account type
        val detectedType = when {
            isDebitCardKeywords -> AccountType.DEBIT_CARD
            isCreditCardKeywords -> AccountType.CREDIT_CARD
            isExplicitBankAccountKeywords -> AccountType.BANK_ACCOUNT
            else -> AccountType.BANK_ACCOUNT
        }

        // Determine last 4 digits
        val cardMatch = cardRegex.find(smsBody)
        val acctMatch = accountRegex.find(smsBody)
        val genericMatch = genericDigitRegex.find(smsBody)

        val last4 = when {
            detectedType == AccountType.CREDIT_CARD || detectedType == AccountType.DEBIT_CARD ->
                cardMatch?.groupValues?.get(1) ?: acctMatch?.groupValues?.get(1) ?: genericMatch?.groupValues?.get(1)
            else ->
                acctMatch?.groupValues?.get(1) ?: cardMatch?.groupValues?.get(1) ?: genericMatch?.groupValues?.get(1)
        }

        return DetectedAccountInfo(
            last4 = last4,
            accountType = detectedType,
            bankName = bankName
        )
    }

    fun buildAccountName(bankName: String?, type: AccountType, last4: String): String {
        val bName = bankName ?: ""
        val typeLabel = when (type) {
            AccountType.CREDIT_CARD -> "Credit Card"
            AccountType.DEBIT_CARD -> "Debit Card"
            AccountType.BANK_ACCOUNT -> "A/c"
            AccountType.WALLET -> "Wallet"
            AccountType.CASH -> "Cash"
        }
        return if (bName.isNotBlank()) {
            "$bName $typeLabel *$last4"
        } else {
            "$typeLabel *$last4"
        }
    }

    /**
     * Parses raw SMS body into structured transaction components.
     */
    fun parseRaw(smsBody: String, senderId: String, timestamp: Long = System.currentTimeMillis()): RawSmsTransaction? {
        val lowerBody = smsBody.lowercase(Locale.ROOT)

        val transactionType = when {
            lowerBody.contains("credit card payment") -> TransactionType.TRANSFER
            lowerBody.contains("debited") || lowerBody.contains("spent") || lowerBody.contains("paid") || lowerBody.contains("sent") -> TransactionType.EXPENSE
            lowerBody.contains("credited") || lowerBody.contains("received") -> TransactionType.INCOME
            else -> return null
        }

        // Extract amount
        val amountMatch = amountRegex.find(smsBody) ?: return null
        val amountString = amountMatch.groupValues[1].replace(",", "")
        val amount = amountString.toDoubleOrNull() ?: return null

        // Extract merchant name
        val merchantMatch = merchantRegex.find(smsBody)
        val rawMerchant = merchantMatch?.groupValues?.get(1)?.trim()?.trimEnd { it == '.' || it == ',' || it == ' ' || it == ':' }
        val merchantName = if (!rawMerchant.isNullOrBlank()) {
            rawMerchant
        } else {
            if (senderId.isNotBlank()) senderId else "Unknown Merchant"
        }

        // Extract account details & accurate type
        val accountInfo = detectAccountTypeAndDetails(smsBody, senderId)

        return RawSmsTransaction(
            amount = amount,
            merchantName = merchantName,
            timestamp = timestamp,
            transactionType = transactionType,
            accountIdentifier = accountInfo.last4,
            detectedAccountType = accountInfo.accountType,
            detectedBankName = accountInfo.bankName
        )
    }

    /**
     * Asynchronously resolves or assigns Category and Account using Rules, Keyword Maps, and SmartCategorizer fallback.
     */
    suspend fun parseAndBuildTransaction(
        smsBody: String,
        senderId: String,
        timestamp: Long,
        categoryRuleDao: CategoryRuleDao,
        categoryDao: CategoryDao,
        accountDao: AccountDao
    ): TransactionEntity? {
        val raw = parseRaw(smsBody, senderId, timestamp) ?: return null

        // 1. Check user-defined CategoryRule first!
        val customRule = categoryRuleDao.findMatchingRuleForMerchant(raw.merchantName)
        val categoryId: Long = if (customRule != null) {
            customRule.targetCategoryId
        } else {
            // 2. Comprehensive Keyword-based categorization
            val keywordCategory = categorizeMerchantKeywords(raw.merchantName, smsBody)
            val finalCategoryName = if (keywordCategory != null) {
                keywordCategory
            } else {
                // 3. Smart Categorizer fallback (heuristic similarity & token clusters)
                smartCategorizer.predictCategory(raw.merchantName)
            }

            // Resolve Category ID from DB or insert
            val existingCategory = categoryDao.getCategoryByName(finalCategoryName)
            existingCategory?.id ?: categoryDao.insertCategory(Category(name = finalCategoryName))
        }

        // Resolve Account with accurate AccountType & upsert classification update
        val accountId: Long = if (raw.accountIdentifier != null) {
            val existingAcc = accountDao.getAccountByLast4(raw.accountIdentifier)
            if (existingAcc != null) {
                // If existing account was misclassified as BANK_ACCOUNT but is actually a card, update it
                if (existingAcc.type == AccountType.BANK_ACCOUNT &&
                    (raw.detectedAccountType == AccountType.CREDIT_CARD || raw.detectedAccountType == AccountType.DEBIT_CARD)
                ) {
                    val updatedName = buildAccountName(raw.detectedBankName, raw.detectedAccountType, raw.accountIdentifier)
                    accountDao.updateAccountTypeAndName(existingAcc.id, raw.detectedAccountType, updatedName)
                }
                existingAcc.id
            } else {
                val newName = buildAccountName(raw.detectedBankName, raw.detectedAccountType, raw.accountIdentifier)
                val newAcc = Account(
                    name = newName,
                    type = raw.detectedAccountType,
                    accountNumberLast4 = raw.accountIdentifier
                )
                accountDao.insertAccount(newAcc)
            }
        } else {
            val firstAcc = accountDao.getAllAccountsDirect().firstOrNull()
            firstAcc?.id ?: run {
                val newName = if (!raw.detectedBankName.isNullOrBlank()) "${raw.detectedBankName} Account" else "Main Account"
                accountDao.insertAccount(Account(name = newName, type = raw.detectedAccountType))
            }
        }

        return TransactionEntity(
            amount = raw.amount,
            merchantName = raw.merchantName,
            timestamp = raw.timestamp,
            transactionType = raw.transactionType,
            accountId = accountId,
            categoryId = categoryId,
            isSplit = false,
            reimbursementAmount = 0.0,
            settled = false
        )
    }

    /**
     * Comprehensive merchant-to-category dictionary matching online, local, and regional brands.
     */
    fun categorizeMerchantKeywords(merchant: String, fullSms: String = ""): String? {
        val text = "${merchant.lowercase(Locale.ROOT)} ${fullSms.lowercase(Locale.ROOT)}"

        return when {
            // 1. Food / Dining / Restaurants & Cafes
            text.containsAny(
                "swiggy", "zomato", "mcdonald", "kfc", "chicking", "grill bay",
                "mandi manzil", "belgian waffle", "starbucks", "domino", "pizza hut",
                "subway", "burger king", "haldiram", "chai point", "wow momo",
                "barbeque nation", "bawarchi", "paradise", "saravana bhavan",
                "sagar ratna", "thalappakatti", "thalassery", "anjappar", "a2b",
                "dosa", "bakery", "cafe", "restaurant", "kitchen", "mess", "diner",
                "shawarma", "biryani", "mandi", "bakes", "food", "juice", "shake",
                "tea", "coffee", "grill", "treat", "dining", "buffet", "pizza",
                "burger", "pasta", "tiffin", "canteen", "sweets", "cake", "ice cream"
            ) -> "Food"

            // 2. Groceries & Supermarkets
            text.containsAny(
                "instamart", "blinkit", "zepto", "bigbasket", "jiomart", "dmart",
                "nature's basket", "more retail", "spencers", "reliance fresh",
                "reliance smart", "nilgiris", "supplyco", "supermarket", "hypermarket",
                "kirana", "provisions", "mart", "grocery", "dairy", "fresh",
                "vegetable", "fruit", "organic", "meat", "fish", "spices", "store"
            ) -> "Grocery"

            // 3. Shopping & Fashion / Retail
            text.containsAny(
                "amazon", "flipkart", "myntra", "ajio", "zara", "h&m", "nykaa",
                "meesho", "tata cliq", "reliance trends", "max fashion", "westside",
                "pantaloons", "shoppers stop", "decathlon", "croma", "vijay sales",
                "reliance digital", "apple", "lenskart", "titan", "tanishq", "kalyan",
                "malabar gold", "joyalukkas", "jos alukkas", "bhima", "lulu mall",
                "mall", "retail", "fashion", "clothing", "apparel", "footwear",
                "jewels", "jewellers", "boutique", "trends", "wear", "electronics",
                "gadgets", "outlet", "tailor", "garments", "beauty", "cosmetics"
            ) -> "Shopping"

            // 4. Utilities, Recharge & Bills
            text.containsAny(
                "kseb", "electricity", "water authority", "kerala water", "jal board",
                "bescom", "msedcl", "tneb", "wbsedcl", "bses", "torrent power",
                "tata power", "airtel", "jio", "vi ", "vodafone", "idea", "bsnl",
                "act fibernet", "hathway", "asianet", "tata play", "dish tv",
                "sun direct", "dth", "recharge", "broadband", "fiber", "indane",
                "bharat gas", "hp gas", "igl", "insurance", "lic", "hdfc ergo",
                "policybazaar", "star health", "utility", "billdesk", "bbps"
            ) -> "Bills & Utilities"

            // 5. Travel, Transport & Fuel
            text.containsAny(
                "uber", "ola", "rapido", "namma yatri", "irctc", "railway", "redbus",
                "abhibus", "makemytrip", "goibibo", "cleartrip", "yatra", "indigo",
                "air india", "spicejet", "akasa", "vistara", "fastag", "toll",
                "metro", "kochi metro", "bangalore metro", "delhi metro", "petrol",
                "diesel", "fuel", "iocl", "indian oil", "bpcl", "bharat petroleum",
                "hpcl", "shell", "nayara", "auto", "cab", "taxi", "parking",
                "flight", "airline", "aviation", "transport", "travel"
            ) -> "Travel"

            // 6. Entertainment, Movies & Streaming
            text.containsAny(
                "bookmyshow", "pvr", "inox", "cinepolis", "aiswarya", "dhanya",
                "trinity", "remya", "kairali", "sree", "carnival", "netflix",
                "amazon prime", "disney", "hotstar", "sonyliv", "zee5", "spotify",
                "youtube", "apple music", "gaana", "wynk", "playstation", "steam",
                "gaming", "multiplex", "theatre", "cinema", "movies", "ticket",
                "concert", "club", "pub", "lounge", "show", "event"
            ) -> "Entertainment"

            // 7. Health & Medical
            text.containsAny(
                "apollo pharmacy", "medplus", "netmeds", "tata 1mg", "pharmeasy",
                "aster", "fortis", "max healthcare", "clinic", "hospital", "lab",
                "diagnostic", "doctor", "dental", "pharmacy", "medicos", "druggist",
                "health", "medicine"
            ) -> "Health"

            // 8. Investments & Wealth
            text.containsAny(
                "zerodha", "groww", "upstox", "angel one", "coin", "indmoney",
                "kuvera", "smallcase", "mutual fund", "sip", "ppf", "nps", "stock",
                "securities", "investment"
            ) -> "Investment"

            else -> null
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it, ignoreCase = true) }
    }
}
