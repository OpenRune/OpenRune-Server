package org.rsmod.content.areas.misc.outskirts.darkwizardstower.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Zandar_Horfyre
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:fairytale_i_growing_pains] symptoms list dialogue
class ZandarHorfyre : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.zandar_horfyre") { startDialogue(it.npc) { zandar() } }
    }

    private suspend fun Dialogue.zandar() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(
            angry,
            "My name is Zandar Horfyre, and you ${player.displayName} are trespassing in my tower, " +
                "not to mention attacking my students! I thank you to leave immediately!",
        )
        when (choice2("Ok, I was going anyway.", 1, "No, I think I'll stay for a bit.", 2)) {
            1 -> {
                chatPlayer(neutral, "Ok, I was going anyway.")
                chatNpc(angry, "Good! And don't forget to close the door behind you!")
            }
            2 -> {
                chatPlayer(happy, "No, I think I'll stay for a bit.")
                chatNpc(
                    verymad,
                    "Actually, that wasn't an invitation. I've tried being polite, now we'll do " +
                        "it the hard way!",
                )
                // TODO(content): [verify:in_game] the swamp tile and any teleport animation
                access.telejump(LUMBRIDGE_SWAMP)
                chatPlayer(angry, "Zamorak curse that mage!")
                chatPlayer(laugh, "Actually, I guess he already has!")
            }
        }
    }

    private companion object {
        val LUMBRIDGE_SWAMP = CoordGrid(3169, 3178, 0)
    }
}
