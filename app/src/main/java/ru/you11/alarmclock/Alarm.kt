package ru.you11.alarmclock

import android.arch.persistence.room.*
import android.os.Parcelable
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.util.*
import kotlin.collections.HashMap

@Parcelize
@Entity(tableName = "alarmData")
@TypeConverters(HashmapStringToBooleanConventer::class)
data class Alarm(@PrimaryKey(autoGenerate = true) var aid: Long = 0,
                 @ColumnInfo(name = "name") var name: String = "",
                 @ColumnInfo(name = "hours") var hours: Int = -1,
                 @ColumnInfo(name = "minutes") var minutes: Int = -1,
                 @ColumnInfo(name = "isVibrate") var vibrate: Boolean = false,
                 @ColumnInfo(name = "days") var days: HashMap<String, Boolean> = hashMapOf("monday" to true,
                         "tuesday" to true,
                         "wednesday" to true,
                         "thursday" to true,
                         "friday" to true,
                         "saturday" to false,
                         "sunday" to false),
                 @ColumnInfo(name = "unlockType") var unlockType: HashMap<String, Boolean> = hashMapOf("buttonPress" to true,
                         "buttonHold" to false,
                         "shakeDevice" to false
                 ),
                 @ColumnInfo(name = "isOn") var isOn: Boolean = false
): Parcelable {

    @Ignore
    @IgnoredOnParcel
    val daysStringToCalendar = hashMapOf("monday" to Calendar.MONDAY,
            "tuesday" to Calendar.TUESDAY,
            "wednesday" to Calendar.WEDNESDAY,
            "thursday" to Calendar.THURSDAY,
            "friday" to Calendar.FRIDAY,
            "saturday" to Calendar.SATURDAY,
            "sunday" to Calendar.SUNDAY)
}