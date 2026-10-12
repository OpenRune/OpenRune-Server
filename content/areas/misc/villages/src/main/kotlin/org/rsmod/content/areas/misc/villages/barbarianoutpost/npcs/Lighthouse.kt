package org.rsmod.content.areas.misc.villages.barbarianoutpost.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Larrissa (after Horror from the Deep)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:horror_from_the_deep] book about lighthouses swap and "Do you have any
// more paint?"
class Lighthouse : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.horror_girlfriend_postquest") { startDialogue(it.npc) { larrissa() } }
    }

    private suspend fun Dialogue.larrissa() {
        chatNpc(happy, "Oh! Thank you so much for all of your help in rescuing my darling Jossik!")
        chatNpc(
            happy,
            "I do not know what would have happened to him had you not chanced along this path!",
        )
        chatPlayer(happy, "Don't worry about it, I was glad to help out.")
    }
}
