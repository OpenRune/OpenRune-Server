package org.rsmod.content.areas.misc.villages.observatory.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Homunculus
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:tower_of_life] the basement opens after the quest, so the after-quest
// dialogue is the default
// TODO(content): [system:creature_creation] "You haveee materials need" altar checks
class TowerOfLifeDungeon : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.tol_homonculus_nocage") { startDialogue(it.npc) { homunculus() } }
    }

    private suspend fun Dialogue.homunculus() {
        chatPlayer(quiz, "Hi there, you mentioned something about creating monsters...?")
        chatNpc(happy, "Good! I gain know from alchemists and builders. Me make beings.")
        chatPlayer(
            neutral,
            "Interesting. Tell me if I'm right. By the alchemists and builders creating you, you " +
                "have inherited their combined knowledge in much the same way that a child might " +
                "inherit the looks of their parents.",
        )
        chatNpc(happy, "Yes, you right!")
        chatPlayer(quiz, "So what do you need me to do?")
        chatNpc(
            neutral,
            "Inspect symbol of life altars around dungeon. You see item give. Use item on altar. " +
                "Activate altar to create, you fight.",
        )
        chatPlayer(happy, "Okay. Sounds like a challenge.")
    }
}
