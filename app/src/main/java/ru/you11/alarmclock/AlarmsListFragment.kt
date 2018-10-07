package ru.you11.alarmclock

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*

class AlarmsListFragment: Fragment() {

    private lateinit var activity: MainActivity
    private val alarms = ArrayList<Alarm>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = this.getActivity() as MainActivity

        return inflater.inflate(R.layout.fragment_alarms_list, container, false)
    }

    override fun onResume() {
        super.onResume()

        setupRecyclerView()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.main_activity_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_activity_main_add -> {
                startAlarmSetupFragment(activity, null)
            }

            R.id.menu_activity_main_settings -> {
                startActivity(Intent(activity, SettingsActivity::class.java))
            }
        }

        return super.onOptionsItemSelected(item)
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
                .subscribe { list ->
                    alarms.clear()
                    alarms.addAll(list.sortedWith(compareBy({ it.hours }, { it.minutes })))
                    rvAdapter.notifyDataSetChanged()
                })

        return alarms
    }
}


class AlarmsRWAdapter(private val allAlarms: ArrayList<Alarm>): RecyclerView.Adapter<AlarmsRWAdapter.ViewHolder> () {


    class ViewHolder(val layout: RelativeLayout) : RecyclerView.ViewHolder(layout) {
        val name: TextView = layout.findViewById(R.id.alarm_card_name_text_view)
        val time: TextView = layout.findViewById(R.id.alarm_card_time_text_view)
        val switch: Switch = layout.findViewById(R.id.alarm_card_switch)
        val days: TextView = layout.findViewById(R.id.alarm_card_days_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmsRWAdapter.ViewHolder {

        val layout = LayoutInflater.from(parent.context)
                .inflate(R.layout.alarm_recycler_view_card, parent, false) as RelativeLayout

        return ViewHolder(layout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val alarm = allAlarms[position]

        setupAlarmNameText(holder.name, alarm)
        setupAlarmTimeText(holder.time, alarm)
        setupAlarmSwitch(holder.switch, alarm, position)
        setupAlarmDaysText(holder.days, alarm)

        //edit alarm
        holder.layout.setOnClickListener {
            val activity = it.context as MainActivity
            val arguments = Bundle()
            arguments.putParcelable("alarm", alarm)

            startAlarmSetupFragment(activity, arguments)
        }
    }

    private fun setupAlarmNameText(name: TextView, alarm: Alarm) {
        if (alarm.name.isNotBlank()) {
            name.text = alarm.name
            name.visibility = TextView.VISIBLE
        }
    }

    private fun setupAlarmTimeText(time: TextView, alarm: Alarm) {
        time.text = Utils.getAlarmTimeDescription(alarm.hours, alarm.minutes)
    }

    private fun setupAlarmSwitch(switch: Switch, alarm: Alarm, position: Int) {

        if (alarm.isOn) switch.isChecked = true

        switch.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.isEnabled = false
            val activity = buttonView.context as MainActivity
            if (isChecked) {
                turnOnAlarm(activity, alarm, position)
            } else {
                turnOffAlarm(activity, alarm, position)
            }
            buttonView.isEnabled = true
        }
    }

    private fun setupAlarmDaysText(days: TextView, alarm: Alarm) {
        var daysText = ""

        val resources = MainApp.applicationContext().resources
        val daysToAbbr = hashMapOf<String, String>(
                resources.getString(R.string.alarm_days_monday) to resources.getString(R.string.alarm_setup_days_monday_text_view),
                resources.getString(R.string.alarm_days_tuesday) to resources.getString(R.string.alarm_setup_days_tuesday_text_view),
                resources.getString(R.string.alarm_days_wednesday) to resources.getString(R.string.alarm_setup_days_wednesday_text_view),
                resources.getString(R.string.alarm_days_thursday) to resources.getString(R.string.alarm_setup_days_thursday_text_view),
                resources.getString(R.string.alarm_days_friday) to resources.getString(R.string.alarm_setup_days_friday_text_view),
                resources.getString(R.string.alarm_days_saturday) to resources.getString(R.string.alarm_setup_days_saturday_text_view),
                resources.getString(R.string.alarm_days_sunday) to resources.getString(R.string.alarm_setup_days_sunday_text_view))

        alarm.days.forEach {
            if (it.value) {
                daysText += daysToAbbr[it.key]
                daysText += ", "
            }
        }
        daysText = daysText.dropLast(2)

        days.text = daysText
    }

    private fun turnOnAlarm(activity: MainActivity, alarm: Alarm, positionInRV: Int) {
        activity.disposable.add(activity.viewModel.updateAlarmStatus(alarm.aid, true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    allAlarms[positionInRV].isOn = true
                    Utils.setAlarmWithDays(alarm, activity)
                    Utils.updateAlarmNotification(allAlarms, activity)
                })
    }

    private fun turnOffAlarm(activity: MainActivity, alarm: Alarm, positionInRV: Int) {
        activity.disposable.add(activity.viewModel.updateAlarmStatus(alarm.aid, false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    allAlarms[positionInRV].isOn = false
                    Utils.stopAlarm(alarm.aid, activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager, activity)
                    Utils.updateAlarmNotification(allAlarms, activity)
                })
    }

    override fun getItemCount(): Int {
        return allAlarms.size
    }
}

private fun startAlarmSetupFragment(activity: AppCompatActivity, arguments: Bundle?) {
    val fragment = AlarmSetupFragment()

    if (arguments != null) fragment.arguments = arguments

    activity.supportFragmentManager.beginTransaction()
            .replace(R.id.empty_fragment_container, fragment)
            .addToBackStack("AlarmsListFragment")
            .commit()
}