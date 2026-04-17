package com.jarvis.ai

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.room.Room
import com.jarvis.ai.data.AppDatabase
import com.jarvis.ai.llm.LLMEngine
import com.jarvis.ai.audio.TTSEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class JarvisApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "jarvis_db")
            .fallbackToDestructiveMigration()
            .build()
    }

    val llmEngine by lazy { LLMEngine(this) }
    val ttsEngine by lazy { TTSEngine(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_JARVIS,
                    "JARVIS Assistant",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "JARVIS running in background"
                    setShowBadge(false)
                }
            )

            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_DOWNLOAD,
                    "Model Downloads",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "AI model download progress"
                }
            )
        }
    }

    companion object {
        const val CHANNEL_JARVIS = "jarvis_service"
        const val CHANNEL_DOWNLOAD = "model_download"
        lateinit var instance: JarvisApplication
            private set
    }
}
