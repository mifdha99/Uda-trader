package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.screens.CockpitScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.RiskCalculatorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.TraderViewModel

enum class TraderNavTab(val title: String, val icon: ImageVector) {
    COCKPIT("Cockpit", Icons.Default.Assessment),
    HISTORY("Riwayat", Icons.Default.History),
    RISK("Risiko", Icons.Default.Security),
    SETTINGS("Pengaturan", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: TraderViewModel by viewModels()

    // MediaProjection screen capture contract
    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.startScreenCapture(result.resultCode, result.data!!)
            Toast.makeText(this, "Screen capture aktif untuk chart MT5", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Izin perekaman layar dibatalkan", Toast.LENGTH_SHORT).show()
        }
    }

    // Overlay permission contract
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (viewModel.canDrawOverlays()) {
            viewModel.toggleFloatingOverlay(this) {}
        } else {
            Toast.makeText(this, "Izin Tampilan di Atas Aplikasi Lain diperlukan untuk Floating Window", Toast.LENGTH_LONG).show()
        }
    }

    // Notification permission contract for Android 13+
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Continue to capture
        requestMediaProjection()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContainer(
                    viewModel = viewModel,
                    onRequestStartCapture = { checkPermissionsAndStartCapture() },
                    onRequestFloatingOverlay = { handleFloatingOverlayRequest() }
                )
            }
        }
    }

    private fun checkPermissionsAndStartCapture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        requestMediaProjection()
    }

    private fun requestMediaProjection() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenCaptureLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    private fun handleFloatingOverlayRequest() {
        if (!viewModel.canDrawOverlays()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        } else {
            viewModel.toggleFloatingOverlay(this) {}
        }
    }
}

@Composable
fun MainAppContainer(
    viewModel: TraderViewModel,
    onRequestStartCapture: () -> Unit,
    onRequestFloatingOverlay: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(TraderNavTab.COCKPIT) }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }

    val currentSignal by viewModel.currentSignal.collectAsState()
    val isCapturing by viewModel.isCapturing.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isFloatingOverlayVisible by viewModel.isFloatingOverlayVisible.collectAsState()
    val signalHistory by viewModel.signalHistory.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val riskSettings by viewModel.riskSettings.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .background(DarkSurface)
                    .navigationBarsPadding()
            ) {
                TraderNavTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) CyanAccent else Slate400
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                color = if (isSelected) CyanAccent else Slate400,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Slate900,
                            selectedIconColor = CyanAccent,
                            selectedTextColor = CyanAccent,
                            unselectedIconColor = Slate400,
                            unselectedTextColor = Slate400
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            TraderNavTab.COCKPIT -> {
                CockpitScreen(
                    currentSignal = currentSignal,
                    isCapturing = isCapturing,
                    isAnalyzing = isAnalyzing,
                    statusMessage = statusMessage,
                    isFloatingOverlayVisible = isFloatingOverlayVisible,
                    activeTimeframe = settings.activeTimeframe,
                    selectedInterval = settings.intervalSeconds,
                    onTimeframeSelected = { viewModel.updateTimeframe(it) },
                    onStartCapture = onRequestStartCapture,
                    onStopCapture = { viewModel.stopScreenCapture() },
                    onToggleFloatingOverlay = onRequestFloatingOverlay,
                    onIntervalSelected = { viewModel.updateInterval(it) },
                    onTestSampleFrame = { viewModel.analyzeSampleChart() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            TraderNavTab.HISTORY -> {
                HistoryScreen(
                    signals = signalHistory,
                    onClearHistory = { viewModel.clearHistory() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            TraderNavTab.RISK -> {
                RiskCalculatorScreen(
                    riskSettings = riskSettings,
                    onBalanceChange = { viewModel.updateRiskBalance(it) },
                    onRiskPercentageChange = { viewModel.updateRiskPercentage(it) },
                    onStopLossPipsChange = { viewModel.updateRiskStopLossPips(it) },
                    onRiskRewardChange = { viewModel.updateRiskReward(it) },
                    onInstrumentChange = { viewModel.updateRiskInstrument(it) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            TraderNavTab.SETTINGS -> {
                SettingsScreen(
                    settings = settings,
                    onUpdateTimeframe = { viewModel.updateTimeframe(it) },
                    onUpdateInterval = { viewModel.updateInterval(it) },
                    onUpdateRiskReward = { viewModel.updatePreferredRiskReward(it) },
                    onUpdateVibration = { viewModel.updateVibration(it) },
                    onUpdateSound = { viewModel.updateSound(it) },
                    onResetOverlayPosition = { viewModel.resetOverlayPosition() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
