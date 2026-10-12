package org.rsmod.content.areas.zeah.lovakengj.npcs.shops

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.areas.zeah.kourend.kourendRumour
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:shops] no server invs for Little Munty's Little Shop, Toothy's Pickaxes
//  and The Deeper Lode (Trade ops)
class LovakengjUnstockedShops : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.lovakengj_generalstore") { startDialogue(it.npc) { munty() } }
        onOpNpc1("npc.lovakengj_sulphur_keeper") { startDialogue(it.npc) { toothy() } }
        onOpNpc1("npc.lovakengj_bartender") { startDialogue(it.npc) { fuggy() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Munty
    private suspend fun Dialogue.munty() {
        chatNpc(happy, "Can I help you, human?")
        while (true) {
            when (choice3("Who are you?", 1, "Let's trade.", 2, "No, I'm fine.", 3)) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        happy,
                        "Why, it's my little shop. I supply all kinds of items that the " +
                            "Lovakengj workers need. Though if you need pickaxes, you should " +
                            "visit Toothy in the sulphur mine.",
                    )
                    chatNpc(quiz, "Now, can I help you today?")
                }
                2 -> {
                    chatPlayer(happy, "Let's trade.")
                    return
                }
                else -> {
                    chatPlayer(neutral, "No, I'm fine.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Toothy
    private suspend fun Dialogue.toothy() {
        while (true) {
            chatNpc(neutral, "Aye? What do ye need?")
            val option =
                choice4(
                    "Tell me about this mine.",
                    1,
                    "What have you got to trade?",
                    2,
                    "Why are you called Toothy?",
                    3,
                    "I'll be off.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Tell me about this mine.")
                    chatNpc(
                        neutral,
                        "'s a sulphur mine. Ye get sulphur here. For making the dynamite, ye " +
                            "know? Folks use it in the blast mine. Watch the poison gas clouds, " +
                            "though - they'll choke ye.",
                    )
                    chatNpc(
                        neutral,
                        "Grab yerself a pickaxe if ye wants to mine some sulphur. Make sure ye " +
                            "keeps yer mouth an' nose covered too.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "What have you got to trade?")
                    chatNpc(neutral, "Pickaxes. For the mining. An' pots to make dynamite in.")
                    return
                }
                3 -> {
                    chatPlayer(quiz, "Why are you called Toothy?")
                    chatNpc(happy, "'Cos of my teeth.")
                }
                else -> {
                    chatPlayer(neutral, "I'll be off.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fuggy
    private suspend fun Dialogue.fuggy() {
        chatNpc(happy, "What can I do for you, traveller?")
        val option =
            choice4(
                "Let's see what you've got.",
                1,
                "I don't want to talk about my pleasure.",
                2,
                "Have you heard any rumours here?",
                3,
                "I'm alright, thank you.",
                4,
            )
        when (option) {
            1 -> chatPlayer(happy, "Let's see what you've got.")
            2 -> {
                chatPlayer(neutral, "I don't want to talk about my pleasure.")
                chatNpc(
                    happy,
                    "Ah, you sound like a human who's not been drinking yet. Would you like to " +
                        "start now?",
                )
                if (choice2("Let's see what you've got.", true, "I'm alright, thank you.", false)) {
                    chatPlayer(happy, "Let's see what you've got.")
                } else {
                    chatPlayer(neutral, "I'm alright, thank you.")
                }
            }
            3 -> {
                chatPlayer(quiz, "Have you heard any rumours here?")
                kourendRumour()
            }
            else -> chatPlayer(neutral, "I'm alright, thank you.")
        }
    }
}
