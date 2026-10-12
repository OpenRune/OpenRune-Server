package org.rsmod.content.areas.city.canifis.mortton

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Afflicted
// https://oldschool.runescape.wiki/w/Transcript:Razmire_Keelgan
// https://oldschool.runescape.wiki/w/Transcript:Ulsquire_Shauncy
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:shades_of_mortton] serums 207 and 208 on the afflicted and Razmire's
//  stores
class MorttonAfflicted : PluginScript() {
    override fun ScriptContext.startup() {
        for (afflicted in AFFLICTED) {
            onOpNpc1(afflicted) { startDialogue(it.npc) { afflicted() } }
        }
        onOpNpc1(ULSQUIRE_AFFLICTED) { startDialogue(it.npc) { ulsquire() } }
    }

    // TODO(content): [verify:in_game] only the afflicted Ulsquire is spawned; after the quest he
    //  should be the cured `npc.ulsquire_shauncy`
    private suspend fun Dialogue.ulsquire() {
        if (!QuestRequirements.hasCompleted(player, SHADES_OF_MORTTON)) {
            return afflicted()
        }
        chatNpc(happy, "Ah, hello again, what can I do for you now?")
        chatPlayer(happy, "Hello there.")
        chatNpc(
            happy,
            "Hello again! Many thanks for helping us to solve the  riddle with the Shades, we " +
                "can start to work now  putting all those Shade spirits to rest and get  a " +
                "little bit of a reward to go along with it!",
        )
    }

    // TODO(content): [verify:in_game] whether the blue line is a second chat page or replaces it
    private suspend fun Dialogue.afflicted() {
        val words = List(access.random.of(1, 6)) { WORDS[access.random.of(WORDS.size)] }
        chatNpc(confused, words.joinToString(" "))
        chatNpc(confused, "<col=0000ff>~ This person doesn't make any sense at all. ~</col>")
    }

    private companion object {
        val AFFLICTED =
            listOf(
                "npc.mort_afflicted_man",
                "npc.mort_afflicted_man2",
                "npc.mort_afflicted_woman",
                "npc.mort_afflicted_woman2",
                "npc.razmire_keelgan_afflicted",
            )
        const val ULSQUIRE_AFFLICTED = "npc.ulsquire_shauncy_afflicted"
        const val SHADES_OF_MORTTON = "quest_shadesofmortton"
        val WORDS =
            listOf(
                "ughugh",
                "knows'is",
                "nots",
                "pirsl",
                "wot's",
                "zurgle",
                "gurghl",
                "mee's",
                "seysyi",
                "sfriess",
                "says",
            )
    }
}
