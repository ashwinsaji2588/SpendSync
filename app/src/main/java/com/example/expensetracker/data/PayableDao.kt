package com.example.expensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PayableDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayable(payable: PayableEntity): Long

    @Query("SELECT * FROM payables WHERE isSettled = 0 ORDER BY timestamp DESC")
    fun getActivePayables(): Flow<List<PayableEntity>>

    @Query("SELECT * FROM payables ORDER BY timestamp DESC")
    fun getAllPayables(): Flow<List<PayableEntity>>

    @Query("SELECT SUM(amount) FROM payables WHERE isSettled = 0")
    fun getTotalOwedAmount(): Flow<Double?>

    @Query("UPDATE payables SET isSettled = :isSettled WHERE id = :id")
    suspend fun updateSettlementStatus(id: Long, isSettled: Boolean)

    @Query("DELETE FROM payables WHERE id = :id")
    suspend fun deletePayableById(id: Long)

    @Delete
    suspend fun deletePayable(payable: PayableEntity)
}
