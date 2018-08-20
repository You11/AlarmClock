package ru.you11.alarmclock

import android.app.*
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
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

class AlarmSetupFragment: Fragment() {

    private lateinit var activity: MainActivity
    private val alarm = Alarm()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = this.getActivity() as MainActivity

        return inflater.inflate(R.layout.fragment_alarm_setup, container, false)
    }

    override fun onResume() {
        super.onResume()

        //TODO: check better
        if (arguments != null) {
            setupOnEdit()
        } else {
            setupTimePicker(null)
        }

        setupSaveButton()
    }

    private fun setupOnEdit() {
        view?.findViewById<TextView>(R.id.alarm_setup_name)?.apply {
            text = this@AlarmSetupFragment.arguments?.getString("alarmName")
        }

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, this@AlarmSetupFragment.arguments?.getInt("alarmHour")!!)
        calendar.set(Calendar.MINUTE, this@AlarmSetupFragment.arguments?.getInt("alarmMinute")!!)
        setupTimePicker(calendar)
        setupDeleteButton()
    }

    private fun setupTimePicker(editTime: Calendar?) {
        view?.findViewById<TextView>(R.id.alarm_setup_time)?.apply {

            val time: Calendar = editTime ?: Calendar.getInstance()

            text = DateFormat.getTimeInstance(DateFormat.SHORT).format(time.time)

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

    private fun setupSaveButton() {
        view?.findViewById<Button>(R.id.alarm_setup_save_button)?.apply {
            setOnClickListener {
                saveAlarm()
            }
        }
    }

    private fun saveAlarm() {

        val alarmNameView = view?.findViewById<EditText>(R.id.alarm_setup_name) ?: throw Exception("Name view is null")
        val alarmTimeView = view?.findViewById<TextView>(R.id.alarm_setup_time) ?: throw Exception("Time view is null")
        val isAlarmVibratingView = view?.findViewById<CheckBox>(R.id.alarm_setup_vibrate_checkbox) ?: throw Exception("Checkbox view is null")
        disableUI(alarmNameView, alarmTimeView, isAlarmVibratingView)

        alarm.aid = getAlarmId()
        alarm.name = alarmNameView.text?.toString()!!
        alarm.vibrate = isAlarmVibratingView.isChecked
        alarm.isOn = true

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

                    if (alarm.aid == null) {
                        alarm.aid = it.count()
                    }

                    Flowable.just(Utils.createAlarmInDatabase(alarm, activity.disposable, activity.viewModel))
                            .observeOn(Schedulers.io())
                            .subscribeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                allAlarms.add(alarm)
                                Utils.setAlarm(alarm, activity)
                                Utils.updateAlarmNotification(allAlarms, activity)
                                fragmentManager?.popBackStack()
                            }
                })
    }

    private fun setupDeleteButton() {
        view?.findViewById<Button>(R.id.alarm_setup_delete_button)?.apply {
            visibility = Button.VISIBLE
            setOnClickListener {
                val id = arguments?.getInt("alarmId")
                if (id != null) createConfirmDeletionDialog(id)
            }
        }
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
                    //TODO: Cancel notification too
                    val alarmManager = activity.getSystemService(android.content.Context.ALARM_SERVICE) as AlarmManager
                    Utils.stopAlarm(id, alarmManager, activity)
                    fragmentManager?.popBackStack()
                })
    }

    private fun getAlarmId(): Int? {
        val id: Int?

        if (arguments != null) {
            id = arguments?.getInt("alarmId")
            cancelExistingAlarm(id)
        } else {
            id = null
        }

        return id
    }

    private fun cancelExistingAlarm(id: Int?) {
        val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = PendingIntent.getBroadcast(context, id!!, Intent(context, AlarmReceiver::class.java), 0)
        alarmManager.cancel(alarmIntent)
    }
}