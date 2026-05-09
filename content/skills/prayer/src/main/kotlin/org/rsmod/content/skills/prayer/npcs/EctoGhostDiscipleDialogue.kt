package org.rsmod.content.skills.prayer.npcs

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.skills.prayer.ecto.ectoTokens
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class EctoGhostDiscipleDialogue : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.ahoy_disciple") { startDiscipleDialogue(it.npc) }
    }

    private suspend fun ProtectedAccess.startDiscipleDialogue(npc: Npc) {
        if (!canUnderstandGhostDisciple()) {
            startDialogue(npc) {
                chatNpc(neutral, "Woooo wooo wooooo woooo")
            }
            return
        }
        startDialogue(npc) { mainMenu() }
    }

    private fun ProtectedAccess.canUnderstandGhostDisciple(): Boolean =
        worn.any { ITEM_GHOSTSPEAK_AMULET.contains(it?.id ?: -1) }

    private suspend fun Dialogue.mainMenu() {
        when (choice5(
                "What is this strange fountain?",
                1,
                "Where do I get ectoplasm from?",
                2,
                "How do I grind bones?",
                3,
                "How do I receive Ectotokens?",
                4,
                "Can I have the tokens I have earned?",
                5
            )) {
            1 -> explainFountain()
            2 -> explainEctoplasm()
            3 -> explainGrinding()
            4 -> explainReceivingEctotokens()
            5 -> claimTokens()
        }
    }

    private suspend fun Dialogue.explainFountain() {
        chatPlayer(quiz, "What is this strange fountain?")
        chatNpc(
            neutral,
            "This is the Ectofuntus, the most marvellous creation of Necrovarus, our glorious leader.",
        )
        explainFountainMenu()
    }

    private suspend fun Dialogue.explainFountainMenu() {
        when (
            choice2(
                "What is the Ectofuntus for?",
                1,
                "Back.",
                2,
            )
        ) {
            1 -> {
                chatPlayer(quiz, "What is the Ectofuntus for?")
                chatNpc(
                    neutral,
                    "It provides the power to keep us ghosts from passing over into the next plane of existence.",
                )
                chatPlayer(quiz, "And how does it work?")
                chatNpc(
                    neutral,
                    "You have to pour a bucket of ectoplasm into the fountain, a pot of ground bones, and then worship at the Ectofuntus. A unit of unholy power will then be created.",
                )
                chatPlayer(quiz, "Can you do it yourself?")
                chatNpc(
                    neutral,
                    "No, we must rely upon the living, as the worship of the undead no longer holds any inherent power.",
                )
                chatPlayer(quiz, "Why would people waste their time helping you out?")
                chatNpc(
                    happy,
                    "For every unit of power produced we will give you five Ectotokens. These tokens can be used in Port Phasmatys to purchase various services, not least of which includes access through the main toll gates.",
                )
            }
        }
        mainMenu()
    }

    private suspend fun Dialogue.explainEctoplasm() {
        chatPlayer(quiz, "Where do I get ectoplasm from?")
        chatNpc(
            neutral,
            "Necrovarus sensed the power bubbling beneath our feet, and we delved long and deep beneath Port Phasmatys, until we found a pool of natural ectoplasm. You may find it by using the trapdoor over there.",
        )
        mainMenu()
    }

    private suspend fun Dialogue.explainGrinding() {
        chatPlayer(quiz, "How do I grind bones?")
        chatNpc(
            neutral,
            "There is a bone grinding machine upstairs. Put bones of any type into the machine's hopper, and then turn the handle to grind them. You will need a pot to empty the machine of ground up bones.",
        )
        mainMenu()
    }

    private suspend fun Dialogue.explainReceivingEctotokens() {
        chatPlayer(quiz, "How do I receive Ectotokens?")
        chatNpc(
            neutral,
            "We disciples keep track of how many units of power have been produced. Just talk to us once you have generated some and we will reimburse you with the correct amount of Ectotokens.",
        )
        chatPlayer(quiz, "How do I generate units of power?")
        chatNpc(
            neutral,
            "You have to pour a bucket of ectoplasm into the fountain and then worship at the Ectofuntus with a pot of ground bones. This will create a unit of unholy power.",
        )
        mainMenu()
    }

    private suspend fun Dialogue.claimTokens() {
        chatPlayer(quiz, "Can I have the tokens I have earned?")

        val earned = access.ectoTokens
        if (earned <= 0) {
            chatNpc(neutral, "You have not earned any Ectotokens yet, mortal.")
            return
        }

        val add = access.invAdd(access.inv, "obj.ectotoken", earned)
        if (add.failure) {
            chatNpc(
                neutral,
                "I have $earned ectotokens waiting for you mortal, but you do not have room in your inventory for them.",
            )
            return
        }

        access.ectoTokens = 0
        chatNpc(happy, "Certainly, mortal. Here's $earned ectotokens.")
        mainMenu()
    }

    private companion object {
        val ITEM_GHOSTSPEAK_AMULET: List<Int> = listOf(
            "obj.amulet_of_ghostspeak".asRSCM(RSCMType.OBJ),
            "obj.amulet_of_ghostspeak_enchanted".asRSCM(RSCMType.OBJ)
        )
    }
}
