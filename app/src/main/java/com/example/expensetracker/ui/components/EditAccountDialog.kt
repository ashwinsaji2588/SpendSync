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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.expensetracker.data.Account
import com.example.expensetracker.data.AccountType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountDialog(
    account: Account,
    allAccounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (name: String, type: AccountType, nickname: String?, last4: String?) -> Unit,
    onDeleteRequest: () -> Unit
) {
    var nameText by remember { mutableStateOf(account.name) }
    var nicknameText by remember { mutableStateOf(account.nickname ?: "") }
    var last4Text by remember { mutableStateOf(account.accountNumberLast4 ?: "") }
    var selectedType by remember { mutableStateOf(account.type) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    val accountTypes = remember {
        listOf(
            AccountType.BANK_ACCOUNT to "Bank Account",
            AccountType.CREDIT_CARD to "Credit Card",
            AccountType.DEBIT_CARD to "Debit Card",
            AccountType.CASH to "Cash",
            AccountType.WALLET to "Digital Wallet"
        )
    }

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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Account",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Account Name
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Account Name") },
                    placeholder = { Text("e.g. HDFC Bank, ICICI Amazon Pay") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Custom Nickname
                OutlinedTextField(
                    value = nicknameText,
                    onValueChange = { nicknameText = it },
                    label = { Text("Custom Nickname") },
                    placeholder = { Text("e.g. Salary Account, Shopping Card") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Account Classification / Type
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = accountTypes.firstOrNull { it.first == selectedType }?.second ?: selectedType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account Classification") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        accountTypes.forEach { (type, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedType = type
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Account Number / Last 4 Digits
                OutlinedTextField(
                    value = last4Text,
                    onValueChange = { if (it.length <= 6) last4Text = it },
                    label = { Text("Account / Card Last 4 Digits (Optional)") },
                    placeholder = { Text("e.g. 7009") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Delete Button (if more than 1 account exists)
                if (allAccounts.size > 1) {
                    OutlinedButton(
                        onClick = onDeleteRequest,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete This Account")
                    }
                }

                // Action Buttons
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
                            val trimmedName = nameText.trim().ifEmpty { account.name }
                            onSave(
                                trimmedName,
                                selectedType,
                                nicknameText.trim().ifEmpty { null },
                                last4Text.trim().ifEmpty { null }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, type: AccountType, nickname: String?, last4: String?) -> Unit
) {
    var nameText by remember { mutableStateOf("") }
    var nicknameText by remember { mutableStateOf("") }
    var last4Text by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.BANK_ACCOUNT) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    val accountTypes = remember {
        listOf(
            AccountType.BANK_ACCOUNT to "Bank Account",
            AccountType.CREDIT_CARD to "Credit Card",
            AccountType.DEBIT_CARD to "Debit Card",
            AccountType.CASH to "Cash",
            AccountType.WALLET to "Digital Wallet"
        )
    }

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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add New Account",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Account Name") },
                    placeholder = { Text("e.g. Axis Bank, SBI Card") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = nicknameText,
                    onValueChange = { nicknameText = it },
                    label = { Text("Nickname (Optional)") },
                    placeholder = { Text("e.g. Travel Card, Savings") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = accountTypes.firstOrNull { it.first == selectedType }?.second ?: selectedType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account Classification") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        accountTypes.forEach { (type, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedType = type
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = last4Text,
                    onValueChange = { if (it.length <= 6) last4Text = it },
                    label = { Text("Account / Card Last 4 Digits (Optional)") },
                    placeholder = { Text("e.g. 1234") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

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
                            val trimmedName = nameText.trim().ifEmpty { "New Account" }
                            onSave(
                                trimmedName,
                                selectedType,
                                nicknameText.trim().ifEmpty { null },
                                last4Text.trim().ifEmpty { null }
                            )
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add Account")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountConfirmationDialog(
    account: Account,
    otherAccounts: List<Account>,
    transactionCount: Int,
    onDismiss: () -> Unit,
    onConfirmDelete: (reassignToAccountId: Long?, cascadeDelete: Boolean) -> Unit
) {
    var selectedOption by remember { mutableIntStateOf(0) } // 0 = Reassign, 1 = Cascade delete
    var selectedTargetAccountId by remember { mutableStateOf(otherAccounts.firstOrNull()?.id ?: 1L) }
    var targetDropdownExpanded by remember { mutableStateOf(false) }

    val targetAccount = otherAccounts.find { it.id == selectedTargetAccountId } ?: otherAccounts.firstOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete \"${account.nickname ?: account.name}\"?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (transactionCount > 0) {
                    Text(
                        text = "This account has $transactionCount recorded transaction${if (transactionCount != 1) "s" else ""}. How would you like to handle them?",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Option 0: Reassign
                    if (otherAccounts.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedOption == 0,
                                onClick = { selectedOption = 0 }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Reassign transactions to another account (Safe)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (selectedOption == 0) {
                            ExposedDropdownMenuBox(
                                expanded = targetDropdownExpanded,
                                onExpandedChange = { targetDropdownExpanded = !targetDropdownExpanded },
                                modifier = Modifier.padding(start = 28.dp)
                            ) {
                                OutlinedTextField(
                                    value = targetAccount?.let { it.nickname ?: it.name } ?: "Select Account",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Transfer to") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetDropdownExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                ExposedDropdownMenu(
                                    expanded = targetDropdownExpanded,
                                    onDismissRequest = { targetDropdownExpanded = false }
                                ) {
                                    otherAccounts.forEach { acc ->
                                        DropdownMenuItem(
                                            text = { Text(acc.nickname ?: acc.name) },
                                            onClick = {
                                                selectedTargetAccountId = acc.id
                                                targetDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Option 1: Cascade Delete
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedOption == 1,
                            onClick = { selectedOption = 1 }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Delete account and all $transactionCount transactions (Permanent)",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Text(
                        text = "Are you sure you want to delete this account? There are no transactions associated with it.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (transactionCount > 0 && selectedOption == 0) {
                        onConfirmDelete(selectedTargetAccountId, false)
                    } else if (transactionCount > 0 && selectedOption == 1) {
                        onConfirmDelete(null, true)
                    } else {
                        onConfirmDelete(null, false)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Account", color = MaterialTheme.colorScheme.onError)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
