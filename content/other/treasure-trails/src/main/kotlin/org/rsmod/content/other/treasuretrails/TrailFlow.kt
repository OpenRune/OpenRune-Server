package org.rsmod.content.other.treasuretrails

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import java.util.concurrent.ConcurrentHashMap
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.table.TrailCharlieTaskRow
import org.rsmod.api.table.TrailMapInterfaceRow
import org.rsmod.api.table.TrailTierRow
import org.rsmod.api.table.cluehelper.CluehelperClueCrypticRow
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

class HeldTrail(val tier: TrailTierRow, val slot: Int, val step: ClueStep)

/**
 * Trail state per tier. Beginner and master hold a single clue obj and keep the step in a varp;
 * easy-elite clue objs are each one step, so advancing swaps the obj in its slot.
 */
object TrailFlow {
    private val pools = ConcurrentHashMap<Int, List<Int>>()

    fun clueSlot(player: Player, tier: TrailTierRow): Int? {
        val clue = tier.clueObj
        return (0 until player.inv.size).firstOrNull { slot ->
            val obj = player.inv[slot] ?: return@firstOrNull false
            if (clue != null) {
                obj.id == clue.id
            } else {
                TrailTiers.clueDifficulty(TrailTiers.type(obj.id)) == tier.difficulty
            }
        }
    }

    fun holdsClue(player: Player, tier: TrailTierRow): Boolean = clueSlot(player, tier) != null

    fun currentStep(player: Player, tier: TrailTierRow): ClueStep? {
        if (tier.clueObj != null) {
            return TrailState(player, tier).step?.let(ClueSteps::step)
        }
        val obj = clueSlot(player, tier)?.let { player.inv[it] } ?: return null
        val challenge = player.vars[CHALLENGE_STEP]
        if (challenge != 0) {
            ClueSteps.step(challenge).takeIf { it.difficulty == tier.difficulty }?.let { return it }
        }
        return TrailTiers.objStep(TrailTiers.type(obj.id))?.let(ClueSteps::step)
    }

    fun heldTrails(player: Player): List<HeldTrail> =
        TrailTiers.playableTiers().mapNotNull { tier ->
            val slot = clueSlot(player, tier) ?: return@mapNotNull null
            val step = currentStep(player, tier) ?: return@mapNotNull null
            HeldTrail(tier, slot, step)
        }

    fun startedStep(player: Player, tier: TrailTierRow, random: GameRandom): ClueStep {
        val state = TrailState(player, tier)
        if (tier.clueObj == null) {
            start(state, random)
            val step = checkNotNull(currentStep(player, tier)) { "No ${tier.name} clue held." }
            VarPlayerIntMapSetter.set(player, INFOBOX, step.rowId)
            return step
        }
        state.step?.let { return ClueSteps.step(it) }
        start(state, random)
        return setStep(player, tier, rollStep(tier, random, exclude = null))
    }

    fun resetTrail(player: Player, tier: TrailTierRow) {
        TrailState(player, tier).clear()
        VarPlayerIntMapSetter.set(player, CHALLENGE_STEP, 0)
        ClueChallengeTasks.clear(player)
        VarPlayerIntMapSetter.set(player, INFOBOX, NO_INFOBOX)
        VarPlayerIntMapSetter.set(player, "varp.trail_hotcold_last_check_coord", 0)
        VarPlayerIntMapSetter.set(player, EMOTE_FIRST, 0)
        VarPlayerIntMapSetter.set(player, ENCOUNTER_DEFEATED, 0)
    }

    fun ProtectedAccess.read(tier: TrailTierRow) {
        val step = startedStep(player, tier, random)
        VarPlayerIntMapSetter.set(
            player,
            "varbit.cluehelper_hideyhole_filled",
            if (ClueStash.filled(player, step.hideyHole)) 1 else 0,
        )
        if (step.kind == ClueKind.Map) {
            val map = mapInterface(step)
            if (map == null) {
                mes("Nothing interesting happens.")
                return
            }
            ifOpenMainModal(map)
            return
        }
        ifOpenMainModal("interface.trail_cluetext")
        ifSetText("component.trail_cluetext:text", clueText(step))
    }

    suspend fun ProtectedAccess.openBox(tier: TrailTierRow) {
        if (TrailTiers.ownedClues(player, tier.difficulty) > 0) {
            mes(ClueTakeHook.ALREADY_HAVE_CLUE)
            return
        }
        val clue = freshClue(tier, random)
        if (invReplace(inv, tier.boxObj.symbol(), 1, clue.symbol()).failure) {
            mes("You don't have enough inventory space.")
            return
        }
        resetTrail(player, tier)
        startDialogue {
            objbox(clue.symbol(), "You open the clue box and find a clue scroll inside!")
        }
    }

    fun ProtectedAccess.openScrollCase(tier: TrailTierRow, case: ItemServerType, varbit: Int) {
        val unlock = TrailTiers.varbit(varbit)
        if (player.vars[unlock] != 0) {
            mes("You have already opened a scroll case like this.")
            return
        }
        if (invDel(inv, case.symbol(), 1).failure) return
        VarPlayerIntMapSetter.set(player, unlock, 1)
        val capacity = TrailTiers.capacity(player, tier)
        mes("You can now hold up to $capacity ${tier.name} clue scrolls and scroll boxes.")
    }

    fun ProtectedAccess.checkSteps(tier: TrailTierRow) {
        val done = TrailState(player, tier).progress
        mes("You have completed $done ${if (done == 1) "step" else "steps"} on this clue scroll.")
    }

    /**
     * Completes the current step. The final step swaps the clue for the tier's casket in one inv
     * transaction; earlier steps roll the next step, swapping the clue obj in its slot for the
     * obj-per-step tiers. Returns the obj the player now holds for the trail, or `null` when the
     * clue is gone and nothing changed.
     */
    fun ProtectedAccess.advance(
        tier: TrailTierRow,
        deliver: ProtectedAccess.(slot: Int, obj: ItemServerType) -> Boolean = { slot, obj ->
            invReplace(inv, slot, 1, obj).success
        },
    ): ItemServerType? {
        val slot = clueSlot(player, tier) ?: return null
        val current = currentStep(player, tier) ?: return null
        val state = TrailState(player, tier)
        start(state, random)
        val progress = state.progress + 1
        if (progress >= state.length) {
            if (!deliver(slot, tier.casketObj)) {
                return null
            }
            resetTrail(player, tier)
            return tier.casketObj
        }
        val clue = tier.clueObj
        if (clue != null) {
            state.progress = progress
            VarPlayerIntMapSetter.set(player, ENCOUNTER_DEFEATED, 0)
            setStep(player, tier, rollStep(tier, random, exclude = state.step))
            return clue
        }
        val next = rollClueObj(tier, random, exclude = current.rowId)
        if (!deliver(slot, next)) {
            return null
        }
        state.progress = progress
        VarPlayerIntMapSetter.set(player, CHALLENGE_STEP, 0)
        ClueChallengeTasks.clear(player)
        VarPlayerIntMapSetter.set(player, EMOTE_FIRST, 0)
        VarPlayerIntMapSetter.set(player, ENCOUNTER_DEFEATED, 0)
        VarPlayerIntMapSetter.set(player, INFOBOX, checkNotNull(TrailTiers.objStep(next)))
        return next
    }

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
                .filter { challenge ->
                    ClueChallengeTasks.isAction(challenge) ||
                        TrailCharlieTaskRow.all().any { it.step.rowId == challenge.rowId }
                }
        return random.pickOrNull(challenges)
    }

    fun ProtectedAccess.assign(tier: TrailTierRow, step: ClueStep): Boolean {
        if (tier.clueObj != null) {
            setStep(player, tier, step.rowId)
            return true
        }
        if (!holdsClue(player, tier)) return false
        VarPlayerIntMapSetter.set(player, CHALLENGE_STEP, step.rowId)
        VarPlayerIntMapSetter.set(player, INFOBOX, step.rowId)
        return true
    }

    fun ClueTarget.TalkTo.matches(npc: Npc): Boolean {
        val ids = setOfNotNull(row.npc.id, row.fallbackNpc?.id)
        return npc.type.id in ids || npc.visType.id in ids
    }

    fun freshClue(tier: TrailTierRow, random: GameRandom): ItemServerType =
        tier.clueObj ?: rollClueObj(tier, random, exclude = null)

    private fun start(state: TrailState, random: GameRandom) {
        if (state.length != 0) return
        state.length = random.of(state.tier.stepsMin, state.tier.stepsMax)
        state.progress = 0
    }

    private fun mapInterface(step: ClueStep): String? =
        TrailMapInterfaceRow.all()
            .firstOrNull { it.step.rowId == step.rowId }
            ?.let { RSCM.getReverseMapping(RSCMType.INTERFACE, it.`interface`) }

    private fun clueText(step: ClueStep): String =
        when (step.kind) {
            ClueKind.Anagram ->
                "The anagram reveals who to speak to next:<br>${step.text.orEmpty().uppercase()}"
            ClueKind.HotCold ->
                CluehelperClueCrypticRow.getRow("dbrow.cluehelper_cryptic_beginner_reldo").clueText
            ClueKind.Music -> "Cecilia would like to hear ${step.music?.displayname}."
            ClueKind.Coordinate -> sextantText(step.text.orEmpty())
            else -> step.text.orEmpty()
        }

    private fun sextantText(compact: String): String {
        val match = SEXTANT_COMPACT.matchEntire(compact) ?: return compact
        val (latDeg, latMin, ns, longDeg, longMin, ew) = match.destructured
        val lat = "$latDeg degrees $latMin minutes ${if (ns == "N") "north" else "south"}"
        val long = "$longDeg degrees $longMin minutes ${if (ew == "E") "east" else "west"}"
        return "$lat $long"
    }

    private fun setStep(player: Player, tier: TrailTierRow, step: Int): ClueStep {
        TrailState(player, tier).step = step
        VarPlayerIntMapSetter.set(player, INFOBOX, step)
        return ClueSteps.step(step)
    }

    private fun rollStep(tier: TrailTierRow, random: GameRandom, exclude: Int?): Int {
        val pool = pools.getOrPut(tier.difficulty) { rollableSteps(tier.difficulty) }
        return random.pick(pool.filter { it != exclude }.ifEmpty { pool })
    }

    private fun rollClueObj(tier: TrailTierRow, random: GameRandom, exclude: Int?): ItemServerType {
        val pool = pools.getOrPut(tier.difficulty) { rollableSteps(tier.difficulty) }.toSet()
        val objs =
            TrailTiers.clueObjs(tier.difficulty).filter { TrailTiers.objStep(it) in pool }
        return random.pick(objs.filter { TrailTiers.objStep(it) != exclude }.ifEmpty { objs })
    }

    /**
     * Skill challenges are handed out by an npc rather than rolled, and the beginner hot/cold intro
     * cryptic is only the text a hot/cold step reads with. Restricted (league/sailing) steps,
     * maps without a known interface and music steps are left out until supported.
     */
    private fun rollableSteps(difficulty: Int): List<Int> {
        val hotColdIntro =
            CluehelperClueCrypticRow.getRow("dbrow.cluehelper_cryptic_beginner_reldo").rowId
        return ClueSteps.all(difficulty)
            .asSequence()
            .filter { it.kind != ClueKind.SkillChallenge && it.rowId != hotColdIntro }
            .filterNot { it.restricted }
            .filter { it.kind != ClueKind.Music }
            .filter {
                it.challenge == null ||
                    ClueSteps.question(it) != null ||
                    ClueSteps.isPuzzleBox(it)
            }
            .filter { it.kind != ClueKind.Map || mapInterface(it) != null }
            .map { it.rowId }
            .toList()
    }

    internal const val EMOTE_FIRST = "varp.trail_emote_first"
    internal const val ENCOUNTER_DEFEATED = "varp.trail_encounter_defeated"
    private val SEXTANT_COMPACT = Regex("""(\d+)\.(\d+)([NS]) (\d+)\.(\d+)([EW])""")
    private const val CHALLENGE_STEP = "varp.trail_challenge_step"
    private const val INFOBOX ="varp.cluehelper_infobox_clue"
    private const val NO_INFOBOX = -1
}
