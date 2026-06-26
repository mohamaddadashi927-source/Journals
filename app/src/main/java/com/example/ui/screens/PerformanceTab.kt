package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.OpenBlue
import com.example.ui.util.Loc
import com.example.ui.viewmodel.JournalViewModel
import com.example.ui.viewmodel.TradeStats
import java.util.*

@Composable
fun PerformanceTab(viewModel: JournalViewModel) {
    val stats by viewModel.statistics.collectAsState()
    val language by viewModel.language.collectAsState()
    val currency by viewModel.currency.collectAsState()

    val currencySymbol = when (currency) {
        "IRT" -> "تومان"
        "USDT" -> "USDT"
        else -> "$"
    }

    var selectedSection by remember { mutableStateOf(0) } // 0: KPIs & Streaks, 1: Time & Assets, 2: Strategy & Grades

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section selector tabs
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedSection,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedSection]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    text = {
                        Text(
                            text = if (language == "fa") "شاخص‌های کلیدی و پیاپی" else "KPIs & Streaks",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
                Tab(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    text = {
                        Text(
                            text = if (language == "fa") "تحلیل زمانی و بازارها" else "Time & Markets",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
                Tab(
                    selected = selectedSection == 2,
                    onClick = { selectedSection = 2 },
                    text = {
                        Text(
                            text = if (language == "fa") "استراتژی‌ها و رتبه‌بندی" else "Strategies & Grades",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }

        if (stats.closedTradesCount == 0) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Text(
                            text = if (language == "fa") "جهت نمایش آمار پیشرفته، ابتدا باید معامله بسته‌شده ثبت کنید."
                                   else "Please register at least one closed trade to view advanced analysis.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            }
        } else {
            when (selectedSection) {
                0 -> {
                    // Section 0: Advanced mathematical indicators
                    item {
                        AdvancedKpisSection(stats = stats, currencySymbol = currencySymbol, lang = language)
                    }

                    item {
                        StreaksSection(stats = stats, lang = language)
                    }

                    item {
                        DistributionChartCard(stats = stats, lang = language)
                    }
                }
                1 -> {
                    // Section 1: Best/Worst Day, Hour, and Asset
                    item {
                        TimePeakSection(stats = stats, lang = language)
                    }

                    item {
                        WeeklyBarChartCard(stats = stats, lang = language, currencySymbol = currencySymbol)
                    }

                    item {
                        MarketBarChartCard(stats = stats, lang = language, currencySymbol = currencySymbol)
                    }
                }
                2 -> {
                    // Section 2: Strategy and setup ratings performance
                    item {
                        StrategyStatsSection(stats = stats, currencySymbol = currencySymbol, lang = language)
                    }

                    item {
                        GradesStatsSection(stats = stats, currencySymbol = currencySymbol, lang = language)
                    }
                }
            }
        }
    }
}

@Composable
fun AdvancedKpisSection(stats: TradeStats, currencySymbol: String, lang: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (lang == "fa") "شاخص‌های عملکردی پیشرفته" else "Advanced Performance KPIs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Grid of mathematical stats
            val items = listOf(
                Quadruple(
                    if (lang == "fa") "فاکتور سود (Profit Factor)" else "Profit Factor",
                    String.format(Locale.US, "%.2f", stats.profitFactor),
                    Icons.Default.QueryStats,
                    if (stats.profitFactor >= 1.5) EmeraldGreen else if (stats.profitFactor >= 1.0) OpenBlue else CrimsonRed
                ),
                Quadruple(
                    if (lang == "fa") "امید ریاضی (Expectancy)" else "Trade Expectancy",
                    "${if (stats.expectancy >= 0) "+" else ""}${String.format(Locale.US, "%,.2f", stats.expectancy)} $currencySymbol",
                    Icons.Default.Calculate,
                    if (stats.expectancy >= 0.0) EmeraldGreen else CrimsonRed
                ),
                Quadruple(
                    if (lang == "fa") "میانگین معامله برد" else "Average Win",
                    "+${String.format(Locale.US, "%,.2f", stats.avgWin)} $currencySymbol",
                    Icons.Default.TrendingUp,
                    EmeraldGreen
                ),
                Quadruple(
                    if (lang == "fa") "میانگین معامله باخت" else "Average Loss",
                    "${String.format(Locale.US, "%,.2f", stats.avgLoss)} $currencySymbol",
                    Icons.Default.TrendingDown,
                    CrimsonRed
                ),
                Quadruple(
                    if (lang == "fa") "ریسک به ریوارد واقعی" else "Actual Risk Reward Ratio",
                    "1 : ${String.format(Locale.US, "%.2f", stats.riskRewardRatio)}",
                    Icons.Default.CompareArrows,
                    MaterialTheme.colorScheme.onSurface
                ),
                Quadruple(
                    if (lang == "fa") "بیشترین افت حساب (Max DD)" else "Max Drawdown (DD)",
                    "${String.format(Locale.US, "%.1f", stats.maxDrawdownPct)}% (${String.format(Locale.US, "%,.0f", stats.maxDrawdownAbs)} $currencySymbol)",
                    Icons.Default.Percent,
                    CrimsonRed
                ),
                Quadruple(
                    if (lang == "fa") "فاکتور بازیابی (Recovery)" else "Recovery Factor",
                    String.format(Locale.US, "%.2f", stats.recoveryFactor),
                    Icons.Default.Autorenew,
                    if (stats.recoveryFactor >= 2.0) EmeraldGreen else OpenBlue
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { item ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = item.third,
                                            contentDescription = null,
                                            tint = item.fourth,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = item.first,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.second,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = item.fourth
                                    )
                                }
                            }
                        }
                        if (rowItems.size < 2) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreaksSection(stats: TradeStats, lang: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (lang == "fa") "سلسله بردها و باخت‌های پیاپی" else "Consecutive Streaks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.08f)),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, EmeraldGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EmeraldGreen)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (lang == "fa") "بیشترین برد پیاپی" else "Max Win Streak",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                        Text(
                            text = "${stats.maxWinStreak} " + (if (lang == "fa") "معامله" else "trades"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldGreen
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CrimsonRed.copy(alpha = 0.08f)),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, CrimsonRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.TrendingDown, contentDescription = null, tint = CrimsonRed)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (lang == "fa") "بیشترین باخت پیاپی" else "Max Loss Streak",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonRed
                        )
                        Text(
                            text = "${stats.maxLossStreak} " + (if (lang == "fa") "معامله" else "trades"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = CrimsonRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimePeakSection(stats: TradeStats, lang: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (lang == "fa") "تحلیل اوج عملکرد زمانی و نمادها" else "Peaks of Performance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val peaks = listOf(
                Triple(
                    if (lang == "fa") "بهترین روز هفته" else "Best Day of Week",
                    stats.bestDay,
                    EmeraldGreen
                ),
                Triple(
                    if (lang == "fa") "بدترین روز هفته" else "Worst Day of Week",
                    stats.worstDay,
                    CrimsonRed
                ),
                Triple(
                    if (lang == "fa") "بهترین ساعت معامله" else "Best Hour of Day",
                    stats.bestHour,
                    EmeraldGreen
                ),
                Triple(
                    if (lang == "fa") "بدترین ساعت معامله" else "Worst Hour of Day",
                    stats.worstHour,
                    CrimsonRed
                ),
                Triple(
                    if (lang == "fa") "سودآورترین نماد" else "Best Symbol",
                    stats.bestSymbol,
                    EmeraldGreen
                ),
                Triple(
                    if (lang == "fa") "زیان‌ده‌ترین نماد" else "Worst Symbol",
                    stats.worstSymbol,
                    CrimsonRed
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                peaks.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { peak ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(peak.third)
                                    )
                                    Column {
                                        Text(
                                            text = peak.first,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = peak.second,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = peak.third
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

@Composable
fun DistributionChartCard(stats: TradeStats, lang: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (lang == "fa") "توزیع معاملات سودده در برابر زیان‌ده" else "Win vs Loss Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Start
            )

            val winCount = stats.winsCount
            val lossCount = stats.lossesCount
            val total = winCount + lossCount

            if (total > 0) {
                val winPct = (winCount.toFloat() / total) * 360f

                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                    Canvas(modifier = Modifier.size(150.dp)) {
                        // Draw Win Arc
                        drawArc(
                            color = EmeraldGreen,
                            startAngle = -90f,
                            sweepAngle = winPct,
                            useCenter = false,
                            style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // Draw Loss Arc
                        drawArc(
                            color = CrimsonRed,
                            startAngle = -90f + winPct,
                            sweepAngle = 360f - winPct,
                            useCenter = false,
                            style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${String.format(Locale.US, "%.1f", stats.winRate)}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (lang == "fa") "نسبت برد" else "Win Rate",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(EmeraldGreen))
                        Text(
                            text = "${if (lang == "fa") "بردها" else "Wins"}: $winCount (${String.format(Locale.US, "%.0f", (winCount.toFloat() / total) * 100)}%)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(CrimsonRed))
                        Text(
                            text = "${if (lang == "fa") "باخت‌ها" else "Losses"}: $lossCount (${String.format(Locale.US, "%.0f", (lossCount.toFloat() / total) * 100)}%)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StrategyStatsSection(stats: TradeStats, currencySymbol: String, lang: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (lang == "fa") "عملکرد استراتژی‌های معاملاتی" else "Strategy Performance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (stats.pnlByStrategy.isEmpty()) {
                Text(
                    text = if (lang == "fa") "هیچ معامله‌ای بر اساس استراتژی ثبت نشده است. هنگام افزودن معامله استراتژی تعریف کنید."
                           else "No trades with defined strategy found. Tag your strategies to view stats here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stats.pnlByStrategy.forEach { (strategy, pnl) ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Extension,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = strategy,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = "${if (pnl >= 0.0) "+" else ""}${String.format(Locale.US, "%,.1f", pnl)} $currencySymbol",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (pnl >= 0.0) EmeraldGreen else CrimsonRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GradesStatsSection(stats: TradeStats, currencySymbol: String, lang: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (lang == "fa") "عملکرد بر اساس امتیازدهی معامله (Grades)" else "Performance by Setup Grade",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (stats.pnlByGrade.isEmpty()) {
                Text(
                    text = if (lang == "fa") "معامله‌ای با امتیاز (A+, A, B...) ثبت نشده است."
                           else "No graded trades found. Assign setups a score (A+, A, B, etc.) to analyze quality.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            } else {
                val gradeOrder = listOf("A+", "A", "B", "C", "F")
                val sortedGrades = stats.pnlByGrade.keys.sortedWith { g1, g2 ->
                    val i1 = gradeOrder.indexOf(g1).let { if (it == -1) 99 else it }
                    val i2 = gradeOrder.indexOf(g2).let { if (it == -1) 99 else it }
                    i1.compareTo(i2)
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sortedGrades.forEach { grade ->
                        val pnl = stats.pnlByGrade[grade] ?: 0.0
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (grade) {
                                                    "A+", "A" -> EmeraldGreen.copy(alpha = 0.15f)
                                                    "B" -> OpenBlue.copy(alpha = 0.15f)
                                                    else -> CrimsonRed.copy(alpha = 0.15f)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = grade,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = when (grade) {
                                                "A+", "A" -> EmeraldGreen
                                                "B" -> OpenBlue
                                                else -> CrimsonRed
                                            }
                                        )
                                    }
                                    Text(
                                        text = (if (lang == "fa") "امتیاز " else "Grade ") + grade,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = "${if (pnl >= 0.0) "+" else ""}${String.format(Locale.US, "%,.1f", pnl)} $currencySymbol",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (pnl >= 0.0) EmeraldGreen else CrimsonRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyBarChartCard(stats: TradeStats, lang: String, currencySymbol: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (lang == "fa") "سود و زیان به تفکیک روز هفته" else "PnL by Day of the Week",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Days order
            val daysOrder = listOf(
                Calendar.SATURDAY,
                Calendar.SUNDAY,
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY
            )

            val maxPnl = stats.pnlByDayOfWeek.values.map { Math.abs(it) }.maxOrNull() ?: 1.0

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                daysOrder.forEach { dayKey ->
                    val pnl = stats.pnlByDayOfWeek[dayKey] ?: 0.0
                    val dayName = when (dayKey) {
                        Calendar.SATURDAY -> if (lang == "fa") "شنبه" else "Sat"
                        Calendar.SUNDAY -> if (lang == "fa") "یکشنبه" else "Sun"
                        Calendar.MONDAY -> if (lang == "fa") "دوشنبه" else "Mon"
                        Calendar.TUESDAY -> if (lang == "fa") "سه‌شنبه" else "Tue"
                        Calendar.WEDNESDAY -> if (lang == "fa") "چهارشنبه" else "Wed"
                        Calendar.THURSDAY -> if (lang == "fa") "پنجشنبه" else "Thu"
                        Calendar.FRIDAY -> if (lang == "fa") "جمعه" else "Fri"
                        else -> ""
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(60.dp)
                        )

                        // Bar Container
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                        ) {
                            val ratio = (Math.abs(pnl) / maxPnl).toFloat().coerceIn(0f, 1f)
                            if (pnl != 0.0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(ratio)
                                        .background(if (pnl > 0.0) EmeraldGreen else CrimsonRed)
                                        .align(Alignment.CenterStart)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "${if (pnl >= 0) "+" else ""}${String.format(Locale.US, "%.0f", pnl)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (pnl > 0.0) EmeraldGreen else if (pnl < 0.0) CrimsonRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(60.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarketBarChartCard(stats: TradeStats, lang: String, currencySymbol: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (lang == "fa") "عملکرد سوددهی بر اساس نماد (پنج نماد برتر)" else "PnL by Traded Markets (Top 5)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val sortedMarkets = remember(stats.pnlBySymbol) {
                stats.pnlBySymbol.toList().sortedByDescending { Math.abs(it.second) }.take(5)
            }

            if (sortedMarkets.isEmpty()) {
                Text(
                    text = if (lang == "fa") "هیچ بازاری معامله نشده است." else "No markets traded yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            } else {
                val maxPnl = sortedMarkets.map { Math.abs(it.second) }.maxOrNull() ?: 1.0

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    sortedMarkets.forEach { (market, pnl) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = market,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(75.dp),
                                maxLines = 1
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                            ) {
                                val ratio = (Math.abs(pnl) / maxPnl).toFloat().coerceIn(0f, 1f)
                                if (pnl != 0.0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(ratio)
                                            .background(if (pnl > 0.0) EmeraldGreen else CrimsonRed)
                                            .align(Alignment.CenterStart)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "${if (pnl >= 0) "+" else ""}${String.format(Locale.US, "%.0f", pnl)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (pnl > 0.0) EmeraldGreen else if (pnl < 0.0) CrimsonRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(60.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
