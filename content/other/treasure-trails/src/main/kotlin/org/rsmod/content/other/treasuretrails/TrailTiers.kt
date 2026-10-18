package org.rsmod.content.other.treasuretrails

import dev.openrune.ParamReferences.param
import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.DBRowType
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.TrailTierRow
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

fun ItemServerType.symbol(): String = RSCM.getReverseMapping(RSCMType.OBJ, id)

object TrailTiers {
    const val BEGINNER = 0

    private val trail_clue_step = param<DBRowType>("trail_clue_step")

    fun tier(difficulty: Int): TrailTierRow =
        TrailTierRow.all().first { it.difficulty == difficulty }

    /** The clue step an easy-elite clue obj stands for; beginner and master keep it in a varp. */
    fun objStep(type: ItemServerType): Int? = type.paramOrNull(trail_clue_step)?.id

    fun clueDifficulty(type: ItemServerType): Int? {
        TrailTierRow.all().firstOrNull { it.clueObj?.id == type.id }?.let { return it.difficulty }
        return objStep(type)?.let { ClueSteps.step(it).difficulty }
    }

    fun boxDifficulty(type: ItemServerType): Int? =
        TrailTierRow.all().firstOrNull { it.boxObj.id == type.id }?.difficulty

    fun ownedClues(player: Player, difficulty: Int): Int =
        player.trailStorage().sumOf { inv -> inv.countWhere { clueDifficulty(it) == difficulty } }

    fun ownedBoxes(player: Player, difficulty: Int): Int =
        player.trailStorage().sumOf { inv -> inv.countWhere { boxDifficulty(it) == difficulty } }

    /** Scroll boxes and scrolls share a per-tier cap that scroll cases raise up to five. */
    fun capacity(player: Player, tier: TrailTierRow): Int =
        BASE_CAPACITY +
            player.vars[varbit(tier.minorCaseVarbit)] +
            player.vars[varbit(tier.majorCaseVarbit)] +
            player.vars["varbit.scroll_case_mimic"]

    private fun Player.trailStorage(): List<Inventory> = listOfNotNull(inv, invMap["inv.bank"])

    private fun Inventory.countWhere(predicate: (ItemServerType) -> Boolean): Int =
        objs.sumOf { obj -> obj?.let { if (predicate(it.type())) it.count else 0 } ?: 0 }

    private fun InvObj.type(): ItemServerType = checkNotNull(ServerCacheManager.getItem(id))

    internal fun varbit(id: Int): String = RSCM.getReverseMapping(RSCMType.VARBIT, id)

    internal fun varp(id: Int): String = RSCM.getReverseMapping(RSCMType.VARP, id)

    private const val BASE_CAPACITY = 2
}

class TrailState(private val player: Player, val tier: TrailTierRow) {
    private val lengthVarbit = TrailTiers.varbit(tier.lengthVarbit)
    private val progressVarbit = TrailTiers.varbit(tier.progressVarbit)

    var length: Int
        get() = player.vars[lengthVarbit]
        set(value) = VarPlayerIntMapSetter.set(player, lengthVarbit, value)

    var progress: Int
        get() = player.vars[progressVarbit]
        set(value) = VarPlayerIntMapSetter.set(player, progressVarbit, value)

    /** The current step of a single-obj tier (beginner, master); `null` when no trail is active. */
    var step: Int?
        get() = stepVarp()?.let { player.vars[it] }?.takeIf { it != NO_STEP }
        set(value) {
            val varp = checkNotNull(stepVarp()) { "Tier ${tier.name} has no step varp." }
            VarPlayerIntMapSetter.set(player, varp, value ?: NO_STEP)
        }

    fun clear() {
        length = 0
        progress = 0
        if (stepVarp() != null) step = null
    }

    private fun stepVarp(): String? = tier.stepVarp?.let(TrailTiers::varp)

    private companion object {
        const val NO_STEP = 0
    }
}
