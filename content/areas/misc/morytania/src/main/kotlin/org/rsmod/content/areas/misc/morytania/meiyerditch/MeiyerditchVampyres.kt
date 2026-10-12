package org.rsmod.content.areas.misc.morytania.meiyerditch

import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:vyre_disguise] "Leave me alone. I'm busy." for players wearing vyre
// noble clothing
// TODO(content): [quest:the_blood_moon_rises] the juvinates' "Leave me alone. I'm busy." after
// the quest
// https://oldschool.runescape.wiki/w/Transcript:Vampyre_Juvenile (Outside Ver Sinhaza)
// https://oldschool.runescape.wiki/w/Transcript:Vampyre_Juvinate
class MeiyerditchVampyres : PluginScript() {
    override fun ScriptContext.startup() {
        for (vampyre in VAMPYRES) {
            onOpNpc1(vampyre) {
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
        val VAMPYRES =
            listOf(
                "npc.sang_myq3_male_juvenile",
                "npc.sang_myq3_female_juvenile",
                "npc.sang_myq3_male_juvinate",
                "npc.sang_myq3_female_juvinate",
            )
    }
}
