package com.frabon.rememberthedate.viewmodels

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frabon.rememberthedate.R
import com.frabon.rememberthedate.data.Event
import com.frabon.rememberthedate.data.EventRepository
import com.frabon.rememberthedate.widget.EventWidgetProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AddEditViewModel(
    private val repository: EventRepository,
    private val context: Context
) : ViewModel() {

    fun getEventById(id: Int): Flow<Event> = repository.getEventById(id)

    fun insertEvent(event: Event): Job = viewModelScope.launch {
        repository.insertEvent(event)
        updateWidget()
    }

    fun updateEvent(event: Event): Job = viewModelScope.launch {
        repository.updateEvent(event)
        updateWidget()
    }

    fun deleteEvent(event: Event): Job = viewModelScope.launch {
        repository.deleteEvent(event)
        updateWidget()
    }

    private fun updateWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, EventWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view)
    }

}