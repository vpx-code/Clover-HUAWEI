package com.xvlaze.clover.repository

import com.xvlaze.clover.model.AlarmPool
import com.xvlaze.clover.model.Treatment

class AlarmsRepository {
    fun syncAndSet(l: ArrayList<Treatment>) {
        AlarmPool.instance.sync(l)
        AlarmPool.instance.setNotification()
    }
}