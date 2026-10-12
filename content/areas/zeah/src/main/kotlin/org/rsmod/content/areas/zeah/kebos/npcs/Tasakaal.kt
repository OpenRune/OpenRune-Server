package org.rsmod.content.areas.zeah.kebos.npcs

import dev.openrune.types.MesAnimType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Kaal-Xil-Dar
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_ascent_of_arceuus] quest dialogue
// TODO(content): [verify:in_game] whether the soul and "further use" topics are still offered
//  after A Kingdom Divided
// TODO(content): [verify:in_game] that all three Tasakaal open the same conversation
class Tasakaal : PluginScript() {
    override fun ScriptContext.startup() {
        for (type in listOf(KET, MEJ, XIL)) {
            onOpNpc1(type) { startDialogue(it.npc) { tasakaal() } }
        }
    }

    private suspend fun Dialogue.tasakaal() {
        when {
            QuestRequirements.hasCompleted(player, KINGDOM_DIVIDED) -> {
                ket(neutral, "The human returns.")
                afterKingdomDivided()
            }
            QuestRequirements.hasCompleted(player, ASCENT_OF_ARCEUUS) -> {
                mej(neutral, "The human returns.")
                afterAscent()
            }
            else -> {
                xil(neutral, "Human.")
                ket(neutral, "Leave us.")
                mej(neutral, "Your presence is not desired.")
                chatPlayer(shifty, "How friendly.")
            }
        }
    }

    private suspend fun Dialogue.afterAscent() {
        while (true) {
            val option =
                choice5(
                    "Tell me about yourselves.",
                    1,
                    "Why did that soul need freeing?",
                    2,
                    "You said you might have further use for me?",
                    3,
                    "Why do you hate Lord Arceuus?",
                    4,
                    "Goodbye.",
                    5,
                )
            when (option) {
                1 -> aboutYourselves()
                2 -> soul()
                3 -> furtherUse()
                4 -> lordArceuus()
                else -> {
                    goodbye()
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.afterKingdomDivided() {
        while (true) {
            val option =
                choice5(
                    "Tell me about yourselves.",
                    1,
                    "What do you know of Xeric?",
                    2,
                    "Do you know anything about this rogue Pyromancer?",
                    3,
                    "Why do you hate Lord Arceuus?",
                    4,
                    "Goodbye.",
                    5,
                )
            when (option) {
                1 -> aboutYourselves()
                2 -> xeric()
                3 -> pyromancer()
                4 -> lordArceuus()
                else -> {
                    goodbye()
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.aboutYourselves() {
        chatPlayer(quiz, "Tell me about yourselves.")
        ket(neutral, "We are the Tasakaal.")
        mej(neutral, "We serve the balance.")
        xil(neutral, "Between life.")
        mej(neutral, "And death.")
        chatPlayer(quiz, "Balance? So you serve Guthix?")
        xil(angry, "A pretender.")
        mej(neutral, "We serve another power.")
        ket(neutral, "A higher power.")
        chatPlayer(quiz, "What power?")
        mej(neutral, "One beyond your understanding.")
        chatPlayer(quiz, "Why do you serve this power?")
        xil(neutral, "The anima of this world, it is fed by life.")
        mej(neutral, "Without life, there is no anima.")
        ket(neutral, "But life can also destroy. It can endanger the anima.")
        xil(neutral, "So life must be kept in check.")
        mej(neutral, "This is our purpose.")
        chatPlayer(quiz, "And you were given this purpose?")
        ket(neutral, "Yes.")
        mej(neutral, "And no.")
        ket(neutral, "Our purpose was to wait. But we saw the world burn.")
        xil(neutral, "We could not stand by and watch.")
        mej(neutral, "So we left our people. Left our home.")
        xil(neutral, "And took on a new purpose.")
        chatPlayer(confused, "I'm still not sure I understand.")
        ket(neutral, "Maybe you never will.")
    }

    private suspend fun Dialogue.soul() {
        chatPlayer(quiz, "Why did that soul need freeing?")
        xil(neutral, "Upon death, a soul should leave this world.")
        mej(neutral, "And move to the one beyond.")
        ket(neutral, "This soul remained.")
        xil(neutral, "It endangered the balance.")
        chatPlayer(quiz, "Was it always there?")
        mej(neutral, "No.")
        ket(neutral, "The body was of one who died a long time ago.")
        xil(neutral, "But the soul was of one who died more recently.")
        ket(neutral, "It possessed this body.")
        mej(neutral, "We know not why.")
        chatPlayer(quiz, "Is this something that often happens?")
        xil(neutral, "No.")
        mej(worried, "It is a concerning occurrence.")
        ket(neutral, "We believe a mortal to be involved.")
        xil(angry, "Meddling.")
        mej(angry, "Where one should not meddle.")
        chatPlayer(neutral, "Interesting...")
    }

    private suspend fun Dialogue.furtherUse() {
        chatPlayer(quiz, "You said you might have further use for me?")
        xil(neutral, "We did.")
        chatPlayer(quiz, "What did you mean by that?")
        ket(neutral, "The task you performed.")
        xil(neutral, "It was a menial one.")
        mej(neutral, "Another could have filled your role.")
        ket(neutral, "But you showed promise.")
        mej(neutral, "So we tested you.")
        xil(neutral, "You passed.")
        chatPlayer(
            confused,
            "You said it was an important task. Now you say it's a menial one. Which is it?",
        )
        mej(neutral, "An important task can still be menial.")
        xil(neutral, "There is no contradiction here.")
        chatPlayer(neutral, "Hmmm... If you say so.")
        chatPlayer(
            quiz,
            "So why was it only a small task? Surely saving the Arceuus people was worth more " +
                "than that.",
        )
        ket(neutral, "You assume it was a trade.")
        mej(neutral, "You assume wrong.")
        chatPlayer(confused, "But you said I had to do it to save them?")
        xil(neutral, "Incorrect.")
        mej(neutral, "We had already committed to saving Arceuus.")
        ket(neutral, "But we needed time to determine the problem.")
        mej(neutral, "While we worked, you needed purpose.")
        xil(neutral, "So we gave you one.")
        chatPlayer(sad, "I feel a little bit used.")
        ket(neutral, "Then you should have paid more attention.")
        chatPlayer(quiz, "So what is the further use you have for me?")
        xil(neutral, "None.")
        ket(neutral, "For now.")
        mej(neutral, "But we will call on you again.")
        xil(neutral, "That, we can guarantee.")
    }

    private suspend fun Dialogue.lordArceuus() {
        chatPlayer(quiz, "Why do you hate Lord Arceuus?")
        ket(neutral, "We granted them knowledge.")
        xil(neutral, "For a specific purpose.")
        mej(angry, "But they misused it.")
        chatPlayer(quiz, "Them? Who else was there?")
        xil(neutral, "Another.")
        chatPlayer(quiz, "Another?")
        mej(neutral, "Another.")
        chatPlayer(neutral, "I can see we're not going to get anywhere here.")
    }

    private suspend fun Dialogue.xeric() {
        chatPlayer(quiz, "What do you know of Xeric?")
        ket(angry, "That name.")
        xil(angry, "It is not welcome here.")
        chatPlayer(
            worried,
            "Right... sorry. What was it you called him? The great betrayer?",
        )
        mej(neutral, "An apt description.")
        chatPlayer(quiz, "But why? What did he do?")
        xil(neutral, "Threatened the balance.")
        chatPlayer(quiz, "Plenty of people threaten the balance. Why was he especially bad?")
        mej(neutral, "It is not your concern.")
        chatPlayer(
            quiz,
            "What are you hiding? You call him a betrayer... Did he betray you? Did you work " +
                "with him?",
        )
        ket(neutral, "It is not your concern.")
    }

    private suspend fun Dialogue.pyromancer() {
        chatPlayer(quiz, "Do you know anything about this rogue Pyromancer?")
        xil(neutral, "No.")
        mej(neutral, "They are one of many.")
        ket(neutral, "We can't determine which one.")
        chatPlayer(quiz, "So you know nothing of them?")
        mej(neutral, "They have a great strength.")
        xil(neutral, "More than many of the others.")
        chatPlayer(worried, "They sound dangerous. What do you plan to do?")
        ket(neutral, "Nothing.")
        chatPlayer(shocked, "Nothing?")
        xil(neutral, "The Doors of Dinh are safe.")
        ket(neutral, "They can do no more harm.")
        chatPlayer(
            angry,
            "But whoever they are, they're working with Xamphur. Many lives could be at risk.",
        )
        mej(neutral, "Lives are always at risk.")
        xil(neutral, "But the balance is what matters.")
        ket(neutral, "Until it is threatened.")
        mej(neutral, "There is no need for action.")
    }

    private suspend fun Dialogue.goodbye() {
        chatPlayer(neutral, "Goodbye.")
        mej(neutral, "Farewell human.")
    }

    private suspend fun Dialogue.xil(mesanim: MesAnimType, text: String) =
        chatNpcSpecific("Kaal-Xil-Dar", XIL, mesanim, text)

    private suspend fun Dialogue.ket(mesanim: MesAnimType, text: String) =
        chatNpcSpecific("Kaal-Ket-Jor", KET, mesanim, text)

    private suspend fun Dialogue.mej(mesanim: MesAnimType, text: String) =
        chatNpcSpecific("Kaal-Mej-San", MEJ, mesanim, text)

    private companion object {
        const val KET = "npc.tasakaal_ket"
        const val MEJ = "npc.tasakaal_mej"
        const val XIL = "npc.tasakaal_xil"
        const val ASCENT_OF_ARCEUUS = "quest_ascentofarceuus"
        const val KINGDOM_DIVIDED = "quest_kingdomdivided"
    }
}
