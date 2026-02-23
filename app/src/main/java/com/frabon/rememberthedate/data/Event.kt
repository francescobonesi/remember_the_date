package com.frabon.rememberthedate.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val day: Int,
    val month: Int,
    val type: EventType,
    val year: Int? = null
)