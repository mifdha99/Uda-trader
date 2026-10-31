package com.example.service

import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.repository.TradingRepository
import com.example.model.AutoTradingState
import com.example.model.ConfirmationType
import com.example.model.DataSourceMode
import com.example.model.SignalType
import com.example.model.TradingSignal

class FloatingWindowService : Service() {

    companion object {
        private const val TAG = "FloatingWindowService"
        const val ACTION_SHOW = "com.example.action.SHOW_FLOATING"
        const val ACTION_HIDE = "com.example.action.HIDE_FLOATING"
    }

    private var windowManager: WindowManager? = null
    private var overlayComposeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private lateinit var lifecycleOwner: ServiceSavedStateLifecycleOwner
    private lateinit var repository: TradingRepository

    // Drag coordination & screen clamping
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var screenWidth = 1080
    private var screenHeight = 2400

    override fun onCreate() {
        super.onCreate()
        repository = TradingRepository.getInstance(this)

        lifecycleOwner = ServiceSavedStateLifecycleOwner()
        lifecycleOwner.onCreate()
        lifecycleOwner.onStart()

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_SHOW
        when (action) {
            ACTION_SHOW -> {
                if (checkOverlayPermission()) {
                    showOverlay()
                } else {
                    Log.w(TAG, "Overlay permission not granted")
                    stopSelf()
                }
            }
            ACTION_HIDE -> {
                hideOverlay()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun checkOverlayPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
    }

    private fun showOverlay() {
        if (overlayComposeView != null) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 200
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                val currentSignal by repository.currentSignal.collectAsState()
                val isCapturing by repository.isCapturing.collectAsState()
                val isAnalyzing by repository.isAnalyzing.collectAsState()
                val activeMode by repository.activeDataSource.collectAsState()
                val autoTradeState by repository.autoTradingState.collectAsState()
                val olympPrice by repository.olympPrice.collectAsState()

                FloatingOverlayContent(
                    signal = currentSignal,
                    isCapturing = isCapturing,
                    isAnalyzing = isAnalyzing,
                    activeMode = activeMode,
                    autoTradeState = autoTradeState,
                    olympPrice = olympPrice,
                    onCopySignal = { copySignalToClipboard(currentSignal, activeMode) },
                    onEmergencyStop = { repository.emergencyStopAutoTrading() },
                    onClose = {
                        hideOverlay()
                        stopSelf()
                    }
                )
            }
        }

        composeView.setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()

                    // Clamp to screen bounds
                    params.x = (initialX + deltaX).coerceIn(0, screenWidth - 200)
                    params.y = (initialY + deltaY).coerceIn(40, screenHeight - 200)
                    windowManager?.updateViewLayout(composeView, params)
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(composeView, layoutParams)
            overlayComposeView = composeView
            repository.setFloatingOverlayVisible(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding overlay view: ${e.message}", e)
        }
    }

    private fun copySignalToClipboard(signal: TradingSignal?, mode: DataSourceMode) {
        val dir = when (signal?.signal) {
            SignalType.BUY -> "BUY / CALL (UP)"
            SignalType.SELL -> "SELL / PUT (DOWN)"
            else -> "WAIT"
        }
        val text = "UDA AI TRADER [${mode.name}]\nPair: ${signal?.pair ?: "EURUSD"}\nSinyal: $dir\nEntry: ${signal?.entry ?: "Market"}\nWaktu: ${signal?.timestamp ?: "Realtime"}"

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("UDA AI Trader Signal", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Sinyal berhasil disalin!", Toast.LENGTH_SHORT).show()
    }

    private fun hideOverlay() {
        overlayComposeView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay view: ${e.message}")
            }
            overlayComposeView = null
        }
        repository.setFloatingOverlayVisible(false)
    }

    override fun onDestroy() {
        hideOverlay()
        lifecycleOwner.onDestroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun FloatingOverlayContent(
    signal: TradingSignal?,
    isCapturing: Boolean,
    isAnalyzing: Boolean,
    activeMode: DataSourceMode,
    autoTradeState: AutoTradingState,
    olympPrice: Double?,
    onCopySignal: () -> Unit,
    onEmergencyStop: () -> Unit,
    onClose: () -> Unit
) {
    var isMinimized by remember { mutableStateOf(false) }

    val signalType = signal?.signal ?: SignalType.WAIT
    val pair = signal?.pair ?: "EURUSD"
    val timeframe = signal?.timeframe ?: "M5"
    val entry = if (activeMode == DataSourceMode.OLYMP_TRADE_WS && olympPrice != null) {
        String.format("%.5f", olympPrice)
    } else {
        signal?.entry?.let { String.format("%.5f", it) } ?: "---"
    }
    val stopLoss = signal?.stopLoss?.let { String.format("%.5f", it) } ?: "---"
    val takeProfit = signal?.takeProfit?.let { String.format("%.5f", it) } ?: "---"
    val riskReward = signal?.riskReward ?: "1:2"
    val confirmation = signal?.confirmation ?: ConfirmationType.UNCONFIRMED

    val (signalColor, badgeBg, signalText) = when (signalType) {
        SignalType.BUY -> Triple(Color(0xFF10B981), Color(0x3310B981), if (activeMode == DataSourceMode.OLYMP_TRADE_WS) "🟢 UP / CALL" else "🟢 BUY NOW")
        SignalType.SELL -> Triple(Color(0xFFF43F5E), Color(0x33F43F5E), if (activeMode == DataSourceMode.OLYMP_TRADE_WS) "🔴 DOWN / PUT" else "🔴 SELL NOW")
        SignalType.WAIT -> Triple(Color(0xFFF59E0B), Color(0x33F59E0B), "🟡 WAIT")
    }

    Surface(
        modifier = Modifier
            .padding(6.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(listOf(signalColor.copy(alpha = 0.8f), Color(0xFF1E293B))),
                shape = RoundedCornerShape(16.dp)
            ),
        color = Color(0xE60B0F19),
        shadowElevation = 8.dp
    ) {
        if (isMinimized) {
            Row(
                modifier = Modifier
                    .clickable { isMinimized = false }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(signalColor, CircleShape)
                )
                Text(
                    text = "$pair: $signalText",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = Color.LightGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .width(270.dp)
                    .padding(12.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (isCapturing || autoTradeState == AutoTradingState.ANALYZING) Color(0xFF10B981) else Color(0xFF64748B),
                                        CircleShape
                                    )
                            )
                            Text(
                                text = "UDA AI TRADER",
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = if (activeMode == DataSourceMode.OLYMP_TRADE_WS) "OLYMP TRADE (WS)" else "MT5 VISION MODE",
                            color = if (activeMode == DataSourceMode.OLYMP_TRADE_WS) Color(0xFF06B6D4) else Color(0xFF34D399),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row {
                        IconButton(
                            onClick = { isMinimized = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Minimize", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Primary Signal Big Badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .border(1.dp, signalColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = signalText,
                            color = signalColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp
                        )
                        if (confirmation == ConfirmationType.CONFIRMED && signalType != SignalType.WAIT) {
                            Text(
                                text = "TERKONFIRMASI",
                                color = Color(0xFF34D399),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Data Rows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(label = "PAIR:", value = pair)
                    MetricItem(label = "TF:", value = timeframe)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(label = "ENTRY / HARGA:", value = entry, highlight = true)
                    MetricItem(label = "R:R / DURASI:", value = riskReward)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons: COPY & STOP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCopySignal,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color(0xFF38BDF8)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("COPY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onEmergencyStop,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Icon(Icons.Default.StopCircle, contentDescription = "Stop", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("STOP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color = Color.White,
    highlight: Boolean = false
) {
    Column {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = value,
            color = if (highlight) Color(0xFF38BDF8) else color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

class ServiceSavedStateLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    fun onCreate() {
        savedStateRegistryController.performRestore(Bundle())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun onStart() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}
