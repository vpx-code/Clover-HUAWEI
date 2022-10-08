package com.xvlaze.clover.util

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

// Efecto de máquina de escribir para los diálogos. Clase personalizada de TextView.
class TypeWriter : AppCompatTextView {
    private val mHandler = Handler()
    private var mText: CharSequence? = null
    private var mIndex = 0
    private var mDelay = 0.00005.toLong() // in ms
    private val characterAdder: Runnable = object : Runnable {
        override fun run() {
            text = mText!!.subSequence(0, mIndex++)
            if (mIndex <= mText!!.length) {
                mHandler.postDelayed(this, mDelay)
            }
        }
    }

    constructor(context: Context?) : super(context!!) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    ) {
    }

    fun animateText(txt: CharSequence?) {
        mText = txt
        mIndex = 0
        text = ""
        mHandler.removeCallbacks(characterAdder)
        mHandler.postDelayed(characterAdder, mDelay)
    }

    fun setCharacterDelay(m: Long) {
        mDelay = m
    }
}