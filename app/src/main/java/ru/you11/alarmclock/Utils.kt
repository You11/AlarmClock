package ru.you11.alarmclock

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.util.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.text.DateFormat
import java.util.*

object Utils {

    private const val ALARM_NOTIFICATION_ID = 100
    private const val NOTIFICATION_REQUEST_CODE = 101

    fun createAlarmInDatabase(alarm: Alarm, disposable: CompositeDisposable, viewModel: AlarmViewModel) {
        disposable.add(viewModel.updateAlarm(alarm)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe())
    }

    //triggers when user presses delay button
    fun setDelayedAlarm(alarm: Alarm, context: Context) {

        val alarmIntent = setupAlarmIntent(alarm, context)

        val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()

        calendar.set(Calendar.HOUR_OF_DAY, alarm.hours)
        calendar.set(Calendar.MINUTE, alarm.minutes)
        calendar.set(Calendar.SECOND, 0)

        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, alarmIntent)
    }

    fun setAlarmWithDays(alarm: Alarm, context: Context) {
        val alarmIntent = setupAlarmIntent(alarm, context)
        val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as AlarmManager
        val currentDate = Calendar.getInstance()
        var earliestAlarm: Calendar = Calendar.getInstance()
        earliestAlarm.timeInMillis = Long.MAX_VALUE

        alarm.days.forEach {
            if (it.value) {
                val alarmDate = Calendar.getInstance()
                alarmDate.set(Calendar.HOUR_OF_DAY, alarm.hours)
                alarmDate.set(Calendar.MINUTE, alarm.minutes)
                alarmDate.set(Calendar.SECOND, 0)
                alarmDate.set(Calendar.DAY_OF_WEEK, alarm.daysStringToCalendar[it.key]!!)
                if (alarmDate.before(currentDate)) {
                    alarmDate.add(Calendar.WEEK_OF_MONTH, 1)
                }

                if (alarmDate.before(earliestAlarm)) {
                    earliestAlarm = alarmDate
                }
            }
        }

        if (earliestAlarm.timeInMillis == Long.MAX_VALUE) {
            throw Exception("days weren't selected")
        }

        Log.d("alarmTime", DateFormat.getTimeInstance(DateFormat.FULL).format(earliestAlarm.time))
        Log.d("alarmDate", DateFormat.getDateInstance(DateFormat.FULL).format(earliestAlarm.time))

        alarmManager.set(AlarmManager.RTC_WAKEUP, earliestAlarm.timeInMillis, alarmIntent)
    }

    //stops alarms with given id
    fun stopAlarm(id: Int, alarmManager: AlarmManager, context: Context) {
        val alarmIntent = PendingIntent.getBroadcast(context, id, Intent(context, AlarmReceiver::class.java), 0)
        alarmManager.cancel(alarmIntent)
    }

    //creates notification for alarm with time and on click event
    fun updateAlarmNotification(allAlarms: List<Alarm>, context: Context) {

        val alarm = getEarliestAlarm(allAlarms)
        if (alarm == null) {
            dismissAlarmNotification(context)
            return
        }

        val notification = setupNotification(context, alarm)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT > 26) {
            val notificationChannel = NotificationChannel("100", "Alarm", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(notificationChannel)
        }
        notificationManager.notify(ALARM_NOTIFICATION_ID, notification.build())
    }

    private fun dismissAlarmNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(ALARM_NOTIFICATION_ID)
    }

    //returns time string for recycler view and notification
    fun getAlarmTime(hours: Int, minutes: Int): String {

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hours)
        calendar.set(Calendar.MINUTE, minutes)

        return DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.time)
    }

    private fun getEarliestAlarm(allAlarms: List<Alarm>): Alarm? {

        //todo: refactoring
        var earliestDate: Calendar = Calendar.getInstance()
        earliestDate.timeInMillis = Long.MAX_VALUE
        var earliestAlarm: Alarm? = null

        for (alarm in allAlarms) {
            if (alarm.isOn) {
                alarm.days.forEach {
                    if (it.value) {
                        val alarmDate = Calendar.getInstance()
                        alarmDate.set(Calendar.HOUR_OF_DAY, alarm.hours)
                        alarmDate.set(Calendar.MINUTE, alarm.minutes)
                        alarmDate.set(Calendar.SECOND, 0)
                        alarmDate.set(Calendar.DAY_OF_WEEK, alarm.daysStringToCalendar[it.key]!!)
                        if (alarmDate.before(Calendar.getInstance())) {
                            alarmDate.add(Calendar.WEEK_OF_MONTH, 1)
                        }

                        if (alarmDate.before(earliestDate)) {
                            earliestDate = alarmDate
                            earliestAlarm = alarm
                        }
                    }
                }
            }
        }

        if (earliestAlarm != null) {
            val alarmDay = earliestDate[Calendar.DAY_OF_WEEK]
            for (day in earliestAlarm!!.days) {
                if (earliestAlarm!!.daysStringToCalendar[day.key] == alarmDay) {
                    day.setValue(true)
                } else {
                    day.setValue(false)
                }
            }
        }

        return earliestAlarm
    }

    private fun setupAlarmIntent(alarm: Alarm, context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
        intent.putExtra("alarmId", alarm.aid)
        return PendingIntent.getBroadcast(context, alarm.aid!!, intent, 0)
    }

    private fun setupNotification(context: Context, alarm: Alarm): NotificationCompat.Builder {
        val notification = NotificationCompat.Builder(context, "100")
        notification.setSmallIcon(R.drawable.baseline_alarm_white_18)
        if (alarm.name.isBlank()) {
            notification.setContentTitle("Alarm")
        } else {
            notification.setContentTitle(alarm.name)
        }

        var day = ""

        alarm.days.forEach {
            if (it.value) {
                day = it.key
            }
        }

        notification.setContentText(day + ", " + Utils.getAlarmTime(alarm.hours, alarm.minutes))
        notification.priority = NotificationCompat.PRIORITY_DEFAULT
        notification.setOngoing(true)

        setupNotificationIntent(context, notification)

        return notification
    }

    private fun setupNotificationIntent(context: Context, notification: NotificationCompat.Builder) {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(context, NOTIFICATION_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        notification.setContentIntent(pendingIntent)
    }
}
