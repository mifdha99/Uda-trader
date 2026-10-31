package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.data.pref.AppSettings
import com.example.data.repository.TradingRepository

/**
 * Foreground Service for continuous MediaProjection screen capture.
 * Supports MULAI (Start), JEDA (Pause), LANJUT (Resume), and BERHENTI (Stop).
 */
class ScreenCaptureService : Service() {

    companion object {
        private const val TAG = "ScreenCaptureService"
        const val ACTION_START = "com.example.action.START_CAPTURE"
        const val ACTION_PAUSE = "com.example.action.PAUSE_CAPTURE"
        const val ACTION_RESUME = "com.example.action.RESUME_CAPTURE"
        const val ACTION_STOP = "com.example.action.STOP_CAPTURE"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "uda_trader_capture_channel"
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handler: Handler? = null

    private var isCapturing = false
    private var isPaused = false

    private lateinit var appSettings: AppSettings
    private lateinit var repository: TradingRepository

    private val captureRunnable = object : Runnable {
        override fun run() {
            if (!isCapturing) return
            if (!isPaused) {
                captureLatestFrame()
            }
            val interval = appSettings.getSettings().intervalSeconds.coerceIn(1, 5) * 1000L
            handler?.postDelayed(this, interval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        appSettings = AppSettings(this)
        repository = TradingRepository.getInstance(this)
        handler = Handler(Looper.getMainLooper())
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopCapture()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                pauseCapture()
                return START_STICKY
            }
            ACTION_RESUME -> {
                resumeCapture()
                return START_STICKY
            }
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                if (resultCode != 0 && resultData != null) {
                    startForegroundWithNotification(isPaused = false)
                    startProjection(resultCode, resultData)
                } else {
                    Log.e(TAG, "Invalid MediaProjection intent data")
                    stopSelf()
                }
            }
        }

        return START_STICKY
    }

    private fun pauseCapture() {
        isPaused = true
        repository.setPaused(true)
        updateNotification(isPaused = true)
        Log.d(TAG, "Screen capture paused (JEDA)")
    }

    private fun resumeCapture() {
        isPaused = false
        repository.setPaused(false)
        updateNotification(isPaused = false)
        Log.d(TAG, "Screen capture resumed (LANJUT)")
    }

    private fun buildNotification(isPaused: Boolean): Notification {
        val stopIntent = Intent(this, ScreenCaptureService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIntent = Intent(this, ScreenCaptureService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val pauseResumePendingIntent = PendingIntent.getService(
            this,
            2,
            pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appIntent = Intent(this, MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (isPaused) {
            "Pemantauan Dijeda (JEDA). Tekan Lanjut untuk memantau."
        } else {
            "Memantau chart visual trading secara real-time"
        }

        val toggleLabel = if (isPaused) "LANJUT" else "JEDA"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("UDA AI TRADER")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(appPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, toggleLabel, pauseResumePendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "BERHENTI", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun startForegroundWithNotification(isPaused: Boolean) {
        val notification = buildNotification(isPaused)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(isPaused: Boolean) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(isPaused))
    }

    @SuppressLint("WrongConstant")
    private fun startProjection(resultCode: Int, resultData: Intent) {
        try {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

            if (mediaProjection == null) {
                Log.e(TAG, "Failed to obtain MediaProjection")
                repository.notifyStreamDisconnected()
                stopSelf()
                return
            }

            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)

            var screenWidth = metrics.widthPixels
            var screenHeight = metrics.heightPixels
            val density = metrics.densityDpi

            // Scale resolution to max 1080 width for CPU/RAM performance
            if (screenWidth > 1080) {
                val scale = 1080f / screenWidth.toFloat()
                screenWidth = 1080
                screenHeight = (screenHeight * scale).toInt()
            }

            if (screenWidth % 2 != 0) screenWidth -= 1
            if (screenHeight % 2 != 0) screenHeight -= 1

            imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "UdaTraderVirtualDisplay",
                screenWidth,
                screenHeight,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                handler
            )

            isCapturing = true
            isPaused = false
            repository.setCapturing(true)
            repository.setPaused(false)

            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                    Log.w(TAG, "MediaProjection stopped by system")
                    repository.notifyStreamDisconnected()
                    stopCapture()
                }
            }, handler)

            // Start capture cycle
            handler?.postDelayed(captureRunnable, 1000)
            Log.d(TAG, "MediaProjection live capture started: ${screenWidth}x${screenHeight}")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting projection: ${e.message}", e)
            repository.notifyStreamDisconnected()
            stopSelf()
        }
    }

    private fun captureLatestFrame() {
        val reader = imageReader ?: return
        var image: Image? = null
        try {
            image = reader.acquireLatestImage()
            if (image != null) {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val width = image.width
                val height = image.height

                val rowPadding = rowStride - pixelStride * width
                val bitmapWidth = width + rowPadding / pixelStride

                val bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(buffer)

                val cleanBitmap = if (bitmapWidth != width) {
                    val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                    bitmap.recycle()
                    cropped
                } else {
                    bitmap
                }

                repository.processCapturedFrame(cleanBitmap)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring frame: ${e.message}")
        } finally {
            image?.close()
        }
    }

    private fun stopCapture() {
        isCapturing = false
        isPaused = false
        handler?.removeCallbacks(captureRunnable)
        repository.setCapturing(false)
        repository.setPaused(false)

        try {
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
            mediaProjection?.stop()
            mediaProjection = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping projection: ${e.message}")
        }
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "UDA AI TRADER Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground service notification for active trading chart monitoring"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
