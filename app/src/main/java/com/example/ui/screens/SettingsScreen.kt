package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CrimsonRed
import com.example.ui.viewmodel.JournalViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: JournalViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val markets by viewModel.allMarkets.collectAsState()
    val tags by viewModel.allTags.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    // Dialog trigger states
    var showMarketsDialog by remember { mutableStateOf(false) }
    var showTagsDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Text inputs for inline lists additions
    var newMarketInput by remember { mutableStateOf("") }
    var newTagInput by remember { mutableStateOf("") }

    // Launcher to select JSON Backup from storage
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val success = viewModel.restoreBackup(uri)
                if (success) {
                    Toast.makeText(context, "دیتا با موفقیت بازیابی شد.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "خطا در بازیابی اطلاعات بک‌آپ. لطفاً فایل معتبر انتخاب کنید.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Helper to share generated files
    val shareFile = { uri: Uri, mimeType: String, chooserTitle: String ->
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, chooserTitle))
    }

    // Force RTL
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("تنظیمات کاربری", fontWeight = FontWeight.Bold) },
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
                Spacer(modifier = Modifier.height(4.dp))

                // Section 1: Customizations and Currency/Theme
                Text("شخصی‌سازی و واحد مالی", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column {
                        // Theme Selection Row
                        SettingsItemRow(
                            icon = Icons.Default.DarkMode,
                            title = "حالت شب / روز (Theme)",
                            subtitle = when (themeMode) {
                                "LIGHT" -> "حالت روز"
                                "DARK" -> "حالت شب"
                                else -> "هماهنگ با سیستم"
                            },
                            onClick = {
                                val nextTheme = when (themeMode) {
                                    "SYSTEM" -> "LIGHT"
                                    "LIGHT" -> "DARK"
                                    else -> "SYSTEM"
                                }
                                viewModel.setThemeMode(nextTheme)
                                Toast.makeText(context, "تم تغییر یافت.", Toast.LENGTH_SHORT).show()
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        // Currency Selection Row
                        SettingsItemRow(
                            icon = Icons.Default.Payments,
                            title = "واحد ارز ژورنال (Currency)",
                            subtitle = when (currency) {
                                "IRT" -> "تومان (IRT)"
                                "USDT" -> "USDT (Tether)"
                                else -> "دلار ($)"
                            },
                            onClick = {
                                val nextCurrency = when (currency) {
                                    "USD" -> "IRT"
                                    "IRT" -> "USDT"
                                    else -> "USD"
                                }
                                viewModel.setCurrency(nextCurrency)
                                Toast.makeText(context, "واحد مالی به‌روز شد.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                // Section 2: Management of Markets & Tags
                Text("مدیریت اطلاعات پایه", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column {
                        SettingsItemRow(
                            icon = Icons.Default.ShowChart,
                            title = "مدیریت نمادهای معاملاتی",
                            subtitle = "${markets.size} بازار فعال",
                            onClick = { showMarketsDialog = true }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        SettingsItemRow(
                            icon = Icons.Default.Style,
                            title = "مدیریت برچسب‌ها (استراتژی)",
                            subtitle = "${tags.size} تگ ثبت شده",
                            onClick = { showTagsDialog = true }
                        )
                    }
                }

                // Section 3: Data Management & Offline Utilities
                Text("پشتیبان‌گیری و مدیریت داده‌ها", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column {
                        // JSON Export Backup
                        SettingsItemRow(
                            icon = Icons.Default.Backup,
                            title = "پشتیبان‌گیری اطلاعات (Backup JSON)",
                            subtitle = "کل داده‌ها را به صورت فایل JSON استخراج کنید.",
                            onClick = {
                                coroutineScope.launch {
                                    val uri = viewModel.exportBackupFile()
                                    if (uri != null) {
                                        shareFile(uri, "application/json", "ذخیره بک‌آپ معامله")
                                    } else {
                                        Toast.makeText(context, "خطا در استخراج بک‌آپ", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        // JSON Import Restore
                        SettingsItemRow(
                            icon = Icons.Default.Restore,
                            title = "بازیابی اطلاعات (Restore JSON)",
                            subtitle = "بازنشانی دیتای ژورنال از فایل پشتیبان JSON قبلی",
                            onClick = { restoreLauncher.launch("application/json") }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        // CSV Export
                        SettingsItemRow(
                            icon = Icons.Default.TableChart,
                            title = "خروجی اکسل (CSV Export)",
                            subtitle = "دانلود دیتای ژورنال برای تحلیل در اکسل",
                            onClick = {
                                coroutineScope.launch {
                                    val uri = viewModel.exportCsvFile()
                                    if (uri != null) {
                                        shareFile(uri, "text/csv", "خروجی اکسل ژورنال")
                                    } else {
                                        Toast.makeText(context, "خطا در تولید خروجی اکسل", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        // Reset Database
                        SettingsItemRow(
                            icon = Icons.Default.DeleteForever,
                            title = "حذف کل داده‌ها (Reset)",
                            subtitle = "پاکسازی کامل ژورنال معاملاتی (غیرقابل بازگشت)",
                            iconColor = CrimsonRed,
                            onClick = { showResetDialog = true }
                        )
                    }
                }

                // Security Tag offline declaration
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔒 تمام اطلاعات شما کاملاً آفلاین روی حافظه داخلی دستگاه شما نگهداری می‌شود و هیچ بخشی از داده‌ها ارسال نمی‌گردد.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Markets list management modal dialog
        if (showMarketsDialog) {
            Dialog(onDismissRequest = { showMarketsDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text("مدیریت نمادهای معاملاتی", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Input field to add market
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newMarketInput,
                                onValueChange = { newMarketInput = it },
                                placeholder = { Text("مثلا USD/CAD") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Button(
                                onClick = {
                                    if (newMarketInput.isNotEmpty()) {
                                        viewModel.addMarket(newMarketInput)
                                        newMarketInput = ""
                                    }
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("ثبت")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // List of current markets
                        Box(modifier = Modifier.height(220.dp)) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                markets.forEach { m ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(m.name, fontWeight = FontWeight.Medium)
                                        IconButton(onClick = { viewModel.deleteMarket(m) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = CrimsonRed, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { showMarketsDialog = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("بستن")
                        }
                    }
                }
            }
        }

        // Tags list management modal dialog
        if (showTagsDialog) {
            Dialog(onDismissRequest = { showTagsDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text("مدیریت برچسب‌های استراتژی", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Input field to add tag
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newTagInput,
                                onValueChange = { newTagInput = it },
                                placeholder = { Text("مثلا واگرایی MACD") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Button(
                                onClick = {
                                    if (newTagInput.isNotEmpty()) {
                                        viewModel.addTag(newTagInput)
                                        newTagInput = ""
                                    }
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("ثبت")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // List of current tags
                        Box(modifier = Modifier.height(220.dp)) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                tags.forEach { t ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(t.name, fontWeight = FontWeight.Medium)
                                        IconButton(onClick = { viewModel.deleteTag(t) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = CrimsonRed, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { showTagsDialog = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("بستن")
                        }
                    }
                }
            }
        }

        // Full Database Reset Dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("پاکسازی کامل ژورنال") },
                text = { Text("آیا مطمئن هستید که می‌خواهید کل معاملات خود را حذف کنید؟ این دیتابیس کاملاً تخلیه خواهد شد و این عمل غیرقابل بازگشت است.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetAllData()
                            showResetDialog = false
                            Toast.makeText(context, "اطلاعات ژورنال با موفقیت بازنشانی شد.", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                    ) {
                        Text("بله، پاکسازی شود")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("انصراف")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
        }
        Icon(
            Icons.Default.ChevronLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
    }
}
