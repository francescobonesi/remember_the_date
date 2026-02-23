package com.frabon.rememberthedate.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.frabon.rememberthedate.data.Event
import com.frabon.rememberthedate.data.EventRepository
import com.frabon.rememberthedate.data.EventType
import com.frabon.rememberthedate.ui.UiItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.time.Month
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

enum class ViewStyle {
    ALL_MONTHS, SINGLE_MONTH
}

class MainViewModel(private val repository: EventRepository) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    private val _viewStyle = MutableStateFlow(ViewStyle.ALL_MONTHS)
    val viewStyle: StateFlow<ViewStyle> = _viewStyle.asStateFlow()

    private val _currentMonth =
        MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1) // 1-12
    val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()

    val currentMonthName = currentMonth.map {
        Month.of(it).getDisplayName(TextStyle.FULL, Locale.getDefault())
    }.asLiveData()

    private val _typeFilter = MutableStateFlow<EventType?>(null) // null means "All"

    private val eventsFlow = combine(
        repository.allEvents,
        searchQuery,
        _viewStyle,
        _currentMonth,
        _typeFilter
    ) { events, query, style, month, type ->

        var filteredList = events

        if (query.isNotEmpty()) {
            filteredList = filteredList.filter { it.name.contains(query, ignoreCase = true) }
        }

        if (style == ViewStyle.SINGLE_MONTH) {
            filteredList = filteredList.filter { it.month == month }
        }

        if (type != null) {
            filteredList = filteredList.filter { it.type == type }
        }

        filteredList

    }

    val groupedEvents = eventsFlow.asLiveData().map { events ->
        groupEventsByMonth(events)
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setViewStyle(style: ViewStyle) {
        _viewStyle.value = style
    }

    fun nextMonth() {
        val current = _currentMonth.value
        _currentMonth.value = if (current == 12) 1 else current + 1
    }

    fun previousMonth() {
        val current = _currentMonth.value
        _currentMonth.value = if (current == 1) 12 else current - 1
    }

    fun setTypeFilter(type: EventType?) {
        _typeFilter.value = type
    }

    private fun groupEventsByMonth(events: List<Event>): List<UiItem> {
        val items = mutableListOf<UiItem>()
        if (events.isEmpty()) return items

        val groupedByMonth = events.groupBy { it.month }

        groupedByMonth.keys.sorted().forEach { monthNumber ->
            items.add(
                UiItem.Header(
                    Month.of(monthNumber).getDisplayName(TextStyle.FULL, Locale.getDefault())
                )
            )
            groupedByMonth[monthNumber]?.forEach { event ->
                items.add(UiItem.EventItem(event))
            }
        }
        return items
    }

    suspend fun exportEventsToCsv(outputStream: OutputStream) {
        val events = repository.allEvents.first()
        val writer = outputStream.bufferedWriter()
        writer.write("name,day,month,type,year\n")
        events.forEach { event ->
            writer.write("${event.name},${event.day},${event.month},${event.type},${event.year ?: ""}\n")
        }
        writer.flush()
    }

    fun importEventsFromCsv(inputStream: InputStream) = viewModelScope.launch {
        val newEvents = mutableListOf<Event>()
        inputStream.bufferedReader().useLines { lines ->
            lines.drop(1)
                .forEach { line ->
                    try {
                        val tokens = line.split(",")
                        val event = Event(
                            name = tokens[0],
                            day = tokens[1].toInt(),
                            month = tokens[2].toInt(),
                            type = EventType.valueOf(tokens[3]),
                            year = tokens[4].toIntOrNull()
                        )
                        newEvents.add(event)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
        }
        if (newEvents.isNotEmpty()) {
            repository.insertAllEvents(newEvents)
        }
    }
}