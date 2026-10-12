package org.rsmod.content.areas.misc.morytania.slepe

import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:vyre_disguise] "Watch yourself out here. These humans are nasty pieces
// of work." for players wearing vyre noble clothing
// https://oldschool.runescape.wiki/w/Transcript:Vyrewatch (In Slepe)
class SlepeVyrewatch : PluginScript() {
    override fun ScriptContext.startup() {
        for (vyrewatch in VYREWATCH) {
            onOpNpc1(vyrewatch) {
                startDialogue(it.npc) { chatNpc(angry, LINES[access.random.of(LINES.size)]) }
            }
        }
    }

    private companion object {
        val VYREWATCH =
            (1..4).map { "npc.slepe_vyrewatch_female_$it" } +
                (1..4).map { "npc.slepe_vyrewatch_male_$it" }

        val LINES =
            listOf(
                "Get lost, food sack!",
                "Get out of my face, meat sack, I don't 'play' with my food.",
                "Get about your business, meat sack, before I perforate you real good!",
            )
    }
}
