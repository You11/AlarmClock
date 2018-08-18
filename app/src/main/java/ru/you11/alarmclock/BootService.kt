package ru.you11.alarmclock

import android.app.IntentService
import android.app.job.JobScheduler
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.support.v4.app.JobIntentService
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class BootService: JobIntentService() {

    private val JOB_ID = 100

    override fun onHandleWork(intent: Intent) {

        AlarmDatabase.getInstance(this).alarmDao().getAll().observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe { alarmList ->
                    alarmList.forEach {
                        if (it.isOn) {
                            Utils.setAlarm(it, this)
                        }

                        Utils.updateAlarmNotification(alarmList, this)
                    }
        }
    }

    fun enqueueWork(context: Context, work: Intent) {
        enqueueWork(context, BootService::class.java, JOB_ID, work)
    }
}