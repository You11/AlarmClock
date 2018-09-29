package ru.you11.alarmclock

import android.content.Context
import android.content.res.TypedArray
import android.preference.DialogPreference
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.NumberPicker
import android.view.Gravity
import android.view.ViewGroup

class NumberPickerPreference(context: Context, attributes: AttributeSet): DialogPreference(context, attributes) {

    private lateinit var picker: NumberPicker
    private var value = 0

    var maxValue = 50
    var minValue = 0

    override fun onCreateDialogView(): View {
        val layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams.gravity = Gravity.CENTER

        picker = NumberPicker(context)
        picker.layoutParams = layoutParams

        val dialogView = FrameLayout(context)
        dialogView.addView(picker)

        return dialogView
    }

    override fun onBindDialogView(view: View?) {
        super.onBindDialogView(view)
        picker.maxValue = maxValue
        picker.minValue = minValue
        picker.wrapSelectorWheel = true
        picker.value = value
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            picker.clearFocus()
            if (callChangeListener(picker.value)) {
                setValue(picker.value)
            }
        }
    }

    override fun onGetDefaultValue(a: TypedArray?, index: Int): Int? {
        return a?.getInt(index, minValue)
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        value = if (restorePersistedValue) {
            getPersistedInt(minValue)
        } else {
            defaultValue as Int
        }
        summary = value.toString()
    }

    private fun setValue(value: Int) {
        this.value = value
        if (shouldPersist()) {
            persistInt(value)
        }
        summary = value.toString()
        callChangeListener(value)
    }
}