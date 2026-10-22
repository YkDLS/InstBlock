package com.example.instagramblocker

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.instagramblocker.accessibility.InstagramBlockService
import com.example.instagramblocker.accessibility.ServiceConnectionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale

private val AppColors = darkColorScheme(
    primary = Color(0xFF65E7FF),
    secondary = Color(0xFF63F5B0),
    tertiary = Color(0xFFB78CFF),
    background = Color(0xFF07111F),
    surface = Color(0xFF0D1B2A),
    onBackground = Color(0xFFE8F7FF),
    onSurface = Color(0xFFE8F7FF),
)

private data class UpdateUiState(
    val checking: Boolean = false,
    val message: String? = null,
    val available: UpdateInfo? = null,
)

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
            MaterialTheme(colorScheme = AppColors) {
                Surface(modifier = Modifier.fillMaxSize(), color = AppColors.background) {
                    BlockScreen(
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

@Composable
private fun BlockScreen(
    state: BlockState,
    remainingMs: Long,
    serviceEnabled: Boolean,
    blockingEnabled: Boolean,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    onToggleBlocking: () -> Unit,
    updateState: UpdateUiState,
    onCheckUpdates: () -> Unit,
    onInstallUpdate: (UpdateInfo) -> Unit,
) {
    val connected by ServiceConnectionState.connected.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                onRefresh()
                delay(1_000L)
            }
        }
    }

    val totalSeconds = (remainingMs + 999L) / 1_000L
    val timer = String.format(Locale.getDefault(), "%02d:%02d:%02d", totalSeconds / 3_600L, (totalSeconds % 3_600L) / 60L, totalSeconds % 60L)
    val status = when {
        !serviceEnabled -> R.string.service_inactive
        connected -> R.string.service_active
        else -> R.string.service_waiting
    }
    val todayKey = LocalDate.now().toString()
    val dailyEntries = state.attemptsByDay.entries.sortedByDescending { it.key }.take(7)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 28.dp),
    ) {
        Text("INSTAGRAM_BLOCKER // CONTROL PANEL", style = MaterialTheme.typography.labelLarge, color = AppColors.primary, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.intro), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(20.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
            Box(
                modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF102E4A), Color(0xFF442B78))), RoundedCornerShape(28.dp)).padding(24.dp),
            ) {
                Column {
                    Text(if (blockingEnabled) "Bloqueio ativo" else "Pausa para trabalho", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(timer, color = AppColors.primary, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(4.dp))
                    Text(if (blockingEnabled) "Cada tentativa soma 5 minutos" else "O tempo está congelado", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard("Tentativas", state.attempts.toString(), AppColors.tertiary, Modifier.weight(1f))
            StatCard("Hoje", (state.attemptsByDay[todayKey] ?: 0).toString(), AppColors.secondary, Modifier.weight(1f))
        }

        Spacer(Modifier.height(18.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A)), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("// TENTATIVAS POR DIA", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(12.dp))
                if (dailyEntries.isEmpty()) {
                    Text("Ainda não há tentativas registradas.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    dailyEntries.forEach { (day, count) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (day == todayKey) "Hoje" else day, style = MaterialTheme.typography.bodyLarge)
                            Text("$count tentativas", fontWeight = FontWeight.SemiBold, color = AppColors.primary)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = AppColors.primary.copy(alpha = 0.08f)), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("// ATUALIZAÇÕES", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(6.dp))
                Text(updateState.message ?: "Mantenha o bloqueador atualizado pelo GitHub.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                if (updateState.available != null && !updateState.checking) {
                    Button(onClick = { onInstallUpdate(updateState.available) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Text("Baixar versão ${updateState.available.version}")
                    }
                } else {
                    Button(onClick = onCheckUpdates, enabled = !updateState.checking, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Text(if (updateState.checking) "Verificando…" else "Verificar atualizações")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("STATUS :: ${stringResource(status)}", style = MaterialTheme.typography.titleMedium, color = AppColors.secondary, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(if (blockingEnabled) R.string.blocking_enabled else R.string.blocking_paused), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onToggleBlocking,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (blockingEnabled) AppColors.tertiary else AppColors.secondary),
        ) {
            Text(stringResource(if (blockingEnabled) R.string.pause_blocking else R.string.resume_blocking))
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.enable_explanation), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Text(stringResource(R.string.open_settings))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = color)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}
