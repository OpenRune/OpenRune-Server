package org.rsmod.content.areas.misc.tzhaar.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:TzHaar-Ket-Zuh
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:bank_space_shop] "I'd like to buy more bank slots." (shop lives in
// generic-npcs' Banker; the wiki has no transcript for this branch)
// TODO(content): [verify:in_game] greeting reads "JalYt-[caste]-[name]" on the wiki
class TzHaarKetZuh : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(KET_ZUH) { startDialogue(it.npc) { ketZuh() } }
        onOpNpc3(KET_ZUH) { tryOpenBank() }
        onOpNpc4(KET_ZUH) { openCollectionBox() }
    }

    private fun ProtectedAccess.openCollectionBox() {
        ifOpenMainModal("interface.ge_collect")
    }

    private suspend fun Dialogue.ketZuh() {
        chatNpc(quiz, "Can I help you ${player.tzhaarName()}?")
        val option =
            choice4(
                "I'd like to access my bank account please.",
                1,
                "What did you call me?",
                2,
                "I'd like to check my PIN settings.",
                3,
                "More options...",
                4,
            )
        when (option) {
            1 -> access.tryOpenBank()
            2 -> whatDidYouCallMe()
            3 -> {
                chatPlayer(neutral, "I'd like to check my PIN settings.")
                access.ifOpenMainModal("interface.bankpin_settings")
            }
            4 -> moreOptions()
        }
    }

    private suspend fun Dialogue.moreOptions() {
        val option =
            choice3(
                "I'd like to collect items.",
                1,
                "No, I'm fine thanks.",
                2,
                "Previous options...",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(neutral, "I'd like to collect items.")
                access.openCollectionBox()
            }
            2 -> chatPlayer(neutral, "No, I'm fine thanks.")
            3 -> ketZuh()
        }
    }

    private suspend fun Dialogue.whatDidYouCallMe() {
        val caste = TzHaarCaste.of(player)
        chatNpc(confused, "Are you not ${player.tzhaarName()}?")
        val option =
            choice3("What's a '${caste.title}'?", 1, "I guess so...", 2, "No I'm not!", 3)
        when (option) {
            1 -> {
                chatPlayer(confused, "What's a '${caste.title}'?")
                chatNpc(neutral, caste.description)
                chatPlayer(neutral, "Well yes I suppose I am...")
                chatNpc(happy, "Then you ${caste.title}!")
                val next = choice2("What are you then?", 1, "Thanks for explaining it.", 2)
                if (next == 1) {
                    chatPlayer(quiz, "What are you then?")
                    chatNpc(
                        neutral,
                        "Daft JalYt, I am TzHaar-Ket, one of the guardians of our city.",
                    )
                    chatNpc(
                        neutral,
                        "There are the wise TzHaar-Mej who guide us, the swift TzHaar-Xil who " +
                            "hunt for our food, and the skilled TzHaar-Hur who craft our homes " +
                            "and tools.",
                    )
                } else {
                    chatPlayer(happy, "Thanks for explaining it.")
                }
            }
            2 -> {
                chatPlayer(neutral, "I guess so...")
                chatNpc(neutral, "Well then, no problems.")
            }
            3 -> {
                chatPlayer(angry, "No I'm not!")
                chatNpc(laugh, "What ever you say, crazy JalYt!")
            }
        }
    }

    private companion object {
        const val KET_ZUH = "npc.tzhaar_banker1"
    }
}
