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
        val closedTrades = allTrades.filter { it.exitPrice != null }
        val wins = closedTrades.filter { (it.pnl ?: 0.0) > 0.0 }
        val losses = closedTrades.filter { (it.pnl ?: 0.0) <= 0.0 }
        
        val totalPnL = closedTrades.sumOf { it.pnl ?: 0.0 }
        val winRate = if (closedTrades.isNotEmpty()) {
            (wins.size.toDouble() / closedTrades.size.toDouble()) * 100
        } else {
            0.0
        }

        val maxProfit = closedTrades.maxOfOrNull { it.pnl ?: 0.0 } ?: 0.0
        val maxLoss = closedTrades.minOfOrNull { it.pnl ?: 0.0 } ?: 0.0

        TradeStats(
            totalPnL = totalPnL,
            winRate = winRate,
            totalTradesCount = allTrades.size,
            closedTradesCount = closedTrades.size,
            openTradesCount = allTrades.filter { it.exitPrice == null }.size,
            winsCount = wins.size,
            lossesCount = losses.size,
            maxProfit = maxProfit,
            maxLoss = maxLoss
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
                    put("tags", t.tags)
                    put("postTradeNotes", t.postTradeNotes)
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

            // Parse and Restore Trades
            if (rootObj.has("trades")) {
                val tradesArr = rootObj.getJSONArray("trades")
                for (i in 0 until tradesArr.length()) {
                    val o = tradesArr.getJSONObject(i)
                    val exitVal = if (o.isNull("exitPrice")) null else o.getDouble("exitPrice")
                    val imageVal = if (o.isNull("imagePath")) null else o.getString("imagePath")
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
                        tags = o.getString("tags"),
                        postTradeNotes = o.optString("postTradeNotes", "")
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
                // Headers
                writer.write("ID,سمت معامله,بازار,حجم,قیمت ورود,قیمت خروج,سود_زیان,وضعیت,کارمزد,تاریخ,دلیل ورود,برچسب‌ها,یادداشت بعد از معامله\n")
                
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                for (t in dbTrades) {
                    val pnlStr = t.pnl?.toString() ?: "باز"
                    val dateStr = sdf.format(Date(t.dateTime))
                    // Escape commas in reasons and notes to prevent CSV shifting
                    val cleanReason = t.reason.replace(",", "،").replace("\n", " ")
                    val cleanNotes = t.postTradeNotes.replace(",", "،").replace("\n", " ")
                    val cleanTags = t.tags.replace(",", " | ")
                    
                    val line = "${t.id},${t.side},${t.market},${t.volume},${t.entryPrice},${t.exitPrice ?: ""},$pnlStr,${t.status},${t.fees},$dateStr,$cleanReason,$cleanTags,$cleanNotes\n"
                    writer.write(line)
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
    val maxLoss: Double = 0.0
)
