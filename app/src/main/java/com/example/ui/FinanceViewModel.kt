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

    // Cloud Synchronization Preferences and Live States
    private val sharedPrefs = application.getSharedPreferences("sync_prefs", android.content.Context.MODE_PRIVATE)

    private val _syncEnabled = MutableStateFlow(sharedPrefs.getBoolean("sync_enabled", true))
    val syncEnabled: StateFlow<Boolean> = _syncEnabled.asStateFlow()

    private val _firebaseUrl = MutableStateFlow(sharedPrefs.getString("firebase_url", "https://mbudgeting-default-rtdb.firebaseio.com") ?: "https://mbudgeting-default-rtdb.firebaseio.com")
    val firebaseUrl: StateFlow<String> = _firebaseUrl.asStateFlow()

    private val _syncEmail = MutableStateFlow(sharedPrefs.getString("sync_email", "adhamalshawafi@gmail.com") ?: "adhamalshawafi@gmail.com")
    val syncEmail: StateFlow<String> = _syncEmail.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncedTime = MutableStateFlow(sharedPrefs.getLong("last_synced_time", 0L))
    val lastSyncedTime: StateFlow<Long> = _lastSyncedTime.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(sharedPrefs.getString("sync_error", null))
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    // Sync Services & Monitors
    private val syncService by lazy { FirebaseSyncService(application, repository) }
    private val networkMonitor = NetworkMonitor(application)

    // Device online monitor exposed to our Jetpack Compose UI
    val isDeviceOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), networkMonitor.isCurrentlyOnline())

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FinanceRepository(database.financeDao())
        fetchExchangeRates()
        processRecurringTransactions()

        // Trigger automatic replication synchronization when device regains connectivity
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online && _syncEnabled.value) {
                    syncWithWebDatabase()
                }
            }
        }
    }

    fun updateSyncEnabled(enabled: Boolean) {
        _syncEnabled.value = enabled
        sharedPrefs.edit().putBoolean("sync_enabled", enabled).apply()
        if (enabled && networkMonitor.isCurrentlyOnline()) {
            syncWithWebDatabase()
        }
    }

    fun updateFirebaseUrl(url: String) {
        _firebaseUrl.value = url
        sharedPrefs.edit().putString("firebase_url", url).apply()
    }

    fun updateSyncEmail(email: String) {
        _syncEmail.value = email
        sharedPrefs.edit().putString("sync_email", email).apply()
    }

    fun syncWithWebDatabase() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            
            val result = syncService.performFullSync(_syncEmail.value, _firebaseUrl.value)
            
            when (result) {
                is SyncResult.Success -> {
                    val now = System.currentTimeMillis()
                    _lastSyncedTime.value = now
                    sharedPrefs.edit().putLong("last_synced_time", now).putString("sync_error", null).apply()
                }
                is SyncResult.Failure -> {
                    _syncError.value = result.message
                    sharedPrefs.edit().putString("sync_error", result.message).apply()
                }
            }
            _isSyncing.value = false
        }
    }

    private fun triggerSyncIfEnabled() {
        if (_syncEnabled.value && networkMonitor.isCurrentlyOnline()) {
            syncWithWebDatabase()
        }
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

    // Editing Transaction State
    private val _editingTransaction = MutableStateFlow<com.example.data.TransactionEntity?>(null)
    val editingTransaction: StateFlow<com.example.data.TransactionEntity?> = _editingTransaction.asStateFlow()

    fun startEditingTransaction(transaction: com.example.data.TransactionEntity) {
        // Find the raw version in case it's converted by currency
        viewModelScope.launch {
            val raw = repository.getTransactionBySyncId(transaction.syncId)
            if (raw != null) {
                // Determine the amount in selected currency for display/edit matching view exchange
                val rate = getExchangeRateFor(_selectedCurrencyCode.value)
                _editingTransaction.value = raw.copy(amount = raw.amount * rate)
            } else {
                _editingTransaction.value = transaction
            }
        }
    }

    fun stopEditingTransaction() {
        _editingTransaction.value = null
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

    // Dynamic Categories Configuration & Management
    private val defaultExpenseCategories = listOf("Food", "Shopping", "Transport", "Utilities", "Entertainment", "Health", "Education", "Other")
    private val defaultIncomeCategories = listOf("Salary", "Bonus", "Investments", "Refund", "Gifts", "Other")

    private val _expenseCategories = MutableStateFlow(loadCategories("expense_categories", defaultExpenseCategories))
    val expenseCategories: StateFlow<List<String>> = _expenseCategories.asStateFlow()

    private val _incomeCategories = MutableStateFlow(loadCategories("income_categories", defaultIncomeCategories))
    val incomeCategories: StateFlow<List<String>> = _incomeCategories.asStateFlow()

    private fun loadCategories(key: String, defaults: List<String>): List<String> {
        val str = sharedPrefs.getString(key, null) ?: return defaults
        return if (str.isEmpty()) emptyList() else str.split(",")
    }

    private fun saveCategories(key: String, list: List<String>) {
        sharedPrefs.edit().putString(key, list.joinToString(",")).apply()
    }

    fun addExpenseCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty() && trimmed !in _expenseCategories.value) {
            val newList = _expenseCategories.value + trimmed
            _expenseCategories.value = newList
            saveCategories("expense_categories", newList)
        }
    }

    fun deleteExpenseCategory(name: String) {
        val newList = _expenseCategories.value.filter { it != name }
        _expenseCategories.value = newList
        saveCategories("expense_categories", newList)
    }

    fun renameExpenseCategory(oldName: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isNotEmpty() && oldName in _expenseCategories.value && trimmed !in _expenseCategories.value) {
            val newList = _expenseCategories.value.map { if (it == oldName) trimmed else it }
            _expenseCategories.value = newList
            saveCategories("expense_categories", newList)
        }
    }

    fun addIncomeCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty() && trimmed !in _incomeCategories.value) {
            val newList = _incomeCategories.value + trimmed
            _incomeCategories.value = newList
            saveCategories("income_categories", newList)
        }
    }

    fun deleteIncomeCategory(name: String) {
        val newList = _incomeCategories.value.filter { it != name }
        _incomeCategories.value = newList
        saveCategories("income_categories", newList)
    }

    fun renameIncomeCategory(oldName: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isNotEmpty() && oldName in _incomeCategories.value && trimmed !in _incomeCategories.value) {
            val newList = _incomeCategories.value.map { if (it == oldName) trimmed else it }
            _incomeCategories.value = newList
            saveCategories("income_categories", newList)
        }
    }

    // Dynamic Accounts Configuration & Management
    private val defaultAccounts = listOf("Cash", "Saving", "Credit")

    private val _accounts = MutableStateFlow(loadCategories("accounts_list", defaultAccounts))
    val accounts: StateFlow<List<String>> = _accounts.asStateFlow()

    fun addAccount(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty() && trimmed !in _accounts.value) {
            val newList = _accounts.value + trimmed
            _accounts.value = newList
            saveCategories("accounts_list", newList)
        }
    }

    fun deleteAccount(name: String) {
        val newList = _accounts.value.filter { it != name }
        _accounts.value = newList
        saveCategories("accounts_list", newList)
    }

    fun renameAccount(oldName: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isNotEmpty() && oldName in _accounts.value && trimmed !in _accounts.value) {
            val newList = _accounts.value.map { if (it == oldName) trimmed else it }
            _accounts.value = newList
            saveCategories("accounts_list", newList)
        }
    }

    // Dynamic Account Balances Map
    val dynamicAccountBalances: StateFlow<Map<String, Double>> = combine(allTransactions, accounts) { list, accList ->
        val balancesMap = accList.associateWith { 0.0 }.toMutableMap()
        list.filter { !it.isDeleted }.forEach { t ->
            val amt = t.amount
            when (t.type) {
                "INCOME" -> {
                    val current = balancesMap[t.account] ?: 0.0
                    balancesMap[t.account] = current + amt
                }
                "EXPENSE" -> {
                    val current = balancesMap[t.account] ?: 0.0
                    balancesMap[t.account] = current - amt
                }
                "TRANSFER" -> {
                    // Subtract from source
                    val currentSrc = balancesMap[t.account] ?: 0.0
                    balancesMap[t.account] = currentSrc - amt
                    // Add to destination
                    if (t.toAccount != null) {
                        val currentDst = balancesMap[t.toAccount] ?: 0.0
                        balancesMap[t.toAccount] = currentDst + amt
                    }
                }
            }
        }
        balancesMap
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Account Balances State Flow - For backward compatibility and existing hardcoded panels
    val accountBalances: StateFlow<AccountBalances> = dynamicAccountBalances.map { map ->
        AccountBalances(
            cash = map["Cash"] ?: 0.0,
            credit = map["Credit"] ?: 0.0,
            saving = map["Saving"] ?: 0.0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountBalances())

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

    private val _overallMonthlyBudget = MutableStateFlow(sharedPrefs.getFloat("overall_monthly_budget", 0.0f).toDouble())
    val overallMonthlyBudget: StateFlow<Double> = _overallMonthlyBudget.asStateFlow()

    fun updateOverallMonthlyBudget(budget: Double) {
        _overallMonthlyBudget.value = budget
        sharedPrefs.edit().putFloat("overall_monthly_budget", budget.toFloat()).apply()
    }

    // Redesigned Settings States
    // 1. Profile
    private val _profileName = MutableStateFlow(sharedPrefs.getString("profile_name", "Adham Alshawafi") ?: "Adham Alshawafi")
    val profileName: StateFlow<String> = _profileName.asStateFlow()
    fun setProfileName(name: String) {
        _profileName.value = name
        sharedPrefs.edit().putString("profile_name", name).apply()
    }

    private val _profileEmail = MutableStateFlow(sharedPrefs.getString("profile_email", "adhamalshawafi@gmail.com") ?: "adhamalshawafi@gmail.com")
    val profileEmail: StateFlow<String> = _profileEmail.asStateFlow()
    fun setProfileEmail(email: String) {
        _profileEmail.value = email
        sharedPrefs.edit().putString("profile_email", email).apply()
    }

    // 2. Currency & Region
    private val _numberFormat = MutableStateFlow(sharedPrefs.getString("number_format", "Standard (1,234.56)") ?: "Standard (1,234.56)")
    val numberFormat: StateFlow<String> = _numberFormat.asStateFlow()
    fun setNumberFormat(format: String) {
        _numberFormat.value = format
        sharedPrefs.edit().putString("number_format", format).apply()
    }

    private val _firstDayOfWeek = MutableStateFlow(sharedPrefs.getString("first_day_of_week", "Sunday") ?: "Sunday")
    val firstDayOfWeek: StateFlow<String> = _firstDayOfWeek.asStateFlow()
    fun setFirstDayOfWeek(day: String) {
        _firstDayOfWeek.value = day
        sharedPrefs.edit().putString("first_day_of_week", day).apply()
    }

    // 3. Budget Defaults
    private val _budgetCycle = MutableStateFlow(sharedPrefs.getString("budget_cycle", "Monthly") ?: "Monthly")
    val budgetCycle: StateFlow<String> = _budgetCycle.asStateFlow()
    fun setBudgetCycle(cycle: String) {
        _budgetCycle.value = cycle
        sharedPrefs.edit().putString("budget_cycle", cycle).apply()
    }

    private val _rolloverEnabled = MutableStateFlow(sharedPrefs.getBoolean("budget_rollover_enabled", false))
    val rolloverEnabled: StateFlow<Boolean> = _rolloverEnabled.asStateFlow()
    fun setRolloverEnabled(enabled: Boolean) {
        _rolloverEnabled.value = enabled
        sharedPrefs.edit().putBoolean("budget_rollover_enabled", enabled).apply()
    }

    private val _overspendingThreshold = MutableStateFlow(sharedPrefs.getFloat("overspending_threshold", 80.0f).toDouble())
    val overspendingThreshold: StateFlow<Double> = _overspendingThreshold.asStateFlow()
    fun setOverspendingThreshold(value: Double) {
        _overspendingThreshold.value = value
        sharedPrefs.edit().putFloat("overspending_threshold", value.toFloat()).apply()
    }

    // 4. Notifications
    private val _billRemindersEnabled = MutableStateFlow(sharedPrefs.getBoolean("bill_reminders_enabled", true))
    val billRemindersEnabled: StateFlow<Boolean> = _billRemindersEnabled.asStateFlow()
    fun setBillRemindersEnabled(enabled: Boolean) {
        _billRemindersEnabled.value = enabled
        sharedPrefs.edit().putBoolean("bill_reminders_enabled", enabled).apply()
    }

    private val _dailyRecapEnabled = MutableStateFlow(sharedPrefs.getBoolean("daily_recap_enabled", false))
    val dailyRecapEnabled: StateFlow<Boolean> = _dailyRecapEnabled.asStateFlow()
    fun setDailyRecapEnabled(enabled: Boolean) {
        _dailyRecapEnabled.value = enabled
        sharedPrefs.edit().putBoolean("daily_recap_enabled", enabled).apply()
    }

    private val _lowBalanceAlertsEnabled = MutableStateFlow(sharedPrefs.getBoolean("low_balance_alerts_enabled", true))
    val lowBalanceAlertsEnabled: StateFlow<Boolean> = _lowBalanceAlertsEnabled.asStateFlow()
    fun setLowBalanceAlertsEnabled(enabled: Boolean) {
        _lowBalanceAlertsEnabled.value = enabled
        sharedPrefs.edit().putBoolean("low_balance_alerts_enabled", enabled).apply()
    }

    private val _paymentConfirmationsEnabled = MutableStateFlow(sharedPrefs.getBoolean("payment_confirmations_enabled", true))
    val paymentConfirmationsEnabled: StateFlow<Boolean> = _paymentConfirmationsEnabled.asStateFlow()
    fun setPaymentConfirmationsEnabled(enabled: Boolean) {
        _paymentConfirmationsEnabled.value = enabled
        sharedPrefs.edit().putBoolean("payment_confirmations_enabled", enabled).apply()
    }

    // 5. Security
    private val _appLockEnabled = MutableStateFlow(sharedPrefs.getBoolean("app_lock_enabled", false))
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled.asStateFlow()
    fun setAppLockEnabled(enabled: Boolean) {
        _appLockEnabled.value = enabled
        sharedPrefs.edit().putBoolean("app_lock_enabled", enabled).apply()
    }

    private val _privacyModeEnabled = MutableStateFlow(sharedPrefs.getBoolean("privacy_mode_enabled", false))
    val privacyModeEnabled: StateFlow<Boolean> = _privacyModeEnabled.asStateFlow()
    fun setPrivacyModeEnabled(enabled: Boolean) {
        _privacyModeEnabled.value = enabled
        sharedPrefs.edit().putBoolean("privacy_mode_enabled", enabled).apply()
    }

    // 5.5. Transactions Time format
    private val _showTransactionTime = MutableStateFlow(sharedPrefs.getBoolean("show_transaction_time", true))
    val showTransactionTime: StateFlow<Boolean> = _showTransactionTime.asStateFlow()
    fun setShowTransactionTime(enabled: Boolean) {
        _showTransactionTime.value = enabled
        sharedPrefs.edit().putBoolean("show_transaction_time", enabled).apply()
    }

    // 5.6. Tools order (reorder section list)
    private val _toolsOrder = MutableStateFlow(sharedPrefs.getString("tools_order", "CALCULATOR,WORLD_CLOCK,DATE_CALC,GRAM_SCALE,PREFERENCES") ?: "CALCULATOR,WORLD_CLOCK,DATE_CALC,GRAM_SCALE,PREFERENCES")
    val toolsOrder: StateFlow<String> = _toolsOrder.asStateFlow()
    fun setToolsOrder(order: String) {
        _toolsOrder.value = order
        sharedPrefs.edit().putString("tools_order", order).apply()
    }

    // 6. Appearance
    private val _appThemeSettings = MutableStateFlow(sharedPrefs.getString("app_theme_settings", "Dark") ?: "Dark")
    val appThemeSettings: StateFlow<String> = _appThemeSettings.asStateFlow()
    fun setAppThemeSettings(theme: String) {
        _appThemeSettings.value = theme
        sharedPrefs.edit().putString("app_theme_settings", theme).apply()
    }

    private val _accentColorIndex = MutableStateFlow(sharedPrefs.getInt("accent_color_index", 0))
    val accentColorIndex: StateFlow<Int> = _accentColorIndex.asStateFlow()
    fun setAccentColorIndex(index: Int) {
        _accentColorIndex.value = index
        sharedPrefs.edit().putInt("accent_color_index", index).apply()
    }

    // 7. Emergency Fund Target
    private val _emergencyTarget = MutableStateFlow(sharedPrefs.getFloat("emergency_target", 10000.0f).toDouble())
    val emergencyTarget: StateFlow<Double> = _emergencyTarget.asStateFlow()
    fun setEmergencyTarget(value: Double) {
        _emergencyTarget.value = value
        sharedPrefs.edit().putFloat("emergency_target", value.toFloat()).apply()
    }

    private val _emergencyCurrent = MutableStateFlow(sharedPrefs.getFloat("emergency_current", 2500.0f).toDouble())
    val emergencyCurrent: StateFlow<Double> = _emergencyCurrent.asStateFlow()
    fun setEmergencyCurrent(value: Double) {
        _emergencyCurrent.value = value
        sharedPrefs.edit().putFloat("emergency_current", value.toFloat()).apply()
    }

    private val _emergencyTargetDate = MutableStateFlow(sharedPrefs.getString("emergency_target_date", "2026-12-31") ?: "2026-12-31")
    val emergencyTargetDate: StateFlow<String> = _emergencyTargetDate.asStateFlow()
    fun setEmergencyTargetDate(dateStr: String) {
        _emergencyTargetDate.value = dateStr
        sharedPrefs.edit().putString("emergency_target_date", dateStr).apply()
    }

    // 8. Spending Freeze Mode
    private val _spendingFreezeEnabled = MutableStateFlow(sharedPrefs.getBoolean("spending_freeze_enabled", false))
    val spendingFreezeEnabled: StateFlow<Boolean> = _spendingFreezeEnabled.asStateFlow()
    fun setSpendingFreezeEnabled(enabled: Boolean) {
        _spendingFreezeEnabled.value = enabled
        sharedPrefs.edit().putBoolean("spending_freeze_enabled", enabled).apply()
    }

    // 9. Categorization Rules
    private val _categorizationRules = MutableStateFlow<Map<String, String>>(loadCategorizationRules())
    val categorizationRules: StateFlow<Map<String, String>> = _categorizationRules.asStateFlow()

    private fun loadCategorizationRules(): Map<String, String> {
        val raw = sharedPrefs.getString("categorization_rules", "Starbucks:Food,Uber:Transport,Netflix:Entertainment,Rent:Rent") ?: "Starbucks:Food,Uber:Transport,Netflix:Entertainment,Rent:Rent"
        if (raw.trim().isEmpty()) return emptyMap()
        return raw.split(",").mapNotNull {
            val parts = it.split(":")
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()
    }

    private fun saveCategorizationRules(rules: Map<String, String>) {
        val serialized = rules.entries.joinToString(",") { "${it.key}:${it.value}" }
        sharedPrefs.edit().putString("categorization_rules", serialized).apply()
    }

    fun addCategorizationRule(keyword: String, category: String) {
        val map = _categorizationRules.value.toMutableMap()
        map[keyword] = category
        _categorizationRules.value = map
        saveCategorizationRules(map)
    }

    fun deleteCategorizationRule(keyword: String) {
        val map = _categorizationRules.value.toMutableMap()
        map.remove(keyword)
        _categorizationRules.value = map
        saveCategorizationRules(map)
    }

    // Suggested category engine helper
    fun suggestCategory(title: String): String? {
        val normalizedTitle = title.trim().lowercase()
        _categorizationRules.value.forEach { (keyword, cat) ->
            if (normalizedTitle.contains(keyword.lowercase())) {
                return cat
            }
        }
        return null
    }

    // 10. Feedback suggestions & upvotes
    private val _votedFeedbacks = MutableStateFlow(sharedPrefs.getStringSet("voted_feedbacks", emptySet()) ?: emptySet())
    val votedFeedbacks: StateFlow<Set<String>> = _votedFeedbacks.asStateFlow()

    private val _feedbacksList = MutableStateFlow<List<FeedbackSuggestion>>(loadFeedbackList())
    val feedbacksList: StateFlow<List<FeedbackSuggestion>> = _feedbacksList.asStateFlow()

    private fun loadFeedbackList(): List<FeedbackSuggestion> {
        val raw = sharedPrefs.getString("feedback_list", null)
        if (raw == null) {
            return listOf(
                FeedbackSuggestion("1", "Detailed PDF Reports", "Allow downloading beautiful visual charts as PDF.", 15),
                FeedbackSuggestion("2", "Splitwise Integration", "Import shared expenses automatically from Splitwise.", 9),
                FeedbackSuggestion("3", "AI Budget Advisor", "Receive personalized tips based on quarterly spending trends.", 24),
                FeedbackSuggestion("4", "Cryptocurrency wallets", "Check balance of Bitcoin/Ethereum automatically.", 5)
            )
        }
        return try {
            raw.split("|||").mapNotNull {
                val parts = it.split(":::")
                if (parts.size == 4) {
                    FeedbackSuggestion(parts[0], parts[1], parts[2], parts[3].toIntOrNull() ?: 0)
                } else null
            }
        } catch(e: Exception) {
            emptyList()
        }
    }

    private fun saveFeedbackList(list: List<FeedbackSuggestion>) {
        val serialized = list.joinToString("|||") { "${it.id}:::${it.title}:::${it.description}:::${it.votes}" }
        sharedPrefs.edit().putString("feedback_list", serialized).apply()
    }

    fun upvoteFeedback(id: String) {
        val voted = _votedFeedbacks.value.toMutableSet()
        val list = _feedbacksList.value.map {
            if (it.id == id) {
                if (id in voted) {
                    voted.remove(id)
                    it.copy(votes = it.votes - 1)
                } else {
                    voted.add(id)
                    it.copy(votes = it.votes + 1)
                }
            } else it
        }
        _votedFeedbacks.value = voted
        sharedPrefs.edit().putStringSet("voted_feedbacks", voted).apply()
        _feedbacksList.value = list
        saveFeedbackList(list)
    }

    fun addFeedback(title: String, description: String) {
        val id = java.util.UUID.randomUUID().toString()
        val item = FeedbackSuggestion(id, title, description, 1)
        val newList = _feedbacksList.value + item
        _feedbacksList.value = newList
        saveFeedbackList(newList)
        
        val voted = _votedFeedbacks.value.toMutableSet()
        voted.add(id)
        _votedFeedbacks.value = voted
        sharedPrefs.edit().putStringSet("voted_feedbacks", voted).apply()
    }

    fun resetCategoriesToDefault() {
        _expenseCategories.value = defaultExpenseCategories
        saveCategories("expense_categories", defaultExpenseCategories)
        _incomeCategories.value = defaultIncomeCategories
        saveCategories("income_categories", defaultIncomeCategories)
    }

    fun resetBudgets() {
        viewModelScope.launch {
            repository.getAllBudgetsForSync().forEach {
                repository.deleteBudgetById(it.id)
            }
            updateOverallMonthlyBudget(0.0)
            _emergencyTarget.value = 10000.0
            _emergencyCurrent.value = 2500.0
            sharedPrefs.edit().putFloat("emergency_target", 10000.0f).putFloat("emergency_current", 2500.0f).apply()
        }
    }

    fun resetToFactoryDefault() {
        viewModelScope.launch {
            clearAllData()
            sharedPrefs.edit().clear().apply()
            
            _profileName.value = "Adham Alshawafi"
            _profileEmail.value = "adhamalshawafi@gmail.com"
            _numberFormat.value = "Standard (1,234.56)"
            _firstDayOfWeek.value = "Sunday"
            _budgetCycle.value = "Monthly"
            _rolloverEnabled.value = false
            _overspendingThreshold.value = 80.0
            _billRemindersEnabled.value = true
            _dailyRecapEnabled.value = false
            _lowBalanceAlertsEnabled.value = true
            _paymentConfirmationsEnabled.value = true
            _appLockEnabled.value = false
            _privacyModeEnabled.value = false
            _showTransactionTime.value = true
            _toolsOrder.value = "CALCULATOR,WORLD_CLOCK,DATE_CALC,GRAM_SCALE,PREFERENCES"
            _appThemeSettings.value = "Dark"
            _accentColorIndex.value = 0
            _emergencyTarget.value = 10000.0
            _emergencyCurrent.value = 2500.0
            _emergencyTargetDate.value = "2026-12-31"
            _spendingFreezeEnabled.value = false
            _categorizationRules.value = loadCategorizationRules()
            _votedFeedbacks.value = emptySet()
            _feedbacksList.value = loadFeedbackList()
            
            resetCategoriesToDefault()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            rawTransactions.value.forEach {
                repository.deleteTransaction(it)
            }
            allNotes.value.forEach {
                repository.deleteNoteById(it.id)
            }
            repository.getAllBudgetsForSync().forEach {
                repository.deleteBudgetById(it.id)
            }
            repository.getAllRecurringTransactionsForSync().forEach {
                repository.deleteRecurringTransactionById(it.id)
            }
            updateOverallMonthlyBudget(0.0)
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
            } else if (t.type == "EXPENSE") {
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
            } else if (t.type == "EXPENSE") {
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
            } else if (t.type == "EXPENSE") {
                expense += t.amount
            }
        }
        DailyStats(income = income, expense = expense, net = income - expense)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyStats())

    val budgetAlerts: StateFlow<List<BudgetAlert>> = combine(
        monthlyBudgets,
        monthlyTransactions,
        budgetAlertsEnabled
    ) { budgets, transactions, alertsEnabled ->
        if (!alertsEnabled) return@combine emptyList()

        val categorySpending = mutableMapOf<String, Double>()
        transactions.filter { it.type == "EXPENSE" }.forEach { t ->
            categorySpending[t.category] = (categorySpending[t.category] ?: 0.0) + t.amount
        }

        budgets.mapNotNull { budget ->
            val spent = categorySpending[budget.category] ?: 0.0
            val thresholdAmount = budget.limitAmount * (budget.alertThreshold / 100.0)
            if (spent >= thresholdAmount) {
                val actualPercent = if (budget.limitAmount > 0) (spent / budget.limitAmount) * 100.0 else 0.0
                BudgetAlert(
                    category = budget.category,
                    limitAmount = budget.limitAmount,
                    spentAmount = spent,
                    thresholdPercent = budget.alertThreshold,
                    actualPercent = actualPercent,
                    isBreached = spent > budget.limitAmount
                )
            } else {
                null
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    fun addTransaction(title: String, amount: Double, type: String, category: String, notes: String, account: String = "Cash", toAccount: String? = null, timestamp: Long = System.currentTimeMillis()) {
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
                    notes = notes,
                    account = account,
                    toAccount = toAccount
                )
            )
            triggerSyncIfEnabled()
        }
    }

    fun updateTransaction(id: Int, title: String, amount: Double, type: String, category: String, notes: String, account: String = "Cash", toAccount: String? = null, timestamp: Long = System.currentTimeMillis(), syncId: String) {
        viewModelScope.launch {
            val rate = getExchangeRateFor(_selectedCurrencyCode.value)
            val baseAmount = amount / rate
            repository.insertTransaction(
                TransactionEntity(
                    id = id,
                    title = title,
                    amount = baseAmount,
                    type = type,
                    category = category,
                    timestamp = timestamp,
                    notes = notes,
                    account = account,
                    toAccount = toAccount,
                    syncId = syncId,
                    lastModified = System.currentTimeMillis()
                )
            )
            triggerSyncIfEnabled()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            triggerSyncIfEnabled()
        }
    }

    fun deleteTransactionById(id: Int) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
            triggerSyncIfEnabled()
        }
    }

    fun addBudget(category: String, limitAmount: Double, alertThreshold: Double = 80.0) {
        viewModelScope.launch {
            val rate = getExchangeRateFor(_selectedCurrencyCode.value)
            val baseLimit = limitAmount / rate
            repository.insertBudget(
                BudgetEntity(
                    category = category,
                    limitAmount = baseLimit,
                    monthYear = _selectedDate.value.format(DateTimeFormatter.ofPattern("MM-yyyy")),
                    alertThreshold = alertThreshold
                )
            )
            triggerSyncIfEnabled()
        }
    }

    fun deleteBudget(id: Int) {
        viewModelScope.launch {
            repository.deleteBudgetById(id)
            triggerSyncIfEnabled()
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
            triggerSyncIfEnabled()
        }
    }

    fun deleteNote(id: Int) {
        viewModelScope.launch {
            repository.deleteNoteById(id)
            triggerSyncIfEnabled()
        }
    }

    // Recurring Transactions States & Actions
    val allRecurringTransactions: StateFlow<List<RecurringTransactionEntity>> = repository.allRecurringTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun processRecurringTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            val activeRecurring = repository.getActiveRecurringTransactionsSync()
            val now = System.currentTimeMillis()
            
            for (curr in activeRecurring) {
                var tempNext = curr.nextTriggerTimestamp
                var lastTriggered = curr.lastTriggeredTimestamp
                val insertedTransactions = mutableListOf<TransactionEntity>()
                var count = 0
                
                while (tempNext <= now && count < 100) {
                    // Create standard transaction
                    insertedTransactions.add(
                        TransactionEntity(
                            title = curr.title,
                            amount = curr.amount, // already in base currency (USD)
                            type = curr.type,
                            category = curr.category,
                            timestamp = tempNext,
                            notes = if (curr.notes.isNotEmpty()) "[Recurring] ${curr.notes}" else "[Recurring] Monthly/Weekly payment"
                        )
                    )
                    
                    // Update trigger timing
                    lastTriggered = tempNext
                    
                    val oldNextDate = java.time.Instant.ofEpochMilli(tempNext)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                        
                    val newNextDate = when (curr.frequency) {
                        "Daily" -> oldNextDate.plusDays(1)
                        "Weekly" -> oldNextDate.plusWeeks(1)
                        "Monthly" -> oldNextDate.plusMonths(1)
                        "Yearly" -> oldNextDate.plusYears(1)
                        else -> oldNextDate.plusMonths(1)
                    }
                    
                    tempNext = newNextDate.atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                        
                    count++
                }
                
                if (insertedTransactions.isNotEmpty()) {
                    // Apply database updates
                    for (t in insertedTransactions) {
                        repository.insertTransaction(t)
                    }
                    
                    // Update recurring transaction in the DB with updated timestamps
                    val updatedRecurring = curr.copy(
                        lastTriggeredTimestamp = lastTriggered,
                        nextTriggerTimestamp = tempNext
                    )
                    repository.insertRecurringTransaction(updatedRecurring)
                }
            }
        }
    }

    fun addRecurringTransaction(
        title: String,
        amount: Double,
        type: String,
        category: String,
        frequency: String,
        startDate: java.time.LocalDate,
        notes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val rate = getExchangeRateFor(_selectedCurrencyCode.value)
            val baseAmount = amount / rate
            
            val startMilli = startDate.atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            
            val recurring = RecurringTransactionEntity(
                title = title,
                amount = baseAmount,
                type = type,
                category = category,
                frequency = frequency,
                startDateTimestamp = startMilli,
                lastTriggeredTimestamp = 0L,
                nextTriggerTimestamp = startMilli,
                notes = notes,
                isActive = true
            )
            
            repository.insertRecurringTransaction(recurring)
            processRecurringTransactions()
            triggerSyncIfEnabled()
        }
    }

    fun deleteRecurringTransaction(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRecurringTransactionById(id)
            triggerSyncIfEnabled()
        }
    }

    fun toggleRecurringTransactionActive(id: Int, isActive: Boolean, item: RecurringTransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = item.copy(isActive = isActive)
            repository.insertRecurringTransaction(updated)
            if (isActive) {
                processRecurringTransactions()
            }
            triggerSyncIfEnabled()
        }
    }
}

data class DailyStats(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val net: Double = 0.0
)

data class AccountBalances(
    val cash: Double = 0.0,
    val credit: Double = 0.0,
    val saving: Double = 0.0
)

data class BudgetAlert(
    val category: String,
    val limitAmount: Double,
    val spentAmount: Double,
    val thresholdPercent: Double,
    val actualPercent: Double,
    val isBreached: Boolean
)

data class FeedbackSuggestion(
    val id: String,
    val title: String,
    val description: String,
    val votes: Int
)
