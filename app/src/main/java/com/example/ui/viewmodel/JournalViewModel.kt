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
                Calendar.SATURDAY -> if (lang == "fa") "شنبه" else "Saturday"
                Calendar.SUNDAY -> if (lang == "fa") "یک‌شنبه" else "Sunday"
                Calendar.MONDAY -> if (lang == "fa") "دوشنبه" else "Monday"
                Calendar.TUESDAY -> if (lang == "fa") "سه‌شنبه" else "Tuesday"
                Calendar.WEDNESDAY -> if (lang == "fa") "چهارشنبه" else "Wednesday"
                Calendar.THURSDAY -> if (lang == "fa") "پنج‌شنبه" else "Thursday"
                Calendar.FRIDAY -> if (lang == "fa") "جمعه" else "Friday"
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

            val rootObj = JSONObject()
            
            // Build Trades Array
            val tradesArr = JSONArray()
            for (t in dbTrades) {
                val o = JSONObject().apply {
                    put("id", t.id)
                    put("side", t.side)
                    put("market", t.market)
                    put("volume", t.volume)
                    put("entryPrice", t.entryPrice)
                    put("exitPrice", t.exitPrice ?: JSONObject.NULL)
                    put("dateTime", t.dateTime)
                    put("fees", t.fees)
                    put("reason", t.reason)
                    put("imagePath", t.imagePath ?: JSONObject.NULL)
                    put("imageBeforePath", t.imageBeforePath ?: JSONObject.NULL)
                    put("imageEntryPath", t.imageEntryPath ?: JSONObject.NULL)
                    put("imageExitPath", t.imageExitPath ?: JSONObject.NULL)
                    put("tags", t.tags)
                    put("strategy", t.strategy)
                    put("grade", t.grade)
                    put("checklistResults", t.checklistResults)
                    put("postTradeNotes", t.postTradeNotes)
                    put("richNotes", t.richNotes)
                    put("emotionalState", t.emotionalState)
                }
                tradesArr.put(o)
            }
            rootObj.put("trades", tradesArr)

            // Build Markets Array
            val marketsArr = JSONArray()
            for (m in dbMarkets) {
                val o = JSONObject().apply {
                    put("id", m.id)
                    put("name", m.name)
                }
                marketsArr.put(o)
            }
            rootObj.put("markets", marketsArr)

            // Build Tags Array
            val tagsArr = JSONArray()
            for (tg in dbTags) {
                val o = JSONObject().apply {
                    put("id", tg.id)
                    put("name", tg.name)
                }
                tagsArr.put(o)
            }
            rootObj.put("tags", tagsArr)

            // Build Daily Journals Array
            val journalsArr = JSONArray()
            for (j in dbJournals) {
                val o = JSONObject().apply {
                    put("dateString", j.dateString)
                    put("content", j.content)
                    put("emotions", j.emotions)
                    put("mistakes", j.mistakes)
                    put("lessons", j.lessons)
                }
                journalsArr.put(o)
            }
            rootObj.put("dailyJournals", journalsArr)

            val backupDir = File(context.cacheDir, "backups")
            backupDir.mkdirs()
            val file = File(backupDir, "trading_journal_backup.json")
            FileWriter(file).use { writer ->
                writer.write(rootObj.toString(2))
            }

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
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
            val rootObj = JSONObject(jsonString)

            // Parse and Restore Markets
            if (rootObj.has("markets")) {
                val marketsArr = rootObj.getJSONArray("markets")
                val marketsList = mutableListOf<Market>()
                for (i in 0 until marketsArr.length()) {
                    val o = marketsArr.getJSONObject(i)
                    marketsList.add(Market(name = o.getString("name")))
                }
                repository.insertMarkets(marketsList)
            }

            // Parse and Restore Tags
            if (rootObj.has("tags")) {
                val tagsArr = rootObj.getJSONArray("tags")
                val tagsList = mutableListOf<Tag>()
                for (i in 0 until tagsArr.length()) {
                    val o = tagsArr.getJSONObject(i)
                    tagsList.add(Tag(name = o.getString("name")))
                }
                repository.insertTags(tagsList)
            }

            // Parse and Restore Daily Journals
            if (rootObj.has("dailyJournals")) {
                val journalsArr = rootObj.getJSONArray("dailyJournals")
                for (i in 0 until journalsArr.length()) {
                    val o = journalsArr.getJSONObject(i)
                    val journal = com.example.data.model.DailyJournal(
                        dateString = o.getString("dateString"),
                        content = o.optString("content", ""),
                        emotions = o.optString("emotions", ""),
                        mistakes = o.optString("mistakes", ""),
                        lessons = o.optString("lessons", "")
                    )
                    dailyJournalDao.insertDailyJournal(journal)
                }
            }

            // Parse and Restore Trades
            if (rootObj.has("trades")) {
                val tradesArr = rootObj.getJSONArray("trades")
                for (i in 0 until tradesArr.length()) {
                    val o = tradesArr.getJSONObject(i)
                    val exitVal = if (o.isNull("exitPrice")) null else o.getDouble("exitPrice")
                    val imageVal = if (o.isNull("imagePath")) null else o.getString("imagePath")
                    val imgBefore = if (o.isNull("imageBeforePath")) null else o.getString("imageBeforePath")
                    val imgEntry = if (o.isNull("imageEntryPath")) null else o.getString("imageEntryPath")
                    val imgExit = if (o.isNull("imageExitPath")) null else o.getString("imageExitPath")
                    val trade = Trade(
                        side = o.getString("side"),
                        market = o.getString("market"),
                        volume = o.getDouble("volume"),
                        entryPrice = o.getDouble("entryPrice"),
                        exitPrice = exitVal,
                        dateTime = o.getLong("dateTime"),
                        fees = o.getDouble("fees"),
                        reason = o.getString("reason"),
                        imagePath = imageVal,
                        imageBeforePath = imgBefore,
                        imageEntryPath = imgEntry,
                        imageExitPath = imgExit,
                        tags = o.getString("tags"),
                        strategy = o.optString("strategy", ""),
                        grade = o.optString("grade", ""),
                        checklistResults = o.optString("checklistResults", ""),
                        postTradeNotes = o.optString("postTradeNotes", ""),
                        richNotes = o.optString("richNotes", ""),
                        emotionalState = o.optString("emotionalState", "")
                    )
                    repository.insertTrade(trade)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("JournalViewModel", "Failed to restore backup", e)
            false
        }
    }

    // CSV Export
    suspend fun exportCsvFile(): Uri? = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        try {
            val dbTrades = repository.allTrades.first()
            val backupDir = File(context.cacheDir, "csv_exports")
            backupDir.mkdirs()
            val file = File(backupDir, "trades_export.csv")
            
            FileWriter(file).use { writer ->
                // Write CSV UTF-8 BOM so Persian characters display correctly in Excel
                writer.write("\uFEFF")
                // Tell Excel to use comma as separator
                writer.write("sep=,\n")
                // Headers (Removed Fees)
                writer.write("شناسه معامله,نوع معامله,بازار,حجم,قیمت ورود,قیمت خروج,سود و زیان,وضعیت معامله,تاریخ,دلایل ورود,استراتژی (تگ‌ها),یادداشت تفصیلی\n")
                
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                for (t in dbTrades) {
                    val pnlStr = if (t.pnl != null) String.format(Locale.US, "%.4f", t.pnl) else "باز"
                    val dateStr = sdf.format(Date(t.dateTime))
                    
                    fun escape(v: String?): String {
                        if (v == null) return "\"\""
                        // Escape double quotes by doubling them, replace newlines with space, and wrap in quotes
                        val clean = v.replace("\"", "\"\"").replace("\n", " ")
                        return "\"$clean\""
                    }
                    
                    val sideStr = if (t.side == "BUY" || t.side == "خرید") "خرید" else "فروش"
                    val statusStr = when (t.status) {
                        "WIN" -> "برد (WIN)"
                        "LOSS" -> "باخت (LOSS)"
                        else -> "موقعیت باز (OPEN)"
                    }

                    val rowFields = listOf(
                        t.id.toString(),
                        escape(sideStr),
                        escape(t.market),
                        String.format(Locale.US, "%.8f", t.volume),
                        String.format(Locale.US, "%.8f", t.entryPrice),
                        if (t.exitPrice != null) String.format(Locale.US, "%.8f", t.exitPrice) else "",
                        pnlStr,
                        escape(statusStr),
                        escape(dateStr),
                        escape(t.reason),
                        escape(t.tags),
                        escape(t.postTradeNotes)
                    )
                    
                    writer.write(rowFields.joinToString(",") + "\n")
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

