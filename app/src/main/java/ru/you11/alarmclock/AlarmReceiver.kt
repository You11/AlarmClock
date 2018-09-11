package ru.you11.alarmclock

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.support.v4.app.NotificationCompat
import android.util.Log

class AlarmReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("alarmBug", "received")

        //passing alarm id to new activity
        val newIntent = Intent(context, ActivatedAlarmActivity::class.java)
        newIntent.putExtra("alarmId", intent?.extras?.getInt("alarmId"))
        newIntent.flags = FLAG_ACTIVITY_NEW_TASK
        context.startActivity(newIntent)

        Log.d("alarmTesting", "alarm started")
    }
}