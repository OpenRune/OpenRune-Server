package org.rsmod.content.areas.city.canifis.mortton

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Afflicted
// https://oldschool.runescape.wiki/w/Transcript:Razmire_Keelgan
// https://oldschool.runescape.wiki/w/Transcript:Ulsquire_Shauncy
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:shades_of_mortton] serums 207 and 208 on the afflicted, Razmire's stores
//  and Ulsquire's after-quest dialogue
class MorttonAfflicted : PluginScript() {
    override fun ScriptContext.startup() {
        for (afflicted in AFFLICTED) {
            onOpNpc1(afflicted) { startDialogue(it.npc) { afflicted() } }
        }
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
                "npc.ulsquire_shauncy_afflicted",
            )
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
