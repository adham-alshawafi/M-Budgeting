package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: FinanceViewModel = viewModel()
        val currentTab by viewModel.currentTab.collectAsState()

        // Dialog trigger states
        var showAddTransaction by remember { mutableStateOf(false) }
        var showAddBudget by remember { mutableStateOf(false) }
        var showAddNote by remember { mutableStateOf(false) }
        var showFilterDialog by remember { mutableStateOf(false) }
        var showSettingsDialog by remember { mutableStateOf(false) }

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          containerColor = DarkBg,
          bottomBar = {
            FinanceNavigationBar(
              currentTab = currentTab,
              onTabSelected = { tab -> viewModel.selectTab(tab) }
            )
          }
        ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(DarkBg)
              .padding(innerPadding)
          ) {
            // View Switcher based on standard state branching
            when (currentTab) {
              "Daily" -> DailyTabScreen(
                viewModel = viewModel,
                onShowAddTransaction = { showAddTransaction = true },
                onShowFilter = { showFilterDialog = true },
                onShowSettings = { showSettingsDialog = true }
              )
              "Calendar" -> CalendarTabScreen(
                viewModel = viewModel,
                onShowAddTransaction = { showAddTransaction = true }
              )
              "Monthly" -> MonthlyTabScreen(
                viewModel = viewModel
              )
              "Budget" -> BudgetTabScreen(
                viewModel = viewModel,
                onShowAddBudget = { showAddBudget = true }
              )
              "Notes" -> NotesTabScreen(
                viewModel = viewModel,
                onShowAddNote = { showAddNote = true }
              )
            }

            // Dialogs
            if (showAddTransaction) {
              AddTransactionDialog(
                viewModel = viewModel,
                onDismiss = { showAddTransaction = false },
                onSave = { title, amount, type, category, notes, account, toAccount, timestamp ->
                  viewModel.addTransaction(title, amount, type, category, notes, account, toAccount, timestamp)
                }
              )
            }

            val editingTransaction by viewModel.editingTransaction.collectAsState()
            if (editingTransaction != null) {
              AddTransactionDialog(
                viewModel = viewModel,
                editingTransaction = editingTransaction,
                onDismiss = { viewModel.stopEditingTransaction() },
                onSave = { title, amount, type, category, notes, account, toAccount, timestamp ->
                  viewModel.updateTransaction(
                    id = editingTransaction!!.id,
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    notes = notes,
                    account = account,
                    toAccount = toAccount,
                    timestamp = timestamp,
                    syncId = editingTransaction!!.syncId
                  )
                  viewModel.stopEditingTransaction()
                }
              )
            }

            if (showFilterDialog) {
              val filterType by viewModel.filterType.collectAsState()
              val filterCategory by viewModel.filterCategory.collectAsState()
              FilterTransactionsDialog(
                currentType = filterType,
                currentCategory = filterCategory,
                onDismiss = { showFilterDialog = false },
                onApply = { type, category ->
                  viewModel.setFilter(type, category)
                }
              )
            }

            if (showSettingsDialog) {
              val currencySymbol by viewModel.currencySymbol.collectAsState()
              val currentCurrencyCode by viewModel.selectedCurrencyCode.collectAsState()
              val isFetchingRates by viewModel.isFetchingRates.collectAsState()
              val apiError by viewModel.apiError.collectAsState()
              val lastFetchedTime by viewModel.lastFetchedTime.collectAsState()
              val budgetAlertsEnabled by viewModel.budgetAlertsEnabled.collectAsState()
              val syncEnabled by viewModel.syncEnabled.collectAsState()
              val firebaseUrl by viewModel.firebaseUrl.collectAsState()
              val syncEmail by viewModel.syncEmail.collectAsState()
              val isSyncing by viewModel.isSyncing.collectAsState()
              val lastSyncedTime by viewModel.lastSyncedTime.collectAsState()
              val syncError by viewModel.syncError.collectAsState()

              SettingsDialog(
                viewModel = viewModel,
                currentCurrency = currencySymbol,
                alertsEnabled = budgetAlertsEnabled,
                onDismiss = { showSettingsDialog = false },
                onCurrencyChange = { code -> viewModel.setCurrencyCode(code) },
                onAlertsToggle = { enabled -> viewModel.setBudgetAlertsEnabled(enabled) },
                onClearAll = { viewModel.clearAllData() },
                currentCurrencyCode = currentCurrencyCode,
                isFetchingRates = isFetchingRates,
                apiError = apiError,
                lastFetchedTime = lastFetchedTime,
                onRefreshRates = { viewModel.fetchExchangeRates() },
                syncEnabled = syncEnabled,
                onSyncToggle = { enabled -> viewModel.updateSyncEnabled(enabled) },
                firebaseUrl = firebaseUrl,
                onUrlChange = { url -> viewModel.updateFirebaseUrl(url) },
                syncEmail = syncEmail,
                onEmailChange = { email -> viewModel.updateSyncEmail(email) },
                isSyncing = isSyncing,
                lastSyncedTime = lastSyncedTime,
                syncError = syncError,
                onManualSync = { viewModel.syncWithWebDatabase() }
              )
            }

            if (showAddBudget) {
              AddBudgetDialog(
                viewModel = viewModel,
                onDismiss = { showAddBudget = false },
                onSave = { category, limit, alertThreshold ->
                  viewModel.addBudget(category, limit, alertThreshold)
                }
              )
            }

            if (showAddNote) {
              AddNoteDialog(
                onDismiss = { showAddNote = false },
                onSave = { title, content, timestamp ->
                  viewModel.addNote(title, content, timestamp)
                }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun FinanceNavigationBar(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    val items = listOf("Daily", "Calendar", "Monthly", "Budget", "Notes")
    val icons = listOf(
        Icons.Default.CalendarToday,
        Icons.Default.DateRange,
        Icons.Default.BarChart,
        Icons.Default.AccountBalanceWallet,
        Icons.Default.Description
    )

    NavigationBar(
        containerColor = DarkSurface,
        modifier = Modifier
            .border(width = 1.dp, color = SurfaceBorder, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .windowInsetsPadding(WindowInsets.navigationBars),
        tonalElevation = 8.dp
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = currentTab == item
            val iconColor = if (isSelected) NetYellow else TextSecondary
            val labelColor = iconColor

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(item) },
                icon = {
                    Icon(
                        imageVector = icons[index],
                        contentDescription = item,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = item,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = labelColor
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = NetYellow.copy(alpha = 0.15f)
                ),
                modifier = Modifier.testTag("nav_item_${item.lowercase()}")
            )
        }
    }
}

