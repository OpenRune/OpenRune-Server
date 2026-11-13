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
    const val EASY = 1
    const val MEDIUM = 2
    const val HARD = 3
    const val ELITE = 4
    const val MASTER = 5

    val PLAYABLE = BEGINNER..MASTER

    @Volatile var baseCapacity = 2

    private val trail_clue_step = param<DBRowType>("trail_clue_step")
    private val trail_item_group = param<Int>("trail_item_group")

    private val clueObjDifficulty: Map<Int, Int> by lazy {
        ServerCacheManager.getItems()
            .values
            .filter(::isStepScroll)
            .associate { it.id to ClueSteps.step(checkNotNull(objStep(it))).difficulty }
    }

    private val challengeObjs: Map<Int, ItemServerType> by lazy {
        ServerCacheManager.getItems()
            .values
            .filter { obj ->
                val step = objStep(obj)
                step != null && ClueSteps.isStep(step) && !isStepScroll(obj) && obj.isChallengeScroll()
            }
            .associateBy { checkNotNull(objStep(it)) }
    }

    fun tier(difficulty: Int): TrailTierRow =
        TrailTierRow.all().first { it.difficulty == difficulty }

    fun playableTiers(): List<TrailTierRow> = PLAYABLE.map(::tier)

    fun newWording(tier: TrailTierRow): Boolean = tier.difficulty >= ELITE

    fun foundText(tier: TrailTierRow, given: ItemServerType): String =
        when {
            given.id == tier.casketObj.id ->
                if (newWording(tier)) MasterParts.CASKET else "You've found a casket!"
            newWording(tier) || tier.difficulty == MEDIUM -> "You've found a new clue!"
            else -> "You've found another clue!"
        }

    fun deviceObj(difficulty: Int): String =
        if (difficulty == MASTER) "obj.master_device" else "obj.beginner_device"

    fun objStep(type: ItemServerType): Int? = type.paramOrNull(trail_clue_step)?.id

    fun itemGroup(type: ItemServerType): Int? = type.paramOrNull(trail_item_group)

    fun clueDifficulty(type: ItemServerType): Int? {
        TrailTierRow.all().firstOrNull { it.clueObj?.id == type.id }?.let { return it.difficulty }
        return clueObjDifficulty[type.id]
    }

    fun clueObjs(difficulty: Int): List<ItemServerType> =
        clueObjDifficulty.filterValues { it == difficulty }.keys.map(::type)

    fun challengeObj(step: Int): ItemServerType? = challengeObjs[step]

    fun challengeObjs(difficulty: Int): List<ItemServerType> =
        challengeObjs.filterKeys { ClueSteps.step(it).difficulty == difficulty }.values.toList()

    fun boxDifficulty(type: ItemServerType): Int? =
        TrailTierRow.all().firstOrNull { it.boxObj.id == type.id }?.difficulty

    fun ownedClues(player: Player, difficulty: Int): Int =
        player.trailStorage().sumOf { inv -> inv.countWhere { clueDifficulty(it) == difficulty } }

    fun ownedBoxes(player: Player, difficulty: Int): Int =
        player.trailStorage().sumOf { inv -> inv.countWhere { boxDifficulty(it) == difficulty } }

    fun owns(player: Player, type: ItemServerType): Boolean =
        player.trailStorage().any { inv -> inv.countWhere { it.id == type.id } > 0 }

    fun capacity(player: Player, tier: TrailTierRow): Int =
        baseCapacity +
            player.vars[varbit(tier.minorCaseVarbit)] +
            player.vars[varbit(tier.majorCaseVarbit)] +
            player.vars["varbit.scroll_case_mimic"]

    /**
     * Whether a new clue roll of [tier] may be handed out. Without scroll boxes a player holds one
     * clue per tier; with them, clues and boxes together fill the tier's capacity.
     */
    fun canReceive(player: Player, tier: TrailTierRow, boxes: Boolean): Boolean {
        val clues = ownedClues(player, tier.difficulty)
        if (!boxes) return clues == 0
        return clues + ownedBoxes(player, tier.difficulty) < capacity(player, tier)
    }

    private fun isStepScroll(type: ItemServerType): Boolean {
        val step = objStep(type) ?: return false
        return type.interfaceOptions.getOrNull(1) == CHECK_STEPS_OP && ClueSteps.isStep(step)
    }

    private fun ItemServerType.isChallengeScroll(): Boolean =
        interfaceOptions.getOrNull(0) == READ_OP && name.startsWith(CHALLENGE_SCROLL_NAME)

    private fun Player.trailStorage(): List<Inventory> = listOfNotNull(inv, invMap["inv.bank"])

    private fun Inventory.countWhere(predicate: (ItemServerType) -> Boolean): Int =
        objs.sumOf { obj -> obj?.let { if (predicate(it.type())) it.count else 0 } ?: 0 }

    private fun InvObj.type(): ItemServerType = type(id)

    internal fun type(id: Int): ItemServerType = checkNotNull(ServerCacheManager.getItem(id))

    internal fun varbit(id: Int): String = RSCM.getReverseMapping(RSCMType.VARBIT, id)

    internal fun varp(id: Int): String = RSCM.getReverseMapping(RSCMType.VARP, id)

    private const val CHECK_STEPS_OP = "Check steps"
    private const val READ_OP = "Read"
    private const val CHALLENGE_SCROLL_NAME = "Challenge scroll"
}

class TrailState(val player: Player, val tier: TrailTierRow) {
    private val lengthVarbit = TrailTiers.varbit(tier.lengthVarbit)
    private val progressVarbit = TrailTiers.varbit(tier.progressVarbit)

    var length: Int
        get() = player.vars[lengthVarbit]
        set(value) {
            VarPlayerIntMapSetter.set(player, lengthVarbit, value)
            syncRemaining()
        }

    var progress: Int
        get() = player.vars[progressVarbit]
        set(value) {
            VarPlayerIntMapSetter.set(player, progressVarbit, value)
            syncRemaining()
        }

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

    private fun syncRemaining() {
        if (tier.difficulty != TrailTiers.MASTER) return
        val remaining = if (length == 0) 0 else (length - progress).coerceAtLeast(0)
        MasterVars.setRemaining(player, remaining)
    }

    private companion object {
        const val NO_STEP = 0
    }
}
