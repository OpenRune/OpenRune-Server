package org.rsmod.content.raids.toa.raid.encounter.het.puzzle

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import org.rsmod.api.config.objParam
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.content.raids.toa.lobby.LobbyCavity
import org.rsmod.content.raids.toa.lobby.ToaPickaxeStorage
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.getInvObj

internal class HetPickaxe(val obj: InvObj, val tier: Int, val animation: String)

internal object HetPickaxeDamage {
    const val MID_LEVEL = 85
    const val HIGH_LEVEL = 99
    const val LOW_BASE = 8
    const val MID_BASE = 15
    const val HIGH_BASE = 17
    const val DRAGON_BONUS = 2
    const val BELOW_RUNE_PENALTY = 3
    const val RANDOM_SPREAD = 2
    const val RUNE_TIER = 6
    const val DRAGON_TIER = 7

    fun base(level: Int): Int =
        when {
            level >= HIGH_LEVEL -> HIGH_BASE
            level >= MID_LEVEL -> MID_BASE
            else -> LOW_BASE
        }

    fun minimum(level: Int, tier: Int): Int {
        var min = base(level)
        if (tier >= DRAGON_TIER) min += DRAGON_BONUS
        if (tier < RUNE_TIER) min -= BELOW_RUNE_PENALTY
        return min
    }

    fun range(level: Int, tier: Int): IntRange {
        val min = minimum(level, tier)
        return min..min + RANDOM_SPREAD
    }
}

private val ItemServerType.pickaxeAnim by objParam(params.skill_anim)
private val ItemServerType.pickaxeLevel by objParam(params.levelrequire)

internal fun Player.bestPickaxe(): HetPickaxe? {
    val level = miningLvl
    return listOf(worn, inv)
        .flatMap { it.pickaxes() }
        .filter { (_, type) -> level >= type.pickaxeLevel }
        .map { (obj, type) ->
            val tier = ToaPickaxeStorage.value(type.internalName)
            val seq = RSCM.getReverseMapping(RSCMType.SEQ, type.pickaxeAnim.id)
            HetPickaxe(obj, tier, seq)
        }
        .maxByOrNull { it.tier }
}

internal fun Player.hasPickaxe(): Boolean =
    listOf(worn, inv).any { it.pickaxes().isNotEmpty() }

private fun Inventory.pickaxes(): List<Pair<InvObj, ItemServerType>> =
    indices.mapNotNull { slot ->
        val obj = this[slot] ?: return@mapNotNull null
        val type = getInvObj(obj)
        if (type.isContentType(LobbyCavity.PICKAXE_CONTENT)) obj to type else null
    }
