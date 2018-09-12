package ru.you11.alarmclock.database

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.arch.persistence.room.Query
import io.reactivex.Maybe
import io.reactivex.Single
import ru.you11.alarmclock.Alarm

@Dao
interface AlarmDao {

    @Query("SELECT * FROM alarmData")
    fun getAll(): Maybe<List<Alarm>>

    @Query("SELECT * FROM alarmData WHERE aid=:id")
    fun getAlarm(id: Long): Single<Alarm>

    @Query("UPDATE alarmData SET isOn=:value WHERE aid=:id")
    fun updateAlarmStatus(id: Long, value: Boolean)

    @Insert(onConflict = REPLACE)
    fun insert(alarm: Alarm): Long

    @Query("DELETE from alarmData")
    fun deleteAll()

    @Query("DELETE from alarmData WHERE aid=:id")
    fun deleteAlarm(id: Long)
}