package com.example.expensetracker.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.Account
import com.example.expensetracker.data.AccountType
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.Category
import com.example.expensetracker.data.CategoryRule
import com.example.expensetracker.data.TransactionEntity
import com.example.expensetracker.data.TransactionType
import com.example.expensetracker.data.TransactionWithDetails
import com.example.expensetracker.domain.HistoricalSmsScanner
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

    private val smsScanner = HistoricalSmsScanner(application, db)
    private val prefs = application.getSharedPreferences("expense_tracker_prefs", Context.MODE_PRIVATE)

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanMessage = MutableStateFlow<String?>(null)
    val scanMessage: StateFlow<String?> = _scanMessage.asStateFlow()

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

    val allTransactions: StateFlow<List<TransactionWithDetails>> = combine(
        _selectedMonth,
        _selectedAccount
    ) { month, account -> month to account }
        .flatMapLatest { (month, account) ->
            if (account == null) {
                transactionDao.getTransactionsWithDetailsForDateRange(
                    month.startTimestamp,
                    month.endTimestamp
                )
            } else {
                transactionDao.getTransactionsWithDetailsForAccountAndDateRange(
                    account.id,
                    month.startTimestamp,
                    month.endTimestamp
                )
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

    fun selectMonth(month: MonthOption) {
        _selectedMonth.value = month
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

    /**
     * Updates transaction category and optionally saves an auto-learning rule.
     */
    fun updateTransactionCategory(
        transactionId: Long,
        newCategoryId: Long,
        saveRule: Boolean = false,
        merchantKeyword: String? = null
    ) {
        viewModelScope.launch {
            transactionDao.updateTransactionCategory(transactionId, newCategoryId)
            if (saveRule && !merchantKeyword.isNullOrBlank()) {
                categoryRuleDao.insertRule(
                    CategoryRule(
                        merchantKeyword = merchantKeyword.trim().lowercase(Locale.ROOT),
                        targetCategoryId = newCategoryId
                    )
                )
            }
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
    }
}
