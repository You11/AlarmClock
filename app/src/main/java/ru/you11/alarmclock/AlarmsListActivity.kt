package ru.you11.alarmclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class AlarmsListActivity : AppCompatActivity() {

    private lateinit var viewModelFactory: ViewModuleFactory.ViewModelFactory
    private lateinit var viewModel: AlarmViewModel
    private val disposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarms_list)

        viewModelFactory = Injection.provideViewModelFactory(this)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(AlarmViewModel::class.java)

        setupAddButton()
        setupStopButton()
        setupRecyclerView()
    }

    private fun setupAddButton() {
        val addAlarmButton = findViewById<Button>(R.id.all_alarms_add_button).apply {
            setOnClickListener {
                it.context?.startActivity(Intent(it.context, AlarmSetupActivity::class.java))
            }
        }
    }

    private fun setupStopButton() {
        val stopButton = findViewById<Button>(R.id.all_alarms_stop_button).apply {
            setOnClickListener {
                val alarmManager = getSystemService(android.content.Context.ALARM_SERVICE) as AlarmManager
                val alarmIntent = PendingIntent.getBroadcast(context, 120, Intent(context, AlarmReceiver::class.java), 0)
                alarmManager.cancel(alarmIntent)
                Log.d("alarm", "alarm stopped")
            }
        }
    }

    private fun setupRecyclerView() {
        //testData

        val alarmNames = ArrayList<Alarm>()

        val rvManager = LinearLayoutManager(this)
        val rvAdapter = AlarmsRWAdapter(alarmNames)

        createFakeDataForAlarm(alarmNames, rvAdapter)

        val recyclerView = findViewById<RecyclerView>(R.id.all_alarms_recycler_view).apply {
            layoutManager = rvManager
            adapter = rvAdapter
        }


    }

    private fun createFakeDataForAlarm(alarms: ArrayList<Alarm>, rvAdapter: AlarmsRWAdapter): ArrayList<Alarm> {

        disposable.add(viewModel.getAlarmList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    alarms.addAll(it)
                    rvAdapter.notifyDataSetChanged()
                })

        return alarms
    }
}


class AlarmsRWAdapter(private val alarms: ArrayList<Alarm>): RecyclerView.Adapter<AlarmsRWAdapter.ViewHolder> () {


    class ViewHolder(val layout: LinearLayout) : RecyclerView.ViewHolder(layout) {
        val name: TextView = layout.findViewById(R.id.alarm_name_text_view)
        val time: TextView = layout.findViewById(R.id.alarm_time_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmsRWAdapter.ViewHolder {

        val layout = LayoutInflater.from(parent.context)
                .inflate(R.layout.alarm_recycler_view_card, parent, false) as LinearLayout

        return ViewHolder(layout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.name.text = alarms[position].name
        holder.time.text = alarms[position].hours.toString() + "hours," + alarms[position].minutes.toString() + "minutes"
        holder.name.setOnClickListener {
            it.context?.startActivity(Intent(it.context, AlarmSetupActivity::class.java))
        }
    }

    override fun getItemCount(): Int {
        return alarms.size
    }
}
