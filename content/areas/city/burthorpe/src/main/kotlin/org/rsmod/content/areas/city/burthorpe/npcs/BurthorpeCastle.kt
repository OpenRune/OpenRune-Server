package org.rsmod.content.areas.city.burthorpe.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class BurthorpeCastle : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.death_ig_commander") { startDialogue(it.npc) { denulth() } }
        onOpNpc1("npc.death_headservant") { startDialogue(it.npc) { eohric() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Denulth (before Death Plateau)
    // TODO(content): [quest:death_plateau] in-progress and after-quest dialogue
    // TODO(content): [quest:troll_stronghold] quest start and after-quest dialogue
    private suspend fun Dialogue.denulth() {
        chatPlayer(happy, "Hello!")
        chatNpc(neutral, "Hello citizen, how can I help?")
        while (true) {
            val option =
                choice3(
                    "Do you have any quests for me?",
                    1,
                    "What is this place?",
                    2,
                    "You can't, thanks.",
                    3,
                )
            when (option) {
                1 -> {
                    denulthQuest()
                    return
                }
                2 -> {
                    chatPlayer(quiz, "What is this place?")
                    chatNpc(neutral, "Welcome to Principality of Burthorpe!")
                    chatNpc(
                        neutral,
                        "We are the Imperial Guard for his Royal Highness Prince Anlaf of " +
                            "Burthorpe.",
                    )
                    chatNpc(neutral, "Can I assist you with anything else?")
                }
                3 -> {
                    chatPlayer(neutral, "You can't, thanks.")
                    return
                }
            }
        }
    }

    // TODO(content): [quest:death_plateau] "Start Death Plateau?" offer and start
    private suspend fun Dialogue.denulthQuest() {
        chatPlayer(quiz, "Do you have any quests for me?")
        chatNpc(worried, "I don't know if you can help us!")
        chatNpc(
            worried,
            "The trolls have taken up camp on Death Plateau! They are using it to launch raids " +
                "at night on the village. We have tried to attack the camp but the main path is " +
                "heavily guarded!",
        )
        chatPlayer(quiz, "Perhaps there is a way you can sneak up at night?")
        chatNpc(neutral, "If there is another way I do not know of it.")
        chatNpc(quiz, "Do you know of such a path?")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Eohric
    // TODO(content): [quest:death_plateau] quest dialogue about Harold
    // TODO(content): [system:treasure_trails] medium challenge scroll
    private suspend fun Dialogue.eohric() {
        chatPlayer(happy, "Hi!")
        chatNpc(happy, "Hi, can I help?")
        while (true) {
            val option =
                choice4(
                    "What is this castle?",
                    1,
                    "Where is the prince?",
                    2,
                    "Do you have any quests for me?",
                    3,
                    "No, I'm just looking around.",
                    4,
                )
            when (option) {
                1 -> {
                    chatNpc(
                        happy,
                        "Welcome to Burthorpe Castle! Home to his Royal Highness the Prince of " +
                            "Burthorpe, Heir to the Throne of Asgarnia!",
                    )
                    chatPlayer(shocked, "Wow!")
                }
                2 -> {
                    chatPlayer(quiz, "Where is the prince?")
                    chatNpc(neutral, "We do not disclose his majesty's business.")
                }
                3 -> {
                    chatPlayer(quiz, "Do you have any quests for me?")
                    chatNpc(neutral, "Yes actually I do, we need another servant.")
                    chatPlayer(quiz, "Does this noble quest have a name?")
                    chatNpc(shifty, "Yes, it's the... er... Empty the Cesspit Quest!")
                    chatPlayer(laugh, "I'll give it a miss!")
                }
                4 -> {
                    chatPlayer(neutral, "No, I'm just looking around.")
                    return
                }
            }
            chatNpc(neutral, "Anything else I can help you with?")
        }
    }
}
