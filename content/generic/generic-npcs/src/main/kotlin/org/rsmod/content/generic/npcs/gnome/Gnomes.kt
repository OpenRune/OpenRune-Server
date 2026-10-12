package org.rsmod.content.generic.npcs.gnome

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class Gnomes : PluginScript() {
    override fun ScriptContext.startup() {
        for (gnome in GNOMES) {
            onOpNpc1(gnome) { startDialogue(it.npc) { gnome() } }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gnome
    private suspend fun Dialogue.gnome() {
        chatPlayer(happy, "Hello.")
        when (access.random.of(5)) {
            0 -> {
                chatNpc(happy, "Can't stop sorry! Busy, busy, busy!")
                mesbox("The gnome is too busy to talk.")
            }
            1 -> {
                chatNpc(happy, "Hello traveller. Are you enjoying your stay?")
                chatPlayer(happy, "It's a nice place.")
                chatNpc(happy, "Yes, we try to keep it that way.")
            }
            2 -> {
                chatPlayer(quiz, "Are you OK?")
                mesbox("The gnome seems a little drunk.")
                chatNpc(drunk, "Youssh go gesshh em.")
                chatPlayer(confused, "Pardon?")
                chatNpc(drunk, "Gessh ush a cosshtail.")
                chatPlayer(confused, "A what?")
                chatNpc(drunk, "Blurssh berry.")
                mesbox("I think he's had enough!")
            }
            3 -> {
                chatNpc(sad, "I don't think I can take much more!")
                chatPlayer(quiz, "What's wrong?")
                chatNpc(sad, "It's just the wife, she won't stop moaning!")
                chatPlayer(neutral, "Maybe you should give her less to moan about.")
                chatNpc(sad, "She'll always find something.")
            }
            else -> {
                chatNpc(happy, "How's life treating you?")
                chatPlayer(happy, "Not bad, not bad at all.")
                chatNpc(happy, "It's good to see a human with a positive attitude.")
            }
        }
    }

    private companion object {
        val GNOMES = listOf("npc.gnome", "npc.browclothedgnome", "npc.darkskinned_gnome")
    }
}
