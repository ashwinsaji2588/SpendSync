package com.example.expensetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccount(account: Account): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccounts(accounts: List<Account>): List<Long>

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    suspend fun getAllAccountsDirect(): List<Account>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): Account?

    @Query("SELECT * FROM accounts WHERE name = :name LIMIT 1")
    suspend fun getAccountByName(name: String): Account?

    @Query("SELECT * FROM accounts WHERE accountNumberLast4 = :last4 LIMIT 1")
    suspend fun getAccountByLast4(last4: String): Account?

    @Query("SELECT accountNumberLast4 FROM accounts WHERE accountNumberLast4 IS NOT NULL")
    suspend fun getAllAccountLast4(): List<String>

    @Query("SELECT id FROM accounts LIMIT 1")
    suspend fun getDefaultAccountId(): Long?

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getCount(): Int

    @Query("UPDATE accounts SET nickname = :nickname WHERE id = :accountId")
    suspend fun updateAccountNickname(accountId: Long, nickname: String?)

    @Query("UPDATE accounts SET type = :type, name = :name WHERE id = :accountId")
    suspend fun updateAccountTypeAndName(accountId: Long, type: AccountType, name: String)

    @Query("""
        UPDATE accounts 
        SET name = :name, type = :type, nickname = :nickname, accountNumberLast4 = :last4 
        WHERE id = :accountId
    """)
    suspend fun updateAccountDetails(
        accountId: Long,
        name: String,
        type: AccountType,
        nickname: String?,
        last4: String?
    )

    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun deleteAccountById(accountId: Long)

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId")
    suspend fun getTransactionCountForAccount(accountId: Long): Int

    @Query("UPDATE transactions SET accountId = :targetAccountId WHERE accountId = :fromAccountId")
    suspend fun reassignTransactions(fromAccountId: Long, targetAccountId: Long)

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteTransactionsByAccount(accountId: Long)
}
