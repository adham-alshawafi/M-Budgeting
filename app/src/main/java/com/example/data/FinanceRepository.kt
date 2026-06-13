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

    // Notes
    val allNotes: Flow<List<FinancialNoteEntity>> = financeDao.getAllNotes()

    suspend fun insertNote(note: FinancialNoteEntity) {
        financeDao.insertNote(note)
    }

    suspend fun deleteNoteById(id: Int) {
        financeDao.deleteNoteById(id)
    }
}
