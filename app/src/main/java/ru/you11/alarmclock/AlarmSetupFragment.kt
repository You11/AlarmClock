package ru.you11.alarmclock

import android.app.*
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.text.DateFormat
import java.util.*
import kotlin.collections.ArrayList

class AlarmSetupFragment: Fragment() {

    private val RINGTONE_ACTIVITY_REQUEST_CODE = 200
    private lateinit var activity: MainActivity
    private var alarm = Alarm()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = this.getActivity() as MainActivity
        return inflater.inflate(R.layout.fragment_alarm_setup, container, false)
    }

    override fun onResume() {
        super.onResume()
        if (arguments != null && arguments?.getParcelable<Alarm>("alarm") != null) {
            setupOnEdit()
        }

        setupTimePicker()
        setupTurnOffModeButton()
        setupRingtoneButton()
        setupDays()
        setupSaveButton()
    }

    private fun setupOnEdit() {
        alarm = arguments?.getParcelable("alarm")!!
        view?.findViewById<TextView>(R.id.alarm_setup_name)?.apply {
            text = alarm.name
        }

        setupVibrateCheckbox()
        setupDeleteButton()
    }

    private fun setupTimePicker() {
        view?.findViewById<LinearLayout>(R.id.alarm_setup_time)?.apply {

            val time = Calendar.getInstance()

            if (alarm.hours != -1 && alarm.minutes != -1) {
                time.set(Calendar.HOUR_OF_DAY, alarm.hours)
                time.set(Calendar.MINUTE, alarm.minutes)
            }

            setSummaryForTime(time)
            alarm.hours = time.get(Calendar.HOUR_OF_DAY)
            alarm.minutes = time.get(Calendar.MINUTE)

            setOnClickListener {

                val listener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                    alarm.hours = hourOfDay
                    alarm.minutes = minute

                    time.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    time.set(Calendar.MINUTE, minute)
                    setSummaryForTime(time)
                }

                val dialog = TimePickerDialog(activity,
                        listener,
                        time.get(Calendar.HOUR_OF_DAY),
                        time.get(Calendar.MINUTE),
                        true)
                dialog.show()
            }
        }
    }

    private fun setupTurnOffModeButton() {
        val turnOffButtonPressDialogText = resources.getString(R.string.alarm_setup_turn_off_dialog_button_press_mode)
        val turnOffButtonHoldDialogText = resources.getString(R.string.alarm_setup_turn_off_dialog_button_hold_mode)
        val turnOffShakeDeviceDialogText = resources.getString(R.string.alarm_setup_turn_off_dialog_shake_device_mode)

        alarm.turnOffMode.forEach {
            if (it.value) {
                when (it.key) {
                    alarm.TURN_OFF_MODE_BUTTON_PRESS -> setSummaryForTurnOffMode(turnOffButtonPressDialogText)
                    alarm.TURN_OFF_MODE_BUTTON_HOLD -> setSummaryForTurnOffMode(turnOffButtonHoldDialogText)
                    alarm.TURN_OFF_MODE_SHAKE_DEVICE -> setSummaryForTurnOffMode(turnOffShakeDeviceDialogText)
                }
            }
        }
        view?.findViewById<LinearLayout>(R.id.alarm_setup_unlock_type)?.apply {
            setOnClickListener {
                val methods = arrayOf(turnOffButtonPressDialogText,
                        turnOffButtonHoldDialogText,
                        turnOffShakeDeviceDialogText)

                val dialog = AlertDialog.Builder(activity)
                dialog.setTitle(resources.getString(R.string.alarm_setup_turn_off_dialog_title))
                dialog.setItems(methods) { _, which ->
                    for (mode in alarm.turnOffMode) {
                        mode.setValue(false)
                    }

                    when (which) {
                        0 -> {
                            alarm.turnOffMode[alarm.TURN_OFF_MODE_BUTTON_PRESS] = true
                            setSummaryForTurnOffMode(turnOffButtonPressDialogText)
                        }

                        1 -> {
                            alarm.turnOffMode[alarm.TURN_OFF_MODE_BUTTON_HOLD] = true
                            setSummaryForTurnOffMode(turnOffButtonHoldDialogText)
                        }

                        2 -> {
                            alarm.turnOffMode[alarm.TURN_OFF_MODE_SHAKE_DEVICE] = true
                            setSummaryForTurnOffMode(turnOffShakeDeviceDialogText)
                        }

                        else -> {
                            alarm.turnOffMode[alarm.TURN_OFF_MODE_BUTTON_PRESS] = true
                            setSummaryForTurnOffMode(turnOffButtonPressDialogText)
                        }
                    }
                }
                dialog.create()
                dialog.show()
            }
        }
    }

    private fun setupRingtoneButton() {
        view?.findViewById<LinearLayout>(R.id.alarm_setup_ringtone_button)?.apply {
            setOnClickListener {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, resources.getString(R.string.alarm_setup_ringtone_dialog_title))
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(alarm.ringtone))
                startActivityForResult(intent, RINGTONE_ACTIVITY_REQUEST_CODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RINGTONE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                val uri = data?.extras?.get(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) as Uri
                alarm.ringtone = uri.toString()
                setSummaryForRingtone(uri)
            }
        }
    }

    private fun setupDays() {
        val daysViews = hashMapOf<String, TextView>(resources.getString(R.string.alarm_days_monday) to view?.findViewById(R.id.alarm_setup_days_monday)!!,
                resources.getString(R.string.alarm_days_tuesday) to view?.findViewById(R.id.alarm_setup_days_tuesday)!!,
                resources.getString(R.string.alarm_days_wednesday) to view?.findViewById(R.id.alarm_setup_days_wednesday)!!,
                resources.getString(R.string.alarm_days_thursday) to view?.findViewById(R.id.alarm_setup_days_thursday)!!,
                resources.getString(R.string.alarm_days_friday) to view?.findViewById(R.id.alarm_setup_days_friday)!!,
                resources.getString(R.string.alarm_days_saturday) to view?.findViewById(R.id.alarm_setup_days_saturday)!!,
                resources.getString(R.string.alarm_days_sunday) to view?.findViewById(R.id.alarm_setup_days_sunday)!!)


        daysViews.forEach {
            if (alarm.days[it.key] == true) {
                it.value.setTextColor(Color.RED)
            }

            it.value.setOnClickListener { _ ->
                if (alarm.days[it.key] == true) {
                    it.value.setTextColor(Color.GRAY)
                    alarm.days[it.key] = false
                } else {
                    it.value.setTextColor(Color.RED)
                    alarm.days[it.key] = true
                }
            }
        }
    }

    private fun setupVibrateCheckbox() {
        view?.findViewById<CheckBox>(R.id.alarm_setup_vibrate_checkbox)?.apply {
            this.isChecked = alarm.vibrate
        }
    }

    private fun setupSaveButton() {
        view?.findViewById<Button>(R.id.alarm_setup_save_button)?.apply {
            setOnClickListener {
                saveAlarm()
            }
        }
    }

    private fun setupDeleteButton() {
        view?.findViewById<Button>(R.id.alarm_setup_delete_button)?.apply {
            visibility = Button.VISIBLE
            setOnClickListener {
                createConfirmDeletionDialog(alarm.aid)
            }
        }
    }

    private fun saveAlarm() {
        val alarmNameView = view?.findViewById<EditText>(R.id.alarm_setup_name) ?: throw Exception("Name view is null")
        val alarmTimeView = view?.findViewById<TextView>(R.id.alarm_setup_time_summary) ?: throw Exception("Time view is null")
        val isAlarmVibratingView = view?.findViewById<CheckBox>(R.id.alarm_setup_vibrate_checkbox) ?: throw Exception("Checkbox view is null")
        disableUI(alarmNameView, alarmTimeView, isAlarmVibratingView)

        alarm.name = alarmNameView.text?.toString()!!
        alarm.vibrate = isAlarmVibratingView.isChecked
        alarm.isOn = true
        Utils.stopAlarm(alarm.aid, activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager, activity)

        createAlarm(alarm)
    }

    private fun disableUI(vararg args: View) {
        args.forEach {
            it.isEnabled = false
        }

    }

    private fun createAlarm(alarm: Alarm) {
        //TODO: dispose flowable onStop?
        //gets all alarms to get id for new alarm
        activity.disposable.add(activity.viewModel.getAlarmList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { it ->

                    val allAlarms = ArrayList<Alarm>()
                    allAlarms.addAll(it)

                    Observable.fromCallable { activity.viewModel.updateAlarm(alarm) }
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                alarm.aid = it
                                allAlarms.add(alarm)
                                Utils.setAlarmWithDays(alarm, activity)
                                Utils.updateAlarmNotification(allAlarms, activity)
                                fragmentManager?.popBackStack()
                            }
                })
    }

    private fun createConfirmDeletionDialog(id: Long) {
        AlertDialog.Builder(activity).apply {
            setTitle(resources.getString(R.string.alarm_setup_delete_dialog_title))
            setPositiveButton(resources.getString(R.string.confirm_button)) { dialog: DialogInterface, _: Int ->
                deleteAlarm(id)
                dialog.dismiss()
            }

            setNegativeButton(resources.getString(R.string.cancel_button)) { dialog: DialogInterface, _: Int ->
                dialog.cancel()
            }
            create()
            show()
        }
    }

    private fun deleteAlarm(id: Long) {
        activity.disposable.add(activity.viewModel.deleteAlarm(id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    activity.disposable.add(activity.viewModel.getAlarmList()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                val alarmManager = activity.getSystemService(android.content.Context.ALARM_SERVICE) as AlarmManager
                                Utils.stopAlarm(id, alarmManager, activity)
                                Utils.updateAlarmNotification(it, activity)
                                fragmentManager?.popBackStack()
                            })
                })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            putParcelable("alarm", alarm)
        }

        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        alarm = savedInstanceState?.getParcelable("alarm") ?: Alarm(ringtone = RingtoneManager.getActualDefaultRingtoneUri(activity.applicationContext, RingtoneManager.TYPE_ALARM).toString())
        setSummaryForRingtone(Uri.parse(alarm.ringtone))
    }

    private fun setSummaryForTime(calendar: Calendar) {
        view?.findViewById<TextView>(R.id.alarm_setup_time_summary)?.text = DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.time)
    }

    private fun setSummaryForTurnOffMode(text: String) {
        view?.findViewById<TextView>(R.id.alarm_setup_unlock_type_summary)?.text = text
    }

    private fun setSummaryForRingtone(uri: Uri) {
        view?.findViewById<TextView>(R.id.alarm_setup_ringtone_summary)?.text = RingtoneManager.getRingtone(activity, uri).getTitle(activity)
    }
}