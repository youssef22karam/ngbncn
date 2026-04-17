package com.jarvis.ai.llm

import android.content.Context
import android.util.Log
import com.jarvis.ai.data.DownloadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ModelDownloader(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)   // streaming — no timeout
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun getModelsDir(): File {
        val dir = File(context.filesDir, "models")
        dir.mkdirs()
        return dir
    }

    fun getModelFile(modelId: String): File =
        File(getModelsDir(), "$modelId.task")

    fun isModelDownloaded(modelId: String): Boolean =
        getModelFile(modelId).exists() && getModelFile(modelId).length() > 1024

    fun downloadModel(modelId: String, url: String): Flow<DownloadState> = flow {
        val destFile = getModelFile(modelId)
        val tempFile = File(getModelsDir(), "$modelId.tmp")

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "JarvisAI/1.0 Android")
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            emit(DownloadState(
                modelId = modelId,
                progress = 0,
                bytesDownloaded = 0,
                totalBytes = 0,
                speedKbps = 0.0,
                error = "HTTP ${response.code}: ${response.message}"
            ))
            return@flow
        }

        val totalBytes = response.body?.contentLength() ?: -1L
        var bytesDownloaded = 0L
        val buffer = ByteArray(BUFFER_SIZE)
        var startTime = System.currentTimeMillis()
        var lastEmitTime = 0L

        try {
            val inputStream = response.body!!.byteStream()
            val outputStream = FileOutputStream(tempFile)

            outputStream.use { out ->
                inputStream.use { inp ->
                    var read: Int
                    while (inp.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                        bytesDownloaded += read

                        val now = System.currentTimeMillis()
                        if (now - lastEmitTime > EMIT_INTERVAL_MS) {
                            val elapsedSec = (now - startTime) / 1000.0
                            val speedKbps = if (elapsedSec > 0)
                                (bytesDownloaded / 1024.0 / elapsedSec) else 0.0
                            val progress = if (totalBytes > 0)
                                ((bytesDownloaded * 100) / totalBytes).toInt() else -1

                            emit(DownloadState(
                                modelId      = modelId,
                                progress     = progress,
                                bytesDownloaded = bytesDownloaded,
                                totalBytes   = totalBytes,
                                speedKbps    = speedKbps
                            ))
                            lastEmitTime = now
                        }
                    }
                }
            }

            // Move temp → final
            if (tempFile.renameTo(destFile)) {
                emit(DownloadState(
                    modelId         = modelId,
                    progress        = 100,
                    bytesDownloaded = bytesDownloaded,
                    totalBytes      = totalBytes,
                    speedKbps       = 0.0,
                    isComplete      = true
                ))
            } else {
                emit(DownloadState(
                    modelId = modelId, progress = 0,
                    bytesDownloaded = 0, totalBytes = 0, speedKbps = 0.0,
                    error = "Failed to save model file"
                ))
            }

        } catch (e: Exception) {
            tempFile.delete()
            Log.e(TAG, "Download failed", e)
            emit(DownloadState(
                modelId = modelId, progress = 0,
                bytesDownloaded = 0, totalBytes = 0, speedKbps = 0.0,
                error = e.message ?: "Unknown download error"
            ))
        }
    }.flowOn(Dispatchers.IO)

    fun deleteModel(modelId: String): Boolean {
        val file = getModelFile(modelId)
        return if (file.exists()) file.delete() else true
    }

    fun formatBytes(bytes: Long): String = when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576     -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024         -> "%.1f KB".format(bytes / 1_024.0)
        else                   -> "$bytes B"
    }

    companion object {
        private const val TAG = "ModelDownloader"
        private const val BUFFER_SIZE = 8 * 1024      // 8 KB read buffer
        private const val EMIT_INTERVAL_MS = 250L     // UI update every 250ms
    }
}
