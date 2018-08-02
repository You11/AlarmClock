package ru.you11.alarmclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TimePicker
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*

class AlarmSetupFragment: Fragment() {

    private lateinit var activity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = this.getActivity() as MainActivity

        return inflater.inflate(R.layout.fragment_alarm_setup, container, false)
    }

    override fun onResume() {
        super.onResume()

        view?.findViewById<Button>(R.id.alarm_setup_save_button)?.apply {
            setOnClickListener {
                updateAlarms()
            }
        }
    }

    private fun updateAlarms() {

        val alarmName = view?.findViewById<EditText>(R.id.alarm_name_setup)?.apply {
            isEnabled = false

        }
        val alarmTime = view?.findViewById<TimePicker>(R.id.alarm_time_setup)?.apply {
            isEnabled = false
        }
        val saveButton = view?.findViewById<Button>(R.id.alarm_setup_save_button)

        var alarmCount: Int

        val thread = Schedulers.single()

        //TODO: how the hell do i thread
        activity.disposable.add(activity.viewModel.getAlarmList()
                .subscribeOn(thread)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    alarmCount = it.count()

                    val selectedHour: Int
                    val selectedMinute: Int

                    if (alarmTime != null) {
                        if (Build.VERSION.SDK_INT >= 23) {
                            selectedHour = alarmTime.hour
                            selectedMinute = alarmTime.minute
                        }
                        else {
                            selectedHour = alarmTime.currentHour
                            selectedMinute = alarmTime.currentMinute
                        }
                    } else {
                        return@subscribe
                    }

                    val alarm = Alarm(alarmCount, alarmName?.text.toString(), selectedHour, selectedMinute)

                    activity.disposable.add(activity.viewModel.updateAlarm(alarm)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({
                                saveButton?.isEnabled = true
                                alarmTime.isEnabled = true

                                setAlarm(alarm)
                            }, { error -> Log.e("Error", "Unable to update username", error) }))
                })
    }

    private fun setAlarm(alarm: Alarm) {
        val alarmManager = activity.getSystemService(android.content.Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = PendingIntent.getBroadcast(activity, 120, Intent(activity, AlarmReceiver::class.java), 0)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, alarm.hours)
        calendar.set(Calendar.MINUTE, alarm.minutes)
        calendar.set(Calendar.SECOND, 0)
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, alarmIntent)
    }
}