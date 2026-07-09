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
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.isSystemInDarkTheme
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val trades by viewModel.trades.collectAsStateWithLifecycle()
    val stats by viewModel.statistics.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    val isDark = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }
    val cardBg = if (isDark) Color(0xFF0C0E12) else MaterialTheme.colorScheme.surface
    val cardBorder = if (isDark) Color(0xFF1F222B) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val textMuted = if (isDark) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant
    val initialBalance by viewModel.initialBalance.collectAsStateWithLifecycle()
    val accountName by viewModel.accountName.collectAsStateWithLifecycle()
    val isAccountInitialized by viewModel.isAccountInitialized.collectAsStateWithLifecycle()
    val allJournals by viewModel.allDailyJournals.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Dashboard, 1: Calendar, 2: Analysis, 3: Journal, 4: Goals
    var showDailyReviewDialog by remember { mutableStateOf(false) }

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
        // Initial Account Configuration Flow
        if (!isAccountInitialized) {
            var showLanguageSelection by remember { mutableStateOf(true) }

            if (showLanguageSelection) {
                var selectedLanguageCode by remember { mutableStateOf(language) }

                AlertDialog(
                    onDismissRequest = {}, // Cannot dismiss until completed
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.setLanguage(selectedLanguageCode)
                                showLanguageSelection = false
                            },
                            modifier = Modifier.testTag("lang_continue_button")
                        ) {
                            Text("Continue")
                        }
                    },
                    title = {
                        Text("Select App Language", fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Please select your preferred language. All sections and configurations will instantly adapt to your choice.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val langOptions = listOf(
                                Triple("en", "English", "🇺🇸"),
                                Triple("fa", "فارسی (Persian)", "🇮🇷"),
                                Triple("ar", "العربية (Arabic)", "🇸🇦")
                            )

                            langOptions.forEach { (code, name, flag) ->
                                val isSelected = selectedLanguageCode == code
                                Card(
                                    onClick = { 
                                        selectedLanguageCode = code
                                        viewModel.setLanguage(code)
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("lang_option_$code")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(flag, fontSize = 20.sp)
                                        Text(name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { 
                                                selectedLanguageCode = code
                                                viewModel.setLanguage(code)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            } else {
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
                            enabled = tempBalanceStr.isNotEmpty() && (tempBalanceStr.toDoubleOrNull() ?: 0.0) >= 0.0,
                            modifier = Modifier.testTag("account_start_button")
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
                                modifier = Modifier.fillMaxWidth().testTag("account_name_input")
                            )

                            OutlinedTextField(
                                value = tempBalanceStr,
                                onValueChange = { tempBalanceStr = it },
                                label = { Text(Loc.tr("initial_balance", language)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth().testTag("initial_balance_input"),
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
                                        label = { Text("$code ($sym)", fontSize = 11.sp) },
                                        modifier = Modifier.testTag("currency_chip_$code")
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        val resolvedAccountName = if (accountName == "حساب شخصی") Loc.tr("personal_account", language) else accountName
        val screenTitle = when (activeTab) {
            0 -> if (isAccountInitialized) resolvedAccountName else Loc.tr("dashboard_title", language)
            1 -> if (language == "fa") "تقویم معاملاتی" else "Trading Calendar"
            2 -> if (language == "fa") "تحلیل عملکرد پیشرفته" else "Performance Analytics"
            3 -> if (language == "fa") "دفترچه روانشناسی روزانه" else "Daily Psychology Journal"
            else -> if (language == "fa") "اهداف معاملاتی و چک‌لیست" else "Trading Goals & Rules"
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 720.dp

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
                            containerColor = if (activeTab == 0 && isDark) Color.Black else MaterialTheme.colorScheme.background,
                            titleContentColor = if (activeTab == 0 && isDark) Color.White else MaterialTheme.colorScheme.onBackground,
                            actionIconContentColor = if (activeTab == 0 && isDark) Color.White else MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = onNavigateToAddTrade,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(bottom = if (isWideScreen) 0.dp else 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = Loc.tr("add_trade", language))
                    }
                },
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar(
                            containerColor = if (activeTab == 0 && isDark) Color(0xFF0C0E12) else if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
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
                }
            ) { paddingValues ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (activeTab == 0 && isDark) Color.Black else MaterialTheme.colorScheme.background)
                        .padding(paddingValues)
                ) {
                    if (isWideScreen) {
                        NavigationRail(
                            containerColor = if (activeTab == 0 && isDark) Color(0xFF0C0E12) else if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
                            header = {
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            NavigationRailItem(
                                selected = activeTab == 0,
                                onClick = { activeTab = 0 },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                                label = { Text(if (language == "fa") "داشبورد" else "Dashboard") }
                            )
                            NavigationRailItem(
                                selected = activeTab == 1,
                                onClick = { activeTab = 1 },
                                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                                label = { Text(if (language == "fa") "تقویم" else "Calendar") }
                            )
                            NavigationRailItem(
                                selected = activeTab == 2,
                                onClick = { activeTab = 2 },
                                icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                                label = { Text(if (language == "fa") "تحلیل" else "Analysis") }
                            )
                            NavigationRailItem(
                                selected = activeTab == 3,
                                onClick = { activeTab = 3 },
                                icon = { Icon(Icons.Default.Book, contentDescription = null) },
                                label = { Text(if (language == "fa") "یادداشت" else "Journal") }
                            )
                            NavigationRailItem(
                                selected = activeTab == 4,
                                onClick = { activeTab = 4 },
                                icon = { Icon(Icons.Default.Stars, contentDescription = null) },
                                label = { Text(if (language == "fa") "اهداف" else "Goals") }
                            )
                        }
                    }

                    Box(
                        modifier = if (isWideScreen) {
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 24.dp)
                        } else {
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 16.dp)
                        },
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = if (isWideScreen) {
                                Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 1000.dp)
                            } else {
                                Modifier.fillMaxSize()
                            }
                        ) {
                when (activeTab) {
                    0 -> {
                        val labels = remember(language) {
                            object {
                                val welcome = if (language == "fa") "خوش‌آمدید،" else if (language == "ar") "مرحباً،" else "Welcome back,"
                                val keyMetrics = if (language == "fa") "شاخص‌های عملکردی کلیدی" else if (language == "ar") "المؤشرات الرئيسية" else "Key Performance Metrics"
                                val netPnl = if (language == "fa") "سود / زیان خالص" else if (language == "ar") "صافي الربح والخسارة" else "Net Profit / Loss"
                                val winRate = if (language == "fa") "وین‌ریت (درصد برد)" else if (language == "ar") "نسبة الفوز" else "Win Rate"
                                val profitFactor = if (language == "fa") "ضریب سود (Profit Factor)" else if (language == "ar") "عامل الربح" else "Profit Factor"
                                val keyInsight = if (language == "fa") "بینش برجسته مربی هوشمند" else if (language == "ar") "رؤية المدرب الذكي المميزة" else "Highlighted Coach Insight"
                                val dailySummary = if (language == "fa") "خلاصه معاملات امروز" else if (language == "ar") "ملخص صفقات اليوم" else "Today's Daily Summary"
                                val tradesToday = if (language == "fa") "تعداد معاملات امروز" else if (language == "ar") "صفقات اليوم" else "Trades Today"
                                val todayResult = if (language == "fa") "برآیند سود/زیان خالص امروز" else if (language == "ar") "النتيجة الصافية اليوم" else "Today's Net Result"
                                val noTrades = if (language == "fa") "هیچ معامله‌ای برای امروز ثبت نشده است." else if (language == "ar") "لم يتم تسجيل أي صفقات اليوم." else "No trades registered for today."
                                val noInsightTitle = if (language == "fa") "داده‌های معاملاتی ناکافی" else if (language == "ar") "بيانات تداول غير كافية" else "Insufficient Trading Data"
                                val noInsightDesc = if (language == "fa") "حداقل ۳ معامله ثبت کنید تا تحلیل‌های هوشمند رفتار معاملاتی و مربی روانشناسی فعال شود." else if (language == "ar") "سجل ٣ صفقات على الأقل لتفعيل التحليلات الذكية للأداء." else "Log at least 3 trades to unlock personalized, AI-driven behavior insights."
                                val tradesUnit = if (language == "fa") "معامله" else if (language == "ar") "صفقة" else "trades"
                                
                                val balanceTitle = if (language == "fa") "بالانس کل حساب" else if (language == "ar") "رصيد الحساب الإجمالي" else "Total Account Balance"
                                val riskReward = if (language == "fa") "ریسک به ریوارد" else if (language == "ar") "نسبة المخاطرة للمكافأة" else "Risk to Reward"
                                val totalTrades = if (language == "fa") "تعداد کل معاملات" else if (language == "ar") "إجمالي الصفقات" else "Total Trades"
                                val tradesListHeader = if (language == "fa") "لیست معاملات اخیر" else if (language == "ar") "أحدث الصفقات" else "Recent Trades List"
                                val equityCurveHeader = if (language == "fa") "نمودار رشد بالانس (Equity Curve)" else if (language == "ar") "منحنى نمو الرصيد" else "Equity Growth Curve"
                            }
                        }

                        val advancedStatsOpt by viewModel.advancedStats.collectAsStateWithLifecycle()

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            if (isWideScreen) {
                                // Side-by-side Key Metrics and Equity Curve
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        // 1. Key Metrics Card (takes weight 1)
                                        val currentBalance = initialBalance + stats.totalPnL
                                        val isPnlProfit = stats.totalPnL >= 0.0
                                        val pnlColor = if (stats.totalPnL > 0.0) Color(0xFF10B981) else if (stats.totalPnL < 0.0) Color(0xFFEF4444) else Color.White
                                        val pnlPrefix = if (stats.totalPnL > 0.0) "+" else ""

                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("key_metrics_card"),
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = cardBg
                                            ),
                                            border = BorderStroke(1.dp, cardBorder)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(20.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                // Header
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = labels.keyMetrics,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.Analytics,
                                                        contentDescription = null,
                                                        tint = Color(0xFF94A3B8),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(18.dp))

                                                // Prominent Balance
                                                Text(
                                                    text = labels.balanceTitle,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF64748B),
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "${String.format(Locale.US, "%,.2f", currentBalance)} $currencySymbol",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    textAlign = TextAlign.Center
                                                )

                                                Spacer(modifier = Modifier.height(18.dp))
                                                HorizontalDivider(color = cardBorder)
                                                Spacer(modifier = Modifier.height(16.dp))

                                                // Smaller stats in 2x2 grid
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    // Win Rate
                                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            text = labels.winRate,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFF64748B),
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = "${String.format(Locale.US, "%.1f", stats.winRate)}%",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF10B981)
                                                        )
                                                    }

                                                    // Risk to Reward
                                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            text = labels.riskReward,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFF64748B),
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = "1 : ${String.format(Locale.US, "%.2f", stats.riskRewardRatio)}",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF3B82F6)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    // Total Trades
                                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            text = labels.totalTrades,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFF64748B),
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = "${stats.totalTradesCount}",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                    }

                                                    // Net PnL (Profit/Loss)
                                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            text = labels.netPnl,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFF64748B),
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = "$pnlPrefix${String.format(Locale.US, "%,.1f", stats.totalPnL)} $currencySymbol",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = pnlColor
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // 2. Equity Curve Card (takes weight 1.2)
                                        Card(
                                            modifier = Modifier
                                                .weight(1.2f)
                                                .testTag("equity_curve_card"),
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(containerColor = cardBg),
                                            border = BorderStroke(1.dp, cardBorder)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = labels.equityCurveHeader,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.ShowChart,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))

                                                EquityCurveChart(
                                                    trades = trades,
                                                    currencySymbol = currencySymbol,
                                                    initialBalance = initialBalance,
                                                    lang = language,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(236.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // 1. KEY PERFORMANCE METRICS CARD (Balance prominent, other stats in smaller size)
                                item {
                                    val currentBalance = initialBalance + stats.totalPnL
                                    val isPnlProfit = stats.totalPnL >= 0.0
                                    val pnlColor = if (stats.totalPnL > 0.0) Color(0xFF10B981) else if (stats.totalPnL < 0.0) Color(0xFFEF4444) else Color.White
                                    val pnlPrefix = if (stats.totalPnL > 0.0) "+" else ""

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("key_metrics_card"),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = cardBg
                                        ),
                                        border = BorderStroke(1.dp, cardBorder)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            // Header
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = labels.keyMetrics,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF94A3B8)
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.Analytics,
                                                    contentDescription = null,
                                                    tint = Color(0xFF94A3B8),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(18.dp))

                                            // Prominent Balance
                                            Text(
                                                text = labels.balanceTitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF64748B),
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${String.format(Locale.US, "%,.2f", currentBalance)} $currencySymbol",
                                                style = MaterialTheme.typography.headlineLarge,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.primary,
                                                textAlign = TextAlign.Center
                                            )

                                            Spacer(modifier = Modifier.height(18.dp))
                                            HorizontalDivider(color = cardBorder)
                                            Spacer(modifier = Modifier.height(16.dp))

                                            // Smaller stats in 2x2 grid
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                // Win Rate
                                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = labels.winRate,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFF64748B),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "${String.format(Locale.US, "%.1f", stats.winRate)}%",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF10B981)
                                                    )
                                                }

                                                // Risk to Reward
                                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = labels.riskReward,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFF64748B),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "1 : ${String.format(Locale.US, "%.2f", stats.riskRewardRatio)}",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF3B82F6)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                // Total Trades
                                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = labels.totalTrades,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFF64748B),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "${stats.totalTradesCount}",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }

                                                // Net PnL (Profit/Loss)
                                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = labels.netPnl,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFF64748B),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "$pnlPrefix${String.format(Locale.US, "%,.1f", stats.totalPnL)} $currencySymbol",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = pnlColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // 2. EQUITY CURVE CHART (Vital visual section on Dashboard)
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("equity_curve_card"),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(containerColor = cardBg),
                                        border = BorderStroke(1.dp, cardBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = labels.equityCurveHeader,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF94A3B8)
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.ShowChart,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))

                                            EquityCurveChart(
                                                trades = trades,
                                                currencySymbol = currencySymbol,
                                                initialBalance = initialBalance,
                                                lang = language,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(200.dp)
                                            )
                                        }
                                    }
                                }
                            }



                            // 3. Simple Daily Summary Card
                            item {
                                val todayStats = remember(trades) {
                                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                    val todayStr = sdf.format(Date())
                                    val todayClosed = trades.filter { t -> sdf.format(Date(t.dateTime)) == todayStr && t.exitPrice != null }
                                    val todayAll = trades.filter { t -> sdf.format(Date(t.dateTime)) == todayStr }
                                    val pnl = todayClosed.sumOf { it.pnl ?: 0.0 }
                                    val count = todayAll.size
                                    Pair(count, pnl)
                                }
                                val todayCount = todayStats.first
                                val todayPnl = todayStats.second

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showDailyReviewDialog = true }
                                        .testTag("daily_summary_card"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = cardBg
                                    ),
                                    border = BorderStroke(1.dp, cardBorder)
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = labels.dailySummary,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF94A3B8)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.CalendarMonth,
                                                contentDescription = null,
                                                tint = Color(0xFF3B82F6),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = labels.tradesToday,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF64748B),
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "$todayCount ${labels.tradesUnit}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = labels.todayResult,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF64748B),
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                val resultColor = if (todayPnl > 0.0) Color(0xFF10B981) else if (todayPnl < 0.0) Color(0xFFEF4444) else Color.White
                                                val resultPrefix = if (todayPnl > 0.0) "+" else ""
                                                Text(
                                                    text = "$resultPrefix${String.format(Locale.US, "%,.2f", todayPnl)} $currencySymbol",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = resultColor
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(color = cardBorder)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (language == "fa") "مشاهده بازنگری روزانه تفصیلی ➔" else if (language == "ar") "عرض المراجعة اليومية التفصيلية ➔" else "View Detailed Daily Review ➔",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF3B82F6),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }

                            // 4. TRADES LIST CARD (Highly requested on Dashboard)
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("recent_trades_card"),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardBg),
                                    border = BorderStroke(1.dp, cardBorder)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = labels.tradesListHeader,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF94A3B8)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.List,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        if (recentTrades.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = labels.noTrades,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        } else {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                recentTrades.forEach { trade ->
                                                    TradeItemRow(
                                                        trade = trade,
                                                        currencySymbol = currencySymbol,
                                                        lang = language,
                                                        onClick = { onNavigateToTradeDetail(trade.id) }
                                                    )
                                                }
                                            }
                                        }
                                    }
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
            }

        if (showDailyReviewDialog) {
            DailyReviewDialog(
                trades = trades,
                journals = allJournals,
                lang = language,
                currencySymbol = currencySymbol,
                onDismiss = { showDailyReviewDialog = false }
            )
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
    val advancedStatsOpt by viewModel.advancedStats.collectAsStateWithLifecycle()
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

@Composable
fun DailyReviewTriggerCard(
    trades: List<Trade>,
    journals: List<com.example.data.model.DailyJournal>,
    lang: String,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val todayStr = sdf.format(Date())
    val todayTrades = trades.filter { t -> sdf.format(Date(t.dateTime)) == todayStr }
    val todayClosed = todayTrades.filter { it.exitPrice != null }
    val todayPnl = todayClosed.sumOf { it.pnl ?: 0.0 }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                1.dp,
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("📅", fontSize = 20.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (lang == "fa") "امروز در یک نگاه (Daily Review)" else "Daily Review Mode",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = if (lang == "fa") {
                        if (todayTrades.isEmpty()) "هیچ تریدی امروز ثبت نشده. صبور باشید!"
                        else "تعداد ${todayTrades.size} ترید با برآیند ${if (todayPnl >= 0.0) "+" else ""}${String.format(Locale.US, "%,.1f", todayPnl)} $currencySymbol"
                    } else {
                        if (todayTrades.isEmpty()) "No trades logged today yet. Review strategy rules!"
                        else "Logged ${todayTrades.size} trades with a net of ${if (todayPnl >= 0.0) "+" else ""}${String.format(Locale.US, "%,.1f", todayPnl)} $currencySymbol"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingFlat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun DailyReviewDialog(
    trades: List<Trade>,
    journals: List<com.example.data.model.DailyJournal>,
    lang: String,
    currencySymbol: String,
    onDismiss: () -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val todayStr = sdf.format(Date())
    
    val todayTrades = trades.filter { t -> sdf.format(Date(t.dateTime)) == todayStr }
    val todayClosed = todayTrades.filter { it.exitPrice != null }
    val todayPnl = todayClosed.sumOf { it.pnl ?: 0.0 }
    val todayWins = todayClosed.count { (it.pnl ?: 0.0) >= 0.0 }
    val todayLosses = todayClosed.count { (it.pnl ?: 0.0) < 0.0 }
    
    val todayJournal = journals.find { it.dateString == todayStr }
    val todayMistake = todayJournal?.mistakes?.ifEmpty { null } 
        ?: if (todayTrades.size > 5) (if (lang == "fa") "بیش‌معاملاتی" else "Overtrading") else null
    val todayEmotions = todayJournal?.emotions?.ifEmpty { null }
        ?: todayTrades.map { it.emotionalState }.filter { it.isNotEmpty() }.distinct().joinToString(", ").ifEmpty { null }

    val todayInsight = when {
        todayTrades.isEmpty() -> if (lang == "fa") "امروز هیچ معامله‌ای انجام نداده‌اید. صبوری شما در انتظار فرصت طلایی عالی است." else "No trades completed today. Great patience waiting for prime setups."
        todayPnl > 0.0 && todayLosses == 0 -> if (lang == "fa") "یک روز بی‌نقص! تمام معاملات امروز سودده بودند. پایبندی به استراتژی عالی بود." else "A flawless day! 100% of today's closed trades were in profit."
        todayPnl > 0.0 -> if (lang == "fa") "روز موفقی بود. برآیند شما مثبت است. سودها را تثبیت کنید." else "Successful day! Your net performance is positive."
        todayTrades.size > 5 -> if (lang == "fa") "هشدار بیش‌معاملاتی! تعداد تریدهای امروز بالا بود. خستگی ذهنی را جدی بگیرید." else "Overtrading detected! High transaction frequency increases mental fatigue."
        todayLosses > todayWins -> if (lang == "fa") "روز سختی بود، اما ضررها بخشی از مسیر یادگیری هستند. آرامش خود را حفظ کنید." else "A challenging day, but losses are stepping stones. Review your entries tomorrow."
        else -> if (lang == "fa") "روز معاملاتی متعادلی را سپری کردید. نتایج را در ژورنال ثبت کنید." else "A balanced trading day. Log your findings in your journal."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (lang == "fa") "بستن" else "Close")
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📅", fontSize = 24.sp)
                Column {
                    Text(
                        text = if (lang == "fa") "خلاصه بازنگری روزانه" else "Daily Review Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = todayStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val pnlColor = if (todayPnl >= 0.0) EmeraldGreen else CrimsonRed
                val pnlSign = if (todayPnl >= 0.0) "+" else ""
                
                Surface(
                    color = pnlColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (lang == "fa") "سود/زیان خالص امروز" else "Today's Net P/L",
                            style = MaterialTheme.typography.labelMedium,
                            color = pnlColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$pnlSign${String.format(Locale.US, "%,.2f", todayPnl)} $currencySymbol",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = pnlColor
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (lang == "fa") "کل معاملات" else "Total Trades",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${todayTrades.size}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (lang == "fa") "برد / باخت" else "Win / Loss",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$todayWins / $todayLosses",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (todayWins > todayLosses) EmeraldGreen else if (todayLosses > todayWins) CrimsonRed else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("💡", fontSize = 16.sp)
                        Column {
                            Text(
                                text = if (lang == "fa") "بینش روزانه مربی" else "Coach Daily Insight",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = todayInsight,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                if (todayMistake != null || todayEmotions != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (todayMistake != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⚠️", fontSize = 14.sp)
                                Text(
                                    text = "${if (lang == "fa") "خطای روز:" else "Mistake of the Day:"} $todayMistake",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CrimsonRed
                                )
                            }
                        }
                        if (todayEmotions != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🎭", fontSize = 14.sp)
                                Text(
                                    text = "${if (lang == "fa") "حالت روحی امروز:" else "Emotions logged:"} $todayEmotions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}
