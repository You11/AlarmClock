package ru.you11.alarmclock

import android.app.Application
import android.content.Context
import com.squareup.leakcanary.LeakCanary

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

    override fun onCreate() {
        super.onCreate()
//        if (LeakCanary.isInAnalyzerProcess(this)) {
//            return
//        }
//        LeakCanary.install(this)
    }
}