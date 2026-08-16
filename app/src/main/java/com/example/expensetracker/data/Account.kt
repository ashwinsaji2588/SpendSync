package com.example.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountType {
    BANK_ACCOUNT,
    CREDIT_CARD,
    DEBIT_CARD,
    CASH,
    WALLET
}

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType = AccountType.BANK_ACCOUNT,
    val accountNumberLast4: String? = null,
    val nickname: String? = null
)
