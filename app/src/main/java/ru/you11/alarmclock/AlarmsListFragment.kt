package ru.you11.alarmclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class AlarmsListFragment: Fragment() {

    private lateinit var activity: MainActivity
    private val alarms = ArrayList<Alarm>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = this.getActivity() as MainActivity

        return inflater.inflate(R.layout.fragment_alarms_list, container, false)
    }

    override fun onResume() {
        super.onResume()

        setupAddButton()
        setupStopButton()
        setupDeleteButton()
        setupRecyclerView()
    }

    private fun setupAddButton() {
        val addAlarmButton = view?.findViewById<Button>(R.id.all_alarms_add_button)?.apply {
            setOnClickListener {
                startFragment(activity)
            }
        }
    }

    private fun setupStopButton() {
        val stopButton = view?.findViewById<Button>(R.id.all_alarms_stop_button)?.apply {
            setOnClickListener {
                val alarmManager = activity.getSystemService(android.content.Context.ALARM_SERVICE) as AlarmManager
                val alarmIntent = PendingIntent.getBroadcast(context, 120, Intent(context, AlarmReceiver::class.java), 0)
                alarmManager.cancel(alarmIntent)
                Log.d("alarm", "alarm stopped")
            }
        }
    }

    private fun setupDeleteButton() {
        val deleteButton = view?.findViewById<Button>(R.id.all_alarms_delete_button)?.apply {
            setOnClickListener {
                activity.disposable.add(activity.viewModel.deleteAllAlarms()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            alarms.clear()
                            view?.findViewById<RecyclerView>(R.id.all_alarms_recycler_view)?.adapter?.notifyDataSetChanged()
                        })
            }
        }
    }

    private fun setupRecyclerView() {
        //testData
        val rvManager = LinearLayoutManager(activity)
        val rvAdapter = AlarmsRWAdapter(alarms)

        createFakeDataForAlarm(rvAdapter)

        val recyclerView = view?.findViewById<RecyclerView>(R.id.all_alarms_recycler_view)?.apply {
            layoutManager = rvManager
            adapter = rvAdapter
        }


    }

    private fun createFakeDataForAlarm(rvAdapter: AlarmsRWAdapter): ArrayList<Alarm> {

        activity.disposable.add(activity.viewModel.getAlarmList()
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
        holder.time.text = alarms[position].hours.toString() + ":" + alarms[position].minutes.toString()
        holder.name.setOnClickListener {
            val activity = it.context as MainActivity
            startFragment(activity)
        }
    }

    override fun getItemCount(): Int {
        return alarms.size
    }
}

private fun startFragment(activity: AppCompatActivity) {
    activity.supportFragmentManager.beginTransaction()
            .replace(R.id.empty_fragment_container, AlarmSetupFragment())
            .addToBackStack("AlarmsListFragment")
            .commit()
}