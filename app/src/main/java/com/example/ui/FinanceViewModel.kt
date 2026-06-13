package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FinanceRepository
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = FinanceRepository(database.financeDao())
    }

    // Active Tab State
    private val _currentTab = MutableStateFlow("Daily")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    fun selectTab(tab: String) {
        _currentTab.value = tab
    }

    // Today / Selected Date State
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    // All Transactions Flow
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtering States
    private val _filterType = MutableStateFlow("ALL") // "ALL", "INCOME", "EXPENSE"
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    private val _filterCategory = MutableStateFlow("ALL") // "ALL" or specific category
    val filterCategory: StateFlow<String> = _filterCategory.asStateFlow()

    fun setFilter(type: String, category: String) {
        _filterType.value = type
        _filterCategory.value = category
    }

    // Settings States
    private val _currencySymbol = MutableStateFlow("$")
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    fun setCurrencySymbol(symbol: String) {
        _currencySymbol.value = symbol
    }

    private val _budgetAlertsEnabled = MutableStateFlow(true)
    val budgetAlertsEnabled: StateFlow<Boolean> = _budgetAlertsEnabled.asStateFlow()

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        _budgetAlertsEnabled.value = enabled
    }

    fun clearAllData() {
        viewModelScope.launch {
            // Re-fetch clean list to delete properly
            allTransactions.value.forEach {
                repository.deleteTransaction(it)
            }
            allNotes.value.forEach {
                repository.deleteNoteById(it.id)
            }
        }
    }

    // Filtered Transactions for Selected Date
    val dailyTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions, 
        _selectedDate,
        _filterType,
        _filterCategory
    ) { transactions, selectedDate, fType, fCategory ->
        val startMilli = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMilli = selectedDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        transactions.filter { t ->
            val inDateRange = t.timestamp in startMilli..endMilli
            val matchesType = fType == "ALL" || t.type == fType
            val matchesCategory = fCategory == "ALL" || t.category.equals(fCategory, ignoreCase = true)
            inDateRange && matchesType && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily Net Calculations
    val dailyStats: StateFlow<DailyStats> = dailyTransactions.map { list ->
        var income = 0.0
        var expense = 0.0
        list.forEach { t ->
            if (t.type == "INCOME") {
                income += t.amount
            } else {
                expense += t.amount
            }
        }
        DailyStats(income = income, expense = expense, net = income - expense)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyStats())

    // Budgets for Selected Month ("MM-YYYY")
    private val _selectedMonthYear = _selectedDate.map { date ->
        date.format(DateTimeFormatter.ofPattern("MM-yyyy"))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocalDate.now().format(DateTimeFormatter.ofPattern("MM-yyyy")))

    val monthlyBudgets: StateFlow<List<BudgetEntity>> = _selectedMonthYear.flatMapLatest { monthYear ->
        repository.getBudgetsForMonth(monthYear)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monthly Transactions for Charts/Totals
    val monthlyTransactions: StateFlow<List<TransactionEntity>> = combine(allTransactions, _selectedDate) { transactions, selectedDate ->
        transactions.filter {
            val tDate = java.time.Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            tDate.year == selectedDate.year && tDate.monthValue == selectedDate.monthValue
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyStats: StateFlow<DailyStats> = monthlyTransactions.map { list ->
        var income = 0.0
        var expense = 0.0
        list.forEach { t ->
            if (t.type == "INCOME") {
                income += t.amount
            } else {
                expense += t.amount
            }
        }
        DailyStats(income = income, expense = expense, net = income - expense)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyStats())

    // All Notes Flow
    val allNotes: StateFlow<List<FinancialNoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily Notes Flow
    val dailyNotes: StateFlow<List<FinancialNoteEntity>> = combine(
        allNotes,
        _selectedDate
    ) { notes, selectedDate ->
        val startMilli = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMilli = selectedDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        notes.filter { it.timestamp in startMilli..endMilli }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions
    fun addTransaction(title: String, amount: Double, type: String, category: String, notes: String, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    timestamp = timestamp,
                    notes = notes
                )
            )
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun deleteTransactionById(id: Int) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
        }
    }

    fun addBudget(category: String, limitAmount: Double) {
        viewModelScope.launch {
            repository.insertBudget(
                BudgetEntity(
                    category = category,
                    limitAmount = limitAmount,
                    monthYear = _selectedDate.value.format(DateTimeFormatter.ofPattern("MM-yyyy"))
                )
            )
        }
    }

    fun deleteBudget(id: Int) {
        viewModelScope.launch {
            repository.deleteBudgetById(id)
        }
    }

    fun addNote(title: String, content: String, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.insertNote(
                FinancialNoteEntity(
                    title = title,
                    content = content,
                    timestamp = timestamp
                )
            )
        }
    }

    fun deleteNote(id: Int) {
        viewModelScope.launch {
            repository.deleteNoteById(id)
        }
    }
}

data class DailyStats(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val net: Double = 0.0
)
