package ru.you11.alarmclock

import android.content.Context

object Injection {

    private fun provideAlarmDataSource(context: Context): AlarmDao {
        val database = AlarmDatabase.getInstance(context)
        return database.alarmDao()
    }

    fun provideViewModelFactory(context: Context): ViewModuleFactory.ViewModelFactory {
        val dataSource = provideAlarmDataSource(context)
        return ViewModuleFactory.ViewModelFactory(dataSource)
    }
}
