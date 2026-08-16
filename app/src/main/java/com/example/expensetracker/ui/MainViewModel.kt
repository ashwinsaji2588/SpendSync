package com.example.expensetracker.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.Account
import com.example.expensetracker.data.AccountType
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.Budget
import com.example.expensetracker.data.BudgetWithCategory
import com.example.expensetracker.data.Category
import com.example.expensetracker.data.CategoryRule
import com.example.expensetracker.data.TransactionEntity
import com.example.expensetracker.data.TransactionType
import com.example.expensetracker.data.TransactionWithDetails
import com.example.expensetracker.domain.BudgetNotificationHelper
import com.example.expensetracker.domain.CsvExporter
import com.example.expensetracker.domain.CsvImportResult
import com.example.expensetracker.domain.CsvStatementParser
import com.example.expensetracker.domain.DetectedSubscription
import com.example.expensetracker.domain.HistoricalSmsScanner
import com.example.expensetracker.domain.SubscriptionDetector
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class MonthOption(
    val year: Int,
    val month: Int,
    val label: String,
    val startTimestamp: Long,
    val endTimestamp: Long
)

data class CategoryBreakdownItem(
    val categoryId: Long,
    val categoryName: String,
    val colorHex: String?,
    val totalAmount: Double,
    val percentage: Float
)

data class MonthlySpendingSummary(
    val grossExpense: Double,
    val totalReimbursements: Double,
    val netExpense: Double
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val transactionDao = db.transactionDao()
    private val accountDao = db.accountDao()
    private val categoryDao = db.categoryDao()
    private val categoryRuleDao = db.categoryRuleDao()
    private val budgetDao = db.budgetDao()

    private val smsScanner = HistoricalSmsScanner(application, db)
    private val csvParser = CsvStatementParser(application, db)
    private val csvExporter = CsvExporter(application)
    private val budgetNotifier = BudgetNotificationHelper(application)
    private val subscriptionDetector = SubscriptionDetector()

    private val prefs = application.getSharedPreferences("expense_tracker_prefs", Context.MODE_PRIVATE)

    // Dark theme state: null = system default, true = dark mode, false = light mode
    private val _isDarkMode = MutableStateFlow<Boolean?>(
        if (prefs.contains(PREF_DARK_MODE)) prefs.getBoolean(PREF_DARK_MODE, false) else null
    )
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    // Biometric lock preference
    private val _isBiometricEnabled = MutableStateFlow(
        prefs.getBoolean(PREF_BIOMETRIC_ENABLED, false)
    )
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanMessage = MutableStateFlow<String?>(null)
    val scanMessage: StateFlow<String?> = _scanMessage.asStateFlow()

    private val _importMessage = MutableStateFlow<CsvImportResult?>(null)
    val importMessage: StateFlow<CsvImportResult?> = _importMessage.asStateFlow()

    // Universal Search & Custom Date Range Filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _customDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val customDateRange: StateFlow<Pair<Long, Long>?> = _customDateRange.asStateFlow()

    val availableMonths: List<MonthOption> = generateAvailableMonths()

    private val _selectedMonth = MutableStateFlow(availableMonths.first())
    val selectedMonth: StateFlow<MonthOption> = _selectedMonth.asStateFlow()

    private val _selectedAccount = MutableStateFlow<Account?>(null)
    val selectedAccount: StateFlow<Account?> = _selectedAccount.asStateFlow()

    val allAccounts: StateFlow<List<Account>> = accountDao.getAllAccounts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allCategories: StateFlow<List<Category>> = categoryDao.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allRules: StateFlow<List<CategoryRule>> = categoryRuleDao.getAllRulesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allBudgets: StateFlow<List<BudgetWithCategory>> = budgetDao.getAllBudgetsWithCategory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reactive Transactions Feed filtered by Month / Custom Date, Account, and Search Query
    val allTransactions: StateFlow<List<TransactionWithDetails>> = combine(
        _selectedMonth,
        _selectedAccount,
        _customDateRange,
        _searchQuery
    ) { month, account, dateRange, query ->
        DateFilterParams(month, account, dateRange, query)
    }
        .flatMapLatest { params ->
            val startTs = params.customDateRange?.first ?: params.month.startTimestamp
            val endTs = params.customDateRange?.second ?: params.month.endTimestamp

            val flow = if (params.account == null) {
                transactionDao.getTransactionsWithDetailsForDateRange(startTs, endTs)
            } else {
                transactionDao.getTransactionsWithDetailsForAccountAndDateRange(params.account.id, startTs, endTs)
            }

            flow.map { list ->
                if (params.query.isBlank()) {
                    list
                } else {
                    val q = params.query.trim().lowercase(Locale.ROOT)
                    list.filter { item ->
                        item.transaction.merchantName.lowercase(Locale.ROOT).contains(q) ||
                                (item.category?.name?.lowercase(Locale.ROOT)?.contains(q) == true) ||
                                (item.account?.name?.lowercase(Locale.ROOT)?.contains(q) == true) ||
                                (item.account?.nickname?.lowercase(Locale.ROOT)?.contains(q) == true) ||
                                (item.transaction.notes?.lowercase(Locale.ROOT)?.contains(q) == true) ||
                                item.transaction.amount.toString().contains(q)
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Previous month transactions for MoM comparison chart
    val previousMonthTransactions: StateFlow<List<TransactionWithDetails>> = _selectedMonth
        .flatMapLatest { currentMonth ->
            val prevMonthOption = availableMonths.getOrNull(availableMonths.indexOf(currentMonth) + 1)
            if (prevMonthOption != null) {
                transactionDao.getTransactionsWithDetailsForDateRange(
                    prevMonthOption.startTimestamp,
                    prevMonthOption.endTimestamp
                )
            } else {
                transactionDao.getTransactionsWithDetailsForDateRange(0, 0)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val monthlySpendingSummary: StateFlow<MonthlySpendingSummary> = allTransactions
        .map { list ->
            val expenseItems = list.filter { it.transaction.transactionType == TransactionType.EXPENSE }
            val gross = expenseItems.sumOf { it.transaction.amount }
            val reimbursements = expenseItems.sumOf { it.transaction.reimbursementAmount }
            val net = (gross - reimbursements).coerceAtLeast(0.0)
            MonthlySpendingSummary(
                grossExpense = gross,
                totalReimbursements = reimbursements,
                netExpense = net
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MonthlySpendingSummary(0.0, 0.0, 0.0)
        )

    val categoryBreakdown: StateFlow<List<CategoryBreakdownItem>> = allTransactions
        .map { list ->
            val expenseItems = list.filter { it.transaction.transactionType == TransactionType.EXPENSE }
            val totalNetExpense = expenseItems.sumOf { it.transaction.amount - it.transaction.reimbursementAmount }
                .coerceAtLeast(0.0)

            if (totalNetExpense <= 0.0) {
                emptyList()
            } else {
                expenseItems
                    .groupBy { it.category?.id ?: 0L }
                    .map { (catId, items) ->
                        val firstCat = items.first().category
                        val catName = firstCat?.name ?: "General"
                        val catColor = firstCat?.colorHex
                        val catNetSum = items.sumOf { it.transaction.amount - it.transaction.reimbursementAmount }
                            .coerceAtLeast(0.0)
                        val pct = ((catNetSum / totalNetExpense) * 100).toFloat()
                        CategoryBreakdownItem(
                            categoryId = catId,
                            categoryName = catName,
                            colorHex = catColor,
                            totalAmount = catNetSum,
                            percentage = pct
                        )
                    }
                    .sortedByDescending { it.totalAmount }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Detected Subscriptions & Recurring Bills
    val detectedSubscriptions: StateFlow<List<DetectedSubscription>> = transactionDao
        .getAllTransactionsWithDetails()
        .map { list -> subscriptionDetector.detectSubscriptions(list) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pendingSettlements: StateFlow<List<TransactionWithDetails>> = transactionDao
        .getPendingSettlements()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalPendingSettlements: StateFlow<Double> = pendingSettlements
        .map { list -> list.sumOf { it.transaction.reimbursementAmount } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    init {
        viewModelScope.launch {
            AppDatabase.seedInitialData(db)
        }

        val hasReadPermission = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val hasCompletedInitialScan = prefs.getBoolean(PREF_INITIAL_SCAN_DONE, false)
        if (hasReadPermission && !hasCompletedInitialScan) {
            scanHistoricalSms(force = false)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCustomDateRange(range: Pair<Long, Long>?) {
        _customDateRange.value = range
    }

    fun setThemeMode(isDark: Boolean?) {
        _isDarkMode.value = isDark
        if (isDark == null) {
            prefs.edit().remove(PREF_DARK_MODE).apply()
        } else {
            prefs.edit().putBoolean(PREF_DARK_MODE, isDark).apply()
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        _isBiometricEnabled.value = enabled
        prefs.edit().putBoolean(PREF_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun setPersistedLoggedIn(loggedIn: Boolean) {
        prefs.edit().putBoolean(PREF_IS_LOGGED_IN, loggedIn).apply()
    }

    fun isPersistedLoggedIn(): Boolean {
        return prefs.getBoolean(PREF_IS_LOGGED_IN, false)
    }

    fun signOut(onComplete: () -> Unit) {
        setPersistedLoggedIn(false)
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            // Safe fallback
        }
        onComplete()
    }

    fun selectMonth(month: MonthOption) {
        _selectedMonth.value = month
        _customDateRange.value = null
    }

    fun selectAccount(account: Account?) {
        _selectedAccount.value = account
    }

    fun scanHistoricalSms(force: Boolean = false) {
        val hasCompletedInitialScan = prefs.getBoolean(PREF_INITIAL_SCAN_DONE, false)
        if (!force && hasCompletedInitialScan) {
            return
        }

        viewModelScope.launch {
            _isScanning.value = true
            _scanMessage.value = "Scanning SMS inbox for bank transactions..."
            try {
                val count = smsScanner.scanInbox()
                prefs.edit().putBoolean(PREF_INITIAL_SCAN_DONE, true).apply()
                _scanMessage.value = if (count > 0) {
                    "Successfully imported $count transaction${if (count > 1) "s" else ""} from SMS inbox"
                } else {
                    "No new bank transactions found"
                }
            } catch (e: Exception) {
                _scanMessage.value = "Failed to scan SMS: ${e.localizedMessage}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun importCsvStatement(uri: Uri, targetAccountId: Long? = null) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val result = csvParser.importCsvFromUri(uri, targetAccountId)
                _importMessage.value = result
            } catch (e: Exception) {
                _importMessage.value = CsvImportResult(0, 0, 0, 1, "Import failed: ${e.localizedMessage}")
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun clearImportMessage() {
        _importMessage.value = null
    }

    fun exportTransactionsCsv(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val list = allTransactions.value
            val success = csvExporter.exportTransactionsToUri(uri, list)
            onResult(success)
        }
    }

    fun saveCategoryBudget(categoryId: Long, monthlyLimit: Double) {
        viewModelScope.launch {
            budgetDao.insertOrUpdateBudget(
                Budget(categoryId = categoryId, monthlyLimit = monthlyLimit)
            )
            checkCategoryBudgetAlert(categoryId)
        }
    }

    fun deleteCategoryBudget(categoryId: Long) {
        viewModelScope.launch {
            val existing = budgetDao.getBudgetForCategory(categoryId)
            if (existing != null) {
                budgetDao.deleteBudget(existing)
            }
        }
    }

    private suspend fun checkCategoryBudgetAlert(categoryId: Long) {
        val budget = budgetDao.getBudgetForCategory(categoryId) ?: return
        val category = categoryDao.getCategoryById(categoryId) ?: return
        val currentMonthTxns = allTransactions.value.filter {
            it.category?.id == categoryId && it.transaction.transactionType == TransactionType.EXPENSE
        }
        val spent = currentMonthTxns.sumOf { it.transaction.amount - it.transaction.reimbursementAmount }
        budgetNotifier.checkAndNotifyBudget(category.name, spent, budget.monthlyLimit)
    }

    fun onPermissionsResult(allGranted: Boolean) {
        if (allGranted) {
            val hasCompletedInitialScan = prefs.getBoolean(PREF_INITIAL_SCAN_DONE, false)
            if (!hasCompletedInitialScan) {
                scanHistoricalSms(force = true)
            }
        }
    }

    fun clearScanMessage() {
        _scanMessage.value = null
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            transactionDao.deleteTransaction(transactionId)
        }
    }

    fun updateAccountNickname(accountId: Long, nickname: String?) {
        viewModelScope.launch {
            val trimmed = nickname?.trim()?.ifEmpty { null }
            accountDao.updateAccountNickname(accountId, trimmed)
        }
    }

    /**
     * Updates all fields of a transaction with live recalculation and optional auto-learning rule.
     */
    fun updateTransactionFull(
        transactionId: Long,
        merchantName: String,
        amount: Double,
        type: TransactionType,
        categoryId: Long,
        accountId: Long,
        isSplit: Boolean,
        reimbursementAmount: Double,
        peerName: String?,
        notes: String?,
        saveRule: Boolean,
        keyword: String
    ) {
        viewModelScope.launch {
            transactionDao.insertTransaction(
                TransactionEntity(
                    id = transactionId,
                    amount = amount,
                    merchantName = merchantName,
                    timestamp = System.currentTimeMillis(),
                    transactionType = type,
                    accountId = accountId,
                    categoryId = categoryId,
                    isSplit = isSplit,
                    reimbursementAmount = reimbursementAmount,
                    settled = false,
                    peerName = peerName,
                    notes = notes
                )
            )

            if (saveRule && keyword.isNotBlank()) {
                categoryRuleDao.insertRule(
                    CategoryRule(
                        merchantKeyword = keyword.trim().lowercase(Locale.ROOT),
                        targetCategoryId = categoryId
                    )
                )
            }

            checkCategoryBudgetAlert(categoryId)
        }
    }

    fun saveCategoryRule(merchantKeyword: String, targetCategoryId: Long) {
        viewModelScope.launch {
            categoryRuleDao.insertRule(
                CategoryRule(
                    merchantKeyword = merchantKeyword.trim().lowercase(Locale.ROOT),
                    targetCategoryId = targetCategoryId
                )
            )
        }
    }

    fun deleteCategoryRule(rule: CategoryRule) {
        viewModelScope.launch {
            categoryRuleDao.deleteRule(rule)
        }
    }

    fun markSettled(transactionId: Long, settled: Boolean = true) {
        viewModelScope.launch {
            transactionDao.updateSettlementStatus(transactionId, settled)
        }
    }

    fun addManualTransaction(
        amount: Double,
        merchantName: String,
        type: TransactionType,
        categoryId: Long,
        accountId: Long,
        timestamp: Long = System.currentTimeMillis(),
        isSplit: Boolean = false,
        reimbursementAmount: Double = 0.0,
        peerName: String? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            transactionDao.insertTransaction(
                TransactionEntity(
                    amount = amount,
                    merchantName = merchantName,
                    timestamp = timestamp,
                    transactionType = type,
                    accountId = accountId,
                    categoryId = categoryId,
                    isSplit = isSplit,
                    reimbursementAmount = reimbursementAmount,
                    settled = false,
                    peerName = peerName,
                    notes = notes
                )
            )

            if (type == TransactionType.EXPENSE) {
                checkCategoryBudgetAlert(categoryId)
            }
        }
    }

    private fun generateAvailableMonths(): List<MonthOption> {
        val list = mutableListOf<MonthOption>()
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())

        for (i in 0 until 12) {
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val label = if (i == 0) "This Month (${sdf.format(cal.time)})" else sdf.format(cal.time)

            val startCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, startCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }

            list.add(
                MonthOption(
                    year = year,
                    month = month,
                    label = label,
                    startTimestamp = startCal.timeInMillis,
                    endTimestamp = endCal.timeInMillis
                )
            )
            cal.add(Calendar.MONTH, -1)
        }
        return list
    }

    companion object {
        private const val PREF_INITIAL_SCAN_DONE = "pref_initial_scan_done"
        private const val PREF_DARK_MODE = "pref_dark_mode"
        private const val PREF_IS_LOGGED_IN = "pref_is_logged_in"
        private const val PREF_BIOMETRIC_ENABLED = "pref_biometric_enabled"
    }
}

private data class DateFilterParams(
    val month: MonthOption,
    val account: Account?,
    val customDateRange: Pair<Long, Long>?,
    val query: String
)
