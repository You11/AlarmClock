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
import android.net.Uri
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

    //TEMP
    private val secondsToHoldButton: Long = 5

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

                    var turnOffMode = ""
                    alarm.turnOffMode.forEach {
                        if (it.value)
                            turnOffMode = it.key
                    }
                    if (turnOffMode == "") return@subscribe

                    makeNoise()

                    when (turnOffMode) {
                        alarm.TURN_OFF_MODE_BUTTON_PRESS -> {
                            setContentView(R.layout.activity_activated_alarm_press)
                            setupDelayButton()
                            setupTurnOffButton()
                            setupLabelText()
                        }

                        alarm.TURN_OFF_MODE_BUTTON_HOLD -> {
                            setContentView(R.layout.activity_activated_alarm_hold)
                            setupOnHoldTurnOffButton()
                            setupTooltipForHoldButton()
                            setupLabelText()
                        }

                        alarm.TURN_OFF_MODE_SHAKE_DEVICE -> {
                            setContentView(R.layout.activity_activated_alarm_shake)
                            setupDelayButton()
                            setupLabelText()
                            setupAccelerometer()
                        }
                    }
                })
    }

    private fun setupTooltipForHoldButton() {
        findViewById<TextView>(R.id.activated_alarm_hold_button_tooltip)?.apply {
            text = resources.getString(R.string.activated_alarm_hold_button_tooltip, secondsToHoldButton)
        }
    }

    private fun wakeUpDevice() {
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
    }

    private fun setupLabelText() {
        if (alarm.name.isNotBlank()) {
            findViewById<TextView>(R.id.activated_alarm_name_label)?.apply {
                text = alarm.name
                visibility = TextView.VISIBLE
            }
        }
        findViewById<TextView>(R.id.activated_alarm_time_label)?.apply {
            text = Utils.getAlarmTime(alarm.hours, alarm.minutes)
        }
    }

    private fun setupAccelerometer() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
            changeTextInShakeDialog()
        }
    }

    private fun updateNotification() {
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

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val volume = prefs.getInt(resources.getString(R.string.pref_alarm_volume_value_key), 75).toFloat() / 100

        mediaPlayer.setVolume(volume, volume)
        mediaPlayer.setDataSource(this, Uri.parse(alarm.ringtone))
        mediaPlayer.prepareAsync()
    }

    private fun makeNoise() {
        if (alarm.vibrate) {
            vibrate()
        }
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val playSoundInSilent = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(resources.getString(R.string.pref_alarm_volume_in_silent_key), false)
        if (playSoundInSilent || audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL)
            setupMediaPlayer()
            mediaPlayer.setOnPreparedListener {
                mediaPlayer.start()
            }
    }

    private fun vibrate() {
        val pattern = longArrayOf(0, 1000, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            vibrator.vibrate(pattern, 0)
        }
        Log.d("vibration", "vibrated")
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
        Toast.makeText(this@ActivatedAlarmActivity, resources.getQuantityString(R.plurals.activated_alarm_delay_toast, delayTime, delayTime), Toast.LENGTH_SHORT).show()
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

    private fun setupOnHoldTurnOffButton() {
        var isTurnedOff = false
        findViewById<Button>(R.id.activated_alarm_delay_button).apply {
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        Completable.timer(secondsToHoldButton, TimeUnit.SECONDS, AndroidSchedulers.mainThread()).doOnComplete {
                            isTurnedOff = true
                            Toast.makeText(this@ActivatedAlarmActivity, context.getString(R.string.activated_alarm_turn_off_toast), Toast.LENGTH_SHORT).show()
                            finish()
                        }.subscribe()
                        true
                    }

                    MotionEvent.ACTION_UP -> {
                        if (!isTurnedOff) {
                            delayAlarm()
                        }
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
            text = resources.getQuantityString(R.plurals.activated_alarm_shake_device_text, amountOfShakeTimes, amountOfShakeTimes)
        }
    }
}