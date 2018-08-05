package ru.you11.alarmclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        //passing alarm id to new activity
        val newIntent = Intent(context, ActivatedAlarmActivity::class.java)
        newIntent.putExtra("alarmId", intent?.extras?.getInt("alarmId"))
        context?.startActivity(newIntent)

        Log.d("alarmTesting", "alarm started")
    }
}