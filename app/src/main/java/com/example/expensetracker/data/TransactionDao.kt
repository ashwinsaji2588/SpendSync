package com.example.expensetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long>

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsWithDetails(): Flow<List<TransactionWithDetails>>

    @Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp 
        ORDER BY timestamp DESC
    """)
    fun getTransactionsWithDetailsForDateRange(
        startTimestamp: Long,
        endTimestamp: Long
    ): Flow<List<TransactionWithDetails>>

    @Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE accountId = :accountId 
        AND timestamp >= :startTimestamp AND timestamp <= :endTimestamp 
        ORDER BY timestamp DESC
    """)
    fun getTransactionsWithDetailsForAccountAndDateRange(
        accountId: Long,
        startTimestamp: Long,
        endTimestamp: Long
    ): Flow<List<TransactionWithDetails>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE isSplit = 1 AND settled = 0 ORDER BY timestamp DESC")
    fun getPendingSettlements(): Flow<List<TransactionWithDetails>>

    @Query("UPDATE transactions SET categoryId = :categoryId WHERE id = :transactionId")
    suspend fun updateTransactionCategory(transactionId: Long, categoryId: Long)

    @Query("UPDATE transactions SET settled = :settled WHERE id = :transactionId")
    suspend fun updateSettlementStatus(transactionId: Long, settled: Boolean)

    @Query("""
        UPDATE transactions 
        SET isSplit = :isSplit, reimbursementAmount = :reimbursementAmount, peerName = :peerName 
        WHERE id = :transactionId
    """)
    suspend fun updateSplitDetails(
        transactionId: Long,
        isSplit: Boolean,
        reimbursementAmount: Double,
        peerName: String?
    )

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransaction(transactionId: Long)

    @Query("UPDATE transactions SET categoryId = :targetCategoryId WHERE LOWER(merchantName) LIKE '%' || LOWER(:keyword) || '%'")
    suspend fun updateCategoryForMatchingMerchants(keyword: String, targetCategoryId: Long): Int

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}
