package com.example.cs2lineuptrainer.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.cs2lineuptrainer.domain.ThrowType

// Выбор способа броска. selected = null, пока игрок ещё ничего не выбрал.
@Composable
fun ThrowTypeSelector(
    selected: ThrowType?,
    onSelect: (ThrowType) -> Unit,
    modifier: Modifier = Modifier,
) {
    OptionChips<ThrowType?>(
        options = ThrowType.entries,
        selected = selected,
        label = { it?.label.orEmpty() },
        onSelect = { it?.let(onSelect) },
        modifier = modifier,
    )
}
