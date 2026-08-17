package com.example.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payables")
data class PayableEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val creditorName: String,
    val amount: Double,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isSettled: Boolean = false
)
