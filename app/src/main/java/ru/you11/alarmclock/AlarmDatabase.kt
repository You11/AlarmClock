package ru.you11.alarmclock

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context

@Database(entities = [(Alarm::class)], version = 1)
abstract class AlarmDatabase: RoomDatabase() {

    abstract fun alarmDao(): AlarmDao

    companion object {
        private var INSTANCE: AlarmDatabase? = null

        fun getInstance(context: Context): AlarmDatabase {
            if (INSTANCE == null) {
                synchronized (AlarmDatabase::class) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                            AlarmDatabase::class.java, "alarm.db")
                            .build()
                }
            }

            return INSTANCE!!
        }
    }
}