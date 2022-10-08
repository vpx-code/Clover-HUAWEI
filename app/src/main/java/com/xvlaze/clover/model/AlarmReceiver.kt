package com.xvlaze.clover.model

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xvlaze.clover.BuildConfig
import com.xvlaze.clover.services.Reminder
import com.xvlaze.clover.util.Constants.TAG
import timber.log.Timber

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) { // Este intent...?
        if (BuildConfig.DEBUG) {
            Timber.tag("$TAG ${this.javaClass.simpleName}")
            Timber.d("¡AlarmReceiver activado!")

        }

        val list = TreatmentsSource.readTreatmentsFromJson(c) // Necesito esto porque si el usuario cierra la app del multitarea no reprograma más.
        when {
            AlarmPool.instance.isAlarmQueueEmpty -> {
                AlarmPool.instance.sync(list)
            }
            else -> {
                AlarmPool.instance.crearCola()
            }
        }

        //val notificationServiceIntent = Intent(c, NotificationService::class.java)

        val numeroDosis: Int = intent.getIntExtra("cantidadDosis", 0)
        //notificationServiceIntent.putExtra("numeroDosis", numeroDosis)

        val nombreTratamiento: String? = intent.getStringExtra("nombreTratamiento")
        //notificationServiceIntent.putExtra("nombreTratamiento", nombreTratamiento)

        //notificationServiceIntent.putExtra("cantidadPaquete", intent.getIntExtra("cantidadPaquete", 0))

        val alarmId: Int = intent.getIntExtra("alarmId", -1)
        //notificationServiceIntent.putExtra("alarmId", alarmId)

        if (BuildConfig.DEBUG) {
            Timber.tag("$TAG ${this.javaClass.simpleName}").d("Tratamiento a procesar: $nombreTratamiento")
        }

        Reminder.throwRegular(c,
            numeroDosis,
            nombreTratamiento,
            alarmId,
            intent.getIntExtra("cantidadPaquete", 0)
        )
    }
}