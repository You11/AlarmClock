package ru.you11.alarmclock

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log

class AlarmReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val newIntent = Intent(context, ActivatedAlarmActivity::class.java)
        //passing alarm id to new activity
        newIntent.putExtra("alarmId", intent?.extras?.getLong("alarmId"))
        newIntent.flags = FLAG_ACTIVITY_NEW_TASK
        context.startActivity(newIntent)
    }
}