package com.lingualink.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LinguaLinkApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val serviceChannel = NotificationChannel(
            CHANNEL_SERVICE,
            "翻译服务",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "保持翻译服务在后台运行"
        }

        val updateChannel = NotificationChannel(
            CHANNEL_UPDATE,
            "应用更新",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "应用更新通知"
        }

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(serviceChannel)
        nm.createNotificationChannel(updateChannel)
    }

    companion object {
        const val CHANNEL_SERVICE = "translation_service"
        const val CHANNEL_UPDATE = "app_update"
    }
}
