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
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.data.pref.AppSettings
import com.example.data.repository.TradingRepository
import com.example.model.ConfirmationType
import com.example.model.EngineState
import com.example.model.SignalType
import com.example.model.TradingSignal

/**
 * Floating Overlay Window Service.
 * Features:
 * - Dedicated drag handle bar to isolate drag gestures from button clicks.
 * - Screen boundary clamping to keep HUD inside display limits.
 * - Persistent coordinate saving & restoration.
 * - Live display of 10 Engine States, verified or TIDAK DIKENALI symbols, TIDAK TERSEDIA prices.
 * - Strict COPY button conforming to Section 10 formatting rules.
 */
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
    private lateinit var appSettings: AppSettings

    // Drag coordination
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var screenWidth = 1080
    private var screenHeight = 1920

    override fun onCreate() {
        super.onCreate()
        repository = TradingRepository.getInstance(this)
        appSettings = AppSettings(this)

        lifecycleOwner = ServiceSavedStateLifecycleOwner()
        lifecycleOwner.onCreate()
        lifecycleOwner.onStart()

        updateScreenMetrics()
    }

    private fun updateScreenMetrics() {
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
        updateScreenMetrics()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val savedSettings = appSettings.getSettings()
        val startX = savedSettings.overlayX.coerceIn(0, (screenWidth - 260).coerceAtLeast(0))
        val startY = savedSettings.overlayY.coerceIn(50, (screenHeight - 300).coerceAtLeast(50))

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = startX
            y = startY
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                val currentSignal by repository.currentSignal.collectAsState()
                val engineState by repository.engineState.collectAsState()
                val isCapturing by repository.isCapturing.collectAsState()
                val isPaused by repository.isPaused.collectAsState()
                val isAnalyzing by repository.isAnalyzing.collectAsState()
                val settings by appSettings.settingsFlow.collectAsState()

                FloatingOverlayContent(
                    signal = currentSignal,
                    engineState = engineState,
                    activeTimeframe = settings.activeTimeframe.code,
                    isCapturing = isCapturing,
                    isPaused = isPaused,
                    isAnalyzing = isAnalyzing,
                    onClose = {
                        hideOverlay()
                        stopSelf()
                    }
                )
            }
        }

        // Touch listener dedicated strictly to dragging:
        // Only touches initiated in the top header drag-handle area trigger window moves.
        // Clicks on buttons, copy, minimize, and close are passed through directly.
        val dragHandleZoneHeightPx = (46 * resources.displayMetrics.density).toInt()

        composeView.setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Only intercept if touched in top drag handle area
                    if (event.y <= dragHandleZoneHeightPx) {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        false // let Compose handle potential child buttons inside header if any
                    } else {
                        false
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()

                    if (Math.abs(deltaX) > 12 || Math.abs(deltaY) > 12) {
                        isDragging = true
                        updateScreenMetrics()
                        // Boundary clamping: keep window fully on screen
                        val clampedX = (initialX + deltaX).coerceIn(0, (screenWidth - 120).coerceAtLeast(120))
                        val clampedY = (initialY + deltaY).coerceIn(40, (screenHeight - 140).coerceAtLeast(140))
                        params.x = clampedX
                        params.y = clampedY
                        windowManager?.updateViewLayout(composeView, params)
                        true
                    } else {
                        false
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        appSettings.saveOverlayPosition(params.x, params.y)
                        isDragging = false
                        true
                    } else {
                        false
                    }
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
    engineState: EngineState,
    activeTimeframe: String,
    isCapturing: Boolean,
    isPaused: Boolean,
    isAnalyzing: Boolean,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var isMinimized by remember { mutableStateOf(false) }

    val signalType = signal?.signal ?: SignalType.WAIT
    val pairText = signal?.formattedSymbol() ?: "TIDAK DIKENALI"
    val timeframe = signal?.timeframe?.ifBlank { activeTimeframe } ?: activeTimeframe
    val entryText = signal?.formattedEntry() ?: "TIDAK TERSEDIA"
    val stopLossText = signal?.formattedStopLoss() ?: "TIDAK TERSEDIA"
    val takeProfitText = signal?.formattedTakeProfit() ?: "TIDAK TERSEDIA"
    val riskReward = signal?.riskReward ?: "1:2"
    val confidenceText = signal?.formattedConfidence() ?: "0%"
    val confirmation = signal?.confirmation ?: ConfirmationType.UNCONFIRMED
    val updateTime = signal?.timestamp?.takeLast(8)?.replace("Z", "") ?: "--:--:--"

    val (signalColor, badgeBg, signalText) = when (signalType) {
        SignalType.BUY -> Triple(Color(0xFF10B981), Color(0x3310B981), "🟢 BUY NOW")
        SignalType.SELL -> Triple(Color(0xFFF43F5E), Color(0x33F43F5E), "🔴 SELL NOW")
        SignalType.WAIT -> Triple(Color(0xFFF59E0B), Color(0x33F59E0B), "🟡 WAIT")
    }

    // Engine State badge color & label
    val (stateBadgeBg, stateBadgeColor, stateLabel) = when (engineState) {
        EngineState.BUY -> Triple(Color(0x3310B981), Color(0xFF10B981), "BUY NOW")
        EngineState.SELL -> Triple(Color(0x33F43F5E), Color(0xFFF43F5E), "SELL NOW")
        EngineState.WAIT -> Triple(Color(0x33F59E0B), Color(0xFFF59E0B), "WAIT")
        EngineState.WAITING_CHART -> Triple(Color(0x3364748B), Color(0xFF94A3B8), "MENUNGGU CHART")
        EngineState.READING_CHART -> Triple(Color(0x330284C7), Color(0xFF38BDF8), "MEMBACA CHART")
        EngineState.VALIDATING_DATA -> Triple(Color(0x338B5CF6), Color(0xFFA78BFA), "MEMVALIDASI DATA")
        EngineState.ANALYZING -> Triple(Color(0x3306B6D4), Color(0xFF22D3EE), "MENGANALISIS")
        EngineState.INVALID_DATA -> Triple(Color(0x33EF4444), Color(0xFFF87171), "DATA TIDAK VALID")
        EngineState.STREAM_DISCONNECTED -> Triple(Color(0x33EF4444), Color(0xFFF87171), "STREAM TERPUTUS")
        EngineState.DATA_EXPIRED -> Triple(Color(0x33F97316), Color(0xFFFB923C), "DATA KEDALUWARSA")
    }

    Surface(
        modifier = Modifier
            .padding(6.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(listOf(signalColor.copy(alpha = 0.85f), Color(0xFF1E293B))),
                shape = RoundedCornerShape(16.dp)
            ),
        color = Color(0xF2090D16),
        shadowElevation = 10.dp
    ) {
        if (isMinimized) {
            // Minimized Floating Pill
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
                    text = "TF: $timeframe | $signalText | $confidenceText",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
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
            // Full Detailed Floating HUD Card
            Column(
                modifier = Modifier
                    .width(280.dp)
                    .padding(12.dp)
            ) {
                // Dedicated Drag Handle Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF64748B))
                    )
                }

                // Header Bar: Drag Icon + Engine State + Minimize & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Drag Handle",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(stateBadgeBg)
                                .border(1.dp, stateBadgeColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stateLabel,
                                color = stateBadgeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = { isMinimized = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Minimize",
                                tint = Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Primary Signal Big Badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .border(1.dp, signalColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = signalText,
                            color = signalColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            letterSpacing = 1.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CONFIDENCE: $confidenceText",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (confirmation == ConfirmationType.CONFIRMED && signalType != SignalType.WAIT) {
                                Text(
                                    text = "• TERKONFIRMASI",
                                    color = Color(0xFF34D399),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Data Rows: TIMEFRAME, PAIR, ENTRY, R:R
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(label = "TIMEFRAME:", value = timeframe)
                    MetricItem(label = "SIMBOL:", value = pairText)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(label = "ENTRY:", value = entryText, highlight = true)
                    MetricItem(label = "RISK REWARD:", value = riskReward)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(label = "STOP LOSS:", value = stopLossText, color = Color(0xFFF87171))
                    MetricItem(label = "TAKE PROFIT:", value = takeProfitText, color = Color(0xFF4ADE80))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Action: COPY Button & Update Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Update: $updateTime",
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp
                    )

                    Button(
                        onClick = {
                            val copyContent = signal?.formattedCopyText() ?: "[UDA AI TRADER]\nSinyal: WAIT\nStatus: MENUNGGU CHART"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("UdaTraderSignal", copyContent)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Sinyal disalin ke Clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("COPY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }

                if (isAnalyzing) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚡ Membaca chart visual...",
                        color = Color(0xFF38BDF8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
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
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            color = if (highlight) Color(0xFF38BDF8) else color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Service-level SavedStateRegistry and LifecycleOwner to host Jetpack Compose.
 */
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
