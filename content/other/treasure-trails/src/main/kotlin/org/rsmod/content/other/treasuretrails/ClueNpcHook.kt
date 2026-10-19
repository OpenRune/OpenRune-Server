package org.rsmod.content.other.treasuretrails

import jakarta.inject.Inject
import org.rsmod.api.enums.TrailEnums
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.music.MusicRepository
import org.rsmod.api.player.hook.PlayerNpcOpHook
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.table.TrailCharlieTaskRow
import org.rsmod.api.table.TrailTierRow
import org.rsmod.api.table.Tuple2
import org.rsmod.api.table.cluehelper.CluehelperClueCrypticRow
import org.rsmod.content.other.treasuretrails.TrailFlow.advance
import org.rsmod.content.other.treasuretrails.TrailFlow.assign
import org.rsmod.content.other.treasuretrails.TrailFlow.matches
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp

class ClueNpcHook
@Inject
constructor(private val npcRepo: NpcRepository, private val music: MusicRepository) :
    PlayerNpcOpHook {
    override fun claims(player: Player, npc: Npc, op: InteractionOp): Boolean {
        if (op != InteractionOp.Op1) return false
        if (npc.uriTier() != null) return true
        return talkTrail(player, npc) != null
    }

    override suspend fun ProtectedAccess.op(npc: Npc, op: InteractionOp) {
        npc.uriTier()?.let { return talkToUri(npc, it) }
        val trail = talkTrail(player, npc) ?: return
        val tier = trail.tier
        val step = trail.step
        when (step.kind) {
            ClueKind.HotCold -> giveDevice(npc)
            ClueKind.SkillChallenge -> handInTask(npc, tier, step)
            else -> {
                val question = ClueSteps.question(step)
                val challenge = TrailFlow.skillChallengeFor(player, step, npc, random)
                when {
                    challenge != null -> {
                        if (!assign(tier, challenge)) return
                        startDialogue(npc) { mesbox(challenge.text.orEmpty()) }
                    }
                    question != null -> challengeScroll(npc, trail, question)
                    ClueSteps.isPuzzleBox(step) -> puzzleBox(npc, trail)
                    else -> completeTalk(npc, tier)
                }
            }
        }
    }

    private fun talkTrail(player: Player, npc: Npc): HeldTrail? =
        TrailFlow.heldTrails(player).firstOrNull { trail ->
            val step = trail.step
            if (step.kind == ClueKind.HotCold) {
                return@firstOrNull reldo().matches(npc) && !player.ownsDevice()
            }
            val target = ClueSteps.target(player, step) as? ClueTarget.TalkTo
            target != null && target.matches(npc) && (step.kind != ClueKind.Music || playing(player, step))
        }

    private fun playing(player: Player, step: ClueStep): Boolean {
        val track = step.music?.let(music::forRow) ?: return false
        return player.vars[CURRENT_TRACK] == track.id
    }

    private suspend fun ProtectedAccess.completeTalk(npc: Npc, tier: TrailTierRow) {
        val given = advance(tier) ?: return
        val name = npcVisType(npc).name
        val text =
            if (given.id == tier.casketObj.id) {
                "$name has given you a casket!"
            } else {
                "$name has given you another clue scroll!"
            }
        startDialogue(npc) { objbox(given.symbol(), text) }
    }

    /**
     * The first talk hands over the challenge scroll; talking again with it asks for the answer.
     * The scroll is only taken once the answer is right and the clue is still held.
     */
    private suspend fun ProtectedAccess.challengeScroll(
        npc: Npc,
        trail: HeldTrail,
        question: Tuple2<String, Int>,
    ) {
        val scroll = TrailTiers.challengeObj(trail.step.rowId)?.symbol()
        if (scroll != null && inv.count(scroll) == 0) {
            if (invAdd(inv, scroll, 1).failure) {
                mes("You don't have enough inventory space.")
                return
            }
            val name = npcVisType(npc).name
            startDialogue(npc) { objbox(scroll, "$name has given you a challenge scroll!") }
            return
        }
        val answer = countDialog(question.t0)
        if (answer != question.t1) {
            startDialogue(npc) { chatNpc(neutral, "That isn't right. Try again.") }
            return
        }
        if (TrailFlow.currentStep(player, trail.tier)?.rowId != trail.step.rowId) return
        if (scroll != null && invDel(inv, scroll, 1).failure) return
        completeTalk(npc, trail.tier)
    }

    private suspend fun ProtectedAccess.puzzleBox(npc: Npc, trail: HeldTrail) {
        val held = ClueSlidePuzzle.held(player)
        if (held == null) {
            val box = ClueSlidePuzzle.handOut(trail.step, random)
            ClueSlidePuzzle.reset(player)
            if (invAdd(inv, box.symbol(), 1).failure) {
                mes("You don't have enough inventory space.")
                return
            }
            val name = npcVisType(npc).name
            startDialogue(npc) { objbox(box.symbol(), "$name has given you a puzzle box!") }
            return
        }
        if (!ClueSlidePuzzle.solved(player)) {
            startDialogue(npc) { chatNpc(neutral, "You haven't solved the puzzle yet.") }
            return
        }
        if (TrailFlow.currentStep(player, trail.tier)?.rowId != trail.step.rowId) return
        if (invDel(inv, held.symbol(), 1).failure) return
        ClueSlidePuzzle.reset(player)
        completeTalk(npc, trail.tier)
    }

    private suspend fun ProtectedAccess.handInTask(npc: Npc, tier: TrailTierRow, step: ClueStep) {
        if (ClueChallengeTasks.isAction(step)) {
            if (!ClueChallengeTasks.isDone(player, step)) {
                startDialogue(npc) { mesbox(step.text.orEmpty()) }
                return
            }
            if (TrailFlow.clueSlot(player, tier) == null) return
            completeTalk(npc, tier)
            return
        }
        val task = TrailCharlieTaskRow.all().firstOrNull { it.step.rowId == step.rowId } ?: return
        val item = task.obj.symbol()
        val worn = RSCM.getReverseMapping(RSCMType.DBROW, step.rowId) == WORN_TASK_STEP
        val held = if (worn) player.worn.count(item) else inv.count(item)
        if (held == 0) {
            startDialogue(npc) { mesbox(step.text.orEmpty()) }
            return
        }
        if (TrailFlow.clueSlot(player, tier) == null) return
        if (!worn && invDel(inv, item, 1).failure) return
        completeTalk(npc, tier)
    }

    private suspend fun ProtectedAccess.giveDevice(npc: Npc) {
        if (invAdd(inv, DEVICE, 1).failure) {
            mes("You don't have enough inventory space.")
            return
        }
        startDialogue(npc) { objbox(DEVICE, "${npcVisType(npc).name} gives you a strange device.") }
    }

    private suspend fun ProtectedAccess.talkToUri(npc: Npc, tier: TrailTierRow) {
        val step = TrailFlow.currentStep(player, tier)
        val summoned = ClueUri.isSummoned(player, npc)
        val ready =
            summoned &&
                step?.kind == ClueKind.Emote &&
                TrailFlow.holdsClue(player, tier) &&
                ClueOutfits.wearing(player, step.outfit)
        if (!ready) {
            startDialogue(npc) {
                chatNpc(neutral, "I do not believe we have any business, Comrade.")
            }
            return
        }
        val given = advance(tier) ?: return
        ClueUri.clear(player)
        val phrase = random.pick(TrailEnums.trail_uri_phrases.mapNotNull { it.value })
        val casket = given.id == tier.casketObj.id
        val text = if (casket) "You've been given a casket!" else "You've been given a clue!"
        startDialogue(npc) {
            chatNpc(shifty, phrase)
            chatPlayer(confused, "What?")
            objbox(given.symbol(), text)
        }
        npc.anim("seq.emote_wave")
        delay(URI_LEAVE_DELAY)
        npcRepo.del(npc, Int.MAX_VALUE)
    }

    private fun reldo(): ClueTarget.TalkTo {
        val intro = CluehelperClueCrypticRow.getRow("dbrow.cluehelper_cryptic_beginner_reldo")
        return ClueTarget.of(intro.target.first().id) as ClueTarget.TalkTo
    }

    private fun Player.ownsDevice(): Boolean =
        inv.count(DEVICE) > 0 || (invMap["inv.bank"]?.count(DEVICE) ?: 0) > 0

    private fun Npc.uriTier(): TrailTierRow? =
        TrailTierRow.all().firstOrNull { it.uriNpc.id == type.id }

    private companion object {
        const val WORN_TASK_STEP = "dbrow.cluehelper_skillchallenge_elite_0"
        const val DEVICE = "obj.beginner_device"
        const val CURRENT_TRACK = "varbit.music_curr_id"
        const val URI_LEAVE_DELAY = 2
    }
}
