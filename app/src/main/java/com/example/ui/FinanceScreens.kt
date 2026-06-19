package com.example.ui

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.theme.*
import android.content.Intent
import android.speech.RecognizerIntent
import android.app.Activity
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.sqrt
import kotlin.math.atan2
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

// Helper function to format currency
fun formatCurrency(amount: Double, symbol: String = "$"): String {
    return String.format(Locale.US, "%s%.2f", symbol, amount)
}

// Category helper to fetch icons and accent background colors
fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "food", "dining", "groceries" -> Icons.Default.Restaurant
        "shopping", "clothing" -> Icons.Default.ShoppingBag
        "transport", "fuel", "car", "taxi" -> Icons.Default.DirectionsCar
        "entertainment", "movie", "fun" -> Icons.Default.LocalActivity
        "utilities", "rent", "bills" -> Icons.Default.FlashOn
        "salary", "bonus", "income" -> Icons.Default.AttachMoney
        "health", "fitness", "medicine" -> Icons.Default.LocalHospital
        "education", "books" -> Icons.Default.School
        else -> Icons.Default.Category
    }
}

fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "food", "dining", "groceries" -> Color(0xFFE28743)
        "shopping", "clothing" -> Color(0xFFE06D8C)
        "transport", "fuel", "car", "taxi" -> Color(0xFF42A5F5)
        "entertainment", "movie", "fun" -> Color(0xFFAB47BC)
        "utilities", "rent", "bills" -> Color(0xFFFFCA28)
        "salary", "bonus", "income" -> Color(0xFF66BB6A)
        "health", "fitness", "medicine" -> Color(0xFF26A69A)
        "education", "books" -> Color(0xFF8D6E63)
        else -> Color(0xFF90A4AE)
    }
}

fun getAccountIcon(name: String): ImageVector {
    val lower = name.lowercase()
    return when {
        lower.contains("cash") -> Icons.Default.Payments
        lower.contains("saving") -> Icons.Default.Savings
        lower.contains("credit") || lower.contains("card") -> Icons.Default.CreditCard
        else -> Icons.Default.AccountBalance
    }
}

fun getAccountColor(name: String): Color {
    val lower = name.lowercase()
    return when {
        lower.contains("cash") -> IncomeGreen
        lower.contains("saving") -> Purple80
        lower.contains("credit") || lower.contains("card") -> ExpenseRed
        else -> NetYellow
    }
}

// ==========================================
// 1. DAILY SCREEN (Main Dashboard View)
// ==========================================
@Composable
fun DailyTabScreen(
    viewModel: FinanceViewModel,
    onShowAddTransaction: () -> Unit,
    onShowFilter: () -> Unit,
    onShowSettings: () -> Unit
) {
    val context = LocalContext.current
    val selectedDate by viewModel.selectedDate.collectAsState()
    val dailyTransactions by viewModel.dailyTransactions.collectAsState()
    val dailyStats by viewModel.dailyStats.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val filterCategory by viewModel.filterCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val customStartDate by viewModel.customStartDate.collectAsState()
    val customEndDate by viewModel.customEndDate.collectAsState()
    val budgetAlerts by viewModel.budgetAlerts.collectAsState()
    val isDeviceOnline by viewModel.isDeviceOnline.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val showTransactionTime by viewModel.showTransactionTime.collectAsState()

    val titleText = remember(selectedPeriod) {
        when (selectedPeriod) {
            "Daily" -> "Daily"
            "Weekly" -> "Weekly"
            "Monthly" -> "Monthly"
            "Yearly" -> "Yearly"
            "Custom" -> "Duration"
            else -> "Daily"
        }
    }

    val finalFormattedDate = remember(selectedDate, selectedPeriod, customStartDate, customEndDate) {
        when (selectedPeriod) {
            "Daily" -> selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))
            "Weekly" -> {
                val startOfWeek = selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
                val endOfWeek = startOfWeek.plusDays(6)
                "${startOfWeek.format(DateTimeFormatter.ofPattern("MMM d"))} - ${endOfWeek.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
            }
            "Monthly" -> selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            "Yearly" -> selectedDate.format(DateTimeFormatter.ofPattern("yyyy"))
            "Custom" -> "${customStartDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))} - ${customEndDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
            else -> selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onShowAddTransaction,
                containerColor = NetYellow,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 8.dp)
                    .testTag("add_transaction_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Transaction",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Premium Title & Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (selectedPeriod != "Custom") {
                                // Date picker
                                val datePickerDialog = DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        viewModel.selectDate(LocalDate.of(year, month + 1, dayOfMonth))
                                    },
                                    selectedDate.year,
                                    selectedDate.monthValue - 1,
                                    selectedDate.dayOfMonth
                                )
                                datePickerDialog.show()
                            }
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = titleText,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        // Online / Sync status badge
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isDeviceOnline) IncomeGreen.copy(alpha = 0.15f) else TextSecondary.copy(alpha = 0.15f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (isDeviceOnline) IncomeGreen else TextSecondary,
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSyncing) "Syncing" else if (isDeviceOnline) "Cloud Synced" else "Offline Mode",
                                    fontSize = 9.sp,
                                    color = if (isDeviceOnline) IncomeGreen else TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = finalFormattedDate,
                            fontSize = 14.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        if (selectedPeriod != "Custom") {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Change Date",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Header Filter Area
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Custom Duration Selector (on the left of the filter area)
                    var showDurationDialog by remember { mutableStateOf(false) }
                    
                    IconButton(
                        onClick = { showDurationDialog = true },
                        modifier = Modifier.testTag("custom_duration_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Custom Duration",
                            tint = if (selectedPeriod == "Custom") NetYellow else TextSecondary
                        )
                    }

                    if (showDurationDialog) {
                        CustomDurationDialog(
                            viewModel = viewModel,
                            onDismiss = { showDurationDialog = false }
                        )
                    }

                    // Time-Period Filter (Dropdown Menu)
                    var showPeriodMenu by remember { mutableStateOf(false) }
                    
                    Box {
                        IconButton(
                            onClick = { showPeriodMenu = true },
                            modifier = Modifier.testTag("period_filter_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventNote,
                                contentDescription = "Time-Period Filters",
                                tint = if (selectedPeriod != "Custom") NetYellow else TextSecondary
                            )
                        }

                        DropdownMenu(
                            expanded = showPeriodMenu,
                            onDismissRequest = { showPeriodMenu = false },
                            modifier = Modifier.background(DarkSurface).border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                        ) {
                            val periodOptions = listOf("Daily", "Weekly", "Monthly", "Yearly")
                            periodOptions.forEach { p ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = p, 
                                            color = if (selectedPeriod == p) NetYellow else TextPrimary,
                                            fontWeight = if (selectedPeriod == p) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        ) 
                                    },
                                    onClick = {
                                        viewModel.setSelectedPeriod(p)
                                        showPeriodMenu = false
                                    },
                                    modifier = Modifier.testTag("period_option_$p")
                                )
                            }
                        }
                    }

                    // Header filter state indication
                    val isFiltered = filterType != "ALL" || filterCategory != "ALL"

                    IconButton(
                        onClick = onShowFilter,
                        modifier = Modifier.testTag("filter_button")
                    ) {
                        Icon(
                            imageVector = if (isFiltered) Icons.Default.FilterListOff else Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (isFiltered) NetYellow else TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onShowSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Elegant Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search transactions or categories...", color = TextSecondary, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.setSearchQuery("") },
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = NetYellow,
                    unfocusedBorderColor = SurfaceBorder,
                    cursorColor = NetYellow,
                    focusedPlaceholderColor = TextSecondary,
                    unfocusedPlaceholderColor = TextSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_search_bar")
            )

            if (budgetAlerts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                BudgetAlertsNotificationPanel(
                    alerts = budgetAlerts,
                    currencySymbol = currencySymbol,
                    onNavigateToBudgets = { viewModel.selectTab("Budget") }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            MonthlyBudgetProgressCard(
                viewModel = viewModel,
                modifier = Modifier.testTag("monthly_budget_progress_card")
            )

            Spacer(modifier = Modifier.height(18.dp))

            // ACCOUNTS SUMMARY PANEL
            Text(
                text = "My Accounts",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NetYellow
            )
            Spacer(modifier = Modifier.height(8.dp))
            val dynamicBalances by viewModel.dynamicAccountBalances.collectAsState()
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val count = dynamicBalances.size
                if (count <= 3) {
                    val cardWidth = if (count > 0) (maxWidth - (8.dp * (count - 1))) / count else maxWidth
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dynamicBalances.forEach { (accName, bal) ->
                            val color = getAccountColor(accName)
                            AccountSummaryCard(
                                title = accName.uppercase(),
                                amountString = formatCurrency(bal, currencySymbol),
                                tintColor = color,
                                backgroundColor = color.copy(alpha = 0.15f),
                                icon = getAccountIcon(accName),
                                modifier = Modifier.width(cardWidth)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dynamicBalances.forEach { (accName, bal) ->
                            val color = getAccountColor(accName)
                            AccountSummaryCard(
                                title = accName.uppercase(),
                                amountString = formatCurrency(bal, currencySymbol),
                                tintColor = color,
                                backgroundColor = color.copy(alpha = 0.15f),
                                icon = getAccountIcon(accName),
                                modifier = Modifier.width(110.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // STATS BANNER: Responsive Row of 3 Cards (Income, XP, Net)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cardWidth = (maxWidth - 16.dp) / 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card 1: Income
                    StatCard(
                        title = "INCOME",
                        amountString = formatCurrency(dailyStats.income, currencySymbol),
                        tintColor = IncomeGreen,
                        backgroundColor = IncomeGreenBg,
                        icon = Icons.Default.ArrowDownward,
                        modifier = Modifier.width(cardWidth)
                    )

                    // Card 2: Expense
                    StatCard(
                        title = "EXPENSES",
                        amountString = "-${formatCurrency(dailyStats.expense, currencySymbol)}",
                        tintColor = ExpenseRed,
                        backgroundColor = ExpenseRedBg,
                        icon = Icons.Default.ArrowUpward,
                        modifier = Modifier.width(cardWidth)
                    )

                    // Card 3: Net
                    StatCard(
                        title = "NET",
                        amountString = (if (dailyStats.net >= 0) "+" else "") + formatCurrency(dailyStats.net, currencySymbol),
                        tintColor = NetYellow,
                        backgroundColor = NetYellowBg,
                        icon = Icons.Default.AccountBalanceWallet,
                        modifier = Modifier.width(cardWidth)
                    )
                }
            }

            // ----------------------------------------------------
            // MONTHLY TRANSACTIONS & SUMMARY SECTION (Daily Tab Addition)
            // ----------------------------------------------------
            var showMonthlySectionList by remember { mutableStateOf(false) }
            val monthlyTransactionsForDailyTab by viewModel.monthlyTransactions.collectAsState()
            val monthlyStatsForDailyTab by viewModel.monthlyStats.collectAsState()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .border(1.dp, NetYellow.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = NetYellow,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Monthly Transactions Summary",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        TextButton(
                            onClick = { showMonthlySectionList = !showMonthlySectionList },
                            colors = ButtonDefaults.textButtonColors(contentColor = NetYellow)
                        ) {
                            Text(
                                text = if (showMonthlySectionList) "Hide List" else "View Monthly List (${monthlyTransactionsForDailyTab.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (showMonthlySectionList) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3-way stats overview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(IncomeGreenBg, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("MONTH INCOME", fontSize = 8.sp, color = IncomeGreen, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatCurrency(monthlyStatsForDailyTab.income, currencySymbol),
                                fontSize = 12.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(ExpenseRedBg, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("MONTH EXPENSES", fontSize = 8.sp, color = ExpenseRed, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "-${formatCurrency(monthlyStatsForDailyTab.expense, currencySymbol)}",
                                fontSize = 12.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        val netColor = if (monthlyStatsForDailyTab.net >= 0) IncomeGreen else ExpenseRed
                        val netBg = if (monthlyStatsForDailyTab.net >= 0) IncomeGreenBg else ExpenseRedBg
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(netBg, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("MONTH NET", fontSize = 8.sp, color = netColor, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = (if (monthlyStatsForDailyTab.net >= 0) "+" else "") + formatCurrency(monthlyStatsForDailyTab.net, currencySymbol),
                                fontSize = 12.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Toggleable monthly transaction list
                    if (showMonthlySectionList) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = SurfaceBorder)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (monthlyTransactionsForDailyTab.isEmpty()) {
                            Text(
                                text = "No transactions this month.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                monthlyTransactionsForDailyTab.forEach { transaction ->
                                    TransactionRowItem(
                                        transaction = transaction,
                                        onDelete = { viewModel.deleteTransactionById(transaction.id) },
                                        currencySymbol = currencySymbol,
                                        onEdit = { viewModel.startEditingTransaction(transaction) },
                                        showTime = showTransactionTime
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (selectedPeriod == "Daily") "Today's Transactions" else "Period Transactions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                var showRecurringDialog by remember { mutableStateOf(false) }
                
                TextButton(
                    onClick = { showRecurringDialog = true },
                    modifier = Modifier.testTag("manage_recurring_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Recurring Transactions",
                        tint = NetYellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Recurring",
                        color = NetYellow,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (showRecurringDialog) {
                    RecurringTransactionsDialog(
                        viewModel = viewModel,
                        onDismiss = { showRecurringDialog = false }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ----------------------------------------------------
            // CATEGORY FILTER DROPDOWN MENU
            // ----------------------------------------------------
            var showCategoryMenu by remember { mutableStateOf(false) }
            val categories = listOf("ALL", "Food", "Transport", "Rent", "Housing/Rent", "Shopping", "Utilities", "Entertainment", "Health", "Education", "Other")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Category Filter:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                
                Box {
                    Button(
                        onClick = { showCategoryMenu = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("category_filter_dropdown_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = filterCategory,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (filterCategory != "ALL") NetYellow else TextPrimary
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown Arrow",
                                tint = if (filterCategory != "ALL") NetYellow else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false },
                        modifier = Modifier
                            .background(DarkSurface)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = cat,
                                        fontSize = 13.sp,
                                        fontWeight = if (filterCategory == cat) FontWeight.Bold else FontWeight.Normal,
                                        color = if (filterCategory == cat) NetYellow else TextPrimary
                                    )
                                },
                                onClick = {
                                    viewModel.setFilter(filterType, cat)
                                    showCategoryMenu = false
                                },
                                modifier = Modifier.testTag("category_filter_option_$cat")
                            )
                        }
                    }
                }

                if (filterCategory != "ALL") {
                    IconButton(
                        onClick = { viewModel.setFilter(filterType, "ALL") },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("clear_category_filter_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Filter",
                            tint = ExpenseRed,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // List or Empty State
            if (dailyTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(DarkSurface, CircleShape)
                                .border(1.dp, SurfaceBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = "Receipt Icon",
                                modifier = Modifier.size(36.dp),
                                tint = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No transactions today",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap + to add your first transaction for today",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    dailyTransactions.forEach { transaction ->
                        TransactionRowItem(
                            transaction = transaction,
                            onDelete = { viewModel.deleteTransactionById(transaction.id) },
                            currencySymbol = currencySymbol,
                            onEdit = { viewModel.startEditingTransaction(transaction) },
                            showTime = showTransactionTime
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reflections Section
            val dailyNotes by viewModel.dailyNotes.collectAsState()
            var showAddNoteDialogInsideDaily by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Reflections & Notes",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(
                    onClick = { showAddNoteDialogInsideDaily = true },
                    modifier = Modifier.testTag("add_day_note_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Day Note",
                        tint = NetYellow
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (dailyNotes.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No notes or reflections saved for this day.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { showAddNoteDialogInsideDaily = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = NetYellow)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Write Daily Reflection", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    dailyNotes.forEach { note ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = NetYellow,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = note.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteNote(note.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Note",
                                            tint = ExpenseRed.copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = note.content,
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            if (showAddNoteDialogInsideDaily) {
                val selectedDateMilli = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                AddNoteDialog(
                    onDismiss = { showAddNoteDialogInsideDaily = false },
                    onSave = { title, content, timestamp ->
                        viewModel.addNote(title, content, timestamp)
                    },
                    initialTimestamp = selectedDateMilli
                )
            }
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
fun AccountSummaryCard(
    title: String,
    amountString: String,
    tintColor: Color,
    backgroundColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(backgroundColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = amountString,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    amountString: String,
    tintColor: Color,
    backgroundColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(backgroundColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tintColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amountString,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = tintColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TransactionRowItem(
    transaction: TransactionEntity,
    onDelete: () -> Unit,
    currencySymbol: String = "$",
    onEdit: (() -> Unit)? = null,
    showTime: Boolean = true
) {
    val isTransfer = transaction.type == "TRANSFER"
    val categoryColor = if (isTransfer) NetYellow else getCategoryColor(transaction.category)
    val categoryIcon = if (isTransfer) Icons.Default.SwapHoriz else getCategoryIcon(transaction.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onEdit != null) { onEdit?.invoke() }
            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon space
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(categoryColor.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, categoryColor.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = transaction.category,
                    tint = categoryColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isTransfer) "Transfer" else transaction.category,
                        fontSize = 12.sp,
                        color = categoryColor,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (showTime) {
                        val formattedTime = try {
                            java.time.Instant.ofEpochMilli(transaction.timestamp)
                                .atZone(java.time.ZoneId.systemDefault())
                                .format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"))
                        } catch (e: Exception) { "" }
                        if (formattedTime.isNotEmpty()) {
                            Text(
                                text = " • $formattedTime",
                                fontSize = 12.sp,
                                color = TextSecondary.copy(alpha = 0.8f)
                            )
                        }
                    }
                    
                    // Account badge capsule
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(categoryColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .border(1.dp, categoryColor.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (isTransfer) "${transaction.account} ➜ ${transaction.toAccount ?: "Saving"}" else transaction.account,
                            fontSize = 9.sp,
                            color = categoryColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (transaction.notes.isNotEmpty()) {
                        Text(
                            text = " • ${transaction.notes}",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Amount / Delete Action
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                val prefix = when (transaction.type) {
                    "INCOME" -> "+"
                    "EXPENSE" -> "-"
                    "TRANSFER" -> "⇆ "
                    else -> ""
                }
                val textColor = when (transaction.type) {
                    "INCOME" -> IncomeGreen
                    "EXPENSE" -> ExpenseRed
                    "TRANSFER" -> NetYellow
                    else -> TextPrimary
                }

                Text(
                    text = "$prefix${formatCurrency(transaction.amount, currencySymbol)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onEdit != null) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(24.dp).testTag("edit_transaction_button_${transaction.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = NetYellow.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp).testTag("delete_transaction_button_${transaction.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = ExpenseRed.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. CALENDAR SCREEN
fun formatCalAmount(amount: Double, symbol: String): String {
    val absVal = kotlin.math.abs(amount)
    if (absVal < 0.01) {
        return "${symbol}0"
    }
    val formatted = when {
        absVal >= 1000000.0 -> {
            val millions = absVal / 1000000.0
            String.format(java.util.Locale.US, "%.1fm", millions)
        }
        absVal >= 1000.0 -> {
            val thousands = absVal / 1000.0
            if ((thousands * 10).toInt() % 10 == 0) {
                String.format(java.util.Locale.US, "%.0fk", thousands)
            } else {
                String.format(java.util.Locale.US, "%.1fk", thousands)
            }
        }
        else -> {
            if (absVal % 1.0 < 0.1) {
                "${absVal.toInt()}"
            } else {
                String.format(java.util.Locale.US, "%.1f", absVal)
            }
        }
    }
    val sign = if (amount >= 0) "+" else "-"
    return "$sign$symbol$formatted"
}

// ==========================================
// 2. CALENDAR SCREEN
// ==========================================
@Composable
fun CalendarTabScreen(
    viewModel: FinanceViewModel,
    onShowAddTransaction: () -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val dailyTransactions by viewModel.calendarTransactions.collectAsState()
    val dailyStats by viewModel.calendarStats.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val showTransactionTime by viewModel.showTransactionTime.collectAsState()

    var currentCalendarMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }

    val daysInMonthList = remember(currentCalendarMonth) {
        val days = mutableListOf<LocalDate?>()
        val firstOfMonth = currentCalendarMonth.atDay(1)
        val dayOfWeekValue = firstOfMonth.dayOfWeek.value // 1 = Monday, 7 = Sunday
        
        // Add null space tags before start of month for spacing
        // Adjust standard Sunday index shift: standard JVM 1=Mon, 7=Sun.
        // We want Sunday (7) or the gap offset. Let's do:
        val gap = if (dayOfWeekValue == 7) 0 else dayOfWeekValue
        for (i in 0 until gap) {
            days.add(null)
        }
        for (day in 1..currentCalendarMonth.lengthOfMonth()) {
            days.add(currentCalendarMonth.atDay(day))
        }
        days
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onShowAddTransaction,
                containerColor = NetYellow,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 8.dp)
                    .testTag("add_transaction_calendar_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Transaction",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Calendar",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Trace money by date",
            fontSize = 14.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Calendar controller header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { currentCalendarMonth = currentCalendarMonth.minusMonths(1) }
                    ) {
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = TextPrimary)
                    }

                    Text(
                        text = currentCalendarMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    IconButton(
                        onClick = { currentCalendarMonth = currentCalendarMonth.plusMonths(1) }
                    ) {
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Month", tint = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Week days rows
                val daysOfWeekNames = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
                Row(modifier = Modifier.fillMaxWidth()) {
                    daysOfWeekNames.forEach { name ->
                        Text(
                            text = name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Calendar Grid!
                val chunks = daysInMonthList.chunked(7)
                chunks.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        week.forEach { date ->
                            if (date == null) {
                                Box(modifier = Modifier.weight(1f).height(52.dp))
                            } else {
                                val isSelected = date == selectedDate
                                val dayTransactions = allTransactions.filter {
                                    !it.isDeleted && java.time.Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate() == date
                                }

                                val dayIncomes = dayTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
                                val dayExpenses = dayTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                                val dayNet = dayIncomes - dayExpenses

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .padding(2.dp)
                                        .background(
                                            color = if (isSelected) NetYellow else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            viewModel.selectDate(date)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = date.dayOfMonth.toString(),
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = if (isSelected) Color.Black else TextPrimary
                                        )
                                        
                                        if (dayTransactions.isNotEmpty()) {
                                            val textAmount = formatCalAmount(dayNet, currencySymbol)
                                            val textColor = when {
                                                isSelected -> Color.Black.copy(alpha = 0.8f)
                                                dayNet > 0.05 -> IncomeGreen
                                                dayNet < -0.05 -> ExpenseRed
                                                else -> TextSecondary
                                            }
                                            Text(
                                                text = textAmount,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.height(10.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Selected Date Summary list
        Text(
            text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Stats card for selected calendar date
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Income", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text(formatCurrency(dailyStats.income, currencySymbol), fontSize = 15.sp, color = IncomeGreen, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Expenses", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text("-${formatCurrency(dailyStats.expense, currencySymbol)}", fontSize = 15.sp, color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Net Cashflow", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text(
                        text = (if (dailyStats.net >= 0) "+" else "") + formatCurrency(dailyStats.net, currencySymbol),
                        fontSize = 15.sp,
                        color = NetYellow,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mini listings
        if (dailyTransactions.isEmpty()) {
            Text(
                text = "No activities on this date",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                textAlign = TextAlign.Center
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dailyTransactions.forEach { transaction ->
                    TransactionRowItem(
                        transaction = transaction,
                        onDelete = { viewModel.deleteTransactionById(transaction.id) },
                        currencySymbol = currencySymbol,
                        onEdit = { viewModel.startEditingTransaction(transaction) },
                        showTime = showTransactionTime
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

// ==========================================
// 3. MONTHLY ANALYSIS SCREEN (With Custom Canvas Graphic)
// ==========================================
@Composable
fun MonthlyTabScreen(viewModel: FinanceViewModel) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val monthlyTransactions by viewModel.monthlyTransactions.collectAsState()
    val monthlyStats by viewModel.monthlyStats.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()

    val currentMonthName = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }

    // Spend distribution calculation
    val spendByCategory = remember(monthlyTransactions) {
        val distribution = mutableMapOf<String, Double>()
        monthlyTransactions.filter { it.type == "EXPENSE" }.forEach { t ->
            distribution[t.category] = (distribution[t.category] ?: 0.0) + t.amount
        }
        distribution.toList().sortedByDescending { it.second }
    }

    val totalExpense = remember(spendByCategory) {
        spendByCategory.sumOf { it.second }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Monthly Review",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Analysis for $currentMonthName",
            fontSize = 14.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Custom canvas visualizer bar graph comparing INCOME vs EXPENSE side by side
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Cashflow Overview",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Canvas Chart
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    val maxVal = maxOf(monthlyStats.income, monthlyStats.expense, 100.0)
                    val factor = height * 0.8f / maxVal.toFloat()

                    // Draw grid lines
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = height * (1f - (i.toFloat() / gridLines))
                        drawLine(
                            color = SurfaceBorder.copy(alpha = 0.5f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Income bar dimensions
                    val barWidth = width * 0.2f
                    val incomeHeight = (monthlyStats.income * factor).toFloat()
                    val expenseHeight = (monthlyStats.expense * factor).toFloat()

                    // Draw income bar
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(IncomeGreen, IncomeGreen.copy(alpha = 0.3f))
                        ),
                        topLeft = Offset(width * 0.2f, height - incomeHeight),
                        size = Size(barWidth, incomeHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    // Draw expense bar
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(ExpenseRed, ExpenseRed.copy(alpha = 0.3f))
                        ),
                        topLeft = Offset(width * 0.6f, height - expenseHeight),
                        size = Size(barWidth, expenseHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(IncomeGreen, RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Income: ${formatCurrency(monthlyStats.income, currencySymbol)}", fontSize = 12.sp, color = TextPrimary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(ExpenseRed, RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Expenses: ${formatCurrency(monthlyStats.expense, currencySymbol)}", fontSize = 12.sp, color = TextPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (spendByCategory.isNotEmpty()) {
            RechartsStyleSpendingPieChart(
                spendByCategory = spendByCategory,
                totalExpense = totalExpense,
                currencySymbol = currencySymbol,
                modifier = Modifier.padding(bottom = 24.dp).testTag("recharts_spending_pie_chart")
            )

            RechartsStyleSpendingBarChart(
                spendByCategory = spendByCategory,
                totalExpense = totalExpense,
                currencySymbol = currencySymbol,
                modifier = Modifier.padding(bottom = 24.dp).testTag("recharts_spending_bar_chart")
            )
        }

        Text(
            text = "Expense Breakdown",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (spendByCategory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No expenses recorded this month",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    spendByCategory.forEach { (cat, amount) ->
                        val percent = if (totalExpense > 0) (amount / totalExpense).toFloat() else 0f
                        val categoryColor = getCategoryColor(cat)

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = getCategoryIcon(cat),
                                        contentDescription = cat,
                                        tint = categoryColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = cat,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "${formatCurrency(amount, currencySymbol)} (${(percent * 100).toInt()}%)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { percent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = categoryColor,
                                trackColor = SurfaceBorder
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(64.dp))
    }
}

// ==========================================
// 4. BUDGET LIMITS SCREEN
// ==========================================
@Composable
fun BudgetTabScreen(
    viewModel: FinanceViewModel,
    onShowAddBudget: () -> Unit
) {
    val monthlyBudgets by viewModel.monthlyBudgets.collectAsState()
    val monthlyTransactions by viewModel.monthlyTransactions.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()

    // Sum spending per category dynamically
    val categorySpending = remember(monthlyTransactions) {
        val spending = mutableMapOf<String, Double>()
        monthlyTransactions.filter { it.type == "EXPENSE" }.forEach { t ->
            spending[t.category] = (spending[t.category] ?: 0.0) + t.amount
        }
        spending
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onShowAddBudget,
                containerColor = NetYellow,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 8.dp)
                    .testTag("add_budget_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Set Budget",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Budgets",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Keep expenses controlled",
                fontSize = 14.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            MonthlyBudgetProgressCard(
                viewModel = viewModel,
                onSetBudgetClick = onShowAddBudget,
                modifier = Modifier.padding(bottom = 24.dp).testTag("monthly_budget_progress_card_tab")
            )

            if (monthlyBudgets.isNotEmpty()) {
                BudgetBarChartCard(
                    monthlyBudgets = monthlyBudgets,
                    categorySpending = categorySpending,
                    currencySymbol = currencySymbol
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (monthlyBudgets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(DarkSurface, CircleShape)
                                .border(1.dp, SurfaceBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Budget Icon",
                                modifier = Modifier.size(36.dp),
                                tint = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No active budgets",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap + to establish budget goals for this month",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    monthlyBudgets.forEach { budget ->
                        val spent = categorySpending[budget.category] ?: 0.0
                        val percentage = if (budget.limitAmount > 0) (spent / budget.limitAmount).toFloat() else 0f
                        val isBreached = spent > budget.limitAmount
                        val alertThresholdPercent = budget.alertThreshold
                        val hasExceededThreshold = spent >= (budget.limitAmount * (alertThresholdPercent / 100.0)) && !isBreached
                        val categoryColor = getCategoryColor(budget.category)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isBreached || hasExceededThreshold) 1.5.dp else 1.dp,
                                    color = if (isBreached) ExpenseRed else if (hasExceededThreshold) NetYellow else SurfaceBorder,
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(categoryColor.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = getCategoryIcon(budget.category),
                                                contentDescription = budget.category,
                                                tint = categoryColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = budget.category,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteBudget(budget.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Budget",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Spent: ${formatCurrency(spent, currencySymbol)}",
                                        fontSize = 13.sp,
                                        color = if (isBreached) ExpenseRed else if (hasExceededThreshold) NetYellow else TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Limit: ${formatCurrency(budget.limitAmount, currencySymbol)}",
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { percentage.coerceAtMost(1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (isBreached) ExpenseRed else if (hasExceededThreshold) NetYellow else categoryColor,
                                    trackColor = SurfaceBorder
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Threshold: ${alertThresholdPercent.toInt()}%",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    if (isBreached) {
                                        Text(
                                            text = "⚠️ Budget Limit Breached!",
                                            fontSize = 11.sp,
                                            color = ExpenseRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else if (hasExceededThreshold) {
                                        Text(
                                            text = "⚠️ Threshold Exceeded!",
                                            fontSize = 11.sp,
                                            color = NetYellow,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            text = "Under warning line",
                                            fontSize = 10.sp,
                                            color = IncomeGreen,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

// ==========================================
// 8.5. RECHARTS-INSPIRED INTERACTIVE SPENDING BAR CHART
// ==========================================
fun formatCurrencyWithAbbreviation(amount: Double, symbol: String, abbreviate: Boolean = false): String {
    if (!abbreviate) {
        return formatCurrency(amount, symbol)
    }
    return if (amount >= 1000.0) {
        String.format(Locale.US, "%s%.1fk", symbol, amount / 1000.0)
    } else {
        String.format(Locale.US, "%s%.0f", symbol, amount)
    }
}

@Composable
fun RechartsStyleSpendingPieChart(
    spendByCategory: List<Pair<String, Double>>,
    totalExpense: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    if (spendByCategory.isEmpty()) return

    var hoveredCategory by remember { mutableStateOf<String?>(null) }

    // Synchronize default selection on launch
    LaunchedEffect(spendByCategory) {
        if (hoveredCategory == null || spendByCategory.none { it.first == hoveredCategory }) {
            hoveredCategory = spendByCategory.firstOrNull()?.first
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Spending Breakdown (Pie Chart)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Interactive, Tap slices or legend to inspect",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Purple80.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .border(0.5.dp, Purple80.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Recharts Style",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Purple80
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Graph Row: Left donut chart with text inside, Right scrollable legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Donut Chart container
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .weight(1.1f),
                    contentAlignment = Alignment.Center
                ) {
                    val density = LocalDensity.current
                    var centerOffset by remember { mutableStateOf(Offset.Zero) }
                    var donutRadius by remember { mutableStateOf(0f) }
                    val thicknessDp = 20.dp
                    val thicknessPx = with(density) { thicknessDp.toPx() }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(spendByCategory, totalExpense) {
                                detectTapGestures { offset ->
                                    val dx = offset.x - centerOffset.x
                                    val dy = offset.y - centerOffset.y
                                    val distance = sqrt(dx * dx + dy * dy)
                                    val outer = donutRadius
                                    val inner = donutRadius - thicknessPx

                                    if (distance in inner..outer * 1.35f) {
                                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                        if (angle < 0) angle += 360f
                                        // Top start: start at -90 degrees
                                        val normalized = (angle + 90f) % 360f

                                        var currentStart = 0f
                                        for ((cat, amount) in spendByCategory) {
                                            val sweep = if (totalExpense > 0) ((amount / totalExpense) * 360f).toFloat() else 0f
                                            if (normalized >= currentStart && normalized < currentStart + sweep) {
                                                hoveredCategory = cat
                                                break
                                            }
                                            currentStart += sweep
                                        }
                                    }
                                }
                            }
                    ) {
                        val width = size.width
                        val height = size.height
                        centerOffset = Offset(width / 2, height / 2)
                        
                        val padding = 16.dp.toPx()
                        val radius = (minOf(width, height) / 2) - padding
                        donutRadius = radius + padding // Outer boundary

                        var currentStartAngle = -90f

                        spendByCategory.forEach { (cat, amount) ->
                            val sweepAngle = if (totalExpense > 0) ((amount / totalExpense) * 360f).toFloat() else 0f
                            val categoryColor = getCategoryColor(cat)
                            val isSelected = hoveredCategory == cat

                            val drawThickness = if (isSelected) thicknessPx + 10f else thicknessPx
                            val finalRadius = if (isSelected) radius + 4f else radius

                            drawArc(
                                color = categoryColor,
                                startAngle = currentStartAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = Offset(width / 2 - finalRadius, height / 2 - finalRadius),
                                size = Size(finalRadius * 2, finalRadius * 2),
                                style = Stroke(width = drawThickness, cap = StrokeCap.Butt)
                            )
                            currentStartAngle += sweepAngle
                        }
                    }

                    // Inside texts
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        if (hoveredCategory != null) {
                            val activeCat = hoveredCategory!!
                            val activeAmt = spendByCategory.firstOrNull { it.first == activeCat }?.second ?: 0.0
                            val percent = if (totalExpense > 0) (activeAmt / totalExpense * 100).toInt() else 0

                            Text(
                                text = activeCat.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = getCategoryColor(activeCat),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$percent%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        } else {
                            Text(
                                text = "TOTAL",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatCurrency(totalExpense, currencySymbol),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Scrollable Legend list on the Right
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center
                ) {
                    spendByCategory.forEach { (cat, amount) ->
                        val isSelected = hoveredCategory == cat
                        val categoryColor = getCategoryColor(cat)
                        val percent = if (totalExpense > 0) (amount / totalExpense * 100).toInt() else 0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) categoryColor.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable { hoveredCategory = cat }
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(categoryColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$percent%",
                                fontSize = 11.sp,
                                color = if (isSelected) categoryColor else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Detailed Inspect Row at the bottom
            hoveredCategory?.let { activeCat ->
                val activeAmt = spendByCategory.firstOrNull { it.first == activeCat }?.second ?: 0.0
                val activePercent = if (totalExpense > 0) (activeAmt / totalExpense).toFloat() else 0f
                val activeColor = getCategoryColor(activeCat)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg, RoundedCornerShape(14.dp))
                        .border(1.dp, activeColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                        .testTag("recharts_pie_tooltip_panel"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(activeColor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(activeCat),
                                contentDescription = activeCat,
                                tint = activeColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = activeCat,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f%% of this month's budget", activePercent * 100),
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Text(
                        text = formatCurrency(activeAmt, currencySymbol),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun RechartsStyleSpendingBarChart(
    spendByCategory: List<Pair<String, Double>>,
    totalExpense: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    if (spendByCategory.isEmpty()) return

    var hoveredCategory by remember { mutableStateOf<String?>(null) }

    // Synchronize default selection on launch
    LaunchedEffect(spendByCategory) {
        if (hoveredCategory == null || spendByCategory.none { it.first == hoveredCategory }) {
            hoveredCategory = spendByCategory.firstOrNull()?.first
        }
    }

    val maxAmount = remember(spendByCategory) {
        spendByCategory.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 100.0
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Spending Distribution",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Interactive, Tap bars to inspect",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .background(NetYellow.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .border(0.5.dp, NetYellow.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Interactive Chart",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NetYellow
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Bar graph container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                // Background ticks/grid lines (Y-axis)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(5) { step ->
                        val ratio = 1f - (step.toFloat() / 4f)
                        val gridValue = maxAmount * ratio
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatCurrencyWithAbbreviation(gridValue, currencySymbol, abbreviate = true),
                                fontSize = 9.sp,
                                color = TextSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.width(42.dp),
                                maxLines = 1
                            )
                            HorizontalDivider(
                                color = SurfaceBorder.copy(alpha = 0.15f),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Interactive Bar Columns
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 46.dp, end = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    spendByCategory.forEach { (category, amount) ->
                        val categoryColor = getCategoryColor(category)
                        val barHeightFraction = (amount / maxAmount).toFloat().coerceIn(0.02f, 1f)
                        val isSelected = hoveredCategory == category

                        Column(
                            modifier = Modifier
                                .width(44.dp)
                                .clickable { hoveredCategory = category }
                                .padding(top = 8.dp)
                                .testTag("recharts_bar_${category.lowercase()}"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                // Background highlight tint pillar
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            color = if (isSelected) categoryColor.copy(alpha = 0.05f) else Color.Transparent,
                                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                        )
                                )

                                // Custom styled Recharts vertical bar with vertical gradient
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(barHeightFraction)
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = if (isSelected) {
                                                    listOf(categoryColor, categoryColor.copy(alpha = 0.60f))
                                                } else {
                                                    listOf(categoryColor.copy(alpha = 0.85f), categoryColor.copy(alpha = 0.40f))
                                                }
                                            ),
                                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.dp,
                                            color = if (isSelected) categoryColor else Color.Transparent,
                                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // X-axis Category Icon Badge
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        color = if (isSelected) categoryColor.copy(alpha = 0.2f) else DarkBg,
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = if (isSelected) 1.dp else 0.5.dp,
                                        color = if (isSelected) categoryColor else SurfaceBorder,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(category),
                                    contentDescription = category,
                                    tint = if (isSelected) categoryColor else TextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tooltip-style interactive data display sheet
            hoveredCategory?.let { activeCat ->
                val activeAmt = spendByCategory.firstOrNull { it.first == activeCat }?.second ?: 0.0
                val activePercent = if (totalExpense > 0) (activeAmt / totalExpense).toFloat() else 0f
                val activeColor = getCategoryColor(activeCat)

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg, RoundedCornerShape(14.dp))
                        .border(1.dp, activeColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                        .testTag("recharts_tooltip_panel"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(activeColor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(activeCat),
                                contentDescription = activeCat,
                                tint = activeColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = activeCat,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", activePercent * 100)}% of total expenses",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatCurrency(activeAmt, currencySymbol),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeColor
                        )
                        Text(
                            text = "Spent this month",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetBarChartCard(
    monthlyBudgets: List<BudgetEntity>,
    categorySpending: Map<String, Double>,
    currencySymbol: String
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    
    // Set default selected category to show details initially
    LaunchedEffect(monthlyBudgets) {
        if (selectedCategory == null || monthlyBudgets.none { it.category == selectedCategory }) {
            selectedCategory = monthlyBudgets.firstOrNull()?.category
        }
    }

    val maxVal = remember(monthlyBudgets, categorySpending) {
        val list = monthlyBudgets.flatMap { budget ->
            val spent = categorySpending[budget.category] ?: 0.0
            listOf(spent, budget.limitAmount)
        }
        (list.maxOrNull() ?: 100.0).coerceAtLeast(1.0).toFloat()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title & Legends
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spending vs. Target",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                // Live Legends
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(NetYellow, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Spent", fontSize = 10.sp, color = TextSecondary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(TextSecondary.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Limit", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Scrollable Bar Row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                // Background grid lines (4 lines)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(4) {
                        HorizontalDivider(
                            color = SurfaceBorder.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    monthlyBudgets.forEach { budget ->
                        val spent = (categorySpending[budget.category] ?: 0.0).toFloat()
                        val limit = budget.limitAmount.toFloat()
                        val categoryColor = getCategoryColor(budget.category)
                        val isSelected = selectedCategory == budget.category

                        // Coerced fraction heights
                        val spentFraction = (spent / maxVal).coerceIn(0.01f, 1f)
                        val limitFraction = (limit / maxVal).coerceIn(0.01f, 1f)

                        Column(
                            modifier = Modifier
                                .width(56.dp)
                                .clickable { selectedCategory = budget.category }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            // Two vertical columns side-by-side representing spent & limit
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Spent Bar
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(spentFraction)
                                        .background(
                                            color = if (spent > limit) ExpenseRed else categoryColor,
                                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                // Limit Bar
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(limitFraction)
                                        .background(
                                            color = TextSecondary.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Indicator Icon bubble with dynamic stroke highlight
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        color = if (isSelected) categoryColor.copy(alpha = 0.25f) else DarkBg,
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) categoryColor else SurfaceBorder,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(budget.category),
                                    contentDescription = budget.category,
                                    tint = if (isSelected) categoryColor else TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Detail Drawer triggered by selected category
            selectedCategory?.let { selCat ->
                val activeBudget = monthlyBudgets.firstOrNull { it.category == selCat }
                if (activeBudget != null) {
                    val spent = categorySpending[selCat] ?: 0.0
                    val limit = activeBudget.limitAmount
                    val pct = if (limit > 0) (spent / limit) else 0.0
                    val over = spent - limit
                    val catColor = getCategoryColor(selCat)

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = SurfaceBorder, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(catColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$selCat Trend",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Spent: ${formatCurrency(spent, currencySymbol)} of ${formatCurrency(limit, currencySymbol)} target",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${(pct * 100).toInt()}% Used",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pct > 1.0) ExpenseRed else IncomeGreen
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (pct > 1.0) {
                                    "Over budget by ${formatCurrency(over, currencySymbol)}"
                                } else {
                                    "Remaining: ${formatCurrency(limit - spent, currencySymbol)}"
                                },
                                fontSize = 11.sp,
                                color = if (pct > 1.0) ExpenseRed else TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. FINANCIAL NOTES SCREEN
// ==========================================
fun getNoteCategoryAndColor(title: String, content: String): Pair<String, Color> {
    val tLower = title.lowercase()
    val cLower = content.lowercase()
    return when {
        tLower.contains("tax") || cLower.contains("tax") || tLower.contains("[tax]") -> Pair("Tax Reminder", Color(0xFF42A5F5))
        tLower.contains("saving") || tLower.contains("goal") || cLower.contains("saving") || cLower.contains("goal") || tLower.contains("[goal]") -> Pair("Savings Goal", Color(0xFF66BB6A))
        tLower.contains("spend") || tLower.contains("thought") || cLower.contains("spend") || cLower.contains("thought") || tLower.contains("[thought]") -> Pair("Spending Thought", Color(0xFFFFB74D))
        else -> Pair("General Note", NetYellow)
    }
}

@Composable
fun NotesTabScreen(
    viewModel: FinanceViewModel,
    onShowAddNote: () -> Unit
) {
    val allNotes by viewModel.allNotes.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedTagFilter by remember { mutableStateOf("All") }

    val tagFilters = listOf("All", "Tax Reminders", "Savings Goals", "Spending Thoughts")

    val filteredNotes = remember(allNotes, searchQuery, selectedTagFilter) {
        allNotes.filter { note ->
            !note.isDeleted &&
            (note.title.contains(searchQuery, ignoreCase = true) || note.content.contains(searchQuery, ignoreCase = true)) &&
            when (selectedTagFilter) {
                "All" -> true
                "Tax Reminders" -> {
                    note.title.contains("tax", ignoreCase = true) ||
                    note.content.contains("tax", ignoreCase = true) ||
                    note.title.contains("[Tax]", ignoreCase = true)
                }
                "Savings Goals" -> {
                    note.title.contains("saving", ignoreCase = true) ||
                    note.title.contains("goal", ignoreCase = true) ||
                    note.content.contains("saving", ignoreCase = true) ||
                    note.content.contains("goal", ignoreCase = true) ||
                    note.title.contains("[Goal]", ignoreCase = true)
                }
                "Spending Thoughts" -> {
                    note.title.contains("spend", ignoreCase = true) ||
                    note.title.contains("thought", ignoreCase = true) ||
                    note.content.contains("spend", ignoreCase = true) ||
                    note.content.contains("thought", ignoreCase = true) ||
                    note.title.contains("[Thought]", ignoreCase = true)
                }
                else -> true
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onShowAddNote,
                containerColor = NetYellow,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 8.dp)
                    .testTag("add_note_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Financial Note",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Notes",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Set plans and saving targets",
                fontSize = 14.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search thoughts or reminders...", color = TextSecondary, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Notes",
                        tint = NetYellow,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLabelColor = NetYellow,
                    unfocusedLabelColor = TextSecondary,
                    focusedBorderColor = NetYellow,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notes_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tag/Category chips row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tagFilters.forEach { filter ->
                    val isSelected = selectedTagFilter == filter
                    val emoji = when (filter) {
                        "All" -> "📁"
                        "Tax Reminders" -> "📄"
                        "Savings Goals" -> "🎯"
                        "Spending Thoughts" -> "💡"
                        else -> "📋"
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTagFilter = filter },
                        label = { Text("$emoji $filter", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NetYellow.copy(alpha = 0.2f),
                            selectedLabelColor = NetYellow,
                            containerColor = DarkSurface,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("note_filter_chip_$filter")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (allNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(DarkSurface, CircleShape)
                                .border(1.dp, SurfaceBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Notes Icon",
                                modifier = Modifier.size(36.dp),
                                tint = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No financial notes",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap + to capture tax reminders, savings goals, or spending thoughts.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No notes found",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try adjusting your search query or category filter.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        if (selectedTagFilter != "All") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onShowAddNote,
                                colors = ButtonDefaults.buttonColors(containerColor = NetYellow),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Add ${selectedTagFilter.removeSuffix("s")}", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filteredNotes.forEach { note ->
                        val formattedDate = remember(note.timestamp) {
                            val instant = java.time.Instant.ofEpochMilli(note.timestamp)
                            val zonedDateTime = instant.atZone(ZoneId.systemDefault())
                            zonedDateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a"))
                        }

                        // Determine note classification and color representation
                        val (categoryLabel, categoryColor) = remember(note.title, note.content) {
                            getNoteCategoryAndColor(note.title, note.content)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Visual left color accent block/indicator
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(16.dp)
                                                .background(categoryColor, RoundedCornerShape(2.dp))
                                        )
                                        Text(
                                            text = note.title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteNote(note.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Note",
                                            tint = ExpenseRed.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formattedDate,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    // Visual badge for note category / tags
                                    Box(
                                        modifier = Modifier
                                            .background(categoryColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = categoryLabel,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = categoryColor
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = note.content,
                                    fontSize = 14.sp,
                                    color = TextPrimary,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

// Voice transaction parser types and helper
data class VoiceParsedTransaction(
    val title: String,
    val amount: Double,
    val type: String,
    val category: String
)

fun parseTransactionFromVoice(spokenText: String): VoiceParsedTransaction? {
    val text = spokenText.lowercase(Locale.US)
    
    // 1. Detect Type
    val isIncome = text.contains("salary") || text.contains("earned") || text.contains("income") || 
                   text.contains("received") || text.contains("paycheck") || text.contains("gift") || 
                   text.contains("dividends") || text.contains("interest") || text.contains("refund")
    val type = if (isIncome) "INCOME" else "EXPENSE"
    
    // 2. Extract amount
    val numberPattern = """\d+(\.\d+)?""".toRegex()
    val match = numberPattern.find(text)
    var parsedAmount = 0.0
    if (match != null) {
        parsedAmount = match.value.toDoubleOrNull() ?: 0.0
    } else {
        val wordNumbers = mapOf(
            "one" to 1.0, "two" to 2.0, "three" to 3.0, "four" to 4.0, "five" to 5.0,
            "six" to 6.0, "seven" to 7.0, "eight" to 8.0, "nine" to 9.0, "ten" to 10.0,
            "twenty" to 20.0, "thirty" to 30.0, "forty" to 40.0, "fifty" to 50.0, "hundred" to 100.0
        )
        for ((word, valDouble) in wordNumbers) {
            if (text.contains(word)) {
                parsedAmount = valDouble
                break
            }
        }
    }
    
    // 3. Match category
    val categoryList = listOf("Food", "Shopping", "Transport", "Utilities", "Entertainment", "Health", "Education", "Other")
    var parsedCategory = "Other"
    
    val categorySynonyms = mapOf(
        "Food" to listOf("food", "grocery", "groceries", "restaurant", "lunch", "dinner", "breakfast", "coffee", "starbucks", "mcdonald", "eat", "snack"),
        "Shopping" to listOf("shopping", "clothe", "clothes", "shoe", "shoes", "amazon", "purchase", "bought", "store"),
        "Transport" to listOf("transport", "bus", "uber", "taxi", "train", "subway", "flight", "gas", "fuel", "car", "parking"),
        "Utilities" to listOf("utilities", "water", "electricity", "electric", "power", "internet", "phone", "bill", "rent", "gas bill"),
        "Entertainment" to listOf("entertainment", "movie", "cinema", "netflix", "concert", "game", "gaming", "pub", "bar", "party", "club"),
        "Health" to listOf("health", "medicine", "doctor", "dentist", "hospital", "pharma", "pharmacy", "medical", "clinic"),
        "Education" to listOf("education", "book", "course", "tuition", "school", "class", "training")
    )
    
    outer@ for (cat in categoryList) {
        val synonyms = categorySynonyms[cat] ?: continue
        for (syn in synonyms) {
            if (text.contains(syn)) {
                parsedCategory = cat
                break@outer
            }
        }
    }
    
    // 4. Extract title
    val keywordsToRemove = listOf("spent", "earned", "dollars", "dollar", "euros", "euro", "on", "for", "a", "an", "the", "at", "to", "from")
    var words = text.split(" ").toMutableList()
    words.removeAll { it.matches("""\d+(\.\d+)?""".toRegex()) }
    words.removeAll { keywordsToRemove.contains(it) }
    
    val parsedTitle = words.joinToString(" ").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }.trim()
    
    return if (parsedAmount > 0.0) {
        VoiceParsedTransaction(
            title = parsedTitle.ifEmpty { "Audio Transaction" },
            amount = parsedAmount,
            type = type,
            category = parsedCategory
        )
    } else {
        null
    }
}

// ==========================================
// 6. POPUPS & DIALOGS
// ==========================================

@Composable
fun ManageableDropdown(
    label: String,
    selectedValue: String,
    items: List<String>,
    onSelect: (String) -> Unit,
    onAdd: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    accentColor: Color = NetYellow
) {
    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<String?>(null) }
    var newItemName by remember { mutableStateOf("") }
    var renameNewName by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = accentColor,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .clickable { expanded = true }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedValue,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Expand dropdown",
                    tint = accentColor
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .background(DarkBg)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item,
                                    fontSize = 14.sp,
                                    color = if (item == selectedValue) accentColor else TextPrimary,
                                    fontWeight = if (item == selectedValue) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            renameNewName = item
                                            showRenameDialog = item
                                            expanded = false
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Rename item",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    
                                    if (items.size > 1) {
                                        IconButton(
                                            onClick = {
                                                onDelete(item)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete item",
                                                tint = ExpenseRed,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        onClick = {
                            onSelect(item)
                            expanded = false
                        }
                    )
                }

                HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 4.dp))

                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Option Action",
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add Option",
                                fontSize = 13.sp,
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    onClick = {
                        newItemName = ""
                        showAddDialog = true
                        expanded = false
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New Option", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = newItemName,
                    onValueChange = { newItemName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = SurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newItemName.trim().isNotEmpty()) {
                            onAdd(newItemName.trim())
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Add", color = accentColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkBg,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
        )
    }

    showRenameDialog?.let { oldName ->
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename Option", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = renameNewName,
                    onValueChange = { renameNewName = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = SurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameNewName.trim().isNotEmpty() && renameNewName.trim() != oldName) {
                            onRename(oldName, renameNewName.trim())
                            showRenameDialog = null
                        }
                    }
                ) {
                    Text("Rename", color = accentColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkBg,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    viewModel: FinanceViewModel,
    editingTransaction: TransactionEntity? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, type: String, category: String, notes: String, account: String, toAccount: String?, timestamp: Long) -> Unit
) {
    var title by remember { mutableStateOf(editingTransaction?.title ?: "") }
    var amount by remember { mutableStateOf(editingTransaction?.amount?.let { if (it == 0.0) "" else String.format(java.util.Locale.US, "%.2f", it) } ?: "") }
    var type by remember { mutableStateOf(editingTransaction?.type ?: "EXPENSE") } // EXPENSE, INCOME, or TRANSFER
    var notes by remember { mutableStateOf(editingTransaction?.notes ?: "") }
    var showCalculator by remember { mutableStateOf(false) }

    val accountsList by viewModel.accounts.collectAsState()

    // Account states
    var account by remember { mutableStateOf(editingTransaction?.account ?: "Cash") }
    var toAccount by remember { mutableStateOf<String?>(editingTransaction?.toAccount ?: "Saving") }

    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val incomeCategories by viewModel.incomeCategories.collectAsState()

    val categories = remember(type, expenseCategories, incomeCategories) {
        if (type == "EXPENSE") expenseCategories else incomeCategories
    }

    var category by remember(type, expenseCategories, incomeCategories) {
        val initialCategory = editingTransaction?.category
        mutableStateOf(
            if (initialCategory != null && initialCategory in (if (type == "EXPENSE") expenseCategories else incomeCategories)) {
                initialCategory
            } else {
                if (type == "EXPENSE") expenseCategories.firstOrNull() ?: "Food" else incomeCategories.firstOrNull() ?: "Salary"
            }
        )
    }

    // Dynamic state synchronization effects
    LaunchedEffect(accountsList) {
        if (editingTransaction == null && account !in accountsList && accountsList.isNotEmpty()) {
            account = accountsList.firstOrNull() ?: "Cash"
        }
    }

    LaunchedEffect(accountsList, account) {
        if (editingTransaction == null) {
            val otherAcc = accountsList.firstOrNull { it != account }
            if (toAccount == account || (toAccount != null && toAccount !in accountsList)) {
                toAccount = otherAcc
            }
        }
    }

    LaunchedEffect(categories) {
        if (category !in categories && categories.isNotEmpty()) {
            category = categories.firstOrNull() ?: "Other"
        }
    }

    LaunchedEffect(title) {
        if (editingTransaction == null) {
            val suggested = viewModel.suggestCategory(title)
            if (suggested != null && suggested in categories) {
                category = suggested
            }
        }
    }

    // Choose date state
    var selectedDate by remember {
        mutableStateOf(
            editingTransaction?.timestamp?.let {
                java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            } ?: viewModel.selectedDate.value
        )
    }
    val computedDayOfWeek = remember(selectedDate) {
        selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    }

    val context = LocalContext.current

    // Launcher for voice title input
    val titleSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                title = matches[0]
            }
        }
    }

    // Launcher for notes input
    val notesSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val spokenText = matches[0]
                notes = if (notes.isEmpty()) spokenText else "$notes $spokenText"
            }
        }
    }

    // Launcher for full sentence quick logging voice command
    val transactionVoiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val spokenText = matches[0]
                val parsed = parseTransactionFromVoice(spokenText)
                if (parsed != null) {
                    title = parsed.title
                    amount = parsed.amount.toString()
                    type = parsed.type
                    category = parsed.category
                    notes = "Logged via voice: \"$spokenText\""
                    android.widget.Toast.makeText(context, "Filled: ${parsed.title} (${parsed.amount})", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    title = spokenText
                    android.widget.Toast.makeText(context, "Speech copied to Title. Please manually set budget/amount.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val startSpeechToText = { launcher: androidx.activity.result.ActivityResultLauncher<Intent>, prompt: String ->
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        }
        try {
            launcher.launch(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Voice recognition is not supported on this device.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        confirmButton = {
            TextButton(
                onClick = {
                    val doubleAmount = evaluateMathExpression(amount) ?: amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotEmpty() && doubleAmount > 0) {
                        // Pass computed timestamp of chosen date
                        val timestamp = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        onSave(title, doubleAmount, type, if (type == "TRANSFER") "Transfer" else category, notes, account, if (type == "TRANSFER") toAccount else null, timestamp)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_transaction_button")
            ) {
                Text("Save", color = NetYellow, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        title = {
            Text(if (editingTransaction != null) "Edit Transaction" else "Add Transaction", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick Voice Logger card button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NetYellow.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .border(1.dp, NetYellow.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { startSpeechToText(transactionVoiceLauncher, "Say e.g., 'Spent fifteen dollars on coffee'...") }
                        .padding(12.dp)
                        .testTag("voice_logger_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Log",
                            tint = NetYellow,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Quick Voice Logger",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NetYellow
                            )
                            Text(
                                text = "Tap & say: 'spent 45 for groceries'",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Go",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 4.dp))

                // Type Switcher Button Group Supporting INCOME, EXPENSE, TRANSFER
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(19.dp))
                        .clip(RoundedCornerShape(19.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (type == "EXPENSE") ExpenseRedBg else Color.Transparent)
                            .clickable { type = "EXPENSE" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "EXPENSE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (type == "EXPENSE") ExpenseRed else TextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (type == "INCOME") IncomeGreenBg else Color.Transparent)
                            .clickable { type = "INCOME" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "INCOME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (type == "INCOME") IncomeGreen else TextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (type == "TRANSFER") NetYellowBg else Color.Transparent)
                            .clickable { 
                                type = "TRANSFER"
                                category = "Transfer"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TRANSFER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (type == "TRANSFER") NetYellow else TextSecondary
                        )
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    trailingIcon = {
                        IconButton(onClick = { startSpeechToText(titleSpeechLauncher, "Speak transaction title...") }) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice entry for title",
                                tint = NetYellow
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = NetYellow,
                        unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = NetYellow,
                        unfocusedBorderColor = SurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("transaction_title_input")
                )

                // Amount Input
                val liveResult = remember(amount) {
                    evaluateMathExpression(amount)
                }
                val hasMath = remember(amount) {
                    amount.any { it == '+' || it == '-' || it == '*' || it == '/' || it == '×' || it == '÷' || it == '−' }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    trailingIcon = {
                        IconButton(
                            onClick = { showCalculator = !showCalculator },
                            modifier = Modifier.testTag("calculator_toggle_icon")
                        ) {
                            Icon(
                                imageVector = if (showCalculator) Icons.Default.Keyboard else Icons.Default.Calculate,
                                contentDescription = "Toggle Calculator Keyboard",
                                tint = NetYellow
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = NetYellow,
                        unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = NetYellow,
                        unfocusedBorderColor = SurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("transaction_amount_input")
                )

                // Live Preview value if it represents math
                if (hasMath && liveResult != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "= " + String.format(Locale.US, "%.2f", liveResult),
                            fontSize = 13.sp,
                            color = NetYellow,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Calculator Keypad Segment
                AnimatedVisibility(
                    visible = showCalculator,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface, RoundedCornerShape(16.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val row1 = listOf("7", "8", "9", "÷")
                        val row2 = listOf("4", "5", "6", "×")
                        val row3 = listOf("1", "2", "3", "−")
                        val row4 = listOf("C", "0", ".", "+")
                        val row5 = listOf("(", ")", "⌫", "=")

                        val allRows = listOf(row1, row2, row3, row4, row5)

                        allRows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { char ->
                                    val isOperator = char in listOf("÷", "×", "−", "+", "=")
                                    val isClearDelete = char in listOf("C", "⌫")
                                    
                                    val buttonColor = when {
                                        char == "=" -> NetYellow
                                        isOperator -> DarkBg
                                        isClearDelete -> ExpenseRed.copy(alpha = 0.15f)
                                        else -> DarkBg
                                    }
                                    
                                    val textColor = when {
                                        char == "=" -> DarkBg
                                        isOperator -> NetYellow
                                        isClearDelete -> ExpenseRed
                                        else -> TextPrimary
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .background(buttonColor, RoundedCornerShape(8.dp))
                                            .border(
                                                1.dp, 
                                                if (char == "=") Color.Transparent else SurfaceBorder, 
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                when (char) {
                                                    "C" -> amount = ""
                                                    "⌫" -> {
                                                        if (amount.isNotEmpty()) {
                                                            amount = amount.dropLast(1)
                                                        }
                                                    }
                                                    "=" -> {
                                                        if (liveResult != null) {
                                                            val formatted = if (liveResult % 1 == 0.0) {
                                                                liveResult.toInt().toString()
                                                            } else {
                                                                String.format(Locale.US, "%.2f", liveResult)
                                                            }
                                                            amount = formatted
                                                        }
                                                    }
                                                    else -> {
                                                        amount += char
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = char,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Date & Day Selector Field (Tappable Card opening DatePickerDialog)
                val context = LocalContext.current
                Text(
                    text = "Transaction Date & Day",
                    fontSize = 12.sp,
                    color = NetYellow,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val dialog = DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                                },
                                selectedDate.year,
                                selectedDate.monthValue - 1,
                                selectedDate.dayOfMonth
                            )
                            dialog.show()
                        }
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp)),
                    color = DarkSurface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Date: ${selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                                fontSize = 14.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Day: $computedDayOfWeek",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Select Date",
                            tint = NetYellow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Dropdowns for Categories and Accounts (For EXPENSE, INCOME or TRANSFER)
                if (type != "TRANSFER") {
                    ManageableDropdown(
                        label = if (type == "EXPENSE") "Pay From Account" else "Receive In Account",
                        selectedValue = account,
                        items = accountsList,
                        onSelect = { account = it },
                        onAdd = { newAcc ->
                            viewModel.addAccount(newAcc)
                            account = newAcc
                        },
                        onRename = { old, new ->
                            viewModel.renameAccount(old, new)
                            if (account == old) account = new
                        },
                        onDelete = { del ->
                            viewModel.deleteAccount(del)
                            if (account == del) {
                                account = accountsList.filter { it != del }.firstOrNull() ?: "Cash"
                            }
                        },
                        accentColor = if (type == "EXPENSE") ExpenseRed else IncomeGreen
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ManageableDropdown(
                        label = "Category",
                        selectedValue = category,
                        items = categories,
                        onSelect = { category = it },
                        onAdd = { newCat ->
                            if (type == "EXPENSE") {
                                viewModel.addExpenseCategory(newCat)
                            } else {
                                viewModel.addIncomeCategory(newCat)
                            }
                            category = newCat
                        },
                        onRename = { old, new ->
                            if (type == "EXPENSE") {
                                viewModel.renameExpenseCategory(old, new)
                            } else {
                                viewModel.renameIncomeCategory(old, new)
                            }
                            if (category == old) category = new
                        },
                        onDelete = { del ->
                            if (type == "EXPENSE") {
                                viewModel.deleteExpenseCategory(del)
                            } else {
                                viewModel.deleteIncomeCategory(del)
                            }
                            if (category == del) {
                                val remaining = categories.filter { it != del }
                                category = remaining.firstOrNull() ?: "Other"
                            }
                        },
                        accentColor = getCategoryColor(category)
                    )
                } else {
                    // Two dropdowns side by side for Transfer (Source & Target)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            ManageableDropdown(
                                label = "Source Account",
                                selectedValue = account,
                                items = accountsList,
                                onSelect = { src ->
                                    account = src
                                    if (toAccount == src) {
                                        toAccount = accountsList.firstOrNull { it != src }
                                    }
                                },
                                onAdd = { newAcc ->
                                    viewModel.addAccount(newAcc)
                                    account = newAcc
                                },
                                onRename = { old, new ->
                                    viewModel.renameAccount(old, new)
                                    if (account == old) account = new
                                },
                                onDelete = { del ->
                                    viewModel.deleteAccount(del)
                                    if (account == del) {
                                        account = accountsList.filter { it != del }.firstOrNull() ?: "Cash"
                                    }
                                },
                                accentColor = NetYellow
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            val targets = remember(accountsList, account) {
                                accountsList.filter { it != account }
                            }
                            ManageableDropdown(
                                label = "Target Account",
                                selectedValue = toAccount ?: "",
                                items = targets,
                                onSelect = { toAccount = it },
                                onAdd = { newAcc ->
                                    viewModel.addAccount(newAcc)
                                    toAccount = newAcc
                                },
                                onRename = { old, new ->
                                    viewModel.renameAccount(old, new)
                                    if (toAccount == old) toAccount = new
                                },
                                onDelete = { del ->
                                    viewModel.deleteAccount(del)
                                    if (toAccount == del) {
                                        toAccount = targets.filter { it != del }.firstOrNull()
                                    }
                                },
                                accentColor = Purple80
                            )
                        }
                    }
                }

                // Notes Input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    trailingIcon = {
                        IconButton(onClick = { startSpeechToText(notesSpeechLauncher, "Speak transaction notes...") }) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice entry for notes",
                                tint = NetYellow
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = NetYellow,
                        unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = NetYellow,
                        unfocusedBorderColor = SurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("transaction_notes_input")
                )
            }
        },
        containerColor = DarkBg,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
    )
}

@Composable
fun AddBudgetDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
    onSave: (category: String, limit: Double, alertThreshold: Double) -> Unit
) {
    val categories by viewModel.expenseCategories.collectAsState()
    var category by remember(categories) { mutableStateOf(categories.firstOrNull() ?: "Food") }
    var limit by remember { mutableStateOf("") }
    var alertThreshold by remember { mutableStateOf(80f) } // Default threshold percentage

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val dLimit = limit.toDoubleOrNull() ?: 0.0
                    if (dLimit > 0) {
                        onSave(category, dLimit, alertThreshold.toDouble())
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_budget_button")
            ) {
                Text("Set", color = NetYellow, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        title = {
            Text("Set Category Budget", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Limit Input
                OutlinedTextField(
                    value = limit,
                    onValueChange = { limit = it },
                    label = { Text("Monthly Limit ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = NetYellow,
                        unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = NetYellow,
                        unfocusedBorderColor = SurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("budget_limit_input")
                )

                // Warning Threshold Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Alert Threshold",
                            fontSize = 12.sp,
                            color = NetYellow,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${alertThreshold.toInt()}%",
                            fontSize = 13.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Slider(
                        value = alertThreshold,
                        onValueChange = { alertThreshold = it },
                        valueRange = 50f..100f,
                        steps = 9, // Increments of 5%
                        colors = SliderDefaults.colors(
                            thumbColor = NetYellow,
                            activeTrackColor = NetYellow,
                            inactiveTrackColor = SurfaceBorder,
                            activeTickColor = NetYellow,
                            inactiveTickColor = SurfaceBorder
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("budget_threshold_slider")
                    )
                    Text(
                        text = "Trigger warning when spending exceeds ${alertThreshold.toInt()}% of budget.",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                ManageableDropdown(
                    label = "Select Category",
                    selectedValue = category,
                    items = categories,
                    onSelect = { category = it },
                    onAdd = { newCat ->
                        viewModel.addExpenseCategory(newCat)
                        category = newCat
                    },
                    onRename = { old, new ->
                        viewModel.renameExpenseCategory(old, new)
                        if (category == old) category = new
                    },
                    onDelete = { del ->
                        viewModel.deleteExpenseCategory(del)
                        if (category == del) {
                            val remaining = categories.filter { it != del }
                            category = remaining.firstOrNull() ?: "Other"
                        }
                    },
                    accentColor = getCategoryColor(category)
                )
            }
        },
        containerColor = DarkBg,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
    )
}

@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, timestamp: Long) -> Unit,
    initialTimestamp: Long = System.currentTimeMillis()
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    var selectedDate by remember {
        val dateValue = java.time.Instant.ofEpochMilli(initialTimestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        mutableStateOf(dateValue)
    }
    val computedDayOfWeek = remember(selectedDate) {
        selectedDate.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
    }

    val context = LocalContext.current

    // Speech recognition launchers for Title & Content
    val titleSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                title = matches[0]
            }
        }
    }

    val contentSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val spokenText = matches[0]
                content = if (content.isEmpty()) spokenText else "$content $spokenText"
            }
        }
    }

    val startSpeechToText = { launcher: androidx.activity.result.ActivityResultLauncher<Intent>, prompt: String ->
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        }
        try {
            launcher.launch(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Voice recording/Recognizer not supported on this device", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotEmpty() && content.isNotEmpty()) {
                        val computedTimestamp = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        onSave(title, content, computedTimestamp)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_note_button")
            ) {
                Text("Save", color = NetYellow, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        title = {
            Text("Create Note & Reflection", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick templates section
                Text(
                    text = "Idea Templates (Tap to prefill):",
                    fontSize = 11.sp,
                    color = NetYellow,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val templates = listOf(
                        Triple("📄 Tax Reminder", "[Tax] Tax Reminder", "• Tax Year: 2026\n• Deadline: April 15, 2026\n• Required Docs:\n  - W-2 Form\n  - Business Expense Receipts\n• Next action:"),
                        Triple("🎯 Savings Goal", "[Goal] Savings Goal", "• Financial Goal: Emergency Fund\n• Target Amount: $5,000.00\n• Deadline date:\n• Weekly savings pace:\n• Action steps:"),
                        Triple("💡 Spending Thought", "[Thought] Spending Thought", "• Item to buy:\n• Estimated Cost: $\n• Why I want this item:\n• Do I really need it now?\n• Mindful waiting period: 30-day delay challenge.")
                    )

                    templates.forEach { (labelText, tVal, cVal) ->
                        Box(
                            modifier = Modifier
                                .background(DarkSurface, RoundedCornerShape(8.dp))
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    title = tVal
                                    content = cVal
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = labelText,
                                fontSize = 11.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title") },
                    trailingIcon = {
                        IconButton(onClick = { startSpeechToText(titleSpeechLauncher, "Speak your note title...") }) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice input for Title",
                                tint = NetYellow
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = NetYellow,
                        unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = NetYellow,
                        unfocusedBorderColor = SurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("note_title_input")
                )

                // Date & Day selector inside Creating Note / Reflection
                val context = LocalContext.current
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val dialog = DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                                },
                                selectedDate.year,
                                selectedDate.monthValue - 1,
                                selectedDate.dayOfMonth
                            )
                            dialog.show()
                        }
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp)),
                    color = DarkSurface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Date: ${selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                                fontSize = 13.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Day: $computedDayOfWeek",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Select Date",
                            tint = NetYellow,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content / Reflection") },
                    minLines = 3,
                    trailingIcon = {
                        IconButton(onClick = { startSpeechToText(contentSpeechLauncher, "Speak your note description...") }) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice input for description content",
                                tint = NetYellow
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = NetYellow,
                        unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = NetYellow,
                        unfocusedBorderColor = SurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("note_content_input")
                )
            }
        },
        containerColor = DarkBg,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterTransactionsDialog(
    currentType: String,
    currentCategory: String,
    onDismiss: () -> Unit,
    onApply: (type: String, category: String) -> Unit
) {
    var selectedType by remember { mutableStateOf(currentType) }
    var selectedCategory by remember { mutableStateOf(currentCategory) }

    val categories = listOf("ALL", "Food", "Shopping", "Transport", "Utilities", "Entertainment", "Health", "Education", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(selectedType, selectedCategory)
                    onDismiss()
                },
                modifier = Modifier.testTag("apply_filter_button")
            ) {
                Text("Apply", color = NetYellow, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    // Reset to ALL
                    onApply("ALL", "ALL")
                    onDismiss()
                }
            ) {
                Text("Reset Filters", color = ExpenseRed)
            }
        },
        title = {
            Text("Filter Activities", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Type Filter Group
                Text("Transaction Type", fontSize = 13.sp, color = NetYellow, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL", "INCOME", "EXPENSE").forEach { type ->
                        val isSelected = selectedType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedType = type },
                            label = { Text(type, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NetYellow.copy(alpha = 0.2f),
                                selectedLabelColor = NetYellow
                            )
                        )
                    }
                }

                // Category Filter Group
                Text("Category", fontSize = 13.sp, color = NetYellow, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val chunks = categories.chunked(3)
                    chunks.forEach { rowCategories ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowCategories.forEach { cat ->
                                val isSelected = selectedCategory == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (cat == "ALL") NetYellow.copy(alpha = 0.2f) else getCategoryColor(cat).copy(alpha = 0.2f),
                                        selectedLabelColor = if (cat == "ALL") NetYellow else getCategoryColor(cat)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = DarkBg,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
    )
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NetYellow,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
fun CalculatorToolView() {
    var expression by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Output screen
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(DarkBg, RoundedCornerShape(12.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = expression.ifEmpty { "0" },
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                textAlign = TextAlign.End
            )
            if (resultText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "= $resultText",
                    color = NetYellow,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End
                )
            }
        }

        // Keypad grid
        val buttons = listOf(
            listOf("C", "(", ")", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "⌫", "=")
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            buttons.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { symbol ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.2f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (symbol) {
                                        "=" -> NetYellow
                                        "C" -> ExpenseRed.copy(alpha = 0.2f)
                                        "+", "−", "×", "÷" -> SurfaceBorder
                                        else -> DarkSurface
                                    }
                                )
                                .border(
                                    1.dp,
                                    when (symbol) {
                                        "=" -> NetYellow
                                        "C" -> ExpenseRed.copy(alpha = 0.4f)
                                        else -> SurfaceBorder
                                    },
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    when (symbol) {
                                        "C" -> {
                                            expression = ""
                                            resultText = ""
                                        }
                                        "⌫" -> {
                                            if (expression.isNotEmpty()) {
                                                expression = expression.dropLast(1)
                                            }
                                        }
                                        "=" -> {
                                            val eval = evaluateMathExpression(expression)
                                            if (eval != null && !eval.isNaN()) {
                                                resultText = if (eval % 1.0 == 0.0) {
                                                    eval.toInt().toString()
                                                } else {
                                                    String.format(Locale.US, "%.4f", eval).trimEnd('0').trimEnd('.')
                                                }
                                            } else {
                                                resultText = "Error"
                                            }
                                        }
                                        else -> {
                                            expression += symbol
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = symbol,
                                color = when (symbol) {
                                    "=" -> DarkBg
                                    "C" -> ExpenseRed
                                    "+", "−", "×", "÷" -> NetYellow
                                    else -> TextPrimary
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorldClockToolView() {
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            tick++
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("world_clock_tool_prefs", android.content.Context.MODE_PRIVATE) }
    
    val defaultCities = listOf(
        "London|Europe/London|🇬🇧 GMT/BST",
        "New York|America/New_York|🇺🇸 EST/EDT",
        "Tokyo|Asia/Tokyo|🇯🇵 JST",
        "Sydney|Australia/Sydney|🇦🇺 AEST/AEDT",
        "Dubai|Asia/Dubai|🇦🇪 GST",
        "Paris|Europe/Paris|🇫🇷 CET/CEST"
    )

    var activeCitiesStringList by remember {
        val saved = prefs.getString("active_cities_list", null)
        val initialList = saved?.split(",")?.filter { it.isNotEmpty() } ?: defaultCities
        mutableStateOf(initialList)
    }

    var primaryTzString by remember {
        val saved = prefs.getString("world_clock_primary_tz", ZoneId.systemDefault().id)
        mutableStateOf(saved ?: ZoneId.systemDefault().id)
    }

    var is24HourFormat by remember {
        val saved = prefs.getBoolean("world_clock_use_24h", false)
        mutableStateOf(saved)
    }

    fun addCity(name: String, tzId: String, desc: String) {
        val newItem = "$name|$tzId|$desc"
        if (!activeCitiesStringList.any { it.contains("|$tzId|") }) {
            val newList = activeCitiesStringList + newItem
            activeCitiesStringList = newList
            prefs.edit().putString("active_cities_list", newList.joinToString(",")).apply()
        }
    }

    fun removeCity(tzId: String) {
        val newList = activeCitiesStringList.filter { !it.contains("|$tzId|") }
        activeCitiesStringList = newList
        prefs.edit().putString("active_cities_list", newList.joinToString(",")).apply()
        if (primaryTzString == tzId) {
            val fallback = ZoneId.systemDefault().id
            primaryTzString = fallback
            prefs.edit().putString("world_clock_primary_tz", fallback).apply()
        }
    }

    val localZone = remember { ZoneId.systemDefault() }

    val masterCities = remember {
        listOf(
            Triple("London", "Europe/London", "🇬🇧 GMT/BST"),
            Triple("New York", "America/New_York", "🇺🇸 EST/EDT"),
            Triple("Tokyo", "Asia/Tokyo", "🇯🇵 JST"),
            Triple("Sydney", "Australia/Sydney", "🇦🇺 AEST/AEDT"),
            Triple("Dubai", "Asia/Dubai", "🇦🇪 GST"),
            Triple("Paris", "Europe/Paris", "🇫🇷 CET/CEST"),
            Triple("Singapore", "Asia/Singapore", "🇸🇬 SGT"),
            Triple("Hong Kong", "Asia/Hong_Kong", "🇭🇰 HKT"),
            Triple("Mumbai", "Asia/Kolkata", "🇮🇳 IST"),
            Triple("Shanghai", "Asia/Shanghai", "🇨🇳 CST"),
            Triple("Cairo", "Africa/Cairo", "🇪🇬 EET/EEST"),
            Triple("Moscow", "Europe/Moscow", "🇷🇺 MSK"),
            Triple("Los Angeles", "America/Los_Angeles", "🇺🇸 PST/PDT"),
            Triple("Chicago", "America/Chicago", "🇺🇸 CST/CDT"),
            Triple("Sao Paulo", "America/Sao_Paulo", "🇧🇷 BRT/BRST"),
            Triple("Cape Town", "Africa/Johannesburg", "🇿🇦 SAST"),
            Triple("Istanbul", "Europe/Istanbul", "🇹🇷 TRT"),
            Triple("Seoul", "Asia/Seoul", "🇰🇷 KST"),
            Triple("Bangkok", "Asia/Bangkok", "🇹🇭 ICT"),
            Triple("Auckland", "Pacific/Auckland", "🇳🇿 NZST/NZDT"),
            Triple("Vancouver", "America/Vancouver", "🇨🇦 PST/PDT"),
            Triple("Toronto", "America/Toronto", "🇨🇦 EST/EDT"),
            Triple("Mexico City", "America/Mexico_City", "🇲🇽 CST/CDT"),
            Triple("Riyadh", "Asia/Riyadh", "🇸🇦 AST"),
            Triple("Rome", "Europe/Rome", "🇮🇹 CET/CEST"),
            Triple("Berlin", "Europe/Berlin", "🇩🇪 CET/CEST"),
            Triple("Athens", "Europe/Athens", "🇬🇷 EET/EEST"),
            Triple("Reykjavik", "Atlantic/Reykjavik", "🇮🇸 GMT"),
            Triple("Anchorage", "America/Anchorage", "🇺🇸 AKST/AKDT"),
            Triple("Lima", "America/Lima", "🇵🇪 PET"),
            Triple("Nairobi", "Africa/Nairobi", "🇰🇪 EAT"),
            Triple("Jakarta", "Asia/Jakarta", "🇮🇩 WITA"),
            Triple("Taipei", "Asia/Taipei", "🇹🇼 NST"),
            Triple("Manila", "Asia/Manila", "🇵🇭 PHT")
        )
    }

    val systemTimeZones = remember { ZoneId.getAvailableZoneIds().sorted() }
    var searchQuery by remember { mutableStateOf("") }

    val filteredMaster = if (searchQuery.isBlank()) {
        emptyList()
    } else {
        masterCities.filter { (name, tz, desc) ->
            name.contains(searchQuery, ignoreCase = true) ||
            tz.contains(searchQuery, ignoreCase = true) ||
            desc.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredSystemTimeZones = if (searchQuery.length >= 3) {
        systemTimeZones.filter { tzId ->
            tzId.contains(searchQuery, ignoreCase = true) &&
            !masterCities.any { it.second.equals(tzId, ignoreCase = true) }
        }.take(8).map { tzId ->
            val name = tzId.substringAfterLast('/').replace('_', ' ')
            Triple(name, tzId, "🌐 $tzId")
        }
    } else {
        emptyList()
    }

    val allSearchResults = (filteredMaster + filteredSystemTimeZones).distinctBy { it.second }

    val activeCitiesTriplets = activeCitiesStringList.map {
        val parts = it.split("|")
        val name = parts.getOrNull(0) ?: ""
        val tzId = parts.getOrNull(1) ?: "UTC"
        val desc = parts.getOrNull(2) ?: ""
        Triple(name, tzId, desc)
    }

    val primaryTzData = remember(primaryTzString, activeCitiesTriplets) {
        activeCitiesTriplets.find { it.second == primaryTzString }
            ?: masterCities.find { it.second == primaryTzString }
            ?: Triple("System Local Time", ZoneId.systemDefault().id, "🌐 System timezone")
    }

    val primaryZone = remember(primaryTzString, tick) {
        try { ZoneId.of(primaryTzString) } catch (e: Exception) { ZoneId.systemDefault() }
    }
    val primaryTime = java.time.ZonedDateTime.now(primaryZone)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // High-Visibility Primary Timezone Clock Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("primary_timezone_clock_card"),
            colors = CardDefaults.cardColors(containerColor = NetYellow.copy(alpha = 0.08f)),
            border = BorderStroke(1.5.dp, NetYellow.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Primary Clock Icon",
                        tint = NetYellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "PRIMARY TIMEZONE CLOCK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NetYellow,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // High-visibility large digital watch face
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = primaryTime.format(DateTimeFormatter.ofPattern(if (is24HourFormat) "HH:mm" else "hh:mm")),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = TextPrimary
                    )
                    Text(
                        text = ":" + primaryTime.format(DateTimeFormatter.ofPattern("ss")),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = NetYellow,
                        modifier = Modifier.padding(bottom = 5.dp, start = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (is24HourFormat) "24H" else primaryTime.format(DateTimeFormatter.ofPattern("a")),
                        fontSize = if (is24HourFormat) 12.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (is24HourFormat) NetYellow.copy(alpha = 0.6f) else TextSecondary,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = primaryTzData.first,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Text(
                    text = primaryTzData.third,
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Text(
                    text = primaryTime.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
        }

        // 12/24 Hour Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, SurfaceBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Time Format",
                        tint = NetYellow,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "24-Hour Format",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Display all times in 24-hour style",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
                Switch(
                    checked = is24HourFormat,
                    onCheckedChange = { checked ->
                        is24HourFormat = checked
                        prefs.edit().putBoolean("world_clock_use_24h", checked).apply()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBg,
                        checkedTrackColor = NetYellow,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = SurfaceBorder
                    ),
                    modifier = Modifier.testTag("world_clock_24h_toggle")
                )
            }
        }

        // Local System Time card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, SurfaceBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("YOUR LOCAL SYSTEM TIME", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = java.time.ZonedDateTime.now(localZone).format(DateTimeFormatter.ofPattern(if (is24HourFormat) "HH:mm:ss" else "hh:mm:ss a")),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = java.time.ZonedDateTime.now(localZone).format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Search Section
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("world_clock_search_input"),
            placeholder = { Text("Search city, country, or timezone...", color = TextSecondary, fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = NetYellow,
                unfocusedBorderColor = SurfaceBorder,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Search Results List
        if (searchQuery.isNotEmpty()) {
            Text("Search Results (${allSearchResults.size})", fontSize = 12.sp, color = NetYellow, fontWeight = FontWeight.Bold)
            if (allSearchResults.isEmpty()) {
                Text(
                    "No matching locations found.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, SurfaceBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allSearchResults.forEach { (cityName, tzId, desc) ->
                            val isAlreadyAdded = activeCitiesStringList.any { it.contains("|$tzId|") }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cityName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(desc, fontSize = 10.sp, color = TextSecondary)
                                }

                                if (isAlreadyAdded) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                primaryTzString = tzId
                                                prefs.edit().putString("world_clock_primary_tz", tzId).apply()
                                            },
                                            modifier = Modifier.testTag("search_set_primary_$tzId")
                                        ) {
                                            Icon(
                                                imageVector = if (primaryTzString == tzId) Icons.Default.Star else Icons.Default.StarBorder,
                                                contentDescription = "Set as Primary",
                                                tint = if (primaryTzString == tzId) NetYellow else TextSecondary.copy(alpha = 0.6f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { removeCity(tzId) },
                                            modifier = Modifier.testTag("remove_via_search_$tzId")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove City",
                                                tint = ExpenseRed.copy(alpha = 0.8f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                } else {
                                    IconButton(
                                        onClick = { addCity(cityName, tzId, desc) },
                                        modifier = Modifier.testTag("add_via_search_$tzId")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add City",
                                            tint = NetYellow,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Text("Global Cities", fontSize = 13.sp, color = NetYellow, fontWeight = FontWeight.Bold)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            activeCitiesTriplets.forEach { (cityName, tzId, desc) ->
                val cityZone = try { ZoneId.of(tzId) } catch (e: Exception) { ZoneId.of("UTC") }
                val cityTime = java.time.ZonedDateTime.now(cityZone)
                val localTime = java.time.ZonedDateTime.now(localZone)
                
                val diffHours = java.time.temporal.ChronoUnit.HOURS.between(localTime.toOffsetDateTime(), cityTime.toOffsetDateTime())
                val diffString = when {
                    diffHours == 0L -> "Same timezone"
                    diffHours > 0L -> "+${diffHours} hrs ahead"
                    else -> "${diffHours} hrs behind"
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, SurfaceBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = {
                                    primaryTzString = tzId
                                    prefs.edit().putString("world_clock_primary_tz", tzId).apply()
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("set_primary_star_$tzId")
                            ) {
                                Icon(
                                    imageVector = if (primaryTzString == tzId) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Set as Primary",
                                    tint = if (primaryTzString == tzId) NetYellow else TextSecondary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { removeCity(tzId) },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("remove_city_button_$tzId")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = ExpenseRed.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(cityName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    if (primaryTzString == tzId) {
                                        Box(
                                            modifier = Modifier
                                                .background(NetYellow.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                        ) {
                                            Text("PRIMARY", fontSize = 8.sp, color = NetYellow, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(desc, fontSize = 10.sp, color = TextSecondary)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = cityTime.format(DateTimeFormatter.ofPattern(if (is24HourFormat) "HH:mm" else "hh:mm a")),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = NetYellow
                            )
                            Text(
                                text = "$diffString, ${cityTime.format(DateTimeFormatter.ofPattern("MMM dd"))}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateCalculatorToolView() {
    val context = LocalContext.current
    var subTab by remember { mutableStateOf("DAYS_BETWEEN") }

    var dateStart by remember { mutableStateOf(LocalDate.now()) }
    var dateEnd by remember { mutableStateOf(LocalDate.now().plusDays(7)) }

    var dateBase by remember { mutableStateOf(LocalDate.now()) }
    var offsetInput by remember { mutableStateOf("10") }
    var offsetOperation by remember { mutableStateOf("ADD") } // ADD or SUBTRACT

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (subTab == "DAYS_BETWEEN") NetYellow.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { subTab = "DAYS_BETWEEN" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Days Between",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (subTab == "DAYS_BETWEEN") NetYellow else TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (subTab == "ADD_SUBTRACT") NetYellow.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { subTab = "ADD_SUBTRACT" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Add / Subtract",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (subTab == "ADD_SUBTRACT") NetYellow else TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (subTab == "DAYS_BETWEEN") {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Column {
                    Text("START DATE", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface, RoundedCornerShape(8.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d -> dateStart = LocalDate.of(y, m + 1, d) },
                                    dateStart.year, dateStart.monthValue - 1, dateStart.dayOfMonth
                                ).show()
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(dateStart.format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy")), color = TextPrimary, fontSize = 13.sp)
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = NetYellow, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Column {
                    Text("END DATE", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface, RoundedCornerShape(8.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d -> dateEnd = LocalDate.of(y, m + 1, d) },
                                    dateEnd.year, dateEnd.monthValue - 1, dateEnd.dayOfMonth
                                ).show()
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(dateEnd.format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy")), color = TextPrimary, fontSize = 13.sp)
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = NetYellow, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val daysDiff = remember(dateStart, dateEnd) {
                    java.time.temporal.ChronoUnit.DAYS.between(dateStart, dateEnd)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NetYellow.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, NetYellow),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("CALCULATED DURATION", fontSize = 10.sp, color = NetYellow, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        val absoluteDays = kotlin.math.abs(daysDiff)
                        val suffix = if (daysDiff < 0) " (Start after End)" else ""
                        
                        Text(
                            text = "$absoluteDays Days",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = "${absoluteDays / 7} Weeks and ${absoluteDays % 7} Days$suffix",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Column {
                    Text("START BASE DATE", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface, RoundedCornerShape(8.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d -> dateBase = LocalDate.of(y, m + 1, d) },
                                    dateBase.year, dateBase.monthValue - 1, dateBase.dayOfMonth
                                ).show()
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(dateBase.format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy")), color = TextPrimary, fontSize = 13.sp)
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = NetYellow, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (offsetOperation == "ADD") IncomeGreenBg else DarkSurface, RoundedCornerShape(6.dp))
                            .border(1.dp, if (offsetOperation == "ADD") IncomeGreen else SurfaceBorder, RoundedCornerShape(6.dp))
                            .clickable { offsetOperation = "ADD" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Add Days", color = if (offsetOperation == "ADD") IncomeGreen else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (offsetOperation == "SUBTRACT") ExpenseRedBg else DarkSurface, RoundedCornerShape(6.dp))
                            .border(1.dp, if (offsetOperation == "SUBTRACT") ExpenseRed else SurfaceBorder, RoundedCornerShape(6.dp))
                            .clickable { offsetOperation = "SUBTRACT" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Subtract Days", color = if (offsetOperation == "SUBTRACT") ExpenseRed else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = offsetInput,
                    onValueChange = { offsetInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Number of Days", fontSize = 11.sp, color = TextSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NetYellow,
                        unfocusedBorderColor = SurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                val calculatedDate = remember(dateBase, offsetInput, offsetOperation) {
                    val days = offsetInput.toLongOrNull() ?: 0L
                    if (offsetOperation == "ADD") dateBase.plusDays(days) else dateBase.minusDays(days)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NetYellow.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, NetYellow),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("TARGET RESULTING DATE", fontSize = 10.sp, color = NetYellow, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = calculatedDate.format(DateTimeFormatter.ofPattern("EEEE")),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = NetYellow
                        )
                        Text(
                            text = calculatedDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                            fontSize = 16.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GramScaleToolView(currencySymbol: String) {
    var weightInput by remember { mutableStateOf("10.00") }
    var pricePerGramInput by remember { mutableStateOf("5.50") }

    val weightDouble = weightInput.toDoubleOrNull() ?: 0.0
    val priceDouble = pricePerGramInput.toDoubleOrNull() ?: 0.0
    val totalCost = weightDouble * priceDouble

    val templates = listOf(
        "Gold Ring" to 8.0,
        "Silver Ring" to 11.5,
        "Spoon" to 50.0,
        "Phone" to 200.0,
        "Coffee" to 250.0,
        "Sugar" to 500.0,
        "Rice" to 1000.0
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, SurfaceBorder),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "VIRTUAL PRECISION BALANCE",
                    fontSize = 10.sp,
                    color = NetYellow,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(DarkBg, CircleShape)
                        .border(2.dp, NetYellow, CircleShape)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format(Locale.US, "%.2f", weightDouble),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "grams",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(-10, -1).forEach { delta ->
                            Button(
                                onClick = {
                                    val newWValue = maxOf(0.0, weightDouble + delta)
                                    weightInput = String.format(Locale.US, "%.2f", newWValue)
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .size(width = 44.dp, height = 28.dp)
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(6.dp)),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkBg),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "$delta",
                                    fontSize = 11.sp,
                                    color = ExpenseRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(1, 10).forEach { delta ->
                            Button(
                                onClick = {
                                    val newWValue = weightDouble + delta
                                    weightInput = String.format(Locale.US, "%.2f", newWValue)
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .size(width = 44.dp, height = 28.dp)
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(6.dp)),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkBg),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "+$delta",
                                    fontSize = 11.sp,
                                    color = IncomeGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Column {
            Text("Presets", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                templates.forEach { (name, value) ->
                    Box(
                        modifier = Modifier
                            .background(DarkSurface, RoundedCornerShape(8.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                weightInput = String.format(Locale.US, "%.2f", value)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$name (${value.toInt()}g)",
                            fontSize = 10.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = weightInput,
                onValueChange = { weightInput = it },
                label = { Text("Weight (g)", fontSize = 10.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = NetYellow,
                    unfocusedBorderColor = SurfaceBorder
                ),
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = pricePerGramInput,
                onValueChange = { pricePerGramInput = it },
                label = { Text("Price ($currencySymbol/g)", fontSize = 10.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = NetYellow,
                    unfocusedBorderColor = SurfaceBorder
                ),
                modifier = Modifier.weight(1f)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NetYellow.copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, NetYellow),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("TOTAL COST", fontSize = 10.sp, color = NetYellow, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${String.format(Locale.US, "%.2fg", weightDouble)} × $currencySymbol${String.format(Locale.US, "%.2f", priceDouble)}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Text(
                    text = formatCurrency(totalCost, currencySymbol),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NetYellow
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    viewModel: FinanceViewModel,
    currentCurrency: String,
    alertsEnabled: Boolean,
    onDismiss: () -> Unit,
    onCurrencyChange: (String) -> Unit,
    onAlertsToggle: (Boolean) -> Unit,
    onClearAll: () -> Unit,
    currentCurrencyCode: String = "USD",
    isFetchingRates: Boolean = false,
    apiError: String? = null,
    lastFetchedTime: Long = 0L,
    onRefreshRates: () -> Unit = {},
    // Cloud Sync Configurations
    syncEnabled: Boolean = true,
    onSyncToggle: (Boolean) -> Unit = {},
    firebaseUrl: String = "",
    onUrlChange: (String) -> Unit = {},
    syncEmail: String = "",
    onEmailChange: (String) -> Unit = {},
    isSyncing: Boolean = false,
    lastSyncedTime: Long = 0L,
    syncError: String? = null,
    onManualSync: () -> Unit = {}
) {
    val context = LocalContext.current

    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val incomeCategories by viewModel.incomeCategories.collectAsState()

    var categoryType by remember { mutableStateOf("EXPENSE") } // EXPENSE or INCOME
    val currentCategories = if (categoryType == "EXPENSE") expenseCategories else incomeCategories
    var newCategoryName by remember { mutableStateOf("") }
    var categoryToRename by remember { mutableStateOf<String?>(null) }
    var renameValue by remember { mutableStateOf("") }

    // Read Redeclared Preference States from ViewModel
    val profileName by viewModel.profileName.collectAsState()
    val profileEmail by viewModel.profileEmail.collectAsState()
    val numberFormat by viewModel.numberFormat.collectAsState()
    val firstDayOfWeek by viewModel.firstDayOfWeek.collectAsState()
    val budgetCycle by viewModel.budgetCycle.collectAsState()
    val rolloverEnabled by viewModel.rolloverEnabled.collectAsState()
    val overspendingThreshold by viewModel.overspendingThreshold.collectAsState()
    val billRemindersEnabled by viewModel.billRemindersEnabled.collectAsState()
    val dailyRecapEnabled by viewModel.dailyRecapEnabled.collectAsState()
    val lowBalanceAlertsEnabled by viewModel.lowBalanceAlertsEnabled.collectAsState()
    val paymentConfirmationsEnabled by viewModel.paymentConfirmationsEnabled.collectAsState()
    val appLockEnabled by viewModel.appLockEnabled.collectAsState()
    val privacyModeEnabled by viewModel.privacyModeEnabled.collectAsState()
    val appThemeSettings by viewModel.appThemeSettings.collectAsState()
    val accentColorIndex by viewModel.accentColorIndex.collectAsState()
    val showTransactionTime by viewModel.showTransactionTime.collectAsState()

    val emergencyTarget by viewModel.emergencyTarget.collectAsState()
    val emergencyCurrent by viewModel.emergencyCurrent.collectAsState()
    val emergencyTargetDate by viewModel.emergencyTargetDate.collectAsState()
    val spendingFreezeEnabled by viewModel.spendingFreezeEnabled.collectAsState()
    val categorizationRules by viewModel.categorizationRules.collectAsState()
    val votedFeedbacks by viewModel.votedFeedbacks.collectAsState()
    val feedbacksList by viewModel.feedbacksList.collectAsState()

    // Local state managers for responsive typing
    var localProfileName by remember(profileName) { mutableStateOf(profileName) }
    var localProfileEmail by remember(profileEmail) { mutableStateOf(profileEmail) }
    var localEmergencyTarget by remember(emergencyTarget) { mutableStateOf(if (emergencyTarget == 0.0) "" else emergencyTarget.toInt().toString()) }
    var localEmergencyCurrent by remember(emergencyCurrent) { mutableStateOf(if (emergencyCurrent == 0.0) "" else emergencyCurrent.toInt().toString()) }
    var localEmergencyDate by remember(emergencyTargetDate) { mutableStateOf(emergencyTargetDate) }

    if (categoryToRename != null) {
        AlertDialog(
            onDismissRequest = { categoryToRename = null },
            title = { Text("Rename Category", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NetYellow,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedLabelColor = NetYellow,
                        unfocusedLabelColor = TextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val oldName = categoryToRename
                        if (oldName != null && renameValue.trim().isNotEmpty()) {
                            if (categoryType == "EXPENSE") {
                                viewModel.renameExpenseCategory(oldName, renameValue.trim())
                            } else {
                                viewModel.renameIncomeCategory(oldName, renameValue.trim())
                            }
                        }
                        categoryToRename = null
                    }
                ) {
                    Text("Rename", color = NetYellow, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToRename = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkBg,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
        )
    }

    var expandedTabs by remember { mutableStateOf(setOf<String>("PREFERENCES")) }
    val toolsOrderStr by viewModel.toolsOrder.collectAsState()
    val orderedToolIds = remember(toolsOrderStr) {
        toolsOrderStr.split(",")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Redesigned Top Bar with Tool Accent Icon & Status Done Button
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = NetYellow,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Toolbox & Settings",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(
                        onClick = {
                            if (localProfileName != profileName) viewModel.setProfileName(localProfileName)
                            if (localProfileEmail != profileEmail) viewModel.setProfileEmail(localProfileEmail)
                            onDismiss()
                        }
                    ) {
                        Text("Done", color = NetYellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(bottom = 6.dp))

                // Scrollable Column holding collapsible drop down tabs
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    orderedToolIds.forEachIndexed { idx, tabId ->
                        val isExpanded = expandedTabs.contains(tabId)
                        val title = when (tabId) {
                            "CALCULATOR" -> "Basic Calculator"
                            "WORLD_CLOCK" -> "World Time Clock"
                            "DATE_CALC" -> "Date Duration Calculator"
                            "GRAM_SCALE" -> "Gram Scale & Pricing"
                            else -> "Preferences & Settings"
                        }
                        val icon = when (tabId) {
                            "CALCULATOR" -> Icons.Default.Calculate
                            "WORLD_CLOCK" -> Icons.Default.Language
                            "DATE_CALC" -> Icons.Default.DateRange
                            "GRAM_SCALE" -> Icons.Default.AttachMoney
                            else -> Icons.Default.Settings
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dropdown_tab_${tabId}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isExpanded) DarkSurface else DarkSurface.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isExpanded) NetYellow.copy(alpha = 0.8f) else SurfaceBorder.copy(alpha = 0.5f))
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedTabs = if (isExpanded) {
                                                expandedTabs - tabId
                                            } else {
                                                expandedTabs + tabId
                                            }
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Reorder Controls inside modern arrows (Up, Down)
                                        IconButton(
                                            onClick = {
                                                val mutableIds = orderedToolIds.toMutableList()
                                                if (idx > 0) {
                                                    val temp = mutableIds[idx]
                                                    mutableIds[idx] = mutableIds[idx - 1]
                                                    mutableIds[idx - 1] = temp
                                                    viewModel.setToolsOrder(mutableIds.joinToString(","))
                                                }
                                            },
                                            enabled = idx > 0,
                                            modifier = Modifier.size(28.dp).testTag("reorder_up_${tabId}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowUpward,
                                                contentDescription = "Move Up",
                                                tint = if (idx > 0) NetYellow else TextSecondary.copy(alpha = 0.2f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                val mutableIds = orderedToolIds.toMutableList()
                                                if (idx < mutableIds.size - 1) {
                                                    val temp = mutableIds[idx]
                                                    mutableIds[idx] = mutableIds[idx + 1]
                                                    mutableIds[idx + 1] = temp
                                                    viewModel.setToolsOrder(mutableIds.joinToString(","))
                                                }
                                            },
                                            enabled = idx < orderedToolIds.size - 1,
                                            modifier = Modifier.size(28.dp).testTag("reorder_down_${tabId}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDownward,
                                                contentDescription = "Move Down",
                                                tint = if (idx < orderedToolIds.size - 1) NetYellow else TextSecondary.copy(alpha = 0.2f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = NetYellow,
                                            modifier = Modifier.size(20.dp)
                                        )

                                        Text(
                                            text = title,
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                                        tint = NetYellow,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                if (isExpanded) {
                                    HorizontalDivider(color = SurfaceBorder)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp)
                                    ) {
                                        when (tabId) {
                                            "CALCULATOR" -> {
                                                Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                                                    CalculatorToolView()
                                                }
                                            }
                                            "WORLD_CLOCK" -> {
                                                Box(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                                                    WorldClockToolView()
                                                }
                                            }
                                            "DATE_CALC" -> {
                                                Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                                                    DateCalculatorToolView()
                                                }
                                            }
                                            "GRAM_SCALE" -> {
                                                Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                                                    GramScaleToolView(currentCurrency)
                                                }
                                            }
                                            "PREFERENCES" -> {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                // SECTION: FREQUENTLY USED QUICK PREFERENCES (Top Row for speed!)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NetYellow.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Frequently Used Settings", fontSize = 12.sp, color = NetYellow, fontWeight = FontWeight.Bold)
                        
                        // Active displays
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                Text("Currency Rate Display", fontSize = 11.sp, color = TextPrimary)
                            }
                            IconButton(onClick = onRefreshRates, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh Rates", tint = NetYellow, modifier = Modifier.size(14.dp))
                            }
                        }

                        // Toggle Alerts Shortcut
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                Text("Immediate Budget Alerts", fontSize = 11.sp, color = TextPrimary)
                            }
                            Switch(
                                checked = alertsEnabled,
                                onCheckedChange = onAlertsToggle,
                                colors = SwitchDefaults.colors(checkedThumbColor = NetYellow)
                            )
                        }

                        // Toggle Transaction Time Shortcut
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                Text("Show Time in Transactions", fontSize = 11.sp, color = TextPrimary)
                            }
                            Switch(
                                checked = showTransactionTime,
                                onCheckedChange = { viewModel.setShowTransactionTime(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = NetYellow)
                            )
                        }
                    }
                }

                // 1. PROFILE SECTION
                SettingsSectionCard(title = "Profile Information", icon = Icons.Default.Person) {
                    OutlinedTextField(
                        value = localProfileName,
                        onValueChange = {
                            localProfileName = it
                            viewModel.setProfileName(it)
                        },
                        label = { Text("Display Name", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NetYellow,
                            unfocusedBorderColor = SurfaceBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = localProfileEmail,
                        onValueChange = {
                            localProfileEmail = it
                            viewModel.setProfileEmail(it)
                        },
                        label = { Text("Email Address", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NetYellow,
                            unfocusedBorderColor = SurfaceBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 2. CURRENCY & REGION
                SettingsSectionCard(title = "Currency & Region", icon = Icons.Default.Language) {
                    Text("Primary Display Currency", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    
                    val currencyOptions = listOf(
                        "USD" to "USD ($)", 
                        "EUR" to "EUR (€)", 
                        "GBP" to "GBP (£)",
                        "JPY" to "JPY (¥)",
                        "CAD" to "CAD (C$)",
                        "AUD" to "AUD (A$)"
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (i in currencyOptions.indices step 3) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (j in 0..2) {
                                    if (i + j < currencyOptions.size) {
                                        val (code, label) = currencyOptions[i + j]
                                        val isSelected = currentCurrencyCode == code
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(
                                                    if (isSelected) NetYellow.copy(alpha = 0.15f) else DarkBg,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) NetYellow else SurfaceBorder,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable { onCurrencyChange(code) }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label, 
                                                fontSize = 11.sp, 
                                                color = if (isSelected) NetYellow else TextPrimary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Number Format Custom Selector
                    var showNumberDropdown by remember { mutableStateOf(false) }
                    Column {
                        Text("Number Format", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { showNumberDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, SurfaceBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(numberFormat, fontSize = 12.sp)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = showNumberDropdown,
                                onDismissRequest = { showNumberDropdown = false },
                                modifier = Modifier.background(DarkSurface).border(1.dp, SurfaceBorder)
                            ) {
                                listOf("Standard (1,234.56)", "European (1.234,56)", " الفرنسي (1 234,56)").forEach { fmt ->
                                    DropdownMenuItem(
                                        text = { Text(fmt, color = TextPrimary, fontSize = 11.sp) },
                                        onClick = {
                                            viewModel.setNumberFormat(fmt)
                                            showNumberDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // First Day of the Week Selector
                    var showDayDropdown by remember { mutableStateOf(false) }
                    Column {
                        Text("First Day of the Week", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { showDayDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, SurfaceBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(firstDayOfWeek, fontSize = 12.sp)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = showDayDropdown,
                                onDismissRequest = { showDayDropdown = false },
                                modifier = Modifier.background(DarkSurface).border(1.dp, SurfaceBorder)
                            ) {
                                listOf("Sunday", "Monday", "Saturday").forEach { day ->
                                    DropdownMenuItem(
                                        text = { Text(day, color = TextPrimary, fontSize = 11.sp) },
                                        onClick = {
                                            viewModel.setFirstDayOfWeek(day)
                                            showDayDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. BUDGET DEFAULTS CARD
                SettingsSectionCard(title = "Budget Defaults", icon = Icons.Default.Tune) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Default Cycle", fontSize = 12.sp, color = TextPrimary)
                        Row(
                            modifier = Modifier
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            listOf("Weekly", "Monthly", "Yearly").forEach { cycle ->
                                val isSelected = budgetCycle == cycle
                                Box(
                                    modifier = Modifier
                                        .background(if (isSelected) NetYellow else Color.Transparent)
                                        .clickable { viewModel.setBudgetCycle(cycle) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = cycle,
                                        fontSize = 11.sp,
                                        color = if (isSelected) DarkBg else TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Rollover Balances", fontSize = 12.sp, color = TextPrimary)
                            Text("Carry leftovers to next budget cycle", fontSize = 9.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = rolloverEnabled,
                            onCheckedChange = { viewModel.setRolloverEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = NetYellow)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Overspending Alert Trigger", fontSize = 12.sp, color = TextPrimary)
                            Text("${overspendingThreshold.toInt()}% Capacity", fontSize = 11.sp, color = NetYellow, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = overspendingThreshold.toFloat(),
                            onValueChange = { viewModel.setOverspendingThreshold(it.toDouble()) },
                            valueRange = 50f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = NetYellow,
                                activeTrackColor = NetYellow,
                                inactiveTrackColor = SurfaceBorder
                            )
                        )
                    }

                    // Pre-existing Categories Manager
                    HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 4.dp))
                    Text("Custom Budget Categories", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (categoryType == "EXPENSE") ExpenseRedBg else Color.Transparent)
                                .clickable { categoryType = "EXPENSE" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("EXPENSE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (categoryType == "EXPENSE") ExpenseRed else TextSecondary)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (categoryType == "INCOME") IncomeGreenBg else Color.Transparent)
                                .clickable { categoryType = "INCOME" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("INCOME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (categoryType == "INCOME") IncomeGreen else TextSecondary)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            placeholder = { Text("Add categories...", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = NetYellow,
                                unfocusedBorderColor = SurfaceBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        )
                        IconButton(
                            onClick = {
                                if (newCategoryName.trim().isNotEmpty()) {
                                    if (categoryType == "EXPENSE") {
                                        viewModel.addExpenseCategory(newCategoryName.trim())
                                    } else {
                                        viewModel.addIncomeCategory(newCategoryName.trim())
                                    }
                                    newCategoryName = ""
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(NetYellow, RoundedCornerShape(8.dp))
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add category", tint = DarkBg, modifier = Modifier.size(16.dp))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 120.dp)
                            .background(DarkBg, RoundedCornerShape(8.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            currentCategories.forEach { cat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(modifier = Modifier.size(6.dp).background(getCategoryColor(cat), CircleShape))
                                        Text(cat, fontSize = 11.sp, color = TextPrimary)
                                    }
                                    Row {
                                        IconButton(onClick = { categoryToRename = cat; renameValue = cat }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "Rename", tint = NetYellow, modifier = Modifier.size(12.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                if (categoryType == "EXPENSE") {
                                                    viewModel.deleteExpenseCategory(cat)
                                                } else {
                                                    viewModel.deleteIncomeCategory(cat)
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. NOTIFICATIONS
                SettingsSectionCard(title = "Notifications & Reminders", icon = Icons.Default.Notifications) {
                    val notifyToggles = listOf(
                        Triple("Bill Due Reminders", "Alert before upcoming expenses are due", billRemindersEnabled) to { v: Boolean -> viewModel.setBillRemindersEnabled(v) },
                        Triple("Daily Spending Recap", "Deliver summaries of active balances every standard night", dailyRecapEnabled) to { v: Boolean -> viewModel.setDailyRecapEnabled(v) },
                        Triple("Low Balance Alerts", "Flag warning warnings when funds drop low", lowBalanceAlertsEnabled) to { v: Boolean -> viewModel.setLowBalanceAlertsEnabled(v) },
                        Triple("Payment Confirmations", "Receive instant receipts on saved transactions", paymentConfirmationsEnabled) to { v: Boolean -> viewModel.setPaymentConfirmationsEnabled(v) }
                    )

                    notifyToggles.forEach { (item, setter) ->
                        val (label, desc, active) = item
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text(desc, fontSize = 9.sp, color = TextSecondary)
                            }
                            Switch(
                                checked = active,
                                onCheckedChange = setter,
                                colors = SwitchDefaults.colors(checkedThumbColor = NetYellow)
                            )
                        }
                    }
                }

                // 5. SECURITY & DATA EXPORT
                SettingsSectionCard(title = "Security & Data Settings", icon = Icons.Default.Security) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Security Lock (Biometric/PIN)", fontSize = 12.sp, color = TextPrimary)
                            Text("Requires authentication on app restart", fontSize = 9.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = appLockEnabled,
                            onCheckedChange = { viewModel.setAppLockEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = NetYellow)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Privacy Mode", fontSize = 12.sp, color = TextPrimary)
                            Text("Hide balances and account digits in background mode", fontSize = 9.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = privacyModeEnabled,
                            onCheckedChange = { viewModel.setPrivacyModeEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = NetYellow)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Data Backup & Export", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                android.widget.Toast.makeText(context, "Completed! Exported financial logs as CSV.", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, SurfaceBorder)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CSV", fontSize = 11.sp, color = TextPrimary)
                        }

                        OutlinedButton(
                            onClick = {
                                android.widget.Toast.makeText(context, "Completed! Bank statements downloaded as PDF.", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, SurfaceBorder)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF Statement", fontSize = 11.sp, color = TextPrimary)
                        }
                    }
                }

                // 6. APPEARANCE SETTINGS
                SettingsSectionCard(title = "Appearance & Theme", icon = Icons.Default.Palette) {
                    Text("Select Default Theme", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Light", "Dark", "System").forEach { theme ->
                            val isSelected = appThemeSettings == theme
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSelected) NetYellow.copy(alpha = 0.15f) else DarkBg, RoundedCornerShape(8.dp))
                                    .border(1.dp, if (isSelected) NetYellow else SurfaceBorder, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setAppThemeSettings(theme) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = theme,
                                    fontSize = 11.sp,
                                    color = if (isSelected) NetYellow else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Accent Highlight Color", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val accentPalette = listOf(
                            Color(0xFFF5C542), // Yellow (Standard)
                            Color(0xFF12D18E), // Green
                            Color(0xFF42A5F5), // Blue
                            Color(0xFFAB47BC)  // Purple
                        )
                        accentPalette.forEachIndexed { idx, color ->
                            val isSelected = accentColorIndex == idx
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(color, CircleShape)
                                    .border(
                                        2.dp,
                                        if (isSelected) Color.White else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { viewModel.setAccentColorIndex(idx) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Active Color", tint = DarkBg, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                // 7. EMERGENCY FUND GOAL TRACKER (PREMIUM SPECIAL)
                SettingsSectionCard(title = "Emergency Fund Tracker", icon = Icons.Default.Savings) {
                    val progressPercent = remember(emergencyTarget, emergencyCurrent) {
                        if (emergencyTarget > 0) ((emergencyCurrent / emergencyTarget) * 100).coerceIn(0.0..100.0) else 0.0
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = localEmergencyTarget,
                            onValueChange = {
                                localEmergencyTarget = it
                                val num = it.toDoubleOrNull() ?: 0.0
                                viewModel.setEmergencyTarget(num)
                            },
                            label = { Text("Goal target ($)", fontSize = 10.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NetYellow),
                            modifier = Modifier.weight(1f).height(48.dp)
                        )
                        OutlinedTextField(
                            value = localEmergencyCurrent,
                            onValueChange = {
                                localEmergencyCurrent = it
                                val num = it.toDoubleOrNull() ?: 0.0
                                viewModel.setEmergencyCurrent(num)
                            },
                            label = { Text("Saved cash ($)", fontSize = 10.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NetYellow),
                            modifier = Modifier.weight(1f).height(48.dp)
                        )
                    }

                    OutlinedTextField(
                        value = localEmergencyDate,
                        onValueChange = {
                            localEmergencyDate = it
                            viewModel.setEmergencyTargetDate(it)
                        },
                        label = { Text("Target Completion Date (YYYY-MM-DD)", fontSize = 10.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NetYellow),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Goal Completion Progress", fontSize = 11.sp, color = TextSecondary)
                            Text("${String.format(Locale.US, "%.1f", progressPercent)}% Done", fontSize = 11.sp, color = IncomeGreen, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = (progressPercent.toFloat() / 100f),
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = IncomeGreen,
                            trackColor = SurfaceBorder
                        )
                    }
                }

                // 8. "SPENDING FREEZE" MODE
                SettingsSectionCard(title = "Discretionary Spending Freeze", icon = Icons.Default.AcUnit) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Freeze Discretionary Shopping", fontSize = 12.sp, color = TextPrimary)
                            Text("Enables strict reminders on active challenges", fontSize = 9.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = spendingFreezeEnabled,
                            onCheckedChange = { viewModel.setSpendingFreezeEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = ExpenseRed)
                        )
                    }

                    if (spendingFreezeEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ExpenseRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .border(1.dp, ExpenseRed, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "Freeze Active", tint = ExpenseRed, modifier = Modifier.size(16.dp))
                            Text(
                                "Spending Freeze Challenging Active! Try not to spend on eating out, shopping, or non-essentials today.",
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }

                // 9. AUTOMATED RULES ENGINE FOR CATEGORIZATION
                SettingsSectionCard(title = "Automated Categorization Rules", icon = Icons.Default.Rule) {
                    var customKeyword by remember { mutableStateOf("") }
                    var selectedRuleCategory by remember { mutableStateOf("") }
                    var expandedRuleDrop by remember { mutableStateOf(false) }
                    
                    val allCategoriesList = remember(expenseCategories, incomeCategories) {
                        expenseCategories + incomeCategories
                    }
                    
                    LaunchedEffect(allCategoriesList) {
                        if (selectedRuleCategory !in allCategoriesList && allCategoriesList.isNotEmpty()) {
                            selectedRuleCategory = allCategoriesList.first()
                        }
                    }

                    Text("If transaction title contains keyword, suggest category automatically.", fontSize = 11.sp, color = TextSecondary)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customKeyword,
                            onValueChange = { customKeyword = it },
                            placeholder = { Text("Keyword e.g. Uber", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NetYellow),
                            modifier = Modifier.weight(1.2f).height(48.dp)
                        )
                        
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { expandedRuleDrop = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                border = BorderStroke(1.dp, SurfaceBorder),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(if (selectedRuleCategory.length > 8) selectedRuleCategory.take(6)+"..." else selectedRuleCategory, fontSize = 10.sp, maxLines = 1)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(12.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = expandedRuleDrop,
                                onDismissRequest = { expandedRuleDrop = false },
                                modifier = Modifier.background(DarkSurface).border(1.dp, SurfaceBorder)
                            ) {
                                allCategoriesList.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat, color = TextPrimary, fontSize = 10.sp) },
                                        onClick = {
                                            selectedRuleCategory = cat
                                            expandedRuleDrop = false
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                if (customKeyword.trim().isNotEmpty() && selectedRuleCategory.isNotEmpty()) {
                                    viewModel.addCategorizationRule(customKeyword.trim(), selectedRuleCategory)
                                    customKeyword = ""
                                }
                            },
                            modifier = Modifier.size(36.dp).background(NetYellow, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Rule", tint = DarkBg, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Display Current Rules
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 120.dp)
                            .background(DarkBg, RoundedCornerShape(8.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            if (categorizationRules.isEmpty()) {
                                Text("No categorization rules found.", fontSize = 10.sp, color = TextSecondary, modifier = Modifier.padding(8.dp))
                            } else {
                                categorizationRules.forEach { (keyword, cat) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("If \"$keyword\"", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(10.dp))
                                            Text(cat, fontSize = 11.sp, color = NetYellow)
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteCategorizationRule(keyword) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete rule", tint = ExpenseRed, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 10. COMMUNITY FEEDBACK & NEW PROPOSALS
                SettingsSectionCard(title = "Community Feature Voting", icon = Icons.Default.ThumbUp) {
                    var newFeedbackTitle by remember { mutableStateOf("") }
                    var newFeedbackDesc by remember { mutableStateOf("") }

                    Text("Vote on development items, or offer suggestions!", fontSize = 11.sp, color = TextSecondary)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        feedbacksList.sortedByDescending { it.votes }.forEach { f ->
                            val hasVoted = f.id in votedFeedbacks
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkBg, RoundedCornerShape(8.dp))
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(f.title, fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                    Text(f.description, fontSize = 9.sp, color = TextSecondary, maxLines = 1)
                                }
                                Button(
                                    onClick = { viewModel.upvoteFeedback(f.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (hasVoted) NetYellow else DarkSurface),
                                    border = BorderStroke(1.dp, if (hasVoted) NetYellow else SurfaceBorder),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.ThumbUp, contentDescription = "Upvote", tint = if (hasVoted) DarkBg else TextSecondary, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${f.votes}", fontSize = 10.sp, color = if (hasVoted) DarkBg else TextPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Propose a Feature", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = newFeedbackTitle,
                        onValueChange = { newFeedbackTitle = it },
                        placeholder = { Text("Title e.g. Excel exports", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NetYellow),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    )
                    OutlinedTextField(
                        value = newFeedbackDesc,
                        onValueChange = { newFeedbackDesc = it },
                        placeholder = { Text("Description...", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NetYellow),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    )
                    OutlinedButton(
                        onClick = {
                            if (newFeedbackTitle.trim().isNotEmpty() && newFeedbackDesc.trim().isNotEmpty()) {
                                viewModel.addFeedback(newFeedbackTitle.trim(), newFeedbackDesc.trim())
                                newFeedbackTitle = ""
                                newFeedbackDesc = ""
                                android.widget.Toast.makeText(context, "Under consideration! Suggested proposal created.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NetYellow),
                        border = BorderStroke(1.dp, NetYellow)
                    ) {
                        Text("Add Proposal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 12.dp))

                // DESTRUCTIVE CORNER (Double Confirmation Resets)
                Text("Database Diagnostics & Resets", fontSize = 13.sp, color = ExpenseRed, fontWeight = FontWeight.Bold)

                var categoriesConfirmState by remember { mutableStateOf(false) }
                var budgetsConfirmState by remember { mutableStateOf(false) }
                var factoryConfirmState by remember { mutableStateOf(false) }

                // 1. Reset Categories (Double confirmed)
                Button(
                    onClick = {
                        if (categoriesConfirmState) {
                            viewModel.resetCategoriesToDefault()
                            categoriesConfirmState = false
                            android.widget.Toast.makeText(context, "Categories reset to factory defaults!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            categoriesConfirmState = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (categoriesConfirmState) ExpenseRed else DarkSurface),
                    border = BorderStroke(1.dp, if (categoriesConfirmState) ExpenseRed else SurfaceBorder)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = if (categoriesConfirmState) Color.White else TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (categoriesConfirmState) "CLICK AGAIN TO CONFIRM RESET" else "Reset Categories to Default",
                        color = if (categoriesConfirmState) Color.White else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 2. Reset Budgets (Double confirmed)
                Button(
                    onClick = {
                        if (budgetsConfirmState) {
                            viewModel.resetBudgets()
                            budgetsConfirmState = false
                            android.widget.Toast.makeText(context, "Budgets and targets cleaned successfully!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            budgetsConfirmState = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (budgetsConfirmState) ExpenseRed else DarkSurface),
                    border = BorderStroke(1.dp, if (budgetsConfirmState) ExpenseRed else SurfaceBorder)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = if (budgetsConfirmState) Color.White else TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (budgetsConfirmState) "CLICK AGAIN TO CONFIRM RESET" else "Reset Budgets & Targets",
                        color = if (budgetsConfirmState) Color.White else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 3. Clear all database (Double confirmed)
                Button(
                    onClick = {
                        if (factoryConfirmState) {
                            viewModel.resetToFactoryDefault()
                            factoryConfirmState = false
                            onDismiss()
                            android.widget.Toast.makeText(context, "Completely wiped all local data!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            factoryConfirmState = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (factoryConfirmState) ExpenseRed else ExpenseRed.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, ExpenseRed)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = if (factoryConfirmState) Color.White else ExpenseRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (factoryConfirmState) "CONFIRM FACTORY WIPE" else "Wipe All Databases & Settings",
                        color = if (factoryConfirmState) Color.White else ExpenseRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 12.dp))

                // CRITICAL SAFETY ACCOUNT MANAGE ACTIONS (at the very bottom)
                Text("Safety Account Actions", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            android.widget.Toast.makeText(context, "Logged out successfully.", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, SurfaceBorder)
                    ) {
                        Text("Logout", fontSize = 12.sp, color = TextPrimary)
                    }

                    Button(
                        onClick = {
                            viewModel.resetToFactoryDefault()
                            onDismiss()
                            android.widget.Toast.makeText(context, "Account balances deleted completely.", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, ExpenseRed)
                    ) {
                        Text("Delete Account", fontSize = 11.sp, color = ExpenseRed)
                    }
                }
            }
        }
    }
}
}
}
}
}
}
}
}
}
}

fun evaluateMathExpression(expr: String): Double? {
    try {
        val sanitized = expr.replace(" ", "")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
        if (sanitized.isEmpty()) return null
        
        return object {
            var pos = -1
            var ch = 0

            fun nextChar() {
                pos++
                ch = if (pos < sanitized.length) sanitized[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < sanitized.length) return Double.NaN
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) {
                        val divisor = parseFactor()
                        x = if (divisor == 0.0) Double.NaN else x / divisor
                    }
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = this.pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                    val str = sanitized.substring(startPos, this.pos)
                    x = str.toDoubleOrNull() ?: 0.0
                } else {
                    return Double.NaN
                }
                return x
            }
        }.parse().let { if (it.isNaN() || it.isInfinite()) null else it }
    } catch (e: Exception) {
        return null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDurationDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allTransactions by viewModel.allTransactions.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val customStart by viewModel.customStartDate.collectAsState()
    val customEnd by viewModel.customEndDate.collectAsState()

    var startDate by remember { mutableStateOf(customStart) }
    var endDate by remember { mutableStateOf(customEnd) }

    val filterType by viewModel.filterType.collectAsState()
    val filterCategory by viewModel.filterCategory.collectAsState()

    val rangeTotals = remember(allTransactions, startDate, endDate, filterType, filterCategory) {
        val startMilli = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMilli = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

        var totalIncome = 0.0
        var totalExpense = 0.0

        allTransactions.forEach { t ->
            if (t.timestamp in startMilli..endMilli) {
                val matchesType = filterType == "ALL" || t.type == filterType
                val matchesCategory = filterCategory == "ALL" || t.category.equals(filterCategory, ignoreCase = true)
                if (matchesType && matchesCategory) {
                    if (t.type == "INCOME") {
                        totalIncome += t.amount
                    } else {
                        totalExpense += t.amount
                    }
                }
            }
        }
        Triple(totalIncome, totalExpense, totalIncome - totalExpense)
    }

    val (income, expense, net) = rangeTotals

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = NetYellow)
                Text("Custom Duration Usage", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.setCustomDateRange(startDate, endDate)
                    viewModel.setSelectedPeriod("Custom")
                    onDismiss()
                },
                modifier = Modifier.testTag("apply_custom_range_button")
            ) {
                Text("Apply to List", color = NetYellow, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Adjust the start and end dates below. The usage summary will update instantly.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Start Date Button
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start Date", fontSize = 11.sp, color = NetYellow, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurface, RoundedCornerShape(8.dp))
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    val picker = DatePickerDialog(
                                        context,
                                        { _, y, m, d -> startDate = LocalDate.of(y, m + 1, d) },
                                        startDate.year,
                                        startDate.monthValue - 1,
                                        startDate.dayOfMonth
                                    )
                                    picker.show()
                                }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = startDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                                fontSize = 12.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // End Date Button
                    Column(modifier = Modifier.weight(1f)) {
                        Text("End Date", fontSize = 11.sp, color = NetYellow, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurface, RoundedCornerShape(8.dp))
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    val picker = DatePickerDialog(
                                        context,
                                        { _, y, m, d -> endDate = LocalDate.of(y, m + 1, d) },
                                        endDate.year,
                                        endDate.monthValue - 1,
                                        endDate.dayOfMonth
                                    )
                                    picker.show()
                                }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = endDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                                fontSize = 12.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (startDate.isAfter(endDate)) {
                    Text(
                        text = "Warning: Start date is after end date!",
                        color = ExpenseRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 4.dp))

                Text("Usage Summary", fontSize = 13.sp, color = NetYellow, fontWeight = FontWeight.Bold)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(12.dp))
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val formattedIncome = formatCurrency(income, currencySymbol)
                    val formattedExpense = formatCurrency(expense, currencySymbol)
                    val formattedNet = (if (net >= 0) "+" else "") + formatCurrency(net, currencySymbol)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Income", fontSize = 13.sp, color = TextSecondary)
                        Text(formattedIncome, fontSize = 14.sp, color = IncomeGreen, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Expense", fontSize = 13.sp, color = TextSecondary)
                        Text("-$formattedExpense", fontSize = 14.sp, color = ExpenseRed, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(color = SurfaceBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Net Balance", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        Text(
                            text = formattedNet,
                            fontSize = 15.sp,
                            color = if (net >= 0) IncomeGreen else ExpenseRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        containerColor = DarkBg,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
    )
}

// ==========================================
// RECURRING TRANSACTIONS DIALOG
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionsDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allRecurring by viewModel.allRecurringTransactions.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    
    var isAdding by remember { mutableStateOf(false) }
    
    // Form States
    var title by remember { mutableStateOf("") }
    var amountString by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") } // INCOME or EXPENSE
    var category by remember { mutableStateOf("Subscription") }
    var frequency by remember { mutableStateOf("Monthly") }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var notes by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }
    
    val categoryOptions = listOf("Subscription", "Housing/Rent", "Bills", "Utilities", "Salary", "Food", "Entertainment", "Other")
    val frequencyOptions = listOf("Daily", "Weekly", "Monthly", "Yearly")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = NetYellow
                )
                Text(
                    text = if (isAdding) "Create Recurring Item" else "Recurring Bills & Income",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        confirmButton = {
            if (isAdding) {
                Button(
                    onClick = {
                        val amount = amountString.toDoubleOrNull()
                        if (title.isBlank()) {
                            formError = "Please enter a title."
                        } else if (amount == null || amount <= 0) {
                            formError = "Please enter a valid amount."
                        } else {
                            viewModel.addRecurringTransaction(
                                title = title,
                                amount = amount,
                                type = type,
                                category = category,
                                frequency = frequency,
                                startDate = startDate,
                                notes = notes
                            )
                            // Reset state
                            title = ""
                            amountString = ""
                            notes = ""
                            formError = null
                            isAdding = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NetYellow, contentColor = Color.Black),
                    modifier = Modifier.testTag("save_recurring_button")
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { isAdding = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NetYellow, contentColor = Color.Black),
                    modifier = Modifier.testTag("new_recurring_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (isAdding) {
                        isAdding = false
                        formError = null
                    } else {
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("close_recurring_dialog")
            ) {
                Text(if (isAdding) "Back" else "Close", color = TextSecondary)
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
            ) {
                if (isAdding) {
                    // Add Recurring Transaction Form
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (formError != null) {
                            Text(
                                text = formError!!,
                                color = ExpenseRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Title Input
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title (e.g. rent, Netflix)", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface,
                                focusedBorderColor = NetYellow,
                                unfocusedBorderColor = SurfaceBorder
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("recurring_title_input")
                        )
                        
                        // Amount Input
                        OutlinedTextField(
                            value = amountString,
                            onValueChange = { amountString = it },
                            label = { Text("Amount", color = TextSecondary) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface,
                                focusedBorderColor = NetYellow,
                                unfocusedBorderColor = SurfaceBorder
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("recurring_amount_input")
                        )

                        // Income or Expense Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                                .background(DarkSurface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(if (type == "EXPENSE") ExpenseRedBg else Color.Transparent)
                                    .clickable { type = "EXPENSE" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "EXPENSE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (type == "EXPENSE") ExpenseRed else TextSecondary
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(if (type == "INCOME") IncomeGreenBg else Color.Transparent)
                                    .clickable { type = "INCOME" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "INCOME",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (type == "INCOME") IncomeGreen else TextSecondary
                                )
                            }
                        }

                        // Category Selection Dropdown
                        var showCategoryDropdown by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category", color = TextSecondary) },
                                trailingIcon = {
                                    IconButton(onClick = { showCategoryDropdown = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Category", tint = TextSecondary)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface,
                                    focusedBorderColor = NetYellow,
                                    unfocusedBorderColor = SurfaceBorder
                                ),
                                modifier = Modifier.fillMaxWidth().clickable { showCategoryDropdown = true }
                            )

                            DropdownMenu(
                                expanded = showCategoryDropdown,
                                onDismissRequest = { showCategoryDropdown = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(DarkSurface)
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                            ) {
                                categoryOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option, color = TextPrimary) },
                                        onClick = {
                                            category = option
                                            showCategoryDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Frequency (Horizontal list of choices)
                        Column {
                            Text("Frequency", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                frequencyOptions.forEach { option ->
                                    val isSelected = frequency == option
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) NetYellow else SurfaceBorder,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .background(
                                                color = if (isSelected) NetYellow.copy(alpha = 0.15f) else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { frequency = option }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = option,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) NetYellow else TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // Start Date DatePicker clicker
                        OutlinedTextField(
                            value = startDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Starting Date", color = TextSecondary) },
                            trailingIcon = {
                                Icon(Icons.Default.DateRange, contentDescription = null, tint = TextSecondary)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface,
                                focusedBorderColor = NetYellow,
                                unfocusedBorderColor = SurfaceBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val picker = DatePickerDialog(
                                        context,
                                        { _, y, m, d -> startDate = LocalDate.of(y, m + 1, d) },
                                        startDate.year,
                                        startDate.monthValue - 1,
                                        startDate.dayOfMonth
                                    )
                                    picker.show()
                                }
                        )

                        // Notes Input
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes (optional)", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface,
                                focusedBorderColor = NetYellow,
                                unfocusedBorderColor = SurfaceBorder
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("recurring_notes_input")
                        )
                    }
                } else {
                    // List Existing Recurring Transactions
                    if (allRecurring.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = TextSecondary.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No recurring items configured",
                                    fontSize = 14.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap 'New' to set up monthly subscriptions or bills",
                                    fontSize = 11.sp,
                                    color = TextSecondary.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(allRecurring) { item ->
                                val rate = viewModel.getExchangeRateFor(viewModel.selectedCurrencyCode.value)
                                val convertedAmount = item.amount * rate
                                val formattedAmountValue = formatCurrency(convertedAmount, currencySymbol)
                                val nextDueFormatted = formatEpochMilli(item.nextTriggerTimestamp)
                                val isIncome = item.type == "INCOME"

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Category icon
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(getCategoryColor(item.category).copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = getCategoryIcon(item.category),
                                                contentDescription = null,
                                                tint = getCategoryColor(item.category),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Subscription Metadata
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = item.title,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            color = if (isIncome) IncomeGreenBg else ExpenseRedBg,
                                                            shape = RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = item.frequency.uppercase(),
                                                        fontSize = 8.sp,
                                                        color = if (isIncome) IncomeGreen else ExpenseRed,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (isIncome) "+$formattedAmountValue" else "-$formattedAmountValue",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isIncome) IncomeGreen else ExpenseRed
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Next payment: $nextDueFormatted",
                                                fontSize = 10.sp,
                                                color = TextSecondary
                                            )
                                        }

                                        // Actions: Pause switch & Delete button
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Switch(
                                                checked = item.isActive,
                                                onCheckedChange = { active ->
                                                    viewModel.toggleRecurringTransactionActive(item.id, active, item)
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = NetYellow,
                                                    checkedTrackColor = NetYellow.copy(alpha = 0.3f),
                                                    uncheckedThumbColor = TextSecondary,
                                                    uncheckedTrackColor = DarkSurface
                                                ),
                                                modifier = Modifier.testTag("toggle_status_${item.id}")
                                            )

                                            IconButton(
                                                onClick = { viewModel.deleteRecurringTransaction(item.id) },
                                                modifier = Modifier.testTag("delete_recurring_${item.id}").size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = ExpenseRed.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = DarkBg,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
    )
}

fun formatEpochMilli(milli: Long): String {
    val date = java.time.Instant.ofEpochMilli(milli)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
    return date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
}

// ==========================================
// BUDGET ALERTS NOTIFICATION PANEL
// ==========================================
@Composable
fun BudgetAlertsNotificationPanel(
    alerts: List<BudgetAlert>,
    currencySymbol: String,
    onNavigateToBudgets: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.3.dp,
                color = if (alerts.any { it.isBreached }) ExpenseRed.copy(alpha = 0.7f) else NetYellow.copy(alpha = 0.7f),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("budget_alerts_panel")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Alerts",
                        tint = if (alerts.any { it.isBreached }) ExpenseRed else NetYellow,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Budget Alerts (${alerts.size})",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "Collapse" else "View All",
                            color = NetYellow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    IconButton(
                        onClick = onNavigateToBudgets,
                        modifier = Modifier.size(28.dp).testTag("manage_budgets_alert_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Configure Budgets",
                            tint = NetYellow,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    alerts.forEach { alert ->
                        val color = if (alert.isBreached) ExpenseRed else NetYellow
                        val formattedSpent = formatCurrency(alert.spentAmount, currencySymbol)
                        val formattedLimit = formatCurrency(alert.limitAmount, currencySymbol)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(color.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .border(0.5.dp, color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = alert.category,
                                        fontSize = 13.sp,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Spent $formattedSpent of $formattedLimit",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (alert.isBreached) "BREACHED" else "${alert.actualPercent.toInt()}% USED",
                                    fontSize = 9.sp,
                                    color = color,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                val majorAlert = alerts.firstOrNull()
                if (majorAlert != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (majorAlert.isBreached) {
                            "Warning: '${majorAlert.category}' has exceeded its budget limit!"
                        } else {
                            "Notice: '${majorAlert.category}' has exceeded its ${majorAlert.thresholdPercent.toInt()}% warning threshold!"
                        },
                        fontSize = 12.sp,
                        color = if (majorAlert.isBreached) ExpenseRed else NetYellow,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// 8. MONTHLY BUDGET PROGRESS COMPONENT
// ==========================================
@Composable
fun MonthlyBudgetProgressCard(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier,
    onSetBudgetClick: (() -> Unit)? = null
) {
    val monthlyBudgets by viewModel.monthlyBudgets.collectAsState()
    val monthlyTransactions by viewModel.monthlyTransactions.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val overallMonthlyBudget by viewModel.overallMonthlyBudget.collectAsState()

    // 1. Calculate aggregated predefined budget limit or overall limit
    val totalBudgetLimit = remember(monthlyBudgets) {
        monthlyBudgets.sumOf { it.limitAmount }
    }
    
    val activeBudgetLimit = if (overallMonthlyBudget > 0.0) overallMonthlyBudget else totalBudgetLimit

    // 2. Calculate current month's spending (all EXPENSE transactions)
    val totalSpending = remember(monthlyTransactions) {
        monthlyTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }

    // 3. Amount left / remaining
    val amountLeft = activeBudgetLimit - totalSpending
    val progress = if (activeBudgetLimit > 0.0) (totalSpending / activeBudgetLimit).toFloat() else 0f
    
    // 4. Color states for visual feedback
    val isLimitBreached = totalSpending > activeBudgetLimit && activeBudgetLimit > 0
    val isNearLimit = totalSpending >= (activeBudgetLimit * 0.8) && !isLimitBreached && activeBudgetLimit > 0

    val progressColor = when {
        isLimitBreached -> ExpenseRed
        isNearLimit -> NetYellow
        else -> IncomeGreen
    }

    var isEditingLimit by remember { mutableStateOf(false) }
    var inputLimitText by remember { mutableStateOf("") }

    // Synchronize inline text field whenever the stored budget updates externally
    LaunchedEffect(overallMonthlyBudget) {
        if (!isEditingLimit) {
            inputLimitText = if (overallMonthlyBudget > 0.0) String.format(Locale.US, "%.2f", overallMonthlyBudget) else ""
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isLimitBreached) ExpenseRed.copy(alpha = 0.5f) else if (isNearLimit) NetYellow.copy(alpha = 0.5f) else SurfaceBorder,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(progressColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isLimitBreached) Icons.Default.Warning else Icons.Default.PieChart,
                            contentDescription = "Budget Status",
                            tint = progressColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Monthly Budget Tracker",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Set a spending limit to stay on track",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Small badge highlighting status
                Box(
                    modifier = Modifier
                        .background(
                            if (activeBudgetLimit <= 0.0) TextSecondary.copy(alpha = 0.1f)
                            else if (isLimitBreached) ExpenseRed.copy(alpha = 0.15f)
                            else if (isNearLimit) NetYellow.copy(alpha = 0.15f)
                            else IncomeGreen.copy(alpha = 0.15f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (activeBudgetLimit <= 0.0) "No Limit"
                               else if (isLimitBreached) "Limit Breached"
                               else if (isNearLimit) "Approaching Limit"
                               else "On Track",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeBudgetLimit <= 0.0) TextSecondary
                                else if (isLimitBreached) ExpenseRed
                                else if (isNearLimit) NetYellow
                                else IncomeGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Inline Budget Input Field
            if (isEditingLimit) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputLimitText,
                        onValueChange = { inputLimitText = it },
                        placeholder = { Text("Limit Amount", color = TextSecondary) },
                        leadingIcon = { Text(currencySymbol, color = NetYellow, fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = {
                                val amt = inputLimitText.toDoubleOrNull() ?: 0.0
                                viewModel.updateOverallMonthlyBudget(amt)
                                isEditingLimit = false
                            }
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NetYellow,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedLabelColor = NetYellow,
                            unfocusedLabelColor = TextSecondary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("inline_budget_limit_input")
                    )

                    IconButton(
                        onClick = {
                            val amt = inputLimitText.toDoubleOrNull() ?: 0.0
                            viewModel.updateOverallMonthlyBudget(amt)
                            isEditingLimit = false
                        },
                        modifier = Modifier.testTag("save_inline_budget_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save Limit",
                            tint = IncomeGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            inputLimitText = if (overallMonthlyBudget > 0.0) String.format(Locale.US, "%.2f", overallMonthlyBudget) else ""
                            isEditingLimit = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Editing",
                            tint = ExpenseRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isEditingLimit = true }
                            .padding(vertical = 4.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Set/Edit Limit",
                            tint = NetYellow,
                            modifier = Modifier.size(16.dp)
                        )
                        val limitValueText = if (activeBudgetLimit > 0.0) {
                            if (overallMonthlyBudget > 0.0) {
                                "${formatCurrency(overallMonthlyBudget, currencySymbol)} (Monthly Limit)"
                            } else {
                                "${formatCurrency(totalBudgetLimit, currencySymbol)} (Sum of Categories)"
                            }
                        } else {
                            "Monthly Spending Limit Not Set"
                        }
                        Text(
                            text = limitValueText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (activeBudgetLimit > 0.0) NetYellow else TextSecondary,
                            modifier = Modifier.testTag("budget_limit_display")
                        )
                    }
                    
                    if (overallMonthlyBudget > 0.0) {
                        TextButton(
                            onClick = { 
                                viewModel.updateOverallMonthlyBudget(0.0)
                                inputLimitText = ""
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.height(28.dp).testTag("clear_overall_budget")
                        ) {
                            Text("Clear", color = ExpenseRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (activeBudgetLimit <= 0.0) {
                // If no budget limit has been set, guide the user elegantly with an inline quick-set
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Establish a monthly spending limit above to actively track and limit your expenses.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { isEditingLimit = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NetYellow),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp).testTag("quick_set_budget_button")
                    ) {
                        Text("Set Limit", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Display spending vs budget limit details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Remaining",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (amountLeft >= 0) formatCurrency(amountLeft, currencySymbol) else formatCurrency(-amountLeft, currencySymbol) + " over",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (amountLeft >= 0) IncomeGreen else ExpenseRed
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Spent vs Limit",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${formatCurrency(totalSpending, currencySymbol)} / ${formatCurrency(activeBudgetLimit, currencySymbol)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Elegant Progress bar comparing progress
                Box(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { progress.coerceAtMost(1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .testTag("overall_budget_progress_bar"),
                        color = progressColor,
                        trackColor = SurfaceBorder
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val percentString = String.format(Locale.US, "%.1f%% used", progress * 100)
                    Text(
                        text = percentString,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )

                    val remainingText = if (amountLeft >= 0) {
                        "${formatCurrency(amountLeft, currencySymbol)} left of your monthly budget limit."
                    } else {
                        "Exceeded by ${formatCurrency(-amountLeft, currencySymbol)}!"
                    }
                    Text(
                        text = remainingText,
                        fontSize = 11.sp,
                        color = if (amountLeft >= 0) TextSecondary else ExpenseRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecentTransactionsLogDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val allTransactions by viewModel.allTransactions.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val showTransactionTime by viewModel.showTransactionTime.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("ALL") }
    var selectedAccount by remember { mutableStateOf("ALL") }
    var selectedCategory by remember { mutableStateOf("ALL") }

    val filteredList = remember(allTransactions, searchQuery, selectedType, selectedAccount, selectedCategory) {
        allTransactions.filter { transaction ->
            !transaction.isDeleted &&
            (transaction.title.contains(searchQuery, ignoreCase = true) ||
             transaction.category.contains(searchQuery, ignoreCase = true) ||
             transaction.notes.contains(searchQuery, ignoreCase = true)) &&
            (selectedType == "ALL" || transaction.type == selectedType) &&
            (selectedAccount == "ALL" || transaction.account.equals(selectedAccount, ignoreCase = true) || (transaction.toAccount != null && transaction.toAccount.equals(selectedAccount, ignoreCase = true))) &&
            (selectedCategory == "ALL" || transaction.category.equals(selectedCategory, ignoreCase = true))
        }.sortedByDescending { it.timestamp }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkBg),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Activity Log",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Scroll and manage all transactions",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(DarkSurface, CircleShape)
                            .size(36.dp)
                            .testTag("close_recent_log_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close dialog",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search title, category or notes...", color = TextSecondary, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search icon",
                            tint = NetYellow,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search query",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NetYellow,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recent_log_search_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Filter tabs logic 1: Type
                Text(
                    text = "Transaction Type:",
                    fontSize = 11.sp,
                    color = NetYellow,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val types = listOf("ALL", "INCOME", "EXPENSE", "TRANSFER")
                    types.forEach { t ->
                        val isSelected = selectedType == t
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedType = t },
                            label = { Text(t, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NetYellow.copy(alpha = 0.2f),
                                selectedLabelColor = NetYellow,
                                containerColor = DarkSurface,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("filter_chip_type_$t")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter tabs logic 2: Account
                Text(
                    text = "Account Group:",
                    fontSize = 11.sp,
                    color = NetYellow,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val accounts = listOf("ALL", "Cash", "Credit", "Saving")
                    accounts.forEach { acc ->
                        val isSelected = selectedAccount == acc
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedAccount = acc },
                            label = { Text(acc, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NetYellow.copy(alpha = 0.2f),
                                selectedLabelColor = NetYellow,
                                containerColor = DarkSurface,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("filter_chip_account_$acc")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter dropdown: Category
                Text(
                    text = "Category Filter:",
                    fontSize = 11.sp,
                    color = NetYellow,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                var showDialogCategoryMenu by remember { mutableStateOf(false) }
                val dialogCategories = listOf("ALL", "Food", "Transport", "Rent", "Housing/Rent", "Shopping", "Utilities", "Entertainment", "Health", "Education", "Other")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box {
                        Button(
                            onClick = { showDialogCategoryMenu = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("dialog_category_filter_dropdown_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = selectedCategory,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedCategory != "ALL") NetYellow else TextPrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Dropdown Arrow",
                                    tint = if (selectedCategory != "ALL") NetYellow else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showDialogCategoryMenu,
                            onDismissRequest = { showDialogCategoryMenu = false },
                            modifier = Modifier
                                .background(DarkSurface)
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                        ) {
                            dialogCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = cat,
                                            fontSize = 12.sp,
                                            fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedCategory == cat) NetYellow else TextPrimary
                                        )
                                    },
                                    onClick = {
                                        selectedCategory = cat
                                        showDialogCategoryMenu = false
                                    },
                                    modifier = Modifier.testTag("dialog_category_filter_option_$cat")
                                )
                            }
                        }
                    }

                    if (selectedCategory != "ALL") {
                        IconButton(
                            onClick = { selectedCategory = "ALL" },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("dialog_clear_category_filter_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Category Filter",
                                tint = ExpenseRed,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = SurfaceBorder, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))

                // Lazy Scrollable transaction list with delete capabilities
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = "No results",
                                tint = TextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No matching transactions.",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("recent_log_lazy_list"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList) { transaction ->
                            TransactionRowItem(
                                transaction = transaction,
                                onDelete = { viewModel.deleteTransactionById(transaction.id) },
                                currencySymbol = currencySymbol,
                                onEdit = { viewModel.startEditingTransaction(transaction) },
                                showTime = showTransactionTime
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Confirm/Dismiss Done CTA
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NetYellow),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("recent_log_done_button")
                ) {
                    Text(
                        text = "Close Log",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

