package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val type: String, // "INCOME", "EXPENSE", or "TRANSFER"
    val category: String,
    val timestamp: Long,
    val notes: String = "",
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val isDeleted: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
    val account: String = "Cash", // "Cash", "Credit", "Saving"
    val toAccount: String? = null // For TRANSFER type
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val limitAmount: Double,
    val monthYear: String, // "MM-YYYY" (e.g. "06-2026")
    val alertThreshold: Double = 80.0, // Default warning threshold representation in percentage (e.g. 80.0 means 80%)
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val isDeleted: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class FinancialNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val timestamp: Long,
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val isDeleted: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
)

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val type: String, // "INCOME" or "EXPENSE"
    val category: String,
    val frequency: String, // "Daily", "Weekly", "Monthly", "Yearly"
    val startDateTimestamp: Long,
    val lastTriggeredTimestamp: Long,
    val nextTriggerTimestamp: Long,
    val isActive: Boolean = true,
    val notes: String = "",
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val isDeleted: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
)
