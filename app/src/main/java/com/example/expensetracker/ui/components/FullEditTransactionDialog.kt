package com.example.expensetracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.expensetracker.data.Account
import com.example.expensetracker.data.Category
import com.example.expensetracker.data.TransactionType
import com.example.expensetracker.data.TransactionWithDetails
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullEditTransactionDialog(
    transactionDetails: TransactionWithDetails,
    categories: List<Category>,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (
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
        keyword: String,
        customCategoryName: String?
    ) -> Unit,
    onDelete: (Long) -> Unit
) {
    val t = transactionDetails.transaction

    var merchantText by remember { mutableStateOf(t.merchantName) }
    var amountText by remember { mutableStateOf(String.format(Locale.US, "%.2f", t.amount)) }
    var selectedType by remember { mutableStateOf(t.transactionType) }
    var selectedCatId by remember { mutableStateOf(t.categoryId) }
    var selectedAccId by remember { mutableStateOf(t.accountId) }
    var isCreatingCustomCategory by remember { mutableStateOf(false) }
    var customCategoryText by remember { mutableStateOf("") }

    var isSplitEnabled by remember { mutableStateOf(t.isSplit) }
    var reimbursementText by remember { mutableStateOf(if (t.reimbursementAmount > 0) String.format(Locale.US, "%.2f", t.reimbursementAmount) else "") }
    var peerNameText by remember { mutableStateOf(t.peerName ?: "") }
    var notesText by remember { mutableStateOf(t.notes ?: "") }

    var saveRuleCheckbox by remember { mutableStateOf(false) }
    var keywordText by remember { mutableStateOf(t.merchantName) }

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
                .height(600.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Edit Transaction", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Type Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedType == TransactionType.EXPENSE,
                            onClick = { selectedType = TransactionType.EXPENSE },
                            label = { Text("Expense") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedType == TransactionType.INCOME,
                            onClick = { selectedType = TransactionType.INCOME },
                            label = { Text("Income") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedType == TransactionType.TRANSFER,
                            onClick = { selectedType = TransactionType.TRANSFER },
                            label = { Text("Transfer") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Merchant / Description
                    OutlinedTextField(
                        value = merchantText,
                        onValueChange = {
                            merchantText = it
                            errorMessage = null
                        },
                        label = { Text("Merchant / Details") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Amount
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            errorMessage = null
                        },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Category Selector & Custom Category Creation
                    if (!isCreatingCustomCategory) {
                        ExposedDropdownMenuBox(
                            expanded = categoryDropdownExpanded,
                            onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = currentCat?.name ?: "Select Category",
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
                                            isCreatingCustomCategory = false
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
                                        isCreatingCustomCategory = true
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = customCategoryText,
                            onValueChange = { customCategoryText = it },
                            label = { Text("New Custom Category Name") },
                            placeholder = { Text("e.g. Investment, KSFE, Donations") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { isCreatingCustomCategory = false }) {
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
                            value = currentAcc?.let { it.nickname ?: it.name } ?: "Select Account",
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

                    // Split Details Toggle
                    if (selectedType == TransactionType.EXPENSE) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Split Expense (Reimbursement)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = isSplitEnabled,
                                onCheckedChange = { isSplitEnabled = it }
                            )
                        }

                        if (isSplitEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = peerNameText,
                                    onValueChange = { peerNameText = it },
                                    label = { Text("Friend Name") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                OutlinedTextField(
                                    value = reimbursementText,
                                    onValueChange = { reimbursementText = it },
                                    label = { Text("Owed (₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    // Notes
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Notes / Tags") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Auto-learning Rule Checkbox
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = saveRuleCheckbox,
                            onCheckedChange = { saveRuleCheckbox = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Auto-learn: Always categorize matching transactions as '${if (isCreatingCustomCategory && customCategoryText.isNotBlank()) customCategoryText else currentCat?.name}'",
                            fontSize = 12.sp
                        )
                    }

                    if (saveRuleCheckbox) {
                        OutlinedTextField(
                            value = keywordText,
                            onValueChange = { keywordText = it },
                            label = { Text("Merchant Keyword Rule") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    errorMessage?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Delete Button
                    OutlinedButton(
                        onClick = { onDelete(t.id) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Transaction")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull()
                            if (amt == null || amt <= 0.0) {
                                errorMessage = "Please enter a valid amount"
                                return@Button
                            }
                            val merch = merchantText.trim().ifEmpty { "Transaction" }
                            val reimb = if (isSplitEnabled) reimbursementText.toDoubleOrNull() ?: 0.0 else 0.0
                            val peer = if (isSplitEnabled) peerNameText.trim().ifEmpty { "Friend" } else null

                            val customCat = if (isCreatingCustomCategory && customCategoryText.isNotBlank()) {
                                customCategoryText.trim()
                            } else null

                            onSave(
                                t.id,
                                merch,
                                amt,
                                selectedType,
                                selectedCatId,
                                selectedAccId,
                                isSplitEnabled,
                                reimb,
                                peer,
                                notesText.trim().ifEmpty { null },
                                saveRuleCheckbox,
                                keywordText,
                                customCat
                            )
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}
