package com.example.cs2lineuptrainer.ui.trainer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cs2lineuptrainer.ui.theme.HitColor
import com.example.cs2lineuptrainer.ui.theme.MissColor
import kotlinx.coroutines.delay

// Время, пока надпись «Попал / Не попал» остаётся на экране, до начала растворения.
private const val FEEDBACK_VISIBLE_MS = 900L

// Обёртка: берёт состояние из ViewModel и выбирает, какой из трёх экранов показать.
@Composable
fun TrainerScreen(viewModel: TrainerViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session = state.session

    // «Назад» во время тренировки возвращает к настройкам.
    BackHandler(enabled = session != null, onBack = viewModel::onBackToSetup)

    Box(modifier = modifier.fillMaxSize()) {
        when {
            session == null -> TrainerSetupContent(
                state = state,
                onSideSelected = viewModel::onSideSelected,
                onAreaSelected = viewModel::onAreaSelected,
                onStart = viewModel::onStart,
            )

            session.isFinished -> TrainerSummaryContent(
                session = session,
                onRestart = viewModel::onStart,
                onBackToSetup = viewModel::onBackToSetup,
            )

            else -> TrainerQuestionContent(
                questionNumber = session.results.size + 1,
                total = session.questions.size,
                lineup = session.questions[session.results.size].lineup,
                window = session.questions[session.results.size].window,
                radar = state.map.radar,
                onConfirm = viewModel::onConfirm,
            )
        }

        FeedbackBanner(
            feedback = state.feedback,
            onShown = viewModel::onFeedbackShown,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
        )
    }
}

// Надпись «Попал» / «Не попал»: появляется, держится меньше секунды и растворяется.
@Composable
private fun FeedbackBanner(
    feedback: Feedback?,
    onShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // remember: это временное состояние анимации, оно не должно переживать пересоздание экрана.
    var visible by remember { mutableStateOf(false) }
    var lastHit by remember { mutableStateOf(false) }

    // Запускается заново при каждом новом сообщении (у него новый number).
    LaunchedEffect(feedback?.number) {
        if (feedback != null) {
            lastHit = feedback.hit
            visible = true
            delay(FEEDBACK_VISIBLE_MS)
            visible = false
            onShown()
        }
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(600)),
    ) {
        Surface(shape = RoundedCornerShape(16.dp), color = if (lastHit) HitColor else MissColor) {
            Text(
                text = if (lastHit) "Попал!" else "Не попал",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
            )
        }
    }
}
