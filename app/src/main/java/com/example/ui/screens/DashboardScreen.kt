package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import coil.compose.AsyncImage
import com.example.data.model.Trade
import com.example.ui.components.EquityCurveChart
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.OpenBlue
import com.example.ui.util.Loc
import com.example.ui.viewmodel.JournalViewModel
import com.example.ui.viewmodel.TradeStats
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: JournalViewModel,
    onNavigateToAddTrade: () -> Unit,
    onNavigateToTradeDetail: (Int) -> Unit,
    onNavigateToTradeList: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val trades by viewModel.trades.collectAsState()
    val stats by viewModel.statistics.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val language by viewModel.language.collectAsState()
    val initialBalance by viewModel.initialBalance.collectAsState()
    val accountName by viewModel.accountName.collectAsState()
    val isAccountInitialized by viewModel.isAccountInitialized.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Dashboard, 1: Calendar, 2: Analysis, 3: Journal, 4: Goals

    val currencySymbol = when (currency) {
        "IRT" -> "تومان"
        "USDT" -> "USDT"
        else -> "$"
    }

    // Filter recent 5 trades
    val recentTrades = remember(trades) {
        trades.take(5)
    }

    // Dynamic Layout Direction based on selected language
    val layoutDirection = if (language == "en") LayoutDirection.Ltr else LayoutDirection.Rtl

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        // Initial Account Configuration Dialog
        if (!isAccountInitialized) {
            var tempName by remember { mutableStateOf("") }
            var tempBalanceStr by remember { mutableStateOf("") }
            var tempCurrency by remember { mutableStateOf("USD") }

            AlertDialog(
                onDismissRequest = {}, // Cannot dismiss until initialized
                confirmButton = {
                    Button(
                        onClick = {
                            val bal = tempBalanceStr.toDoubleOrNull() ?: 10000.0
                            val name = tempName.ifEmpty {
                                if (language == "fa") "حساب اصلی" else if (language == "ar") "حساب شخصي" else "Personal Account"
                            }
                            viewModel.initializeAccount(name, bal)
                            viewModel.setCurrency(tempCurrency)
                        },
                        enabled = tempBalanceStr.isNotEmpty() && (tempBalanceStr.toDoubleOrNull() ?: 0.0) >= 0.0
                    ) {
                        Text(Loc.tr("start", language))
                    }
                },
                title = {
                    Text(Loc.tr("initial_balance_title", language), fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(Loc.tr("initial_balance_desc", language), fontSize = 13.sp)

                        OutlinedTextField(
                            value = tempName,
                            onValueChange = { tempName = it },
                            label = { Text(Loc.tr("account_name", language)) },
                            placeholder = { Text(if (language == "fa") "حساب اصلی" else if (language == "ar") "حساب شخصي" else "Personal Account") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = tempBalanceStr,
                            onValueChange = { tempBalanceStr = it },
                            label = { Text(Loc.tr("initial_balance", language)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("10000") }
                        )

                        Text(Loc.tr("currency", language), style = MaterialTheme.typography.labelMedium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("USD" to "$", "IRT" to "تومان", "USDT" to "USDT").forEach { (code, sym) ->
                                val isSel = tempCurrency == code
                                FilterChip(
                                    selected = isSel,
                                    onClick = { tempCurrency = code },
                                    label = { Text("$code ($sym)", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            )
        }

        val resolvedAccountName = if (accountName == "حساب شخصی") Loc.tr("personal_account", language) else accountName
        val screenTitle = when (activeTab) {
            0 -> if (isAccountInitialized) resolvedAccountName else Loc.tr("dashboard_title", language)
            1 -> if (language == "fa") "تقویم معاملاتی" else "Trading Calendar"
            2 -> if (language == "fa") "تحلیل عملکرد پیشرفته" else "Performance Analytics"
            3 -> if (language == "fa") "دفترچه روانشناسی روزانه" else "Daily Psychology Journal"
            else -> if (language == "fa") "اهداف معاملاتی و چک‌لیست" else "Trading Goals & Rules"
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = screenTitle,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = Loc.tr("settings_title", language))
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNavigateToAddTrade,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = Loc.tr("add_trade", language))
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                ) {
                    NavigationBarItem(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                        label = { Text(if (language == "fa") "داشبورد" else "Dashboard", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                        label = { Text(if (language == "fa") "تقویم" else "Calendar", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                        label = { Text(if (language == "fa") "تحلیل" else "Analysis", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = activeTab == 3,
                        onClick = { activeTab = 3 },
                        icon = { Icon(Icons.Default.Book, contentDescription = null) },
                        label = { Text(if (language == "fa") "یادداشت" else "Journal", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = activeTab == 4,
                        onClick = { activeTab = 4 },
                        icon = { Icon(Icons.Default.Stars, contentDescription = null) },
                        label = { Text(if (language == "fa") "اهداف" else "Goals", fontSize = 10.sp) }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                when (activeTab) {
                    0 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Key Portfolio Stat Highlights
                            item {
                                PortfolioSummaryCard(
                                    stats = stats,
                                    currencySymbol = currencySymbol,
                                    initialBalance = initialBalance,
                                    lang = language
                                )
                            }

                            // Equity Curve
                            item {
                                EquityCurveChart(
                                    trades = trades,
                                    currencySymbol = currencySymbol,
                                    initialBalance = initialBalance,
                                    lang = language,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp)
                                )
                            }

                            // AI Smart Coach Summary
                            item {
                                DashboardAiSummaryCard(
                                    viewModel = viewModel,
                                    lang = language,
                                    onNavigateToAnalysis = { activeTab = 2 }
                                )
                            }

                            // Stats Grid
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    StatSmallCard(
                                        title = if (language == "fa") "بیشترین سود" else if (language == "ar") "أقصى ربح" else "Max Profit",
                                        value = "${if (stats.maxProfit >= 0.0) "+" else ""}${String.format(Locale.US, "%,.1f", stats.maxProfit)} $currencySymbol",
                                        icon = Icons.Default.TrendingUp,
                                        color = EmeraldGreen,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatSmallCard(
                                        title = if (language == "fa") "بیشترین ضرر" else if (language == "ar") "أقصى خسارة" else "Max Loss",
                                        value = "${String.format(Locale.US, "%,.1f", stats.maxLoss)} $currencySymbol",
                                        icon = Icons.Default.TrendingDown,
                                        color = CrimsonRed,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Recent Trades Section Header
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (language == "fa") "آخرین معاملات" else if (language == "ar") "آخر صفقات" else "Latest Trades",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    TextButton(onClick = onNavigateToTradeList) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(if (language == "fa") "مشاهده همه" else if (language == "ar") "عرض الكل" else "View All")
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.AutoMirrored.Filled.TrendingFlat,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // List of 5 recent trades
                            if (recentTrades.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (language == "fa") "هیچ معامله‌ای ثبت نشده است." else if (language == "ar") "لم يتم تسجيل أي صفقة بعد." else "No trades registered yet.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            } else {
                                items(recentTrades, key = { it.id }) { trade ->
                                    TradeItemRow(
                                        trade = trade,
                                        currencySymbol = currencySymbol,
                                        lang = language,
                                        onClick = { onNavigateToTradeDetail(trade.id) }
                                    )
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }
                    1 -> CalendarTab(viewModel, onNavigateToTradeDetail)
                    2 -> PerformanceTab(viewModel)
                    3 -> DailyJournalTab(viewModel)
                    4 -> GoalsTab(viewModel)
                }
            }
        }
    }
}

@Composable
fun PortfolioSummaryCard(
    stats: TradeStats,
    currencySymbol: String,
    initialBalance: Double,
    lang: String
) {
    val currentBalance = initialBalance + stats.totalPnL
    val isProfit = stats.totalPnL >= 0.0
    val pnlColor = if (isProfit) EmeraldGreen else CrimsonRed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = Loc.tr("current_balance", lang),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${String.format(Locale.US, "%,.2f", currentBalance)} $currencySymbol",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Total PnL (Next to win rate and trades!)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (lang == "fa") "سود/زیان کل" else if (lang == "ar") "إجمالي الربح/الخسارة" else "Total P&L",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${if (isProfit) "+" else ""}${String.format(Locale.US, "%,.1f", stats.totalPnL)} $currencySymbol",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = pnlColor
                    )
                }

                // Win Rate
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = Loc.tr("winrate", lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${String.format(Locale.US, "%.1f", stats.winRate)}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen
                    )
                }

                // Total Trades
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = Loc.tr("total_trades", lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${stats.totalTradesCount}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Open Trades
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = Loc.tr("open_trades", lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${stats.openTradesCount}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OpenBlue
                    )
                }
            }
        }
    }
}

@Composable
fun StatSmallCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

@Composable
fun TradeItemRow(
    trade: Trade,
    currencySymbol: String,
    lang: String = "fa",
    onClick: () -> Unit
) {
    val isProfit = (trade.pnl ?: 0.0) >= 0.0
    val isClosed = trade.exitPrice != null
    val trendColor = when {
        !isClosed -> OpenBlue
        isProfit -> EmeraldGreen
        else -> CrimsonRed
    }

    val statusText = when {
        !isClosed -> if (lang == "en") "Open" else if (lang == "ar") "مفتوحة" else "باز"
        isProfit -> if (lang == "en") "Win" else if (lang == "ar") "ربح" else "برد"
        else -> if (lang == "en") "Loss" else if (lang == "ar") "خسارة" else "باخت"
    }

    val sdf = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            if (trade.imagePath != null) {
                AsyncImage(
                    model = File(trade.imagePath),
                    contentDescription = "تصویر معامله",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            trendColor.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (trade.side == "BUY") Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = trendColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        trade.market,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Box(
                        modifier = Modifier
                            .background(
                                if (trade.side == "BUY") EmeraldGreen.copy(alpha = 0.12f) else CrimsonRed.copy(alpha = 0.12f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (trade.side == "BUY") "خرید" else "فروش",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (trade.side == "BUY") EmeraldGreen else CrimsonRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${sdf.format(Date(trade.dateTime))} • حجم: ${trade.volume}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            // PnL/Status column
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isClosed) {
                        "${if (isProfit) "+" else ""}${String.format(Locale.US, "%,.2f", trade.pnl)} $currencySymbol"
                    } else {
                        "باز"
                    },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = trendColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .background(trendColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = trendColor
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardAiSummaryCard(viewModel: JournalViewModel, lang: String, onNavigateToAnalysis: () -> Unit) {
    val advancedStatsOpt by viewModel.advancedStats.collectAsState()
    val stats = advancedStatsOpt ?: return

    val score = stats.disciplineScore
    val scoreColor = when {
        score >= 85 -> EmeraldGreen
        score >= 70 -> OpenBlue
        score >= 50 -> Color(0xFFF59E0B)
        else -> CrimsonRed
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToAnalysis() }
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🧠", fontSize = 20.sp)
                    Text(
                        text = if (lang == "fa") "آنالیز الگو و مربی هوشمند (AI)" else "AI Coaching & Patterns",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "${if (lang == "fa") "انضباط" else "Discipline"}: $score/100",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = scoreColor,
                    modifier = Modifier
                        .background(scoreColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            if (stats.insights.isEmpty()) {
                Text(
                    text = if (lang == "fa") "جهت دریافت تحلیل‌های هوشمند رفتار معاملاتی و مربیگری، حداقل ۳ معامله ثبت کنید."
                           else "Register at least 3 trades to unlock personalized coaching insights and behavior analysis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val primaryInsight = stats.insights.first()
                val insightColor = when (primaryInsight.type) {
                    com.example.data.analysis.InsightType.POSITIVE -> EmeraldGreen
                    com.example.data.analysis.InsightType.NEGATIVE -> CrimsonRed
                    com.example.data.analysis.InsightType.WARNING -> Color(0xFFF59E0B)
                    com.example.data.analysis.InsightType.INFO -> OpenBlue
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(insightColor, CircleShape)
                        )
                        Text(
                            text = primaryInsight.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = primaryInsight.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (lang == "fa") "مشاهده جزئیات تحلیل رفتار معاملاتی" else "View detailed performance psychology",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingFlat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
