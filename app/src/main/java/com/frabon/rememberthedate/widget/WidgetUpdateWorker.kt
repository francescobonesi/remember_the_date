package com.frabon.rememberthedate.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.frabon.rememberthedate.R

class WidgetUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        return try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, EventWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}