package org.rsmod.content.areas.misc.islands.fossilisland.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Fossil_Collector
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:fossil_exchange] "I'd like to trade some fossils." and the Trade option:
// 100, 200, 300 and 500 numulites per small, medium or plant, large and rare fossil
// TODO(content): [system:fossil_notebook] "I've lost my notebook." and the first notebook
class FossilCollector : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.fossil_shopkeeper") { startDialogue(it.npc) { collector() } }
    }

    private suspend fun Dialogue.collector() {
        chatPlayer(happy, "Hello!")
        chatNpc(happy, "Hello there. Do you have any fossils for me?")
        while (true) {
            val option =
                choice3(
                    "Who are you?",
                    1,
                    "Why are you here?",
                    2,
                    "Goodbye.",
                    3,
                )
            when (option) {
                1 ->
                    chatNpc(
                        happy,
                        "I'm Mary. In the tent you'll find Tray, my dog. He's quite friendly and " +
                            "doesn't bite. I'm here for the fossils. I'll go anywhere for fossils!",
                    )
                2 ->
                    chatNpc(
                        happy,
                        "Well, I have some rather nice things I've found. I collect fossils, " +
                            "infact you might say I'm obsessed with them. Large or small, but " +
                            "especially the small numulites.",
                    )
                3 -> {
                    chatPlayer(neutral, "Goodbye.")
                    return
                }
            }
        }
    }
}
