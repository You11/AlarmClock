package ru.you11.alarmclock

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import java.util.*

@Entity(tableName = "alarmData")
data class Alarm(@PrimaryKey(autoGenerate = true) var aid: Int? = null,
                 @ColumnInfo(name = "name") var name: String = ""
//                 @ColumnInfo(name = "time") val time: Date = Date())
)