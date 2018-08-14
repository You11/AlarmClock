package ru.you11.alarmclock

import android.arch.lifecycle.ViewModelProviders
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.Toast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*

class ActivatedAlarmActivity: AppCompatActivity() {

    private val disposable = CompositeDisposable()
    private lateinit var viewModel: AlarmViewModel
    private lateinit var viewModelFactory: ViewModuleFactory.ViewModelFactory
    private val mediaPlayer = MediaPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activated_alarm)

        viewModelFactory = Injection.provideViewModelFactory(this)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(AlarmViewModel::class.java)

        setupMediaPlayer()
        setupDelayButton()
        setupTurnOffButton()
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
        findViewById<Button>(R.id.activated_alarm_delay_button).apply {
            setOnClickListener { _ ->

                this.isEnabled = false

                val alarmId = intent.extras.getInt("alarmId")

                disposable.add(viewModel.getAlarm(alarmId)
                        .subscribeOn(Schedulers.single())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { alarm ->

                            val delayTime = getDelayAlarmTime()
                            updateAlarmTime(alarm, delayTime)
                            Utils().setAlarm(alarm, this@ActivatedAlarmActivity)
                            finish()
                        })
            }
        }
    }

    private fun getDelayAlarmTime(): Int {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        return sharedPref.getString("pref_delay_time", "1").toInt()
    }

    private fun updateAlarmTime(alarm: Alarm, delayTime: Int) {
        val currentTime = Calendar.getInstance()
        currentTime.add(Calendar.MINUTE, delayTime)

        alarm.hours = currentTime.get(Calendar.HOUR_OF_DAY)
        alarm.minutes = currentTime.get(Calendar.MINUTE)
    }

    private fun setupTurnOffButton() {
        findViewById<Button>(R.id.activated_alarm_turn_off_button).apply {
            setOnClickListener {
                mediaPlayer.stop()
                finish()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer.stop()
        mediaPlayer.release()
        disposable.clear()
    }
}