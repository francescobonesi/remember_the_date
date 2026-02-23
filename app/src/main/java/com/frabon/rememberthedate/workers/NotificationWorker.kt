package com.frabon.rememberthedate.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.frabon.rememberthedate.R
import com.frabon.rememberthedate.RememberTheDateApplication
import com.frabon.rememberthedate.data.Event
import com.frabon.rememberthedate.data.EventType
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as RememberTheDateApplication).repository
        val today = LocalDate.now()
        val eventsToday = repository.allEvents.first().filter {
            it.day == today.dayOfMonth && today.monthValue == it.month
        }

        val oneWeekFromNow = LocalDate.now().plusDays(7)
        val eventsOneWeekFromNow = repository.allEvents.first().filter {
            it.day == oneWeekFromNow.dayOfMonth && oneWeekFromNow.monthValue == it.month
        }

        if (eventsToday.isNotEmpty() || eventsOneWeekFromNow.isNotEmpty()) {
            sendNotification(eventsToday, eventsOneWeekFromNow)
        }

        return Result.success()
    }

    private fun sendNotification(eventsToday: List<Event>, eventsOneWeekFromNow: List<Event>) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Daily Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        val contentText = buildNotificationString(eventsToday, eventsOneWeekFromNow)

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(applicationContext.getString(R.string.notification_title))
            .setContentText(
                applicationContext.getString(
                    R.string.notification_summary,
                    eventsToday.size + eventsOneWeekFromNow.size
                )
            )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .build()

        notificationManager.notify(1, notification)
    }

    private fun buildNotificationString(eventsToday: List<Event>, eventsOneWeekFromNow: List<Event>): String {
        val stringBuilder = StringBuilder()

        if(eventsToday.isNotEmpty()){
            val groupedEvents = eventsToday.groupBy { it.type }

            groupedEvents[EventType.BIRTHDAY]?.let {
                val names = it.joinToString(", ") { event -> event.name }
                stringBuilder.append(applicationContext.getString(R.string.notification_birthdays_header) + names + "\n")
            }

            groupedEvents[EventType.ANNIVERSARY]?.let {
                val names = it.joinToString(", ") { event -> event.name }
                stringBuilder.append(applicationContext.getString(R.string.notification_anniversaries_header) + names + "\n")
            }

            groupedEvents[EventType.HOLIDAY]?.let {
                val names = it.joinToString(", ") { event -> event.name }
                stringBuilder.append(applicationContext.getString(R.string.notification_holidays_header) + names + "\n")
            }
        }

        if(eventsOneWeekFromNow.isNotEmpty()){
            val groupedEventsInAWeek = eventsOneWeekFromNow.groupBy { it.type }

            groupedEventsInAWeek[EventType.BIRTHDAY]?.let {
                val names = it.joinToString(", ") { event -> event.name }
                stringBuilder.append(applicationContext.getString(R.string.notification_birthdays_in_a_week_header) + names + "\n")
            }

            groupedEventsInAWeek[EventType.ANNIVERSARY]?.let {
                val names = it.joinToString(", ") { event -> event.name }
                stringBuilder.append(applicationContext.getString(R.string.notification_anniversaries_in_a_week_header) + names + "\n")
            }

            groupedEventsInAWeek[EventType.HOLIDAY]?.let {
                val names = it.joinToString(", ") { event -> event.name }
                stringBuilder.append(applicationContext.getString(R.string.notification_holidays_in_a_week_header) + names + "\n")
            }
        }

        return stringBuilder.toString().trim()
    }

    companion object {
        const val CHANNEL_ID = "remember_the_date_channel"
    }
}