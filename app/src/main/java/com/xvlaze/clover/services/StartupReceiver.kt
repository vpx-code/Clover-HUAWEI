package com.xvlaze.clover.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.xvlaze.clover.BuildConfig
import com.xvlaze.clover.model.AlarmPool
import com.xvlaze.clover.model.TreatmentsSource
import com.xvlaze.clover.util.Constants.TAG
import com.xvlaze.clover.watch.Watch
import timber.log.Timber

class StartupReceiver : BroadcastReceiver() {

    override fun onReceive(c: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            Toast.makeText(c, "Restarting Clover Engine...", Toast.LENGTH_LONG).show()
            if (BuildConfig.DEBUG) {
                Timber.tag(TAG).d("Boot receiver on!")
                Timber.tag(TAG).d("Restarting Clover Engine...")
            }
            TreatmentsSource.checkStock(c)
            TreatmentsSource.restoreAlarms(c)
            AlarmPool.instance.sync(TreatmentsSource.readTreatmentsFromJson(c)) // TODO: No va. revisar el tema de la lista vacía
            AlarmPool.instance.setNotification()
            Watch.checkDevices()
        }
    }
}