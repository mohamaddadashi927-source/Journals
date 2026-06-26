package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyJournal
import com.example.data.model.Trade
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.util.Loc
import com.example.ui.viewmodel.JournalViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarTab(
    viewModel: JournalViewModel,
    onNavigateToTradeDetail: (Int) -> Unit
) {
    val trades by viewModel.trades.collectAsState()
    val language by viewModel.language.collectAsState()
    val currency by viewModel.currency.collectAsState()

    val currencySymbol = when (currency) {
        "IRT" -> "تومان"
        "USDT" -> "USDT"
        else -> "$"
    }

    // Current viewed month/year in calendar
    var calendarState by remember { mutableStateOf(Calendar.getInstance()) }
    val currentYear = calendarState.get(Calendar.YEAR)
    val currentMonth = calendarState.get(Calendar.MONTH) // 0-indexed

    // Format current month string
    val monthYearString = remember(currentYear, currentMonth, language) {
        val sdf = if (language == "fa") {
            SimpleDateFormat("MMMM yyyy", Locale("fa", "IR"))
        } else {
            SimpleDateFormat("MMMM yyyy", Locale.US)
        }
        sdf.format(calendarState.time)
    }

    // Selected calendar day
    var selectedDayState by remember { mutableStateOf<Calendar?>(Calendar.getInstance()) }

    // Group closed trades by date string "yyyy-MM-dd"
    val dailyPnLMap = remember(trades) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val closed = trades.filter { it.exitPrice != null }
        closed.groupBy { sdf.format(Date(it.dateTime)) }
            .mapValues { entry -> entry.value.sumOf { it.pnl ?: 0.0 } }
    }

    // List of trades on selected day
    val selectedDayTrades = remember(trades, selectedDayState) {
        if (selectedDayState == null) emptyList()
        else {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val selectedDateStr = sdf.format(selectedDayState!!.time)
            trades.filter { sdf.format(Date(it.dateTime)) == selectedDateStr }
        }
    }

    // Selected day's journal note
    val selectedDateStr = remember(selectedDayState) {
        selectedDayState?.let { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it.time) } ?: ""
    }
    val dailyJournalFlow = remember(selectedDateStr) {
        viewModel.getDailyJournalByDate(selectedDateStr)
    }
    val dailyJournal by dailyJournalFlow.collectAsState(initial = null)

    // Editor modal or dialog state for Daily Notes
    var showJournalEditor by remember { mutableStateOf(false) }

    // Month Stats
    val monthStats = remember(trades, currentYear, currentMonth) {
        val cal = Calendar.getInstance()
        val monthTrades = trades.filter {
            cal.timeInMillis = it.dateTime
            cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
        }
        val closed = monthTrades.filter { it.exitPrice != null }
        val wins = closed.filter { (it.pnl ?: 0.0) > 0.0 }
        val netPnl = closed.sumOf { it.pnl ?: 0.0 }
        val winRate = if (closed.isNotEmpty()) (wins.size.toDouble() / closed.size) * 100.0 else 0.0
        Triple(monthTrades.size, netPnl, winRate)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month navigation header
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val newCal = Calendar.getInstance().apply {
                            time = calendarState.time
                            add(Calendar.MONTH, -1)
                        }
                        calendarState = newCal
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Month"
                        )
                    }

                    Text(
                        text = monthYearString,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(onClick = {
                        val newCal = Calendar.getInstance().apply {
                            time = calendarState.time
                            add(Calendar.MONTH, 1)
                        }
                        calendarState = newCal
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Month"
                        )
                    }
                }
            }
        }

        // Calendar Grid
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Week Days Header
                    val daysOfWeek = if (language == "fa") {
                        listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
                    } else {
                        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        daysOfWeek.forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Days list generation
                    val calendarGrid = remember(calendarState) {
                        val grid = mutableListOf<Calendar?>()
                        val tempCal = Calendar.getInstance().apply {
                            time = calendarState.time
                            set(Calendar.DAY_OF_MONTH, 1)
                        }
                        
                        // Fill leading empty spots based on first day of week
                        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) // 1-indexed (Sun=1, Mon=2...)
                        val emptySpots = if (language == "fa") {
                            // Persian week starts with Saturday (Calendar.SATURDAY = 7)
                            // Map Sunday to 1, Monday to 2, Tuesday to 3, Wednesday to 4, Thursday to 5, Friday to 6, Saturday to 0
                            val dayIndex = (firstDayOfWeek + 1) % 7 // Convert Sun=1 to 2, Sat=7 to 1
                            // We need Saturday to be index 0
                            val lead = when (firstDayOfWeek) {
                                Calendar.SATURDAY -> 0
                                Calendar.SUNDAY -> 1
                                Calendar.MONDAY -> 2
                                Calendar.TUESDAY -> 3
                                Calendar.WEDNESDAY -> 4
                                Calendar.THURSDAY -> 5
                                Calendar.FRIDAY -> 6
                                else -> 0
                            }
                            lead
                        } else {
                            firstDayOfWeek - 1
                        }

                        for (i in 0 until emptySpots) {
                            grid.add(null)
                        }

                        // Add all days of this month
                        val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        for (i in 1..maxDays) {
                            val dayCal = Calendar.getInstance().apply {
                                time = calendarState.time
                                set(Calendar.DAY_OF_MONTH, i)
                            }
                            grid.add(dayCal)
                        }
                        grid
                    }

                    // Render grid days in rows of 7
                    val rows = calendarGrid.chunked(7)
                    rows.forEach { week ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            week.forEach { dayCal ->
                                if (dayCal == null) {
                                    Box(modifier = Modifier.weight(1f))
                                } else {
                                    val dayNum = dayCal.get(Calendar.DAY_OF_MONTH)
                                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                    val formattedDate = sdf.format(dayCal.time)
                                    val dailyPnl = dailyPnLMap[formattedDate]

                                    val isSelected = selectedDayState != null &&
                                            selectedDayState!!.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                                            selectedDayState!!.get(Calendar.MONTH) == dayCal.get(Calendar.MONTH) &&
                                            selectedDayState!!.get(Calendar.DAY_OF_MONTH) == dayNum

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                else Color.Transparent
                                            )
                                            .clickable {
                                                selectedDayState = dayCal
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = dayNum.toString(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurface
                                            )

                                            if (dailyPnl != null) {
                                                val isDayProfit = dailyPnl >= 0.0
                                                val pnlText = if (dailyPnl == 0.0) "0"
                                                else if (Math.abs(dailyPnl) >= 1000) String.format(Locale.US, "%.1fk", dailyPnl / 1000.0)
                                                else String.format(Locale.US, "%.0f", dailyPnl)

                                                Text(
                                                    text = "${if (isDayProfit && dailyPnl > 0.0) "+" else ""}$pnlText",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isDayProfit) EmeraldGreen else CrimsonRed,
                                                    maxLines = 1
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
        }

        // Selected Month Statistics
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (language == "fa") "خلاصه عملکرد ماهانه" else "Monthly Performance Summary",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == "fa") "معاملات" else "Trades",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = monthStats.first.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == "fa") "سود/زیان خالص" else "Net P&L",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${if (monthStats.second >= 0.0) "+" else ""}${String.format(Locale.US, "%,.1f", monthStats.second)} $currencySymbol",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (monthStats.second >= 0.0) EmeraldGreen else CrimsonRed
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == "fa") "نسبت برد" else "Win Rate",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", monthStats.third)}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Daily Journal Card for the Selected Day
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EventNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == "fa") "دفترچه یادداشت روزانه" else "Daily Journal Log",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { showJournalEditor = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Text(
                                text = if (dailyJournal == null) (if (language == "fa") "ثبت یادداشت" else "Create Log")
                                       else (if (language == "fa") "ویرایش یادداشت" else "Edit Log"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (dailyJournal != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (dailyJournal!!.emotions.isNotEmpty()) {
                                Text(
                                    text = "🧠 " + (if (language == "fa") "حالت روحی احساسی: " else "Emotions: ") + dailyJournal!!.emotions,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (dailyJournal!!.content.isNotEmpty()) {
                                Text(
                                    text = "📝 " + (if (language == "fa") "خلاصه بازار: " else "Market Note: ") + dailyJournal!!.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (dailyJournal!!.mistakes.isNotEmpty()) {
                                Text(
                                    text = "❌ " + (if (language == "fa") "اشتباهات معاملاتی: " else "Mistakes: ") + dailyJournal!!.mistakes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CrimsonRed
                                )
                            }
                            if (dailyJournal!!.lessons.isNotEmpty()) {
                                Text(
                                    text = "💡 " + (if (language == "fa") "درس آموخته شده: " else "Lessons: ") + dailyJournal!!.lessons,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = EmeraldGreen
                                )
                            }
                        }
                    } else {
                        Text(
                            text = if (language == "fa") "هیچ خلاصه یادداشت روزانه‌ای برای این تاریخ ثبت نشده است." else "No journal logs or emotional notes saved for this date.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // Header for Trades list on selected day
        item {
            val formattedDate = selectedDayState?.let {
                val sdf = if (language == "fa") SimpleDateFormat("yyyy/MM/dd", Locale("fa", "IR"))
                          else SimpleDateFormat("yyyy-MM-dd", Locale.US)
                sdf.format(it.time)
            } ?: ""

            Text(
                text = "${if (language == "fa") "معاملات تاریخ" else "Trades for"} $formattedDate",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Render trades of the day
        if (selectedDayTrades.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (language == "fa") "هیچ معامله‌ای در این تاریخ ثبت نشده است." else "No trades executed on this date.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            items(selectedDayTrades, key = { it.id }) { trade ->
                TradeItemRow(
                    trade = trade,
                    currencySymbol = currencySymbol,
                    lang = language,
                    onClick = { onNavigateToTradeDetail(trade.id) }
                )
            }
        }
    }

    // Editor Sheet/Dialog for Daily Note
    if (showJournalEditor) {
        var noteContent by remember { mutableStateOf(dailyJournal?.content ?: "") }
        var noteEmotions by remember { mutableStateOf(dailyJournal?.emotions ?: "") }
        var noteMistakes by remember { mutableStateOf(dailyJournal?.mistakes ?: "") }
        var noteLessons by remember { mutableStateOf(dailyJournal?.lessons ?: "") }

        AlertDialog(
            onDismissRequest = { showJournalEditor = false },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedJournal = DailyJournal(
                            dateString = selectedDateStr,
                            content = noteContent,
                            emotions = noteEmotions,
                            mistakes = noteMistakes,
                            lessons = noteLessons
                        )
                        viewModel.saveDailyJournal(updatedJournal)
                        showJournalEditor = false
                    }
                ) {
                    Text(if (language == "fa") "ذخیره" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJournalEditor = false }) {
                    Text(if (language == "fa") "انصراف" else "Cancel")
                }
            },
            title = {
                Text(
                    text = (if (language == "fa") "ثبت یادداشت روزانه - " else "Daily Journal - ") + selectedDateStr,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = noteEmotions,
                        onValueChange = { noteEmotions = it },
                        label = { Text(if (language == "fa") "🧠 حالت روحی و احساسی شما" else "🧠 Feelings & Emotional State") },
                        placeholder = { Text(if (language == "fa") "مثال: آرام، نگران، غلبه طمع" else "e.g. Calm, anxious, greedy") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text(if (language == "fa") "📝 گزارش و خلاصه بازار امروز" else "📝 Today's Market Summary") },
                        placeholder = { Text(if (language == "fa") "روند کلی بازار، خبرهای مهم" else "General market trends, news") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = noteMistakes,
                        onValueChange = { noteMistakes = it },
                        label = { Text(if (language == "fa") "❌ اشتباهاتی که امروز مرتکب شدید" else "❌ Trading Mistakes Committed") },
                        placeholder = { Text(if (language == "fa") "مثال: ورود زودهنگام، عدم رعایت حد ضرر" else "e.g. FOMO, skipped stop loss") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = noteLessons,
                        onValueChange = { noteLessons = it },
                        label = { Text(if (language == "fa") "💡 درس اصلی آموخته شده امروز" else "💡 Key Lesson Learned Today") },
                        placeholder = { Text(if (language == "fa") "چه تغییری در رفتار خود ایجاد می‌کنید؟" else "How will you improve next time?") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        )
    }
}
