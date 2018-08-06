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
    private val NOTIFICATION_REQUEST_CODE = 101

    fun createAlarmInDatabase(alarm: Alarm, disposable: CompositeDisposable, viewModel: AlarmViewModel) {
        disposable.add(viewModel.updateAlarm(alarm)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe())
    }

    //sets repeating alarm which goes off each day
    fun setAlarm(alarm: Alarm, activity: AppCompatActivity) {

        val alarmIntent = setupAlarmIntent(alarm, activity)

        val alarmManager = activity.getSystemService(android.content.Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, alarm.hours)
        calendar.set(Calendar.MINUTE, alarm.minutes)
        calendar.set(Calendar.SECOND, 0)

        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, alarmIntent)
    }

    //stops alarms with given id
    fun stopAlarm(id: Int, alarmManager: AlarmManager, context: Context) {
        val alarmIntent = PendingIntent.getBroadcast(context, id, Intent(context, AlarmReceiver::class.java), 0)
        alarmManager.cancel(alarmIntent)
    }

    //creates notification for alarm with time and on click event
    fun createAlarmNotification(alarm: Alarm, activity: AppCompatActivity) {
        val notification = NotificationCompat.Builder(activity, "100")
        notification.setSmallIcon(R.drawable.baseline_alarm_white_18)
        notification.setContentTitle("Alarm")
        notification.setContentText(Utils().getAlarmTime(alarm.hours, alarm.minutes))
        notification.priority = NotificationCompat.PRIORITY_DEFAULT
        notification.setOngoing(true)

        val intent = Intent(activity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(activity, NOTIFICATION_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        notification.setContentIntent(pendingIntent)

        val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT > 26) {
            val notificationChannel = NotificationChannel("100", "Alarm", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(notificationChannel)
        }
        notificationManager.notify(ALARM_NOTIFICATION_ID, notification.build())
    }

    fun dismissAlarmNotification(activity: AppCompatActivity) {
        val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(ALARM_NOTIFICATION_ID)
    }

    //returns time string for recycler view and notification
    fun getAlarmTime(hours: Int, minutes: Int): String {
        return if (minutes < 10) {
            //xx:0x
            hours.toString() + ":0" + minutes.toString()
        } else {
            //xx:xx
            hours.toString() + ":" + minutes.toString()
        }
    }

    private fun setupAlarmIntent(alarm: Alarm, activity: AppCompatActivity): PendingIntent {
        val intent = Intent(activity, AlarmReceiver::class.java)
        intent.putExtra("alarmId", alarm.aid)
        return PendingIntent.getBroadcast(activity, alarm.aid!!, intent, 0)
    }
}