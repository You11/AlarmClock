package ru.you11.alarmclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RestartAlarmReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        if ("android.intent.action.BOOT_COMPLETED" == intent?.action) {
            val bootIntent = Intent(context, BootService::class.java)
            context?.let { BootService().enqueueWork(it, bootIntent) }
        }
    }
}