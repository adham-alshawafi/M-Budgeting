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

    val formattedDate = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
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
                ) {
                    Text(
                        text = "Daily",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formattedDate,
                            fontSize = 14.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Change Date",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
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

            Spacer(modifier = Modifier.height(20.dp))

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

            Text(
                text = "Today's Transactions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

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
    val categoryColor = getCategoryColor(transaction.category)
    val categoryIcon = getCategoryIcon(transaction.category)

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
                        text = transaction.category,
                        fontSize = 12.sp,
                        color = categoryColor,
                        fontWeight = FontWeight.SemiBold
                    )
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
                val prefix = if (transaction.type == "INCOME") "+" else "-"
                val textColor = if (transaction.type == "INCOME") IncomeGreen else ExpenseRed

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
    val dailyTransactions by viewModel.dailyTransactions.collectAsState()
    val dailyStats by viewModel.dailyStats.collectAsState()
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
                        val categoryColor = getCategoryColor(budget.category)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isBreached) 1.5.dp else 1.dp,
                                    color = if (isBreached) ExpenseRed else SurfaceBorder,
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
                                        color = if (isBreached) ExpenseRed else TextPrimary,
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
                                    color = if (isBreached) ExpenseRed else categoryColor,
                                    trackColor = SurfaceBorder
                                )

                                if (isBreached) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "⚠️ Budget Limit Breached!",
                                        fontSize = 11.sp,
                                        color = ExpenseRed,
                                        fontWeight = FontWeight.Bold
                                    )
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

// ==========================================
// 6. POPUPS & DIALOGS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, type: String, category: String, notes: String, timestamp: Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") } // EXPENSE or INCOME
    var category by remember { mutableStateOf("Food") }
    var notes by remember { mutableStateOf("") }
    var showCalculator by remember { mutableStateOf(false) }

    // Choose date state
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val computedDayOfWeek = remember(selectedDate) {
        selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    }

    val categories = listOf("Food", "Shopping", "Transport", "Utilities", "Entertainment", "Health", "Education", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val doubleAmount = evaluateMathExpression(amount) ?: amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotEmpty() && doubleAmount > 0) {
                        // Pass computed timestamp of chosen date
                        val timestamp = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        onSave(title, doubleAmount, type, category, notes, timestamp)
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
                // Type Switcher Button Group
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

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
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

                // Category dropdown menu trigger (custom selection flow)
                Text(
                    text = "Category",
                    fontSize = 12.sp,
                    color = NetYellow,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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

                // Notes Input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = NetYellow,
                        unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = NetYellow,
                        unfocusedBorderColor = SurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
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
    onSave: (category: String, limit: Double) -> Unit
) {
    var category by remember { mutableStateOf("Food") }
    var limit by remember { mutableStateOf("") }
    val categories = listOf("Food", "Shopping", "Transport", "Utilities", "Entertainment", "Health", "Education", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val dLimit = limit.toDoubleOrNull() ?: 0.0
                    if (dLimit > 0) {
                        onSave(category, dLimit)
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
    currentCurrency: String,
    alertsEnabled: Boolean,
    onDismiss: () -> Unit,
    onCurrencyChange: (String) -> Unit,
    onAlertsToggle: (Boolean) -> Unit,
    onClearAll: () -> Unit
) {
    var showConfirmClear by remember { mutableStateOf(false) }

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
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Currency Preference Selection (USD, EUR, GBP)
                Text("Currency System", fontSize = 13.sp, color = NetYellow, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val currencies = listOf("$" to "USD ($)", "€" to "EUR (€)", "£" to "GBP (£)")
                    currencies.forEach { (symbol, label) ->
                        val isSelected = currentCurrency == symbol
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCurrencyChange(symbol) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NetYellow.copy(alpha = 0.2f),
                                selectedLabelColor = NetYellow
                            )
                        )
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
