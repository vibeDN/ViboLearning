package com.vibo.vibolearning.ui.result

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibo.vibolearning.ui.components.SecondaryButton
import com.vibo.vibolearning.ui.components.VliButton
import com.vibo.vibolearning.ui.session.PlayUiState
import com.vibo.vibolearning.ui.session.SessionMode
import com.vibo.vibolearning.ui.theme.Vli

@Composable
fun CompleteScreen(state: PlayUiState, onContinue: () -> Unit) {
    val p = Vli.palette
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, tween(450)) }

    Column(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Icon(
            Icons.Filled.Star, null,
            tint = p.accent,
            modifier = Modifier.size(56.dp).scale(pop.value),
        )
        Text(
            when (state.mode) {
                SessionMode.Lesson -> "Урок пройден!"
                SessionMode.Exam -> "Модуль зачтён!"
                SessionMode.Practice -> "Практика завершена!"
            },
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            color = p.text,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Stat("+${state.xpEarned}", "Опыт", p.accent)
            Stat("${state.correctCount}/${state.interactiveTotal}", "Верно", p.text)
            if (state.mode == SessionMode.Practice) {
                Stat("${state.bestStreak}", "Серия", p.accent)
            } else {
                Stat("${state.hearts}", "Сердечки", p.danger)
            }
        }

        if (state.learned.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(p.surface)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("ТЫ РАЗОБРАЛ(А)", color = p.textMuted, fontSize = 11.sp, letterSpacing = 0.8.sp)
                Spacer(Modifier.height(4.dp))
                state.learned.forEach { term ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(term.display, color = p.text, fontSize = 13.sp)
                        Text(term.meaning, color = p.textMuted, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        VliButton("Продолжить", onClick = onContinue, filled = true, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Stat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = Vli.palette.textMuted, fontSize = 12.sp)
    }
}

@Composable
fun FailedScreen(state: PlayUiState, onRetry: () -> Unit, onExit: () -> Unit) {
    val p = Vli.palette
    Column(
        Modifier.fillMaxSize().background(p.bg).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(p.dangerSoft), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.SentimentDissatisfied, null, tint = p.danger, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            state.failReason.ifBlank { "Не получилось" },
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            color = p.text,
        )
        Spacer(Modifier.height(6.dp))
        Text("Не переживай, попробуй ещё раз", color = p.textMuted, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        VliButton("Начать заново", onClick = onRetry, filled = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        SecondaryButton("Выйти", onClick = onExit, modifier = Modifier.fillMaxWidth())
    }
}
