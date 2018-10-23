package ru.you11.alarmclock

import android.arch.persistence.room.*
import android.os.Parcelable
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import ru.you11.alarmclock.database.HashmapToStringConventer
import ru.you11.alarmclock.database.LinkedHashmapToStringConventer
import java.util.*
import kotlin.collections.HashMap

@Parcelize
@Entity(tableName = "alarmData")
@TypeConverters(LinkedHashmapToStringConventer::class, HashmapToStringConventer::class)
data class Alarm(@PrimaryKey(autoGenerate = true) var aid: Long = 0,
                 @ColumnInfo(name = "name") var name: String = "",
                 @ColumnInfo(name = "hours") var hours: Int = -1,
                 @ColumnInfo(name = "minutes") var minutes: Int = -1,
                 @ColumnInfo(name = "isVibrate") var vibrate: Boolean = false,
                 @ColumnInfo(name = "days") var days: LinkedHashMap<String, Boolean> = linkedMapOf(MainApp.applicationContext().resources.getString(R.string.alarm_days_monday) to true,
                         MainApp.applicationContext().resources.getString(R.string.alarm_days_tuesday) to true,
                         MainApp.applicationContext().resources.getString(R.string.alarm_days_wednesday) to true,
                         MainApp.applicationContext().resources.getString(R.string.alarm_days_thursday) to true,
                         MainApp.applicationContext().resources.getString(R.string.alarm_days_friday) to true,
                         MainApp.applicationContext().resources.getString(R.string.alarm_days_saturday) to false,
                         MainApp.applicationContext().resources.getString(R.string.alarm_days_sunday) to false),
                 @ColumnInfo(name = "ringtone") var ringtone: String? = null,
                 @ColumnInfo(name = "turnOffMode") var turnOffMode: HashMap<String, Boolean> = hashMapOf(
                         MainApp.applicationContext().resources.getString(R.string.alarm_turn_off_mode_press_button) to true,
                         MainApp.applicationContext().resources.getString(R.string.alarm_turn_off_mode_hold_button) to false,
                         MainApp.applicationContext().resources.getString(R.string.alarm_turn_off_mode_shake_device) to false
                 ),
                 @ColumnInfo(name = "isOn") var isOn: Boolean = false
): Parcelable {

    @Ignore @IgnoredOnParcel
    val daysStringToCalendar = hashMapOf(MainApp.applicationContext().resources.getString(R.string.alarm_days_monday) to Calendar.MONDAY,
            MainApp.applicationContext().resources.getString(R.string.alarm_days_tuesday) to Calendar.TUESDAY,
            MainApp.applicationContext().resources.getString(R.string.alarm_days_wednesday) to Calendar.WEDNESDAY,
            MainApp.applicationContext().resources.getString(R.string.alarm_days_thursday) to Calendar.THURSDAY,
            MainApp.applicationContext().resources.getString(R.string.alarm_days_friday) to Calendar.FRIDAY,
            MainApp.applicationContext().resources.getString(R.string.alarm_days_saturday) to Calendar.SATURDAY,
            MainApp.applicationContext().resources.getString(R.string.alarm_days_sunday) to Calendar.SUNDAY)

    @Ignore @IgnoredOnParcel
    val TURN_OFF_MODE_BUTTON_PRESS = MainApp.applicationContext().resources.getString(R.string.alarm_turn_off_mode_press_button)

    @Ignore @IgnoredOnParcel
    val TURN_OFF_MODE_BUTTON_HOLD = MainApp.applicationContext().resources.getString(R.string.alarm_turn_off_mode_hold_button)

    @Ignore @IgnoredOnParcel
    val TURN_OFF_MODE_SHAKE_DEVICE = MainApp.applicationContext().resources.getString(R.string.alarm_turn_off_mode_shake_device)


    fun getEarliestDate(): Calendar {
        var earliestDate = Calendar.getInstance()
        earliestDate.timeInMillis = Long.MAX_VALUE

        days.forEach {
            if (it.value) {
                val alarmDate = Utils.getAlarmDateFromAlarm(this, it.key)

                if (alarmDate.before(earliestDate)) {
                    earliestDate = alarmDate
                }
            }
        }

        if (isSingleAlarm()) {
            val alarmDate = Utils.getAlarmDateFromAlarm(this)

            if (alarmDate.before(earliestDate)) {
                earliestDate = alarmDate
            }
        }

        return earliestDate
    }

    fun isSingleAlarm(): Boolean {
        var isSingle = true

        days.forEach {
            if (it.value) {
                isSingle = false
            }
        }

        return isSingle
    }
}