package ru.you11.alarmclock

import android.os.Bundle
import android.preference.PreferenceFragment

class SettingsFragment: PreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)

        setupHoldButtonPref()
        setupShakeTimesPref()
    }

    private fun setupHoldButtonPref() {
        val holdButtonForSecondsPref = findPreference(resources.getString(R.string.pref_alarm_seconds_hold_button_key)) as NumberPickerPreference
        holdButtonForSecondsPref.apply {
            setOnPreferenceChangeListener { _, newValue ->
                summary = resources.getQuantityString(R.plurals.pref_alarm_seconds_hold_button_summary, newValue as Int, newValue)
                true
            }
            summary = resources.getQuantityString(R.plurals.pref_alarm_seconds_hold_button_summary, summary.toString().toInt(), summary.toString().toInt())
            minValue = 1
            maxValue = 10
        }
    }

    private fun setupShakeTimesPref() {
        val shakesToTurnOffAlarmPref = findPreference(resources.getString(R.string.pref_alarm_shake_times_number_key)) as NumberPickerPreference
        shakesToTurnOffAlarmPref.apply {
            setOnPreferenceChangeListener { _, newValue ->
                summary = resources.getQuantityString(R.plurals.pref_alarm_shake_times_number_summary, newValue as Int, newValue)
                true
            }
            summary = resources.getQuantityString(R.plurals.pref_alarm_shake_times_number_summary, summary.toString().toInt(), summary.toString().toInt())
        }
    }
}