package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.data.model.ChecklistItem
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.viewmodel.JournalViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GoalsTab(viewModel: JournalViewModel) {
    val trades by viewModel.trades.collectAsState()
    val checklistItems by viewModel.allChecklistItems.collectAsState()
    val language by viewModel.language.collectAsState()
    val currency by viewModel.currency.collectAsState()

    val currencySymbol = when (currency) {
        "IRT" -> "تومان"
        "USDT" -> "USDT"
        else -> "$"
    }

    // Goals settings values
    val dailyGoalVal by viewModel.dailyGoal.collectAsState()
    val weeklyGoalVal by viewModel.weeklyGoal.collectAsState()
    val monthlyGoalVal by viewModel.monthlyGoal.collectAsState()

    // Goals target editors states
    var showEditGoalsDialog by remember { mutableStateOf(false) }

    // Pre-trade checklist item addition text state
    var newRuleTitle by remember { mutableStateOf("") }

    // Period P&L calculations
    val (todayPnl, weekPnl, monthPnl) = remember(trades) {
        val cal = Calendar.getInstance()
        val closed = trades.filter { it.exitPrice != null }

        val todayCal = Calendar.getInstance()
        val todayYear = todayCal.get(Calendar.YEAR)
        val todayDayOfYear = todayCal.get(Calendar.DAY_OF_YEAR)

        val thisWeekVal = todayCal.get(Calendar.WEEK_OF_YEAR)
        val thisMonthVal = todayCal.get(Calendar.MONTH)

        var tPnl = 0.0
        var wPnl = 0.0
        var mPnl = 0.0

        for (t in closed) {
            cal.timeInMillis = t.dateTime
            val p = t.pnl ?: 0.0

            // Check today
            if (cal.get(Calendar.YEAR) == todayYear && cal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear) {
                tPnl += p
            }
            // Check this week
            if (cal.get(Calendar.YEAR) == todayYear && cal.get(Calendar.WEEK_OF_YEAR) == thisWeekVal) {
                wPnl += p
            }
            // Check this month
            if (cal.get(Calendar.YEAR) == todayYear && cal.get(Calendar.MONTH) == thisMonthVal) {
                mPnl += p
            }
        }
        Triple(tPnl, wPnl, mPnl)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Goals overview card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Stars, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = if (language == "fa") "اهداف سودآوری معاملاتی" else "Profit Targets & Goals",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = { showEditGoalsDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Goals", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Progress indicators
                    val goalsList = listOf(
                        GoalProgressItem(
                            title = if (language == "fa") "هدف روزانه" else "Daily Target",
                            current = todayPnl,
                            target = dailyGoalVal,
                            color = EmeraldGreen
                        ),
                        GoalProgressItem(
                            title = if (language == "fa") "هدف هفتگی" else "Weekly Target",
                            current = weekPnl,
                            target = weeklyGoalVal,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        GoalProgressItem(
                            title = if (language == "fa") "هدف ماهانه" else "Monthly Target",
                            current = monthPnl,
                            target = monthlyGoalVal,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    )

                    goalsList.forEach { goal ->
                        val ratio = if (goal.target > 0.0) {
                            (goal.current.coerceAtLeast(0.0) / goal.target).toFloat()
                        } else {
                            0f
                        }.coerceIn(0f, 1f)

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = goal.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "${String.format(Locale.US, "%,.0f", goal.current)} / ${String.format(Locale.US, "%,.0f", goal.target)} $currencySymbol",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (goal.current >= goal.target) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(modifier = Modifier.fillMaxWidth()) {
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    color = if (goal.current >= goal.target) EmeraldGreen else goal.color,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val percentage = ratio * 100
                                Text(
                                    text = "${String.format(Locale.US, "%.0f", percentage)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (goal.current >= goal.target) EmeraldGreen else goal.color
                                )

                                if (goal.current >= goal.target) {
                                    Text(
                                        text = if (language == "fa") "🎯 هدف پاس شد!" else "🎯 Target Met!",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else if (goal.current < 0.0) {
                                    Text(
                                        text = if (language == "fa") "⚠️ در ضرر" else "⚠️ In Loss",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CrimsonRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Checklist template builder
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.FactCheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = if (language == "fa") "قوانین چک‌لیست قبل از معامله" else "Pre-Trade Checklist Template",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = if (language == "fa") "این قوانین به عنوان قالب چک‌لیست معامله استفاده می‌شوند و قبل از باز کردن هر معامله ملزم به تایید آن‌ها هستید."
                               else "These rules form the pre-trade checklist that you must verify before saving any new trade entry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Checklist template items list
                    if (checklistItems.isEmpty()) {
                        Text(
                            text = if (language == "fa") "چک‌لیستی تعریف نشده است." else "No checklist items defined.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            checklistItems.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(onClick = { viewModel.deleteChecklistItem(item) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Rule",
                                            tint = CrimsonRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Add checklist item form
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newRuleTitle,
                            onValueChange = { newRuleTitle = it },
                            placeholder = { Text(if (language == "fa") "قانون جدید (مثال: بررسی اخبار بازار)" else "New rule (e.g. Check calendar)") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                if (newRuleTitle.isNotEmpty()) {
                                    viewModel.addChecklistItem(newRuleTitle)
                                    newRuleTitle = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(if (language == "fa") "افزودن" else "Add")
                        }
                    }
                }
            }
        }
    }

    // Edit Goals Dialog
    if (showEditGoalsDialog) {
        var tempDailyStr by remember { mutableStateOf(dailyGoalVal.toString()) }
        var tempWeeklyStr by remember { mutableStateOf(weeklyGoalVal.toString()) }
        var tempMonthlyStr by remember { mutableStateOf(monthlyGoalVal.toString()) }

        AlertDialog(
            onDismissRequest = { showEditGoalsDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val dVal = tempDailyStr.toDoubleOrNull() ?: dailyGoalVal
                        val wVal = tempWeeklyStr.toDoubleOrNull() ?: weeklyGoalVal
                        val mVal = tempMonthlyStr.toDoubleOrNull() ?: monthlyGoalVal
                        viewModel.setDailyGoal(dVal)
                        viewModel.setWeeklyGoal(wVal)
                        viewModel.setMonthlyGoal(mVal)
                        showEditGoalsDialog = false
                    }
                ) {
                    Text(if (language == "fa") "تایید" else "Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditGoalsDialog = false }) {
                    Text(if (language == "fa") "انصراف" else "Cancel")
                }
            },
            title = {
                Text(if (language == "fa") "تنظیم اهداف سود معاملاتی" else "Set Profit Goal Targets", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (language == "fa") "اهداف مورد نظر خود را بر اساس واحد ارز انتخابی خود تنظیم کنید."
                               else "Configure profit target limits in your selected trading currency unit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = tempDailyStr,
                        onValueChange = { tempDailyStr = it },
                        label = { Text(if (language == "fa") "هدف سود روزانه" else "Daily Target Profit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tempWeeklyStr,
                        onValueChange = { tempWeeklyStr = it },
                        label = { Text(if (language == "fa") "هدف سود هفتگی" else "Weekly Target Profit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tempMonthlyStr,
                        onValueChange = { tempMonthlyStr = it },
                        label = { Text(if (language == "fa") "هدف سود ماهانه" else "Monthly Target Profit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }
}

private data class GoalProgressItem(
    val title: String,
    val current: Double,
    val target: Double,
    val color: Color
)
