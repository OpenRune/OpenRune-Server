package org.rsmod.content.other.treasuretrails

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import java.util.concurrent.ConcurrentHashMap
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.table.TrailMapInterfaceRow
import org.rsmod.api.table.TrailTierRow
import org.rsmod.api.table.cluehelper.CluehelperClueCrypticRow
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

/** Trail state for the single-obj tiers, where the current step lives in a varp. */
object TrailFlow {
    private val pools = ConcurrentHashMap<Int, List<Int>>()

    fun clueSlot(player: Player, tier: TrailTierRow): Int? {
        val clue = tier.clueObj ?: return null
        return (0 until player.inv.size).firstOrNull { player.inv[it]?.id == clue.id }
    }

    fun holdsClue(player: Player, tier: TrailTierRow): Boolean = clueSlot(player, tier) != null

    fun currentStep(player: Player, tier: TrailTierRow): ClueStep? =
        TrailState(player, tier).step?.let(ClueSteps::step)

    fun startedStep(player: Player, tier: TrailTierRow, random: GameRandom): ClueStep {
        val state = TrailState(player, tier)
        state.step?.let { return ClueSteps.step(it) }
        state.length = random.of(tier.stepsMin, tier.stepsMax)
        state.progress = 0
        return setStep(player, tier, rollStep(tier, random, exclude = null))
    }

    fun resetTrail(player: Player, tier: TrailTierRow) {
        TrailState(player, tier).clear()
        VarPlayerIntMapSetter.set(player, "varp.cluehelper_infobox_clue", NO_INFOBOX)
        VarPlayerIntMapSetter.set(player, "varp.trail_hotcold_last_check_coord", 0)
    }

    fun ProtectedAccess.read(tier: TrailTierRow) {
        val step = startedStep(player, tier, random)
        if (step.kind == ClueKind.Map) {
            val map = TrailMapInterfaceRow.all().firstOrNull { it.step.rowId == step.rowId }
            if (map == null) {
                mes("Nothing interesting happens.")
                return
            }
            ifOpenMainModal(RSCM.getReverseMapping(RSCMType.INTERFACE, map.`interface`))
            return
        }
        ifOpenMainModal("interface.trail_cluetext")
        ifSetText("component.trail_cluetext:text", clueText(step))
    }

    /** A scroll box becomes a fresh clue in one inv transaction; a held clue blocks it. */
    suspend fun ProtectedAccess.openBox(tier: TrailTierRow) {
        val clue = checkNotNull(tier.clueObj).symbol()
        if (TrailTiers.ownedClues(player, tier.difficulty) > 0) {
            mes(ClueTakeHook.ALREADY_HAVE_CLUE)
            return
        }
        if (invReplace(inv, tier.boxObj.symbol(), 1, clue).failure) {
            mes("You don't have enough inventory space.")
            return
        }
        resetTrail(player, tier)
        startDialogue { objbox(clue, "You open the clue box and find a clue scroll inside!") }
    }

    fun ProtectedAccess.checkSteps(tier: TrailTierRow) {
        val done = TrailState(player, tier).progress
        mes("You have completed $done ${if (done == 1) "step" else "steps"} on this clue scroll.")
    }

    /**
     * Completes the current step. The final step swaps the clue for the tier's casket in one inv
     * transaction; earlier steps only roll the next step. Returns the obj the player now holds for
     * the trail, or `null` when the clue is gone and nothing changed.
     */
    fun ProtectedAccess.advance(tier: TrailTierRow): ItemServerType? {
        val clue = tier.clueObj ?: return null
        val slot = clueSlot(player, tier) ?: return null
        val state = TrailState(player, tier)
        val progress = state.progress + 1
        if (progress >= state.length) {
            if (invReplace(inv, slot, 1, tier.casketObj).failure) {
                return null
            }
            resetTrail(player, tier)
            return tier.casketObj
        }
        state.progress = progress
        setStep(player, tier, rollStep(tier, random, exclude = state.step))
        return clue
    }

    /** Charlie and Sherlock hand out one of their skill challenges in place of the cryptic step. */
    fun skillChallengeFor(
        player: Player,
        step: ClueStep,
        npc: Npc,
        random: GameRandom,
    ): ClueStep? {
        if (step.kind != ClueKind.Cryptic) return null
        val challenges =
            ClueSteps.all(step.difficulty)
                .filter { it.kind == ClueKind.SkillChallenge }
                .filter { challenge ->
                    val target = ClueSteps.target(player, challenge) as? ClueTarget.TalkTo
                    target?.matches(npc) ?: false
                }
        return random.pickOrNull(challenges)
    }

    fun assign(player: Player, tier: TrailTierRow, step: ClueStep) {
        setStep(player, tier, step.rowId)
    }

    fun ClueTarget.TalkTo.matches(npc: Npc): Boolean {
        val ids = setOfNotNull(row.npc.id, row.fallbackNpc?.id)
        return npc.type.id in ids || npc.visType.id in ids
    }

    private fun clueText(step: ClueStep): String =
        when (step.kind) {
            ClueKind.Anagram ->
                "The anagram reveals who to speak to next:<br>${step.text.orEmpty().uppercase()}"
            ClueKind.HotCold ->
                CluehelperClueCrypticRow.getRow("dbrow.cluehelper_cryptic_beginner_reldo").clueText
            else -> step.text.orEmpty()
        }

    private fun setStep(player: Player, tier: TrailTierRow, step: Int): ClueStep {
        TrailState(player, tier).step = step
        VarPlayerIntMapSetter.set(player, "varp.cluehelper_infobox_clue", step)
        return ClueSteps.step(step)
    }

    private fun rollStep(tier: TrailTierRow, random: GameRandom, exclude: Int?): Int {
        val pool = pools.getOrPut(tier.difficulty) { rollableSteps(tier.difficulty) }
        return random.pick(pool.filter { it != exclude }.ifEmpty { pool })
    }

    /**
     * Skill challenges are handed out by an npc rather than rolled, and the beginner hot/cold intro
     * cryptic is only the text a hot/cold step reads with.
     */
    private fun rollableSteps(difficulty: Int): List<Int> {
        val hotColdIntro =
            CluehelperClueCrypticRow.getRow("dbrow.cluehelper_cryptic_beginner_reldo").rowId
        return ClueSteps.all(difficulty)
            .filter { it.kind != ClueKind.SkillChallenge && it.rowId != hotColdIntro }
            .map { it.rowId }
    }

    private const val NO_INFOBOX = -1
}
