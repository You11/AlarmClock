package ru.you11.alarmclock

import android.app.AlarmManager
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Switch
import android.widget.TextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlin.math.acos

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
        view?.findViewById<Button>(R.id.all_alarms_add_button)?.apply {
            setOnClickListener {
                startFragment(activity, null)
            }
        }
    }

    private fun setupStopButton() {
        view?.findViewById<Button>(R.id.all_alarms_stop_button)?.apply {
            setOnClickListener { _ ->
                val alarmManager = activity.getSystemService(android.content.Context.ALARM_SERVICE) as AlarmManager
                activity.disposable.add(activity.viewModel.getAlarmList()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            for (alarm in it) {
                                Utils().stopAlarm(alarm.aid!!, alarmManager, activity)
                            }

                            Log.d("alarm", "alarms stopped")
                        })
            }
        }
    }

    private fun setupDeleteButton() {
        view?.findViewById<Button>(R.id.all_alarms_delete_button)?.apply {
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
        val rvManager = LinearLayoutManager(activity)
        val rvAdapter = AlarmsRWAdapter(alarms)

        loadAlarmsIntoRV(rvAdapter)

        view?.findViewById<RecyclerView>(R.id.all_alarms_recycler_view)?.apply {
            addItemDecoration(DividerItemDecoration(context, rvManager.orientation))
            layoutManager = rvManager
            adapter = rvAdapter
        }


    }

    private fun loadAlarmsIntoRV(rvAdapter: AlarmsRWAdapter): ArrayList<Alarm> {

        activity.disposable.add(activity.viewModel.getAlarmList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    alarms.clear()
                    alarms.addAll(it)
                    rvAdapter.notifyDataSetChanged()
                })

        return alarms
    }
}


class AlarmsRWAdapter(private val alarms: ArrayList<Alarm>): RecyclerView.Adapter<AlarmsRWAdapter.ViewHolder> () {


    class ViewHolder(val layout: RelativeLayout) : RecyclerView.ViewHolder(layout) {
        val name: TextView = layout.findViewById(R.id.alarm_card_name_text_view)
        val time: TextView = layout.findViewById(R.id.alarm_card_time_text_view)
        val switch: Switch = layout.findViewById(R.id.alarm_card_switch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmsRWAdapter.ViewHolder {

        val layout = LayoutInflater.from(parent.context)
                .inflate(R.layout.alarm_recycler_view_card, parent, false) as RelativeLayout

        return ViewHolder(layout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val alarm = alarms[position]
        val utils = Utils()

        holder.name.text = alarm.name
        holder.time.text = utils.getAlarmTime(alarm.hours, alarm.minutes)

        //turn alarm off/on
        setupSwitch(holder, utils, alarm)

        //edit alarm
        holder.layout.setOnClickListener {
            val activity = it.context as MainActivity

            //TODO: this is horrible, put in parcelable
            val arguments = Bundle()
            arguments.putInt("alarmId", alarm.aid!!)
            arguments.putString("alarmName", alarm.name)
            arguments.putInt("alarmHour", alarm.hours)
            arguments.putInt("alarmMinute", alarm.minutes)

            startFragment(activity, arguments)
        }
    }

    private fun setupSwitch(holder: ViewHolder, utils: Utils, alarm: Alarm) {
        if (alarm.isOn) holder.switch.isChecked = true

        holder.switch.setOnCheckedChangeListener { buttonView, isChecked ->
            val activity = buttonView.context as MainActivity
            if (isChecked) {
                //turn on alarm
                buttonView.isEnabled = false
                utils.setupAlarm(alarm, activity)
                utils.createNotification(alarm, activity)
                activity.disposable.add(activity.viewModel.updateAlarmStatus(alarm.aid!!, true)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            buttonView.isEnabled = true
                        })
            } else {
                //turn off alarm
                buttonView.isEnabled = false
                utils.stopAlarm(alarm.aid!!, activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager, activity)
                utils.dismissNotification(activity)
                activity.disposable.add(activity.viewModel.updateAlarmStatus(alarm.aid!!, false)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            buttonView.isEnabled = true
                        })
            }
        }
    }

    override fun getItemCount(): Int {
        return alarms.size
    }
}

private fun startFragment(activity: AppCompatActivity, arguments: Bundle?) {
    val fragment = AlarmSetupFragment()

    if (arguments != null) fragment.arguments = arguments

    activity.supportFragmentManager.beginTransaction()
            .replace(R.id.empty_fragment_container, fragment)
            .addToBackStack("AlarmsListFragment")
            .commit()
}