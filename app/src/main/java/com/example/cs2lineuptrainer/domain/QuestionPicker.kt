package com.example.cs2lineuptrainer.domain

import kotlin.random.Random

const val DEFAULT_SESSION_LENGTH = 10
const val UNLEARNED_WEIGHT = 3
const val LEARNED_WEIGHT = 1

// Подбирает вопросы для сессии тренировки с учётом выученности.
object QuestionPicker {

    // Возвращает count лайнапов из pool.
    // Невыученные выпадают чаще; пока есть другие, лайнап не повторяется;
    // один и тот же лайнап не идёт два раза подряд. Если pool меньше count, круг повторяется.
    fun pick(
        pool: List<Lineup>,
        progress: Map<String, LineupProgress>,
        count: Int = DEFAULT_SESSION_LENGTH,
        random: Random = Random.Default,
    ): List<Lineup> {
        if (pool.isEmpty()) return emptyList()

        val result = mutableListOf<Lineup>()
        var remaining = pool.toMutableList()

        repeat(count) {
            if (remaining.isEmpty()) remaining = pool.toMutableList()

            // Исключаем прошлый вопрос, но только если есть из чего выбрать.
            val lastId = result.lastOrNull()?.id
            val candidates = remaining.filter { it.id != lastId }.ifEmpty { remaining }

            val chosen = weightedChoice(candidates, progress, random)
            result += chosen
            remaining.remove(chosen)
        }
        return result
    }

    // Выбор по весам: бросаем «стрелку» в отрезок длиной в сумму весов и смотрим, на чей участок она попала.
    private fun weightedChoice(
        candidates: List<Lineup>,
        progress: Map<String, LineupProgress>,
        random: Random,
    ): Lineup {
        val weights = candidates.map { if (progress[it.id]?.learned == true) LEARNED_WEIGHT else UNLEARNED_WEIGHT }
        var roll = random.nextInt(weights.sum())
        for (i in candidates.indices) {
            roll -= weights[i]
            if (roll < 0) return candidates[i]
        }
        return candidates.last()
    }
}
