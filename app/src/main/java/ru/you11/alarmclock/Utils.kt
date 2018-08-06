package ru.you11.alarmclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.util.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*

class Utils {

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