package org.rsmod.content.areas.misc.morytania.meiyerditch

import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:darkness_of_hallowvale] "Send me to the mines." and the teleport to the
// Meiyerditch mine after the quest
// TODO(content): [system:vyrewatch_patrols] being noticed and taken to the mine or asked for a
// blood tithe
// https://oldschool.runescape.wiki/w/Transcript:Darkness_of_Hallowvale (Talking to a vyrewatch)
class MeiyerditchVyrewatch : PluginScript() {
    override fun ScriptContext.startup() {
        for (vyrewatch in VYREWATCH) {
            onOpNpc1(vyrewatch) {
                startDialogue(it.npc) {
                    chatPlayer(quiz, "Hello?")
                    chatNpc(angry, LINES[access.random.of(LINES.size)])
                }
            }
        }
    }

    private companion object {
        val VYREWATCH =
            listOf(
                "npc.sang_myq3_female_flying_ns_vyrewatch_1",
                "npc.sang_myq3_female_flying_ns_vyrewatch_2",
                "npc.sang_myq3_female_flying_ns_vyrewatch_3",
                "npc.sang_myq3_female_flying_ns_vyrewatch_4",
                "npc.sang_myq3_female_flying_vyrewatch_1",
                "npc.sang_myq3_female_flying_vyrewatch_2",
                "npc.sang_myq3_female_flying_vyrewatch_3",
                "npc.sang_myq3_male_flying_ns_vyrewatch_1",
                "npc.sang_myq3_male_flying_ns_vyrewatch_2",
                "npc.sang_myq3_male_flying_ns_vyrewatch_3",
                "npc.sang_myq3_male_flying_ns_vyrewatch_4",
                "npc.sang_myq3_male_flying_vyrewatch_1",
                "npc.sang_myq3_male_flying_vyrewatch_2",
                "npc.sang_myq3_male_flying_vyrewatch_3",
                "npc.sang_myq3_male_flying_vyrewatch_4",
            )

        val LINES =
            listOf(
                "Get lost, food sack!",
                "Get out of my face, meat sack, I don't 'play' with my food.",
                "Go about your business, meat sack, before I perforate you real good!",
            )
    }
}
