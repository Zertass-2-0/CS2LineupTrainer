package com.example.cs2lineuptrainer.data

import com.example.cs2lineuptrainer.R
import com.example.cs2lineuptrainer.domain.Area
import com.example.cs2lineuptrainer.domain.GrenadeType
import com.example.cs2lineuptrainer.domain.Lineup
import com.example.cs2lineuptrainer.domain.LineupMedia
import com.example.cs2lineuptrainer.domain.MouseButtons
import com.example.cs2lineuptrainer.domain.Movement
import com.example.cs2lineuptrainer.domain.Offset01
import com.example.cs2lineuptrainer.domain.Side
import com.example.cs2lineuptrainer.domain.ThrowModifier
import com.example.cs2lineuptrainer.domain.ThrowType

// Этот файл создаёт скрипт tools/convert_lineups.py. Вручную не править.
object MirageGenerated {
    // Каждая точка описана один раз, лайнапы ссылаются на неё.
    private val p_b_main = mapPoint(MirageCalibration, "b_main", "B main", 284.388214f, -749.841248f)
    private val p_heaven = mapPoint(MirageCalibration, "heaven", "Heaven", -344.489258f, -712.183777f)
    private val p_sunroom_close = mapPoint(MirageCalibration, "sunroom_close", "sunroom close corner", 579.240479f, -1449.972168f)
    private val p_long_close_t = mapPoint(MirageCalibration, "long_close_t", "long", 1315.96875f, 1587.953979f)
    private val p_a_site_center = mapPoint(MirageCalibration, "a_site_center", "A site", 27.414381f, 1513.494507f)
    private val p_ct_mid_right = mapPoint(MirageCalibration, "ct_mid_right", "connector exit right", -275.53125f, -93.582458f)
    private val p_boost = mapPoint(MirageCalibration, "boost", "boost", 887.675171f, 590.295776f)
    private val p_tbase_3rd_line = mapPoint(MirageCalibration, "tbase_3rd_line", "t base 3rd line", 2716.099365f, 16.538013f)
    private val p_connector = mapPoint(MirageCalibration, "connector", "connector", -345.940308f, -10.693665f)
    private val p_garbage = mapPoint(MirageCalibration, "garbage", "garbage", 1399.943481f, -188.033936f)
    private val p_ct_b_help = mapPoint(MirageCalibration, "ct_b_help", "ct_b_help", -503.564941f, -934.818665f)
    private val p_sunroom_far = mapPoint(MirageCalibration, "sunroom_far", "sunroom far corner", 1165.96875f, -1435.967285f)
    private val p_b_plant = mapPoint(MirageCalibration, "b_plant", "b_plant", -5.107361f, -1112.437134f)

    val lineups: List<Lineup> = listOf(
        Lineup(
            id = "mirage_smoke_bmain_heaven",
            mapId = MirageData.MAP_ID,
            grenade = GrenadeType.SMOKE,
            side = Side.T,
            area = Area.B,
            title = "Смок на Heaven с мэйна",
            target = p_heaven,
            origin = p_b_main,
            standView = LineupMedia.Image(R.drawable.gen_mirage_smoke_bmain_heaven_stand),
            aimPoint = Offset01(0.5f, 0.5f),
            throwType = ThrowType(
                modifier = ThrowModifier.NONE,
                movement = Movement.STAND,
                jump = false,
                buttons = MouseButtons.LMB,
            ),
            howTo = LineupMedia.Image(R.drawable.gen_mirage_smoke_bmain_heaven_howto),
            description = "Встаньте в B main и прицельтесь в ориентир. Смок ляжет на Heaven.",
        ),
        Lineup(
            id = "mirage_smoke_sunroom_heaven",
            mapId = MirageData.MAP_ID,
            grenade = GrenadeType.SMOKE,
            side = Side.T,
            area = Area.B,
            title = "Смок на Heaven с санрума",
            target = p_heaven,
            origin = p_sunroom_close,
            standView = LineupMedia.Image(R.drawable.gen_mirage_smoke_sunroom_heaven_stand),
            aimPoint = Offset01(0.5f, 0.5f),
            zoom = 4.0f,
            throwType = ThrowType(
                modifier = ThrowModifier.NONE,
                movement = Movement.STAND,
                jump = false,
                buttons = MouseButtons.LMB,
            ),
            howTo = LineupMedia.Image(R.drawable.gen_mirage_smoke_sunroom_heaven_howto),
            description = "Встаньте в sunroom, упритесь в бордюр и прицельтесь чуть правее светлого элемента на кромке стены. Смок ляжет на Heaven.",
        ),
        Lineup(
            id = "mirage_smoke_long_a_site",
            mapId = MirageData.MAP_ID,
            grenade = GrenadeType.SMOKE,
            side = Side.T,
            area = Area.A,
            title = "Смок с лога на сайт",
            target = p_a_site_center,
            origin = p_long_close_t,
            standView = LineupMedia.Image(R.drawable.gen_mirage_smoke_long_a_site_stand),
            aimPoint = Offset01(0.5f, 0.5f),
            throwType = ThrowType(
                modifier = ThrowModifier.NONE,
                movement = Movement.STAND,
                jump = false,
                buttons = MouseButtons.LMB,
            ),
            howTo = LineupMedia.Image(R.drawable.gen_mirage_smoke_long_a_site_howto),
            description = "Встаньте в лонг, упритесь в угол и прицельтесь в ориентир. Смок ляжет на середину плента, отрезав шорт и кт.",
        ),
        Lineup(
            id = "mirage_molly_boost",
            mapId = MirageData.MAP_ID,
            grenade = GrenadeType.MOLOTOV,
            side = Side.CT,
            area = Area.MID,
            title = "молотов на буст",
            target = p_boost,
            origin = p_ct_mid_right,
            standView = LineupMedia.Image(R.drawable.gen_mirage_molly_boost_stand),
            aimPoint = Offset01(0.5f, 0.5f),
            zoom = 5.0f,
            throwType = ThrowType(
                modifier = ThrowModifier.NONE,
                movement = Movement.STAND,
                jump = true,
                buttons = MouseButtons.LMB,
            ),
            howTo = LineupMedia.Image(R.drawable.gen_mirage_molly_boost_howto),
            description = "Встаньте в коннекторе, упритесь в угол правого перила и прицельтесь в ориентир. молотов ляжет на буст.",
        ),
        Lineup(
            id = "mirage_smoke_connector",
            mapId = MirageData.MAP_ID,
            grenade = GrenadeType.SMOKE,
            side = Side.T,
            area = Area.MID,
            title = "смок на коннектор с т спавна",
            target = p_connector,
            origin = p_tbase_3rd_line,
            standView = LineupMedia.Image(R.drawable.gen_mirage_smoke_connector_stand),
            aimPoint = Offset01(0.5f, 0.5f),
            zoom = 2.0f,
            throwType = ThrowType(
                modifier = ThrowModifier.NONE,
                movement = Movement.FORWARD_RUN,
                jump = true,
                buttons = MouseButtons.LMB,
            ),
            howTo = LineupMedia.Image(R.drawable.gen_mirage_smoke_connector_howto),
            description = "выровняйтесь по третьей линии зебры, с разбега бросьте в прыжке целясь в букву С",
        ),
        Lineup(
            id = "mirage_smoke_garbage_heaven",
            mapId = MirageData.MAP_ID,
            grenade = GrenadeType.SMOKE,
            side = Side.T,
            area = Area.B,
            title = "Смок на Heaven с гарбейджа (труба на -2y)",
            target = p_heaven,
            origin = p_garbage,
            standView = LineupMedia.Image(R.drawable.gen_mirage_smoke_garbage_heaven_stand),
            aimPoint = Offset01(0.5f, 0.5f),
            tolerance = 0.02f,
            zoom = 1.3f,
            throwType = ThrowType(
                modifier = ThrowModifier.CTRL,
                movement = Movement.STAND,
                jump = true,
                buttons = MouseButtons.LMB,
            ),
            howTo = LineupMedia.Image(R.drawable.gen_mirage_smoke_garbage_heaven_howto),
            description = "присядьте, поставьте угол левой трубы на отметку -2у, кидайте с прыжка",
        ),
        Lineup(
            id = "mirage_smoke_sunroom_ct",
            mapId = MirageData.MAP_ID,
            grenade = GrenadeType.SMOKE,
            side = Side.T,
            area = Area.B,
            title = "Смок с санрума на кт",
            target = p_ct_b_help,
            origin = p_sunroom_close,
            standView = LineupMedia.Image(R.drawable.gen_mirage_smoke_sunroom_ct_stand),
            aimPoint = Offset01(0.5f, 0.5f),
            throwType = ThrowType(
                modifier = ThrowModifier.NONE,
                movement = Movement.STAND,
                jump = true,
                buttons = MouseButtons.LMB_RMB,
            ),
            howTo = LineupMedia.Image(R.drawable.gen_mirage_smoke_sunroom_ct_howto),
            description = "Встаньте в sunroom, упритесь в бордюр и прицельтесь на угловой стык над отколотой плиткой, кидайте с прыжка с двух кнопок. Смок отрежет кт.",
        ),
        Lineup(
            id = "mirage_smoke_sunroom_site",
            mapId = MirageData.MAP_ID,
            grenade = GrenadeType.SMOKE,
            side = Side.T,
            area = Area.B,
            title = "Смок с санрума на плэнт",
            target = p_b_plant,
            origin = p_sunroom_far,
            standView = LineupMedia.Image(R.drawable.gen_mirage_smoke_sunroom_site_stand),
            aimPoint = Offset01(0.5f, 0.5f),
            zoom = 1.5f,
            throwType = ThrowType(
                modifier = ThrowModifier.NONE,
                movement = Movement.STAND,
                jump = false,
                buttons = MouseButtons.LMB,
            ),
            howTo = LineupMedia.Image(R.drawable.gen_mirage_smoke_sunroom_site_howto),
            description = "Встаньте в sunroom, упритесь в дальний угол и прицельтесь между окнами. Смок закроет плэнт.",
        ),
    )
}
