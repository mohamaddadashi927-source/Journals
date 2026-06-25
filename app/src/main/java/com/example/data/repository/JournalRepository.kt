package com.example.data.repository

import com.example.data.local.MarketDao
import com.example.data.local.TagDao
import com.example.data.local.TradeDao
import com.example.data.model.Market
import com.example.data.model.Tag
import com.example.data.model.Trade
import kotlinx.coroutines.flow.Flow

class JournalRepository(
    private val tradeDao: TradeDao,
    private val marketDao: MarketDao,
    private val tagDao: TagDao
) {
    val allTrades: Flow<List<Trade>> = tradeDao.getAllTrades()
    val allMarkets: Flow<List<Market>> = marketDao.getAllMarkets()
    val allTags: Flow<List<Tag>> = tagDao.getAllTags()

    fun getTradeById(id: Int): Flow<Trade?> = tradeDao.getTradeById(id)

    suspend fun insertTrade(trade: Trade): Long = tradeDao.insertTrade(trade)

    suspend fun updateTrade(trade: Trade) = tradeDao.updateTrade(trade)

    suspend fun deleteTrade(trade: Trade) = tradeDao.deleteTrade(trade)

    suspend fun deleteAllTrades() = tradeDao.deleteAllTrades()

    suspend fun insertMarket(market: Market): Long = marketDao.insertMarket(market)

    suspend fun insertMarkets(markets: List<Market>) = marketDao.insertMarkets(markets)

    suspend fun deleteMarket(market: Market) = marketDao.deleteMarket(market)

    suspend fun insertTag(tag: Tag): Long = tagDao.insertTag(tag)

    suspend fun insertTags(tags: List<Tag>) = tagDao.insertTags(tags)

    suspend fun deleteTag(tag: Tag) = tagDao.deleteTag(tag)
}
