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

class AlarmSetupFragment: Fragment() {

    private lateinit var activity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = this.getActivity() as MainActivity

        return inflater.inflate(R.layout.fragment_alarm_setup, container, false)
    }

    override fun onResume() {
        super.onResume()

        setupOnEdit()
        setupSaveButton()
        setupDeleteButton()
    }

    private fun setupOnEdit() {
        //TODO: check better
        if (arguments != null) {
            view?.findViewById<TextView>(R.id.alarm_name_setup)?.apply {
                text = this@AlarmSetupFragment.arguments?.getString("alarmName")
            }

            view?.findViewById<TimePicker>(R.id.alarm_time_setup)?.apply {
                val alarmHour = this@AlarmSetupFragment.arguments?.getInt("alarmHour")!!
                val alarmMinute = this@AlarmSetupFragment.arguments?.getInt("alarmMinute")!!

                if (Build.VERSION.SDK_INT >= 23) {
                    hour = alarmHour
                    minute = alarmMinute
                } else {
                    currentHour = alarmHour
                    currentMinute = alarmMinute
                }
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

        val alarmName = view?.findViewById<EditText>(R.id.alarm_name_setup)?.apply {
            isEnabled = false
        }
        val alarmTime = view?.findViewById<TimePicker>(R.id.alarm_time_setup)?.apply {
            isEnabled = false
        }

        val selectedHour: Int
        val selectedMinute: Int

        if (alarmTime != null) {
            if (Build.VERSION.SDK_INT >= 23) {
                selectedHour = alarmTime.hour
                selectedMinute = alarmTime.minute
            }
            else {
                selectedHour = alarmTime.currentHour
                selectedMinute = alarmTime.currentMinute
            }
        } else {
            return
        }

        val id: Int? = getAlarmId()

        val alarm = Alarm(aid = id,
                name = alarmName?.text?.toString()!!,
                hours = selectedHour,
                minutes = selectedMinute,
                isOn = true)

        createAlarm(alarm)
    }

    private fun createAlarm(alarm: Alarm) {
        val utils = Utils()
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

                    Flowable.just(utils.createAlarmInDatabase(alarm, activity.disposable, activity.viewModel))
                            .observeOn(Schedulers.io())
                            .subscribeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                allAlarms.add(alarm)
                                utils.setAlarm(alarm, activity)
                                utils.updateAlarmNotification(allAlarms, activity)
                                fragmentManager?.popBackStack()
                            }
                })
    }

    private fun setupDeleteButton() {
        view?.findViewById<Button>(R.id.alarm_setup_delete_button)?.apply {
            if (arguments != null) {
                setOnClickListener {
                    val id = arguments?.getInt("alarmId")
                    if (id != null) createConfirmDeletionDialog(id)
                }
            } else {
                visibility = Button.GONE
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
                    Utils().stopAlarm(id, alarmManager, activity)
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