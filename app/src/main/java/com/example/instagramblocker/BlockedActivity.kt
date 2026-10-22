package com.example.instagramblocker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Locale

/** Tela mostrada quando uma tentativa de abrir o Instagram é bloqueada. */
class BlockedActivity : ComponentActivity() {
    private val store by lazy { BlockStore(this) }
    private var state by mutableStateOf(BlockState(0L, 0))
    private var nowMs by mutableLongStateOf(System.currentTimeMillis())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })
        state = store.read()
        setContent {
            LaunchedEffect(Unit) {
                while (true) {
                    nowMs = System.currentTimeMillis()
                    state = store.read()
                    delay(1_000L)
                }
            }
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BlockedScreen(
                        remainingMs = BlockPolicy.remainingMs(nowMs, state.deadlineMs),
                        onGoHome = ::goHome,
                    )
                }
            }
        }
    }

    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(home)
        finishAndRemoveTask()
    }
}

@androidx.compose.runtime.Composable
private fun BlockedScreen(remainingMs: Long, onGoHome: () -> Unit) {
    val totalSeconds = (remainingMs + 999L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    val timer = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)

    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Ops! O feed escapou por pouco 😅", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "O Instagram está de castigo. Cada tentativa acrescenta mais 5 minutos ao bloqueio.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(28.dp))
        Text("Tempo restante", style = MaterialTheme.typography.titleMedium)
        Text(timer, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("+5 minutos adicionados por esta tentativa.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onGoHome, modifier = Modifier.fillMaxWidth()) {
            Text("Voltar para a tela inicial")
        }
    }
}
