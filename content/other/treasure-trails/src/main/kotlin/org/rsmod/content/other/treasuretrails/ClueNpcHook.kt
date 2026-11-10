package org.rsmod.content.other.treasuretrails

import jakarta.inject.Inject
import org.rsmod.api.enums.TrailEnums
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.music.MusicRepository
import org.rsmod.api.player.hook.PlayerNpcOpHook
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.worn.EquipmentChecks
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.table.TrailCharlieTaskRow
import org.rsmod.api.table.TrailTierRow
import org.rsmod.api.table.Tuple2
import org.rsmod.api.table.cluehelper.CluehelperClueCrypticRow
import org.rsmod.api.table.cluehelper.CluehelperTargetNpcRow
import org.rsmod.content.other.treasuretrails.TrailFlow.advance
import org.rsmod.content.other.treasuretrails.TrailFlow.assign
import org.rsmod.content.other.treasuretrails.TrailFlow.matches
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp

class ClueNpcHook
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val music: MusicRepository,
    private val bonuses: WornBonuses,
) :
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
        if (trail.part != null) return handOverPart(npc, trail)
        when (step.kind) {
            ClueKind.HotCold -> giveDevice(npc, tier)
            ClueKind.SkillChallenge -> handInTask(npc, tier, step)
            ClueKind.FaloBard -> faloSong(npc, tier, step)
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
                    angersAbbot(step) -> completeTalk(npc, tier, epilogue = ABBOT_EPILOGUE)
                    else -> completeTalk(npc, tier)
                }
            }
        }
    }

    private fun talkTrail(player: Player, npc: Npc): HeldTrail? =
        TrailFlow.heldTrails(player).firstOrNull { trail ->
            val step = trail.step
            if (step.kind == ClueKind.HotCold) {
                val master = trail.tier.difficulty == TrailTiers.MASTER
                return@firstOrNull deviceGiver(trail.tier).matches(npc) &&
                    (master || !player.ownsDevice(TrailTiers.deviceObj(trail.tier.difficulty)))
            }
            val target = ClueSteps.target(player, step) as? ClueTarget.TalkTo
            target != null &&
                target.matches(npc) &&
                (step.kind != ClueKind.Music || playing(player, step)) &&
                outfitMet(player, trail) &&
                (gates[step.targets.firstOrNull()]?.invoke(player) ?: true)
        }

    private val gates: Map<Int, (Player) -> Boolean> by lazy {
        mapOf(
            targetRow("abbot_langley_prayer") to { p -> bonuses.prayerBonus(p) < 0 },
            targetRow("wizard_cromperty") to { p -> baseMagicBonus(p) >= MIN_MAGIC_ATTACK },
            targetRow("ghommal") to { p -> bonuses.strengthBonus(p) >= MIN_MELEE_STRENGTH },
            targetRow("robin") to { p -> bonuses.offensiveRangedBonus(p) >= MIN_RANGED_ATTACK },
            targetRow("monk_of_entrana") to { p -> MasterParts.isWornAny(p) },
        )
    }

    private fun outfitMet(player: Player, trail: HeldTrail): Boolean =
        if (trail.part != null) {
            MasterParts.meets(player, trail.step)
        } else {
            ClueOutfits.wearing(player, trail.step)
        }

    private fun targetRow(name: String): Int =
        CluehelperTargetNpcRow.getRow("dbrow.cluehelper_target_npc_$name").rowId

    private fun baseMagicBonus(player: Player): Int {
        val bonus = bonuses.offensiveMagicBonus(player)
        return if (EquipmentChecks.isTumekensShadow(player.righthand)) bonus / SHADOW_MULTIPLIER else bonus
    }

    private fun angersAbbot(step: ClueStep): Boolean =
        step.targets.firstOrNull() == targetRow("abbot_langley_prayer")

    private fun playing(player: Player, step: ClueStep): Boolean {
        val track = step.music?.let(music::forRow) ?: return false
        return player.vars[CURRENT_TRACK] == track.id
    }

    private suspend fun ProtectedAccess.completeTalk(
        npc: Npc,
        tier: TrailTierRow,
        praise: String? = null,
        epilogue: String? = null,
    ) {
        val given = advance(tier) ?: return
        val name = npcVisType(npc).name
        val casket = given.id == tier.casketObj.id
        val master = tier.difficulty == TrailTiers.MASTER
        val text =
            when {
                casket && master -> MasterParts.CASKET
                casket -> "$name has given you a casket!"
                master -> "You've been given a new clue!"
                else -> "$name has given you another clue scroll!"
            }
        startDialogue(npc) {
            praise?.let { chatNpc(neutral, it) }
            objbox(given.symbol(), text)
            epilogue?.let { chatNpc(neutral, it) }
        }
    }

    private suspend fun ProtectedAccess.handOverPart(npc: Npc, trail: HeldTrail) {
        val index = trail.part ?: return
        if (!MasterParts.open(player, index)) return
        if (!with(MasterParts) { give(index) }) {
            startDialogue(npc) { chatNpc(neutral, MasterParts.NO_SPACE) }
            return
        }
        val angered = angersAbbot(trail.step)
        startDialogue(npc) {
            chatNpc(neutral, "Here you go.")
            objbox(MasterParts.partObjs[index], MasterParts.HANDED)
            if (angered) chatNpc(neutral, ABBOT_EPILOGUE)
        }
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
        val held = ClueBoxes.held(player)
        if (held == null) {
            val box = ClueBoxes.handOut(trail.step, random)
            ClueBoxes.reset(player)
            if (invAdd(inv, box.symbol(), 1).failure) {
                mes("You don't have enough inventory space.")
                return
            }
            val name = npcVisType(npc).name
            val master = trail.tier.difficulty == TrailTiers.MASTER
            if (master) ClueBoxes.masterCode(box)?.let { MasterVars.setBox(player, it) }
            val light = box.symbol() == ClueLightBox.BOX
            val text =
                when {
                    light -> "$name hands you a light puzzle box."
                    master -> "$name hands you a puzzlebox!"
                    else -> "$name has given you a puzzle box!"
                }
            startDialogue(npc) {
                if (master) chatNpc(neutral, "Here, solve this.")
                objbox(box.symbol(), text)
            }
            return
        }
        if (!ClueBoxes.solved(player, held)) {
            startDialogue(npc) { chatNpc(neutral, "You haven't solved the puzzle yet.") }
            return
        }
        if (TrailFlow.currentStep(player, trail.tier)?.rowId != trail.step.rowId) return
        if (invDel(inv, held.symbol(), 1).failure) return
        ClueBoxes.reset(player)
        val master = trail.tier.difficulty == TrailTiers.MASTER
        if (master) MasterVars.setReady(player, true)
        val praise = "Well done ${player.displayName}. Here you go.".takeIf { master }
        completeTalk(npc, trail.tier, praise)
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

    private suspend fun ProtectedAccess.faloSong(npc: Npc, tier: TrailTierRow, step: ClueStep) {
        startDialogue(npc) { chatNpc(neutral, step.text.orEmpty()) }
        if (!ClueSteps.holdsRequiredItem(player, step)) return
        if (TrailFlow.currentStep(player, tier)?.rowId != step.rowId) return
        completeTalk(npc, tier)
    }

    private suspend fun ProtectedAccess.giveDevice(npc: Npc, tier: TrailTierRow) {
        val device = TrailTiers.deviceObj(tier.difficulty)
        if (tier.difficulty == TrailTiers.MASTER) {
            return with(JorralDialogue) { masterDevice(npc) }
        }
        if (invAdd(inv, device, 1).failure) {
            mes("You don't have enough inventory space.")
            return
        }
        startDialogue(npc) { objbox(device, "${npcVisType(npc).name} gives you a strange device.") }
    }

    private suspend fun ProtectedAccess.talkToUri(npc: Npc, tier: TrailTierRow) {
        val step = TrailFlow.currentStep(player, tier)
        val summoned = ClueUri.isSummoned(player, npc)
        val ready =
            summoned &&
                step?.kind == ClueKind.Emote &&
                TrailFlow.holdsClue(player, tier) &&
                ClueOutfits.wearing(player, step)
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

    private fun deviceGiver(tier: TrailTierRow): ClueTarget.TalkTo {
        val row =
            if (tier.difficulty == TrailTiers.MASTER) {
                "dbrow.cluehelper_cryptic_master_jorral"
            } else {
                "dbrow.cluehelper_cryptic_beginner_reldo"
            }
        val intro = CluehelperClueCrypticRow.getRow(row)
        return ClueTarget.of(intro.target.first().id) as ClueTarget.TalkTo
    }

    private fun Player.ownsDevice(device: String): Boolean =
        inv.count(device) > 0 || (invMap["inv.bank"]?.count(device) ?: 0) > 0

    private fun Npc.uriTier(): TrailTierRow? =
        TrailTierRow.all().firstOrNull { it.uriNpc.id == type.id }

    private companion object {
        const val ABBOT_EPILOGUE = "I must say, I despise the equipment you have chosen to wear."
        const val MIN_MAGIC_ATTACK = 100
        const val MIN_MELEE_STRENGTH = 100
        const val MIN_RANGED_ATTACK = 180
        const val SHADOW_MULTIPLIER = 3
        const val WORN_TASK_STEP = "dbrow.cluehelper_skillchallenge_elite_0"
        const val CURRENT_TRACK = "varbit.music_curr_id"
        const val URI_LEAVE_DELAY = 2
    }
}
