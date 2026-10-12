package org.rsmod.content.areas.misc.islands.harmony.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_great_brain_robbery] the wiki only has these lines; check them against
// the quest's progress
class HarmonyMonks : PluginScript() {
    override fun ScriptContext.startup() {
        for (monk in MONKS) {
            onOpNpc1(monk) { startDialogue(it.npc) { monk() } }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Monk_(Harmony_Island)
    private suspend fun Dialogue.monk() {
        when (access.random.of(4)) {
            0 -> {
                chatNpc(happy, "I knew you would save us! Bless you, my child!")
                chatPlayer(
                    neutral,
                    "Thanks! Just stay indoors while those zombies are still prowling around.",
                )
            }
            1 -> {
                chatNpc(happy, "May Saradomin's light shine upon you!")
                chatPlayer(
                    neutral,
                    "Tell him to save it for when I manage to defeat those zombies!",
                )
            }
            2 -> {
                chatNpc(happy, "Saradomin smile upon you! You've saved us all!")
                chatPlayer(happy, "All in a day's work, brother.")
            }
            else -> {
                chatNpc(happy, "Your name will forever be listed amongst the virtuous!")
                chatPlayer(laugh, "I'll remember that the next time I go drinking...")
            }
        }
    }

    private companion object {
        val MONKS = (1..4).map { "npc.brain_real_monk_$it" }
    }
}
