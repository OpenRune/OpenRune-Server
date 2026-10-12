package org.rsmod.content.areas.misc.villages.tyrascamp.npcs

import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// https://oldschool.runescape.wiki/w/Transcript:Tyras_guard (In Tyras Camp)
class TyrasCamp : PluginScript() {
    override fun ScriptContext.startup() {
        for (guard in CAMP_GUARDS) {
            onOpNpc1(guard) {
                startDialogue(it.npc) {
                    chatPlayer(happy, "Hello.")
                    chatNpc(
                        neutral,
                        "Sorry, can't stop to talk. You should go to General Hining if you need " +
                            "something.",
                    )
                }
            }
        }
    }

    private companion object {
        val CAMP_GUARDS =
            listOf("npc.regicide_tyras_camp_guard", "npc.regicide_tyras_camp_tent_guard")
    }
}
