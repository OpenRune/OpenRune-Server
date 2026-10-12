package org.rsmod.content.areas.city.burthorpe.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class BurthorpeCastle : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.death_ig_commander") { startDialogue(it.npc) { denulth() } }
        onOpNpc1("npc.death_headservant") { startDialogue(it.npc) { eohric() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Denulth
    // TODO(content): [quest:death_plateau] in-progress dialogue
    // TODO(content): [quest:troll_stronghold] in-progress dialogue
    private suspend fun Dialogue.denulth() {
        if (QuestRequirements.hasCompleted(player, DEATH_PLATEAU)) {
            denulthAfterDeathPlateau()
            return
        }
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

    // TODO(content): [verify:in_game] the wiki gives "I thought the White Knights controlled
    //  Asgarnia?" only after Troll Stronghold; this uses that text after Death Plateau too
    private suspend fun Dialogue.denulthAfterDeathPlateau() {
        val trollStronghold = QuestRequirements.hasCompleted(player, TROLL_STRONGHOLD)
        chatPlayer(happy, "Hello!")
        chatNpc(happy, "Welcome back friend!")
        while (true) {
            val option =
                choice3(
                    "How goes your fight with the trolls?",
                    1,
                    "I thought the White Knights controlled Asgarnia?",
                    2,
                    "See you about Denulth!",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "How goes your fight with the trolls?")
                    if (trollStronghold) {
                        chatNpc(
                            happy,
                            "We are busy preparing for an attack by night. Godric knows of a " +
                                "secret entrance to the stronghold. Once we destroy the " +
                                "stronghold Burthorpe will be safe! Friend, we are indebted to " +
                                "you!",
                        )
                        chatPlayer(happy, "Good luck!")
                        return
                    }
                    denulthTrollStrongholdOffer()
                    return
                }
                2 -> denulthWhiteKnights()
                3 -> {
                    chatPlayer(happy, "See you about Denulth!")
                    chatNpc(happy, "God speed friend!")
                    return
                }
            }
        }
    }

    // TODO(content): [quest:troll_stronghold] "Start Troll Stronghold Quest?" offer and start
    private suspend fun Dialogue.denulthTrollStrongholdOffer() {
        chatNpc(
            sad,
            "I'm afraid I have bad news. We made our attack as planned, but we met unexpected " +
                "resistance.",
        )
        chatPlayer(quiz, "What happened?")
        chatNpc(
            sad,
            "We were ambushed by trolls coming from the north. They captured Dunstan's son, " +
                "Godric, who we enlisted at your request, we tried to follow but we were " +
                "repelled at the foot of their stronghold.",
        )
    }

    private suspend fun Dialogue.denulthWhiteKnights() {
        chatPlayer(quiz, "I thought the White Knights controlled Asgarnia?")
        chatNpc(
            neutral,
            "You are right citizen. The White Knights have taken advantage of the old and weak " +
                "king, they control most of Asgarnia, including Falador. However they do not " +
                "control Burthorpe!",
        )
        chatNpc(neutral, "We are the prince's elite troops! We keep Burthorpe secure!")
        chatNpc(
            angry,
            "The White Knights have overlooked us, until now! They are pouring money into their " +
                "war against the Black Knights, they are looking for an excuse to stop our " +
                "funding and I'm afraid they may have found it!",
        )
        chatNpc(
            angry,
            "If we can not destroy the troll camp on Death Plateau then the Imperial Guard will " +
                "be disbanded and Burthorpe will come under control of the White Knights. We " +
                "can not let this happen!",
        )
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

    private companion object {
        const val DEATH_PLATEAU = "quest_deathplateau"
        const val TROLL_STRONGHOLD = "quest_trollstronghold"
    }
}
