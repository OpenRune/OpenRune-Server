package org.rsmod.content.areas.misc.villages.barbarianoutpost.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Otto_Godblessed (Barbarian Training not completed)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [miniquest:barbarian_training] "Let's talk about my training." and the
//  after-training greeting
// TODO(content): [miniquest:barbarian_training] "Can you help me with hastae?"
// TODO(content): [system:plant_pot_smashing] plant pot smashing toggle options
// TODO(content): [system:treasure_trails] medium and hard clue challenges
class Otto @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.brut_otto") { startDialogue(it.npc) { otto() } }
    }

    private suspend fun Dialogue.otto() {
        chatNpc(happy, "Good day to you, hearty warrior! May the spirits guide you.")
        while (true) {
            val option =
                if (playerContainsObj(MY_NOTES)) {
                    choice3(
                        "Let's talk about my training.",
                        TRAINING,
                        "Could you tell me more of these spirits you continually refer to?",
                        SPIRITS,
                        "I'd better get going.",
                        GOODBYE,
                    )
                } else {
                    choice4(
                        "Let's talk about my training.",
                        TRAINING,
                        "Do you have anything for me?",
                        NOTES,
                        "Could you tell me more of these spirits you continually refer to?",
                        SPIRITS,
                        "I'd better get going.",
                        GOODBYE,
                    )
                }
            when (option) {
                TRAINING -> return
                NOTES -> myNotes()
                SPIRITS -> spirits()
                GOODBYE -> {
                    chatPlayer(neutral, "I'd better get going.")
                    return
                }
            }
        }
    }

    // TODO(content): [verify:in_game] Otto's reply to a player with a full inventory
    private suspend fun Dialogue.myNotes() {
        chatPlayer(quiz, "Do you have anything for me?")
        chatNpc(
            neutral,
            "I see you have free space and no way to record any information you may recover from " +
                "the caverns. Please take this book as a record of your researches.",
        )
        player.invAddOrDrop(objRepo, MY_NOTES)
        chatPlayer(quiz, "What kind of information are you thinking of?")
        chatNpc(
            neutral,
            "Those who have previously entered the cavern have not all returned. Some of them " +
                "seemed intent upon research and discovery as much as loot and daring deeds.",
        )
        chatNpc(
            neutral,
            "It would be a shame if you were to recover any of their notes and be unable to " +
                "record them handily. I hope the new book will give you space for such " +
                "discoveries.",
        )
    }

    private suspend fun Dialogue.spirits() {
        chatPlayer(quiz, "Could you tell me more of these spirits you continually refer to?")
        chatNpc(
            neutral,
            "Certainly, though it is quite simple - they are the barbarians of the past who have " +
                "died in the passing of the years. There are three categories which learned " +
                "sages claim may describe all spirits encountered.",
        )
        chatNpc(
            neutral,
            "The majority are at peace, having died with their ambitions sated, even if this " +
                "ambition was as simple as a glorious death. They can contact those in our world " +
                "in order to spur us on to glory.",
        )
        chatNpc(
            neutral,
            "Of course, they cannot or will not contact all inhabitants of these lands, for not " +
                "all are worthy.",
        )
        chatPlayer(
            quiz,
            "So those are the spirits who give us guidance. What of the other sorts?",
        )
        chatNpc(
            neutral,
            "A second category of spirit is made up of those who are not yet at peace. However, " +
                "all that is needed is for their mortal remains to be laid to rest and they will " +
                "join the peaceful majority.",
        )
        chatPlayer(
            quiz,
            "I can see the pyres serve this purpose. The final category is also restless?",
        )
        chatNpc(
            sad,
            "Alas, there is a small minority who will never be at rest. These have been slain in " +
                "moments of insanity by their brothers or by adventurers.",
        )
        chatNpc(
            worried,
            "They are not at rest and will attack those they blame for their tortured state. " +
                "They are not known, however, for being able to determine this blame with any " +
                "great accuracy, so should be considered dangerous to all.",
        )
        chatPlayer(happy, "Thank you Otto. It makes a bit more sense now.")
    }

    private companion object {
        const val MY_NOTES = "obj.brut_logbook"
        const val TRAINING = 1
        const val NOTES = 2
        const val SPIRITS = 3
        const val GOODBYE = 4
    }
}
