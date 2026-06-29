package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trades")
data class Trade(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val side: String, // "BUY" or "SELL"
    val market: String, // e.g. "BTC/USDT", "EUR/USD"
    val volume: Double,
    val entryPrice: Double,
    val exitPrice: Double?, // Nullable for open trades
    val dateTime: Long, // timestamp
    val fees: Double = 0.0,
    val reason: String,
    val imagePath: String? = null, // local file path or content URI (legacy)
    val imageBeforePath: String? = null, // image before entering trade
    val imageEntryPath: String? = null, // image at entry
    val imageExitPath: String? = null, // image at exit
    val tags: String, // Comma-separated list of tags
    val strategy: String = "", // strategy classification
    val grade: String = "", // "A+", "A", "B", "C", "F"
    val checklistResults: String = "", // Comma/Semicolon separated list of "Item:Checked" status
    val postTradeNotes: String = "", // Notes added after trade is closed
    val richNotes: String = "", // Detailed rich notes and entry reasons
    val emotionalState: String = "", // Emotional state during the trade
    val customPnl: Double? = null, // Manually registered profit/loss
    val accountId: String = "acc_default" // Associated account identifier
) {
    val pnl: Double?
        get() = if (exitPrice != null) {
            customPnl ?: 0.0
        } else {
            null
        }

    val status: String
        get() = when {
            exitPrice == null -> "OPEN"
            (pnl ?: 0.0) >= 0.0 -> "WIN"
            else -> "LOSS"
        }
}

@Entity(tableName = "markets")
data class Market(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

@Entity(tableName = "daily_journals")
data class DailyJournal(
    @PrimaryKey val dateString: String, // format "YYYY-MM-DD"
    val content: String = "",
    val emotions: String = "",
    val mistakes: String = "",
    val lessons: String = ""
)

@Entity(tableName = "checklist_items")
data class ChecklistItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val isEnabled: Boolean = true
)
