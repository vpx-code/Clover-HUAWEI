package com.xvlaze.clover.util

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.xvlaze.clover.BuildConfig
import timber.log.Timber.DebugTree
import timber.log.Timber.Forest.plant


class MyApplication: Application() {
    lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        app = this

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val primaryChannel = NotificationChannel(
            Constants.PRIMARY_CHANNEL_ID,
            "Clover's Main Notification Channel",
            NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Clover's Notification Channel for Treatments."
        }

        notificationManager.createNotificationChannel(primaryChannel)

        // Start HUAWEI Watch.
        com.xvlaze.clover.watch.Watch

        if (BuildConfig.DEBUG) {
            plant(DebugTree())
        }
    }

    companion object {
        private var app: MyApplication? = null

        fun getApp(): MyApplication? {
            return app
        }
    }
}