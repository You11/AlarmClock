package ru.you11.alarmclock

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.Toast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*

class ActivatedAlarmActivity: AppCompatActivity() {

    private lateinit var alarm: Alarm
    private val disposable = CompositeDisposable()
    private lateinit var viewModel: AlarmViewModel
    private lateinit var viewModelFactory: ViewModuleFactory.ViewModelFactory
    private val mediaPlayer = MediaPlayer()
    private lateinit var vibrator: Vibrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activated_alarm)

        viewModelFactory = Injection.provideViewModelFactory(this)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(AlarmViewModel::class.java)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        val alarmId = intent.extras.getInt("alarmId")

        disposable.add(viewModel.getAlarm(alarmId)
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { alarm ->
                    this.alarm = alarm

                    setupNewAlarm()
                    setupMediaPlayer()
                    makeNoise()
                    setupDelayButton()
                    setupTurnOffButton()
                })
    }

    private fun setupNewAlarm() {
        disposable.add(viewModel.getAlarmList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { allAlarms ->
                    Utils.setAlarmWithDays(alarm, this@ActivatedAlarmActivity)
                    Utils.updateAlarmNotification(allAlarms, this@ActivatedAlarmActivity)
                })
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
    }

    private fun makeNoise() {
        mediaPlayer.setOnPreparedListener {
            if (alarm.vibrate) {
                vibrate()
            }
            Toast.makeText(this, "prepared!", Toast.LENGTH_SHORT).show()
            mediaPlayer.start()
        }
    }

    private fun vibrate() {
        val pattern = longArrayOf(0, 1000, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
            Log.d("vibration", "vibrated")
        } else {
            vibrator.vibrate(pattern, 0)
            Log.d("vibration", "vibrated")
        }
    }

    private fun setupDelayButton() {
        findViewById<Button>(R.id.activated_alarm_delay_button).apply {
            setOnClickListener { _ ->

                this.isEnabled = false

                val delayTime = getDelayAlarmTime()
                updateAlarmTime(alarm, delayTime)
                Utils.setDelayedAlarm(alarm, this@ActivatedAlarmActivity)
                finish()
            }
        }
    }

    private fun getDelayAlarmTime(): Int {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        return sharedPref.getString(resources.getString(R.string.pref_alarm_delay_time_key),
                resources.getString(R.string.pref_alarm_delay_time_default)).toInt()
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
                finish()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer.stop()
        mediaPlayer.release()
        vibrator.cancel()
        disposable.clear()
    }
}