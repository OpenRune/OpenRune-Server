package org.rsmod.content.areas.zeah.hosidius.npcs.shops

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Horace
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:shops] no server inv for Little Shop of Horace (Trade op)
class LittleShopOfHorace : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.hosidius_generalstore") { startDialogue(it.npc) { horace() } }
    }

    private suspend fun Dialogue.horace() {
        chatNpc(
            happy,
            "Hello, I'm Horace. Welcome to my shop. Would you like to buy or sell something?",
        )
        while (true) {
            val option =
                choice3("Who are you?", 1, "Okay, let's trade.", 2, "No, I'm fine.", 3)
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        happy,
                        "Hello my friend, my name is Horace! You're in the city of Hosidius. " +
                            "Most everyone round here tends the farms. Without us, Kourend would " +
                            "have no crops at all.",
                    )
                    chatNpc(happy, "So, do you need to buy or sell something today?")
                }
                2 -> {
                    chatPlayer(happy, "Okay, let's trade.")
                    // TODO(content): [system:shops] opens Little Shop of Horace
                    return
                }
                else -> {
                    chatPlayer(neutral, "No, I'm fine.")
                    return
                }
            }
        }
    }
}
