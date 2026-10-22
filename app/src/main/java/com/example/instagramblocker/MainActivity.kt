package com.example.instagramblocker

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
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
import com.example.instagramblocker.accessibility.InstagramBlockService
import com.example.instagramblocker.data.BlockControlStore
import com.example.instagramblocker.data.BlockStore
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
    private val controlStore by lazy { BlockControlStore(this) }
    private val updateChecker by lazy { UpdateChecker(this) }
    private var state by mutableStateOf(BlockState(0L, 0))
    private var serviceEnabled by mutableStateOf(false)
    private var blockingEnabled by mutableStateOf(true)
    private var remainingMs by mutableLongStateOf(0L)
    private var updateState by mutableStateOf(UpdateUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InstBlockTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = AppColors.background) {
                    DashboardScreen(
                        state = state,
                        remainingMs = remainingMs,
                        serviceEnabled = serviceEnabled,
                        blockingEnabled = blockingEnabled,
                        onOpenSettings = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                        onRefresh = ::refresh,
                        onToggleBlocking = ::toggleBlocking,
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
    }

    private fun refresh() {
        val nowMs = System.currentTimeMillis()
        state = store.read()
        val control = controlStore.read()
        blockingEnabled = control.enabled
        remainingMs = control.pausedRemainingMs ?: BlockPolicy.remainingMs(nowMs, state.deadlineMs)

        val component = ComponentName(this, InstagramBlockService::class.java)
        val names = setOf(component.flattenToString(), component.flattenToShortString())
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        serviceEnabled = enabledServices.split(':').any { it in names }
    }

    private fun toggleBlocking() {
        val nowMs = System.currentTimeMillis()
        if (blockingEnabled) {
            controlStore.pause(nowMs, state.deadlineMs)
        } else {
            val newDeadline = controlStore.resume(nowMs)
            store.restoreDeadline(newDeadline)
        }
        refresh()
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
}
