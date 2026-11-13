package org.rsmod.content.other.treasuretrails

import dev.openrune.util.Wearpos
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.table.cluehelper.CluehelperClueCrypticRow
import org.rsmod.api.table.cluehelper.CluehelperClueSkillchallengeRow
import org.rsmod.content.other.treasuretrails.TrailFlow.advance
import org.rsmod.game.entity.Player

/**
 * Master's three-part step: the clue reads three cryptics, each solved one hands over a torn part,
 * and combining all three gives the next step. Which cryptics make up the step is stored as indices
 * into [candidates] in `varp.trail_master_parts`, next to a found flag per part.
 */
object MasterParts {
    const val COUNT = 3
    const val NO_SPACE = "Hey, I have something for you but you don't have any space for it!"
    const val HANDED = "You are handed a torn part of a clue scroll."
    const val FOUND = "You find a torn part of a clue scroll."
    const val ALREADY_FOUND =
        "You distinctly remember already finding a torn clue scroll part here and there " +
            "isn't another one to take."
    const val NEW_CLUE = "You've obtained a new clue!"
    const val CASKET = "You've obtained a casket!"
    private const val MISSING = "I cannot combine the pieces without having all 3."
    private const val NO_CLUE =
        "You cannot see how to combine the 3 pieces without also carrying your clue scroll."
    private const val INSPECT = "There must be more parts to this. If I can combine them together..."
    private const val GOD_ITEMS = 3

    val partObjs = listOf("obj.trail_master_part1", "obj.trail_master_part2", "obj.trail_master_part3")

    private val indexVarbits =
        listOf(
            "varbit.trail_master_part1_index",
            "varbit.trail_master_part2_index",
            "varbit.trail_master_part3_index",
        )
    private val foundVarbits =
        listOf(
            "varbit.trail_master_part1_found",
            "varbit.trail_master_part2_found",
            "varbit.trail_master_part3_found",
        )

    val combineRow: Int =
        CluehelperClueSkillchallengeRow.getRow("dbrow.cluehelper_skillchallenge_master_25").rowId

    val faloIntroRow: Int =
        CluehelperClueCrypticRow.getRow("dbrow.cluehelper_cryptic_master_falo_the_bard").rowId

    private val notParts: Set<Int> =
        setOf(
            faloIntroRow,
            CluehelperClueCrypticRow.getRow("dbrow.cluehelper_cryptic_master_jorral").rowId,
            CluehelperClueCrypticRow.getRow("dbrow.cluehelper_cryptic_master_watson").rowId,
        )

    private val viggora: Int =
        CluehelperClueCrypticRow.getRow("dbrow.cluehelper_cryptic_master_viggora").rowId

    private val godItemSteps: Set<Int> =
        setOf(
            CluehelperClueCrypticRow.getRow("dbrow.cluehelper_cryptic_master_mage_of_zamorak").rowId,
            CluehelperClueCrypticRow.getRow("dbrow.cluehelper_cryptic_master_juna").rowId,
        )

    /** Every master cryptic that can be a part; the order is fixed so stored indices stay valid. */
    val candidates: List<ClueStep> =
        ClueSteps.all(TrailTiers.MASTER)
            .filter { it.kind == ClueKind.Cryptic && it.rowId !in notParts }
            .sortedBy { it.rowId }

    /** Viggora needs the Curse of the Empty Lord, which has no content here yet. */
    private val rollable: List<ClueStep> =
        candidates.filter { !it.restricted && it.rowId != viggora }

    val standaloneExcluded: Set<Int> = candidates.map { it.rowId }.toSet() + faloIntroRow

    fun isCombine(step: ClueStep): Boolean = step.rowId == combineRow

    fun roll(player: Player, random: GameRandom) {
        val pool = rollable.toMutableList()
        for (part in 0 until COUNT) {
            val chosen = random.pick(pool)
            pool.remove(chosen)
            VarPlayerIntMapSetter.set(player, indexVarbits[part], candidates.indexOf(chosen))
            VarPlayerIntMapSetter.set(player, foundVarbits[part], 0)
        }
    }

    /** Distinct parts mean at most one index is 0, so all zeros means nothing was rolled. */
    fun rolled(player: Player): Boolean = indexVarbits.any { player.vars[it] != 0 }

    fun ensureRolled(player: Player, random: GameRandom) {
        if (!rolled(player)) roll(player, random)
    }

    fun clear(player: Player) {
        for (part in 0 until COUNT) {
            VarPlayerIntMapSetter.set(player, indexVarbits[part], 0)
            VarPlayerIntMapSetter.set(player, foundVarbits[part], 0)
        }
    }

    fun stepChanged(player: Player, step: ClueStep, random: GameRandom) {
        clear(player)
        if (isCombine(step)) roll(player, random)
    }

    fun part(player: Player, index: Int): ClueStep = candidates[player.vars[indexVarbits[index]]]

    fun found(player: Player, index: Int): Boolean = player.vars[foundVarbits[index]] != 0

    fun owns(player: Player, index: Int): Boolean {
        val obj = partObjs[index]
        return player.inv.count(obj) > 0 || (player.invMap["inv.bank"]?.count(obj) ?: 0) > 0
    }

    fun open(player: Player, index: Int): Boolean = !found(player, index) || !owns(player, index)

    /** The part indices of the held three-part step, or empty when it is not the current step. */
    fun active(player: Player): List<Int> {
        val master = TrailTiers.tier(TrailTiers.MASTER)
        val step = TrailFlow.currentStep(player, master) ?: return emptyList()
        if (!isCombine(step) || !rolled(player) || !TrailFlow.holdsClue(player, master)) {
            return emptyList()
        }
        return (0 until COUNT).toList()
    }

    fun openParts(player: Player): List<Int> = active(player).filter { open(player, it) }

    fun ownedParts(player: Player): List<Int> =
        active(player).filter { found(player, it) && owns(player, it) }

    fun text(player: Player): String =
        (0 until COUNT).joinToString("<br><br>") { index ->
            val text = part(player, index).text.orEmpty()
            if (found(player, index)) "<str>$text</str>" else text
        }

    fun refreshInfobox(player: Player) {
        val next = openParts(player).firstOrNull()?.let { part(player, it).rowId } ?: combineRow
        VarPlayerIntMapSetter.set(player, TrailFlow.INFOBOX, next)
    }

    /** Whether [player] meets the extra condition some parts carry, beyond the usual outfit check. */
    fun meets(player: Player, step: ClueStep): Boolean {
        if (step.rowId in godItemSteps) {
            val outfit = step.outfit ?: return false
            return ClueOutfits.wornCount(player, outfit) >= GOD_ITEMS
        }
        return ClueOutfits.wearing(player, step)
    }

    fun carriesRequirements(player: Player, step: ClueStep): Boolean {
        if (step.itemRequirements.isEmpty()) return true
        val have = buildSet {
            for (slot in 0 until player.inv.size) player.inv[slot]?.let { add(it.id) }
            for (obj in player.worn.objs) obj?.let { add(it.id) }
        }
        return step.itemRequirements.all { row -> row.item.any { it.id in have } }
    }

    fun ProtectedAccess.give(index: Int): Boolean {
        if (!open(player, index)) return false
        if (invAdd(inv, partObjs[index], 1).failure) return false
        VarPlayerIntMapSetter.set(player, foundVarbits[index], 1)
        refreshInfobox(player)
        return true
    }

    suspend fun ProtectedAccess.combine() {
        val master = TrailTiers.tier(TrailTiers.MASTER)
        if (active(player).isEmpty()) {
            mes(NO_CLUE)
            return
        }
        val complete =
            (0 until COUNT).all { found(player, it) && inv.count(partObjs[it]) > 0 }
        if (!complete) {
            startDialogue { mesbox(MISSING) }
            return
        }
        var consumed = false
        val given =
            advance(master) { slot, obj ->
                if (invReplace(inv, slot, 1, obj).failure) return@advance false
                partObjs.forEach { invDel(inv, it, 1) }
                consumed = true
                true
            } ?: return
        if (!consumed) partObjs.forEach { invDel(inv, it, 1) }
        val text = if (given.id == master.casketObj.id) CASKET else NEW_CLUE
        startDialogue { objbox(given.symbol(), text) }
    }

    suspend fun ProtectedAccess.inspect() {
        startDialogue { mesbox(INSPECT) }
    }

    fun isWornAny(player: Player): Boolean =
        WORN_SLOTS.any { player.worn[it.slot] != null }

    private val WORN_SLOTS =
        listOf(Wearpos.Hat, Wearpos.RightHand, Wearpos.Torso, Wearpos.LeftHand, Wearpos.Legs)
}
