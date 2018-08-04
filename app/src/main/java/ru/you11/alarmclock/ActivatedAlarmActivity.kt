package ru.you11.alarmclock

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.Toast

class ActivatedAlarmActivity: AppCompatActivity() {

    val mediaPlayer = MediaPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activated_alarm)

        setupMediaPlayer()
        setupDelayButton()
        setupDismissButton()
    }

    private fun setupMediaPlayer() {
        if (Build.VERSION.SDK_INT < 21) {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM)
        } else {
            val attributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
            mediaPlayer.setAudioAttributes(attributes)
        }
        val url = "https://d1u5p3l4wpay3k.cloudfront.net/dota2_gamepedia/5/58/Pain_pain_17.mp3"
        mediaPlayer.setDataSource(url)
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener {
            Toast.makeText(this, "prepared!", Toast.LENGTH_SHORT).show()
            mediaPlayer.start()
        }
    }

    private fun setupDelayButton() {

    }

    private fun setupDismissButton() {
        findViewById<Button>(R.id.activated_alarm_dismiss_button).apply {
            setOnClickListener {
                mediaPlayer.stop()
                finish()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer.stop()
    }
}