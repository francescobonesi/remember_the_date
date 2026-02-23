package com.frabon.rememberthedate

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.frabon.rememberthedate.data.AppDatabase
import com.frabon.rememberthedate.data.EventRepository
import com.frabon.rememberthedate.widget.WidgetUpdateWorker
import com.frabon.rememberthedate.workers.NotificationWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

class RememberTheDateApplication : Application() {

    private val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { EventRepository(database.eventDao()) }

    override fun onCreate() {
        super.onCreate()
        setupDailyNotification()
        setupHourlyWidgetUpdate()
    }

    private fun setupHourlyWidgetUpdate() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val currentTime = Calendar.getInstance()
        val dueTime = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 5)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val initialDelay = dueTime.timeInMillis - currentTime.timeInMillis

        val repeatingRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(1, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "hourly_widget_update_work",
            ExistingPeriodicWorkPolicy.KEEP,
            repeatingRequest
        )
    }

    private fun setupDailyNotification() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val currentTime = Calendar.getInstance()
        val dueTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (currentTime.after(dueTime)) {
            dueTime.add(Calendar.DAY_OF_YEAR, 1)
        }

        val initialDelay = dueTime.timeInMillis - currentTime.timeInMillis

        val dailyNotificationRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "daily_notification_work",
            ExistingWorkPolicy.REPLACE,
            dailyNotificationRequest
        )
    }
}