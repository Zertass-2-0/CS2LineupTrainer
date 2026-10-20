package com.example.cs2lineuptrainer.domain

// Сколько верных ответов подряд нужно, чтобы лайнап считался выученным.
const val LEARN_STREAK = 3

// Сколько ошибок подряд по выученному лайнапу снимают статус «выучен».
const val FORGET_MISSES = 2

data class LineupProgress(
    val lineupId: String,
    val correctStreak: Int = 0,
    val missStreak: Int = 0,
    val learned: Boolean = false,
)

// Чистая функция: возвращает новый прогресс после одной попытки, ничего не изменяя.
fun LineupProgress.afterAttempt(hit: Boolean): LineupProgress {
    if (hit) {
        val streak = correctStreak + 1
        return copy(
            correctStreak = streak,
            missStreak = 0,
            learned = learned || streak >= LEARN_STREAK,
        )
    }
    if (!learned) {
        return copy(correctStreak = 0)
    }
    val misses = missStreak + 1
    return if (misses >= FORGET_MISSES) {
        copy(correctStreak = 0, missStreak = 0, learned = false)
    } else {
        copy(missStreak = misses)
    }
}
