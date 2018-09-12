package ru.you11.alarmclock

import android.arch.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.internal.operators.completable.CompletableFromAction

class AlarmViewModel(private val dataSource: AlarmDao) : ViewModel() {

    fun getAlarmList(): Maybe<List<Alarm>> {
        return dataSource.getAll()
    }

    fun getAlarm(id: Int): Single<Alarm> {
        return dataSource.getAlarm(id)
    }

    fun updateAlarm(alarm: Alarm): Long {
        return dataSource.insert(alarm)
    }

    //TODO: should i keep this?
    fun updateAlarmStatus(id: Long, value: Boolean): Completable {
        return Completable.fromAction {
            dataSource.updateAlarmStatus(id, value)
        }
    }

    fun deleteAllAlarms(): Completable {
        return Completable.fromAction {
            dataSource.deleteAll()
        }
    }

    fun deleteAlarm(id: Long): Completable {
        return Completable.fromAction {
            dataSource.deleteAlarm(id)
        }
    }
}