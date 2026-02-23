package com.frabon.rememberthedate.data

import kotlinx.coroutines.flow.Flow

class EventRepository(private val eventDao: EventDao) {

    val allEvents: Flow<List<Event>> = eventDao.getAllOrderedByMonthAndDay()

    suspend fun insertEvent(event: Event) {
        eventDao.insert(event)
    }

    suspend fun updateEvent(event: Event) {
        eventDao.update(event)
    }

    suspend fun deleteEvent(event: Event) {
        eventDao.delete(event)
    }

    fun getEventById(id: Int): Flow<Event> {
        return eventDao.getEventById(id)
    }

    suspend fun insertAllEvents(events: List<Event>) {
        eventDao.insertAll(events)
    }

}