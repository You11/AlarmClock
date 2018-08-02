package ru.you11.alarmclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class AlarmReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Toast.makeText(context, "meow", Toast.LENGTH_SHORT).show()
        Log.d("alarmTesting", "alarm started")
    }
}