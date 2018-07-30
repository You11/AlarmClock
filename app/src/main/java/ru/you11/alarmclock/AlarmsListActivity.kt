package ru.you11.alarmclock

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class AlarmsListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarms_list)

        setupAddButton()
        setupRecyclerView()
    }

    private fun setupAddButton() {
        val addAlarmButton = findViewById<Button>(R.id.all_alarms_add_button).apply {
            setOnClickListener {
                it.context?.startActivity(Intent(it.context, AlarmSetupActivity::class.java))
            }
        }
    }

    private fun setupRecyclerView() {
        //testData
        val alarmNames = createFakeDataForAlarm()

        val rvManager = LinearLayoutManager(this)
        val rvAdapter = AlarmsRWAdapter(alarmNames)
        val recyclerView = findViewById<RecyclerView>(R.id.all_alarms_recycler_view).apply {
            layoutManager = rvManager
            adapter = rvAdapter
        }
    }

    private fun createFakeDataForAlarm(): ArrayList<String> {
        val alarmNames = ArrayList<String>()
        alarmNames.add("alarm")
        alarmNames.add("alarm2")
        alarmNames.add("alarm3")

        return alarmNames
    }
}


class AlarmsRWAdapter(private val alarmNames: ArrayList<String>): RecyclerView.Adapter<AlarmsRWAdapter.ViewHolder> () {


    class ViewHolder(val layout: LinearLayout) : RecyclerView.ViewHolder(layout) {
        val alarmName: TextView = layout.findViewById(R.id.alarm_name_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmsRWAdapter.ViewHolder {

        val layout = LayoutInflater.from(parent.context)
                .inflate(R.layout.alarm_recycler_view_card, parent, false) as LinearLayout

        return ViewHolder(layout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.alarmName.text = alarmNames[position]
        holder.alarmName.setOnClickListener {
            it.context?.startActivity(Intent(it.context, AlarmSetupActivity::class.java))
        }
    }

    override fun getItemCount(): Int {
        return alarmNames.size
    }
}
