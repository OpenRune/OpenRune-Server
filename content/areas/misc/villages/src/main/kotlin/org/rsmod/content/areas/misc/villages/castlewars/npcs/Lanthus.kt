package org.rsmod.content.areas.misc.villages.castlewars.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Lanthus
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:castle_wars] ticket exchange (Trade op and "What do you have for
//  trade?"), the Disable-XP op and bracelet charges
// TODO(content): [quest:animal_magnetism] "Take a look at this dead chicken I've got."
class Lanthus @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.castlewars_judge") { startDialogue(it.npc) { lanthus() } }
        onOpNpc1("npc.castlewars_judge_model") { startDialogue(it.npc) { lanthus() } }
    }

    private suspend fun Dialogue.lanthus() {
        chatNpc(happy, "Good day, how may I help you?")
        val option =
            choice3(
                "What is this place?",
                1,
                "What do you have for trade?",
                2,
                "Do you have a manual? I'd like to learn how to play!",
                3,
            )
        when (option) {
            1 -> whatIsThisPlace()
            2 -> return
            3 -> {
                chatPlayer(quiz, "Do you have a manual? I'd like to learn how to play!")
                chatNpc(happy, "Sure, here you go.")
                player.invAddOrDrop(objRepo, MANUAL)
            }
        }
    }

    private suspend fun Dialogue.whatIsThisPlace() {
        chatPlayer(quiz, "What is this place?")
        chatNpc(
            happy,
            "This is the great Castle Wars arena! Here you can fight for the glory of Saradomin or " +
                "Zamorak.",
        )
        val option =
            choice3(
                "Really, how do I do that?",
                1,
                "Are there any rules?",
                2,
                "What can I win?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Really, how do I do that?")
                chatNpc(
                    neutral,
                    "Easy, you just step through one of the three portals. To join Zamorak, pass " +
                        "through the red portal. To join Saradomin, pass through the blue portal. " +
                        "If you don't mind then pass through the green portal.",
                )
            }
            2 -> {
                chatPlayer(quiz, "Are there any rules?")
                chatNpc(
                    neutral,
                    "Of course, there are always rules. Firstly you can't wear a cape as you " +
                        "enter the portal, you'll be given your team colours to wear while in the " +
                        "arena. You're also prohibited from taking non-combat related items in " +
                        "with you. So you should only have equipment, potions, and runes on you. " +
                        "Secondly, attacking your own team or your team's defences isn't " +
                        "allowed. You don't want to be angering your patron god, do you? Other " +
                        "than that, just have fun and enjoy it!",
                )
                chatPlayer(happy, "Great! Oh, how do I win the game?")
                chatNpc(
                    neutral,
                    "The aim is to get into your opponents' castle and take their team standard. " +
                        "Then bring that back and capture it on your team's standard.",
                )
            }
            3 -> {
                chatPlayer(quiz, "What can I win?")
                chatNpc(
                    happy,
                    "Players on the winning team will receive 2 Castle Wars Tickets which you can " +
                        "trade back to me for other items. In the event of a draw every player " +
                        "will get 1 ticket.",
                )
            }
        }
    }

    private companion object {
        // TODO(content): [verify:in_game] whether Lanthus checks for space before the manual
        const val MANUAL = "obj.castlewars_manual"
    }
}
