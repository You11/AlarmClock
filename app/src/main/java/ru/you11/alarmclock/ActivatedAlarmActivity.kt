package ru.you11.alarmclock

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import ru.you11.alarmclock.database.AlarmViewModel
import ru.you11.alarmclock.database.Injection
import ru.you11.alarmclock.database.ViewModuleFactory
import java.util.*
import java.util.concurrent.TimeUnit

class ActivatedAlarmActivity: AppCompatActivity(), SensorEventListener {

    private lateinit var alarm: Alarm
    private val disposable = CompositeDisposable()
    private lateinit var viewModel: AlarmViewModel
    private lateinit var viewModelFactory: ViewModuleFactory.ViewModelFactory
    private val mediaPlayer = MediaPlayer()
    private lateinit var vibrator: Vibrator
    private var lastShakeTime: Long = System.currentTimeMillis()
    private lateinit var sensorManager: SensorManager
    private var amountOfShakeTimes = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wakeUpDevice()

        viewModelFactory = Injection.provideViewModelFactory(this)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(AlarmViewModel::class.java)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        updateNotification()

        val alarmId = intent.extras.getLong("alarmId") / 10

        disposable.add(viewModel.getAlarm(alarmId)
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { alarm ->
                    this.alarm = alarm

                    var unlockType = ""
                    alarm.unlockType.forEach {
                        if (it.value)
                            unlockType = it.key
                    }
                    if (unlockType == "") return@subscribe

                    Log.d("alarmBug", "noise")
                    makeNoise()

                    when (unlockType) {
                        "buttonPress" -> {
                            setContentView(R.layout.activity_activated_alarm_press)
                            setupDelayButton()
                            setupTurnOffButton()
                        }

                        "buttonHold" -> {
                            setContentView(R.layout.activity_activated_alarm_hold)
                            setupOnPressTurnOffButton()
                        }

                        "shakeDevice" -> {
                            setContentView(R.layout.activity_activated_alarm_shake)
                            setupShakeLayout()
                        }
                    }
                })
    }

    private fun wakeUpDevice() {
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
    }

    private fun setupShakeLayout() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
            changeTextInShakeDialog()
        }
    }

    private fun updateNotification() {
        //TODO: gets called when delay button pressed too
        disposable.add(viewModel.getAlarmList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { allAlarms ->
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
        if (alarm.vibrate) {
            vibrate()
        }
        setupMediaPlayer()
        mediaPlayer.setOnPreparedListener {
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
                delayAlarm()
            }
        }
    }

    private fun delayAlarm() {
        val delayTime = getDelayAlarmTime()
        updateAlarmTime(alarm, delayTime)
        Utils.setDelayedAlarm(alarm, this@ActivatedAlarmActivity)
        Toast.makeText(this@ActivatedAlarmActivity, "Delayed", Toast.LENGTH_SHORT).show()
        finish()
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

    private fun setupOnPressTurnOffButton() {
        findViewById<Button>(R.id.activated_alarm_delay_button).apply {
            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        Completable.timer(5, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                                .subscribe {
                                    Toast.makeText(this@ActivatedAlarmActivity, "Turn off", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                        true
                    }

                    MotionEvent.ACTION_UP -> {
                        delayAlarm()
                        true
                    }

                    else -> {
                        false
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (this.isFinishing) {
            mediaPlayer.stop()
            mediaPlayer.release()
            vibrator.cancel()
            disposable.clear()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        val minTimeBetweenShakes: Long = 500
        val shakeThreshold = 3.25f

        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER &&
                System.currentTimeMillis() - lastShakeTime > minTimeBetweenShakes) {
            val x = event.values[0].toDouble()
            val y = event.values[1].toDouble()
            val z = event.values[2].toDouble()

            val acceleration = Math.sqrt(Math.pow(x, 2.0) +
                    Math.pow(y, 2.0) +
                    Math.pow(z, 2.0)) - SensorManager.GRAVITY_EARTH

            if (acceleration > shakeThreshold) {
                lastShakeTime = System.currentTimeMillis()

                amountOfShakeTimes--
                changeTextInShakeDialog()

                if (amountOfShakeTimes == 0) {
                    sensorManager.unregisterListener(this)
                    finish()
                }
            }
        }
    }

    private fun changeTextInShakeDialog() {
        findViewById<TextView>(R.id.activated_alarm_shake_text).apply {
            text = "Shake device " + amountOfShakeTimes.toString() + " times"
        }
    }
}