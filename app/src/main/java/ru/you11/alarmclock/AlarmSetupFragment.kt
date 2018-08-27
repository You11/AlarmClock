package ru.you11.alarmclock

import android.app.*
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.text.DateFormat
import java.util.*
import kotlin.collections.ArrayList

class AlarmSetupFragment: Fragment() {

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
        } else {
            setupTimePicker(editTime = null)
        }

        setupDays()
        setupSaveButton()
    }

    private fun setupOnEdit() {
        alarm = arguments?.getParcelable("alarm")!!
        view?.findViewById<TextView>(R.id.alarm_setup_name)?.apply {
            text = alarm.name
        }

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, alarm.hours)
        calendar.set(Calendar.MINUTE, alarm.minutes)
        setupTimePicker(calendar)
        setupVibrateCheckbox()
        setupDeleteButton()
    }

    private fun setupTimePicker(editTime: Calendar?) {
        view?.findViewById<TextView>(R.id.alarm_setup_time)?.apply {

            val time: Calendar = editTime ?: Calendar.getInstance()

            text = DateFormat.getTimeInstance(DateFormat.SHORT).format(time.time)
            alarm.hours = time.get(Calendar.HOUR_OF_DAY)
            alarm.minutes = time.get(Calendar.MINUTE)

            setOnClickListener {

                val listener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                    alarm.hours = hourOfDay
                    alarm.minutes = minute

                    time.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    time.set(Calendar.MINUTE, minute)
                    text = DateFormat.getTimeInstance(DateFormat.SHORT).format(time.time)
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

    private fun setupDays() {
        val daysViews = hashMapOf<String, TextView>("monday" to view?.findViewById(R.id.alarm_setup_days_monday)!!,
                "tuesday" to view?.findViewById(R.id.alarm_setup_days_tuesday)!!,
                "wednesday" to view?.findViewById(R.id.alarm_setup_days_wednesday)!!,
                "thursday" to view?.findViewById(R.id.alarm_setup_days_thursday)!!,
                "friday" to view?.findViewById(R.id.alarm_setup_days_friday)!!,
                "saturday" to view?.findViewById(R.id.alarm_setup_days_saturday)!!,
                "sunday" to view?.findViewById(R.id.alarm_setup_days_sunday)!!)


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
                if (alarm.aid != null) createConfirmDeletionDialog(alarm.aid!!)
            }
        }
    }

    private fun saveAlarm() {

        val alarmNameView = view?.findViewById<EditText>(R.id.alarm_setup_name) ?: throw Exception("Name view is null")
        val alarmTimeView = view?.findViewById<TextView>(R.id.alarm_setup_time) ?: throw Exception("Time view is null")
        val isAlarmVibratingView = view?.findViewById<CheckBox>(R.id.alarm_setup_vibrate_checkbox) ?: throw Exception("Checkbox view is null")
        disableUI(alarmNameView, alarmTimeView, isAlarmVibratingView)

        alarm.name = alarmNameView.text?.toString()!!
        alarm.vibrate = isAlarmVibratingView.isChecked
        alarm.isOn = true
        if (alarm.aid != null) {
            Utils.stopAlarm(alarm.aid!!, activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager, activity)
        }

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

                    if (alarm.aid == null) alarm.aid = getNewAlarmId(it)

                    Flowable.just(Utils.createAlarmInDatabase(alarm, activity.disposable, activity.viewModel))
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.io())
                            .subscribe {
                                allAlarms.add(alarm)
                                Utils.setAlarm(alarm, activity)
                                Utils.updateAlarmNotification(allAlarms, activity)
                                fragmentManager?.popBackStack()
                            }
                })
    }

    private fun getNewAlarmId(alarms: List<Alarm>): Int {
        return alarms.count()
    }

    private fun createConfirmDeletionDialog(id: Int) {
        AlertDialog.Builder(activity).apply {
            setTitle("Delete alarm?")
            setPositiveButton("Yes") { dialog: DialogInterface, _: Int ->
                deleteAlarm(id)
                dialog.dismiss()
            }

            setNegativeButton("No") { dialog: DialogInterface, _: Int ->
                dialog.cancel()
            }
            create()
            show()
        }
    }

    private fun deleteAlarm(id: Int) {
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
}