package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    // Transactions
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getTransactionsByPeriod(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET isDeleted = 1, lastModified = :lastModified WHERE id = :id")
    suspend fun softDeleteTransactionById(id: Int, lastModified: Long = System.currentTimeMillis())

    @Transaction
    suspend fun deleteTransaction(transaction: TransactionEntity) {
        softDeleteTransactionById(transaction.id)
    }

    @Transaction
    suspend fun deleteTransactionById(id: Int) {
        softDeleteTransactionById(id)
    }

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsForSync(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE syncId = :syncId LIMIT 1")
    suspend fun getTransactionBySyncId(syncId: String): TransactionEntity?

    // Budgets
    @Query("SELECT * FROM budgets WHERE monthYear = :monthYear AND isDeleted = 0")
    fun getBudgetsForMonth(monthYear: String): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Query("UPDATE budgets SET isDeleted = 1, lastModified = :lastModified WHERE id = :id")
    suspend fun softDeleteBudgetById(id: Int, lastModified: Long = System.currentTimeMillis())

    @Transaction
    suspend fun deleteBudgetById(id: Int) {
        softDeleteBudgetById(id)
    }

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgetsForSync(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE syncId = :syncId LIMIT 1")
    suspend fun getBudgetBySyncId(syncId: String): BudgetEntity?

    // Notes
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<FinancialNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: FinancialNoteEntity)

    @Query("UPDATE notes SET isDeleted = 1, lastModified = :lastModified WHERE id = :id")
    suspend fun softDeleteNoteById(id: Int, lastModified: Long = System.currentTimeMillis())

    @Transaction
    suspend fun deleteNoteById(id: Int) {
        softDeleteNoteById(id)
    }

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesForSync(): List<FinancialNoteEntity>

    @Query("SELECT * FROM notes WHERE syncId = :syncId LIMIT 1")
    suspend fun getNoteBySyncId(syncId: String): FinancialNoteEntity?

    // Recurring Transactions
    @Query("SELECT * FROM recurring_transactions WHERE isDeleted = 0 ORDER BY startDateTimestamp DESC")
    fun getAllRecurringTransactions(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND isDeleted = 0")
    suspend fun getActiveRecurringTransactionsSync(): List<RecurringTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransaction(recurring: RecurringTransactionEntity)

    @Query("UPDATE recurring_transactions SET isDeleted = 1, lastModified = :lastModified WHERE id = :id")
    suspend fun softDeleteRecurringTransactionById(id: Int, lastModified: Long = System.currentTimeMillis())

    @Transaction
    suspend fun deleteRecurringTransactionById(id: Int) {
        softDeleteRecurringTransactionById(id)
    }

    @Query("SELECT * FROM recurring_transactions")
    suspend fun getAllRecurringTransactionsForSync(): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions WHERE syncId = :syncId LIMIT 1")
    suspend fun getRecurringTransactionBySyncId(syncId: String): RecurringTransactionEntity?
}
