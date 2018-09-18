package ru.you11.alarmclock

import android.os.Bundle
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat

class SettingsFragment: PreferenceFragmentCompat() {

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }
}