package com.xvlaze.clover.services

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.xvlaze.clover.BuildConfig
import com.xvlaze.clover.R
import com.xvlaze.clover.model.AlarmPool
import com.xvlaze.clover.ui.ConsumeDialog
import com.xvlaze.clover.ui.FarmaciaDialog
import com.xvlaze.clover.util.Constants
import com.xvlaze.clover.util.MyApplication
import com.xvlaze.clover.watch.Watch
import timber.log.Timber
import kotlin.random.Random

object Reminder {
    private var alarmId = -1
    private lateinit var remoteViews: RemoteViews

    // FIXME: Se confunde cuando hay 2 tratamientos a la vez.
    fun throwRegular(context: Context,
                     numeroDosis: Int,
                     nombreTratamiento: String?,
                     alarmId: Int,
                     cantidadPaquete: Int) {
        remoteViews = RemoteViews(context.packageName, R.layout.not_time)
        remoteViews.setTextViewText(R.id.title, context.getString(R.string.not_take))
        remoteViews.setTextViewText(
            R.id.subtitle,
            context.resources.getQuantityString(
                R.plurals.not_desc,
                numeroDosis,
                numeroDosis,
                nombreTratamiento
            )
        )

        this.alarmId = alarmId

        val consumeIntent = Intent(context, ConsumeDialog::class.java).apply {
            putExtra("alarmId", alarmId)
            putExtra("nombreTratamiento", nombreTratamiento)
            putExtra("cantidadDosis", numeroDosis)
            putExtra("cantidadPaquete", cantidadPaquete)
            flags = (Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        val notifyPendingIntent =
            PendingIntent.getActivity(context, Random.nextInt(), consumeIntent,
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    }
                    else -> PendingIntent.FLAG_IMMUTABLE
                }
            )

        if (BuildConfig.DEBUG) {
            Timber.tag("${Constants.TAG} ${this.javaClass.simpleName}").d("Lanzando recordatorio NORMAL")
            Timber.tag("${Constants.TAG} ${this.javaClass.simpleName}").d("Tratamiento a procesar: $nombreTratamiento")
        }

        if (nombreTratamiento != null) {
            AlarmPool.instance.pushNotification(nombreTratamiento, alarmId)
        }
        AlarmPool.instance.setNotification()
        notify(context, notifyPendingIntent)
        Watch.sendReminder(context, nombreTratamiento!!, numeroDosis, alarmId)
    }

    fun throwStock(context: Context,
                   numeroDosis: Int,
                   nombreTratamiento: String?,
                   remaining: Int,
                   alarmId: Int,
                   cantidadPaquete: Int) {
        val header: String
        this.alarmId = alarmId

        // TODO: Esto no está del todo actualizado y no sé por qué. Acceder a isRunOut cuando mTreatmentRestantes = 0 da false. Hay que acceder a la propiedad en sí.
        if (remaining <= 0) {
            // Se ha acabado la dosis y hay que reponer YA.
            header = context.getString(R.string.not_ran_out, nombreTratamiento)
            remoteViews = RemoteViews(context.packageName, R.layout.not_danger)
        } else {
            // Se está a punto de acabar.
            header = context.getString(R.string.not_nearly_ran_out, nombreTratamiento)
            remoteViews = RemoteViews(context.packageName, R.layout.not_warn)
        }
        remoteViews.setTextViewText(R.id.title, header)

        val searchPharmaIntent = Intent(context, FarmaciaDialog::class.java). apply {
            putExtra("alarmId", alarmId)
            putExtra("nombreTratamiento", nombreTratamiento)
            putExtra("cantidadDosis", numeroDosis)
            putExtra("cantidadPaquete", cantidadPaquete)
            flags = (Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        // Create the PendingIntent
        val notifyPendingIntent = PendingIntent.getActivity(
            context,
            Random.nextInt(),
            searchPharmaIntent,
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                }
                else -> PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        if (BuildConfig.DEBUG) {
            Timber.tag("${Constants.TAG} ${this.javaClass.simpleName}").d("Lanzando recordatorio de STOCK")
            Timber.tag("${Constants.TAG} ${this.javaClass.simpleName}").d("Tratamiento a procesar: $nombreTratamiento")
        }

        if (nombreTratamiento != null) {
            AlarmPool.instance.pushNotification(nombreTratamiento, alarmId)
        }
        AlarmPool.instance.setNotification()
        notify(context, notifyPendingIntent)
        // TODO: No vamos a mandar mensajes de stock si no es tras tomar un tratamiento. Para qué? Si no podemos hacer nada sin el teléfono.
    }

    private fun notify(context: Context, notifyPendingIntent: PendingIntent) {
        MyApplication.getApp()!!.notificationManager.notify(
            alarmId,
            Notification.Builder(context, Constants.PRIMARY_CHANNEL_ID)
                .setSmallIcon(R.drawable.idle_1)
                .setCustomContentView(remoteViews)
                .setOngoing(true)
                .setContentIntent(notifyPendingIntent)
                .setAutoCancel(false).build()
        )
    }
}