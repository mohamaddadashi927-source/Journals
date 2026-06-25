package com.example.data.local

import androidx.room.*
import com.example.data.model.Market
import com.example.data.model.Tag
import com.example.data.model.Trade
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {
    @Query("SELECT * FROM trades ORDER BY dateTime DESC")
    fun getAllTrades(): Flow<List<Trade>>

    @Query("SELECT * FROM trades WHERE id = :id")
    fun getTradeById(id: Int): Flow<Trade?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: Trade): Long

    @Update
    suspend fun updateTrade(trade: Trade)

    @Delete
    suspend fun deleteTrade(trade: Trade)

    @Query("DELETE FROM trades")
    suspend fun deleteAllTrades()
}

@Dao
interface MarketDao {
    @Query("SELECT * FROM markets ORDER BY name ASC")
    fun getAllMarkets(): Flow<List<Market>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMarket(market: Market): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMarkets(markets: List<Market>)

    @Delete
    suspend fun deleteMarket(market: Market)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: Tag): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTags(tags: List<Tag>)

    @Delete
    suspend fun deleteTag(tag: Tag)
}
