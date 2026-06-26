package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val context = LocalContext.current

    val trades by viewModel.trades.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedMarket by viewModel.selectedMarket.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    val sortType by viewModel.sortType.collectAsState()

    val currency by viewModel.currency.collectAsState()
    val markets by viewModel.allMarkets.collectAsState()
    val tags by viewModel.allTags.collectAsState()
    val language by viewModel.language.collectAsState()

    var showFiltersPanel by remember { mutableStateOf(false) }

    val currencySymbol = when (currency) {
        "IRT" -> "تومان"
        "USDT" -> "USDT"
        else -> "$"
    }

    val sdf = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    val layoutDirection = if (language == "en") LayoutDirection.Ltr else LayoutDirection.Rtl

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(Loc.tr("journal_book", language), fontWeight = FontWeight.Bold)
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = Loc.tr("back", language))
                        }
                    },
                    actions = {
                        IconButton(onClick = { showFiltersPanel = !showFiltersPanel }) {
                            Icon(
                                imageVector = if (showFiltersPanel) Icons.Default.FilterListOff else Icons.Default.FilterList,
                                contentDescription = Loc.tr("advanced_filter", language),
                                tint = if (selectedMarket != null || selectedStatus != null || selectedTags.isNotEmpty() || startDate != null || endDate != null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onBackground
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
            ) {
                // 1. Text Search Bar & Sorting Quick Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = { Text(Loc.tr("search_placeholder_adv", language), fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = Loc.tr("clear_all", language), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Sort order trigger
                    Box {
                        var sortMenuExpanded by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { sortMenuExpanded = true },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .size(56.dp)
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = Loc.tr("sort_order", language))
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(Loc.tr("sort_date_desc_text", language)) },
                                leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) },
                                onClick = {
                                    viewModel.sortType.value = SortType.DATE_DESC
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Loc.tr("sort_date_asc_text", language)) },
                                leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) },
                                onClick = {
                                    viewModel.sortType.value = SortType.DATE_ASC
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Loc.tr("sort_pnl_desc_text", language)) },
                                leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EmeraldGreen) },
                                onClick = {
                                    viewModel.sortType.value = SortType.PNL_DESC
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Loc.tr("sort_pnl_asc_text", language)) },
                                leadingIcon = { Icon(Icons.Default.TrendingDown, contentDescription = null, tint = CrimsonRed) },
                                onClick = {
                                    viewModel.sortType.value = SortType.PNL_ASC
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // 2. Collapsible Advanced Filters panel
                AnimatedVisibility(
                    visible = showFiltersPanel,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(24.dp)
                            ),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(Loc.tr("advanced_filter_panel", language), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                TextButton(onClick = { viewModel.resetFilters() }) {
                                    Text(Loc.tr("clear_all", language), fontSize = 12.sp)
                                }
                            }

                            // A. Status filter chips
                            Column {
                                Text(Loc.tr("trade_status", language), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("WIN" to Loc.tr("win_status", language), "LOSS" to Loc.tr("loss_status", language), "OPEN" to Loc.tr("open_status", language)).forEach { pair ->
                                        val isSelected = selectedStatus == pair.first
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                viewModel.selectedStatus.value = if (isSelected) null else pair.first
                                            },
                                            label = { Text(pair.second, fontSize = 10.sp) },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            }

                            // B. Market selection filter chips
                            Column {
                                Text(Loc.tr("markets_filter", language), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    markets.forEach { m ->
                                        val isSelected = selectedMarket == m.name
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                viewModel.selectedMarket.value = if (isSelected) null else m.name
                                            },
                                            label = { Text(m.name, fontSize = 10.sp) },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            }

                            // C. Strategy tag filter chips
                            if (tags.isNotEmpty()) {
                                Column {
                                    Text(Loc.tr("tags_filter", language), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        tags.forEach { t ->
                                            val isSelected = t.name in selectedTags
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    val current = selectedTags.toMutableSet()
                                                    if (isSelected) current.remove(t.name) else current.add(t.name)
                                                    viewModel.selectedTags.value = current
                                                },
                                                label = { Text(t.name, fontSize = 10.sp) },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // D. Date Range Selection
                            Column {
                                Text(Loc.tr("date_range", language), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Start Date Chip
                                    OutlinedButton(
                                        onClick = {
                                            val calendar = Calendar.getInstance()
                                            startDate?.let { calendar.timeInMillis = it }
                                            DatePickerDialog(
                                                context,
                                                { _, year, month, day ->
                                                    calendar.set(year, month, day)
                                                    viewModel.startDate.value = calendar.timeInMillis
                                                },
                                                calendar.get(Calendar.YEAR),
                                                calendar.get(Calendar.MONTH),
                                                calendar.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (startDate != null) {
                                                val prefix = if (language == "fa") "از: " else if (language == "ar") "من: " else "From: "
                                                "$prefix${sdf.format(Date(startDate!!))}"
                                            } else {
                                                Loc.tr("start_date", language)
                                            },
                                            fontSize = 11.sp
                                        )
                                    }

                                    // End Date Chip
                                    OutlinedButton(
                                        onClick = {
                                            val calendar = Calendar.getInstance()
                                            endDate?.let { calendar.timeInMillis = it }
                                            DatePickerDialog(
                                                context,
                                                { _, year, month, day ->
                                                    calendar.set(year, month, day)
                                                    viewModel.endDate.value = calendar.timeInMillis
                                                },
                                                calendar.get(Calendar.YEAR),
                                                calendar.get(Calendar.MONTH),
                                                calendar.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (endDate != null) {
                                                val prefix = if (language == "fa") "تا: " else if (language == "ar") "إلى: " else "To: "
                                                "$prefix${sdf.format(Date(endDate!!))}"
                                            } else {
                                                Loc.tr("end_date", language)
                                            },
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Trades list
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
                                Icons.Default.Inventory,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Text(
                                Loc.tr("no_trades_match", language),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
                    ) {
                        items(trades, key = { it.id }) { trade ->
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
}
