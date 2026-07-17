package org.rsmod.content.areas.city.lumbridge.npcs

import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.lumbridge.RuneMysteriesQuest
import org.rsmod.content.quest.area.lumbridge.rmTalisman
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DukeHoracio @Inject constructor(private val runeMysteries: RuneMysteriesQuest) : PluginScript() {

    private val quest
        get() = runeMysteries.quest

    private var Player.dragonQuest by intVarp("varp.dragonquest")

    override fun ScriptContext.startup() {
        onOpNpc1("npc.duke_of_lumbridge") { startDukeDialogue(it.npc) }
    }

    private suspend fun ProtectedAccess.startDukeDialogue(npc: Npc) {
        startDialogue(npc) { dukeDialogue(npc) }
    }

    private suspend fun Dialogue.dukeDialogue(npc: Npc) {
        chatNpc(happy, "Greetings. Welcome to my castle.")

        val hasShield = player.inv.contains(ANTI_DRAGON_SHIELD)
        val showShieldOption = !hasShield && player.dragonQuest >= 1
        val stage = quest.getQuestStage(player)
        val questOption =
            when {
                stage == RuneMysteriesQuest.STAGE_TALISMAN -> "What did you want me to do again?"
                else -> "Have you any quests for me?"
            }

        val choice =
            if (showShieldOption) {
                choice3(
                    "I seek a shield that will protect me from dragonbreath.",
                    1,
                    questOption,
                    2,
                    "Where can I find money?",
                    3,
                )
            } else {
                choice2(
                    questOption,
                    2,
                    "Where can I find money?",
                    3,
                )
            }

        when (choice) {
            1 -> dukeDragonShield(npc)
            2 -> dukeQuestBranch(npc)
            3 -> {
                chatPlayer(quiz, "Where can I find money?")
                chatNpc(
                    happy,
                    "I've heard that the blacksmiths are prosperous amongst the peasantry. " +
                        "Maybe you could try your hand at that?",
                )
            }
        }
    }

    private suspend fun Dialogue.dukeQuestBranch(npc: Npc) {
        val stage = quest.getQuestStage(player)
        when {
            stage == 0 -> dukeStartQuest(npc)
            stage == RuneMysteriesQuest.STAGE_TALISMAN && !hasAirTalisman(player) ->
                dukeReplaceTalisman(npc)
            stage == RuneMysteriesQuest.STAGE_TALISMAN -> {
                chatPlayer(quiz, "What did you want me to do again?")
                chatNpc(
                    happy,
                    "Take that talisman I gave you to Sedridor at the Wizards' Tower. You'll find " +
                        "it south west of here, across the bridge from Draynor Village.",
                )
                chatPlayer(happy, "Okay, will do.")
            }
            stage in RuneMysteriesQuest.STAGE_TALISMAN_GIVEN until RuneMysteriesQuest.STAGE_COMPLETE -> {
                chatPlayer(quiz, "Have you any quests for me?")
                chatNpc(happy, "The only job I had was the delivery of that talisman, so I'm afraid not.")
            }
            else -> {
                chatPlayer(quiz, "Have you any quests for me?")
                chatNpc(happy, "No, all is well for me.")
            }
        }
    }

    private suspend fun Dialogue.dukeStartQuest(npc: Npc) {
        chatPlayer(quiz, "Have you any quests for me?")
        chatNpc(
            quiz,
            "Well, I wouldn't describe it as a quest, but there is something I could use some help with.",
        )
        chatPlayer(quiz, "What is it?")
        chatNpc(
            happy,
            "We were recently sorting through some of the things stored down in the cellar, and we " +
                "found this old talisman.",
        )
        mesbox("The Duke shows you a talisman.")
        chatNpc(
            happy,
            "The Order of Wizards over at the Wizards' Tower have been on the hunt for magical " +
                "artefacts recently. I wonder if this might be just the kind of thing they're after.",
        )
        chatNpc(quiz, "Would you be willing to take it to them for me?")

        when (
            choice2(
                "Sure, no problem.",
                1,
                "Not right now.",
                2,
                title = "Start the Rune Mysteries quest?",
            )
        ) {
            1 -> {
                chatPlayer(happy, "Sure, no problem.")
                if (access.invAdd(access.inv, RuneMysteriesQuest.AIR_TALISMAN).failure) {
                    chatNpc(
                        sad,
                        "You don't seem to have any free inventory space. Come back when you do.",
                    )
                    return
                }
                player.rmTalisman = true
                quest.advanceQuestStage(access)
                chatNpc(
                    happy,
                    "Thank you very much. You'll find the Wizards' Tower south west of here, " +
                        "across the bridge from Draynor Village. When you arrive, look for " +
                        "Sedridor. He is the Archmage of the wizards there.",
                )
                objbox(RuneMysteriesQuest.AIR_TALISMAN, "The Duke hands you the talisman.")
            }
            2 -> {
                chatPlayer(sad, "Not right now.")
                chatNpc(sad, "As you wish. Hopefully I can find someone else to help.")
            }
        }
    }

    private suspend fun Dialogue.dukeReplaceTalisman(npc: Npc) {
        chatPlayer(quiz, "What did you want me to do again?")
        chatNpc(quiz, "Did you take that talisman to Sedridor?")
        chatPlayer(sad, "No, I lost it.")
        chatNpc(
            happy,
            "Ah, well that explains things. One of my servants found it outside, and it seemed " +
                "too much of a coincidence that another would suddenly show up.",
        )
        if (access.invAdd(access.inv, RuneMysteriesQuest.AIR_TALISMAN).failure) {
            chatNpc(sad, "You don't have space for it right now. Come back when you do.")
            return
        }
        player.rmTalisman = true
        chatNpc(
            happy,
            "Here, take it to the Wizards' Tower, south west of here. Please try not to lose it this time.",
        )
        objbox(RuneMysteriesQuest.AIR_TALISMAN, "The Duke hands you the talisman.")
    }

    private suspend fun Dialogue.dukeDragonShield(npc: Npc) {
        chatPlayer(quiz, "I seek a shield that will protect me from dragonbreath.")
        chatNpc(quiz, "A knight going on a dragon quest, hmm? What dragon do you intend to slay?")

        when (
            choice2(
                "Elvarg, the dragon of Crandor island!",
                1,
                "Oh, no dragon in particular.",
                2,
            )
        ) {
            1 -> {
                chatPlayer(happy, "Elvarg, the dragon of Crandor island!")
                chatNpc(shocked, "Elvarg? Are you sure?")
                when (choice2("Yes.", 1, "I'd better leave that dragon alone.", 2, title = "Well, are you sure?")) {
                    1 -> {
                        chatPlayer(happy, "Yes.")
                        val gender =
                            if (player.appearance.bodyType == Constants.bodytype_a) "man" else "woman"
                        chatNpc(happy, "Well, you're a braver $gender than I!")
                        chatPlayer(quiz, "Why is everyone so scared of this dragon?")
                        chatNpc(
                            sad,
                            "Back in my father's day, Crandor was an important city-state. " +
                                "Politically it was as important as Falador and Varrock and its ships " +
                                "traded with every port.",
                        )
                        chatNpc(
                            sad,
                            "But, one day, when I was little, all contact was lost. The trading ships " +
                                "and the diplomatic envoys just stopped coming.",
                        )
                        chatNpc(
                            sad,
                            "I remember my father being very scared. He posted lookouts on the roof " +
                                "to warn if the dragon was approaching. All the city rulers worried " +
                                "that Elvarg would devastate the whole continent.",
                        )
                        when (
                            choice2(
                                "So, are you going to give me the shield or not?",
                                1,
                                "I'd better leave that dragon alone.",
                                2,
                            )
                        ) {
                            1 -> giveAntiDragonShield(finishedDragonSlayer = false)
                            2 -> leaveDragonAlone()
                        }
                    }
                    2 -> leaveDragonAlone()
                }
            }
            2 -> {
                chatPlayer(quiz, "Oh, no dragon in particular. I just feel like killing a dragon.")
                if (player.dragonQuest >= 2) {
                    chatNpc(
                        happy,
                        "Of course. Now you've slain Elvarg, you've earned the right to call the " +
                            "shield your own!",
                    )
                    giveAntiDragonShield(finishedDragonSlayer = true)
                } else {
                    chatNpc(
                        angry,
                        "I don't have an infinite supply of these shields, you know. I'll only give " +
                            "one for a truly worthy cause.",
                    )
                }
            }
        }
    }

    private suspend fun Dialogue.leaveDragonAlone() {
        chatPlayer(sad, "I'd better leave that dragon alone.")
        chatNpc(
            happy,
            "That's a relief. I would hate to see such a promising adventurer cut down in " +
                "${if (player.appearance.bodyType == Constants.bodytype_a) "his" else "her"} prime.",
        )
    }

    private suspend fun Dialogue.giveAntiDragonShield(finishedDragonSlayer: Boolean) {
        if (!finishedDragonSlayer) {
            chatPlayer(quiz, "So, are you going to give me the shield or not?")
            chatNpc(
                happy,
                "If you really think you're up to it then perhaps you are the one who can kill this dragon.",
            )
        }
        if (!player.inv.contains(ANTI_DRAGON_SHIELD)) {
            if (access.invAdd(access.inv, ANTI_DRAGON_SHIELD).failure) {
                chatNpc(sad, "You don't have enough inventory space for the shield.")
                return
            }
        }
        mesbox("The Duke hands you a heavy orange shield.")
        if (!finishedDragonSlayer) {
            chatNpc(worried, "Take care out there. If you kill it...")
            chatNpc(angry, "If you kill it, for Saradomin's sake make sure it's really dead!")
        }
    }

    private fun hasAirTalisman(player: Player): Boolean =
        player.inv.contains(RuneMysteriesQuest.AIR_TALISMAN)

    private companion object {
        const val ANTI_DRAGON_SHIELD = "obj.antidragonbreathshield"
    }
}
