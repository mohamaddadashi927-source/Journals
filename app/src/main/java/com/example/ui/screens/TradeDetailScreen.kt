package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.Trade
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.OpenBlue
import com.example.ui.theme.OrangeWarn
import com.example.ui.viewmodel.JournalViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import com.example.ui.util.Loc

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeDetailScreen(
    viewModel: JournalViewModel,
    tradeId: Int,
    onNavigateToEditTrade: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val tradeState = viewModel.getTradeById(tradeId).collectAsState(initial = null)
    val trade = tradeState.value

    val currency by viewModel.currency.collectAsState()
    val currencySymbol = when (currency) {
        "IRT" -> "تومان"
        "USDT" -> "USDT"
        else -> "$"
    }
    val language by viewModel.language.collectAsState()

    var showPostTradeDialog by remember { mutableStateOf(false) }
    var postNotesInput by remember { mutableStateOf("") }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showZoomImageDialog by remember { mutableStateOf(false) }

    val sdf = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    // Initialize inputs when trade is loaded
    LaunchedEffect(trade) {
        trade?.let {
            postNotesInput = it.postTradeNotes
        }
    }

    // Dynamic Layout Direction based on selected language
    val layoutDirection = if (language == "en") LayoutDirection.Ltr else LayoutDirection.Rtl

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(Loc.tr("trade_details", language), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = Loc.tr("back", language))
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            trade?.let { t ->
                                coroutineScope.launch {
                                    val pdfUri = com.example.ui.pdf.PdfExportHelper.generateTradeDetailPdf(context, t, currencySymbol)
                                    if (pdfUri != null) {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(android.content.Intent.EXTRA_STREAM, pdfUri)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(intent, "اشتراک‌گذاری گزارش PDF"))
                                    } else {
                                        Toast.makeText(context, "خطا در ایجاد گزارش PDF", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "خروجی PDF", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف معامله", tint = CrimsonRed)
                        }
                        IconButton(onClick = { trade?.let { onNavigateToEditTrade(it.id) } }) {
                            Icon(Icons.Default.Edit, contentDescription = "ویرایش معامله")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { paddingValues ->
            if (trade == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val isProfit = (trade.pnl ?: 0.0) > 0.0
                val isClosed = trade.exitPrice != null
                val trendColor = when {
                    !isClosed -> OpenBlue
                    isProfit -> EmeraldGreen
                    else -> CrimsonRed
                }

                val statusText = when {
                    !isClosed -> "باز"
                    isProfit -> "برد (Profit)"
                    else -> "باخت (Loss)"
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Card with Quick Status & PnL
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
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
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = trade.market,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .background(trendColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = statusText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = trendColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "سود / زیان کل:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isClosed) {
                                    "${if (isProfit) "+" else ""}${String.format(Locale.US, "%,.2f", trade.pnl)} $currencySymbol"
                                } else {
                                    "باز"
                                },
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = trendColor
                            )
                        }
                    }

                    // Key metrics grid
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(24.dp)
                            ),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("جزئیات معامله", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                            DetailRow(label = "نوع معامله:", value = if (trade.side == "BUY") "خرید (BUY)" else "فروش (SELL)", valueColor = if (trade.side == "BUY") EmeraldGreen else CrimsonRed)
                            DetailRow(label = "حجم معامله:", value = trade.volume.toString())
                            DetailRow(label = "قیمت ورود:", value = String.format(Locale.US, "%,.4f", trade.entryPrice))
                            DetailRow(label = "قیمت خروج:", value = if (isClosed) String.format(Locale.US, "%,.4f", trade.exitPrice) else "تعیین نشده (موقعیت باز)")

                            DetailRow(label = "زمان ثبت معامله:", value = sdf.format(Date(trade.dateTime)))
                        }
                    }

                    // Tags Used
                    if (trade.tags.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("برچسب‌های استراتژی:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                trade.tags.split(",").forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(tag.trim(), fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Reason section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(24.dp)
                            ),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "دلایل ورود و جزئیات ترید",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                
                                // Beautiful Emotional State pill
                                if (trade.emotionalState.isNotEmpty()) {
                                    val (emoji, label, color) = when (trade.emotionalState) {
                                        "CALM" -> Triple("🟢", "آرام", EmeraldGreen)
                                        "CONFIDENT" -> Triple("🔵", "مطمئن", MaterialTheme.colorScheme.primary)
                                        "EXCITED" -> Triple("🟡", "هیجان‌زده", Color(0xFFFFB300))
                                        "ANXIOUS" -> Triple("🟠", "مضطرب", Color(0xFFFF6D00))
                                        "GREEDY" -> Triple("🔴", "حریص", CrimsonRed)
                                        "FEARFUL" -> Triple("🟣", "ترسیده", Color(0xFF9C27B0))
                                        else -> Triple("🧠", trade.emotionalState, MaterialTheme.colorScheme.secondary)
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "حالت روحی: $emoji $label",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = color
                                        )
                                    }
                                }
                            }
                            
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            
                            val displayNotes = if (trade.richNotes.isNotEmpty()) trade.richNotes else trade.reason
                            if (displayNotes.isNotEmpty()) {
                                Text(
                                    text = parseMarkdownToAnnotatedString(displayNotes, MaterialTheme.colorScheme.primary),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 22.sp
                                )
                            } else {
                                Text(
                                    text = "یادداشتی برای ورود ثبت نشده است.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    // Attached Chart Screenshot & AI Analyzer Trigger
                    if (trade.imagePath != null) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("تصویر چارت معامله", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showZoomImageDialog = true }
                            ) {
                                AsyncImage(
                                    model = File(trade.imagePath),
                                    contentDescription = "چارت معامله",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ZoomIn, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("مشاهده بزرگ‌تر", color = Color.White, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Post-trade Notes (Post-trade review)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(24.dp)
                            ),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("بررسی بعد از معامله (Post-Trade Review)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = { showPostTradeDialog = true }) {
                                    Icon(Icons.Default.EditCalendar, contentDescription = "ویرایش یادداشت تکمیلی")
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            Text(
                                text = trade.postTradeNotes.ifEmpty { "هیچ یادداشت و درسی بعد از بسته شدن معامله اضافه نشده است. برای افزودن روی دکمه ادیت بالا بزنید." },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    // PDF Export & Print Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(24.dp)
                            ),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Column {
                                        Text(
                                            "خروجی PDF و چاپ گزارش",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "دریافت فایل فرمت شده آماده اشتراک‌گذاری یا چاپ نمودار",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val pdfUri = com.example.ui.pdf.PdfExportHelper.generateTradeDetailPdf(context, trade, currencySymbol)
                                        if (pdfUri != null) {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(android.content.Intent.EXTRA_STREAM, pdfUri)
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(android.content.Intent.createChooser(intent, "اشتراک‌گذاری و چاپ گزارش معامله"))
                                        } else {
                                            Toast.makeText(context, "خطا در تولید فایل PDF", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تولید PDF و اشتراک‌گذاری / چاپ گزارش", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // 1. Post Trade Note Editing Dialog
        if (showPostTradeDialog) {
            AlertDialog(
                onDismissRequest = { showPostTradeDialog = false },
                title = { Text("ویرایش بررسی معامله") },
                text = {
                    OutlinedTextField(
                        value = postNotesInput,
                        onValueChange = { postNotesInput = it },
                        placeholder = { Text("مثلا: در این معامله طمع کردم و زود خارج شدم. ریسک به ریوارد مناسب رعایت شد...") },
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        trade?.let {
                            val updated = it.copy(postTradeNotes = postNotesInput)
                            viewModel.updateTrade(updated)
                            showPostTradeDialog = false
                            Toast.makeText(context, "بررسی ثبت شد", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("ثبت یادداشت")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPostTradeDialog = false }) {
                        Text("انصراف")
                    }
                }
            )
        }

        // 2. Delete Confirmation Dialog
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("حذف معامله") },
                text = { Text("آیا از حذف کامل این معامله از دفترچه ژورنال خود مطمئن هستید؟ این عمل غیرقابل بازگشت است.") },
                confirmButton = {
                    Button(
                        onClick = {
                            trade?.let {
                                viewModel.deleteTrade(it)
                                showDeleteConfirmDialog = false
                                Toast.makeText(context, "معامله با موفقیت حذف شد", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                    ) {
                        Text("بله، حذف شود")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("انصراف")
                    }
                }
            )
        }

        // 3. High Fidelity Zoom Dialog for attached chart
        if (showZoomImageDialog && trade?.imagePath != null) {
            var scale by remember { mutableStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
                scale *= zoomChange
                // Limit scale range
                scale = scale.coerceIn(1f, 5f)
                offset += offsetChange
            }

            Dialog(
                onDismissRequest = { showZoomImageDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    // Full screen display
                    AsyncImage(
                        model = File(trade.imagePath),
                        contentDescription = "چارت زوم شده",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                            .transformable(state = transformState),
                        contentScale = ContentScale.Fit
                    )

                    // Close action button overlay
                    IconButton(
                        onClick = { showZoomImageDialog = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(24.dp)
                            .background(Color.White.copy(alpha = 0.25f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                    }

                    // Bottom zoom indicator instruction
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(24.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("با دو انگشت بکشید تا بزرگ‌نمایی شود.", color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            textAlign = TextAlign.Left
        )
    }
}

private fun parseMarkdownToAnnotatedString(text: String, primaryColor: Color): AnnotatedString {
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
