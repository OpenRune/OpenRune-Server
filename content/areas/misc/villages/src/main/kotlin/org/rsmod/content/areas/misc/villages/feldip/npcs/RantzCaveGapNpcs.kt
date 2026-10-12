package org.rsmod.content.areas.misc.villages.feldip.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class RantzCaveGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.bugs") { startDialogue(it.npc) { bugs() } }
        onOpNpc1("npc.fycie") { startDialogue(it.npc) { fycie() } }
    }

    private fun Dialogue.chompyDone(): Boolean =
        QuestRequirements.hasCompleted(player, "quest_bigchompybirdhunting")

    // https://oldschool.runescape.wiki/w/Transcript:Bugs
    private suspend fun Dialogue.bugs() {
        if (chompyDone()) {
            chatNpc(happy, "Thanks for da chompy, it was deloverly!")
            return
        }
        chatNpc(neutral, "You's better talk to Dad, him chasey sneaky da chompy.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fycie
    // TODO(content): [system:treasure_trails] medium clue step reward
    private suspend fun Dialogue.fycie() {
        if (chompyDone()) {
            chatNpc(happy, "Thanks for da chompy, it was deloverly!")
            return
        }
        chatNpc(neutral, "You's better talk to Dad, We not talk to wierdly 'umans.")
    }
}
