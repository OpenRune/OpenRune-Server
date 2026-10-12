package org.rsmod.content.areas.city.canifis.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class CanifisGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.waa_werewolf_guard") { startDialogue(it.npc) { trapdoorGuard() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Werewolf_(Werewolf_Agility_Course)
    private suspend fun Dialogue.trapdoorGuard() {
        chatPlayer(quiz, "What's beneath the trapdoor?")
        if (player.wearsRingOfCharos()) {
            chatNpc(
                happy,
                "It's an agility course designed for lycanthropes like ourselves, my friend.",
            )
            chatPlayer(quiz, "Can I come in and use it?")
            chatNpc(
                happy,
                "Certainly. The cavern contains two courses - on the west side is a Level 60 " +
                    "Agility Course, and and the east side is a Level 25 Skullball Course.",
            )
            return
        }
        chatNpc(angry, "That's none of your business, human, and I'll never tell.")
        chatPlayer(happy, "Oh, come on - I'm only curious.")
        chatNpc(
            angry,
            "If it wasn't my duty to stand here and guard our agility course from the likes of " +
                "you, I would be relieving you of your life right now.",
        )
        chatPlayer(quiz, "So it's an agility course, then?")
        chatNpc(shocked, "No ... yes ... oh blast - you didn't hear me say anything, right?")
        chatPlayer(happy, "No problem. Can I come in?")
        chatNpc(angry, "No, human - it's werewolves only.")
    }
}
