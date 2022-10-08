package com.xvlaze.clover.util

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.view.View
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import com.huawei.wearengine.p2p.Message
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*


object Extensions {
    fun View.disable() {
        @SuppressWarnings("deprecation")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            background.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(Color.GRAY, BlendModeCompat.LUMINOSITY)
        }
        else {
            background.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC)
        }
        isClickable = false
        isEnabled = false
    }

    fun View.enable() {
        background.colorFilter = null
        isClickable = true
        isEnabled = true
    }

    fun Message.toPlainText(charset: Charset = StandardCharsets.UTF_8): String =
        charset.decode(ByteBuffer.wrap(this.data)).toString()

    fun Long.toMinutesEpoch(): Long {
        return this / 1000 / 60
    }

    @SuppressLint("SimpleDateFormat")
    fun Long.trimMinute(): Long {
        val d = Date(this)
        d.seconds = 0
        return d.time
    }
}