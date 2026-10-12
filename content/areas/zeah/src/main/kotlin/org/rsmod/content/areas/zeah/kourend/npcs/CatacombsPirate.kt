package org.rsmod.content.areas.zeah.kourend.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class CatacombsPirate : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.cata_pirate") { startDialogue(it.npc) { pirate() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Pirate_(Catacombs_of_Kourend)
    private suspend fun Dialogue.pirate() {
        chatPlayer(happy, "Hello.")
        chatNpc(confused, "Arrr?")
        chatPlayer(quiz, "What are you doing down here?")
        chatNpc(angry, "You can't have me treasure!")
        chatPlayer(confused, "What treasure?")
        chatNpc(shifty, "Where's the treasure? Who told ye about me treasure?")
        chatPlayer(confused, "I don't know, you said something about treasure.")
        chatNpc(shifty, "Ain't no treasure down 'ere.")
        chatPlayer(quiz, "So why are you here?")
        chatNpc(shifty, "Takin' a walk, gettin' some air.")
        chatPlayer(neutral, "Riiiiight.")
    }
}
