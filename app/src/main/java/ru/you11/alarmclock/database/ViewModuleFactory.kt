package ru.you11.alarmclock.database

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider

class ViewModuleFactory {

    class ViewModelFactory(private val dataSource: AlarmDao) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AlarmViewModel::class.java)) {
                return AlarmViewModel(dataSource) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
