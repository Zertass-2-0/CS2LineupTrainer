package com.example.cs2lineuptrainer.ui.trainer

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cs2lineuptrainer.domain.FrameWindow
import com.example.cs2lineuptrainer.domain.Lineup
import com.example.cs2lineuptrainer.domain.Offset01
import com.example.cs2lineuptrainer.domain.ThrowType
import com.example.cs2lineuptrainer.ui.components.RadarClip
import com.example.cs2lineuptrainer.ui.components.TapTargetImage
import com.example.cs2lineuptrainer.ui.components.ThrowTypeSelector
import com.example.cs2lineuptrainer.ui.components.VerticalProgressBar
import com.example.cs2lineuptrainer.ui.isLandscape

// Как сохранить маркер (или его отсутствие) при пересоздании экрана: два числа или пустой список.
private val MarkerSaver = listSaver<Offset01?, Float>(
    save = { marker -> if (marker == null) emptyList() else listOf(marker.x, marker.y) },
    restore = { values -> if (values.isEmpty()) null else Offset01(values[0], values[1]) },
)

// Экран вопроса: подпись, кусок радара с позицией, кадр для прицеливания, выбор броска и кнопка «Подтвердить».
// Выбор игрока (маркер и бросок) хранится здесь, а раскладка зависит от ориентации экрана.
@Composable
fun TrainerQuestionContent(
    questionNumber: Int,
    total: Int,
    lineup: Lineup,
    window: FrameWindow,
    @DrawableRes radar: Int,
    onConfirm: (Offset01, ThrowType) -> Unit,
    modifier: Modifier = Modifier,
) {
    // rememberSaveable: выбор до подтверждения переживает поворот экрана.
    // questionNumber в списке аргументов сбрасывает маркер и бросок при переходе к следующему вопросу.
    var marker by rememberSaveable(questionNumber, stateSaver = MarkerSaver) { mutableStateOf<Offset01?>(null) }
    var throwType by rememberSaveable(questionNumber) { mutableStateOf<ThrowType?>(null) }

    val selectedMarker = marker
    val selectedThrow = throwType
    val canConfirm = selectedMarker != null && selectedThrow != null
    val confirm = { if (selectedMarker != null && selectedThrow != null) onConfirm(selectedMarker, selectedThrow) }

    if (isLandscape()) {
        QuestionLandscape(
            questionNumber, total, lineup, window, radar,
            marker = selectedMarker,
            throwType = selectedThrow,
            canConfirm = canConfirm,
            onMarkerChange = { marker = it },
            onThrowChange = { throwType = it },
            onConfirm = confirm,
            modifier = modifier,
        )
    } else {
        QuestionPortrait(
            questionNumber, total, lineup, window, radar,
            marker = selectedMarker,
            throwType = selectedThrow,
            canConfirm = canConfirm,
            onMarkerChange = { marker = it },
            onThrowChange = { throwType = it },
            onConfirm = confirm,
            modifier = modifier,
        )
    }
}

// Портрет: всё в один прокручиваемый столбец.
@Composable
private fun QuestionPortrait(
    questionNumber: Int,
    total: Int,
    lineup: Lineup,
    window: FrameWindow,
    @DrawableRes radar: Int,
    marker: Offset01?,
    throwType: ThrowType?,
    canConfirm: Boolean,
    onMarkerChange: (Offset01) -> Unit,
    onThrowChange: (ThrowType) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier,
) {
    // remember: положение прокрутки не нужно сохранять, новый вопрос начинается сверху.
    val scrollState = remember(questionNumber) { ScrollState(0) }

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Вопрос $questionNumber из $total", style = MaterialTheme.typography.labelLarge)
        LinearProgressIndicator(
            progress = { (questionNumber - 1).toFloat() / total },
            modifier = Modifier.fillMaxWidth(),
        )

        Text(lineup.title, style = MaterialTheme.typography.headlineSmall)

        Text("Откуда бросать:", style = MaterialTheme.typography.titleMedium)
        RadarClip(radar = radar, center = lineup.origin)

        Text("Куда навести прицел? Тапните по кадру", style = MaterialTheme.typography.titleMedium)
        TapTargetImage(
            media = lineup.standView,
            window = window,
            marker = marker,
            onTap = onMarkerChange,
        )

        Text("Как бросать?", style = MaterialTheme.typography.titleMedium)
        ThrowTypeSelector(selected = throwType, onSelect = onThrowChange)

        Button(onClick = onConfirm, enabled = canConfirm, modifier = Modifier.fillMaxWidth()) {
            Text("Подтвердить")
        }
    }
}

// Альбом: слева вертикальная полоса прогресса, правее три колонки, которые можно листать вправо:
// 1) название, способ броска и «Подтвердить», 2) кадр для прицеливания, 3) откуда бросать.
@Composable
private fun QuestionLandscape(
    questionNumber: Int,
    total: Int,
    lineup: Lineup,
    window: FrameWindow,
    @DrawableRes radar: Int,
    marker: Offset01?,
    throwType: ThrowType?,
    canConfirm: Boolean,
    onMarkerChange: (Offset01) -> Unit,
    onThrowChange: (ThrowType) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier = modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VerticalProgressBar(
            fraction = (questionNumber - 1).toFloat() / total,
            label = "$questionNumber/$total",
            modifier = Modifier.fillMaxHeight(),
        )

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxHeight()) {
            // Кадр занимает всю высоту под подписью (формат 16:9), поэтому его ширина считается из высоты.
            val frameWidth = ((maxHeight - 32.dp) * 16f / 9f).coerceAtLeast(240.dp)
            // remember: новый вопрос начинается с первой колонки.
            val horizontalScroll = remember(questionNumber) { ScrollState(0) }

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .horizontalScroll(horizontalScroll),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Колонка 1: название и способ броска. «Подтвердить» закреплена внизу и всегда на виду,
                // даже если кнопок способов броска много и они прокручиваются.
                Column(
                    modifier = Modifier.width(260.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(lineup.title, style = MaterialTheme.typography.headlineSmall)
                        Text("Как бросать?", style = MaterialTheme.typography.titleMedium)
                        ThrowTypeSelector(selected = throwType, onSelect = onThrowChange)
                    }
                    Button(onClick = onConfirm, enabled = canConfirm, modifier = Modifier.fillMaxWidth()) {
                        Text("Подтвердить")
                    }
                }

                Column(
                    modifier = Modifier.width(frameWidth).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Куда целиться? Тапните по кадру", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    TapTargetImage(
                        media = lineup.standView,
                        window = window,
                        marker = marker,
                        onTap = onMarkerChange,
                    )
                }

                // Колонка 3: откуда бросать.
                Column(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Откуда бросать:", style = MaterialTheme.typography.titleMedium)
                    RadarClip(radar = radar, center = lineup.origin)
                }
            }
        }
    }
}
