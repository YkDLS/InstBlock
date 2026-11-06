package com.example.instagramblocker

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.app.AppOpsManager
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.instagramblocker.usage.InstagramMonitor
import com.example.instagramblocker.data.BlockStore
import com.example.instagramblocker.data.SystemTimer
import com.example.instagramblocker.domain.BlockPhase
import com.example.instagramblocker.domain.BlockPolicy
import com.example.instagramblocker.domain.BlockState
import com.example.instagramblocker.ui.dashboard.DashboardScreen
import com.example.instagramblocker.ui.dashboard.UpdateUiState
import com.example.instagramblocker.ui.theme.AppColors
import com.example.instagramblocker.ui.theme.InstBlockTheme
import com.example.instagramblocker.updates.UpdateChecker
import com.example.instagramblocker.updates.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val store by lazy { BlockStore(this) }
    private val updateChecker by lazy { UpdateChecker(this) }
    private var state by mutableStateOf(BlockState(0L, 0))
    private var usageGranted by mutableStateOf(false)
    private var overlayGranted by mutableStateOf(false)
    private var blockPhase by mutableStateOf(BlockPhase.READY)
    private var remainingMs by mutableLongStateOf(0L)
    private var updateState by mutableStateOf(UpdateUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        setContent {
            InstBlockTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = AppColors.background) {
                    DashboardScreen(
                        state = state,
                        remainingMs = remainingMs,
                        serviceEnabled = usageGranted && overlayGranted,
                        blockPhase = blockPhase,
                        onOpenSettings = ::openRequiredSettings,
                        onRefresh = ::refresh,
                        updateState = updateState,
                        onCheckUpdates = ::checkForUpdates,
                        onInstallUpdate = ::installUpdate,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
        syncMonitor()
    }

    private fun refresh() {
        val nowMs = SystemTimer.elapsedRealtimeMs()
        state = store.current(nowMs, SystemTimer.bootCount(this))
        val snapshot = BlockPolicy.snapshot(nowMs, state.phase, state.deadlineMs)
        blockPhase = snapshot.phase
        remainingMs = snapshot.remainingMs

        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        usageGranted = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
        overlayGranted = Settings.canDrawOverlays(this)
    }

    private fun syncMonitor() {
        if (usageGranted && overlayGranted) {
            startForegroundService(Intent(this, InstagramMonitor::class.java))
        } else {
            stopService(Intent(this, InstagramMonitor::class.java))
        }
    }

    private fun openRequiredSettings() {
        if (!usageGranted) startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        else if (!overlayGranted) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).setData(android.net.Uri.parse("package:$packageName")))
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST)
        }
    }

    private fun checkForUpdates() {
        updateState = UpdateUiState(checking = true, message = "Verificando atualizações…")
        lifecycleScope.launch {
            updateState = try {
                val update = withContext(Dispatchers.IO) { updateChecker.findAvailableUpdate() }
                if (update == null) {
                    UpdateUiState(message = "Você já está usando a versão mais recente.")
                } else {
                    UpdateUiState(message = "Nova versão ${update.version} disponível.", available = update)
                }
            } catch (error: Exception) {
                UpdateUiState(message = "Não foi possível verificar agora. Tente novamente mais tarde.")
            }
        }
    }

    private fun installUpdate(update: UpdateInfo) {
        if (!updateChecker.canInstallPackages()) {
            updateState = UpdateUiState(message = "Permita instalações desta fonte para continuar.")
            updateChecker.openInstallPermissionSettings()
            return
        }
        updateState = UpdateUiState(checking = true, message = "Baixando a versão ${update.version}…")
        lifecycleScope.launch {
            updateState = try {
                val apk = withContext(Dispatchers.IO) { updateChecker.download(update) }
                updateChecker.openInstaller(apk)
                UpdateUiState(message = "Confirme a instalação na janela do Android.")
            } catch (error: Exception) {
                UpdateUiState(message = "Não foi possível baixar a atualização.")
            }
        }
    }

    private companion object {
        const val NOTIFICATION_PERMISSION_REQUEST = 100
    }
}
