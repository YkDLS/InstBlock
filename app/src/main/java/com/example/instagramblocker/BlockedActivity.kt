package com.example.instagramblocker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.instagramblocker.data.BlockStore
import com.example.instagramblocker.domain.BlockPolicy
import com.example.instagramblocker.domain.BlockState
import com.example.instagramblocker.ui.blocked.BlockedScreen
import kotlinx.coroutines.delay

/** Tela mostrada quando uma tentativa de abrir o Instagram é bloqueada. */
class BlockedActivity : ComponentActivity() {
    private val blockMessages = listOf(
        "O feed foi colocado em intervalo. Seu foco ganhou esta rodada.",
        "Seu dedo tentou um atalho, mas o bloqueio chegou primeiro.",
        "A rolagem infinita entrou em manutenção para cuidar da sua produtividade.",
        "O Instagram pediu passagem, mas o foco estava na portaria.",
        "Tentativa registrada. Agora você tem alguns minutos para fazer algo que termina.",
        "A porta do feed está fechada por excesso de produtividade pendente.",
        "O bloqueio fez hora extra para proteger seu tempo.",
        "Você quase entrou. Quase também conta como treino de autocontrole.",
    )
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
                        message = blockMessages[(state.attempts - 1).coerceAtLeast(0) % blockMessages.size],
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
