package ru.you11.alarmclock.database

import android.arch.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import ru.you11.alarmclock.Alarm

class AlarmViewModel(private val dataSource: AlarmDao) : ViewModel() {

    fun getAlarmList(): Maybe<List<Alarm>> {
        return dataSource.getAll()
    }

    fun getAlarm(id: Long): Single<Alarm> {
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