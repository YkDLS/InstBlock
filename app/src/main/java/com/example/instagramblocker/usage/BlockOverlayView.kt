package com.example.instagramblocker.usage

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.example.instagramblocker.R
import java.util.Locale

/** Janela de bloqueio exibida somente enquanto o Instagram está em primeiro plano. */
class BlockOverlayView(context: Context, message: String, onGoHome: () -> Unit) : FrameLayout(context) {
    private val ink = Color.rgb(38, 32, 68)
    private val muted = Color.rgb(105, 100, 132)
    private val violet = Color.rgb(101, 85, 217)
    private val timer = label("00:00:00", 43f, Color.WHITE, Typeface.BOLD)
    private val attempt = label("", 13f, muted)
    private val motivation = label(message, 16f, muted)

    init {
        isClickable = true
        setBackgroundColor(Color.rgb(247, 246, 253))

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(38), dp(24), dp(34))
        }
        val scroll = ScrollView(context).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            addView(content)
        }
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        content.addView(label("✦  INSTBLOCK", 13f, violet, Typeface.BOLD))
        content.addView(space(36))
        content.addView(label("Um respiro antes\ndo feed", 36f, ink, Typeface.BOLD).apply {
            letterSpacing = -0.03f
        })
        content.addView(space(12))
        content.addView(motivation)
        content.addView(space(30))

        val focusCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(25), dp(24), dp(26))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(80, 65, 190), Color.rgb(151, 119, 244)),
            ).apply { cornerRadius = dp(28).toFloat() }
            elevation = dp(7).toFloat()
        }
        content.addView(focusCard, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        focusCard.addView(label("●  BLOQUEIO ATIVO", 12f, Color.WHITE, Typeface.BOLD))
        focusCard.addView(space(32))
        focusCard.addView(label("TEMPO RESTANTE", 12f, Color.WHITE).apply { alpha = 0.8f })
        focusCard.addView(space(5))
        focusCard.addView(timer)
        focusCard.addView(space(14))
        focusCard.addView(label("Cada nova entrada soma 5 minutos de foco", 14f, Color.WHITE).apply { alpha = 0.9f })

        content.addView(space(25))
        val note = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            background = rounded(Color.WHITE, 22)
        }
        content.addView(note, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        note.addView(label("Depois da pausa", 16f, ink, Typeface.BOLD))
        note.addView(space(6))
        note.addView(label("Quando o tempo terminar, você terá 10 minutos corridos para acessar o Instagram.", 14f, muted))
        note.addView(space(10))
        note.addView(attempt)

        content.addView(space(24))
        content.addView(label("A contagem continua mesmo se você fechar esta tela", 13f, muted).apply {
            gravity = Gravity.CENTER
        })
        content.addView(space(16))
        val button = label("Voltar para a tela inicial  →", 16f, Color.WHITE, Typeface.BOLD).apply {
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(17), dp(16), dp(17))
            background = rounded(violet, 18)
            isClickable = true
            setOnClickListener { onGoHome() }
        }
        content.addView(button, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    fun update(remainingMs: Long, attempts: Int) {
        val seconds = (remainingMs + 999L) / 1_000L
        timer.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", seconds / 3_600L, (seconds % 3_600L) / 60L, seconds % 60L)
        attempt.text = resources.getQuantityString(R.plurals.attempts_registered, attempts, attempts)
    }

    private fun label(text: String, size: Float, color: Int, weight: Int = Typeface.NORMAL) = TextView(context).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        setTypeface(Typeface.DEFAULT, weight)
        includeFontPadding = false
    }

    private fun space(height: Int) = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
