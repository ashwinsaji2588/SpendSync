package com.example.expensetracker.data

import androidx.room.Embedded
import androidx.room.Relation

data class TransactionWithDetails(
    @Embedded
    val transaction: TransactionEntity,

    @Relation(
        parentColumn = "accountId",
        entityColumn = "id"
    )
    val account: Account?,

    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: Category?
)
