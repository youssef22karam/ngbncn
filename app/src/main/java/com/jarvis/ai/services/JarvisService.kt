package com.jarvis.ai.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.jarvis.ai.JarvisApplication
import com.jarvis.ai.MainActivity
import com.jarvis.ai.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class JarvisService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null

    private val _isRunning = MutableStateFlow(false)

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "JARVIS Service created")
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundService()
            ACTION_STOP  -> stopSelf()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = buildNotification("JARVIS is active", "Listening in background...")
        startForeground(NOTIFICATION_ID, notification)
        _isRunning.value = true
        Log.i(TAG, "JARVIS foreground service started")
    }

    fun updateNotification(title: String, content: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(title, content))
    }

    private fun buildNotification(title: String, content: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, JarvisService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, JarvisApplication.CHANNEL_JARVIS)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_delete, "Stop", pendingStop)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "JarvisAI::WakeLock"
        ).apply { acquire(10 * 60 * 1000L) } // 10 min max
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        serviceScope.cancel()
        wakeLock?.release()
        Log.i(TAG, "JARVIS Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG             = "JarvisService"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.jarvis.ai.START"
        const val ACTION_STOP  = "com.jarvis.ai.STOP"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning

        fun start(context: Context) {
            val intent = Intent(context, JarvisService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, JarvisService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
