package org.rsmod.content.skills.crafting.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.skills.crafting.interfaces.TannerPrices
import org.rsmod.content.skills.crafting.interfaces.openTanner
import org.rsmod.content.skills.crafting.interfaces.tannableHideObjs
import org.rsmod.content.skills.crafting.interfaces.tannedLeatherObjs
import org.rsmod.content.skills.crafting.util.hasCompletedQuest
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MaryScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(TANNER_MARY) { greet(it.npc) }
        onOpNpc3(TANNER_MARY) { tan(it.npc) }
        onOpNpcU(TANNER_MARY) { usedItemOnMary(it.npc, it.objType.internalName) }
    }

    private suspend fun ProtectedAccess.preQuestChat(npc: Npc) {
        startDialogue(npc) {
            chatPlayer(quiz, "Hello there. Is this your home?")
            chatNpc(neutral, "It is. What brings you here?")
            chatPlayer(neutral, "I'm just looking around.")
            chatNpc(
                neutral,
                "Well you won't find anything too exciting here I'm afraid. Just farming.",
            )
            chatPlayer(happy, "Well you have fun with it.")
        }
    }

    private suspend fun ProtectedAccess.greet(npc: Npc) {
        if (!tansForPlayer()) {
            preQuestChat(npc)
            return
        }
        val firstMeeting = !player.seenGettingAheadDialogue
        player.seenGettingAheadDialogue = true

        startDialogue(npc) {
            if (firstMeeting) {
                gettingAheadAftermath()
            } else {
                returningCustomer()
            }
        }
    }

    private suspend fun Dialogue.gettingAheadAftermath() {
        chatNpc(neutral, "Well I have to say that mounted head looks awful.")
        chatPlayer(neutral, "Sorry.")
        chatNpc(
            happy,
            "Not to worry. If it keeps Gordon happy, so be it. Anyway, thank you for keeping " +
                "our farm safe. With the beast dealt with, I've been able to get back to " +
                "tanning again.",
        )

        val tan =
            choice2(
                "Could you tan something for me?",
                true,
                "Happy to have helped. All the best.",
                false,
            )
        if (tan) {
            tanRequest()
        } else {
            chatPlayer(happy, "Happy to have helped. All the best.")
        }
    }

    private suspend fun Dialogue.returningCustomer() {
        chatNpc(happy, "Good to see you again! Anything I can do for you?")
        val tan = choice2("Could you tan something for me?", true, "I'm good.", false)
        if (tan) {
            tanRequest()
        } else {
            chatPlayer(neutral, "I'm good.")
        }
    }

    private suspend fun Dialogue.tanRequest() {
        chatPlayer(quiz, "Could you tan something for me?")
        chatNpc(happy, "Of course.")
        access.openTanner(TannerPrices.Table)
    }

    private suspend fun ProtectedAccess.tan(npc: Npc) {
        if (tansForPlayer()) {
            openTanner(TannerPrices.Table)
        } else {
            preQuestChat(npc)
        }
    }

    private suspend fun ProtectedAccess.usedItemOnMary(npc: Npc, obj: String) {
        when {
            !tansForPlayer() -> startDialogue(npc) { chatNpc(neutral, "Er... Thanks, but no thanks!") }
            obj in tannableHideObjs -> openTanner(TannerPrices.Table)
            obj in tannedLeatherObjs ->
                startDialogue(npc) { chatNpc(neutral, "Er... I have no use for that, I make the stuff!") }
            else -> startDialogue(npc) { chatNpc(neutral, "Er... Thanks, but no thanks!") }
        }
    }

    private fun ProtectedAccess.tansForPlayer(): Boolean =
        player.hasCompletedQuest(QUEST_GETTING_AHEAD)
}

private const val TANNER_MARY = "npc.ga_mary"

private const val QUEST_GETTING_AHEAD = "quest_gettingahead"

private const val VARBIT_MARY_DIALOGUE_SEEN = "varbit.ga_mary_dialogue"

private var Player.seenGettingAheadDialogue by boolVarBit(VARBIT_MARY_DIALOGUE_SEEN)
