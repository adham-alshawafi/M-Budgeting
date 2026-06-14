package com.example.data

import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val financeDao: FinanceDao) {
    // Transactions
    val allTransactions: Flow<List<TransactionEntity>> = financeDao.getAllTransactions()

    fun getTransactionsByPeriod(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> {
        return financeDao.getTransactionsByPeriod(startTime, endTime)
    }

    suspend fun insertTransaction(transaction: TransactionEntity) {
        financeDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        financeDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Int) {
        financeDao.deleteTransactionById(id)
    }

    suspend fun getAllTransactionsForSync(): List<TransactionEntity> {
        return financeDao.getAllTransactionsForSync()
    }

    suspend fun getTransactionBySyncId(syncId: String): TransactionEntity? {
        return financeDao.getTransactionBySyncId(syncId)
    }

    suspend fun insertTransactionsBulk(transactions: List<TransactionEntity>) {
        transactions.forEach { financeDao.insertTransaction(it) }
    }

    // Budgets
    fun getBudgetsForMonth(monthYear: String): Flow<List<BudgetEntity>> {
        return financeDao.getBudgetsForMonth(monthYear)
    }

    suspend fun insertBudget(budget: BudgetEntity) {
        financeDao.insertBudget(budget)
    }

    suspend fun deleteBudgetById(id: Int) {
        financeDao.deleteBudgetById(id)
    }

    suspend fun getAllBudgetsForSync(): List<BudgetEntity> {
        return financeDao.getAllBudgetsForSync()
    }

    suspend fun getBudgetBySyncId(syncId: String): BudgetEntity? {
        return financeDao.getBudgetBySyncId(syncId)
    }

    suspend fun insertBudgetsBulk(budgets: List<BudgetEntity>) {
        budgets.forEach { financeDao.insertBudget(it) }
    }

    // Notes
    val allNotes: Flow<List<FinancialNoteEntity>> = financeDao.getAllNotes()

    suspend fun insertNote(note: FinancialNoteEntity) {
        financeDao.insertNote(note)
    }

    suspend fun deleteNoteById(id: Int) {
        financeDao.deleteNoteById(id)
    }

    suspend fun getAllNotesForSync(): List<FinancialNoteEntity> {
        return financeDao.getAllNotesForSync()
    }

    suspend fun getNoteBySyncId(syncId: String): FinancialNoteEntity? {
        return financeDao.getNoteBySyncId(syncId)
    }

    suspend fun insertNotesBulk(notes: List<FinancialNoteEntity>) {
        notes.forEach { financeDao.insertNote(it) }
    }

    // Recurring Transactions
    val allRecurringTransactions: Flow<List<RecurringTransactionEntity>> = financeDao.getAllRecurringTransactions()

    suspend fun getActiveRecurringTransactionsSync(): List<RecurringTransactionEntity> {
        return financeDao.getActiveRecurringTransactionsSync()
    }

    suspend fun insertRecurringTransaction(recurring: RecurringTransactionEntity) {
        financeDao.insertRecurringTransaction(recurring)
    }

    suspend fun deleteRecurringTransactionById(id: Int) {
        financeDao.deleteRecurringTransactionById(id)
    }

    suspend fun getAllRecurringTransactionsForSync(): List<RecurringTransactionEntity> {
        return financeDao.getAllRecurringTransactionsForSync()
    }

    suspend fun getRecurringTransactionBySyncId(syncId: String): RecurringTransactionEntity? {
        return financeDao.getRecurringTransactionBySyncId(syncId)
    }

    suspend fun insertRecurringTransactionsBulk(recurrings: List<RecurringTransactionEntity>) {
        recurrings.forEach { financeDao.insertRecurringTransaction(it) }
    }
}
