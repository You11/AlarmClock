package ru.you11.alarmclock

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.arch.persistence.room.Query
import io.reactivex.Flowable
import io.reactivex.Maybe

@Dao
interface AlarmDao {

    @Query("SELECT * FROM alarmData")
    fun getAll(): Maybe<List<Alarm>>

    @Insert(onConflict = REPLACE)
    fun insert(alarm: Alarm)

    @Query("DELETE from alarmData")
    fun deleteAll()
}