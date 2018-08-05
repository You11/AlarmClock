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

    fun setupAlarm(alarm: Alarm, disposable: CompositeDisposable, viewModel: AlarmViewModel, activity: AppCompatActivity) {
        val thread = Schedulers.single()

        //get all alarms
        disposable.add(viewModel.getAlarmList()
                .subscribeOn(thread)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val alarmCount = it.count()

                    disposable.add(viewModel.updateAlarm(alarm)
                            .subscribeOn(thread)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({

                                setupAlarm(alarm, alarmCount, activity)
                            }, { error -> Log.e("Error", "Unable to update alarm", error) }))
                })
    }

    private fun setupAlarm(alarm: Alarm, alarmCount: Int, activity: AppCompatActivity) {
        val alarmManager = activity.getSystemService(android.content.Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = PendingIntent.getBroadcast(activity, alarmCount + 200, Intent(activity, AlarmReceiver::class.java), 0)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, alarm.hours)
        calendar.set(Calendar.MINUTE, alarm.minutes)
        calendar.set(Calendar.SECOND, 0)
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, alarmIntent)
    }
}