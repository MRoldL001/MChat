package com.mroldl001.mimochat.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mroldl001.mimochat.MainActivity

class ChatService : Service() {

    companion object {
        const val CHANNEL_ID = "chat_service_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.mroldl001.mimochat.action.START_CHAT_SERVICE"
        const val ACTION_STOP = "com.mroldl001.mimochat.action.STOP_CHAT_SERVICE"
        const val ACTION_UPDATE_NOTIFICATION = "com.mroldl001.mimochat.action.UPDATE_NOTIFICATION"
        const val EXTRA_NOTIFICATION_TEXT = "com.mroldl001.mimochat.extra.NOTIFICATION_TEXT"
    }

    private val binder = LocalBinder()
    private lateinit var notificationManager: NotificationManager
    private var currentNotificationText = "MiMo正在回复你"

    inner class LocalBinder : Binder() {
        fun getService(): ChatService = this@ChatService
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val notification = createNotification(currentNotificationText)
                startForeground(NOTIFICATION_ID, notification)
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_UPDATE_NOTIFICATION -> {
                val text = intent.getStringExtra(EXTRA_NOTIFICATION_TEXT) ?: currentNotificationText
                updateNotification(text)
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun updateNotificationText(text: String) {
        currentNotificationText = text
        updateNotification(text)
    }

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "对话服务",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "保持对话在进行中"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MiMo Chat")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setProgress(0, 0, true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            builder.setOnlyAlertOnce(true)
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }

        val notification = builder.build()

        if (Build.VERSION.SDK_INT >= 36) {
            try {
                val liveUpdateField = Notification::class.java.getDeclaredField("liveUpdate")
                liveUpdateField.isAccessible = true
                liveUpdateField.setBoolean(notification, true)
            } catch (e: Exception) {
            }
        }

        return notification
    }
}
