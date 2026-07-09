package com.example.data.repository

import com.example.data.local.ChecklistItemDao
import com.example.data.local.DailyJournalDao
import com.example.data.local.MarketDao
import com.example.data.local.TagDao
import com.example.data.local.TradeDao
import com.example.data.model.ChecklistItem
import com.example.data.model.DailyJournal
import com.example.data.model.Market
import com.example.data.model.Tag
import com.example.data.model.Trade
import kotlinx.coroutines.flow.Flow

class JournalRepository(
    private val tradeDao: TradeDao,
    private val marketDao: MarketDao,
    private val tagDao: TagDao,
    private val dailyJournalDao: DailyJournalDao,
    private val checklistItemDao: ChecklistItemDao
) {
    // Trades
    val allTrades: Flow<List<Trade>> = tradeDao.getAllTrades()

    fun getTradeById(id: Int): Flow<Trade?> = tradeDao.getTradeById(id)

    suspend fun insertTrade(trade: Trade): Long = tradeDao.insertTrade(trade)

    suspend fun updateTrade(trade: Trade) = tradeDao.updateTrade(trade)

    suspend fun deleteTrade(trade: Trade) = tradeDao.deleteTrade(trade)

    suspend fun deleteAllTrades() = tradeDao.deleteAllTrades()

    // Markets
    val allMarkets: Flow<List<Market>> = marketDao.getAllMarkets()

    suspend fun insertMarket(market: Market): Long = marketDao.insertMarket(market)

    suspend fun insertMarkets(markets: List<Market>) = marketDao.insertMarkets(markets)

    suspend fun deleteMarket(market: Market) = marketDao.deleteMarket(market)

    suspend fun deleteAllMarkets() = marketDao.deleteAllMarkets()

    // Tags
    val allTags: Flow<List<Tag>> = tagDao.getAllTags()

    suspend fun insertTag(tag: Tag): Long = tagDao.insertTag(tag)

    suspend fun insertTags(tags: List<Tag>) = tagDao.insertTags(tags)

    suspend fun deleteTag(tag: Tag) = tagDao.deleteTag(tag)

    suspend fun deleteAllTags() = tagDao.deleteAllTags()

    // Daily Journals
    val allDailyJournals: Flow<List<DailyJournal>> = dailyJournalDao.getAllDailyJournals()

    fun getDailyJournalByDate(dateString: String): Flow<DailyJournal?> = dailyJournalDao.getDailyJournalByDate(dateString)

    suspend fun insertDailyJournal(journal: DailyJournal) = dailyJournalDao.insertDailyJournal(journal)

    suspend fun deleteDailyJournal(journal: DailyJournal) = dailyJournalDao.deleteDailyJournal(journal)

    suspend fun deleteAllDailyJournals() = dailyJournalDao.deleteAllDailyJournals()

    // Checklist Items
    val allChecklistItems: Flow<List<ChecklistItem>> = checklistItemDao.getAllChecklistItems()
    val enabledChecklistItems: Flow<List<ChecklistItem>> = checklistItemDao.getEnabledChecklistItems()

    suspend fun insertChecklistItem(item: ChecklistItem): Long = checklistItemDao.insertChecklistItem(item)

    suspend fun insertChecklistItems(items: List<ChecklistItem>) = checklistItemDao.insertChecklistItems(items)

    suspend fun deleteChecklistItem(item: ChecklistItem) = checklistItemDao.deleteChecklistItem(item)

    suspend fun deleteAllChecklistItems() = checklistItemDao.deleteAllChecklistItems()
}
