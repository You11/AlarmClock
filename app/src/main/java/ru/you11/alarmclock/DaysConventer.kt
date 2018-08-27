package ru.you11.alarmclock

import android.arch.persistence.room.TypeConverter
import kotlin.collections.HashMap

class DaysConventer {

    @TypeConverter
    fun fromDays(days: HashMap<String, Boolean>): String {
        var final = ""
        days.forEach {
            final += it.key
            final += ","
            final += it.value.toString()
            final += ";"
        }

        return final.dropLast(1)
    }

    @TypeConverter
    fun toDays(data: String): HashMap<String, Boolean> {
        val hashMapDays = HashMap<String, Boolean>()
        val dataList = data.split(";")
        dataList.forEach {
            val stringAndBoolean = it.split(",")
            hashMapDays[stringAndBoolean[0]] = stringAndBoolean[1].toBoolean()
        }


        return hashMapDays
    }
}