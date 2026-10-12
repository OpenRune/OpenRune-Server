package org.rsmod.content.areas.misc.ver_sinhaza

import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:darkness_of_hallowvale] "Could you send me to the mines?" and the teleport
// to the Meiyerditch mine after the quest
// TODO(content): [quest:sins_of_the_father] the disguised "Need something?" escort after the quest
// https://oldschool.runescape.wiki/w/Transcript:Vyrewatch (Next to the bank in Ver Sinhaza)
class VerSinhazaBankGuard : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.myq4_bank_guard") {
            startDialogue(it.npc) { chatNpc(angry, "Clear off food sack.") }
        }
    }
}
