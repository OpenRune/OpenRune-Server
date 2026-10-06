package org.rsmod.content.quest.area.baxtorianfalls.waterfall

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.util.Wearpos
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.content.quest.area.ardougne.fadeFromBlack
import org.rsmod.content.quest.area.ardougne.fadeToBlack
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.Amulet
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.getInvObj
import org.rsmod.map.CoordGrid

internal object WaterfallCoords {
    /** Where the log raft runs aground, beside the broken raft on Hudon's island. */
    val RAFT_CRASH = CoordGrid(2512, 3481, 0)

    /** The shore by Gerald and the tourist centre, where the river washes swimmers up. */
    val DOWNSTREAM = CoordGrid(2527, 3413, 0)

    /** The patch of land beside the dead tree, reached by roping the rock. */
    val TREE_ISLAND = CoordGrid(2513, 3468, 0)

    /** The ledge in front of the waterfall door, reached by roping the dead tree. */
    val LEDGE = CoordGrid(2511, 3463, 0)

    /** Just inside the waterfall, north of the exit door (2575,9861). */
    val FALLS_ENTRY = CoordGrid(2575, 9862, 0)

    /** Beside the ladder up out of Glarial's tomb (2556,9844). */
    val TOMB_ENTRY = CoordGrid(2555, 9844, 0)

    fun onHudonIsland(coords: CoordGrid): Boolean =
        coords.level == 0 && coords.x in 2509..2515 && coords.z in 3476..3485

    fun onTreeIsland(coords: CoordGrid): Boolean =
        coords.level == 0 && coords.x in 2512..2513 && coords.z in 3466..3474
}

internal fun Player.hasAmulet(): Boolean = inv.contains(Amulet) || worn.contains(Amulet)

/** Every door and gate here is a wall on the north edge of its tile. */
internal fun BoundLocInfo.playerIsSouth(coords: CoordGrid): Boolean = coords.z <= this.coords.z

/** The tile just across a north-edge wall from [from]. */
internal fun BoundLocInfo.tileAcross(from: CoordGrid): CoordGrid =
    if (from.z > coords.z) coords else coords.translateZ(1)

internal fun Player.wearsAmulet(): Boolean = worn.contains(Amulet)

internal fun Player.ownsAnywhere(obj: String): Boolean =
    inv.contains(obj) || worn.contains(obj) || invMap.getOrPut("inv.bank").contains(obj)

/**
 * Carries the player off down the river to [WaterfallCoords.DOWNSTREAM]. [ouch] is for the falls
 * themselves: the player takes a knock and says so.
 */
internal suspend fun ProtectedAccess.washDownstream(ouch: Boolean) {
    soundSynth(SPLASH_SOUND)
    spotanim(SPLASH_SPOTANIM)
    fadeToBlack()
    telejump(WaterfallCoords.DOWNSTREAM, TeleportType.Exempt)
    delay(1)
    fadeFromBlack()
    if (ouch) {
        val damage = FALL_DAMAGE.coerceAtMost(player.hitpoints - 1)
        if (damage > 0) {
            queueHit(delay = 1, type = HitType.Typeless, damage = damage)
        }
        say("Ouch!")
    }
}

/**
 * Glarial's tombstone only opens for visitors with peaceful intent. The wiki's restricted list:
 * weapons, armour (worn gear with combat bonuses, capes included) and the supplies to make it,
 * runes, ammunition and its supplies, looting bags, clue scrolls, magic secateurs, mythical capes,
 * skillcapes, Ardougne cloaks, weight-reducing camo clothing and the dwarf multicannon. Clothing
 * without combat bonuses (a Graceful cape among it), jewellery, food and potions pass, as do the
 * off-hand tomes, the Imcando hammer (off-hand), the chronicle and the goblin paint cannon.
 */
internal fun Player.carriesUnpeacefulItem(): Boolean {
    val carried = inv.filterNotNull { true } + worn.filterNotNull { true }
    return carried.any { getInvObj(it).isUnpeaceful() }
}

private fun ItemServerType.isUnpeaceful(): Boolean {
    val lower = name.lowercase()
    if (ALLOWED_NAME_PARTS.any { it in lower }) {
        return false
    }
    val slot = Wearpos[wearpos1]
    if (slot in ALWAYS_FORBIDDEN_SLOTS) {
        return true
    }
    if (slot in ARMOUR_SLOTS && BONUS_PARAMS.any { (paramOrNull(it) ?: 0) != 0 }) {
        return true
    }
    if (category == RUNE_CATEGORY) {
        return true
    }
    if (" rune" in lower && "pouch" !in lower) {
        return true
    }
    return lower in FORBIDDEN_NAMES || FORBIDDEN_NAME_PARTS.any { it in lower }
}

private const val SPLASH_SOUND = "synth.splash_and_river"
private const val SPLASH_SPOTANIM = "spotanim.watersplash"
private const val FALL_DAMAGE = 8

private val RUNE_CATEGORY = "category.rune".asRSCM(RSCMType.CATEGORY)

private val ALWAYS_FORBIDDEN_SLOTS = setOf(Wearpos.RightHand, Wearpos.Quiver)

private val ARMOUR_SLOTS =
    setOf(
        Wearpos.Hat,
        Wearpos.Back,
        Wearpos.Torso,
        Wearpos.LeftHand,
        Wearpos.Legs,
        Wearpos.Hands,
        Wearpos.Feet,
    )

private val FORBIDDEN_NAMES =
    setOf(
        "logs",
        "knife",
        "fletching knife",
        "needle",
        "thread",
        "ball of wool",
        "leather",
        "hard leather",
        "snakeskin",
        "nails",
        "feather",
        "bow string",
        "arrow shaft",
        "headless arrow",
        "looting bag",
        "magic secateurs",
        "mythical cape",
        "fancy boots",
        "fighting boots",
        "fancier boots",
        "spotted cape",
        "spottier cape",
    )

private val FORBIDDEN_NAME_PARTS =
    listOf(
        " logs",
        "arrowtips",
        "clue scroll",
        "(u)",
        "dragon leather",
        "cannon base",
        "cannon stand",
        "cannon barrels",
        "cannon furnace",
        "dwarf cannon set",
        "rune pack",
        "feather pack",
        "initiate harness",
        "proselyte harness",
        "ardougne cloak",
        " cape(t)",
        " camo top",
        " camo legs",
    )

private val ALLOWED_NAME_PARTS =
    listOf("tome of ", "imcando hammer (off-hand)", "chronicle", "goblin paint cannon")

private val BONUS_PARAMS =
    listOf(
        params.attack_stab,
        params.attack_slash,
        params.attack_crush,
        params.attack_magic,
        params.attack_ranged,
        params.defence_stab,
        params.defence_slash,
        params.defence_crush,
        params.defence_magic,
        params.defence_ranged,
        params.melee_strength,
        params.ranged_strength,
    )
