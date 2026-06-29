package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Trade
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.OpenBlue
import com.example.ui.viewmodel.JournalViewModel
import com.example.ui.viewmodel.SortType
import com.example.ui.util.Loc
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeListScreen(
    viewModel: JournalViewModel,
    onNavigateToTradeDetail: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    val trades by viewModel.trades.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()

    val currencySymbol = when (currency) {
        "IRT" -> "تومان"
        "USDT" -> "USDT"
        else -> "$"
    }

    val layoutDirection = if (language == "en") LayoutDirection.Ltr else LayoutDirection.Rtl

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = Loc.tr("trade_list", language),
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = Loc.tr("back", language),
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Black
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(paddingValues),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 900.dp)
                ) {
                // 1. Sleek Search & Sort Layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = {
                            Text(
                                text = if (language == "fa") "جستجو با نماد..." else if (language == "ar") "بحث بالرمز..." else "Search symbol...",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFF64748B)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFF64748B)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("trade_search_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0C0E12),
                            unfocusedContainerColor = Color(0xFF0C0E12),
                            focusedBorderColor = Color(0xFF1F222B),
                            unfocusedBorderColor = Color(0xFF1F222B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Sort order trigger
                    Box {
                        var sortMenuExpanded by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { sortMenuExpanded = true },
                            modifier = Modifier
                                .background(Color(0xFF0C0E12), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF1F222B), RoundedCornerShape(12.dp))
                                .size(56.dp)
                                .testTag("trade_sort_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = Loc.tr("sort_order", language),
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            modifier = Modifier.background(Color(0xFF0C0E12))
                        ) {
                            DropdownMenuItem(
                                text = { Text(Loc.tr("sort_date_desc_text", language), color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color.White) },
                                onClick = {
                                    viewModel.sortType.value = SortType.DATE_DESC
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Loc.tr("sort_date_asc_text", language), color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color.White) },
                                onClick = {
                                    viewModel.sortType.value = SortType.DATE_ASC
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Loc.tr("sort_pnl_desc_text", language), color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF10B981)) },
                                onClick = {
                                    viewModel.sortType.value = SortType.PNL_DESC
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Loc.tr("sort_pnl_asc_text", language), color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color(0xFFEF4444)) },
                                onClick = {
                                    viewModel.sortType.value = SortType.PNL_ASC
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // 2. Trades list
                if (trades.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = Color(0xFF475569)
                            )
                            Text(
                                text = Loc.tr("no_trades_match", language),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp)
                    ) {
                        items(trades, key = { it.id }) { trade ->
                            ImprovedTradeRow(
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
}
}

@Composable
fun ImprovedTradeRow(
    trade: Trade,
    currencySymbol: String,
    lang: String,
    onClick: () -> Unit
) {
    val isClosed = trade.exitPrice != null
    val pnl = trade.pnl ?: 0.0
    val isProfit = pnl >= 0.0

    val trendColor = when {
        !isClosed -> Color(0xFF3B82F6) // OpenBlue
        isProfit -> Color(0xFF10B981) // EmeraldGreen
        else -> Color(0xFFEF4444) // CrimsonRed
    }

    val sdf = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("trade_list_item_${trade.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0C0E12)
        ),
        border = BorderStroke(1.dp, Color(0xFF1F222B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // A. Vertical Color Indicator Bar
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(trendColor)
            )

            // B. Content details with high-contrast spacing and alignment
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Symbol (Market) and Date
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = trade.market,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    
                    Text(
                        text = sdf.format(Date(trade.dateTime)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                }

                // Right side: Profit/Loss and Side Badge
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // PnL or Open status text
                    val pnlText = if (isClosed) {
                        val prefix = if (pnl > 0.0) "+" else ""
                        "$prefix${String.format(Locale.US, "%,.2f", pnl)} $currencySymbol"
                    } else {
                        if (lang == "fa") "باز" else if (lang == "ar") "مفتوحة" else "OPEN"
                    }
                    Text(
                        text = pnlText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = trendColor,
                        fontSize = 16.sp
                    )

                    // Side (BUY/SELL) badge
                    val sideText = if (trade.side == "BUY") {
                        if (lang == "fa") "خرید" else if (lang == "ar") "شراء" else "BUY"
                    } else {
                        if (lang == "fa") "فروش" else if (lang == "ar") "بيع" else "SELL"
                    }
                    val sideColor = if (trade.side == "BUY") Color(0xFF10B981) else Color(0xFFEF4444)
                    
                    Box(
                        modifier = Modifier
                            .background(sideColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = sideText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = sideColor,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
