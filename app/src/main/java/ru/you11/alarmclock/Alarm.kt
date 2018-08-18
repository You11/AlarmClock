package ru.you11.alarmclock

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import java.util.*

@Entity(tableName = "alarmData")
data class Alarm(@PrimaryKey(autoGenerate = true) var aid: Int? = null,
                 @ColumnInfo(name = "name") var name: String = "",
                 @ColumnInfo(name = "hours") var hours: Int = -1,
                 @ColumnInfo(name = "minutes") var minutes: Int = -1,
                 @ColumnInfo(name = "isVibrate") var vibrate: Boolean = false,
                 @ColumnInfo(name = "isOn") var isOn: Boolean = false
)