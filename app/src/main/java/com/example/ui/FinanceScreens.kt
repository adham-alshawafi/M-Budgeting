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
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cardWidth = (maxWidth - 16.dp) / 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val balances by viewModel.accountBalances.collectAsState()
                    
                    // Card 1: Cash
                    AccountSummaryCard(
                        title = "CASH",
                        amountString = formatCurrency(balances.cash, currencySymbol),
                        tintColor = IncomeGreen,
                        backgroundColor = IncomeGreenBg,
                        icon = Icons.Default.Payments,
                        modifier = Modifier.width(cardWidth)
                    )

                    // Card 2: Savings
                    AccountSummaryCard(
                        title = "SAVING",
                        amountString = formatCurrency(balances.saving, currencySymbol),
                        tintColor = Purple80,
                        backgroundColor = Purple40.copy(alpha = 0.15f),
                        icon = Icons.Default.Savings,
                        modifier = Modifier.width(cardWidth)
                    )

                    // Card 3: Credit Card
                    AccountSummaryCard(
                        title = "CREDIT",
                        amountString = formatCurrency(balances.credit, currencySymbol),
                        tintColor = ExpenseRed,
                        backgroundColor = ExpenseRedBg,
                        icon = Icons.Default.CreditCard,
                        modifier = Modifier.width(cardWidth)
                    )
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
                            currencySymbol = currencySymbol
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = SurfaceBorder.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(20.dp))

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
    currencySymbol: String = "$"
) {
    val isTransfer = transaction.type == "TRANSFER"
    val categoryColor = if (isTransfer) NetYellow else getCategoryColor(transaction.category)
    val categoryIcon = if (isTransfer) Icons.Default.SwapHoriz else getCategoryIcon(transaction.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
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

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
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

// ==========================================
// 2. CALENDAR SCREEN
// ==========================================
@Composable
fun CalendarTabScreen(viewModel: FinanceViewModel) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val dailyTransactions by viewModel.calendarTransactions.collectAsState()
    val dailyStats by viewModel.calendarStats.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()

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

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    daysOfWeekNames.forEach { name ->
                        Text(
                            text = name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.width(32.dp),
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
                                Box(modifier = Modifier.size(38.dp))
                            } else {
                                val isSelected = date == selectedDate
                                val dayTransactions = allTransactions.filter {
                                    val tDate = java.time.Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                                    tDate == date
                                }

                                val hasIncome = dayTransactions.any { it.type == "INCOME" }
                                val hasExpense = dayTransactions.any { it.type == "EXPENSE" }

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            color = if (isSelected) NetYellow else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            viewModel.selectDate(date)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = date.dayOfMonth.toString(),
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = if (isSelected) Color.Black else TextPrimary
                                        )
                                        // Indicators underneath
                                        if (dayTransactions.isNotEmpty() && !isSelected) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (hasIncome) {
                                                    Box(modifier = Modifier.size(4.dp).background(IncomeGreen, CircleShape))
                                                }
                                                if (hasExpense) {
                                                    Box(modifier = Modifier.size(4.dp).background(ExpenseRed, CircleShape))
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
                        currencySymbol = currencySymbol
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(64.dp))
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
@Composable
fun NotesTabScreen(
    viewModel: FinanceViewModel,
    onShowAddNote: () -> Unit
) {
    val allNotes by viewModel.allNotes.collectAsState()

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

            Spacer(modifier = Modifier.height(24.dp))

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
                            text = "Tap + to capture targets, plans or shopping lists",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    allNotes.forEach { note ->
                        val formattedDate = remember(note.timestamp) {
                            val instant = java.time.Instant.ofEpochMilli(note.timestamp)
                            val zonedDateTime = instant.atZone(ZoneId.systemDefault())
                            zonedDateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a"))
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
                                    Text(
                                        text = note.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
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
                                Text(
                                    text = formattedDate,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, type: String, category: String, notes: String, account: String, toAccount: String?, timestamp: Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") } // EXPENSE, INCOME, or TRANSFER
    var notes by remember { mutableStateOf("") }
    var showCalculator by remember { mutableStateOf(false) }

    // Account states
    var account by remember { mutableStateOf("Cash") }
    var toAccount by remember { mutableStateOf<String?>("Saving") }

    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val incomeCategories by viewModel.incomeCategories.collectAsState()

    val categories = remember(type, expenseCategories, incomeCategories) {
        if (type == "EXPENSE") expenseCategories else incomeCategories
    }

    var category by remember(type, expenseCategories, incomeCategories) {
        mutableStateOf(if (type == "EXPENSE") expenseCategories.firstOrNull() ?: "Food" else incomeCategories.firstOrNull() ?: "Salary")
    }

    // Choose date state
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
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
            Text("Add Transaction", color = TextPrimary, fontWeight = FontWeight.Bold)
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

                // Account Selector (For EXPENSE or INCOME)
                if (type != "TRANSFER") {
                    Text(
                        text = if (type == "EXPENSE") "Pay From Account" else "Receive In Account",
                        fontSize = 12.sp,
                        color = NetYellow,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Cash", "Saving", "Credit").forEach { acc ->
                            val isSelected = account == acc
                            val selectedCol = if (type == "EXPENSE") ExpenseRed else IncomeGreen
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) selectedCol.copy(alpha = 0.15f) else DarkSurface,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) selectedCol else SurfaceBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { account = acc }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = acc,
                                    fontSize = 12.sp,
                                    color = if (isSelected) selectedCol else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Category selection list
                    Text(
                        text = "Category",
                        fontSize = 12.sp,
                        color = NetYellow,
                        fontWeight = FontWeight.Bold
                    )

                    val chunks = categories.chunked(3)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        chunks.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowItems.forEach { cat ->
                                    val isSelected = category == cat
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (isSelected) getCategoryColor(cat).copy(alpha = 0.15f) else DarkSurface,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) getCategoryColor(cat) else SurfaceBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { category = cat }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cat,
                                            fontSize = 11.sp,
                                            color = if (isSelected) getCategoryColor(cat) else TextPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (rowItems.size < 3) {
                                    repeat(3 - rowItems.size) {
                                        Box(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Double Column setup for Transfers (Source and Target)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Source Account", fontSize = 11.sp, color = NetYellow, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            listOf("Cash", "Saving", "Credit").forEach { acc ->
                                val isSelected = account == acc
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .background(
                                            if (isSelected) NetYellow.copy(alpha = 0.15f) else DarkSurface,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) NetYellow else SurfaceBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { 
                                            account = acc
                                            if (toAccount == acc) {
                                                toAccount = listOf("Cash", "Saving", "Credit").first { it != acc }
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(acc, fontSize = 12.sp, color = if (isSelected) NetYellow else TextPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Target Account", fontSize = 11.sp, color = Purple80, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            listOf("Cash", "Saving", "Credit").forEach { acc ->
                                val isEnabled = account != acc
                                val isSelected = toAccount == acc && isEnabled
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .background(
                                            if (isSelected) Purple80.copy(alpha = 0.15f) else if (isEnabled) DarkSurface else DarkSurface.copy(alpha = 0.4f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) Purple80 else SurfaceBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable(enabled = isEnabled) { toAccount = acc }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(acc, fontSize = 12.sp, color = if (isSelected) Purple80 else if (isEnabled) TextPrimary else TextSecondary.copy(alpha = 0.4f), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
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
    onDismiss: () -> Unit,
    onSave: (category: String, limit: Double, alertThreshold: Double) -> Unit
) {
    var category by remember { mutableStateOf("Food") }
    var limit by remember { mutableStateOf("") }
    var alertThreshold by remember { mutableStateOf(80f) } // Default threshold percentage
    val categories = listOf("Food", "Shopping", "Transport", "Utilities", "Entertainment", "Health", "Education", "Other")

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

                Text(
                    text = "Select Category",
                    fontSize = 12.sp,
                    color = NetYellow,
                    fontWeight = FontWeight.Bold
                )

                // Category chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val firstRow = categories.take(4)
                    val secondRow = categories.drop(4)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        firstRow.forEach { cat ->
                            val isSelected = category == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = getCategoryColor(cat).copy(alpha = 0.2f),
                                    selectedLabelColor = getCategoryColor(cat)
                                )
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        secondRow.forEach { cat ->
                            val isSelected = category == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = getCategoryColor(cat).copy(alpha = 0.2f),
                                    selectedLabelColor = getCategoryColor(cat)
                                )
                            )
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
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
    var showConfirmClear by remember { mutableStateOf(false) }

    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val incomeCategories by viewModel.incomeCategories.collectAsState()

    var categoryType by remember { mutableStateOf("EXPENSE") } // EXPENSE or INCOME
    val currentCategories = if (categoryType == "EXPENSE") expenseCategories else incomeCategories
    var newCategoryName by remember { mutableStateOf("") }
    var categoryToRename by remember { mutableStateOf<String?>(null) }
    var renameValue by remember { mutableStateOf("") }

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

    if (showConfirmClear) {
        AlertDialog(
            onDismissRequest = { showConfirmClear = false },
            title = { Text("Clear All Data?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete all transactions, budgets, and notes. This cannot be undone.", color = TextPrimary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAll()
                        showConfirmClear = false
                        onDismiss()
                    }
                ) {
                    Text("Delete Everything", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClear = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkBg,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = NetYellow, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text("Settings & Preferences", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Currency Preference Selection (USD, EUR, GBP, JPY, CAD, AUD)
                Text("Active Currency Display", fontSize = 13.sp, color = NetYellow, fontWeight = FontWeight.Bold)
                
                // Live Rates Status Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFetchingRates) "Updating live rates..." else "Rates via open.er-api.com",
                            fontSize = 11.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        if (apiError != null) {
                            Text(apiError, fontSize = 9.sp, color = ExpenseRed)
                        } else if (lastFetchedTime > 0L) {
                            Text("Updated live exchange rates", fontSize = 10.sp, color = TextSecondary)
                        } else {
                            Text("Using default fallback rates", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                    IconButton(
                        onClick = onRefreshRates,
                        enabled = !isFetchingRates,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh exchange rates",
                            tint = NetYellow,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                val currencyOptions = listOf(
                    "USD" to "USD ($)", 
                    "EUR" to "EUR (€)", 
                    "GBP" to "GBP (£)",
                    "JPY" to "JPY (¥)",
                    "CAD" to "CAD (C$)",
                    "AUD" to "AUD (A$)"
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in currencyOptions.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (j in 0..1) {
                                if (i + j < currencyOptions.size) {
                                    val (code, label) = currencyOptions[i + j]
                                    val isSelected = currentCurrencyCode == code
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (isSelected) NetYellow.copy(alpha = 0.15f) else DarkSurface,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) NetYellow else SurfaceBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { onCurrencyChange(code) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = label, 
                                                fontSize = 12.sp, 
                                                color = if (isSelected) NetYellow else TextPrimary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = NetYellow,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 4.dp))

                // Toggle Preferences
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Budget Alerts", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Notify when transaction exceeds limit", fontSize = 11.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = alertsEnabled,
                        onCheckedChange = onAlertsToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NetYellow,
                            checkedTrackColor = NetYellow.copy(alpha = 0.4f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = DarkSurface
                        )
                    )
                }

                HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 4.dp))

                // --- Cloud Sync Section ---
                Text("Web Cloud Synchronization", fontSize = 13.sp, color = NetYellow, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Real-time Sync", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Connects mobile cache with mbudgeting.netlify.app", fontSize = 11.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = syncEnabled,
                        onCheckedChange = onSyncToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NetYellow,
                            checkedTrackColor = NetYellow.copy(alpha = 0.4f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = DarkSurface
                        )
                    )
                }

                if (syncEnabled) {
                    var localUrl by remember { mutableStateOf(firebaseUrl) }
                    var localEmail by remember { mutableStateOf(syncEmail) }

                    LaunchedEffect(firebaseUrl) {
                        localUrl = firebaseUrl
                    }
                    LaunchedEffect(syncEmail) {
                        localEmail = syncEmail
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = localUrl,
                            onValueChange = {
                                localUrl = it
                                onUrlChange(it)
                            },
                            label = { Text("Firebase RTDB Web URL", color = TextSecondary, fontSize = 12.sp) },
                            placeholder = { Text("https://mbudgeting-default-rtdb.firebaseio.com", color = TextSecondary.copy(alpha = 0.5f)) },
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

                        OutlinedTextField(
                            value = localEmail,
                            onValueChange = {
                                localEmail = it
                                onEmailChange(it)
                            },
                            label = { Text("Sync Key / User Email", color = TextSecondary, fontSize = 12.sp) },
                            placeholder = { Text("adhamalshawafi@gmail.com", color = TextSecondary.copy(alpha = 0.5f)) },
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

                        // Status Info Block
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurface, RoundedCornerShape(8.dp))
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Synchronization Status", fontSize = 11.sp, color = TextSecondary)
                                        if (isSyncing) {
                                            Text("Syncing with cloud database...", fontSize = 13.sp, color = NetYellow, fontWeight = FontWeight.Bold)
                                        } else if (syncError != null) {
                                            Text(syncError, fontSize = 12.sp, color = ExpenseRed, fontWeight = FontWeight.Bold)
                                        } else if (lastSyncedTime > 0L) {
                                            val dateText = java.time.Instant.ofEpochMilli(lastSyncedTime)
                                                .atZone(java.time.ZoneId.systemDefault())
                                                .format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a"))
                                            Text("Synced: $dateText", fontSize = 12.sp, color = IncomeGreen, fontWeight = FontWeight.Bold)
                                        } else {
                                            Text("Offline cache active. Never synced yet.", fontSize = 12.sp, color = TextPrimary)
                                        }
                                    }
                                    
                                    if (isSyncing) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        CircularProgressIndicator(
                                            color = NetYellow,
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = onManualSync,
                                        enabled = !isSyncing,
                                        colors = ButtonDefaults.buttonColors(containerColor = NetYellow),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sync,
                                            contentDescription = "Sync",
                                            tint = DarkBg,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sync Web Now", color = DarkBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 4.dp))

                // Category Management Section
                Text("Manage Categories", fontSize = 13.sp, color = NetYellow, fontWeight = FontWeight.Bold)

                // Tab Switcher for Expense vs Income Categories
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (categoryType == "EXPENSE") ExpenseRedBg else Color.Transparent)
                            .clickable { categoryType = "EXPENSE" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "EXPENSE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (categoryType == "EXPENSE") ExpenseRed else TextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (categoryType == "INCOME") IncomeGreenBg else Color.Transparent)
                            .clickable { categoryType = "INCOME" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "INCOME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (categoryType == "INCOME") IncomeGreen else TextSecondary
                        )
                    }
                }

                // Add Category input field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        placeholder = { Text("New category name...", fontSize = 12.sp, color = TextSecondary.copy(alpha = 0.5f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NetYellow,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedLabelColor = NetYellow,
                            unfocusedLabelColor = TextSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
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
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add category", tint = DarkBg)
                    }
                }

                // List categories
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(12.dp))
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (currentCategories.isEmpty()) {
                        Text("No categories. Add one above!", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(8.dp))
                    } else {
                        currentCategories.forEach { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(getCategoryColor(cat), CircleShape)
                                    )
                                    Text(text = cat, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Rename button
                                    IconButton(
                                        onClick = {
                                            categoryToRename = cat
                                            renameValue = cat
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Rename $cat",
                                            tint = NetYellow,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Remove button
                                    IconButton(
                                        onClick = {
                                            if (categoryType == "EXPENSE") {
                                                viewModel.deleteExpenseCategory(cat)
                                            } else {
                                                viewModel.deleteIncomeCategory(cat)
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete $cat",
                                            tint = ExpenseRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            if (cat != currentCategories.last()) {
                                HorizontalDivider(color = SurfaceBorder.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 4.dp))

                // Delete Actions
                Text("Data Security", fontSize = 13.sp, color = NetYellow, fontWeight = FontWeight.Bold)

                Button(
                    onClick = { showConfirmClear = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.4f))
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "Clear All", tint = ExpenseRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Wipe All Databases", color = ExpenseRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = DarkBg,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
    )
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

