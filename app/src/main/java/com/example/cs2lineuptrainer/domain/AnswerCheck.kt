package com.example.cs2lineuptrainer.domain

// Запасная маска лайнапа: круг вокруг точки прицела в координатах ЦЕЛОГО кадра.
// tolerance задан в долях ширины показанного окна, поэтому для целого кадра он умножается на размер окна:
// при любом зуме зона на экране остаётся одного размера, а в игре при большом зуме она точнее.
fun fallbackHitMask(lineup: Lineup, window: FrameWindow): HitMask =
    CircleHitMask(lineup.aimPoint, lineup.tolerance * window.size)

// Ответ верный, если тап попал в зону И выбран правильный способ броска.
// marker - точка, которую поставил игрок в окне (0..1 внутри окна); маска работает в координатах целого кадра.
fun checkAnswer(
    lineup: Lineup,
    window: FrameWindow,
    marker: Offset01,
    throwType: ThrowType,
    mask: HitMask = fallbackHitMask(lineup, window),
): Boolean {
    val onFrame = window.toSource(marker)
    return mask.contains(onFrame.x, onFrame.y) && throwType == lineup.throwType
}
