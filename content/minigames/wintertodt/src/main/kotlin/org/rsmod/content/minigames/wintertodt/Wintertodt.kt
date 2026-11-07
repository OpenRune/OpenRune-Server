package org.rsmod.content.minigames.wintertodt

import dev.openrune.ParamReferences.param
import dev.openrune.types.ItemServerType
import dev.openrune.types.enums.EnumTypeMap
import kotlin.math.min
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
    const val LIT_BRAZIERS_MAX = 3
    const val REWARD_POOL_MAX = 8000
    const val DRUIDIC_RITUAL = "quest_druidicritual"

    const val FLETCH_SAFE_MESSAGE = "You can't fletch here."

    const val HEADBAR_WARMTH = 79
    const val HEADBAR_SEGMENTS = 30
    const val HEADBAR_HEALTH = 0
    const val HITMARK_NPC_DAMAGE = 17
    const val HITMARK_NPC_HEAL = 6
    const val NPC_FIRE = "npc.wint_fire"
    const val NPC_BREWMA = "npc.wint_brewma"
    const val NPC_ISH = "npc.wint_pisc_navigator"
    const val NPC_KALT = "npc.wint_shay_guard"
    const val NPC_UNDOR = "npc.wint_master_smith"
    const val NPC_UNDOR_NORMAL = "npc.wint_master_smith_normal"
    const val NPC_UNDOR_DINH = "npc.wint_master_smith_dinh"
    const val NPC_ESTHER = "npc.wint_wcguild_rep"
    const val NPC_SOLDIER = "npc.wint_soldier"
    const val NPC_CAT = "npc.wint_troublecat"
    const val SNOWFALL_WARNING = "loc.clanwars_snowfalling"
    const val SPOTANIM_AREA_SPOT = "spotanim.wint_area_attack_spot"
    const val SPOTANIM_AREA_IMPACT = "spotanim.wint_area_attack_impact"
    const val SPOTANIM_AREA_BREATH = "spotanim.wyvern_skeleton_impact_breath"
    const val SPOTANIM_TRAVEL_BREATH = "spotanim.wyvern_skeleton_travel_breath"

    const val PILLAR_JUMP = "loc.wint_pillar_jump"
    const val PILLAR_JUMP_ANIM = "seq.human_spot_jump"
    const val PILLAR_JUMP_SYNTH = "synth.wilderness_ditch_jump"
    const val PILLAR_JUMP_LEVEL = 60
    const val PILLAR_JUMP_XP = 18.0
    const val PILLAR_JUMP_LOCK = 6
    const val PILLAR_JUMP_LOCKED = "You only just jumped over that."

    const val CART_SEARCH_DELAY = 3
    const val CART_PICK_ANIM = "seq.picking_mid_shorter"
    const val CART_PICK_SYNTH = "synth.pick"
    const val CART_FULL_MESSAGE =
        "You need at least one free inventory space to take from the reward cart."
    const val CART_EMPTY_MESSAGE =
        "There are no rewards here for you. Maybe you could help subdue the Wintertodt...."
    const val CART_TAKEN_MESSAGE =
        "You think you've taken as much as you're owed from the reward cart."

    const val SOUND_HIT = "synth.human_hit"
    const val SOUND_HIT_2 = "synth.human_hit_2"
    const val SOUND_HIT_4 = "synth.human_hit_4"
    const val SOUND_DEATH = "synth.human_death"
    const val SOUND_EXPLOSION = "synth.explosion"

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
            CoordGrid(1619, 3996, 0),
            CoordGrid(1619, 4018, 0),
            CoordGrid(1641, 4018, 0),
            CoordGrid(1641, 3996, 0),
        )

    val WARMTH_EXCLUDED_FOOD = setOf("obj.triangle_sandwich", "obj.rag_bottle_wine")

    val PYROMANCER_DOWN_LINES =
        listOf(
            "Mummy!",
            "My flame burns low.",
            "We are doomed.",
            "I think I'm dying.",
            "Ugh, help me!",
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
    const val EMPTY_VIAL = "obj.vial_empty"
    const val TORCH = "obj.wint_torch"
    const val TORCH_OFFHAND = "obj.wint_torch_offhand"

    const val HUD = "interface.wint_status"
    const val HUD_TARGET = "component.toplevel_osrs_stretch:overlay_hud"

    const val SCRIPT_UPDATE = 1421
    const val SCRIPT_INSIDE = 1433
    const val SCRIPT_OUTSIDE = 1432

    const val ANIM_CHOP = "seq.human_woodcutting_bronze_axe"
    const val ANIM_LIGHT = "seq.human_createfire"
    const val ANIM_LIGHT_TORCH = "seq.wint_light_brazier_bruma"
    const val ANIM_FIX = "seq.human_poh_build"
    const val ANIM_FLETCH = "seq.human_fletching"
    const val ANIM_HERB = "seq.picking_low"
    const val ANIM_FEED = "seq.human_pickuptable"
    const val ANIM_DRINK = "seq.human_eat"

    fun isInPrison(coords: CoordGrid): Boolean =
        coords.level == 0 && coords.x in 1610..1652 && coords.z in 3968..4025

    /** The southern entrance strip; no cold damage and no fletching here. */
    fun isSafe(coords: CoordGrid): Boolean = isInPrison(coords) && coords.z <= 3981

    fun standardDamage(firemaking: Int, warmItems: Int, litBraziers: Int): Int {
        val firemakingLevel = firemaking.coerceAtLeast(1)
        val warm = warmItems.coerceIn(0, WARM_ITEMS_MAX)
        val braziers = litBraziers.coerceIn(0, LIT_BRAZIERS_MAX)
        val percent =
            ((16 - warm - min(2 * braziers, 6)) * 100 / firemakingLevel).coerceAtLeast(1)
        return percent * 10
    }

    fun brazierDamage(firemaking: Int, warmItems: Int): Int {
        val firemakingLevel = firemaking.coerceAtLeast(1)
        val warm = warmItems.coerceIn(0, WARM_ITEMS_MAX)
        val percent = ((10 - warm) * 100 / firemakingLevel).coerceAtLeast(0)
        return (2 * percent).coerceAtLeast(2) * 10
    }

    fun areaDamage(firemaking: Int, warmItems: Int): Int {
        val firemakingLevel = firemaking.coerceAtLeast(1)
        val warm = warmItems.coerceIn(0, WARM_ITEMS_MAX)
        val percent = ((10 - warm) * 100 / firemakingLevel).coerceAtLeast(0)
        return (3 * percent).coerceAtLeast(1) * 10
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
    var nextSpeak: Int = 0
}

internal class WintertodtSession(val player: Player) {
    var points: Int = 0
    var lastCold: Int = Int.MIN_VALUE / 2
    var regenTenths: Int = 0
    var nextHud: Int = 0
    var nextDrink: Int = 0
}

internal var Player.wintWarmth by intVarBit("varbit.wint_warmth")
internal var Player.wintRewardPool by intVarBit("varbit.wint_reward_pool")
internal var Player.wintNeedsWarning by intVarBit("varbit.wint_needs_warning")
internal var Player.wintRespawnDelay by intVarBit("varbit.wint_transmit_respawndelay")
internal var Player.wintAreaAttack by intVarBit("varbit.wint_area_attack")
internal var Player.wintertodtHouseLocation by intVarBit("varbit.poh_house_location")
internal var Player.wintertodtKills by intVarp("varp.total_wintertodt_kills")
internal var Player.wintertodtLifetimeScore by intVarp("varp.wintertodt_lifetime_score")
internal var Player.wintertodtHighestScore by intVarp("varp.wintertodt_highest_score")
internal var Player.wintertodtIntroSeen by intVarp("varp.wintertodt_intro_seen")

internal var Player.caWintertodtKill5 by intVarBit("varbit.ca_task_wintertodt_killcount_1_completed")
internal var Player.caWintertodtKill10 by intVarBit("varbit.ca_task_wintertodt_killcount_2_completed")
internal var Player.caWintertodtHandyman by intVarBit("varbit.ca_task_wintertodt_mechanical_1_completed")
internal var Player.caWintertodtMummy by intVarBit("varbit.ca_task_wintertodt_mechanical_2_completed")
internal var Player.caWintertodtNoOneBehind by intVarBit("varbit.ca_task_wintertodt_perfection_1_completed")
internal var Player.caWintertodtCanWeFixIt by intVarBit("varbit.ca_task_wintertodt_perfection_2_completed")
internal var Player.caWintertodtCosy by intVarBit("varbit.ca_task_wintertodt_restriction_1_completed")
internal var Player.caWintertodtWhyFletch by intVarBit("varbit.ca_task_wintertodt_stamina_1_completed")
internal var Player.caWintertodtTotal by intVarBit("varbit.ca_total_tasks_completed_wintertodt")

internal object WintertodtParams {
    val successRates = param<EnumTypeMap<ItemServerType, Int>>("woodcutting_axe_success_rates")
}
