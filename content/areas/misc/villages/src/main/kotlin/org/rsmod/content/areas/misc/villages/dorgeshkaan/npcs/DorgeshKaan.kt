package org.rsmod.content.areas.misc.villages.dorgeshkaan.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class DorgeshKaan : PluginScript() {
    override fun ScriptContext.startup() {
        for (goblin in CAVE_GOBLINS) {
            onOpNpc1(goblin) { startDialogue(it.npc) { caveGoblin() } }
        }
        for (miner in MINERS) {
            onOpNpc1(miner) { startDialogue(it.npc) { miner() } }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Cave_goblin_(Dorgesh-Kaan)
    private suspend fun Dialogue.caveGoblin() {
        when (access.random.of(5)) {
            0 -> chatNpc(quiz, "How can you see properly with such tiny little eyes?")
            1 -> {
                chatNpc(happy, "Ooh! Are you ${player.displayName}?")
                chatPlayer(happy, "Yes.")
                chatNpc(
                    happy,
                    "Everyone's talking about how you helped Zanik save the city! You're a hero!",
                )
            }
            2 -> {
                chatNpc(
                    happy,
                    "Have you visited the market? There are visitors selling foods from all over " +
                        "the surface there. It's so exotic!",
                )
                chatNpc(
                    neutral,
                    "Well, I suppose it wouldn't be exotic for you, since you're from the surface.",
                )
                chatNpc(
                    happy,
                    "But you should check it out anyway. Maybe you could sell some of your food. " +
                        "Everyone's crazy about surface food at the moment.",
                )
            }
            3 -> {
                chatNpc(quiz, "So, what do you think of our city, surface-dweller?")
                val option = choice2("I like it!", 1, "I think it's rubbish.", 2)
                if (option == 1) {
                    chatPlayer(happy, "I like it!")
                    chatNpc(happy, "That, surface-dweller, was the right answer.")
                } else {
                    chatPlayer(neutral, "I think it's rubbish.")
                    chatNpc(
                        angry,
                        "Well, it beats crawling around on the surface with nothing above your " +
                            "head!",
                    )
                }
            }
            else -> {
                chatNpc(
                    worried,
                    "What's it like on the surface? It sounds scary to me, having nothing above " +
                        "my head.",
                )
                val option = choice2("It's alright.", 1, "It's not all that great.", 2)
                if (option == 1) {
                    chatPlayer(neutral, "It's alright.")
                    chatNpc(worried, "I'm still not sure it would be for me, though.")
                } else {
                    chatPlayer(neutral, "It's not all that great.")
                    chatNpc(happy, "Never mind, you can stay down here now!")
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Cave_goblin_miner
    private suspend fun Dialogue.miner() {
        if (QuestRequirements.hasNotCompleted(player, LOST_TRIBE)) {
            chatNpc(shocked, "Help! An outsider!")
            return
        }
        chatNpc(happy, "Hello.")
        val option =
            choice3(
                "How's the mining going?",
                1,
                "Where do you take all this ore?",
                2,
                "Why do you use bones to build with?",
                3,
            )
        when (option) {
            1 -> miningGoing()
            2 -> {
                chatPlayer(quiz, "Where do you take all this ore?")
                chatNpc(neutral, "To the town of Dorgesh-Kaan, deep in the caves.")
            }
            else -> bones()
        }
    }

    private suspend fun Dialogue.miningGoing() {
        chatPlayer(quiz, "How's the mining going?")
        when (access.random.of(5)) {
            0 -> {
                chatNpc(worried, "I nearly got caught in a rockfall earlier!")
                chatNpc(
                    happy,
                    "Still, since humans started coming down here, the proportion of miners " +
                        "killed in rockfalls has fallen dramatically!",
                )
            }
            1 -> chatNpc(neutral, "Oh, you know, can't complain.")
            2 ->
                chatNpc(
                    happy,
                    "It's going well. I find repeatedly hitting a piece of rock very therapeutic.",
                )
            3 -> {
                chatNpc(shifty, "Well, don't tell Mistag...")
                chatNpc(laugh, "...but actually I wasn't working very hard!")
            }
            else ->
                chatNpc(
                    neutral,
                    "We're a bit behind schedule. If you lent us a hand, Mistag would pay you " +
                        "for your work.",
                )
        }
    }

    private suspend fun Dialogue.bones() {
        chatPlayer(quiz, "Why do you use bones to build with?")
        chatNpc(
            neutral,
            "Well, our legends say that on the surface there was a substance called 'wood', " +
                "which grew out of the ground in towers.",
        )
        chatNpc(
            neutral,
            "But we cannot get wood here, so we can only build with stone, bone, and iron.",
        )
        val option = choice2("Oh, right", 1, "Giant frogs aren't fearsome!", 2)
        if (option == 1) {
            chatPlayer(neutral, "Oh, right.")
            return
        }
        chatPlayer(laugh, "Giant frogs aren't fearsome!")
        chatNpc(
            worried,
            "Perhaps not for a mighty adventurer like you, but we think they're terrifying!",
        )
    }

    private companion object {
        const val LOST_TRIBE = "quest_losttribe"

        val CAVE_GOBLINS =
            listOf(
                "npc.dorgesh_female_1",
                "npc.dorgesh_female_4",
                "npc.dorgesh_female_5",
                "npc.dorgesh_female_6",
                "npc.dorgesh_female_7",
                "npc.dorgesh_female_8",
                "npc.dorgesh_male_1",
                "npc.dorgesh_male_2",
                "npc.dorgesh_male_4",
                "npc.dorgesh_male_5",
                "npc.dorgesh_male_6",
                "npc.dorgesh_male_7",
                "npc.dorgesh_male_patrol2",
            )

        val MINERS =
            listOf(
                "npc.cave_goblin_miner",
                "npc.cave_goblin_miner2",
                "npc.cave_goblin_miner3",
                "npc.cave_goblin_miner4",
                "npc.cave_goblin_miner4_mining",
            )
    }
}
