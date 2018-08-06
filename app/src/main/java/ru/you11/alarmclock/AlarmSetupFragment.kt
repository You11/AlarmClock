package ru.you11.alarmclock

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class AlarmSetupFragment: Fragment() {

    private lateinit var activity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = this.getActivity() as MainActivity

        return inflater.inflate(R.layout.fragment_alarm_setup, container, false)
    }

    override fun onResume() {
        super.onResume()

        //TODO: check better
        if (arguments != null) {
            view?.findViewById<TextView>(R.id.alarm_name_setup)?.apply {
                text = this@AlarmSetupFragment.arguments?.getString("alarmName")
            }

            view?.findViewById<TimePicker>(R.id.alarm_time_setup)?.apply {
                val alarmHour = this@AlarmSetupFragment.arguments?.getInt("alarmHour")!!
                val alarmMinute = this@AlarmSetupFragment.arguments?.getInt("alarmMinute")!!

                if (Build.VERSION.SDK_INT >= 23) {
                    hour = alarmHour
                    minute = alarmMinute
                } else {
                    currentHour = alarmHour
                    currentMinute = alarmMinute
                }
            }
        }

        //save alarm
        view?.findViewById<Button>(R.id.alarm_setup_save_button)?.apply {
            setOnClickListener {
                saveAlarm()
            }
        }
    }

    private fun saveAlarm() {

        val alarmName = view?.findViewById<EditText>(R.id.alarm_name_setup)?.apply {
            isEnabled = false
        }
        val alarmTime = view?.findViewById<TimePicker>(R.id.alarm_time_setup)?.apply {
            isEnabled = false
        }

        val saveButton = view?.findViewById<Button>(R.id.alarm_setup_save_button)

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
            return
        }

        val id: Int? = getAlarmId()

        val alarm = Alarm(aid = id,
                name = alarmName?.text?.toString()!!,
                hours = selectedHour,
                minutes = selectedMinute)
        //TODO: dispose onStop?
        Flowable.just(Utils().createAlarmInDatabase(alarm, activity.disposable, activity.viewModel))
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe( {
                    Utils().setupAlarm(alarm, activity)
                    createNotification(alarm)
                    fragmentManager?.popBackStack()
                }, {
                    Toast.makeText(activity, "Error: " + it.localizedMessage, Toast.LENGTH_SHORT).show()
                    saveButton?.isEnabled = true
                    alarmTime.isEnabled = true
                })
    }

    private fun createNotification(alarm: Alarm) {
        val notification = NotificationCompat.Builder(activity, "100")
        notification.setSmallIcon(R.drawable.baseline_alarm_white_18)
        notification.setContentTitle("Alarm")
        notification.setContentText(Utils().getAlarmTime(alarm.hours, alarm.minutes))
        notification.priority = NotificationCompat.PRIORITY_DEFAULT
        val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT > 26) {
            val notificationChannel = NotificationChannel("100", "name?????", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(notificationChannel)
        }
        notificationManager.notify(100, notification.build())
    }

    private fun getAlarmId(): Int? {
        val id: Int?

        if (arguments != null) {
            id = arguments?.getInt("alarmId")
            cancelExistingAlarm(id)
        } else {
            id = null
        }

        return id
    }

    private fun cancelExistingAlarm(id: Int?) {
        val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = PendingIntent.getBroadcast(context, id!!, Intent(context, AlarmReceiver::class.java), 0)
        alarmManager.cancel(alarmIntent)
    }
}