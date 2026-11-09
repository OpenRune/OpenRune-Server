package org.rsmod.content.areas.city.taverley.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Miazrqa, the princess in the Burthorpe tower keeping Rupert out of sight. */
class Miazrqa : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.grim_miazrqa") { startDialogue(it.npc) { greeting() } }
    }

    private suspend fun Dialogue.greeting() {
        if (QuestRequirements.hasCompleted(player, GRIM_TALES)) {
            chatNpc(happy, "Thanks again for finding my pendant. I felt so lost without it.")
            chatPlayer(neutral, "You're welcome. Goodbye.")
            return
        }
        chatPlayer(happy, "Hello!")
        chatNpc(
            neutral,
            "Oh, hello there. I'm afraid I don't have time to talk right now. I'm rather busy. " +
                "Come back some other time please.",
        )
        chatPlayer(neutral, "Ok, sorry.")
    }

    private companion object {
        private const val GRIM_TALES = "quest_grimtales"
    }
}
