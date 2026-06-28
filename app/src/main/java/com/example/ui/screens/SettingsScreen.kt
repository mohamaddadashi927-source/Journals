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
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.viewmodel.JournalViewModel
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

    // Dialog states
    var showExportSelectionDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

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
            val title = if (language == "fa") "پشتیبان‌گیری و بازیابی" else if (language == "ar") "النسخ الاحتياطي والاستعادة" else "Backup & Restore"
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
                                imageVector = Icons.Default.ArrowBack,
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // 1. Export Large Action Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showExportSelectionDialog = true }
                        .testTag("export_backup_button"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0C0E12)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF1F222B))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFF10B981).copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = labels.exportTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = labels.exportDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // 2. Import Large Action Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { restoreLauncher.launch("*/*") }
                        .testTag("import_backup_button"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0C0E12)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF1F222B))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFF3B82F6).copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = labels.importTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = labels.importDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Security Note
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labels.offlineNote,
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 1. Export Format Selection Dialog
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

        // 2. Import Restore Confirmation Warning Dialog
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
    }
}
