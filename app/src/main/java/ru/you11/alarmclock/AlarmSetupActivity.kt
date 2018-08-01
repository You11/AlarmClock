package ru.you11.alarmclock

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.EditText
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.alarm_recycler_view_card.*

class AlarmSetupActivity: AppCompatActivity() {

    private lateinit var viewModelFactory: ViewModuleFactory.ViewModelFactory

    private lateinit var viewModel: AlarmViewModel

    private val disposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_setup)

        viewModelFactory = Injection.provideViewModelFactory(this)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(AlarmViewModel::class.java)

        findViewById<Button>(R.id.alarm_setup_save_button).apply {
            setOnClickListener {
                updateAlarms()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Subscribe to the emissions of the user name from the view model.
        // Update the user name text view, at every onNext emission.
        // In case of error, log the exception.
        disposable.add(viewModel.getAlarmList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe())
    }

    override fun onStop() {
        super.onStop()

        // clear all the subscription
        disposable.clear()
    }

    private fun updateAlarms() {
        val alarmName = findViewById<EditText>(R.id.alarm_name_setup).apply {
            isEnabled = false

        }
        val saveButton = findViewById<Button>(R.id.alarm_setup_save_button)

        var alarmCount: Int

        val thread = Schedulers.single()

        //TODO: how the hell do i thread
        disposable.add(viewModel.getAlarmList()
                .subscribeOn(thread)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    alarmCount = it.count()
                    disposable.add(viewModel.updateAlarm(alarmCount, alarmName.text.toString())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ saveButton.isEnabled = true },
                                    { error -> Log.e("meow", "Unable to update username", error) }))
                })
    }

}
