package ru.you11.alarmclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        context?.startActivity(Intent(context, ActivatedAlarmActivity::class.java))
        Log.d("alarmTesting", "alarm started")
    }
}