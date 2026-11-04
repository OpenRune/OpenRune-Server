package org.rsmod.content.minigames.wintertodt

import kotlin.math.min
import kotlin.math.roundToInt
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocInfo
import org.rsmod.map.CoordGrid

internal object Wintertodt {
    const val NAME = "Wintertodt"

    const val WARMTH_MAX = 1000
    const val ENERGY_MAX = 3500
    const val ENERGY_PER_PERCENT = 35
    const val DRAIN_INTERVAL = 14
    const val ATTACK_INTERVAL = 5
    const val TARGET_COOLDOWN = 10
    const val RESPAWN_TIMER = 100
    const val RESPAWN_STEP = 2
    const val FM_LEVEL = 50
    const val POINT_THRESHOLD = 500
    const val WARM_ITEMS_MAX = 4
    const val REWARD_POOL_MAX = 8000

    val DOOR = CoordGrid(1627, 3964, 0)
    val ENTRY = CoordGrid(1628, 3979, 0)
    val EXIT = CoordGrid(1631, 3963, 0)
    val STORM = CoordGrid(1627, 4004, 0)

    // HUD order used by wint_update: 1 = SW, 2 = NW, 3 = NE, 4 = SE.
    val BRAZIER_COORDS =
        listOf(
            CoordGrid(1620, 3997, 0),
            CoordGrid(1620, 4015, 0),
            CoordGrid(1638, 4015, 0),
            CoordGrid(1638, 3997, 0),
        )

    val PYROMANCER_COORDS =
        listOf(
            CoordGrid(1615, 3993, 0),
            CoordGrid(1615, 4021, 0),
            CoordGrid(1645, 4021, 0),
            CoordGrid(1645, 3993, 0),
        )

    const val BRAZIER_UNLIT = "loc.wint_brazier"
    const val BRAZIER_BROKEN = "loc.wint_brazier_broken"
    const val BRAZIER_LIT = "loc.wint_brazier_lit"

    const val STORM_ACTIVE = "loc.wint_snow_storm"
    const val STORM_IDLE = "loc.wint_snow_storm_idle"

    const val WARNING_SNOW = "loc.wint_area_attack_snow"
    const val WARNING_ICICLE = "loc.wint_area_attack_icicle"

    const val NPC_PYROMANCER = "npc.wint_wizard"
    const val NPC_PYROMANCER_DOWN = "npc.wint_wizard_down"

    const val ROOT = "obj.wint_bruma_root"
    const val KINDLING = "obj.wint_bruma_kindling"
    const val VIAL = "obj.wint_vial"
    const val HERB = "obj.wint_herb"
    const val POTION4 = "obj.wint_potion4"
    const val POTION3 = "obj.wint_potion3"
    const val POTION2 = "obj.wint_potion2"
    const val POTION1 = "obj.wint_potion1"
    const val EMPTY_VIAL = "obj.vial"

    const val HUD = "interface.wint_status"
    const val HUD_TARGET = "component.toplevel_osrs_stretch:overlay_hud"

    const val SCRIPT_UPDATE = 1421
    const val SCRIPT_INSIDE = 1433
    const val SCRIPT_OUTSIDE = 1432

    const val ANIM_CHOP = "seq.human_axe_chop"
    const val ANIM_LIGHT = "seq.human_createfire"
    const val ANIM_FIX = "seq.human_smithing"
    const val ANIM_FLETCH = "seq.human_knife_chop"
    const val ANIM_HERB = "seq.human_pickuptable"
    const val ANIM_DRINK = "seq.human_herbing_vial"

    fun isInPrison(coords: CoordGrid): Boolean =
        coords.level == 0 && coords.x in 1610..1652 && coords.z in 3968..4025

    /** The southern entrance strip; no cold damage and no fletching here. */
    fun isSafe(coords: CoordGrid): Boolean = isInPrison(coords) && coords.z <= 3981

    fun standardDamage(firemaking: Int, warmItems: Int, litBraziers: Int): Int {
        val b = min(litBraziers, 3)
        val percent =
            ((16 - warmItems - min(2 * b, 6)) * firemaking / 100.0)
                .roundToInt()
                .coerceAtLeast(0)
        return percent * 10
    }

    fun brazierDamage(firemaking: Int, warmItems: Int): Int {
        val percent = ((10 - warmItems) * firemaking / 100.0).toInt().coerceAtLeast(0)
        return 2 * percent * 10
    }

    fun areaDamage(firemaking: Int, warmItems: Int): Int {
        val percent = ((10 - warmItems) * firemaking / 100.0).toInt().coerceAtLeast(0)
        return 3 * percent * 10
    }

    fun pointsToRewards(points: Int): Int {
        if (points < POINT_THRESHOLD) return 0
        return 1 + points / POINT_THRESHOLD
    }

    fun rewardFraction(points: Int): Int = if (points < POINT_THRESHOLD) 0 else points % POINT_THRESHOLD
}

internal enum class BrazierState(val state: Int, val loc: String) {
    BROKEN(0, Wintertodt.BRAZIER_BROKEN),
    UNLIT(1, Wintertodt.BRAZIER_UNLIT),
    LIT(2, Wintertodt.BRAZIER_LIT),
}

internal class Brazier(val index: Int, val coords: CoordGrid) {
    var state: BrazierState = BrazierState.UNLIT
    var loc: LocInfo? = null
    var nextDrain: Int = 0
    var lastHit: Int = Int.MIN_VALUE / 2
}

internal class Pyromancer(val index: Int, val coords: CoordGrid) {
    var npc: Npc? = null
    var hp: Int = 14
    var down: Boolean = false
    var lastHit: Int = Int.MIN_VALUE / 2
    var resumeAt: Int = 0
}

internal class WintertodtSession(val player: Player) {
    var points: Int = 0
    var lastCold: Int = Int.MIN_VALUE / 2
    var regenTenths: Int = 0
    var nextHud: Int = 0
}

internal var Player.wintWarmth by intVarBit("varbit.wint_warmth")
internal var Player.wintRewardPool by intVarBit("varbit.wint_reward_pool")
internal var Player.wintNeedsWarning by intVarBit("varbit.wint_needs_warning")
internal var Player.wintRespawnDelay by intVarBit("varbit.wint_transmit_respawndelay")
internal var Player.wintAreaAttack by intVarBit("varbit.wint_area_attack")
internal var Player.wintertodtKills by intVarp("varp.total_wintertodt_kills")
