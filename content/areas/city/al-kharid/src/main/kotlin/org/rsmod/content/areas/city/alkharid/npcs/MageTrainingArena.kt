package org.rsmod.content.areas.city.alkharid.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [verify:in_game] the wiki lists the four conversations without saying how one
// is picked; a random one is used
class MageTrainingArena : PluginScript() {
    override fun ScriptContext.startup() {
        for (warrior in CHARMED_WARRIORS) {
            onOpNpc1(warrior) { startDialogue(it.npc) { charmedWarrior() } }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Charmed_Warrior
    private suspend fun Dialogue.charmedWarrior() {
        when (access.random.of(4)) {
            0 -> {
                chatPlayer(quiz, "Hello?")
                chatNpc(angry, "Hey! You haven't paid your Magic Training Arena Membership money!")
                chatPlayer(laugh, "You're lying. I can see right through you!")
                chatNpc(neutral, "Oh, HA HA, very funny.")
            }
            1 -> {
                chatPlayer(quiz, "Hello?")
                chatNpc(angry, "Can't you see I'm busy?")
                chatPlayer(neutral, "Well, I can't really see YOU.")
            }
            2 -> {
                chatPlayer(quiz, "Is there anybody there?")
                chatNpc(neutral, "What do you think?")
            }
            else -> {
                chatPlayer(quiz, "Is there anybody there?")
                chatNpc(laugh, "Wooo wooo! Be afraid for I'm a scary ghost. Wooo!")
                chatPlayer(neutral, "Er, whatever.")
            }
        }
    }

    private companion object {
        val CHARMED_WARRIORS =
            listOf(
                "npc.magictraining_ghost_shoes",
                "npc.magictraining_ghost_shoes2",
                "npc.magictraining_ghost_shoes3",
                "npc.magictraining_ghost_shoes4",
            )
    }
}
