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
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.mroldl001.mimochat.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class UpdateDownloadService : Service() {
    companion object {
        const val ACTION_DOWNLOAD = "com.mroldl001.mimochat.action.DOWNLOAD_UPDATE"
        const val EXTRA_DOWNLOAD_URL = "download_url"
        const val EXTRA_VERSION_NAME = "version_name"
        private const val CHANNEL_ID = "app_update_download"
        private const val NOTIFICATION_ID = 2001
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var notificationManager: NotificationManager
    private var downloadJob: Job? = null

    /** 按用户选择语言包装的 Context，确保更新通知文本也随语言切换。 */
    private val localizedContext: Context
        get() = AppLocale.wrap(this, PreferencesManager(this).getAppLanguage())

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, localizedContext.getString(R.string.notif_update_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
                description = localizedContext.getString(R.string.notif_update_channel_desc)
                setShowBadge(false)
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_DOWNLOAD || downloadJob?.isActive == true) return START_NOT_STICKY
        val url = intent.getStringExtra(EXTRA_DOWNLOAD_URL) ?: return START_NOT_STICKY
        val version = intent.getStringExtra(EXTRA_VERSION_NAME).orEmpty()
        startForeground(NOTIFICATION_ID, buildNotification(version, 0, false))
        downloadJob = serviceScope.launch { downloadAndInstall(url, version) }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun downloadAndInstall(downloadUrl: String, version: String) {
        val updateDir = File(filesDir, "updates").apply { mkdirs() }
        val safeVersion = version.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val apkFile = File(updateDir, "MChat-$safeVersion.apk")
        var downloadCompleted = false
        try {
            val connection = URL(downloadUrl).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("User-Agent", "MChat")
            connection.connect()
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("HTTP ${connection.responseCode}")
            }
            val totalBytes = connection.contentLengthLong
            connection.inputStream.use { input ->
                apkFile.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L
                    var lastProgress = -1
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloaded += count
                        val progress = if (totalBytes > 0) {
                            ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
                        } else 0
                        if (progress != lastProgress) {
                            lastProgress = progress
                            notificationManager.notify(
                                NOTIFICATION_ID,
                                buildNotification(version, progress, totalBytes <= 0)
                            )
                        }
                    }
                }
            }
            connection.disconnect()
            downloadCompleted = true
            notificationManager.notify(NOTIFICATION_ID, buildCompleteNotification(version, apkFile))
            runCatching { openInstaller(apkFile) }
        } catch (error: Exception) {
            if (!downloadCompleted) apkFile.delete()
            notificationManager.notify(NOTIFICATION_ID, buildFailureNotification(error.message))
        } finally {
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun buildNotification(version: String, progress: Int, indeterminate: Boolean): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= 36) {
            val style = Notification.ProgressStyle()
                .setStyledByProgress(true)
                .setProgress(progress)
            return Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(localizedContext.getString(R.string.notif_downloading, version))
                .setContentText(if (indeterminate) localizedContext.getString(R.string.notif_downloading_indeterminate) else "$progress%")
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setStyle(style)
                .build()
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(localizedContext.getString(R.string.notif_downloading, version))
            .setContentText(if (indeterminate) localizedContext.getString(R.string.notif_downloading_indeterminate) else "$progress%")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setProgress(100, progress, indeterminate)
            .build()
    }

    private fun buildCompleteNotification(version: String, apkFile: File): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(localizedContext.getString(R.string.notif_download_complete, version))
            .setContentText(localizedContext.getString(R.string.notif_install_update))
            .setAutoCancel(true)
            .setContentIntent(installerPendingIntent(apkFile))
            .build()
    }

    private fun buildFailureNotification(message: String?): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(localizedContext.getString(R.string.notif_update_failed))
            .setContentText(message ?: localizedContext.getString(R.string.notif_retry_later))
            .setAutoCancel(true)
            .build()
    }

    private fun installerIntent(apkFile: File) = Intent(Intent.ACTION_VIEW).apply {
        val uri = FileProvider.getUriForFile(
            this@UpdateDownloadService,
            "$packageName.fileprovider",
            apkFile
        )
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun installerPendingIntent(apkFile: File): PendingIntent = PendingIntent.getActivity(
        this, 1, installerIntent(apkFile),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun openInstaller(apkFile: File) {
        startActivity(installerIntent(apkFile))
    }
}
