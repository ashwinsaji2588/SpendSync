package com.example.expensetracker.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.expensetracker.data.Account
import com.example.expensetracker.data.AccountType
import com.example.expensetracker.data.Category
import com.example.expensetracker.data.CategoryRule
import com.example.expensetracker.data.TransactionType
import com.example.expensetracker.data.TransactionWithDetails
import com.example.expensetracker.ui.components.AdvancedAnalyticsSection
import com.example.expensetracker.ui.components.BudgetsDialog
import com.example.expensetracker.ui.components.CreateAccountDialog
import com.example.expensetracker.ui.components.DeleteAccountConfirmationDialog
import com.example.expensetracker.ui.components.EditAccountDialog
import com.example.expensetracker.ui.components.FullEditTransactionDialog
import com.example.expensetracker.ui.components.SpendSyncLogo
import com.example.expensetracker.ui.components.SubscriptionsDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val transactions by viewModel.allTransactions.collectAsState()
    val previousMonthTransactions by viewModel.previousMonthTransactions.collectAsState()
    val spendingSummary by viewModel.monthlySpendingSummary.collectAsState()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedAccount by viewModel.selectedAccount.collectAsState()
    val allAccounts by viewModel.allAccounts.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val allRules by viewModel.allRules.collectAsState()
    val allBudgets by viewModel.allBudgets.collectAsState()
    val detectedSubscriptions by viewModel.detectedSubscriptions.collectAsState()
    val isBalanceVisible by viewModel.isBalanceVisible.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanMessage by viewModel.scanMessage.collectAsState()
    val importMessage by viewModel.importMessage.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showManualAddDialog by remember { mutableStateOf(false) }
    var showRulesManagerDialog by remember { mutableStateOf(false) }
    var showBudgetsDialog by remember { mutableStateOf(false) }
    var showSubscriptionsDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var selectedTransactionForEdit by remember { mutableStateOf<TransactionWithDetails?>(null) }
    var transactionToDelete by remember { mutableStateOf<TransactionWithDetails?>(null) }
    var selectedCategoryForDrillDown by remember { mutableStateOf<CategoryBreakdownItem?>(null) }
    var showAiInsightsDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    var showCreateAccountDialog by remember { mutableStateOf(false) }
    var accountToDeleteWithCount by remember { mutableStateOf<Pair<Account, Int>?>(null) }

    // File Import Launcher
    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importCsvStatement(it, selectedAccount?.id)
        }
    }

    // CSV Export Launcher
    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportTransactionsCsv(it) { success ->
                Toast.makeText(
                    context,
                    if (success) "Statement exported successfully!" else "Failed to export CSV",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val requiredPermissions = remember {
        arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS
        )
    }

    var hasPermissions by remember {
        mutableStateOf(
            requiredPermissions.all { perm ->
                ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        hasPermissions = allGranted
        viewModel.onPermissionsResult(allGranted)
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(310.dp)
            ) {
                SidebarContent(
                    accounts = allAccounts,
                    selectedAccount = selectedAccount,
                    isDarkMode = isDarkMode,
                    isBiometricEnabled = isBiometricEnabled,
                    onToggleTheme = { dark -> viewModel.setThemeMode(dark) },
                    onToggleBiometric = { enable -> viewModel.setBiometricEnabled(enable) },
                    onAccountSelected = { account ->
                        viewModel.selectAccount(account)
                        coroutineScope.launch { drawerState.close() }
                    },
                    onEditAccount = { account ->
                        accountToEdit = account
                        coroutineScope.launch { drawerState.close() }
                    },
                    onAddNewAccount = {
                        showCreateAccountDialog = true
                        coroutineScope.launch { drawerState.close() }
                    },
                    onOpenBudgets = {
                        showBudgetsDialog = true
                        coroutineScope.launch { drawerState.close() }
                    },
                    onOpenAiInsights = {
                        showAiInsightsDialog = true
                        coroutineScope.launch { drawerState.close() }
                    },
                    onOpenSubscriptions = {
                        showSubscriptionsDialog = true
                        coroutineScope.launch { drawerState.close() }
                    },
                    onImportCsv = {
                        coroutineScope.launch { drawerState.close() }
                        csvImportLauncher.launch(arrayOf("text/*", "application/vnd.ms-excel", "text/csv", "text/comma-separated-values"))
                    },
                    onExportCsv = {
                        coroutineScope.launch { drawerState.close() }
                        csvExportLauncher.launch("SpendSync_${selectedMonth.label.replace(" ", "_")}.csv")
                    },
                    onOpenRules = {
                        showRulesManagerDialog = true
                        coroutineScope.launch { drawerState.close() }
                    },
                    onOpenSupport = {
                        showSupportDialog = true
                        coroutineScope.launch { drawerState.close() }
                    },
                    onOpenNotificationSettings = {
                        coroutineScope.launch { drawerState.close() }
                        try {
                            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open notification settings", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSignOut = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.signOut {
                            onSignOut()
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SpendSyncLogo(size = 32.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "SpendSync",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = selectedAccount?.let { it.nickname ?: it.name } ?: "All Accounts",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Open Sidebar")
                        }
                    },
                    actions = {
                        // Search Toggle
                        IconButton(onClick = { showSearchBar = !showSearchBar }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Transactions",
                                tint = if (searchQuery.isNotBlank() || showSearchBar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // Help / Support action button
                        IconButton(onClick = { showSupportDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Help & Support",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 8.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    if (hasPermissions) {
                                        viewModel.scanHistoricalSms(force = true)
                                    } else {
                                        permissionLauncher.launch(requiredPermissions)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Scan SMS Inbox",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showManualAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                // Scanning Progress Indicator
                if (isScanning) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }

                // Search Bar if toggled
                AnimatedVisibility(visible = showSearchBar) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search by merchant, category, note, amount...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // Month Selector Filter
                MonthSelectorRow(
                    months = viewModel.availableMonths,
                    selectedMonth = selectedMonth,
                    onMonthSelected = { viewModel.selectMonth(it) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Permission Banner if missing
                AnimatedVisibility(
                    visible = !hasPermissions,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "SMS Permissions Required",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Allow reading SMS to auto-track bank expenses.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { permissionLauncher.launch(requiredPermissions) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Grant", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // CSV Import Result Feedback Banner
                AnimatedVisibility(visible = importMessage != null) {
                    importMessage?.let { res ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (res.errorCount == 0 && res.importedCount > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Statement Import Result",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(text = res.message, fontSize = 11.sp)
                                }
                                TextButton(onClick = { viewModel.clearImportMessage() }) {
                                    Text("Dismiss", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Scan feedback banner
                AnimatedVisibility(visible = scanMessage != null) {
                    scanMessage?.let { msg ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = msg,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                TextButton(onClick = { viewModel.clearScanMessage() }) {
                                    Text("Dismiss", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Net & Gross Monthly Expenses Card
                    item {
                        MonthlyExpensesCard(
                            summary = spendingSummary,
                            monthLabel = selectedMonth.label,
                            accountLabel = selectedAccount?.let { it.nickname ?: it.name } ?: "All Accounts",
                            isBalanceVisible = isBalanceVisible,
                            onToggleVisibility = { viewModel.toggleBalanceVisibility() }
                        )
                    }

                    // Advanced Analytics (Daily Burn Rate & MoM Comparison)
                    item {
                        AdvancedAnalyticsSection(
                            currentMonthTransactions = transactions,
                            previousMonthTransactions = previousMonthTransactions,
                            monthLabel = selectedMonth.label
                        )
                    }

                    // Category Breakdown Section (Clickable drill-down)
                    if (categoryBreakdown.isNotEmpty()) {
                        item {
                            CategoryBreakdownCard(
                                breakdownItems = categoryBreakdown,
                                onCategoryClick = { item ->
                                    selectedCategoryForDrillDown = item
                                }
                            )
                        }
                    }

                    // Transactions Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "Search Results" else "Transactions",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${transactions.size} items",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Transactions List
                    if (transactions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(52.dp),
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "No matching transactions found" else "No transactions for this period",
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Import CSV statement, add manual expense, or scan SMS",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = transactions,
                            key = { it.transaction.id }
                        ) { item ->
                            TransactionItemCard(
                                item = item,
                                onClick = { selectedTransactionForEdit = item },
                                onDelete = { transactionToDelete = item }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }
    }

    // Full Transaction Edit Dialog
    selectedTransactionForEdit?.let { item ->
        FullEditTransactionDialog(
            transactionDetails = item,
            categories = allCategories,
            accounts = allAccounts,
            onDismiss = { selectedTransactionForEdit = null },
            onSave = { id, merchant, amt, type, catId, accId, notes, saveRule, kw, customCat ->
                viewModel.updateTransactionFull(
                    id, merchant, amt, type, catId, accId, notes, saveRule, kw, customCat
                )
                selectedTransactionForEdit = null
            },
            onDelete = { id ->
                viewModel.deleteTransaction(id)
                selectedTransactionForEdit = null
            }
        )
    }

    // AI Financial Insights Dialog
    if (showAiInsightsDialog) {
        val apiKey = viewModel.getGeminiApiKey()
        AiInsightsDialog(
            currentApiKey = apiKey,
            financialContext = "",
            onDismiss = { showAiInsightsDialog = false },
            onSaveApiKey = { key -> viewModel.setGeminiApiKey(key) },
            onSendQuery = { query -> viewModel.queryGeminiFinancialAssistant(query) }
        )
    }

    // Budgets Dialog
    if (showBudgetsDialog) {
        BudgetsDialog(
            categories = allCategories,
            budgets = allBudgets,
            spendingByCategory = categoryBreakdown,
            monthLabel = selectedMonth.label,
            onDismiss = { showBudgetsDialog = false },
            onSaveBudget = { catId, limit -> viewModel.saveCategoryBudget(catId, limit) },
            onDeleteBudget = { catId -> viewModel.deleteCategoryBudget(catId) }
        )
    }

    // Subscriptions & Recurring Bills Dialog
    if (showSubscriptionsDialog) {
        SubscriptionsDialog(
            subscriptions = detectedSubscriptions,
            onDismiss = { showSubscriptionsDialog = false }
        )
    }

    // Delete Confirmation Dialog
    transactionToDelete?.let { item ->
        val t = item.transaction
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Transaction", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete \"${t.merchantName}\" for ₹${String.format(Locale.getDefault(), "%.2f", t.amount)}? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(t.id)
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Full Edit Account Dialog
    accountToEdit?.let { account ->
        EditAccountDialog(
            account = account,
            allAccounts = allAccounts,
            onDismiss = { accountToEdit = null },
            onSave = { name, type, nickname, last4 ->
                viewModel.updateAccountDetails(account.id, name, type, nickname, last4)
                accountToEdit = null
            },
            onDeleteRequest = {
                val currentAccount = account
                accountToEdit = null
                coroutineScope.launch {
                    val count = viewModel.getTransactionCountForAccount(currentAccount.id)
                    accountToDeleteWithCount = currentAccount to count
                }
            }
        )
    }

    // Create Account Dialog
    if (showCreateAccountDialog) {
        CreateAccountDialog(
            onDismiss = { showCreateAccountDialog = false },
            onSave = { name, type, nickname, last4 ->
                viewModel.createAccount(name, type, nickname, last4)
                showCreateAccountDialog = false
            }
        )
    }

    // Delete Account Confirmation Dialog
    accountToDeleteWithCount?.let { (account, txnCount) ->
        val otherAccounts = allAccounts.filter { it.id != account.id }
        DeleteAccountConfirmationDialog(
            account = account,
            otherAccounts = otherAccounts,
            transactionCount = txnCount,
            onDismiss = { accountToDeleteWithCount = null },
            onConfirmDelete = { reassignId, cascade ->
                viewModel.deleteAccount(account.id, reassignId, cascade)
                accountToDeleteWithCount = null
            }
        )
    }

    // Category Drill-Down Dialog
    selectedCategoryForDrillDown?.let { categoryItem ->
        val filteredCategoryTransactions = transactions.filter {
            it.category?.id == categoryItem.categoryId || it.category?.name.equals(categoryItem.categoryName, ignoreCase = true)
        }
        CategoryDrillDownDialog(
            categoryItem = categoryItem,
            transactions = filteredCategoryTransactions,
            monthLabel = selectedMonth.label,
            onDismiss = { selectedCategoryForDrillDown = null },
            onTransactionClick = { item ->
                selectedCategoryForDrillDown = null
                selectedTransactionForEdit = item
            }
        )
    }

    // Support & Help Dialog
    if (showSupportDialog) {
        SupportHelpDialog(onDismiss = { showSupportDialog = false })
    }

    // Manual Entry Dialog
    if (showManualAddDialog) {
        ManualEntryDialog(
            accounts = allAccounts,
            categories = allCategories,
            onDismiss = { showManualAddDialog = false },
            onSave = { amount, merchant, type, categoryId, accountId, customCat ->
                viewModel.addManualTransaction(
                    amount = amount,
                    merchantName = merchant,
                    type = type,
                    categoryId = categoryId,
                    accountId = accountId,
                    customCategoryName = customCat
                )
                showManualAddDialog = false
            }
        )
    }

    // Category Rules Manager Dialog
    if (showRulesManagerDialog) {
        CategoryRulesDialog(
            rules = allRules,
            categories = allCategories,
            onDismiss = { showRulesManagerDialog = false },
            onDeleteRule = { rule -> viewModel.deleteCategoryRule(rule) },
            onAddRule = { keyword, categoryId, customCat -> viewModel.saveCategoryRule(keyword, categoryId, customCat) }
        )
    }
}

@Composable
fun SidebarContent(
    accounts: List<Account>,
    selectedAccount: Account?,
    isDarkMode: Boolean?,
    isBiometricEnabled: Boolean,
    onToggleTheme: (Boolean?) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onAccountSelected: (Account?) -> Unit,
    onEditAccount: (Account) -> Unit,
    onAddNewAccount: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenAiInsights: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onImportCsv: () -> Unit,
    onExportCsv: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onSignOut: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val sidebarPrefs = remember { context.getSharedPreferences("expense_tracker_prefs", android.content.Context.MODE_PRIVATE) }
    var isAccountsExpanded by remember {
        mutableStateOf(sidebarPrefs.getBoolean("pref_accounts_section_expanded", true))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpendSyncLogo(size = 38.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "SpendSync",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Smart Expense Manager",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Accounts & Cards Header with Collapse Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ACCOUNTS & CARDS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.8.sp
            )
            IconButton(
                onClick = {
                    isAccountsExpanded = !isAccountsExpanded
                    sidebarPrefs.edit().putBoolean("pref_accounts_section_expanded", isAccountsExpanded).apply()
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (isAccountsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isAccountsExpanded) "Collapse Accounts" else "Expand Accounts",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // All Accounts item
        NavigationDrawerItem(
            label = { Text("All Accounts", fontWeight = if (selectedAccount == null) FontWeight.Bold else FontWeight.Normal) },
            selected = selectedAccount == null,
            onClick = { onAccountSelected(null) },
            icon = { Icon(Icons.Default.AccountBox, contentDescription = null) },
            modifier = Modifier.padding(vertical = 2.dp)
        )

        // Collapsible accounts list
        if (isAccountsExpanded) {
            accounts.forEach { account ->
                val isSelected = selectedAccount?.id == account.id
                val icon = when (account.type) {
                    AccountType.CREDIT_CARD -> Icons.Default.ShoppingCart
                    AccountType.DEBIT_CARD -> Icons.Default.AccountBox
                    AccountType.CASH -> Icons.Default.Person
                    AccountType.WALLET -> Icons.Default.Share
                    else -> Icons.Default.AccountBox
                }
                val displayName = account.nickname ?: account.name
                NavigationDrawerItem(
                    label = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(displayName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                if (account.nickname != null) {
                                    Text(account.name, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                } else {
                                    Text(account.type.name.replace("_", " "), fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                            IconButton(
                                onClick = { onEditAccount(account) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Account",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    },
                    selected = isSelected,
                    onClick = { onAccountSelected(account) },
                    icon = { Icon(icon, contentDescription = null) },
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(14.dp))

        // Advanced Tools
        Text(
            text = "FINANCIAL SUITE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        // AI Financial Insights
        NavigationDrawerItem(
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("AI Financial Advisor")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gemini", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            selected = false,
            onClick = onOpenAiInsights,
            icon = { Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        )

        // Budgets
        NavigationDrawerItem(
            label = { Text("Monthly Budgets") },
            selected = false,
            onClick = onOpenBudgets,
            icon = { Icon(Icons.Default.DateRange, contentDescription = null) }
        )

        // Subscriptions
        NavigationDrawerItem(
            label = { Text("Subscriptions & Bills") },
            selected = false,
            onClick = onOpenSubscriptions,
            icon = { Icon(Icons.Default.Refresh, contentDescription = null) }
        )

        // Import CSV
        NavigationDrawerItem(
            label = { Text("Import Statement (CSV)") },
            selected = false,
            onClick = onImportCsv,
            icon = { Icon(Icons.Default.Add, contentDescription = null) }
        )

        // Export CSV
        NavigationDrawerItem(
            label = { Text("Export Data (CSV)") },
            selected = false,
            onClick = onExportCsv,
            icon = { Icon(Icons.Default.Share, contentDescription = null) }
        )

        // Auto-Rules item
        NavigationDrawerItem(
            label = { Text("Auto-Learning Rules") },
            selected = false,
            onClick = onOpenRules,
            icon = { Icon(Icons.Default.Edit, contentDescription = null) }
        )

        // GPay Notification Listener Settings
        NavigationDrawerItem(
            label = { Text("UPI Split Listener") },
            selected = false,
            onClick = onOpenNotificationSettings,
            icon = { Icon(Icons.Default.Notifications, contentDescription = null) }
        )

        // Support & Help
        NavigationDrawerItem(
            label = { Text("Help & Support") },
            selected = false,
            onClick = onOpenSupport,
            icon = { Icon(Icons.Default.Info, contentDescription = null) }
        )

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(14.dp))

        // Security & Appearance
        Text(
            text = "SECURITY & SETTINGS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        // Biometric App Lock
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Biometric App Lock",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isBiometricEnabled) "Fingerprint/PIN required" else "Off",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Switch(
                checked = isBiometricEnabled,
                onCheckedChange = { onToggleBiometric(it) }
            )
        }

        // Dark Theme Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Dark Mode",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isDarkMode == null) "System default" else if (isDarkMode == true) "Always on" else "Always off",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Switch(
                checked = isDarkMode == true,
                onCheckedChange = { onToggleTheme(it) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Sign Out Button
        TextButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun EditAccountNicknameDialog(
    account: Account,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit
) {
    var nicknameText by remember { mutableStateOf(account.nickname ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Account Nickname", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Account: ${account.name}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = nicknameText,
                    onValueChange = { nicknameText = it },
                    label = { Text("Custom Nickname") },
                    placeholder = { Text("e.g. Salary Account, Primary UPI") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(nicknameText.trim().ifEmpty { null })
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MonthlyExpensesCard(
    summary: MonthlySpendingSummary,
    monthLabel: String,
    accountLabel: String,
    isBalanceVisible: Boolean,
    onToggleVisibility: () -> Unit
) {
    val indianLocale = remember { Locale.Builder().setLanguage("en").setRegion("IN").build() }
    val formattedNet = remember(summary.netExpense) {
        "₹${String.format(indianLocale, "%.2f", summary.netExpense)}"
    }
    val formattedGross = remember(summary.grossExpense) {
        "₹${String.format(indianLocale, "%.2f", summary.grossExpense)}"
    }
    val formattedReimbursement = remember(summary.totalReimbursements) {
        "₹${String.format(indianLocale, "%.2f", summary.totalReimbursements)}"
    }

    fun maskAmount(amountStr: String): String {
        val prefix = "₹"
        val numeric = amountStr.removePrefix("₹").trim()
        if (numeric.isEmpty()) return "₹****"
        val parts = numeric.split(".")
        val integerPart = parts[0]
        val maskedInteger = if (integerPart.length <= 2) {
            "*"
        } else {
            val visibleChars = if (integerPart.length <= 4) 1 else 2
            integerPart.take(visibleChars) + integerPart.drop(visibleChars).map { if (it == ',') ',' else '*' }.joinToString("")
        }
        return "$prefix $maskedInteger.***"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF4A00E0),
                            Color(0xFF8E2DE2)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Net Spent ($monthLabel)",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = accountLabel,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBalanceVisible) formattedNet else maskAmount(formattedNet),
                        color = Color.White,
                        fontSize = if (isBalanceVisible) 32.sp else 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onToggleVisibility,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isBalanceVisible) "Hide amount" else "Reveal amount",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Sub-breakdown if split reimbursements exist
                if (summary.totalReimbursements > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Gross: " + (if (isBalanceVisible) formattedGross else maskAmount(formattedGross)),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Reimbursed: -" + (if (isBalanceVisible) formattedReimbursement else maskAmount(formattedReimbursement)),
                            color = Color(0xFF81C784),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Text(
                        text = "Auto-tracked from Bank SMS, CSV & Cash",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}



@Composable
fun CategoryBreakdownCard(
    breakdownItems: List<CategoryBreakdownItem>,
    onCategoryClick: (CategoryBreakdownItem) -> Unit
) {
    val indianLocale = remember { Locale.Builder().setLanguage("en").setRegion("IN").build() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Breakdown",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tap to view list",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            breakdownItems.forEach { item ->
                val categoryColor = getCategoryColor(item.categoryName)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onCategoryClick(item) }
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(categoryColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.categoryName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${String.format(indianLocale, "%.1f", item.percentage)}%)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Text(
                            text = "₹${String.format(indianLocale, "%.2f", item.totalAmount)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { (item.percentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = categoryColor,
                        trackColor = categoryColor.copy(alpha = 0.15f),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryDrillDownDialog(
    categoryItem: CategoryBreakdownItem,
    transactions: List<TransactionWithDetails>,
    monthLabel: String,
    onDismiss: () -> Unit,
    onTransactionClick: (TransactionWithDetails) -> Unit
) {
    val indianLocale = remember { Locale.Builder().setLanguage("en").setRegion("IN").build() }
    val categoryColor = getCategoryColor(categoryItem.categoryName)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(categoryColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = categoryItem.categoryName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "$monthLabel • ${transactions.size} transactions",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Total Spend Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = categoryColor.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Spent",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "₹${String.format(indianLocale, "%.2f", categoryItem.totalAmount)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = categoryColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Transaction list in category
                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions found in this category",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(transactions, key = { it.transaction.id }) { item ->
                            TransactionItemCard(
                                item = item,
                                onClick = { onTransactionClick(item) },
                                onDelete = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SupportHelpDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val supportEmail = "ashwinsaji2588@gmail.com"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Help & Support",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                LazyColumn(
                    modifier = Modifier.height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "About SpendSync",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "SpendSync automatically reads transaction SMS from your bank to track debit, spend, and split reimbursements completely on-device.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        Text(
                            text = "CSV Statement & Bulk File Upload",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Import bank statements in CSV format from your device storage. SpendSync auto-maps columns, auto-categorizes entries, and skips duplicates.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        Text(
                            text = "Background SMS Permissions Troubleshooting",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "On devices with aggressive battery optimization (Xiaomi/MIUI, Samsung, OnePlus, Oppo, Vivo):\n" +
                                    "1. Go to Settings > Apps > SpendSync.\n" +
                                    "2. Enable 'Autostart' or 'Background Activity'.\n" +
                                    "3. Set Battery Saver to 'No Restrictions'.\n" +
                                    "4. Ensure SMS permissions are set to 'Always Allow'.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        Text(
                            text = "Developer & Support Contact",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "For queries, bug reports, or feature requests, contact:\n$supportEmail",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:$supportEmail")
                            putExtra(Intent.EXTRA_SUBJECT, "SpendSync Support & Feedback")
                        }
                        runCatching { context.startActivity(emailIntent) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Email Support ($supportEmail)", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun TransactionItemCard(
    item: TransactionWithDetails,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val transaction = item.transaction
    val categoryName = item.category?.name ?: "General"
    val accountDisplayName = item.account?.let { it.nickname ?: it.name } ?: "Account"

    val indianLocale = remember { Locale.Builder().setLanguage("en").setRegion("IN").build() }
    val formattedDate = remember(transaction.timestamp) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(transaction.timestamp))
    }

    val (amountColor, amountPrefix) = when (transaction.transactionType) {
        TransactionType.EXPENSE -> Color(0xFFE53935) to "- "
        TransactionType.INCOME -> Color(0xFF43A047) to "+ "
        TransactionType.TRANSFER -> Color(0xFF757575) to "⇄ "
    }

    val categoryIcon = getCategoryIcon(categoryName)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Category Icon Bubble
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(amountColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = categoryName,
                        tint = amountColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = transaction.merchantName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$categoryName • $accountDisplayName",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (categoryName.equals("Reimbursements", ignoreCase = true)) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFE0F2F1)
                            ) {
                                Text(
                                    text = "Offset",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00796B),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix₹${String.format(indianLocale, "%.2f", transaction.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = amountColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Transaction",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryRulesDialog(
    rules: List<CategoryRule>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onDeleteRule: (CategoryRule) -> Unit,
    onAddRule: (String, Long, String?) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddSheet by remember { mutableStateOf(false) }

    // Group rules by Category
    val groupedRules = remember(rules, categories, searchQuery) {
        val query = searchQuery.trim().lowercase(Locale.ROOT)
        categories.mapNotNull { category ->
            val catRules = rules.filter { it.targetCategoryId == category.id }
            val matchingRules = if (query.isBlank()) {
                catRules
            } else {
                catRules.filter {
                    it.merchantKeyword.contains(query, ignoreCase = true) ||
                            category.name.contains(query, ignoreCase = true)
                }
            }
            if (matchingRules.isNotEmpty() || (query.isNotBlank() && category.name.contains(query, ignoreCase = true))) {
                category to matchingRules
            } else if (query.isBlank() && catRules.isNotEmpty()) {
                category to catRules
            } else {
                null
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .heightIn(min = 400.dp, max = 650.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Auto-Categorize Rules",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${rules.size} active keyword mapping${if (rules.size != 1) "s" else ""}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { showAddSheet = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sticky Search Bar Filter
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search keywords or categories...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Category-Centric Cards
                if (groupedRules.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No rules match \"$searchQuery\"" else "No custom rules defined yet",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Rules auto-learn when you change transaction categories, or tap '+ Add'",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(groupedRules, key = { it.first.id }) { (category, catRules) ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        try {
                                                            Color(android.graphics.Color.parseColor(category.colorHex))
                                                        } catch (e: Exception) {
                                                            MaterialTheme.colorScheme.primary
                                                        }
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = category.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = "${catRules.size} keyword${if (catRules.size != 1) "s" else ""}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        catRules.forEach { rule ->
                                            InputChip(
                                                selected = false,
                                                onClick = {},
                                                label = {
                                                    Text(
                                                        text = rule.merchantKeyword,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                },
                                                trailingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Delete ${rule.merchantKeyword}",
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .clickable { onDeleteRule(rule) }
                                                    )
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = InputChipDefaults.inputChipColors(
                                                    containerColor = MaterialTheme.colorScheme.surface
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Add Modal Bottom Sheet
    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            var newKeyword by remember { mutableStateOf("") }
            var selectedCatId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 1L) }
            var isCustomCat by remember { mutableStateOf(false) }
            var customCatName by remember { mutableStateOf("") }
            var categoryDropdownExpanded by remember { mutableStateOf(false) }

            val currentCat = categories.find { it.id == selectedCatId } ?: categories.firstOrNull()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Add Auto-Categorize Rule",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Any transaction whose merchant contains this keyword will be auto-categorized.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = newKeyword,
                    onValueChange = { newKeyword = it },
                    label = { Text("Merchant Keyword") },
                    placeholder = { Text("e.g. uber, swiggy, starbucks, ksfe") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (!isCustomCat) {
                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentCat?.name ?: "Target Category",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Target Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        selectedCatId = category.id
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("+ Add Custom Category...", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                    }
                                },
                                onClick = {
                                    isCustomCat = true
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = customCatName,
                        onValueChange = { customCatName = it },
                        label = { Text("New Custom Category Name") },
                        placeholder = { Text("e.g. Mutual Funds, KSFE Chitty") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { isCustomCat = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel Custom")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        if (newKeyword.isNotBlank()) {
                            val custom = if (isCustomCat && customCatName.isNotBlank()) customCatName.trim() else null
                            onAddRule(newKeyword.trim(), selectedCatId, custom)
                            showAddSheet = false
                        }
                    },
                    enabled = newKeyword.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Rule")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryDialog(
    accounts: List<Account>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (amount: Double, merchant: String, type: TransactionType, categoryId: Long, accountId: Long, customCat: String?) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var merchantText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCatId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 1L) }
    var selectedAccId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 1L) }
    var isCustomCat by remember { mutableStateOf(false) }
    var customCatName by remember { mutableStateOf("") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val currentCat = categories.find { it.id == selectedCatId } ?: categories.firstOrNull()
    val currentAcc = accounts.find { it.id == selectedAccId } ?: accounts.firstOrNull()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = when (selectedType) {
                        TransactionType.INCOME -> "Add Income / Reimbursement"
                        TransactionType.TRANSFER -> "Add Transfer"
                        else -> "Add Expense"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Type Toggle (Expense / Income / Transfer)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedType == TransactionType.EXPENSE,
                            onClick = { selectedType = TransactionType.EXPENSE },
                            label = { Text("Expense", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedType == TransactionType.INCOME,
                            onClick = {
                                selectedType = TransactionType.INCOME
                                val reimbCat = categories.find { it.name.equals("Reimbursements", ignoreCase = true) }
                                if (reimbCat != null) {
                                    selectedCatId = reimbCat.id
                                }
                            },
                            label = { Text("Income", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedType == TransactionType.TRANSFER,
                            onClick = { selectedType = TransactionType.TRANSFER },
                            label = { Text("Transfer", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Amount
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            errorMessage = null
                        },
                        label = { Text("Amount (₹)") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Merchant / Description
                    OutlinedTextField(
                        value = merchantText,
                        onValueChange = {
                            merchantText = it
                            errorMessage = null
                        },
                        label = { Text(if (selectedType == TransactionType.EXPENSE) "Merchant / Spent on" else "Source / From") },
                        placeholder = { Text("e.g. Swiggy, Salary, Friend Reimbursement") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Category Selector
                    if (!isCustomCat) {
                        ExposedDropdownMenuBox(
                            expanded = categoryDropdownExpanded,
                            onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = currentCat?.name ?: "Category",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                shape = RoundedCornerShape(12.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false }
                            ) {
                                categories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category.name) },
                                        onClick = {
                                            selectedCatId = category.id
                                            isCustomCat = false
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("+ Add Custom Category...", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                        }
                                    },
                                    onClick = {
                                        isCustomCat = true
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = customCatName,
                            onValueChange = { customCatName = it },
                            label = { Text("New Custom Category Name") },
                            placeholder = { Text("e.g. Investment, KSFE, Pet Care") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { isCustomCat = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel Custom Category")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Account Selector
                    ExposedDropdownMenuBox(
                        expanded = accountDropdownExpanded,
                        onExpandedChange = { accountDropdownExpanded = !accountDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentAcc?.let { it.nickname ?: it.name } ?: "Account",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Account") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = accountDropdownExpanded,
                            onDismissRequest = { accountDropdownExpanded = false }
                        ) {
                            accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text(account.nickname ?: account.name) },
                                    onClick = {
                                        selectedAccId = account.id
                                        accountDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val amountVal = amountText.toDoubleOrNull()
                            if (amountVal == null || amountVal <= 0.0) {
                                errorMessage = "Please enter a valid amount greater than 0"
                                return@Button
                            }

                            val merchantVal = merchantText.trim().ifEmpty {
                                if (selectedType == TransactionType.EXPENSE) "Manual Expense" else "Manual Income"
                            }
                            val custom = if (isCustomCat && customCatName.isNotBlank()) customCatName.trim() else null

                            onSave(
                                amountVal,
                                merchantVal,
                                selectedType,
                                selectedCatId,
                                selectedAccId,
                                custom
                            )
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun MonthSelectorRow(
    months: List<MonthOption>,
    selectedMonth: MonthOption,
    onMonthSelected: (MonthOption) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        months.forEach { month ->
            val isSelected = month.startTimestamp == selectedMonth.startTimestamp
            FilterChip(
                selected = isSelected,
                onClick = { onMonthSelected(month) },
                label = {
                    Text(
                        text = month.label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

private fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase(Locale.ROOT)) {
        "food" -> Icons.Default.ShoppingCart
        "entertainment" -> Icons.Default.DateRange
        "shopping" -> Icons.Default.ShoppingCart
        "grocery" -> Icons.Default.ShoppingCart
        "travel" -> Icons.Default.Call
        "bills & utilities" -> Icons.Default.AccountBox
        "health" -> Icons.Default.Info
        "investment" -> Icons.Default.Share
        else -> Icons.Default.Info
    }
}

private fun getCategoryColor(category: String): Color {
    return when (category.lowercase(Locale.ROOT)) {
        "food" -> Color(0xFFFF7043)
        "entertainment" -> Color(0xFFAB47BC)
        "shopping" -> Color(0xFF29B6F6)
        "grocery" -> Color(0xFF66BB6A)
        "travel" -> Color(0xFFFFA726)
        "bills & utilities" -> Color(0xFFEF5350)
        "health" -> Color(0xFFE91E63)
        "investment" -> Color(0xFF009688)
        "salary" -> Color(0xFF26A69A)
        "freelance" -> Color(0xFF42A5F5)
        else -> Color(0xFF7E57C2)
    }
}
