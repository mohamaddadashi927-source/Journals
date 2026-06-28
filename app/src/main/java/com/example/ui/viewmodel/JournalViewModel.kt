package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Market
import com.example.data.model.Tag
import com.example.data.model.Trade
import com.example.data.repository.JournalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

enum class SortType {
    DATE_DESC, DATE_ASC, PNL_DESC, PNL_ASC
}

class JournalViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = JournalRepository(db.tradeDao(), db.marketDao(), db.tagDao())

    private val sharedPrefs = application.getSharedPreferences("trading_journal_settings", Context.MODE_PRIVATE)

    // Search and Filters
    val searchQuery = MutableStateFlow("")
    val selectedMarket = MutableStateFlow<String?>(null)
    val selectedStatus = MutableStateFlow<String?>(null) // "WIN", "LOSS", "OPEN", null
    val selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val startDate = MutableStateFlow<Long?>(null)
    val endDate = MutableStateFlow<Long?>(null)
    val sortType = MutableStateFlow(SortType.DATE_DESC)

    // Settings States
    val currency = MutableStateFlow(sharedPrefs.getString("currency", "USD") ?: "USD") // USD, IRT (تومان), USDT
    val themeMode = MutableStateFlow(sharedPrefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM") // SYSTEM, LIGHT, DARK
    val language = MutableStateFlow(sharedPrefs.getString("language", "fa") ?: "fa") // fa, en, ar
    val initialBalance = MutableStateFlow(sharedPrefs.getFloat("initial_balance", 10000f).toDouble())
    val accountName = MutableStateFlow(sharedPrefs.getString("account_name", "حساب شخصی") ?: "حساب شخصی")
    val isAccountInitialized = MutableStateFlow(sharedPrefs.getBoolean("is_account_initialized", false))

    // Accounts list StateFlow
    val accountsList = MutableStateFlow<List<TradingAccount>>(emptyList())

    init {
        loadAccounts()
    }

    // Trading Goals
    val dailyGoal = MutableStateFlow(sharedPrefs.getFloat("daily_goal", 100f).toDouble())
    val weeklyGoal = MutableStateFlow(sharedPrefs.getFloat("weekly_goal", 500f).toDouble())
    val monthlyGoal = MutableStateFlow(sharedPrefs.getFloat("monthly_goal", 2000f).toDouble())

    // Daily Journal & Checklist DAOs
    private val dailyJournalDao = db.dailyJournalDao()
    private val checklistItemDao = db.checklistItemDao()

    val allDailyJournals: StateFlow<List<com.example.data.model.DailyJournal>> = dailyJournalDao.getAllDailyJournals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChecklistItems: StateFlow<List<com.example.data.model.ChecklistItem>> = checklistItemDao.getAllChecklistItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val enabledChecklistItems: StateFlow<List<com.example.data.model.ChecklistItem>> = checklistItemDao.getEnabledChecklistItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // Data Sources
    val allMarkets: StateFlow<List<Market>> = repository.allMarkets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags: StateFlow<List<Tag>> = repository.allTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered and Sorted Trades
    val trades: StateFlow<List<Trade>> = combine(
        repository.allTrades,
        searchQuery,
        selectedMarket,
        selectedStatus,
        selectedTags,
        startDate,
        endDate,
        sortType
    ) { flows ->
        val allTrades = flows[0] as List<Trade>
        val query = flows[1] as String
        val market = flows[2] as String?
        val status = flows[3] as String?
        val tags = flows[4] as Set<String>
        val start = flows[5] as Long?
        val end = flows[6] as Long?
        val sort = flows[7] as SortType

        var list = allTrades

        // Apply Text Search
        if (query.isNotEmpty()) {
            list = list.filter {
                it.market.contains(query, ignoreCase = true) ||
                it.reason.contains(query, ignoreCase = true) ||
                it.tags.contains(query, ignoreCase = true) ||
                it.postTradeNotes.contains(query, ignoreCase = true)
            }
        }

        // Apply Market Filter
        if (market != null) {
            list = list.filter { it.market.equals(market, ignoreCase = true) }
        }

        // Apply Status Filter
        if (status != null) {
            list = list.filter {
                val isClosed = it.exitPrice != null
                val isProfit = (it.pnl ?: 0.0) > 0.0
                when (status) {
                    "WIN" -> isClosed && isProfit
                    "LOSS" -> isClosed && !isProfit
                    "OPEN" -> !isClosed
                    else -> true
                }
            }
        }

        // Apply Tags Filter (Match if trade has ANY of selected tags)
        if (tags.isNotEmpty()) {
            list = list.filter { trade ->
                val tradeTagSet = trade.tags.split(",").map { it.trim().lowercase() }.toSet()
                tags.any { it.trim().lowercase() in tradeTagSet }
            }
        }

        // Apply Date Range Filter
        if (start != null) {
            list = list.filter { it.dateTime >= start }
        }
        if (end != null) {
            // Include full end day
            list = list.filter { it.dateTime <= end + 86400000L }
        }

        // Apply Sorting
        list = when (sort) {
            SortType.DATE_DESC -> list.sortedByDescending { it.dateTime }
            SortType.DATE_ASC -> list.sortedBy { it.dateTime }
            SortType.PNL_DESC -> list.sortedByDescending { it.pnl ?: Double.NEGATIVE_INFINITY }
            SortType.PNL_ASC -> list.sortedBy { it.pnl ?: Double.POSITIVE_INFINITY }
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Statistics derived from full list
    val statistics = repository.allTrades.map { allTrades ->
        val closedTrades = allTrades.filter { it.exitPrice != null }.sortedBy { it.dateTime }
        val wins = closedTrades.filter { (it.pnl ?: 0.0) > 0.0 }
        val losses = closedTrades.filter { (it.pnl ?: 0.0) < 0.0 }
        
        val totalPnL = closedTrades.sumOf { it.pnl ?: 0.0 }
        val winRate = if (closedTrades.isNotEmpty()) {
            (wins.size.toDouble() / closedTrades.size.toDouble()) * 100
        } else {
            0.0
        }

        val maxProfit = wins.maxOfOrNull { it.pnl ?: 0.0 } ?: 0.0
        val maxLoss = losses.minOfOrNull { it.pnl ?: 0.0 } ?: 0.0

        val avgWin = if (wins.isNotEmpty()) wins.sumOf { it.pnl ?: 0.0 } / wins.size else 0.0
        val avgLoss = if (losses.isNotEmpty()) losses.sumOf { it.pnl ?: 0.0 } / losses.size else 0.0

        val profitFactor = if (losses.isNotEmpty()) {
            val totalLossSum = losses.sumOf { Math.abs(it.pnl ?: 0.0) }
            if (totalLossSum > 0.0) wins.sumOf { it.pnl ?: 0.0 } / totalLossSum else 0.0
        } else {
            0.0
        }

        val expectancy = if (closedTrades.isNotEmpty()) {
            val winFraction = wins.size.toDouble() / closedTrades.size
            val lossFraction = losses.size.toDouble() / closedTrades.size
            (winFraction * avgWin) + (lossFraction * avgLoss)
        } else {
            0.0
        }

        val rrr = if (Math.abs(avgLoss) > 0.0) avgWin / Math.abs(avgLoss) else 0.0

        // Consecutive Wins / Losses Streak
        var currentWinStreak = 0
        var maxWinStreak = 0
        var currentLossStreak = 0
        var maxLossStreak = 0
        for (t in closedTrades) {
            val p = t.pnl ?: 0.0
            if (p > 0.0) {
                currentWinStreak++
                maxWinStreak = Math.max(maxWinStreak, currentWinStreak)
                currentLossStreak = 0
            } else if (p < 0.0) {
                currentLossStreak++
                maxLossStreak = Math.max(maxLossStreak, currentLossStreak)
                currentWinStreak = 0
            }
        }

        // Drawdown calculation
        var peak = initialBalance.value
        var currentEquity = initialBalance.value
        var maxDrawdownAbs = 0.0
        var maxDrawdownPct = 0.0

        for (t in closedTrades) {
            currentEquity += (t.pnl ?: 0.0)
            if (currentEquity > peak) {
                peak = currentEquity
            } else {
                val ddAbs = peak - currentEquity
                val ddPct = if (peak > 0.0) (ddAbs / peak) * 100.0 else 0.0
                if (ddAbs > maxDrawdownAbs) {
                    maxDrawdownAbs = ddAbs
                }
                if (ddPct > maxDrawdownPct) {
                    maxDrawdownPct = ddPct
                }
            }
        }

        val recoveryFactor = if (maxDrawdownAbs > 0.0) totalPnL / maxDrawdownAbs else 0.0

        // Performance stats groupings
        val calendar = Calendar.getInstance()
        val lang = language.value
        
        val pnlByDayOfWeek = closedTrades.groupBy {
            calendar.timeInMillis = it.dateTime
            calendar.get(Calendar.DAY_OF_WEEK)
        }.mapValues { entry -> entry.value.sumOf { it.pnl ?: 0.0 } }

        fun getDayName(day: Int?, lang: String): String {
            if (day == null) return "N/A"
            return when (day) {
                Calendar.SATURDAY -> if (lang == "fa") "شنبه" else if (lang == "ar") "السبت" else "Saturday"
                Calendar.SUNDAY -> if (lang == "fa") "یک‌شنبه" else if (lang == "ar") "الأحد" else "Sunday"
                Calendar.MONDAY -> if (lang == "fa") "دوشنبه" else if (lang == "ar") "الإثنين" else "Monday"
                Calendar.TUESDAY -> if (lang == "fa") "سه‌شنبه" else if (lang == "ar") "الثلاثاء" else "Tuesday"
                Calendar.WEDNESDAY -> if (lang == "fa") "چهارشنبه" else if (lang == "ar") "الأربعاء" else "Wednesday"
                Calendar.THURSDAY -> if (lang == "fa") "پنج‌شنبه" else if (lang == "ar") "الخميس" else "Thursday"
                Calendar.FRIDAY -> if (lang == "fa") "جمعه" else if (lang == "ar") "الجمعة" else "Friday"
                else -> "N/A"
            }
        }
        
        val bestDayInt = pnlByDayOfWeek.maxByOrNull { it.value }?.key
        val worstDayInt = pnlByDayOfWeek.minByOrNull { it.value }?.key
        
        val bestDay = getDayName(bestDayInt, lang)
        val worstDay = getDayName(worstDayInt, lang)

        val pnlByHour = closedTrades.groupBy {
            calendar.timeInMillis = it.dateTime
            calendar.get(Calendar.HOUR_OF_DAY)
        }.mapValues { entry -> entry.value.sumOf { it.pnl ?: 0.0 } }

        val bestHourInt = pnlByHour.maxByOrNull { it.value }?.key
        val worstHourInt = pnlByHour.minByOrNull { it.value }?.key
        
        val bestHour = if (bestHourInt != null) "$bestHourInt:00" else "N/A"
        val worstHour = if (worstHourInt != null) "$worstHourInt:00" else "N/A"

        val pnlBySymbol = closedTrades.groupBy { it.market }
            .mapValues { entry -> entry.value.sumOf { it.pnl ?: 0.0 } }
        
        val bestSymbol = pnlBySymbol.maxByOrNull { it.value }?.key ?: "N/A"
        val worstSymbol = pnlBySymbol.minByOrNull { it.value }?.key ?: "N/A"

        val pnlByStrategy = closedTrades.filter { it.strategy.isNotEmpty() }
            .groupBy { it.strategy }
            .mapValues { entry -> entry.value.sumOf { it.pnl ?: 0.0 } }

        val pnlByGrade = closedTrades.filter { it.grade.isNotEmpty() }
            .groupBy { it.grade }
            .mapValues { entry -> entry.value.sumOf { it.pnl ?: 0.0 } }

        TradeStats(
            totalPnL = totalPnL,
            winRate = winRate,
            totalTradesCount = allTrades.size,
            closedTradesCount = closedTrades.size,
            openTradesCount = allTrades.filter { it.exitPrice == null }.size,
            winsCount = wins.size,
            lossesCount = losses.size,
            maxProfit = maxProfit,
            maxLoss = maxLoss,
            profitFactor = profitFactor,
            expectancy = expectancy,
            avgWin = avgWin,
            avgLoss = avgLoss,
            riskRewardRatio = rrr,
            maxDrawdownAbs = maxDrawdownAbs,
            maxDrawdownPct = maxDrawdownPct,
            recoveryFactor = recoveryFactor,
            maxWinStreak = maxWinStreak,
            maxLossStreak = maxLossStreak,
            bestDay = bestDay,
            worstDay = worstDay,
            bestHour = bestHour,
            worstHour = worstHour,
            bestSymbol = bestSymbol,
            worstSymbol = worstSymbol,
            pnlByDayOfWeek = pnlByDayOfWeek,
            pnlByHour = pnlByHour,
            pnlBySymbol = pnlBySymbol,
            pnlByStrategy = pnlByStrategy,
            pnlByGrade = pnlByGrade
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TradeStats())

    val advancedStats: StateFlow<com.example.data.analysis.AdvancedStats?> = combine(
        repository.allTrades,
        allDailyJournals,
        language
    ) { allTrades, allJournals, lang ->
        com.example.data.analysis.AnalysisEngine.analyze(allTrades, allJournals, lang)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Save/Update Settings in SharedPreferences
    fun setCurrency(newCurrency: String) {
        currency.value = newCurrency
        sharedPrefs.edit().putString("currency", newCurrency).apply()
    }

    fun setThemeMode(newTheme: String) {
        themeMode.value = newTheme
        sharedPrefs.edit().putString("theme_mode", newTheme).apply()
    }

    fun setLanguage(newLanguage: String) {
        language.value = newLanguage
        sharedPrefs.edit().putString("language", newLanguage).apply()
    }

    fun initializeAccount(name: String, balance: Double) {
        accountName.value = name
        initialBalance.value = balance
        isAccountInitialized.value = true
        sharedPrefs.edit()
            .putString("account_name", name)
            .putFloat("initial_balance", balance.toFloat())
            .putBoolean("is_account_initialized", true)
            .apply()
        
        // Sync inside our loaded list
        val activeId = sharedPrefs.getString("active_account_id", "acc_default") ?: "acc_default"
        val currentList = accountsList.value.map {
            if (it.id == activeId) it.copy(name = name, initialBalance = balance) else it
        }
        accountsList.value = currentList
        sharedPrefs.edit()
            .putString("${activeId}_name", name)
            .putFloat("${activeId}_balance", balance.toFloat())
            .apply()
    }

    fun loadAccounts() {
        val idsString = sharedPrefs.getString("accounts_ids_list", "") ?: ""
        if (idsString.isEmpty()) {
            val defaultAcc = TradingAccount(
                id = "acc_default",
                name = accountName.value,
                initialBalance = initialBalance.value
            )
            accountsList.value = listOf(defaultAcc)
            sharedPrefs.edit()
                .putString("accounts_ids_list", "acc_default")
                .putString("acc_default_name", defaultAcc.name)
                .putFloat("acc_default_balance", defaultAcc.initialBalance.toFloat())
                .putString("active_account_id", "acc_default")
                .apply()
        } else {
            val ids = idsString.split(",")
            val list = mutableListOf<TradingAccount>()
            for (id in ids) {
                if (id.isNotEmpty()) {
                    val name = sharedPrefs.getString("${id}_name", "") ?: ""
                    val balance = sharedPrefs.getFloat("${id}_balance", 10000f).toDouble()
                    if (name.isNotEmpty()) {
                        list.add(TradingAccount(id, name, balance))
                    }
                }
            }
            if (list.isEmpty()) {
                val defaultAcc = TradingAccount("acc_default", accountName.value, initialBalance.value)
                list.add(defaultAcc)
            }
            accountsList.value = list
        }
    }

    fun addAccount(name: String, balance: Double) {
        val newId = "acc_" + System.currentTimeMillis()
        val currentList = accountsList.value.toMutableList()
        val newAcc = TradingAccount(newId, name, balance)
        currentList.add(newAcc)
        accountsList.value = currentList

        val idsString = currentList.joinToString(",") { it.id }
        sharedPrefs.edit()
            .putString("accounts_ids_list", idsString)
            .putString("${newId}_name", name)
            .putFloat("${newId}_balance", balance.toFloat())
            .apply()
    }

    fun updateAccountDetails(id: String, name: String, balance: Double) {
        val currentList = accountsList.value.map {
            if (it.id == id) it.copy(name = name, initialBalance = balance) else it
        }
        accountsList.value = currentList
        sharedPrefs.edit()
            .putString("${id}_name", name)
            .putFloat("${id}_balance", balance.toFloat())
            .apply()
        
        val activeId = sharedPrefs.getString("active_account_id", "acc_default") ?: "acc_default"
        if (activeId == id) {
            accountName.value = name
            initialBalance.value = balance
            sharedPrefs.edit()
                .putString("account_name", name)
                .putFloat("initial_balance", balance.toFloat())
                .apply()
        }
    }

    fun switchAccount(id: String) {
        val account = accountsList.value.find { it.id == id } ?: return
        sharedPrefs.edit().putString("active_account_id", id).apply()
        initializeAccount(account.name, account.initialBalance)
    }

    fun deleteAccount(id: String) {
        if (id == "acc_default") return
        val currentList = accountsList.value.toMutableList()
        currentList.removeAll { it.id == id }
        accountsList.value = currentList

        val idsString = currentList.joinToString(",") { it.id }
        sharedPrefs.edit()
            .putString("accounts_ids_list", idsString)
            .remove("${id}_name")
            .remove("${id}_balance")
            .apply()
        
        val activeId = sharedPrefs.getString("active_account_id", "acc_default") ?: "acc_default"
        if (activeId == id) {
            val fallback = currentList.firstOrNull() ?: TradingAccount("acc_default", "حساب شخصی", 10000.0)
            switchAccount(fallback.id)
        }
    }

    fun setDailyGoal(goal: Double) {
        dailyGoal.value = goal
        sharedPrefs.edit().putFloat("daily_goal", goal.toFloat()).apply()
    }

    fun setWeeklyGoal(goal: Double) {
        weeklyGoal.value = goal
        sharedPrefs.edit().putFloat("weekly_goal", goal.toFloat()).apply()
    }

    fun setMonthlyGoal(goal: Double) {
        monthlyGoal.value = goal
        sharedPrefs.edit().putFloat("monthly_goal", goal.toFloat()).apply()
    }

    // Daily Journal Operations
    fun getDailyJournalByDate(dateString: String): Flow<com.example.data.model.DailyJournal?> =
        dailyJournalDao.getDailyJournalByDate(dateString)

    fun saveDailyJournal(journal: com.example.data.model.DailyJournal) {
        viewModelScope.launch(Dispatchers.IO) {
            dailyJournalDao.insertDailyJournal(journal)
        }
    }

    fun deleteDailyJournal(journal: com.example.data.model.DailyJournal) {
        viewModelScope.launch(Dispatchers.IO) {
            dailyJournalDao.deleteDailyJournal(journal)
        }
    }

    // Checklist Operations
    fun addChecklistItem(title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            checklistItemDao.insertChecklistItem(com.example.data.model.ChecklistItem(title = title))
        }
    }

    fun deleteChecklistItem(item: com.example.data.model.ChecklistItem) {
        viewModelScope.launch(Dispatchers.IO) {
            checklistItemDao.deleteChecklistItem(item)
        }
    }


    // Trade DB Operations
    fun getTradeById(id: Int): Flow<Trade?> = repository.getTradeById(id)

    fun insertTrade(trade: Trade) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTrade(trade)
        }
    }

    fun updateTrade(trade: Trade) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTrade(trade)
        }
    }

    fun deleteTrade(trade: Trade) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTrade(trade)
        }
    }

    fun resetAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllTrades()
            sharedPrefs.edit()
                .remove("account_name")
                .remove("initial_balance")
                .remove("is_account_initialized")
                .apply()
            accountName.value = "حساب شخصی"
            initialBalance.value = 10000.0
            isAccountInitialized.value = false
        }
    }

    // Markets & Tags Operations
    fun addMarket(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMarket(Market(name = name))
        }
    }

    fun deleteMarket(market: Market) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMarket(market)
        }
    }

    fun addTag(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTag(Tag(name = name))
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTag(tag)
        }
    }

    // Reset All Filters
    fun resetFilters() {
        searchQuery.value = ""
        selectedMarket.value = null
        selectedStatus.value = null
        selectedTags.value = emptySet()
        startDate.value = null
        endDate.value = null
    }

    // Image helper: Save image to local app files directory
    suspend fun saveImageToLocal(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val filename = "trade_${System.currentTimeMillis()}.jpg"
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
        val file = File(directory, filename)
        return@withContext try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("JournalViewModel", "Failed to save image locally", e)
            null
        }
    }

    // Backup & Restore & Export Functions

    // JSON Backup sharing URI
    suspend fun exportBackupFile(): Uri? = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        try {
            // Retrieve all data
            val dbTrades = repository.allTrades.first()
            val dbMarkets = repository.allMarkets.first()
            val dbTags = repository.allTags.first()
            val dbJournals = dailyJournalDao.getAllDailyJournals().first()
            val dbChecklist = checklistItemDao.getAllChecklistItems().first()

            com.example.data.backup.BackupHelper.exportDatabaseToJson(
                context, dbTrades, dbJournals, dbMarkets, dbTags, dbChecklist
            )
        } catch (e: Exception) {
            Log.e("JournalViewModel", "Failed to export backup", e)
            null
        }
    }

    // JSON Restore
    suspend fun restoreBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val jsonString = inputStream?.bufferedReader().use { it?.readText() } ?: return@withContext false
            com.example.data.backup.BackupHelper.importDatabaseFromJson(context, jsonString, db)
        } catch (e: Exception) {
            Log.e("JournalViewModel", "Failed to restore backup", e)
            false
        }
    }

    // CSV Trades Export
    suspend fun exportCsvBackupFile(): Uri? = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        try {
            val dbTrades = repository.allTrades.first()
            com.example.data.backup.BackupHelper.exportTradesToCsv(context, dbTrades)
        } catch (e: Exception) {
            Log.e("JournalViewModel", "Failed to export CSV backup", e)
            null
        }
    }

    // Beautiful Excel Styled Export
    suspend fun exportCsvFile(): Uri? = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        try {
            val dbTrades = repository.allTrades.first()
            val backupDir = File(context.cacheDir, "excel_exports")
            backupDir.mkdirs()
            val file = File(backupDir, "trades_journal_export.xls")
            
            java.io.FileOutputStream(file).use { fos ->
                java.io.OutputStreamWriter(fos, "UTF-8").use { writer ->
                    // Excel-compatible HTML Template with proper borders, widths, colors, and gridlines
                    writer.write("<html xmlns:o=\"urn:schemas-microsoft-com:office:office\" xmlns:x=\"urn:schemas-microsoft-com:office:excel\" xmlns=\"http://www.w3.org/TR/REC-html40\">\n")
                    writer.write("<head>\n")
                    writer.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n")
                    writer.write("<!--[if gte mso 9]>\n")
                    writer.write("<xml>\n")
                    writer.write(" <x:ExcelWorkbook>\n")
                    writer.write("  <x:ExcelWorksheets>\n")
                    writer.write("   <x:ExcelWorksheet>\n")
                    writer.write("    <x:Name>Journal Trades</x:Name>\n")
                    writer.write("    <x:WorksheetOptions>\n")
                    writer.write("     <x:DisplayGridlines/>\n")
                    writer.write("    </x:WorksheetOptions>\n")
                    writer.write("   </x:ExcelWorksheet>\n")
                    writer.write("  </x:ExcelWorksheets>\n")
                    writer.write(" </x:ExcelWorkbook>\n")
                    writer.write("</xml>\n")
                    writer.write("<![endif]-->\n")
                    writer.write("<style>\n")
                    writer.write("  body { font-family: 'Tahoma', 'Segoe UI', Arial, sans-serif; }\n")
                    writer.write("  table { border-collapse: collapse; width: 100%; direction: rtl; }\n")
                    writer.write("  th { background-color: #1E3A8A; color: #FFFFFF; font-weight: bold; border: 1px solid #111827; padding: 12px 10px; font-size: 13px; text-align: center; }\n")
                    writer.write("  td { border: 1px solid #D1D5DB; padding: 10px 8px; font-size: 12px; text-align: center; vertical-align: middle; }\n")
                    writer.write("  .win-bg { background-color: #DCFCE7; color: #15803D; font-weight: bold; }\n")
                    writer.write("  .loss-bg { background-color: #FEE2E2; color: #B91C1C; font-weight: bold; }\n")
                    writer.write("  .open-bg { background-color: #EFF6FF; color: #1D4ED8; font-weight: bold; }\n")
                    writer.write("  .buy-side { color: #16A34A; font-weight: bold; }\n")
                    writer.write("  .sell-side { color: #DC2626; font-weight: bold; }\n")
                    writer.write("  .notes-cell { text-align: right; white-space: normal; }\n")
                    writer.write("  .header-title { font-size: 18px; font-weight: bold; color: #1E3A8A; padding: 15px 0px; text-align: center; }\n")
                    writer.write("</style>\n")
                    writer.write("</head>\n")
                    writer.write("<body>\n")
                    val lang = language.value
                    val titleText = when (lang) {
                        "fa" -> "گزارش جامع دفترچه ژورنال معاملاتی حرفه‌ای"
                        "ar" -> "التقرير الشامل لسجل التداول الاحترافي"
                        else -> "Comprehensive Professional Trading Journal Report"
                    }
                    val thId = if (lang == "fa") "شناسه معامله" else if (lang == "ar") "معرّف الصفقة" else "Trade ID"
                    val thSide = if (lang == "fa") "نوع معامله" else if (lang == "ar") "نوع الصفقة" else "Side"
                    val thMarket = if (lang == "fa") "نماد / بازار" else if (lang == "ar") "الرمز / السوق" else "Asset / Market"
                    val thVolume = if (lang == "fa") "حجم" else if (lang == "ar") "الحجم" else "Volume"
                    val thEntry = if (lang == "fa") "قیمت ورود" else if (lang == "ar") "سعر الدخول" else "Entry Price"
                    val thExit = if (lang == "fa") "قیمت خروج" else if (lang == "ar") "سعر الخروج" else "Exit Price"
                    val thPnl = if (lang == "fa") "سود و زیان" else if (lang == "ar") "الربح/الخسارة" else "PnL"
                    val thStatus = if (lang == "fa") "وضعیت" else if (lang == "ar") "الحالة" else "Status"
                    val thDate = if (lang == "fa") "تاریخ و ساعت" else if (lang == "ar") "التاريخ والوقت" else "Date & Time"
                    val thNotes = if (lang == "fa") "دلایل ورود و تحلیل" else if (lang == "ar") "أسباب الدخول والتحليل" else "Entry Reasons & Analysis"
                    val thStrategy = if (lang == "fa") "استراتژی (تگ‌ها)" else if (lang == "ar") "الاستراتيجية (الوسوم)" else "Strategy (Tags)"
                    val thReview = if (lang == "fa") "یادداشت‌های بررسی معامله" else if (lang == "ar") "ملاحظات مراجعة الصفقة" else "Post-Trade Review Notes"

                    writer.write("  <div class=\"header-title\">$titleText</div>\n")
                    writer.write("  <table>\n")
                    writer.write("    <tr>\n")
                    writer.write("      <th>$thId</th>\n")
                    writer.write("      <th>$thSide</th>\n")
                    writer.write("      <th>$thMarket</th>\n")
                    writer.write("      <th>$thVolume</th>\n")
                    writer.write("      <th>$thEntry</th>\n")
                    writer.write("      <th>$thExit</th>\n")
                    writer.write("      <th>$thPnl</th>\n")
                    writer.write("      <th>$thStatus</th>\n")
                    writer.write("      <th>$thDate</th>\n")
                    writer.write("      <th>$thNotes</th>\n")
                    writer.write("      <th>$thStrategy</th>\n")
                    writer.write("      <th>$thReview</th>\n")
                    writer.write("    </tr>\n")
                    
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    fun escapeHtml(v: String?): String {
                        if (v == null) return ""
                        return v.replace("&", "&amp;")
                            .replace("<", "&lt;")
                            .replace(">", "&gt;")
                            .replace("\"", "&quot;")
                            .replace("\n", "<br/>")
                    }

                    for (t in dbTrades) {
                        val pnlStr = if (t.pnl != null) {
                            String.format(Locale.US, "%.4f", t.pnl)
                        } else {
                            if (lang == "fa") "باز" else if (lang == "ar") "مفتوح" else "Open"
                        }
                        val dateStr = sdf.format(Date(t.dateTime))
                        
                        val sideClass = if (t.side == "BUY" || t.side == "خرید") "buy-side" else "sell-side"
                        val sideText = if (t.side == "BUY" || t.side == "خرید") {
                            if (lang == "fa") "خرید (BUY)" else if (lang == "ar") "شراء (BUY)" else "BUY"
                        } else {
                            if (lang == "fa") "فروش (SELL)" else if (lang == "ar") "بيع (SELL)" else "SELL"
                        }
                        
                        val statusClass = when (t.status) {
                            "WIN" -> "win-bg"
                            "LOSS" -> "loss-bg"
                            else -> "open-bg"
                        }
                        val statusText = when (t.status) {
                            "WIN" -> if (lang == "fa") "برد (WIN)" else if (lang == "ar") "ربح (WIN)" else "WIN"
                            "LOSS" -> if (lang == "fa") "باخت (LOSS)" else if (lang == "ar") "خسارة (LOSS)" else "LOSS"
                            else -> if (lang == "fa") "موقعیت باز" else if (lang == "ar") "مركز مفتوح" else "Open Position"
                        }

                        writer.write("    <tr>\n")
                        writer.write("      <td>${t.id}</td>\n")
                        writer.write("      <td class=\"$sideClass\">$sideText</td>\n")
                        writer.write("      <td><strong>${escapeHtml(t.market)}</strong></td>\n")
                        writer.write("      <td>${String.format(Locale.US, "%.8f", t.volume)}</td>\n")
                        writer.write("      <td>${String.format(Locale.US, "%.8f", t.entryPrice)}</td>\n")
                        writer.write("      <td>${if (t.exitPrice != null) String.format(Locale.US, "%.8f", t.exitPrice) else ""}</td>\n")
                        writer.write("      <td class=\"$statusClass\">$pnlStr</td>\n")
                        writer.write("      <td class=\"$statusClass\">$statusText</td>\n")
                        writer.write("      <td>$dateStr</td>\n")
                        writer.write("      <td class=\"notes-cell\">${escapeHtml(t.reason)}</td>\n")
                        writer.write("      <td>${escapeHtml(t.tags)}</td>\n")
                        writer.write("      <td class=\"notes-cell\">${escapeHtml(t.postTradeNotes)}</td>\n")
                        writer.write("    </tr>\n")
                    }
                    
                    writer.write("  </table>\n")
                    writer.write("</body>\n")
                    writer.write("</html>\n")
                }
            }

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.e("JournalViewModel", "Failed to export CSV", e)
            null
        }
    }
}

data class TradeStats(
    val totalPnL: Double = 0.0,
    val winRate: Double = 0.0,
    val totalTradesCount: Int = 0,
    val closedTradesCount: Int = 0,
    val openTradesCount: Int = 0,
    val winsCount: Int = 0,
    val lossesCount: Int = 0,
    val maxProfit: Double = 0.0,
    val maxLoss: Double = 0.0,
    // New metrics
    val profitFactor: Double = 0.0,
    val expectancy: Double = 0.0,
    val avgWin: Double = 0.0,
    val avgLoss: Double = 0.0,
    val riskRewardRatio: Double = 0.0,
    val maxDrawdownPct: Double = 0.0,
    val maxDrawdownAbs: Double = 0.0,
    val recoveryFactor: Double = 0.0,
    val maxWinStreak: Int = 0,
    val maxLossStreak: Int = 0,
    // Performance stats
    val bestDay: String = "N/A",
    val worstDay: String = "N/A",
    val bestHour: String = "N/A",
    val worstHour: String = "N/A",
    val bestSymbol: String = "N/A",
    val worstSymbol: String = "N/A",
    // Performance grouping maps (for charts)
    val pnlByDayOfWeek: Map<Int, Double> = emptyMap(),
    val pnlByHour: Map<Int, Double> = emptyMap(),
    val pnlBySymbol: Map<String, Double> = emptyMap(),
    val pnlByStrategy: Map<String, Double> = emptyMap(),
    val pnlByGrade: Map<String, Double> = emptyMap()
)

data class TradingAccount(
    val id: String,
    val name: String,
    val initialBalance: Double
)

