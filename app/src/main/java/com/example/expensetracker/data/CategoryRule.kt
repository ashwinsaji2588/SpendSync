package com.example.expensetracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "category_rules",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["targetCategoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["merchantKeyword"], unique = true),
        Index(value = ["targetCategoryId"])
    ]
)
data class CategoryRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val merchantKeyword: String,
    val targetCategoryId: Long
)
