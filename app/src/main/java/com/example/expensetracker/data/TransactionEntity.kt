package com.example.expensetracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["timestamp", "amount", "merchantName", "accountId"], unique = true),
        Index(value = ["accountId"]),
        Index(value = ["categoryId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val merchantName: String,
    val timestamp: Long,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val accountId: Long,
    val categoryId: Long,
    val notes: String? = null
)

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return runCatching { TransactionType.valueOf(value) }.getOrDefault(TransactionType.EXPENSE)
    }

    @TypeConverter
    fun fromAccountType(type: AccountType): String = type.name

    @TypeConverter
    fun toAccountType(value: String): AccountType {
        return runCatching { AccountType.valueOf(value) }.getOrDefault(AccountType.BANK_ACCOUNT)
    }
}
