package com.example.cs2lineuptrainer.data

import com.example.cs2lineuptrainer.R
import com.example.cs2lineuptrainer.domain.Area
import com.example.cs2lineuptrainer.domain.GrenadeType
import com.example.cs2lineuptrainer.domain.Lineup
import com.example.cs2lineuptrainer.domain.LineupMedia
import com.example.cs2lineuptrainer.domain.Offset01
import com.example.cs2lineuptrainer.domain.Side
import com.example.cs2lineuptrainer.domain.ThrowType

// Этот файл создаёт скрипт tools/convert_lineups.py. Вручную не править.
object CacheGenerated {
    // Каждая точка описана один раз, лайнапы ссылаются на неё.
    private val p_b_main = cachePoint("b_main", "B main", 284.388214f, -749.841248f)
    private val p_heaven = cachePoint("heaven", "Heaven", -344.489258f, -712.183777f)
    private val p_sunroom_close = cachePoint("sunroom_close", "sunroom close corner", 579.240479f, -1449.972168f)
    private val p_long_close_t = cachePoint("long_close_t", "long", 1315.96875f, 1587.953979f)
    private val p_a_site_center = cachePoint("a_site_center", "A site", 27.414381f, 1513.494507f)
    private val p_ct_mid_right = cachePoint("ct_mid_right", "connector exit right", -275.53125f, -93.582458f)
    private val p_boost = cachePoint("boost", "boost", 887.675171f, 590.295776f)

    val lineups: List<Lineup> = listOf(
        Lineup(
            id = "smoke_bmain_heaven",
            mapId = CacheData.MAP_ID,
            grenade = GrenadeType.SMOKE,
            side = Side.T,
            area = Area.B,
            title = "Смок на Heaven с мэйна",
            target = p_heaven,
            origin = p_b_main,
            standView = LineupMedia.Image(R.drawable.gen_smoke_bmain_heaven_stand),
            aimPoint = Offset01(0.5f, 0.5f),
            throwType = ThrowType.LMB,
            howTo = LineupMedia.Image(R.drawable.gen_smoke_bmain_heaven_howto),
            description = "Встаньте в B main и прицельтесь в ориентир. Смок ляжет на Heaven.",
        ),
        Lineup(
            id = "smoke_sunroom_heaven",
            mapId = CacheData.MAP_ID,
            grenade = GrenadeType.SMOKE,
            side = Side.T,
            area = Area.B,
            title = "Смок на Heaven с санрума",
            target = p_heaven,
            origin = p_sunroom_close,
            standView = LineupMedia.Image(R.drawable.gen_smoke_sunroom_heaven_stand),
            aimPoint = Offset01(0.5f, 0.5f),
            zoom = 4.0f,
            throwType = ThrowType.LMB,
            howTo = LineupMedia.Image(R.drawable.gen_smoke_sunroom_heaven_howto),
            description = "Встаньте в sunroom, упритесь в бордюр и прицельтесь чуть правее светлого элемента на кромке стены. Смок ляжет на Heaven.",
        ),
        Lineup(
            id = "smoke_long_a_site",
            mapId = CacheData.MAP_ID,
            grenade = GrenadeType.SMOKE,
            side = Side.T,
            area = Area.A,
            title = "Смок с лога на сайт",
            target = p_a_site_center,
            origin = p_long_close_t,
            standView = LineupMedia.Image(R.drawable.gen_smoke_long_a_site_stand),
            aimPoint = Offset01(0.5f, 0.5f),
            throwType = ThrowType.LMB,
            howTo = LineupMedia.Image(R.drawable.gen_smoke_long_a_site_howto),
            description = "Встаньте в лонг, упритесь в угол и прицельтесь в ориентир. Смок ляжет на середину плента, отрезав шорт и кт.",
        ),
        Lineup(
            id = "molly_boost",
            mapId = CacheData.MAP_ID,
            grenade = GrenadeType.MOLOTOV,
            side = Side.CT,
            area = Area.MID,
            title = "молотов на буст",
            target = p_boost,
            origin = p_ct_mid_right,
            standView = LineupMedia.Image(R.drawable.gen_molly_boost_stand),
            aimPoint = Offset01(0.5f, 0.5f),
            zoom = 5.0f,
            throwType = ThrowType.JUMP_THROW,
            howTo = LineupMedia.Image(R.drawable.gen_molly_boost_howto),
            description = "Встаньте в коннекторе, упритесь в угол правого перила и прицельтесь в ориентир. молотов ляжет на буст.",
        ),
    )
}
