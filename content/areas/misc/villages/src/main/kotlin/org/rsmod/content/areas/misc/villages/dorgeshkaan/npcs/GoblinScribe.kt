package org.rsmod.content.areas.misc.villages.dorgeshkaan.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Goblin_scribe
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [verify:in_game] the values of the varbit.dorgesh_history_* answers, and what
// the scribe says before a question has been answered (the first answer is used)
class GoblinScribe : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.dorgesh_male_scribe") { startDialogue(it.npc) { scribe() } }
    }

    private suspend fun Dialogue.scribe() {
        chatNpc(happy, "Hmm? Oh, hello, surface-dweller!")
        val option =
            choice3(
                "What are you doing?",
                1,
                "What is this building?",
                2,
                "Can you tell me about the history of the city?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What are you doing?")
                chatNpc(happy, "I'm writing the definitive history of Dorgesh-Kaan!")
                chatNpc(quiz, "Actually, I wonder if you could help me...")
                when (access.random.of(5)) {
                    0 -> askTitle()
                    1 -> askGod()
                    2 -> askReason()
                    3 -> askHam()
                    else -> askSpecies()
                }
            }
            2 -> {
                chatPlayer(quiz, "What is this building?")
                chatNpc(happy, "This is the Dorgeshuun Council chamber!")
                chatNpc(
                    neutral,
                    "Whenever a decision needs to be made, the seven members of the Dorgeshuun " +
                        "Council meet here. The last meeting was when they decided to open up " +
                        "the city to surface-dwellers.",
                )
                chatNpc(
                    neutral,
                    "In between meetings, I work here writing up the minutes of the meetings and " +
                        "working on the definitive history of Dorgesh-Kaan.",
                )
            }
            3 -> history()
        }
    }

    private suspend fun Dialogue.askTitle() {
        chatNpc(
            quiz,
            "I know the ruler of Lumbridge is called Horacio, but I don't know what his title " +
                "is. Can you tell me?",
        )
        val title =
            choice4("He's a duke.", 1, "He's a general.", 2, "He's a king.", 3, "He's a lord.", 4)
        chatPlayer(neutral, "He's a ${TITLES[title - 1].lowercase()}.")
        vars[TITLE] = title
        chatNpc(happy, "${TITLES[title - 1]} Horacio. Thank you.")
    }

    private suspend fun Dialogue.askGod() {
        chatNpc(
            quiz,
            "You've talked to the goblins on the surface, haven't you? Do you know the name of " +
                "the god that the surface goblins worship?",
        )
        while (true) {
            val god =
                choice4(
                    "Nobody knows.",
                    1,
                    "Big High War God.",
                    2,
                    "Zamorak.",
                    3,
                    "More...",
                    0,
                )
            if (god != 0) {
                vars[GOD] = god
                when (god) {
                    1 -> {
                        chatPlayer(neutral, "Nobody knows.")
                        chatNpc(neutral, "Oh well. Thanks anyway.")
                    }
                    2 -> {
                        chatPlayer(neutral, "It's just called the Big High War God.")
                        chatNpc(happy, "Ah, thank you.")
                    }
                    else -> {
                        chatPlayer(neutral, "It was Zamorak.")
                        chatNpc(happy, "Ah, thank you.")
                    }
                }
                return
            }
            val more =
                choice4(
                    "It was Guthix.",
                    4,
                    "It was Saradomin.",
                    5,
                    "It was ${player.displayName}",
                    6,
                    "More...",
                    0,
                )
            if (more == 0) continue
            vars[GOD] = more
            when (more) {
                4 -> {
                    chatPlayer(neutral, "It was Guthix.")
                    chatNpc(happy, "Ah, thank you.")
                }
                5 -> {
                    chatPlayer(neutral, "It was Saradomin.")
                    chatNpc(happy, "Ah, thank you.")
                }
                else -> {
                    chatPlayer(neutral, "It was ${player.displayName}.")
                    chatNpc(shocked, "${player.displayName}? What a terrible, fear-inducing name!")
                }
            }
            return
        }
    }

    private suspend fun Dialogue.askReason() {
        chatNpc(
            quiz,
            "I'm writing about the great war between the gods that happened thousands of years " +
                "ago. Do you know the reason for this war?",
        )
        val reason =
            choice4(
                "It was for the gods' own entertainment.",
                1,
                "Zamorak attacked the other gods.",
                2,
                "The other gods attacked Zaros.",
                3,
                "Guthix wanted to preserve balance.",
                4,
            )
        vars[REASON] = reason
        when (reason) {
            1 -> {
                chatPlayer(neutral, "It was for the gods' own entertainment.")
                chatNpc(happy, "Just as I'd thought. Thank you.")
            }
            2 -> {
                chatPlayer(neutral, "Zamorak attacked the other gods.")
                chatNpc(happy, "Really? Thank you for the information.")
            }
            3 -> {
                chatPlayer(neutral, "The other gods attacked Zaros.")
                chatNpc(happy, "Thanks.")
            }
            else -> {
                chatPlayer(
                    neutral,
                    "Guthix wanted to preserve balance so he made the other gods fight.",
                )
                chatNpc(happy, "A strange idea of balance! Thank you.")
            }
        }
    }

    private suspend fun Dialogue.askHam() {
        chatNpc(
            quiz,
            "What's the name of the group of surface-people that tried to destroy Dorgesh-Kaan?",
        )
        val group = choice4("H.A.M.", 1, "B.A.C.O.N.", 2, "P.O.R.K.", 3, "B.E.E.F.", 4)
        vars[HAM] = group
        chatPlayer(neutral, GROUPS[group - 1])
        if (group == 1) {
            chatNpc(quiz, "Hmm, I wonder what that stands for?")
            chatPlayer(neutral, "It stands for Humans Against Monsters.")
            chatNpc(angry, "So we're monsters, are we? Thanks for telling me!")
            return
        }
        val question = if (group == 3) "?" else "."
        chatNpc(quiz, "Hmm, I wonder what that stands for$question Thank you.")
    }

    private suspend fun Dialogue.askSpecies() {
        chatNpc(
            quiz,
            "What species are you, if you don't mind my asking? I know there are many different " +
                "races living on the surface, but I don't know which one you are.",
        )
        val previous = vars[SPECIES].coerceAtLeast(1)
        val species =
            choice4("I'm human!", 1, "I'm an ogre.", 2, "I'm a dwarf.", 3, "I'm an elf.", 4)
        vars[SPECIES] = species
        when (species) {
            1 -> {
                chatPlayer(happy, "I'm a human!")
                when (previous) {
                    1 -> chatNpc(happy, "Ah, good, that's what I thought.")
                    2 ->
                        chatNpc(
                            happy,
                            "That's a relief! I thought you were some kind of horrible ogre!",
                        )
                    3 ->
                        chatNpc(
                            neutral,
                            "Ah, of course. I thought you looked a little tall for a dwarf.",
                        )
                    else ->
                        chatNpc(
                            shocked,
                            "Oh! I thought you were an elf. Surface species are confusing!",
                        )
                }
                chatNpc(happy, "A human, hmm? Thank you.")
            }
            2 -> {
                chatPlayer(angry, "I'm an ogre. Raah!")
                when (previous) {
                    1 -> chatNpc(shocked, "Eek! An ogre! I had thought you were human!")
                    2 -> chatNpc(happy, "Ah, that's what I thought!")
                    3 ->
                        chatNpc(
                            neutral,
                            "Ah, of course. I thought you looked a little tall for a dwarf.",
                        )
                    else -> chatNpc(shocked, "And I thought you were an elf! What a silly mistake!")
                }
                chatNpc(happy, "An ogre, hmm? Thank you.")
            }
            3 -> {
                chatPlayer(happy, "I'm a dwarf!")
                when (previous) {
                    1 ->
                        chatNpc(
                            confused,
                            "Oh? I thought dwarves were shorter. I thought you were human!",
                        )
                    2 ->
                        chatNpc(
                            happy,
                            "That's a relief! I thought you were some kind of horrible ogre!",
                        )
                    3 -> chatNpc(neutral, "Ah, yes, of course, you said.")
                    else ->
                        chatNpc(
                            shocked,
                            "Oh! I thought you were an elf. Surface species are confusing!",
                        )
                }
                chatNpc(happy, "A dwarf, hmm? Thank you.")
            }
            else -> {
                chatPlayer(happy, "I'm an elf!")
                when (previous) {
                    1 ->
                        chatNpc(
                            confused,
                            "Oh? I thought elves were more graceful. I thought you were human!",
                        )
                    2 ->
                        chatNpc(
                            happy,
                            "That's a relief! I thought you were some kind of horrible ogre!",
                        )
                    3 -> {
                        chatNpc(
                            confused,
                            "Oh? I thought elves were more graceful. I thought you were a dwarf!",
                        )
                        return
                    }
                    else -> {
                        chatPlayer(happy, "I'm an elf!")
                        chatNpc(happy, "Ah yes, that's what I thought.")
                    }
                }
                chatNpc(happy, "An elf, hmm? Thank you.")
            }
        }
    }

    private suspend fun Dialogue.history() {
        chatPlayer(quiz, "Can you tell me about the history of the city?")
        chatNpc(
            happy,
            "Of course! I've gone through all of my predecessors' notes and compiled the " +
                "definitive history of the Dorgeshuun! What would you like to hear about?",
        )
        val option =
            choice3(
                "The founding of Dorgesh-Kaan.",
                1,
                "The Dorgeshuun civil war.",
                2,
                "Recent years.",
                3,
            )
        when (option) {
            1 -> founding()
            2 -> civilWar()
            3 -> recentYears()
        }
    }

    private suspend fun Dialogue.founding() {
        chatPlayer(quiz, "Can you tell me about the founding of Dorgesh-Kaan?")
        chatNpc(happy, "Of course!  *Ahem*")
        val god =
            when (vars[GOD].coerceAtLeast(1)) {
                1 -> "an evil god whose name has been forgotten"
                2 -> "an evil god called Big High War God"
                3 -> "an evil god called Zamorak"
                4 -> "an evil god called Guthix"
                5 -> "an evil god called Saradomin"
                else -> "an evil god called ${player.displayName}"
            }
        chatNpc(
            neutral,
            "Before Dorgesh-Kaan, the Dorgeshuun lived on the surface with many other goblin " +
                "tribes. All goblins were ruled over by $god.",
        )
        val war =
            when (vars[REASON].coerceAtLeast(1)) {
                1 ->
                    "The gods became bored and decided to make the mortal races fight for their " +
                        "amusement."
                2 -> "The god Zamorak attacked the other gods, starting a great war."
                3 ->
                    "The other gods decided to remove the great god Zaros from power, and " +
                        "started a great war."
                else ->
                    "In order to preserve balance, the god Guthix made the other gods fight " +
                        "against one another."
            }
        chatNpc(
            neutral,
            "$war The goblins were sent to fight in many bloody battles against each other and " +
                "other races.",
        )
        chatNpc(
            neutral,
            "The war lasted for thousands of years, and millions of goblins died. Eventually, " +
                "the evil god sent the Dorgeshuun tribe to fight a battle that they could not win.",
        )
        chatNpc(
            neutral,
            "But General Bloodfist of the Dorgeshuun refused to fight in this battle. Instead, " +
                "he commanded the Dorgeshuun army to march down into a fissure in the ground.",
        )
        chatNpc(
            neutral,
            "General Bloodfist stood at the mouth of the fissure and defied the might of the " +
                "evil god.",
        )
        chatNpc(
            neutral,
            "The evil god smote him with a mighty blast that shook the earth, but that blast " +
                "also closed the fissure, sealing the Dorgeshuun away from the reach of the gods.",
        )
        chatNpc(
            neutral,
            "Lieutenant Strongaxe, now the leader of the Dorgeshuun, led the tribe deeper into " +
                "the caves. They found this cavern, and here they founded Dorgesh-Kaan, city of " +
                "the Dorgeshuun.",
        )
    }

    private suspend fun Dialogue.civilWar() {
        chatPlayer(quiz, "Can you tell me about the Dorgeshuun civil war?")
        chatNpc(happy, "Certainly!  *Ahem*")
        for (line in CIVIL_WAR) chatNpc(neutral, line)
    }

    private suspend fun Dialogue.recentYears() {
        chatPlayer(quiz, "Can you tell me about recent years?")
        chatNpc(happy, "Ah, this is where you come in!  *Ahem*")
        chatNpc(
            neutral,
            "In the twenty-ninth century since the founding of the city, one of the miners broke " +
                "through a wall into the cellar of a surface dwelling. The miners blocked up the " +
                "tunnel and went back into their mines.",
        )
        val species =
            when (vars[SPECIES].coerceAtLeast(1)) {
                1 -> "a human"
                2 -> "an ogre"
                3 -> "a dwarf"
                else -> "an elf"
            }
        chatNpc(
            neutral,
            "However, $species named ${player.displayName} found ${pronoun()} way through the " +
                "caves and made contact with the miners.",
        )
        val title = TITLES[vars[TITLE].coerceAtLeast(1) - 1]
        chatNpc(
            neutral,
            "Ur-tag, head of the Council, signed a peace treaty with $title Horacio of " +
                "Lumbridge, ruler of the town above the Dorgeshuuns' mine.",
        )
        val group = GROUPS[vars[HAM].coerceAtLeast(1) - 1]
        chatNpc(
            neutral,
            "But not all surface-dwellers were friendly, and a faction called $group attempted " +
                "to destroy the Dorgeshuun.",
        )
        chatNpc(
            neutral,
            "The Council sent Zanik to explore the surface to see if the $group group was " +
                "plotting anything. With ${player.displayName}'s help, Zanik defeated a $group " +
                "plot to flood Dorgesh-Kaan.",
        )
        chatNpc(
            neutral,
            "Despite reservations from many citizens, the Council eventually voted 4-3 to open " +
                "Dorgesh-Kaan to surface-dwellers.",
        )
    }

    private fun Dialogue.pronoun(): String = if (access.isBodyTypeA()) "his" else "her"

    private companion object {
        const val GOD = "varbit.dorgesh_history_god"
        const val REASON = "varbit.dorgesh_history_reason"
        const val SPECIES = "varbit.dorgesh_history_species"
        const val TITLE = "varbit.dorgesh_history_title"
        const val HAM = "varbit.dorgesh_history_ham"
        val TITLES = listOf("Duke", "General", "King", "Lord")
        val GROUPS = listOf("H.A.M.", "B.A.C.O.N.", "P.O.R.K.", "B.E.E.F.")
        val CIVIL_WAR =
            listOf(
                "At first, Dorgesh-Kaan was ruled over by generals, just as the tribe had been " +
                    "when it was on the surface. Each general succeeded the last by defeating " +
                    "him in combat.",
                "The generals were advised by a council of elders, but the Dorgeshuun were no " +
                    "longer an army, and many of the people felt that the Council should have " +
                    "authority.",
                "The people voted to become a republic, but General Bonehelm refused to give up " +
                    "power, and so the Dorgeshuun Civil War began.",
                "The republican faction was larger, but the military faction contained most of " +
                    "the trained warriors and controlled the magical equipment that the tribe " +
                    "had brought from the surface.",
                "General Bonehelm was driven from the city, but dug his own base out of the " +
                    "caves nearby.",
                "General Bonehelm's army attacked the city using archers mounted on giant frogs. " +
                    "There was a bloody battle, and General Bonehelm was driven back again into " +
                    "his base.",
                "But General Bonehelm's mining had weakened the stone around his base, and the " +
                    "retreating army's footsteps caused the roof to collapse, crushing his army.",
                "That was the last time one Dorgeshuun has raised arms against another. Since " +
                    "then, the city has been ruled peacefully by an elected council.",
            )
    }
}
