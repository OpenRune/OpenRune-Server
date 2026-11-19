package org.rsmod.content.areas.city.varrock.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.varrock.shieldofarrav.ShieldOfArravQuest
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): [quest:bone_voyage] news topic and the Fossil Island options
class CuratorHaigHalen @Inject constructor(private val shieldOfArrav: ShieldOfArravQuest) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.curator") { startDialogue(it.npc) { curator() } }
    }

    private suspend fun Dialogue.curator() {
        chatNpc(happy, "Welcome to the museum of Varrock.")
        if (with(shieldOfArrav) { curatorShieldTopic() }) {
            return
        }
        val treasure =
            choice2(
                "Do you know where I could find any treasure?",
                true,
                "How do I get a medallion?",
                false,
            )
        if (treasure) {
            chatPlayer(quiz, "Do you know where I could find any treasure?")
            chatNpc(happy, "Look around you! This museum is full of treasures!")
            chatPlayer(neutral, "No, I meant treasures for ME.")
            chatNpc(
                neutral,
                "Any treasures this museum knows about it goes to great lengths to acquire.",
            )
            return
        }
        chatPlayer(quiz, "How do I get a medallion?")
        chatNpc(
            neutral,
            "Well, firstly you'll need to do a bit of work around the museum, then maybe we can " +
                "talk.",
        )
    }
}
