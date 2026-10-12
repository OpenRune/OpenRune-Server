package org.rsmod.content.areas.misc.morytania.meiyerditch

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:vyre_disguise] "Leave me alone. I'm busy." for players wearing vyre
// noble clothing
// https://oldschool.runescape.wiki/w/Transcript:Vampyre_Juvenile (Outside Ver Sinhaza)
// https://oldschool.runescape.wiki/w/Transcript:Vampyre_Juvinate
class MeiyerditchVampyres : PluginScript() {
    override fun ScriptContext.startup() {
        for (juvenile in JUVENILES) {
            onOpNpc1(juvenile) { startDialogue(it.npc) { threat() } }
        }
        for (juvinate in JUVINATES) {
            onOpNpc1(juvinate) {
                startDialogue(it.npc) {
                    if (QuestRequirements.hasCompleted(player, BLOOD_MOON_RISES)) {
                        chatNpc(neutral, "Leave me alone. I'm busy.")
                    } else {
                        threat()
                    }
                }
            }
        }
    }

    private suspend fun Dialogue.threat() {
        chatNpc(
            angry,
            "What's your problem, food sack? Better move on before I perforate you real good!",
        )
    }

    private companion object {
        val JUVENILES = listOf("npc.sang_myq3_male_juvenile", "npc.sang_myq3_female_juvenile")
        val JUVINATES = listOf("npc.sang_myq3_male_juvinate", "npc.sang_myq3_female_juvinate")
        const val BLOOD_MOON_RISES = "quest_bloodmoonrises"
    }
}
