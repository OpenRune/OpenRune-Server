package org.rsmod.content.quest.area.baxtorianfalls.waterfall

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.util.Wearpos
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.table.GlarialRestrictionsRow
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.AMULET
import org.rsmod.content.quest.util.fadeFromBlack
import org.rsmod.content.quest.util.fadeToBlack
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.getInvObj
import org.rsmod.map.CoordGrid

internal object WaterfallCoords {
    val RAFT_CRASH = CoordGrid(2512, 3481, 0)

    val DOWNSTREAM = CoordGrid(2527, 3413, 0)

    val TREE_ISLAND = CoordGrid(2513, 3468, 0)

    val LEDGE = CoordGrid(2511, 3463, 0)

    val FALLS_ENTRY = CoordGrid(2575, 9862, 0)

    val TOMB_ENTRY = CoordGrid(2555, 9844, 0)

    fun onHudonIsland(coords: CoordGrid): Boolean =
        coords.level == 0 && coords.x in 2509..2515 && coords.z in 3476..3485

    fun onTreeIsland(coords: CoordGrid): Boolean =
        coords.level == 0 && coords.x in 2512..2513 && coords.z in 3466..3474
}

internal fun Player.hasAmulet(): Boolean = inv.contains(AMULET) || worn.contains(AMULET)

/** Every door and gate here is a wall on the north edge of its tile. */
internal fun BoundLocInfo.playerIsSouth(coords: CoordGrid): Boolean = coords.z <= this.coords.z

internal fun BoundLocInfo.tileAcross(from: CoordGrid): CoordGrid =
    if (from.z > coords.z) coords else coords.translateZ(1)

internal fun Player.wearsAmulet(): Boolean = worn.contains(AMULET)

internal fun Player.ownsAnywhere(obj: String): Boolean =
    inv.contains(obj) || worn.contains(obj) || invMap.getOrPut("inv.bank").contains(obj)

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

private val FORBIDDEN_NAMES: Set<String> by lazy {
    GlarialRestrictionsRow.getRow("dbrow.glarial_forbidden_names").entries.toSet()
}

private val FORBIDDEN_NAME_PARTS: List<String> by lazy {
    GlarialRestrictionsRow.getRow("dbrow.glarial_forbidden_parts").entries
}

private val ALLOWED_NAME_PARTS: List<String> by lazy {
    GlarialRestrictionsRow.getRow("dbrow.glarial_allowed_parts").entries
}

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
