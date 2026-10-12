package org.rsmod.content.areas.misc.villages.castlewars.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Postie_Pete (Castle Wars Lobby)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class PostiePete : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.postie_pete") { startDialogue(it.npc) { postiePete() } }
    }

    private suspend fun Dialogue.postiePete() {
        chatPlayer(quiz, "Hey Pete what you doing here?")
        chatNpc(
            happy,
            "Just doing a quick survey on famous people and their favourite colour. It's a " +
                "special feature for 'West Wyverns Women's Weekly'.",
        )
        chatPlayer(quiz, "Can I get a copy?")
        chatNpc(quiz, "Can you speak Wyvern?")
        chatPlayer(quiz, "No. Can you?")
        chatNpc(happy, "Well obviously.")
        chatPlayer(happy, "Go on then.")
        chatNpc(
            shocked,
            "Here! Are you mad? No, 'fraid I can't do that. Could cause all sorts of trouble.",
        )
        chatPlayer(confused, "You're a bit mad really aren't you?")
        chatNpc(
            happy,
            "So would you be if you could speak over 1000 languages. I bet it's bliss being " +
                "ignorant of this world's trouble! I'm off!",
        )
    }
}
