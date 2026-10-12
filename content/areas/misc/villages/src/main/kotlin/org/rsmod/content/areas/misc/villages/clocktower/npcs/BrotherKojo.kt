package org.rsmod.content.areas.misc.villages.clocktower.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Brother_Kojo (before Clock Tower)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:clock_tower] start offer, quest stages and after-quest dialogue
// TODO(content): [system:treasure_trails] the watch for a player with a chart and sextant, and the
//  hard clue challenge scroll
class BrotherKojo : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.brother_kojo") { startDialogue(it.npc) { kojo() } }
    }

    private suspend fun Dialogue.kojo() {
        chatPlayer(happy, "Hello monk.")
        chatNpc(
            neutral,
            "Hello adventurer. My name is Brother Kojo. Do you happen to know the time?",
        )
        chatPlayer(neutral, "No, sorry, I don't.")
        chatNpc(
            worried,
            "Exactly! This clock tower has recently broken down, and without it nobody can tell " +
                "the correct time. I must fix it before the town people become too angry!",
        )
        chatNpc(
            quiz,
            "I don't suppose you could assist me in the repairs? I'll pay you for your help.",
        )
    }
}
