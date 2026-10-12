package org.rsmod.content.areas.city.desert.banditcamp.npcs

import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [verify:in_game] the sitting bandits aren't on the wiki page; they are given the
// same lines as the standing ones
// https://oldschool.runescape.wiki/w/Transcript:Bandit_(Bandit_Camp)
class BanditCamp : PluginScript() {
    override fun ScriptContext.startup() {
        for (bandit in BANDITS) {
            onOpNpc1(bandit) {
                startDialogue(it.npc) {
                    if (QuestRequirements.hasCompleted(player, DESERT_TREASURE)) {
                        chatNpc(
                            happy,
                            "So you're the one who freed Azzanadra from his prison? Thank you!",
                        )
                    } else {
                        chatNpc(angry, "Get out of this village. You are not welcome here.")
                    }
                }
            }
        }
    }

    private companion object {
        const val DESERT_TREASURE = "quest_deserttreasure"

        val BANDITS =
            listOf(
                "npc.fourdiamonds_sword_bandit_1",
                "npc.fourdiamonds_sword_bandit_free",
                "npc.fourdiamonds_sword_bandit_siteast",
                "npc.fourdiamonds_sword_bandit_sitnorth",
                "npc.fourdiamonds_sword_bandit_sitsouth",
                "npc.fourdiamonds_sword_bandit_sitwest",
            )
    }
}
