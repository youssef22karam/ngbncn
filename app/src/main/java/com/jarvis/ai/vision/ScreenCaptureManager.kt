package com.jarvis.ai.vision

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ScreenCaptureManager(private val context: Context) {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val displayMetrics = DisplayMetrics()

    init {
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(displayMetrics)
    }

    fun getProjectionManager(): MediaProjectionManager =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

    fun startCapture(resultCode: Int, data: Intent) {
        try {
            val projectionManager = getProjectionManager()
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)

            val width  = displayMetrics.widthPixels
            val height = displayMetrics.heightPixels
            val dpi    = displayMetrics.densityDpi

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "JarvisCapture",
                width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null, null
            )

            _isCapturing.value = true
            Log.i(TAG, "Screen capture started (${width}x${height})")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start capture", e)
        }
    }

    suspend fun captureScreen(): Bitmap? = withContext(Dispatchers.IO) {
        if (!_isCapturing.value || imageReader == null) return@withContext null

        try {
            val image: Image? = imageReader?.acquireLatestImage()
            image?.use {
                val planes = it.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride   = planes[0].rowStride
                val rowPadding  = rowStride - pixelStride * displayMetrics.widthPixels

                val bitmap = Bitmap.createBitmap(
                    displayMetrics.widthPixels + rowPadding / pixelStride,
                    displayMetrics.heightPixels,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)

                // Crop to exact dimensions
                Bitmap.createBitmap(bitmap, 0, 0,
                    displayMetrics.widthPixels, displayMetrics.heightPixels)
                    .also { bitmap.recycle() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Capture failed", e)
            null
        }
    }

    suspend fun captureAsBase64(quality: Int = 60): String? {
        val bitmap = captureScreen() ?: return null
        return withContext(Dispatchers.IO) {
            try {
                // Scale down to reduce token usage
                val scaled = Bitmap.createScaledBitmap(bitmap, 720,
                    (720 * bitmap.height / bitmap.width), true)
                bitmap.recycle()

                val stream = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                scaled.recycle()

                Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            } catch (e: Exception) {
                Log.e(TAG, "Base64 encode failed", e)
                null
            }
        }
    }

    fun stopCapture() {
        try {
            virtualDisplay?.release()
            imageReader?.close()
            mediaProjection?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Stop capture error", e)
        }
        virtualDisplay  = null
        imageReader     = null
        mediaProjection = null
        _isCapturing.value = false
        Log.i(TAG, "Screen capture stopped")
    }

    companion object {
        private const val TAG = "ScreenCaptureManager"
        const val SCREEN_CAPTURE_REQUEST_CODE = 1001
    }
}
