package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FinanceRepository
    
    // Live exchange rates and currency state
    private val _selectedCurrencyCode = MutableStateFlow("USD")
    val selectedCurrencyCode: StateFlow<String> = _selectedCurrencyCode.asStateFlow()

    private val _exchangeRates = MutableStateFlow<Map<String, Double>>(emptyMap())
    val exchangeRates: StateFlow<Map<String, Double>> = _exchangeRates.asStateFlow()

    private val _isFetchingRates = MutableStateFlow(false)
    val isFetchingRates: StateFlow<Boolean> = _isFetchingRates.asStateFlow()

    private val _apiError = MutableStateFlow<String?>(null)
    val apiError: StateFlow<String?> = _apiError.asStateFlow()

    private val _lastFetchedTime = MutableStateFlow<Long>(0L)
    val lastFetchedTime: StateFlow<Long> = _lastFetchedTime.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FinanceRepository(database.financeDao())
        fetchExchangeRates()
    }

    fun fetchExchangeRates() {
        viewModelScope.launch(Dispatchers.IO) {
            _isFetchingRates.value = true
            try {
                val url = URL("https://open.er-api.com/v6/latest/USD")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                if (connection.responseCode == 200) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseText)
                    if (json.optString("result", "error") == "success") {
                        val ratesObj = json.getJSONObject("rates")
                        val newRates = mutableMapOf<String, Double>()
                        val keys = ratesObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            newRates[key] = ratesObj.getDouble(key)
                        }
                        _exchangeRates.value = newRates
                        _apiError.value = null
                        _lastFetchedTime.value = System.currentTimeMillis()
                    } else {
                        _apiError.value = "Failed to parse API rates status"
                    }
                } else {
                    _apiError.value = "HTTP error code: ${connection.responseCode}"
                }
            } catch (e: Exception) {
                _apiError.value = "Network error: ${e.localizedMessage ?: "Unknown failure"}"
                e.printStackTrace()
            } finally {
                _isFetchingRates.value = false
            }
        }
    }

    fun getExchangeRateFor(code: String): Double {
        return _exchangeRates.value[code] ?: when (code) {
            "USD" -> 1.0
            "EUR" -> 0.93
            "GBP" -> 0.79
            "JPY" -> 156.4
            "CAD" -> 1.36
            "AUD" -> 1.51
            else -> 1.0
        }
    }

    fun getCurrencySymbol(code: String): String {
        return when (code) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            "CAD" -> "C$"
            "AUD" -> "A$"
            else -> "$"
        }
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

    // Raw database transactions
    private val rawTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Transactions Flow, converted to active currency
    val allTransactions: StateFlow<List<TransactionEntity>> = combine(
        rawTransactions,
        _selectedCurrencyCode,
        _exchangeRates
    ) { transactions, currencyCode, rates ->
        val rate = rates[currencyCode] ?: getExchangeRateFor(currencyCode)
        transactions.map { it.copy(amount = it.amount * rate) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtering States
    private val _filterType = MutableStateFlow("ALL") // "ALL", "INCOME", "EXPENSE"
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    private val _filterCategory = MutableStateFlow("ALL") // "ALL" or specific category
    val filterCategory: StateFlow<String> = _filterCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(type: String, category: String) {
        _filterType.value = type
        _filterCategory.value = category
    }

    // Time period filter: Daily, Weekly, Monthly, Yearly, Custom
    private val _selectedPeriod = MutableStateFlow("Daily")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _customStartDate = MutableStateFlow(LocalDate.now().minusDays(7))
    val customStartDate: StateFlow<LocalDate> = _customStartDate.asStateFlow()

    private val _customEndDate = MutableStateFlow(LocalDate.now())
    val customEndDate: StateFlow<LocalDate> = _customEndDate.asStateFlow()

    fun setSelectedPeriod(period: String) {
        _selectedPeriod.value = period
    }

    fun setCustomDateRange(start: LocalDate, end: LocalDate) {
        _customStartDate.value = start
        _customEndDate.value = end
    }

    fun getPeriodRange(selectedDate: LocalDate, period: String, customStart: LocalDate, customEnd: LocalDate): Pair<Long, Long> {
        val startMilli: Long
        val endMilli: Long
        when (period) {
            "Daily" -> {
                startMilli = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                endMilli = selectedDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            }
            "Weekly" -> {
                val startOfWeek = selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
                startMilli = startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                endMilli = startOfWeek.plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            }
            "Monthly" -> {
                val startOfMonth = selectedDate.withDayOfMonth(1)
                startMilli = startOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                endMilli = startOfMonth.plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            }
            "Yearly" -> {
                val startOfYear = selectedDate.withDayOfYear(1)
                startMilli = startOfYear.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                endMilli = startOfYear.plusYears(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            }
            "Custom" -> {
                startMilli = customStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                endMilli = customEnd.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            }
            else -> {
                startMilli = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                endMilli = selectedDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            }
        }
        return Pair(startMilli, endMilli)
    }

    // Settings States
    private val _currencySymbol = MutableStateFlow("$")
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    fun setCurrencyCode(code: String) {
        _selectedCurrencyCode.value = code
        _currencySymbol.value = getCurrencySymbol(code)
    }

    fun setCurrencySymbol(symbol: String) {
        val code = when (symbol) {
            "$" -> "USD"
            "€" -> "EUR"
            "£" -> "GBP"
            "¥" -> "JPY"
            "C$" -> "CAD"
            "A$" -> "AUD"
            else -> symbol
        }
        setCurrencyCode(code)
    }

    private val _budgetAlertsEnabled = MutableStateFlow(true)
    val budgetAlertsEnabled: StateFlow<Boolean> = _budgetAlertsEnabled.asStateFlow()

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        _budgetAlertsEnabled.value = enabled
    }

    fun clearAllData() {
        viewModelScope.launch {
            rawTransactions.value.forEach {
                repository.deleteTransaction(it)
            }
            allNotes.value.forEach {
                repository.deleteNoteById(it.id)
            }
        }
    }

    // Combined Period Range Flow
    private val periodRangeFlow: kotlinx.coroutines.flow.Flow<Pair<Long, Long>> = combine(
        _selectedDate,
        _selectedPeriod,
        _customStartDate,
        _customEndDate
    ) { selectedDate, period, customStart, customEnd ->
        getPeriodRange(selectedDate, period, customStart, customEnd)
    }

    // Filtered Transactions for Selected Date & Period
    val dailyTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions, 
        periodRangeFlow,
        _filterType,
        _filterCategory,
        _searchQuery
    ) { transactions, range, fType, fCategory, query ->
        val startMilli = range.first
        val endMilli = range.second
        transactions.filter { t ->
            val inDateRange = t.timestamp in startMilli..endMilli
            val matchesType = fType == "ALL" || t.type == fType
            val matchesCategory = fCategory == "ALL" || t.category.equals(fCategory, ignoreCase = true)
            val matchesSearch = query.isEmpty() ||
                    t.title.contains(query, ignoreCase = true) ||
                    t.category.contains(query, ignoreCase = true)
            inDateRange && matchesType && matchesCategory && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily / Period Net Calculations
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

    // Strictly Daily Transactions for Calendar Tab
    val calendarTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions, 
        _selectedDate,
        _filterType,
        _filterCategory,
        _searchQuery
    ) { transactions, selectedDate, fType, fCategory, query ->
        val startMilli = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMilli = selectedDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        transactions.filter { t ->
            val inDateRange = t.timestamp in startMilli..endMilli
            val matchesType = fType == "ALL" || t.type == fType
            val matchesCategory = fCategory == "ALL" || t.category.equals(fCategory, ignoreCase = true)
            val matchesSearch = query.isEmpty() ||
                    t.title.contains(query, ignoreCase = true) ||
                    t.category.contains(query, ignoreCase = true)
            inDateRange && matchesType && matchesCategory && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calendar Stats
    val calendarStats: StateFlow<DailyStats> = calendarTransactions.map { list ->
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

    private val rawBudgets: StateFlow<List<BudgetEntity>> = _selectedMonthYear.flatMapLatest { monthYear ->
        repository.getBudgetsForMonth(monthYear)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyBudgets: StateFlow<List<BudgetEntity>> = combine(
        rawBudgets,
        _selectedCurrencyCode,
        _exchangeRates
    ) { budgets, currencyCode, rates ->
        val rate = rates[currencyCode] ?: getExchangeRateFor(currencyCode)
        budgets.map { it.copy(limitAmount = it.limitAmount * rate) }
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
            val rate = getExchangeRateFor(_selectedCurrencyCode.value)
            val baseAmount = amount / rate
            repository.insertTransaction(
                TransactionEntity(
                    title = title,
                    amount = baseAmount,
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
            val rate = getExchangeRateFor(_selectedCurrencyCode.value)
            val baseLimit = limitAmount / rate
            repository.insertBudget(
                BudgetEntity(
                    category = category,
                    limitAmount = baseLimit,
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
