package com.example.cs2lineuptrainer.ui.trainer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cs2lineuptrainer.ui.theme.HitColor
import com.example.cs2lineuptrainer.ui.theme.MissColor

// Итог сессии: счёт, сколько лайнапов стали выученными и список всех вопросов.
@Composable
fun TrainerSummaryContent(
    session: TrainerSession,
    onRestart: () -> Unit,
    onBackToSetup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Тренировка завершена", style = MaterialTheme.typography.headlineMedium)
        Text("Попаданий: ${session.hits} из ${session.questions.size}", style = MaterialTheme.typography.titleLarge)
        Text("Новых выученных лайнапов: ${session.newlyLearned}", style = MaterialTheme.typography.bodyLarge)

        // weight(1f): список занимает всё свободное место между заголовком и кнопками.
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Один и тот же лайнап может встретиться дважды, поэтому ключ - номер вопроса.
            itemsIndexed(session.results, key = { index, _ -> index }) { index, result ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${index + 1}. ${result.lineup.title}", style = MaterialTheme.typography.titleMedium)
                            if (result.lineup.id in session.learnedNow) {
                                Text("Выучен", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Text(
                            text = if (result.hit) "Попал" else "Не попал",
                            color = if (result.hit) HitColor else MissColor,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onRestart, modifier = Modifier.weight(1f)) { Text("Ещё раз") }
            OutlinedButton(onClick = onBackToSetup, modifier = Modifier.weight(1f)) { Text("Настройки") }
        }
    }
}
