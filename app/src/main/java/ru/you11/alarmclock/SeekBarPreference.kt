package ru.you11.alarmclock

import android.content.Context
import android.content.res.TypedArray
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar

class SeekBarPreference(context: Context, attributeSet: AttributeSet): Preference(context, attributeSet) {

    private var value = 0

    override fun onCreateView(parent: ViewGroup?): View {
        layoutResource = R.layout.preference_seekbar
        return super.onCreateView(parent)
    }

    override fun onBindView(view: View?) {
        super.onBindView(view)
        view?.findViewById<SeekBar>(R.id.preference_seekbar)?.apply {
            progress = value
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        setValue(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }

            })
        }
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        setValue(if (restorePersistedValue) getPersistedInt(value) else defaultValue as Int)
    }

    private fun setValue(value: Int) {
        this.value = value
        if (shouldPersist()) {
            persistInt(value)
        }
        summary = value.toString()
    }

    override fun onGetDefaultValue(a: TypedArray?, index: Int): Int? {
        return a?.getInt(index, 0)
    }
}