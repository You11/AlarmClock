package ru.you11.alarmclock.database

import android.arch.persistence.room.TypeConverter
import kotlin.collections.HashMap

class HashmapToStringConventer {

    @TypeConverter
    fun fromToHashMap(hashMap: HashMap<String, Boolean>): String {
        var final = ""
        hashMap.forEach {
            final += it.key
            final += ","
            final += it.value.toString()
            final += ";"
        }

        return final.dropLast(1)
    }

    @TypeConverter
    fun toHashMap(data: String): HashMap<String, Boolean> {
        val hashMap = HashMap<String, Boolean>()
        val dataList = data.split(";")
        dataList.forEach {
            val stringAndBoolean = it.split(",")
            hashMap[stringAndBoolean[0]] = stringAndBoolean[1].toBoolean()
        }

        return hashMap
    }
}