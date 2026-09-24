package com.mroldl001.mimochat.service
import com.mroldl001.mimochat.R
import android.content.Context
import com.mroldl001.mimochat.data.preferences.PreferencesManager
import com.mroldl001.mimochat.ui.settings.AppLocale

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
        const val EXTRA_CHAT_ID = "com.mroldl001.mimochat.extra.CHAT_ID"
    }

    private val binder = LocalBinder()
    private lateinit var notificationManager: NotificationManager
    private var currentNotificationText = ""
    private var currentChatId: Long = 0L

    /** 按用户选择语言包装的 Context，确保通知文本也随语言切换。 */
    private val localizedContext: Context
        get() = AppLocale.wrap(this, PreferencesManager(this).getAppLanguage())

    inner class LocalBinder : Binder() {
        fun getService(): ChatService = this@ChatService
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        currentNotificationText = localizedContext.getString(R.string.notif_chat_reply)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentChatId = intent.getLongExtra(EXTRA_CHAT_ID, 0L)
                currentNotificationText = localizedContext.getString(R.string.notif_chat_reply)
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
            localizedContext.getString(R.string.notif_chat_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = localizedContext.getString(R.string.notif_chat_channel_desc)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                putExtra(EXTRA_CHAT_ID, currentChatId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= 36) {
            val style = Notification.ProgressStyle()
                .setStyledByProgress(false)
                .setProgress(1)
            return Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("MChat")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_send)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setStyle(style)
                .build()
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MChat")
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

        return notification
    }
}
