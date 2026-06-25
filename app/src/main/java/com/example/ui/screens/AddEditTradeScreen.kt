package com.example.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaRecorder
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.Trade
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.viewmodel.JournalViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTradeScreen(
    viewModel: JournalViewModel,
    tradeId: Int?, // if null, we are in Add mode. Otherwise Edit mode.
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val markets by viewModel.allMarkets.collectAsState()
    val tags by viewModel.allTags.collectAsState()

    // Form fields states
    var side by remember { mutableStateOf("BUY") } // "BUY" or "SELL"
    var selectedMarketText by remember { mutableStateOf("") }
    var volumeStr by remember { mutableStateOf("") }
    var entryPriceStr by remember { mutableStateOf("") }
    var exitPriceStr by remember { mutableStateOf("") }
    var dateTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var feesStr by remember { mutableStateOf("") }
    var reasonText by remember { mutableStateOf("") }
    var richNotesValue by remember { mutableStateOf(TextFieldValue("")) }
    var emotionalStateSelected by remember { mutableStateOf("") }
    var imagePathState by remember { mutableStateOf<String?>(null) }
    var selectedTagsList by remember { mutableStateOf(mutableStateListOf<String>()) }
    var postNotesText by remember { mutableStateOf("") }

    // Dropdowns and UI expansion
    var marketDropdownExpanded by remember { mutableStateOf(false) }
    var showNewMarketDialog by remember { mutableStateOf(false) }
    var newMarketInput by remember { mutableStateOf("") }
    var showNewTagDialog by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }

    // Date/Time Formatter
    val sdf = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    // Load trade data if we are in Edit mode
    LaunchedEffect(tradeId) {
        if (tradeId != null) {
            viewModel.getTradeById(tradeId).collect { trade ->
                if (trade != null) {
                    side = trade.side
                    selectedMarketText = trade.market
                    volumeStr = trade.volume.toString()
                    entryPriceStr = trade.entryPrice.toString()
                    exitPriceStr = trade.exitPrice?.toString() ?: ""
                    dateTime = trade.dateTime
                    feesStr = trade.fees.toString()
                    reasonText = trade.reason
                    val notes = if (trade.richNotes.isNotEmpty()) trade.richNotes else trade.reason
                    richNotesValue = TextFieldValue(text = notes, selection = TextRange(notes.length))
                    emotionalStateSelected = trade.emotionalState
                    imagePathState = trade.imagePath
                    postNotesText = trade.postTradeNotes
                    
                    selectedTagsList.clear()
                    if (trade.tags.isNotEmpty()) {
                        selectedTagsList.addAll(trade.tags.split(",").map { it.trim() })
                    }
                }
            }
        }
    }

    // Auto-calculated PnL preview
    val calculatedPnlPreview = remember(side, volumeStr, entryPriceStr, exitPriceStr, feesStr) {
        val vol = volumeStr.toDoubleOrNull() ?: 0.0
        val entry = entryPriceStr.toDoubleOrNull() ?: 0.0
        val exit = exitPriceStr.toDoubleOrNull()
        val fees = feesStr.toDoubleOrNull() ?: 0.0

        if (exit != null) {
            val rawPnl = if (side == "BUY") (exit - entry) * vol else (entry - exit) * vol
            rawPnl - fees
        } else {
            null
        }
    }

    // Launchers for Image Selection
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    val path = viewModel.saveImageToLocal(bitmap)
                    if (path != null) {
                        imagePathState = path
                        Toast.makeText(context, "تصویر با موفقیت ضمیمه شد", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "خطا در ضمیمه کردن تصویر: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            coroutineScope.launch {
                val path = viewModel.saveImageToLocal(bitmap)
                if (path != null) {
                    imagePathState = path
                    Toast.makeText(context, "تصویر از دوربین ضمیمه شد", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Force RTL
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            if (tradeId == null) "ثبت معامله جدید" else "ویرایش معامله",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
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
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. BUY / SELL Toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { side = "BUY" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (side == "BUY") EmeraldGreen else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (side == "BUY") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("خرید (BUY)", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { side = "SELL" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (side == "SELL") CrimsonRed else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (side == "SELL") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("فروش (SELL)", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 2. Market Autocomplete Selection
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedMarketText,
                        onValueChange = {
                            selectedMarketText = it
                            marketDropdownExpanded = true
                        },
                        label = { Text("نماد بازار (مثلا BTC/USDT)") },
                        leadingIcon = { Icon(Icons.Default.ShowChart, contentDescription = null) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { marketDropdownExpanded = !marketDropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                                IconButton(onClick = { showNewMarketDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "افزودن بازار جدید")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    DropdownMenu(
                        expanded = marketDropdownExpanded,
                        onDismissRequest = { marketDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        val filteredMarkets = markets.filter {
                            it.name.contains(selectedMarketText, ignoreCase = true)
                        }
                        if (filteredMarkets.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("بازاری یافت نشد. برای تعریف سریع کلیک کنید...") },
                                onClick = {
                                    newMarketInput = selectedMarketText
                                    showNewMarketDialog = true
                                    marketDropdownExpanded = false
                                }
                            )
                        } else {
                            filteredMarkets.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.name) },
                                    onClick = {
                                        selectedMarketText = m.name
                                        marketDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 3. Volume and Entry Price Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = volumeStr,
                        onValueChange = { volumeStr = it },
                        label = { Text("حجم معامله") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = entryPriceStr,
                        onValueChange = { entryPriceStr = it },
                        label = { Text("قیمت ورود") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 4. Exit Price and Fees Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = exitPriceStr,
                        onValueChange = { exitPriceStr = it },
                        label = { Text("قیمت خروج (اختیاری)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = feesStr,
                        onValueChange = { feesStr = it },
                        label = { Text("کارمزد") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // PnL Auto-calculated preview banner
                AnimatedVisibility(
                    visible = calculatedPnlPreview != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    calculatedPnlPreview?.let { pnl ->
                        val isProfit = pnl >= 0.0
                        val color = if (isProfit) EmeraldGreen else CrimsonRed
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = color.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(20.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "محاسبه خودکار سود/زیان معامله:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${if (isProfit) "+" else ""}${String.format(Locale.US, "%,.2f", pnl)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = color
                                )
                            }
                        }
                    }
                }

                // 5. Date and Time Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = sdf.format(Date(dateTime)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("تاریخ و ساعت") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance().apply { timeInMillis = dateTime }
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    calendar.set(Calendar.YEAR, year)
                                    calendar.set(Calendar.MONTH, month)
                                    calendar.set(Calendar.DAY_OF_MONTH, day)
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            calendar.set(Calendar.HOUR_OF_DAY, hour)
                                            calendar.set(Calendar.MINUTE, minute)
                                            dateTime = calendar.timeInMillis
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        true
                                    ).show()
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("تغییر")
                    }
                }

                // 6. Detailed Notes & Emotional State & Rich Text Toolbar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Emotional State Label
                    Text(
                        "حالت روحی شما حین ورود به معامله",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Emotional State Selection Chips row
                    val emotions = listOf(
                        Triple("CALM", "🟢 آرام", EmeraldGreen),
                        Triple("CONFIDENT", "🔵 مطمئن", MaterialTheme.colorScheme.primary),
                        Triple("EXCITED", "🟡 هیجان‌زده", Color(0xFFFFB300)),
                        Triple("ANXIOUS", "🟠 مضطرب", Color(0xFFFF6D00)),
                        Triple("GREEDY", "🔴 حریص", CrimsonRed),
                        Triple("FEARFUL", "🟣 ترسیده", Color(0xFF9C27B0))
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        emotions.forEach { (id, label, color) ->
                            val isSelected = emotionalStateSelected == id
                            FilterChip(
                                selected = isSelected,
                                onClick = { emotionalStateSelected = if (isSelected) "" else id },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color.copy(alpha = 0.15f),
                                    selectedLabelColor = color
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    selected = isSelected,
                                    enabled = true,
                                    selectedBorderColor = color,
                                    selectedBorderWidth = 2.dp
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Text(
                        "دلایل ورود و یادداشت تفصیلی (با امکان قالب‌بندی متنی)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Formatting Toolbar row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val formats = listOf(
                            Triple("B", "**", "**"),   // Bold
                            Triple("I", "*", "*"),     // Italic
                            Triple("لیست", "- ", ""),  // List
                            Triple("نقل", "> ", ""),   // Quote
                            Triple("کد", "`", "`")      // Code
                        )
                        formats.forEach { (label, prefix, suffix) ->
                            FilledTonalButton(
                                onClick = {
                                    val text = richNotesValue.text
                                    val selection = richNotesValue.selection
                                    val selectedText = text.substring(selection.start, selection.end)
                                    val replacement = "$prefix$selectedText$suffix"
                                    val newText = text.replaceRange(selection.start, selection.end, replacement)
                                    val newSelectionStart = selection.start + prefix.length
                                    val newSelectionEnd = newSelectionStart + selectedText.length
                                    richNotesValue = TextFieldValue(
                                        text = newText,
                                        selection = TextRange(newSelectionStart, newSelectionEnd)
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Rich notes TextField
                    OutlinedTextField(
                        value = richNotesValue,
                        onValueChange = { richNotesValue = it },
                        placeholder = { Text("دلایل ورود، استراتژی تحلیل، احساسات و جزئیات ترید را با قالب‌بندی غنی ثبت کنید...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Live Markdown Styled Preview Card
                    if (richNotesValue.text.isNotEmpty()) {
                        var isPreviewExpanded by remember { mutableStateOf(false) }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                width = 1.dp,
                                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isPreviewExpanded = !isPreviewExpanded },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "پیش‌نمایش زنده نوشته غنی شده",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isPreviewExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (isPreviewExpanded) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = parseMarkdownToAnnotatedString(richNotesValue.text, MaterialTheme.colorScheme.primary),
                                        fontSize = 12.sp,
                                        lineHeight = 22.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                // 7. Dynamic Tags Selection & Add Tag
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "برچسب‌ها (استراتژی)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { showNewTagDialog = true }) {
                            Icon(Icons.Default.AddCircle, contentDescription = "برچسب جدید")
                        }
                    }

                    // Wrap of available tags
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tags.forEach { tag ->
                            val isSelected = tag.name in selectedTagsList
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        selectedTagsList.remove(tag.name)
                                    } else {
                                        selectedTagsList.add(tag.name)
                                    }
                                },
                                label = { Text(tag.name, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // 8. Attached Image Container & Actions
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "تصویر نمودار (چارت)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    if (imagePathState != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = File(imagePathState!!),
                                contentDescription = "نمودار معامله",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Overlay clear button
                            IconButton(
                                onClick = { imagePathState = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "حذف عکس", tint = Color.White)
                            }
                        }
                    } else {
                        // Action buttons to attach image
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("انتخاب از گالری", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { cameraLauncher.launch() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("عکس با دوربین", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // 9. Post-trade Notes (only visible if closed/has exit price, or in edit mode)
                if (exitPriceStr.isNotEmpty() || tradeId != null) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "بررسی بعد از معامله (Post-Trade Review)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = postNotesText,
                            onValueChange = { postNotesText = it },
                            placeholder = { Text("درس‌های گرفته شده، اشتباهات معاملاتی، مدیریت احساسات...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // 10. SAVE / CANCEL buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("انصراف")
                    }

                    Button(
                        onClick = {
                            // Validation
                            val vol = volumeStr.toDoubleOrNull()
                            val entry = entryPriceStr.toDoubleOrNull()
                            val fees = feesStr.toDoubleOrNull() ?: 0.0
                            val exit = if (exitPriceStr.isEmpty()) null else exitPriceStr.toDoubleOrNull()

                            if (selectedMarketText.isEmpty()) {
                                Toast.makeText(context, "لطفاً نماد بازار را مشخص کنید.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (vol == null || vol <= 0.0) {
                                Toast.makeText(context, "لطفاً حجم معامله معتبری وارد کنید.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (entry == null || entry <= 0.0) {
                                Toast.makeText(context, "لطفاً قیمت ورود معتبری وارد کنید.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val tagsJoined = selectedTagsList.joinToString(",")

                            val trade = Trade(
                                id = tradeId ?: 0,
                                side = side,
                                market = selectedMarketText,
                                volume = vol,
                                entryPrice = entry,
                                exitPrice = exit,
                                dateTime = dateTime,
                                fees = fees,
                                reason = richNotesValue.text,
                                imagePath = imagePathState,
                                tags = tagsJoined,
                                postTradeNotes = postNotesText,
                                richNotes = richNotesValue.text,
                                emotionalState = emotionalStateSelected
                            )

                            if (tradeId == null) {
                                viewModel.insertTrade(trade)
                            } else {
                                viewModel.updateTrade(trade)
                            }
                            onNavigateBack()
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ذخیره موقعیت", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // New Market Dialog
        if (showNewMarketDialog) {
            AlertDialog(
                onDismissRequest = { showNewMarketDialog = false },
                title = { Text("افزودن بازار جدید") },
                text = {
                    OutlinedTextField(
                        value = newMarketInput,
                        onValueChange = { newMarketInput = it },
                        placeholder = { Text("مثلا ADA/USDT") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newMarketInput.isNotEmpty()) {
                            viewModel.addMarket(newMarketInput)
                            selectedMarketText = newMarketInput
                            newMarketInput = ""
                            showNewMarketDialog = false
                        }
                    }) {
                        Text("افزودن")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewMarketDialog = false }) {
                        Text("انصراف")
                    }
                }
            )
        }

        // New Tag Dialog
        if (showNewTagDialog) {
            AlertDialog(
                onDismissRequest = { showNewTagDialog = false },
                title = { Text("افزودن برچسب (استراتژی) جدید") },
                text = {
                    OutlinedTextField(
                        value = newTagInput,
                        onValueChange = { newTagInput = it },
                        placeholder = { Text("مثلا کانال صعودی") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newTagInput.isNotEmpty()) {
                            viewModel.addTag(newTagInput)
                            if (newTagInput !in selectedTagsList) {
                                selectedTagsList.add(newTagInput)
                            }
                            newTagInput = ""
                            showNewTagDialog = false
                        }
                    }) {
                        Text("افزودن")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewTagDialog = false }) {
                        Text("انصراف")
                    }
                }
            )
        }
    }
}

// Custom flow row layout for tag placement
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        var rowWidth = 0
        var rowHeight = 0
        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()

        placeables.forEach { placeable ->
            if (rowWidth + placeable.width > constraints.maxWidth) {
                rows.add(currentRow)
                currentRow = mutableListOf(placeable)
                rowWidth = placeable.width
                rowHeight += placeable.height
            } else {
                currentRow.add(placeable)
                rowWidth += placeable.width
            }
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        val totalHeight = if (rows.isEmpty()) {
            0
        } else {
            val rowsHeight = rows.sumOf { r -> r.maxOfOrNull { it.height } ?: 0 }
            val spacingHeight = (rows.size - 1) * (try { verticalArrangement.spacing.roundToPx() } catch (e: Exception) { 0 })
            kotlin.math.max(0, rowsHeight + spacingHeight)
        }
        layout(constraints.maxWidth, totalHeight) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                val rowMaxHeight = row.maxOfOrNull { it.height } ?: 0
                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + (try { horizontalArrangement.spacing.roundToPx() } catch (e: Exception) { 0 })
                }
                y += rowMaxHeight + (try { verticalArrangement.spacing.roundToPx() } catch (e: Exception) { 0 })
            }
        }
    }
}

@Composable
fun Image(
    bitmap: Bitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val imageBitmap = remember(bitmap) {
        bitmap.asImageBitmap()
    }
    androidx.compose.foundation.Image(
        bitmap = imageBitmap,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}

fun parseMarkdownToAnnotatedString(text: String, primaryColor: Color): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { lineIndex, line ->
            var currentLine = line
            var isBullet = false
            var isQuote = false
            
            if (currentLine.startsWith("- ")) {
                isBullet = true
                currentLine = currentLine.substring(2)
            } else if (currentLine.startsWith("> ")) {
                isQuote = true
                currentLine = currentLine.substring(2)
            }
            
            if (isBullet) {
                append("• ")
            }
            
            val startLength = length
            
            // Basic parser for Bold (**) and Italic (*) and Code (`)
            var i = 0
            while (i < currentLine.length) {
                when {
                    currentLine.startsWith("**", i) -> {
                        val endIdx = currentLine.indexOf("**", i + 2)
                        if (endIdx != -1) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(currentLine.substring(i + 2, endIdx))
                            }
                            i = endIdx + 2
                        } else {
                            append("**")
                            i += 2
                        }
                    }
                    currentLine.startsWith("*", i) -> {
                        val endIdx = currentLine.indexOf("*", i + 1)
                        if (endIdx != -1) {
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(currentLine.substring(i + 1, endIdx))
                            }
                            i = endIdx + 1
                        } else {
                            append("*")
                            i += 1
                        }
                    }
                    currentLine.startsWith("`", i) -> {
                        val endIdx = currentLine.indexOf("`", i + 1)
                        if (endIdx != -1) {
                            withStyle(SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = primaryColor.copy(alpha = 0.1f),
                                color = primaryColor
                            )) {
                                append(currentLine.substring(i + 1, endIdx))
                            }
                            i = endIdx + 1
                        } else {
                            append("`")
                            i += 1
                        }
                    }
                    else -> {
                        append(currentLine[i])
                        i++
                    }
                }
            }
            
            if (isQuote) {
                addStyle(
                    style = SpanStyle(
                        fontStyle = FontStyle.Italic,
                        color = primaryColor.copy(alpha = 0.8f)
                    ),
                    start = startLength,
                    end = length
                )
            }
            
            if (lineIndex < lines.size - 1) {
                append("\n")
            }
        }
    }
}
