package com.example.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.model.ChecklistItem
import com.example.data.model.DailyJournal
import com.example.data.model.Market
import com.example.data.model.Tag
import com.example.data.model.Trade
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupHelper {

    private const val TAG = "BackupHelper"

    /**
     * Exports the entire application database into a formatted JSON file and returns its shared Uri.
     */
    fun exportDatabaseToJson(
        context: Context,
        trades: List<Trade>,
        journals: List<DailyJournal>,
        markets: List<Market>,
        tags: List<Tag>,
        checklistItems: List<ChecklistItem>
    ): Uri? {
        try {
            val root = JSONObject()
            root.put("backupVersion", 1)
            root.put("exportedAt", System.currentTimeMillis())

            // 1. Serialize Trades
            val tradesArray = JSONArray()
            trades.forEach { t ->
                val obj = JSONObject().apply {
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
                tradesArray.put(obj)
            }
            root.put("trades", tradesArray)

            // 2. Serialize Journals
            val journalsArray = JSONArray()
            journals.forEach { j ->
                val obj = JSONObject().apply {
                    put("dateString", j.dateString)
                    put("content", j.content)
                    put("emotions", j.emotions)
                    put("mistakes", j.mistakes)
                    put("lessons", j.lessons)
                }
                journalsArray.put(obj)
            }
            root.put("journals", journalsArray)

            // 3. Serialize Markets
            val marketsArray = JSONArray()
            markets.forEach { m ->
                val obj = JSONObject().apply {
                    put("id", m.id)
                    put("name", m.name)
                }
                marketsArray.put(obj)
            }
            root.put("markets", marketsArray)

            // 4. Serialize Tags
            val tagsArray = JSONArray()
            tags.forEach { t ->
                val obj = JSONObject().apply {
                    put("id", t.id)
                    put("name", t.name)
                }
                tagsArray.put(obj)
            }
            root.put("tags", tagsArray)

            // 5. Serialize ChecklistItems
            val checklistArray = JSONArray()
            checklistItems.forEach { c ->
                val obj = JSONObject().apply {
                    put("id", c.id)
                    put("title", c.title)
                    put("isEnabled", c.isEnabled)
                }
                checklistArray.put(obj)
            }
            root.put("checklistItems", checklistArray)

            // Write JSON String to Cache File for sharing
            val backupDir = File(context.cacheDir, "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(backupDir, "trading_journal_backup_$timeStamp.json")
            if (file.exists()) {
                file.delete()
            }

            file.writeText(root.toString(2)) // Indented for readability

            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export JSON backup", e)
            return null
        }
    }

    /**
     * Exports trades into a beautifully structured CSV file for spreadsheet compatibility.
     */
    fun exportTradesToCsv(context: Context, trades: List<Trade>): Uri? {
        try {
            val csvBuilder = StringBuilder()
            
            // Header row
            csvBuilder.append("Trade ID,Side,Market,Volume,Entry Price,Exit Price,PnL,Fees,Date & Time,Strategy,Grade,Emotional State,Reason,Post-Trade Notes,Tags\n")

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

            trades.forEach { t ->
                val pnlStr = if (t.exitPrice != null) String.format(Locale.US, "%.4f", t.pnl) else "OPEN"
                val exitPriceStr = if (t.exitPrice != null) String.format(Locale.US, "%.4f", t.exitPrice) else ""
                val dateStr = sdf.format(Date(t.dateTime))

                csvBuilder.append("${t.id},")
                    .append("${escapeCsv(t.side)},")
                    .append("${escapeCsv(t.market)},")
                    .append("${String.format(Locale.US, "%.6f", t.volume)},")
                    .append("${String.format(Locale.US, "%.4f", t.entryPrice)},")
                    .append("${exitPriceStr},")
                    .append("${pnlStr},")
                    .append("${String.format(Locale.US, "%.4f", t.fees)},")
                    .append("${dateStr},")
                    .append("${escapeCsv(t.strategy)},")
                    .append("${escapeCsv(t.grade)},")
                    .append("${escapeCsv(t.emotionalState)},")
                    .append("${escapeCsv(t.reason)},")
                    .append("${escapeCsv(t.postTradeNotes)},")
                    .append("${escapeCsv(t.tags)}\n")
            }

            val backupDir = File(context.cacheDir, "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(backupDir, "trades_export_$timeStamp.csv")
            if (file.exists()) {
                file.delete()
            }

            file.writeText(csvBuilder.toString(), Charsets.UTF_8)

            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export CSV trades", e)
            return null
        }
    }

    /**
     * Imports and restores the database from a JSON string under a Room Database Transaction.
     */
    suspend fun importDatabaseFromJson(context: Context, jsonString: String, db: AppDatabase): Boolean {
        return try {
            val root = JSONObject(jsonString)

            db.withTransaction {
                // 1. Restore ChecklistItems
                if (root.has("checklistItems")) {
                    val list = root.getJSONArray("checklistItems")
                    db.checklistItemDao().deleteAllChecklistItems()
                    for (i in 0 until list.length()) {
                        val obj = list.getJSONObject(i)
                        val item = ChecklistItem(
                            id = obj.optInt("id", 0),
                            title = obj.getString("title"),
                            isEnabled = obj.optBoolean("isEnabled", true)
                        )
                        db.checklistItemDao().insertChecklistItem(item)
                    }
                }

                // 2. Restore Tags
                if (root.has("tags")) {
                    val list = root.getJSONArray("tags")
                    // Clearing tags
                    db.tagDao().deleteAllTags()
                    for (i in 0 until list.length()) {
                        val obj = list.getJSONObject(i)
                        val tag = Tag(
                            id = obj.optInt("id", 0),
                            name = obj.getString("name")
                        )
                        db.tagDao().insertTag(tag)
                    }
                }

                // 3. Restore Markets
                if (root.has("markets")) {
                    val list = root.getJSONArray("markets")
                    // Clearing markets
                    db.marketDao().deleteAllMarkets()
                    for (i in 0 until list.length()) {
                        val obj = list.getJSONObject(i)
                        val market = Market(
                            id = obj.optInt("id", 0),
                            name = obj.getString("name")
                        )
                        db.marketDao().insertMarket(market)
                    }
                }

                // 4. Restore Journals
                if (root.has("journals")) {
                    val list = root.getJSONArray("journals")
                    db.dailyJournalDao().deleteAllDailyJournals()
                    for (i in 0 until list.length()) {
                        val obj = list.getJSONObject(i)
                        val journal = DailyJournal(
                            dateString = obj.getString("dateString"),
                            content = obj.optString("content", ""),
                            emotions = obj.optString("emotions", ""),
                            mistakes = obj.optString("mistakes", ""),
                            lessons = obj.optString("lessons", "")
                        )
                        db.dailyJournalDao().insertDailyJournal(journal)
                    }
                }

                // 5. Restore Trades
                if (root.has("trades")) {
                    val list = root.getJSONArray("trades")
                    db.tradeDao().deleteAllTrades()
                    for (i in 0 until list.length()) {
                        val obj = list.getJSONObject(i)
                        val trade = Trade(
                            id = obj.optInt("id", 0),
                            side = obj.getString("side"),
                            market = obj.getString("market"),
                            volume = obj.getDouble("volume"),
                            entryPrice = obj.getDouble("entryPrice"),
                            exitPrice = if (obj.isNull("exitPrice")) null else obj.getDouble("exitPrice"),
                            dateTime = obj.getLong("dateTime"),
                            fees = obj.optDouble("fees", 0.0),
                            reason = obj.optString("reason", ""),
                            imagePath = if (obj.isNull("imagePath")) null else obj.optString("imagePath"),
                            imageBeforePath = if (obj.isNull("imageBeforePath")) null else obj.optString("imageBeforePath"),
                            imageEntryPath = if (obj.isNull("imageEntryPath")) null else obj.optString("imageEntryPath"),
                            imageExitPath = if (obj.isNull("imageExitPath")) null else obj.optString("imageExitPath"),
                            tags = obj.optString("tags", ""),
                            strategy = obj.optString("strategy", ""),
                            grade = obj.optString("grade", ""),
                            checklistResults = obj.optString("checklistResults", ""),
                            postTradeNotes = obj.optString("postTradeNotes", ""),
                            richNotes = obj.optString("richNotes", ""),
                            emotionalState = obj.optString("emotionalState", "")
                        )
                        db.tradeDao().insertTrade(trade)
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import JSON backup", e)
            false
        }
    }

    /**
     * Helper to clean strings and wrap them in quotes if commas/quotes are present.
     */
    private fun escapeCsv(value: String?): String {
        if (value == null) return ""
        val cleaned = value.replace("\"", "\"\"")
        return if (cleaned.contains(",") || cleaned.contains("\n") || cleaned.contains("\"")) {
            "\"$cleaned\""
        } else {
            cleaned
        }
    }
}
