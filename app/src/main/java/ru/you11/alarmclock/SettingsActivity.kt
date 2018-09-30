package ru.you11.alarmclock

import android.app.Fragment
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity

class SettingsActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_preferences)
    }
}