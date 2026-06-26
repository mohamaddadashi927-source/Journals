package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyJournal
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.viewmodel.JournalViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DailyJournalTab(viewModel: JournalViewModel) {
    val allJournals by viewModel.allDailyJournals.collectAsState()
    val language by viewModel.language.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedJournalForEdit by remember { mutableStateOf<DailyJournal?>(null) }
    var journalToDelete by remember { mutableStateOf<DailyJournal?>(null) }

    val filteredJournals = remember(allJournals, searchQuery) {
        if (searchQuery.isEmpty()) {
            allJournals.sortedByDescending { it.dateString }
        } else {
            allJournals.filter {
                it.content.contains(searchQuery, ignoreCase = true) ||
                it.emotions.contains(searchQuery, ignoreCase = true) ||
                it.mistakes.contains(searchQuery, ignoreCase = true) ||
                it.lessons.contains(searchQuery, ignoreCase = true) ||
                it.dateString.contains(searchQuery)
            }.sortedByDescending { it.dateString }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search & Stat Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == "fa") "دفترچه یادداشت‌های معاملاتی" else "Trading Journals Feed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text("${allJournals.size} " + (if (language == "fa") "یادداشت" else "logs"), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(if (language == "fa") "جستجو در یادداشت‌ها، احساسات، درس‌ها..." else "Search notes, mistakes, feelings...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        if (filteredJournals.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (language == "fa") "هیچ یادداشت روزانه‌ای یافت نشد." else "No daily journals found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else {
            items(filteredJournals, key = { it.dateString }) { journal ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Header Date
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = journal.dateString,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row {
                                IconButton(onClick = { selectedJournalForEdit = journal }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { journalToDelete = journal }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonRed, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (journal.emotions.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🧠", fontSize = 16.sp)
                                    Column {
                                        Text(
                                            text = if (language == "fa") "احساسات و حالت روحی" else "Psychology & Emotion",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(text = journal.emotions, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }

                            if (journal.content.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("📝", fontSize = 16.sp)
                                    Column {
                                        Text(
                                            text = if (language == "fa") "خلاصه بازار و معاملات روز" else "Daily Market Summary",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(text = journal.content, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }

                            if (journal.mistakes.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("❌", fontSize = 16.sp)
                                    Column {
                                        Text(
                                            text = if (language == "fa") "اشتباهات معاملاتی" else "Trading Mistakes",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CrimsonRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(text = journal.mistakes, style = MaterialTheme.typography.bodyMedium, color = CrimsonRed)
                                    }
                                }
                            }

                            if (journal.lessons.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("💡", fontSize = 16.sp)
                                    Column {
                                        Text(
                                            text = if (language == "fa") "درس اصلی آموخته شده" else "Key Takeaway Lesson",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(text = journal.lessons, style = MaterialTheme.typography.bodyMedium, color = EmeraldGreen)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Dialog
    if (selectedJournalForEdit != null) {
        val editing = selectedJournalForEdit!!
        var noteContent by remember { mutableStateOf(editing.content) }
        var noteEmotions by remember { mutableStateOf(editing.emotions) }
        var noteMistakes by remember { mutableStateOf(editing.mistakes) }
        var noteLessons by remember { mutableStateOf(editing.lessons) }

        AlertDialog(
            onDismissRequest = { selectedJournalForEdit = null },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedJournal = DailyJournal(
                            dateString = editing.dateString,
                            content = noteContent,
                            emotions = noteEmotions,
                            mistakes = noteMistakes,
                            lessons = noteLessons
                        )
                        viewModel.saveDailyJournal(updatedJournal)
                        selectedJournalForEdit = null
                    }
                ) {
                    Text(if (language == "fa") "ذخیره" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedJournalForEdit = null }) {
                    Text(if (language == "fa") "انصراف" else "Cancel")
                }
            },
            title = {
                Text(
                    text = (if (language == "fa") "ویرایش یادداشت - " else "Edit Journal - ") + editing.dateString,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = noteEmotions,
                        onValueChange = { noteEmotions = it },
                        label = { Text(if (language == "fa") "🧠 حالت روحی و احساسی شما" else "🧠 Emotional State") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text(if (language == "fa") "📝 خلاصه بازار امروز" else "📝 Market Summary") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = noteMistakes,
                        onValueChange = { noteMistakes = it },
                        label = { Text(if (language == "fa") "❌ اشتباهات معاملاتی امروز" else "❌ Trading Mistakes") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = noteLessons,
                        onValueChange = { noteLessons = it },
                        label = { Text(if (language == "fa") "💡 درس آموخته شده امروز" else "💡 Key Lesson") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        )
    }

    // Delete confirmation
    if (journalToDelete != null) {
        val deleting = journalToDelete!!
        AlertDialog(
            onDismissRequest = { journalToDelete = null },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDailyJournal(deleting)
                        journalToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                ) {
                    Text(if (language == "fa") "حذف" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { journalToDelete = null }) {
                    Text(if (language == "fa") "انصراف" else "Cancel")
                }
            },
            title = {
                Text(if (language == "fa") "حذف یادداشت روزانه؟" else "Delete Journal Log?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = if (language == "fa") "آیا از حذف یادداشت تاریخ ${deleting.dateString} اطمینان دارید؟ این عمل غیرقابل بازگشت است."
                           else "Are you sure you want to delete the journal log of ${deleting.dateString}? This cannot be undone."
                )
            }
        )
    }
}
