package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.viewmodel.JournalViewModel
import com.example.ui.viewmodel.TradingAccount
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: JournalViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val language by viewModel.language.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val accountsList by viewModel.accountsList.collectAsStateWithLifecycle()
    val activeAccountName by viewModel.accountName.collectAsStateWithLifecycle()
    val activeBalance by viewModel.initialBalance.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    
    val currencySymbol = when (currency) {
        "IRT" -> if (language == "fa") "تومان" else "IRT"
        "USDT" -> "USDT"
        else -> "$"
    }

    // Dialog states
    var showExportSelectionDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showResetWarningDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<TradingAccount?>(null) }

    // Launcher to select JSON Backup from storage safely
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingRestoreUri = uri
        }
    }

    // Helper to share generated backup files securely
    val shareFile = { uri: Uri, mimeType: String, chooserTitle: String ->
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, chooserTitle))
    }

    // Layout direction helper
    val layoutDirection = if (language == "en") LayoutDirection.Ltr else LayoutDirection.Rtl

    // Translations object
    val labels = remember(language) {
        object {
            val title = if (language == "fa") "تنظیمات پیشرفته" else if (language == "ar") "الإعدادات المتقدمة" else "Advanced Settings"
            val accountsHeader = if (language == "fa") "حساب‌های معاملاتی" else if (language == "ar") "الحسابات التجارية" else "Trading Accounts"
            val addAccount = if (language == "fa") "افزودن حساب جدید" else if (language == "ar") "إضافة حساب جديد" else "Add New Account"
            val activeLabel = if (language == "fa") "فعال" else if (language == "ar") "نشط" else "Active"
            val switchLabel = if (language == "fa") "فعال‌سازی" else if (language == "ar") "تفعيل" else "Activate"
            val deleteLabel = if (language == "fa") "حذف" else if (language == "ar") "حذف" else "Delete"
            val editLabel = if (language == "fa") "ویرایش" else if (language == "ar") "تعديل" else "Edit"
            
            val languageHeader = if (language == "fa") "زبان برنامه" else if (language == "ar") "لغة التطبيق" else "App Language"
            val themeHeader = if (language == "fa") "پوسته و تم" else if (language == "ar") "مظهر التطبيق" else "App Theme"
            val themeSystem = if (language == "fa") "سیستم" else if (language == "ar") "تلقائي" else "System"
            val themeLight = if (language == "fa") "روشن" else if (language == "ar") "مضيء" else "Light"
            val themeDark = if (language == "fa") "تاریک" else if (language == "ar") "مظلم" else "Dark"

            val backupHeader = if (language == "fa") "پشتیبان‌گیری و بازیابی" else if (language == "ar") "النسخ الاحتياطي والاستعادة" else "Backup & Restore"
            val exportTitle = if (language == "fa") "استخراج اطلاعات" else if (language == "ar") "تصدير البيانات" else "Export Data"
            val exportDesc = if (language == "fa") "کل دیتای ژورنال را به صورت فایل JSON یا معاملات را به صورت CSV دانلود کنید." else if (language == "ar") "نسخ احتياطي للسجل كملف JSON أو تصدير الصفقات إلى ملف CSV." else "Backup your complete journal database as a JSON file or export trades to CSV."
            val importTitle = if (language == "fa") "بازیابی اطلاعات" else if (language == "ar") "استيراد البيانات" else "Import Data"
            val importDesc = if (language == "fa") "بازیابی و جایگزینی کل اطلاعات ژورنال از روی فایل بک‌آپ JSON قبلی." else if (language == "ar") "استعادة جميع سجلاتك من ملف نسخة احتياطية JSON تم تصديره سابقاً." else "Restore your database records from a previously exported JSON backup file."
            val selectExportFormat = if (language == "fa") "انتخاب فرمت خروجی" else if (language == "ar") "اختر صيغة التصدير" else "Select Export Format"
            val exportJsonTitle = if (language == "fa") "بک‌آپ کامل پایگاه داده (JSON)" else if (language == "ar") "نسخة احتياطية كاملة (JSON)" else "Full Database Backup (JSON)"
            val exportJsonDesc = if (language == "fa") "مناسب برای بازیابی اطلاعات در آینده روی دستگاه دیگر" else if (language == "ar") "مناسب لاستعادة البيانات لاحقاً على جهاز آخر" else "Recommended for restoring on other devices later"
            val exportCsvTitle = if (language == "fa") "لیست معاملات اکسل (CSV)" else if (language == "ar") "جدول الصفقات (CSV)" else "Trades Spreadsheet (CSV)"
            val exportCsvDesc = if (language == "fa") "مناسب برای باز کردن در اکسل و محاسبات ریاضی" else if (language == "ar") "مناسب للفتح في إكسل والحسابات" else "Perfect for opening in Excel and spreadsheets"
            val restoreConfirmTitle = if (language == "fa") "هشدار بازیابی اطلاعات" else if (language == "ar") "تأكيد استعادة البيانات" else "Restore Confirmation"
            val restoreConfirmDesc = if (language == "fa") "آیا مطمئن هستید؟ با بازیابی این بک‌آپ، تمام اطلاعات فعلی ژورنال پاک شده و اطلاعات فایل بک‌آپ جایگزین آن خواهد شد. این عملیات غیرقابل بازگشت است." else if (language == "ar") "هل أنت متأكد؟ عند استعادة هذه النسخة الاحتياطية، سيتم مسح جميع بيانات السجل الحالية واستبدالها ببيانات الملف. هذا الإجراء لا يمكن التراجع عنه." else "Are you sure you want to restore? This will completely replace your current database records with the backup file's contents. This action is irreversible."
            val confirmReplace = if (language == "fa") "تایید و جایگزینی" else if (language == "ar") "تأكيد واستبدال" else "Confirm & Replace"
            val cancel = if (language == "fa") "انصراف" else if (language == "ar") "إلغاء" else "Cancel"
            val offlineNote = if (language == "fa") "🔒 تمام اطلاعات شما کاملاً آفلاین روی حافظه داخلی دستگاه شما نگهداری می‌شود و هیچ بخشی از داده‌ها ارسال نمی‌گردد." else if (language == "ar") "🔒 يتم تخزين جميع بياناتك محلياً بشكل غير متصل على جهازك بالكامل ولا يتم إرسال أي جزء من بياناتك." else "🔒 All your information is stored completely offline on your device storage; no data ever leaves your device."
            val close = if (language == "fa") "بستن" else if (language == "ar") "إغلاق" else "Close"
            val exportSuccess = if (language == "fa") "فایل خروجی با موفقیت آماده شد" else if (language == "ar") "تم تجهيز ملف التصدير بنجاح" else "Export file ready"
            val exportError = if (language == "fa") "خطا در استخراج فایل" else if (language == "ar") "فشل تصدير الملف" else "Failed to generate export"
            val restoreSuccess = if (language == "fa") "دیتا با موفقیت بازیابی شد." else if (language == "ar") "تمت استعادة البيانات بنجاح." else "Backup restored successfully."
            val restoreError = if (language == "fa") "خطا در بازیابی بک‌آپ. لطفاً فایل معتبر انتخاب کنید." else if (language == "ar") "خطأ في استعادة النسخة الاحتياطية." else "Error restoring backup. Please select a valid file."
            
            val resetTitle = if (language == "fa") "پاکسازی کامل اطلاعات" else if (language == "ar") "مسح جميع البيانات" else "Reset All Data"
            val resetDesc = if (language == "fa") "حذف همیشگی تمام معاملات، یادداشت‌های روانشناسی روزانه و تنظیمات حساب کاربری." else if (language == "ar") "حذف دائم لجميع الصفقات والملاحظات اليومية وإعدادات الحساب." else "Permanently delete all trades, daily psychology notes, and account configurations."
            val resetConfirmTitle = if (language == "fa") "هشدار پاکسازی کل اطلاعات" else if (language == "ar") "تأكيد مسح جميع البيانات" else "Confirm Factory Reset"
            val resetConfirmDesc = if (language == "fa") "آیا از پاک کردن کامل و دائمی تمام اطلاعات برنامه اطمینان دارید؟ این عمل به هیچ وجه قابل بازیابی نخواهد بود." else if (language == "ar") "هل أنت متأكد تماماً من حذف جميع البيانات بشكل دائم؟ لا يمكن استعادة هذه البيانات بعد مسحها بأي شكل من الأشكال." else "Are you absolutely sure you want to permanently wipe all application data? This action is absolute and cannot be undone under any circumstances."
            val resetConfirmBtn = if (language == "fa") "پاکسازی نهایی" else if (language == "ar") "مسح نهائي" else "Wipe Database"
            
            val editAccountTitle = if (language == "fa") "ویرایش حساب" else if (language == "ar") "تعديل الحساب" else "Edit Account"
            val accountNameLabel = if (language == "fa") "نام حساب" else if (language == "ar") "اسم الحساب" else "Account Name"
            val initialBalanceLabel = if (language == "fa") "موجودی اولیه" else if (language == "ar") "الرصيد الأولي" else "Initial Balance"
            val save = if (language == "fa") "ذخیره" else if (language == "ar") "حفظ" else "Save"
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = labels.title,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = if (language == "en") Icons.Default.ArrowBack else Icons.Default.ArrowForward,
                                contentDescription = null,
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
                        .fillMaxHeight()
                        .widthIn(max = 800.dp)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                Spacer(modifier = Modifier.height(8.dp))

                // ==========================================
                // 1. ACCOUNT MANAGEMENT SECTION
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0E12)),
                    border = BorderStroke(1.dp, Color(0xFF1F222B))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = labels.accountsHeader,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            IconButton(onClick = { showAddAccountDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = labels.addAccount,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Account Cards
                        accountsList.forEach { account ->
                            val isActive = activeAccountName == account.name
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color(0xFF1F222B),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color(0xFF07080A)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = account.name,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                if (isActive) {
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.primary,
                                                        contentColor = Color.Black,
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = labels.activeLabel,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${String.format(Locale.US, "%,.2f", account.initialBalance)} $currencySymbol",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(onClick = { accountToEdit = account }) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = labels.editLabel,
                                                    tint = Color(0xFF3B82F6),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            if (accountsList.size > 1) {
                                                IconButton(onClick = { viewModel.deleteAccount(account.id) }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = labels.deleteLabel,
                                                        tint = CrimsonRed,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (!isActive) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { viewModel.switchAccount(account.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = labels.switchLabel,
                                                fontSize = 12.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 2. PREFERENCES SECTION (LANGUAGE & THEME)
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0E12)),
                    border = BorderStroke(1.dp, Color(0xFF1F222B))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Language Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = labels.languageHeader,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Language Selector Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("fa" to "فارسی", "en" to "English", "ar" to "العربية").forEach { (code, name) ->
                                val isSelected = language == code
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setLanguage(code) }
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF1F222B),
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Black
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = name,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }

                        Divider(color = Color(0xFF1F222B))

                        // Theme Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = labels.themeHeader,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Theme Options Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "SYSTEM" to labels.themeSystem,
                                "LIGHT" to labels.themeLight,
                                "DARK" to labels.themeDark
                            ).forEach { (mode, name) ->
                                val isSelected = themeMode == mode
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setThemeMode(mode) }
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF1F222B),
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Black
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = name,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 3. BACKUP & RESTORE SECTION
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0E12)),
                    border = BorderStroke(1.dp, Color(0xFF1F222B))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = labels.backupHeader,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Export Button
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showExportSelectionDialog = true }
                                .testTag("export_backup_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black),
                            border = BorderStroke(1.dp, Color(0xFF1F222B))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(EmeraldGreen.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Upload, contentDescription = null, tint = EmeraldGreen)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(labels.exportTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(labels.exportDesc, color = Color(0xFF64748B), fontSize = 11.sp, lineHeight = 16.sp)
                                }
                            }
                        }

                        // Import Button
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { restoreLauncher.launch("*/*") }
                                .testTag("import_backup_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black),
                            border = BorderStroke(1.dp, Color(0xFF1F222B))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFF3B82F6).copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF3B82F6))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(labels.importTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(labels.importDesc, color = Color(0xFF64748B), fontSize = 11.sp, lineHeight = 16.sp)
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 4. FACTORY RESET / CLEAN DANGER ZONE
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0E12)),
                    border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showResetWarningDialog = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(CrimsonRed.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = CrimsonRed)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(labels.resetTitle, color = CrimsonRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(labels.resetDesc, color = Color(0xFF64748B), fontSize = 11.sp, lineHeight = 16.sp)
                        }
                    }
                }

                // Security Offline Note
                Text(
                    text = labels.offlineNote,
                    fontSize = 11.sp,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // ==========================================
        // DIALOGS & SHEET CONTROLS
        // ==========================================

        // 1. ADD ACCOUNT DIALOG
        if (showAddAccountDialog) {
            var accName by remember { mutableStateOf("") }
            var accBalance by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddAccountDialog = false },
                containerColor = Color(0xFF0C0E12),
                title = {
                    Text(text = labels.addAccount, color = Color.White, fontWeight = FontWeight.Black)
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = accName,
                            onValueChange = { accName = it },
                            label = { Text(labels.accountNameLabel) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFF1F222B),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = Color(0xFF64748B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = accBalance,
                            onValueChange = { accBalance = it },
                            label = { Text(labels.initialBalanceLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFF1F222B),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = Color(0xFF64748B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val bal = accBalance.toDoubleOrNull() ?: 10000.0
                            if (accName.isNotBlank()) {
                                viewModel.addAccount(accName, bal)
                                showAddAccountDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(labels.save, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddAccountDialog = false }) {
                        Text(labels.cancel, color = Color(0xFF64748B))
                    }
                }
            )
        }

        // 2. EDIT ACCOUNT DIALOG
        accountToEdit?.let { account ->
            var accName by remember { mutableStateOf(account.name) }
            var accBalance by remember { mutableStateOf(account.initialBalance.toString()) }

            AlertDialog(
                onDismissRequest = { accountToEdit = null },
                containerColor = Color(0xFF0C0E12),
                title = {
                    Text(text = labels.editAccountTitle, color = Color.White, fontWeight = FontWeight.Black)
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = accName,
                            onValueChange = { accName = it },
                            label = { Text(labels.accountNameLabel) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFF1F222B),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = Color(0xFF64748B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = accBalance,
                            onValueChange = { accBalance = it },
                            label = { Text(labels.initialBalanceLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFF1F222B),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = Color(0xFF64748B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val bal = accBalance.toDoubleOrNull() ?: account.initialBalance
                            if (accName.isNotBlank()) {
                                viewModel.updateAccountDetails(account.id, accName, bal)
                                accountToEdit = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(labels.save, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { accountToEdit = null }) {
                        Text(labels.cancel, color = Color(0xFF64748B))
                    }
                }
            )
        }

        // 3. EXPORT SELECTION DIALOG
        if (showExportSelectionDialog) {
            Dialog(onDismissRequest = { showExportSelectionDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1F222B), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0E12))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = labels.selectExportFormat,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Choice A: JSON
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showExportSelectionDialog = false
                                    coroutineScope.launch {
                                        val uri = viewModel.exportBackupFile()
                                        if (uri != null) {
                                            shareFile(uri, "application/json", labels.exportSuccess)
                                        } else {
                                            Toast.makeText(context, labels.exportError, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = Color.Black),
                            border = BorderStroke(1.dp, Color(0xFF1F222B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = labels.exportJsonTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF10B981)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = labels.exportJsonDesc,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        // Choice B: CSV
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showExportSelectionDialog = false
                                    coroutineScope.launch {
                                        val uri = viewModel.exportCsvBackupFile()
                                        if (uri != null) {
                                            shareFile(uri, "text/csv", labels.exportSuccess)
                                        } else {
                                            Toast.makeText(context, labels.exportError, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = Color.Black),
                            border = BorderStroke(1.dp, Color(0xFF1F222B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = labels.exportCsvTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF3B82F6)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = labels.exportCsvDesc,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { showExportSelectionDialog = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(labels.close, color = Color.White)
                        }
                    }
                }
            }
        }

        // 4. RESTORE DATABASE WARNING DIALOG
        if (pendingRestoreUri != null) {
            AlertDialog(
                onDismissRequest = { pendingRestoreUri = null },
                containerColor = Color(0xFF0C0E12),
                title = {
                    Text(
                        text = labels.restoreConfirmTitle,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                },
                text = {
                    Text(
                        text = labels.restoreConfirmDesc,
                        color = Color(0xFF94A3B8),
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                        onClick = {
                            val uriToRestore = pendingRestoreUri
                            pendingRestoreUri = null
                            if (uriToRestore != null) {
                                coroutineScope.launch {
                                    val success = viewModel.restoreBackup(uriToRestore)
                                    if (success) {
                                        Toast.makeText(context, labels.restoreSuccess, Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, labels.restoreError, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    ) {
                        Text(labels.confirmReplace, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingRestoreUri = null }) {
                        Text(labels.cancel, color = Color(0xFF64748B))
                    }
                }
            )
        }

        // 5. FACTORY RESET WARNING DIALOG
        if (showResetWarningDialog) {
            AlertDialog(
                onDismissRequest = { showResetWarningDialog = false },
                containerColor = Color(0xFF0C0E12),
                title = {
                    Text(
                        text = labels.resetConfirmTitle,
                        fontWeight = FontWeight.Black,
                        color = CrimsonRed
                    )
                },
                text = {
                    Text(
                        text = labels.resetConfirmDesc,
                        color = Color(0xFF94A3B8),
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                        onClick = {
                            showResetWarningDialog = false
                            viewModel.resetAllData()
                            Toast.makeText(context, if (language == "fa") "برنامه با موفقیت ریست شد." else "Data fully reset.", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(labels.resetConfirmBtn, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetWarningDialog = false }) {
                        Text(labels.cancel, color = Color(0xFF64748B))
                    }
                }
            )
        }
    }
}
