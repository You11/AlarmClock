package ru.you11.alarmclock

import android.app.Application
import android.content.Context

class MainApp: Application() {

    init {
        instance = this
    }

    companion object {
        private var instance: MainApp? = null

        fun applicationContext() : Context {
            return instance!!.applicationContext
        }
    }
}