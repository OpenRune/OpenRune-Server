package org.rsmod.content.other.treasuretrails

import jakarta.inject.Inject
import org.rsmod.api.enums.TrailEnums
import org.rsmod.api.player.hook.PlayerNpcOpHook
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.table.TrailCharlieTaskRow
import org.rsmod.api.table.TrailTierRow
import org.rsmod.api.table.cluehelper.CluehelperClueCrypticRow
import org.rsmod.content.other.treasuretrails.TrailFlow.advance
import org.rsmod.content.other.treasuretrails.TrailFlow.matches
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp

class ClueNpcHook @Inject constructor(private val npcRepo: NpcRepository) : PlayerNpcOpHook {
    override fun claims(player: Player, npc: Npc, op: InteractionOp): Boolean {
        if (op != InteractionOp.Op1) return false
        if (npc.uriTier() != null) return true
        val tier = TrailTiers.tier(TrailTiers.BEGINNER)
        if (!TrailFlow.holdsClue(player, tier)) return false
        val step = TrailFlow.currentStep(player, tier) ?: return false
        if (step.kind == ClueKind.HotCold) {
            return reldo().matches(npc) && !player.ownsDevice()
        }
        val target = ClueSteps.target(player, step) as? ClueTarget.TalkTo ?: return false
        return target.matches(npc)
    }

    override suspend fun ProtectedAccess.op(npc: Npc, op: InteractionOp) {
        npc.uriTier()?.let { return talkToUri(npc, it) }
        val tier = TrailTiers.tier(TrailTiers.BEGINNER)
        val step = TrailFlow.currentStep(player, tier) ?: return
        when (step.kind) {
            ClueKind.HotCold -> giveDevice(npc)
            ClueKind.SkillChallenge -> handInTask(npc, tier, step)
            else -> {
                val challenge = TrailFlow.skillChallengeFor(player, step, npc, random)
                if (challenge != null) {
                    TrailFlow.assign(player, tier, challenge)
                    startDialogue(npc) { mesbox(challenge.text.orEmpty()) }
                } else {
                    completeTalk(npc, tier)
                }
            }
        }
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

    private suspend fun ProtectedAccess.handInTask(npc: Npc, tier: TrailTierRow, step: ClueStep) {
        val task = TrailCharlieTaskRow.all().firstOrNull { it.step.rowId == step.rowId } ?: return
        val item = task.obj.symbol()
        if (inv.count(item) == 0) {
            startDialogue(npc) { mesbox(step.text.orEmpty()) }
            return
        }
        if (TrailFlow.clueSlot(player, tier) == null || invDel(inv, item, 1).failure) {
            return
        }
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
        val summoned = player.vars[SUMMONED_NPC] == npc.uid.packed
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
        VarPlayerIntMapSetter.set(player, SUMMONED_NPC, NO_NPC)
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
        const val DEVICE = "obj.beginner_device"
        const val SUMMONED_NPC = "varp.cluehelper_npc_id"
        const val NO_NPC = -1
        const val URI_LEAVE_DELAY = 2
    }
}
