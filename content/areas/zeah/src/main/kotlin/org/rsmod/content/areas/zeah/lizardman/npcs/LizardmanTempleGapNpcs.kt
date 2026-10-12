package org.rsmod.content.areas.zeah.lizardman.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class LizardmanTempleGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.molch_temple_shaman_vis") { startDialogue(it.npc) { kahtBalam() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kaht_B'alam
    private suspend fun Dialogue.kahtBalam() {
        chatNpc(quiz, "Yesss?")
        val option =
            choice3("You can speak!?", 1, "Why are you down here?", 2, "Nevermind, bye.", 3)
        when (option) {
            1 -> {
                chatPlayer(shocked, "You can speak!?")
                chatNpc(
                    neutral,
                    "Of courssse, sssome of usss remember the high ssspeech out old massster " +
                        "ssspoke in.",
                )
                chatPlayer(
                    neutral,
                    "Hmm, I suppose I've never had a chance to speak with most Lizardmen, " +
                        "they're generally quite aggressive.",
                )
                chatNpc(
                    neutral,
                    "Yesss, we ssspeak the language of war far better. It isss the only way we " +
                        "know of.",
                )
                master()
            }
            2 -> {
                chatPlayer(quiz, "Why are you down here?")
                chatNpc(
                    neutral,
                    "Thisss isss our temple, we don't usssually get many visssitorsss... We make " +
                        "sssacrifice, offeringsss to our massster.",
                )
                chatPlayer(worried, "What kind of... sacrifices?")
                chatNpc(neutral, "Thisss and that. Whatever out massster desssiresss.")
                master()
            }
            3 -> {
                chatPlayer(neutral, "Nevermind, bye.")
                chatNpc(neutral, "Ye niauh, human.")
            }
        }
    }

    private suspend fun Dialogue.master() {
        chatPlayer(quiz, "This master you speak of...")
        chatNpc(neutral, "I know only what my eldersss passssed down. Very little.")
        chatPlayer(quiz, "How long ago since this master left you?")
        chatNpc(neutral, "Many generationsss have come and gone sssince he walked amongssst usss.")
        chatPlayer(quiz, "Do you remember his name?")
        chatNpc(
            neutral,
            "K'uhul ajaw isss the name we ssspeak. You manfolk ssspeak a different name.",
        )
        chatPlayer(confused, "Hmmm...")
    }
}
