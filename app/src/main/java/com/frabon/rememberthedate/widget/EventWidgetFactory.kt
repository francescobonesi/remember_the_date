package com.frabon.rememberthedate.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.frabon.rememberthedate.R
import com.frabon.rememberthedate.data.AppDatabase
import com.frabon.rememberthedate.data.Event
import com.frabon.rememberthedate.data.EventType
import java.util.Calendar

class EventWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var events: List<Event> = emptyList()
    private val eventDao = AppDatabase.getDatabase(context).eventDao()

    override fun onCreate() {
        onDataSetChanged()
    }

    override fun onDataSetChanged() {
        val allEvents = eventDao.getAllForWidget()
        events = getTodayEvents(allEvents)
    }

    override fun onDestroy() {
        events = emptyList()
    }

    override fun getCount(): Int = events.size

    override fun getViewAt(position: Int): RemoteViews {
        val event = events[position]
        val views = RemoteViews(context.packageName, R.layout.widget_item)

        val dayFormatted = event.day.toString().padStart(2, '0')
        val monthFormatted = event.month.toString().padStart(2, '0')
        views.setTextViewText(R.id.widget_item_date, "$dayFormatted/$monthFormatted")

        val emoji = when (event.type) {
            EventType.BIRTHDAY -> "🎂"
            EventType.ANNIVERSARY -> "❤️"
            EventType.HOLIDAY -> "🎉"
        }
        views.setTextViewText(R.id.widget_item_name, "$emoji ${event.name}")

        val intent = Intent()
        views.setOnClickFillInIntent(R.id.widget_item_root, intent)
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = events[position].id.toLong()

    override fun hasStableIds(): Boolean = true

    private fun getTodayEvents(allEvents: List<Event>): List<Event> {
        val today = Calendar.getInstance()
        val currentMonth = today.get(Calendar.MONTH) + 1
        val currentDay = today.get(Calendar.DAY_OF_MONTH)

        return allEvents.filter { it.month == currentMonth && it.day == currentDay }
    }
}