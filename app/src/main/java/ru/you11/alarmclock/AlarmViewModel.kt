package ru.you11.alarmclock

import android.arch.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.Maybe

class AlarmViewModel(private val dataSource: AlarmDao) : ViewModel() {

    fun getAlarmList(): Maybe<List<Alarm>> {
        return dataSource.getAll()
    }

    fun updateAlarm(alarm: Alarm): Completable {
        return Completable.fromAction {
            dataSource.insert(alarm)
        }
    }

    fun deleteAllAlarms(): Completable {
        return Completable.fromAction {
            dataSource.deleteAll()
        }
    }
}