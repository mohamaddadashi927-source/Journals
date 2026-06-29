package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Market
import com.example.data.model.Tag
import com.example.data.model.Trade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Trade::class,
        Market::class,
        Tag::class,
        com.example.data.model.DailyJournal::class,
        com.example.data.model.ChecklistItem::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tradeDao(): TradeDao
    abstract fun marketDao(): MarketDao
    abstract fun tagDao(): TagDao
    abstract fun dailyJournalDao(): DailyJournalDao
    abstract fun checklistItemDao(): ChecklistItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "trading_journal_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val marketDao = database.marketDao()
                    val tagDao = database.tagDao()
                    
                    val defaultMarkets = listOf(
                        Market(name = "BTC/USDT"),
                        Market(name = "ETH/USDT"),
                        Market(name = "EUR/USD"),
                        Market(name = "GBP/USD"),
                        Market(name = "GOLD/USD")
                    )
                    marketDao.insertMarkets(defaultMarkets)

                    val defaultTags = listOf(
                        Tag(name = "Breakout"),
                        Tag(name = "Trend Following"),
                        Tag(name = "Scalping"),
                        Tag(name = "Support/Resistance"),
                        Tag(name = "Pullback")
                    )
                    tagDao.insertTags(defaultTags)

                    val checklistItemDao = database.checklistItemDao()
                    val defaultChecklists = listOf(
                        com.example.data.model.ChecklistItem(title = "آیا حد ضرر (Stop Loss) مشخص شده است؟"),
                        com.example.data.model.ChecklistItem(title = "آیا حد سود (Take Profit) مشخص شده است؟"),
                        com.example.data.model.ChecklistItem(title = "آیا میزان ریسک معامله بر اساس مدیریت سرمایه است؟"),
                        com.example.data.model.ChecklistItem(title = "آیا معامله با استراتژی اصلی من همخوانی کامل دارد؟"),
                        com.example.data.model.ChecklistItem(title = "آیا احساس هیجان، طمع یا انتقام در تصمیم من دخیل نیست؟")
                    )
                    checklistItemDao.insertChecklistItems(defaultChecklists)
                }
            }
        }
    }
}
