package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SignalDao {
    @Query("SELECT * FROM signals ORDER BY id DESC")
    fun getAllSignals(): Flow<List<SignalEntity>>

    @Query("SELECT * FROM signals WHERE signal = :type ORDER BY id DESC")
    fun getSignalsByType(type: String): Flow<List<SignalEntity>>

    @Query("SELECT * FROM signals ORDER BY id DESC LIMIT 1")
    suspend fun getLatestSignal(): SignalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignal(signal: SignalEntity): Long

    @Query("DELETE FROM signals")
    suspend fun clearAllSignals()

    @Query("DELETE FROM signals WHERE id NOT IN (SELECT id FROM signals ORDER BY id DESC LIMIT 200)")
    suspend fun trimOldSignals()
}
