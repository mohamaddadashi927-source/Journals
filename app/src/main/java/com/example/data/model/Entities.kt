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
    val fees: Double,
    val reason: String,
    val imagePath: String?, // local file path or content URI
    val tags: String, // Comma-separated list of tags
    val postTradeNotes: String = "", // Notes added after trade is closed
    val richNotes: String = "", // Detailed rich notes and entry reasons
    val emotionalState: String = "", // Emotional state during the trade (e.g. CALM, ANXIOUS, etc.)
    val customPnl: Double? = null // Manually registered profit/loss
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
