package ru.you11.alarmclock

import android.arch.lifecycle.ViewModelProviders
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import com.crashlytics.android.Crashlytics
import io.reactivex.disposables.CompositeDisposable
import ru.you11.alarmclock.database.AlarmViewModel
import ru.you11.alarmclock.database.Injection
import ru.you11.alarmclock.database.ViewModuleFactory

class MainActivity : AppCompatActivity() {

    private lateinit var viewModelFactory: ViewModuleFactory.ViewModelFactory
    lateinit var viewModel: AlarmViewModel
    val disposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        setContentView(R.layout.activity_main)

        viewModelFactory = Injection.provideViewModelFactory(this)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(AlarmViewModel::class.java)

        supportFragmentManager.beginTransaction()
                .add(R.id.empty_fragment_container, AlarmsListFragment())
                .commit()
    }

    override fun onStop() {
        super.onStop()

        // clear all the subscription
        disposable.clear()
    }
}
