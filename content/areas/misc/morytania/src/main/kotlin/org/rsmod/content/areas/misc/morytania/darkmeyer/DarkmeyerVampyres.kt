package org.rsmod.content.areas.misc.morytania.darkmeyer

import org.rsmod.api.combat.commons.npc.queueCombatRetaliate
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:vyre_disguise] the juveniles' and juvinates' lines for players wearing
// vyre noble clothing
// TODO(content): [quest:the_blood_moon_rises] the juvinates' "Leave me alone. I'm busy." after
// the quest
// https://oldschool.runescape.wiki/w/Transcript:Vampyre_Juvenile (In Darkmeyer)
// https://oldschool.runescape.wiki/w/Transcript:Vampyre_Juvinate
class DarkmeyerVampyres : PluginScript() {
    override fun ScriptContext.startup() {
        for (juvenile in JUVENILES) {
            onOpNpc1(juvenile) {
                it.npc.say("You're not meant to be here human! Time to die!")
                it.npc.queueCombatRetaliate(player)
            }
        }
        for (juvinate in JUVINATES) {
            onOpNpc1(juvinate) {
                startDialogue(it.npc) {
                    chatNpc(
                        angry,
                        "What's your problem, food sack? Better move on before I perforate you " +
                            "real good!",
                    )
                }
            }
        }
    }

    private companion object {
        val JUVENILES = listOf("npc.darkm_juvenile_01", "npc.darkm_juvenile_02")
        val JUVINATES = listOf("npc.darkm_juvinate_01", "npc.darkm_juvinate_02")
    }
}
