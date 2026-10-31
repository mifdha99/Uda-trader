package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.data.repository.TradingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OlympTradeBackgroundService : Service() {

    companion object {
        private const val TAG = "OlympTradeBgService"
        const val ACTION_START = "com.example.action.START_OLYMP_BG"
        const val ACTION_STOP = "com.example.action.STOP_OLYMP_BG"
        private const val NOTIFICATION_ID = 2002
        private const val CHANNEL_ID = "uda_olymp_bg_channel"
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observerJob: Job? = null
    private lateinit var repository: TradingRepository

    override fun onCreate() {
        super.onCreate()
        repository = TradingRepository.getInstance(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopRobot()
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_START) {
            acquireWakeLock()
            startForegroundWithNotification()
            observeRobotStatus()
        }

        return START_STICKY
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "UdaAiTrader:OlympTradeRobotWakeLock"
            ).apply {
                // Acquire with 30-minute safety limit to prevent indefinite battery drain
                acquire(30 * 60 * 1000L)
            }
            Log.d(TAG, "Partial WakeLock acquired for background execution")
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring WakeLock: ${e.message}")
        }
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification("Menginisialisasi robot trading...", "Menghubungkan...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            }
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val stopIntent = Intent(this, OlympTradeBackgroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appIntent = Intent(this, MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(appPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "STOP DARURAT", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun observeRobotStatus() {
        observerJob?.cancel()
        observerJob = scope.launch {
            repository.currentSignal.collectLatest { signal ->
                val pair = signal?.pair ?: "EURUSD"
                val rec = signal?.displaySignalText() ?: "WAIT"
                val notification = buildNotification(
                    title = "UDA Robot Aktif: $pair [$rec]",
                    text = signal?.reason ?: "Memantau pasar di latar belakang..."
                )
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun stopRobot() {
        observerJob?.cancel()
        observerJob = null
        repository.stopAutoTrading()

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "WakeLock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing WakeLock: ${e.message}")
        }
    }

    override fun onDestroy() {
        stopRobot()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "UDA AI TRADER Robot Background",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi aktif robot trading Olymp Trade saat layar mati"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
