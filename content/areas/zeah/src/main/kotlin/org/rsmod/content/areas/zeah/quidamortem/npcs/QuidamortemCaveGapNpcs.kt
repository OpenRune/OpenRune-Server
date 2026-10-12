package org.rsmod.content.areas.zeah.quidamortem.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:tale_of_the_righteous] the cave dialogue while the quest is in progress;
//  the after-quest dialogue is used since the cave is found in the quest
class QuidamortemCaveGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.shayzienquest_duffy") { startDialogue(it.npc) { historianDuffy() } }
        onOpNpc1("npc.shayzienquest_gnosi") { startDialogue(it.npc) { gnosi() } }
        onOpNpc1("npc.shayzienquest_historian") { startDialogue(it.npc) { naturalHistorian() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Historian_Duffy
    private suspend fun Dialogue.historianDuffy() {
        chatPlayer(quiz, "How are things going?")
        chatNpc(
            happy,
            "Good thanks. Now that we've found this cave, we're learning more about the " +
                "lizardmen than ever before.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gnosi
    private suspend fun Dialogue.gnosi() {
        chatPlayer(happy, "Hello. How's it going?")
        chatNpc(
            happy,
            "The discovery of this cave could change everything. We might finally be able to " +
                "discover the link between the lizardmen, the Chambers of Xeric and Xeric himself.",
        )
        while (true) {
            val option =
                choice3(
                    "Who is Xeric?",
                    1,
                    "What are the Chambers of Xeric?",
                    2,
                    "Goodbye.",
                    3,
                )
            when (option) {
                1 -> whoIsXeric()
                2 -> {
                    chambers()
                    if (!chambersMenu()) return
                }
                else -> {
                    chatPlayer(neutral, "Goodbye.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.whoIsXeric() {
        chatPlayer(quiz, "Who is Xeric?")
        chatNpc(
            neutral,
            "Xeric was an incredibly powerful dark mage. He ruled Great Kourend for ten decades.",
        )
        chatNpc(
            neutral,
            "If you'd like to find out more you should visit the Historical Archive at the Grand " +
                "Library.",
        )
    }

    private suspend fun Dialogue.chambers() {
        chatPlayer(quiz, "What are the Chambers of Xeric?")
        chatNpc(
            neutral,
            "At the summit of this mountain we've found an ancient temple and a vast network of " +
                "caves that we believe were formed by Xeric and his followers.",
        )
        chatNpc(
            neutral,
            "The discovery of the Chambers tells us Xeric invested significant time and " +
                "resources here, yet it was never documented.",
        )
        chatNpc(
            neutral,
            "While much of Kourend's history under Xeric remains unknown, it is very surprising " +
                "to discover such a huge area that was previously undiscovered to us.",
        )
    }

    private suspend fun Dialogue.chambersMenu(): Boolean {
        while (true) {
            val option =
                choice4(
                    "Can I explore the Chambers?",
                    1,
                    "What do you think Xeric used this area for?",
                    2,
                    "Who is Xeric?",
                    3,
                    "Goodbye",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Can I explore the Chambers?")
                    chatNpc(
                        neutral,
                        "Of course, though be careful. The dangers of the Chambers of Xeric are " +
                            "as yet unknown. You should take a strong group of companions with " +
                            "you.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "What do you think Xeric used this area for?")
                    chatNpc(
                        neutral,
                        "When he ruled Kourend, we know Xeric performed experiments on the " +
                            "living, using power drawn from the Dark Altar. After he was " +
                            "overthrown, he would have needed a new source of power.",
                    )
                    chatNpc(
                        neutral,
                        "We believe this altar is that source. It seems to act as an amplifier " +
                            "for the Dark Altar.",
                    )
                    chatNpc(
                        neutral,
                        "As for what he used the Chambers for, that we still don't know. " +
                            "There's a lot of creatures in the Chambers. Maybe he used them in " +
                            "his experiments.",
                    )
                }
                3 -> whoIsXeric()
                else -> {
                    chatPlayer(neutral, "Goodbye.")
                    return false
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Natural_Historian_(Mount_Quidamortem)
    private suspend fun Dialogue.naturalHistorian() {
        chatPlayer(happy, "Hello.")
        when (access.random.of(5)) {
            0 -> chatNpc(worried, "I've lost my notes... Duffy, have you seen my notes?")
            1 -> chatNpc(worried, "My wife will kill me if I don't take her back a souvenir.")
            2 ->
                chatNpc(
                    sad,
                    "I've had such a terrible headache since we arrived on this mountain.",
                )
            3 ->
                chatNpc(
                    happy,
                    "Thank Saradomin for that mountain guide. We couldn't have made it here " +
                        "without him!",
                )
            else -> chatNpc(worried, "I can't find my rock pick!")
        }
    }
}
