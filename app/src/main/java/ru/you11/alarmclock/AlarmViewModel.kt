package ru.you11.alarmclock

import android.arch.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.Maybe

class AlarmViewModel(private val dataSource: AlarmDao) : ViewModel() {

    fun getAlarmList(): Maybe<List<Alarm>> {
        return dataSource.getAll()
    }

    fun updateAlarm(alarmId: Int, alarmName: String): Completable {
        return Completable.fromAction {
            val alarm = Alarm(alarmId, alarmName)
            dataSource.insert(alarm)
        }
    }
}