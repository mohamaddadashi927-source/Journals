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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.ui.util.Loc
import java.util.Locale
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
    val language by viewModel.language.collectAsState()
    val initialBalance by viewModel.initialBalance.collectAsState()
    val accountName by viewModel.accountName.collectAsState()

    // Dialog trigger states
    var showMarketsDialog by remember { mutableStateOf(false) }
    var showTagsDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showBalanceEditDialog by remember { mutableStateOf(false) }

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

    // Dynamic Layout Direction based on selected language
    val layoutDirection = if (language == "en") LayoutDirection.Ltr else LayoutDirection.Rtl

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(Loc.tr("settings_title", language), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = Loc.tr("back", language))
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
                Text(
                    text = if (language == "fa") "شخصی‌سازی و واحد مالی" else if (language == "ar") "التخصيص والعملة" else "Personalization & Currency",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

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
                            title = Loc.tr("theme", language),
                            subtitle = when (themeMode) {
                                "LIGHT" -> Loc.tr("theme_light", language)
                                "DARK" -> Loc.tr("theme_dark", language)
                                else -> Loc.tr("theme_system", language)
                            },
                            onClick = {
                                val nextTheme = when (themeMode) {
                                    "SYSTEM" -> "LIGHT"
                                    "LIGHT" -> "DARK"
                                    else -> "SYSTEM"
                                }
                                viewModel.setThemeMode(nextTheme)
                                val toastMsg = when (nextTheme) {
                                    "LIGHT" -> if (language == "fa") "تم روز فعال شد." else if (language == "ar") "تم تفعيل المظهر الفاتح." else "Light theme activated."
                                    "DARK" -> if (language == "fa") "تم شب فعال شد." else if (language == "ar") "تم تفعيل المظهر الداكن." else "Dark theme activated."
                                    else -> if (language == "fa") "تم هماهنگ با سیستم شد." else if (language == "ar") "تم التوافق مع النظام." else "System theme activated."
                                }
                                Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        // Currency Selection Row
                        SettingsItemRow(
                            icon = Icons.Default.Payments,
                            title = Loc.tr("currency", language),
                            subtitle = when (currency) {
                                "IRT" -> if (language == "fa") "تومان (IRT)" else if (language == "ar") "تومان (IRT)" else "Toman (IRT)"
                                "USDT" -> if (language == "fa") "تتر (USDT)" else if (language == "ar") "تتر (USDT)" else "Tether (USDT)"
                                else -> if (language == "fa") "دلار ($)" else if (language == "ar") "دولار ($)" else "Dollar ($)"
                            },
                            onClick = {
                                val nextCurrency = when (currency) {
                                    "USD" -> "IRT"
                                    "IRT" -> "USDT"
                                    else -> "USD"
                                }
                                viewModel.setCurrency(nextCurrency)
                                val toastMsg = if (language == "fa") "واحد مالی به‌روز شد." else if (language == "ar") "تم تحديث العملة." else "Currency updated."
                                Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        // Language Selection Row
                        SettingsItemRow(
                            icon = Icons.Default.Language,
                            title = Loc.tr("app_lang", language),
                            subtitle = when (language) {
                                "en" -> "English"
                                "ar" -> "العربية"
                                else -> "فارسی"
                            },
                            onClick = {
                                val nextLanguage = when (language) {
                                    "fa" -> "en"
                                    "en" -> "ar"
                                    else -> "fa"
                                }
                                viewModel.setLanguage(nextLanguage)
                                val toastMsg = when (nextLanguage) {
                                    "en" -> "Language changed to English."
                                    "ar" -> "تم تغيير اللغة إلى العربية."
                                    else -> "زبان به فارسی تغییر یافت."
                                }
                                Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        // Account Balance & Details Setup Row
                        SettingsItemRow(
                            icon = Icons.Default.AccountBalanceWallet,
                            title = Loc.tr("initial_balance", language),
                            subtitle = "$accountName - ${String.format(Locale.US, "%,.1f", initialBalance)} ${
                                when (currency) {
                                    "IRT" -> "تومان"
                                    "USDT" -> "USDT"
                                    else -> "$"
                                }
                            }",
                            onClick = { showBalanceEditDialog = true }
                        )
                    }
                }

                // Section 2: Management of Markets & Tags
                Text(
                    text = if (language == "fa") "مدیریت اطلاعات پایه" else if (language == "ar") "إدارة البيانات الأساسية" else "Base Data Management",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

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
                            title = if (language == "fa") "مدیریت نمادهای معاملاتی" else if (language == "ar") "إدارة الرموز المتداولة" else "Asset Symbols Management",
                            subtitle = if (language == "fa") "${markets.size} بازار فعال" else if (language == "ar") "الأسواق النشطة: ${markets.size}" else "${markets.size} active assets",
                            onClick = { showMarketsDialog = true }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        SettingsItemRow(
                            icon = Icons.Default.Style,
                            title = Loc.tr("tags", language),
                            subtitle = if (language == "fa") "${tags.size} تگ ثبت شده" else if (language == "ar") "الوسوم المسجلة: ${tags.size}" else "${tags.size} tags registered",
                            onClick = { showTagsDialog = true }
                        )
                    }
                }

                // Section 3: Data Management & Offline Utilities
                Text(
                    text = Loc.tr("import_export", language),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

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
                            title = Loc.tr("export_backup", language),
                            subtitle = if (language == "fa") "کل داده‌ها را به صورت فایل JSON استخراج کنید." else if (language == "ar") "تصدير كافة البيانات كملف JSON." else "Export all journal data as a JSON file.",
                            onClick = {
                                coroutineScope.launch {
                                    val uri = viewModel.exportBackupFile()
                                    if (uri != null) {
                                        shareFile(uri, "application/json", if (language == "fa") "ذخیره بک‌آپ معامله" else if (language == "ar") "حفظ النسخة الاحتياطية" else "Save Journal Backup")
                                    } else {
                                        val err = if (language == "fa") "خطا در استخراج بک‌آپ" else if (language == "ar") "فشل تصدير النسخة الاحتياطية" else "Error exporting backup"
                                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        // JSON Import Restore
                        SettingsItemRow(
                            icon = Icons.Default.Restore,
                            title = Loc.tr("restore_backup", language),
                            subtitle = if (language == "fa") "بازنشانی دیتای ژورنال از فایل پشتیبان JSON قبلی" else if (language == "ar") "استعادة بيانات السجل من ملف JSON احتياطي" else "Restore journal data from a previously exported JSON file",
                            onClick = { restoreLauncher.launch("application/json") }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        // CSV Export
                        SettingsItemRow(
                            icon = Icons.Default.TableChart,
                            title = Loc.tr("export_csv", language),
                            subtitle = if (language == "fa") "دانلود دیتای ژورنال برای تحلیل در اکسل" else if (language == "ar") "تحميل بيانات السجل للتحليل في إكسل" else "Download journal database to analyze in Excel",
                            onClick = {
                                coroutineScope.launch {
                                    val uri = viewModel.exportCsvFile()
                                    if (uri != null) {
                                        shareFile(uri, "text/csv", if (language == "fa") "خروجی اکسل ژورنال" else if (language == "ar") "تصدير السجل إلى إكسل" else "Excel Journal Export")
                                    } else {
                                        val err = if (language == "fa") "خطا در تولید خروجی اکسل" else if (language == "ar") "فشل إنشاء ملف إكسل" else "Error generating Excel CSV"
                                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        // Reset Database
                        SettingsItemRow(
                            icon = Icons.Default.DeleteForever,
                            title = Loc.tr("reset_data", language),
                            subtitle = if (language == "fa") "پاکسازی کامل ژورنال معاملاتی (غیرقابل بازگشت)" else if (language == "ar") "تنظيف سجل التداول بالكامل (لا يمكن التراجع عنه)" else "Completely clear trading journal database (Irreversible)",
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
                        text = if (language == "fa") "🔒 تمام اطلاعات شما کاملاً آفلاین روی حافظه داخلی دستگاه شما نگهداری می‌شود و هیچ بخشی از داده‌ها ارسال نمی‌گردد." else if (language == "ar") "🔒 يتم تخزين جميع بياناتك محلياً بشكل غير متصل على جهازك بالكامل ولا يتم إرسال أي جزء من بياناتك." else "🔒 All your information is stored completely offline on your device storage; no data ever leaves your device.",
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
                        Text(
                            text = if (language == "fa") "مدیریت نمادهای معاملاتی" else if (language == "ar") "إدارة الرموز المتداولة" else "Asset Symbols Management",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
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
                                placeholder = { Text(if (language == "fa") "مثلا USD/CAD" else if (language == "ar") "مثلاً USD/CAD" else "e.g. USD/CAD") },
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
                                Text(if (language == "fa") "ثبت" else if (language == "ar") "إضافة" else "Add")
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
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = if (language == "fa") "حذف" else if (language == "ar") "حذف" else "Delete",
                                                tint = CrimsonRed,
                                                modifier = Modifier.size(20.dp)
                                            )
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
                            Text(if (language == "fa") "بستن" else if (language == "ar") "إغلاق" else "Close")
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
                        Text(
                            text = if (language == "fa") "مدیریت برچسب‌های استراتژی" else if (language == "ar") "إدارة وسوم الاستراتيجية" else "Strategy Tags Management",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
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
                                placeholder = { Text(if (language == "fa") "مثلا واگرایی MACD" else if (language == "ar") "مثلاً تباعد MACD" else "e.g. MACD Divergence") },
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
                                Text(if (language == "fa") "ثبت" else if (language == "ar") "إضافة" else "Add")
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
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = if (language == "fa") "حذف" else if (language == "ar") "حذف" else "Delete",
                                                tint = CrimsonRed,
                                                modifier = Modifier.size(20.dp)
                                            )
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
                            Text(if (language == "fa") "بستن" else if (language == "ar") "إغلاق" else "Close")
                        }
                    }
                }
            }
        }

        // Full Database Reset Dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text(Loc.tr("reset_confirm_title", language)) },
                text = { Text(Loc.tr("reset_confirm_desc", language)) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetAllData()
                            showResetDialog = false
                            val toastMsg = if (language == "fa") "اطلاعات ژورنال با موفقیت بازنشانی شد." else if (language == "ar") "تمت إعادة تعيين بيانات السجل بنجاح." else "Trading journal successfully reset."
                            Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                    ) {
                        Text(if (language == "fa") "بله، پاکسازی شود" else if (language == "ar") "نعم، احذف الكل" else "Yes, Clear All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text(Loc.tr("cancel", language))
                    }
                }
            )
        }

        // Account Details & Balance Editor Dialog
        if (showBalanceEditDialog) {
            var tempName by remember { mutableStateOf(accountName) }
            var tempBalanceStr by remember { mutableStateOf(String.format(Locale.US, "%.1f", initialBalance)) }

            AlertDialog(
                onDismissRequest = { showBalanceEditDialog = false },
                confirmButton = {
                    Button(
                        onClick = {
                            val bal = tempBalanceStr.toDoubleOrNull() ?: initialBalance
                            viewModel.initializeAccount(tempName, bal)
                            showBalanceEditDialog = false
                        },
                        enabled = tempBalanceStr.isNotEmpty() && (tempBalanceStr.toDoubleOrNull() ?: 0.0) >= 0.0
                    ) {
                        Text(Loc.tr("save", language))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBalanceEditDialog = false }) {
                        Text(Loc.tr("cancel", language))
                    }
                },
                title = {
                    Text(Loc.tr("initial_balance_title", language), fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = tempName,
                            onValueChange = { tempName = it },
                            label = { Text(Loc.tr("account_name", language)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = tempBalanceStr,
                            onValueChange = { tempBalanceStr = it },
                            label = { Text(Loc.tr("initial_balance", language)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
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
