package com.jarvis.ai.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ModelType {
    MEDIAPIPE,   // .task files (Gemma, Phi via MediaPipe)
    GGUF,        // llama.cpp GGUF (future extension)
    ONNX         // ONNX Runtime models (future extension)
}

enum class ModelStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    LOADING,
    READY,
    ERROR
}

@Entity(tableName = "models")
data class ModelInfo(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val sizeLabel: String,          // e.g. "1.3 GB"
    val sizeMb: Long,               // approximate size in MB
    val downloadUrl: String,
    val localPath: String = "",
    val type: ModelType = ModelType.MEDIAPIPE,
    val status: ModelStatus = ModelStatus.NOT_DOWNLOADED,
    val downloadProgress: Int = 0,  // 0-100
    val parameters: String = "",    // e.g. "2B"
    val quantization: String = "",  // e.g. "INT4"
    val contextLength: Int = 2048,
    val isActive: Boolean = false,
    val badgeLabel: String = "",    // e.g. "Recommended", "Fast", "Powerful"
    val iconEmoji: String = "🤖",
    val requiresAuth: Boolean = false,
    val authNote: String = ""
)

// Predefined available models
object AvailableModels {
    val catalog = listOf(
        ModelInfo(
            id = "gemma2b_int4",
            name = "Gemma 2B",
            description = "Google's lightweight language model. Great balance of speed and quality. Runs on most modern phones.",
            sizeLabel = "~1.3 GB",
            sizeMb = 1300,
            downloadUrl = "https://huggingface.co/litert-community/Gemma2-2b-it-CPU-int4/resolve/main/gemma2-2b-it-cpu-int4.task",
            type = ModelType.MEDIAPIPE,
            parameters = "2B",
            quantization = "INT4",
            contextLength = 8192,
            badgeLabel = "Recommended",
            iconEmoji = "✨",
            requiresAuth = false
        ),
        ModelInfo(
            id = "gemma3_1b",
            name = "Gemma 3 1B",
            description = "Smallest Gemma model. Very fast responses, ideal for quick commands and phone control tasks.",
            sizeLabel = "~600 MB",
            sizeMb = 600,
            downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT-int4/resolve/main/gemma3-1b-it-int4.task",
            type = ModelType.MEDIAPIPE,
            parameters = "1B",
            quantization = "INT4",
            contextLength = 4096,
            badgeLabel = "Fastest",
            iconEmoji = "⚡",
            requiresAuth = false
        ),
        ModelInfo(
            id = "phi2",
            name = "Phi-2",
            description = "Microsoft's compact but powerful model. Excellent reasoning and code capabilities.",
            sizeLabel = "~1.5 GB",
            sizeMb = 1500,
            downloadUrl = "https://huggingface.co/litert-community/Phi-2/resolve/main/phi2-int4.task",
            type = ModelType.MEDIAPIPE,
            parameters = "2.7B",
            quantization = "INT4",
            contextLength = 2048,
            badgeLabel = "Smart",
            iconEmoji = "🧠",
            requiresAuth = false
        ),
        ModelInfo(
            id = "falcon_rw_1b",
            name = "Falcon 1B",
            description = "TII's efficient 1B model. Good for general conversation and device control commands.",
            sizeLabel = "~500 MB",
            sizeMb = 500,
            downloadUrl = "https://huggingface.co/litert-community/falcon-rw-1b/resolve/main/falcon-rw-1b-int4.task",
            type = ModelType.MEDIAPIPE,
            parameters = "1B",
            quantization = "INT4",
            contextLength = 2048,
            badgeLabel = "Compact",
            iconEmoji = "🦅",
            requiresAuth = false
        )
    )
}

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isThinking: Boolean = false,
    val isVoice: Boolean = false,
    val screenshotBase64: String? = null
)

data class DownloadState(
    val modelId: String,
    val progress: Int,          // 0-100
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val speedKbps: Double,
    val isComplete: Boolean = false,
    val error: String? = null
)
