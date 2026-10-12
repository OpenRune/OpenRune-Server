package org.rsmod.content.areas.zeah.kebos.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Kaal-Xil-Dar (before The Ascent of Arceuus)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_ascent_of_arceuus] quest dialogue and the after-quest topics
// TODO(content): [verify:in_game] that all three Tasakaal open the same conversation
class Tasakaal : PluginScript() {
    override fun ScriptContext.startup() {
        for (type in listOf(KET, MEJ, XIL)) {
            onOpNpc1(type) { startDialogue(it.npc) { tasakaal() } }
        }
    }

    private suspend fun Dialogue.tasakaal() {
        chatNpcSpecific("Kaal-Xil-Dar", XIL, neutral, "Human.")
        chatNpcSpecific("Kaal-Ket-Jor", KET, neutral, "Leave us.")
        chatNpcSpecific("Kaal-Mej-San", MEJ, neutral, "Your presence is not desired.")
        chatPlayer(shifty, "How friendly.")
    }

    private companion object {
        const val KET = "npc.tasakaal_ket"
        const val MEJ = "npc.tasakaal_mej"
        const val XIL = "npc.tasakaal_xil"
    }
}
