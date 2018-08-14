package ru.you11.alarmclock

import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity

class SettingsActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
    }

}