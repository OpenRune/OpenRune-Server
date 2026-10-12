package org.rsmod.content.areas.city.varrock.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.falador.knightssword.KnightsSwordQuest
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.quest.manager.menu
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class Reldo @Inject constructor(private val knightsSword: KnightsSwordQuest) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.reldo") { startDialogue(it.npc) { reldo() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Reldo
    // TODO(content): [quest:the_lost_tribe] brooch dialogue
    // TODO(content): [quest:a_tail_of_two_cats] Robert the Strong dialogue
    // TODO(content): [quest:dragon_slayer_ii] Tristan Corvo and the census records
    // TODO(content): [system:wise_old_man_tasks] delivering the Wise Old Man's message
    // TODO(content): [system:treasure_trails] beginner hot and cold clue
    private suspend fun Dialogue.reldo() {
        val soaStarted =
            !QuestRequirements.hasNotCompleted(player, SHIELD_OF_ARRAV) ||
                QuestRequirements.isOnQuest(player, SHIELD_OF_ARRAV)
        if (vars[RELDO_MET] == 0 && !soaStarted) {
            vars[RELDO_MET] = 1
            chatNpc(neutral, "Hello, stranger.")
            chatPlayer(
                happy,
                "Hello there! The name's ${player.displayName}. Who are you?",
            )
            chatNpc(happy, "My name is Reldo. I'm the palace librarian.")
            chatPlayer(happy, "Ah, that explains why you're in the library then.")
            chatNpc(
                happy,
                "Well, truth be told, I would probably be here even if it wasn't where I " +
                    "worked. I very much like reading, not to mention sharing what I learn with " +
                    "all who are interested.",
            )
            chatPlayer(happy, "Sounds like a very noble goal!")
        } else {
            chatNpc(happy, "Hello there, ${player.displayName}.")
        }
        val imcando =
            QuestRequirements.isOnQuest(player, KNIGHTS_SWORD) ||
                QuestRequirements.isOnQuest(player, GIANT_DWARF)
        val option =
            menu(
                buildList {
                    if (!soaStarted) add("I'm in search of a quest." to 1)
                    if (imcando) add("What do you know about the Imcando dwarves?" to 4)
                    add("Do you have anything to trade?" to 2)
                    add("I'd better get going." to 3)
                }
            )
        when (option) {
            4 -> reldoImcando()
            1 -> {
                chatPlayer(quiz, "I'm in search of a quest.")
                chatNpc(neutral, "Hmm... I don't believe there are any here.")
                chatNpc(neutral, "Let me think actually...")
                chatNpc(
                    happy,
                    "Ah, yes. I think I have something, if you're definitely interested?",
                )
                // TODO(content): [quest:shield_of_arrav] start offer and the combat level warning
            }
            2 -> {
                chatPlayer(quiz, "Do you have anything to trade?")
                chatNpc(happy, "Only knowledge.")
                chatPlayer(quiz, "How much do you want for that then?")
                chatNpc(laugh, "No, sorry, that was just my little joke. I'm not the trading type.")
                chatPlayer(sad, "Ah well.")
            }
            3 -> {
                chatPlayer(neutral, "I'd better get going.")
                chatNpc(happy, "Until next time.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Reldo
    // (during The Knight's Sword or The Giant Dwarf)
    // Mesanims and the stage change: live capture, rev 241, 20261012T145648-df49f48@5781-5786.
    private suspend fun Dialogue.reldoImcando() {
        chatPlayer(quiz, "What do you know about the Imcando dwarves?")
        chatNpc(neutral, "The Imcando dwarves, you say? Let's see...")
        chatNpc(
            neutral,
            "Ah, yes... For many hundreds of years, they were the world's most skilled smiths. " +
                "They used secret smithing knowledge passed down from generation to generation.",
        )
        chatNpc(
            neutral,
            "Unfortunately, about a century ago, the once thriving group was wiped out during " +
                "the barbarian invasions of that time.",
        )
        chatNpc(
            neutral,
            "I believe a few of them survived, but with the bulk of their population destroyed, " +
                "their numbers dwindled even further. If any are left now, they'll no doubt keep " +
                "to themselves.",
        )
        chatNpc(
            shifty,
            "However, I have heard tales of their love for redberry pie. Perhaps one of those " +
                "could convince one to reveal their true heritage.",
        )
        chatPlayer(quiz, "So I just go around offering random dwarves redberry pie?")
        with(knightsSword) { access.learnAboutImcando() }
        chatNpc(
            neutral,
            "Well, not totally random. Apparently some of the surviving Imcando made a home for " +
                "themselves near the cliffs on the Asgarnian southern peninsula. That would be a " +
                "good place to start.",
        )
    }

    private companion object {
        const val RELDO_MET = "varbit.reldo_met"
        const val SHIELD_OF_ARRAV = "quest_shieldofarrav"
        const val KNIGHTS_SWORD = "quest_knightssword"
        const val GIANT_DWARF = "quest_giantdwarf"
    }
}
