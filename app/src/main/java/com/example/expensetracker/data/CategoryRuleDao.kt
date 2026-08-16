package com.example.expensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CategoryRule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<CategoryRule>): List<Long>

    @Query("SELECT * FROM category_rules ORDER BY id DESC")
    fun getAllRulesFlow(): Flow<List<CategoryRule>>

    @Query("SELECT * FROM category_rules")
    suspend fun getAllRules(): List<CategoryRule>

    @Query("SELECT * FROM category_rules WHERE LOWER(:merchant) LIKE '%' || LOWER(merchantKeyword) || '%' LIMIT 1")
    suspend fun findMatchingRuleForMerchant(merchant: String): CategoryRule?

    @Delete
    suspend fun deleteRule(rule: CategoryRule)

    @Query("DELETE FROM category_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)
}
