package com.frabon.rememberthedate.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: Event)

    @Update
    suspend fun update(event: Event)

    @Delete
    suspend fun delete(event: Event)

    @Query("SELECT * FROM events ORDER BY month, day ASC")
    fun getAllOrderedByMonthAndDay(): Flow<List<Event>>

    @Query("SELECT * from events WHERE id = :id")
    fun getEventById(id: Int): Flow<Event>

    @Query("SELECT * FROM events WHERE name LIKE :searchQuery ORDER BY month, day ASC")
    fun searchDatabase(searchQuery: String): Flow<List<Event>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<Event>)

    @Query("SELECT * FROM events ORDER BY month, day ASC")
    fun getAllForWidget(): List<Event>

}