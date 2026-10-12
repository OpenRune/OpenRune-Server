package org.rsmod.content.areas.misc.zanaris.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class ZanarisCowGapNpc : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.fairy_cow") { startDialogue(it.npc) { zanarisCow() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Cow
    // TODO(content): [quest:a_fairy_tale_part_ii] using Nuff's certificate on the cow
    private suspend fun Dialogue.zanarisCow() {
        chatNpc(
            neutral,
            "Hmm, interesting, there appears to be a human trying to talk to me. Is it aware " +
                "that cows can't talk I wonder? Maybe if I just ignore it, it might go away.",
        )
        mesbox("The cow is studiously ignoring you.")
    }
}
