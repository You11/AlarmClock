package ru.you11.alarmclock

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*

class Utils {

    private val ALARM_NOTIFICATION_ID = 100

    fun createAlarmInDatabase(alarm: Alarm, disposable: CompositeDisposable, viewModel: AlarmViewModel) {
        disposable.add(viewModel.getAlarmList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (alarm.aid == null) {
                        alarm.aid = it.count()
                    }
                    disposable.add(viewModel.updateAlarm(alarm)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe())
                })
    }

    fun setupAlarm(alarm: Alarm, activity: AppCompatActivity) {

        val alarmIntent = setupAlarmIntent(alarm, activity)

        val alarmManager = activity.getSystemService(android.content.Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, alarm.hours)
        calendar.set(Calendar.MINUTE, alarm.minutes)
        calendar.set(Calendar.SECOND, 0)
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, alarmIntent)
    }

    fun stopAlarm(id: Int, alarmManager: AlarmManager, context: Context) {
        val alarmIntent = PendingIntent.getBroadcast(context, id, Intent(context, AlarmReceiver::class.java), 0)
        alarmManager.cancel(alarmIntent)
    }

    fun createNotification(alarm: Alarm, activity: AppCompatActivity) {
        val notification = NotificationCompat.Builder(activity, "100")
        notification.setSmallIcon(R.drawable.baseline_alarm_white_18)
        notification.setContentTitle("Alarm")
        notification.setContentText(Utils().getAlarmTime(alarm.hours, alarm.minutes))
        notification.priority = NotificationCompat.PRIORITY_DEFAULT
        notification.setOngoing(true)
        val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT > 26) {
            //TODO: NAME???????
            val notificationChannel = NotificationChannel("100", "name?????", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(notificationChannel)
        }
        notificationManager.notify(ALARM_NOTIFICATION_ID, notification.build())
    }

    fun dismissNotification(activity: AppCompatActivity) {
        val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(ALARM_NOTIFICATION_ID)
    }

    fun getAlarmTime(hours: Int, minutes: Int): String {
        return if (minutes < 10) {
            hours.toString() + ":0" + minutes.toString()
        } else {
            hours.toString() + ":" + minutes.toString()
        }
    }

    private fun setupAlarmIntent(alarm: Alarm, activity: AppCompatActivity): PendingIntent {
        val intent = Intent(activity, AlarmReceiver::class.java)
        intent.putExtra("alarmId", alarm.aid)
        return PendingIntent.getBroadcast(activity, alarm.aid!!, intent, 0)
    }
}