package org.rsmod.content.areas.city.rellekka.miscellania.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.areas.city.rellekka.fremennikName
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:throne_of_miscellania] "Your Royal Highness" variants after the quest
// TODO(content): [system:managing_miscellania] approval percentage after Throne of Miscellania
// and "The storerooms are full" lines
class MiscellaniaTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        for (subject in SUBJECTS) {
            onOpNpc1(subject) { startDialogue(it.npc) { subject() } }
        }
        onOpNpc1("npc.misc_smithy") { startDialogue(it.npc) { derrik() } }
        onOpNpc1("npc.misc_fisherman") { startDialogue(it.npc) { fishermanFrodi() } }
        onOpNpc1("npc.misc_lumberjack") { startDialogue(it.npc) { lumberjackLeif() } }
        onOpNpc1("npc.misc_miner") { startDialogue(it.npc) { minerMagnus() } }
        for (worker in WORKERS) {
            onOpNpc3(worker) { startDialogue(it.npc) { checkApproval() } }
        }
        onOpNpc1("npc.misc_ulby_doorguard") { startDialogue(it.npc) { throneRoomGuard() } }
        onOpNpc1("npc.royal_misc_guard") { startDialogue(it.npc) { dungeonGuard() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Alrik (same for Broddi, Einar,
    // Halla (Miscellania), Ragnar (Miscellania), Ragnvald, Rannveig, Thora (Miscellania),
    // Thorhild, Valgerd and Skraeling (Miscellania))
    private suspend fun Dialogue.subject() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Good day.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Derrik
    // TODO(content): [quest:throne_of_miscellania] giant nib for King Vargas's pen
    private suspend fun Dialogue.derrik() {
        val title = if (access.isBodyTypeA()) "Sir" else "Madam"
        chatNpc(happy, "Good day, $title. Can I help you with anything?")
        when (choice2("Can I use your anvil?", 1, "Nothing, thanks.", 2)) {
            1 -> {
                chatPlayer(quiz, "Can I use your anvil?")
                chatNpc(happy, "You may.")
            }
            2 -> chatPlayer(neutral, "Nothing, thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fisherman_Frodi
    // TODO(content): [verify:in_game] the transcript only has the after-quest greeting; check
    // whether Frodi says "Hello." before Throne of Miscellania like the other workers
    private suspend fun Dialogue.fishermanFrodi() {
        chatNpc(happy, "Good day, Your Royal Highness.")
        chatPlayer(quiz, "Good day. What are you doing?")
        chatNpc(neutral, "You know very well what I'm doing, Your Royal Highness.")
        chatPlayer(laugh, "Just teasing!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lumberjack_Leif
    private suspend fun Dialogue.lumberjackLeif() {
        chatNpc(happy, "Hello.")
        chatPlayer(quiz, "Good day. What are you doing?")
        chatNpc(neutral, "I'm cutting down maple trees for their wood. It's a living.")
        chatPlayer(happy, "Good luck with that.")
        chatNpc(happy, "Thanks.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Miner_Magnus
    // TODO(content): [system:treasure_trails] hard challenge scroll
    private suspend fun Dialogue.minerMagnus() {
        chatNpc(happy, "Hello.")
        chatPlayer(quiz, "Good day. What are you doing?")
        chatNpc(neutral, "I'm mining coal. It's pretty tough going.")
        chatPlayer(happy, "Good luck with that.")
        chatNpc(happy, "Thanks.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lumberjack_Leif (Check-Approval)
    // TODO(content): [verify:in_game] the transcripts of Frodi, Gunnhild and Magnus only give
    // the after-quest reply; Leif's "Who are you again?" is used for them too
    private suspend fun Dialogue.checkApproval() {
        chatNpc(confused, "Who are you again?")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Throne_of_Miscellania (Guard at the door)
    // TODO(content): [quest:heroes_quest] "I am a member of the Heroes' Guild; does that not
    // show my worth?" and "Then you may pass." for Heroes' Guild members
    // TODO(content): [quest:throne_of_miscellania] "Welcome back, Sir/Madam." once
    // varbit.misc_grantedaudience is set
    private suspend fun Dialogue.throneRoomGuard() {
        chatNpc(angry, "Halt! Who goes there?")
        chatPlayer(quiz, "My name is ${fremennikName()}, will you not let me through?")
        chatNpc(
            neutral,
            "I'm afraid the King won't give an audience to anyone except a worthy hero.",
        )
        chatNpc(
            neutral,
            "If you become a member of the Heroes' Guild, he will definitely grant you an " +
                "audience.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Guard_(Miscellania)
    // TODO(content): [quest:royal_trouble] "You're free to go into the dungeons, your Highness."
    // after the quest
    private suspend fun Dialogue.dungeonGuard() {
        chatNpc(neutral, "Sorry, but nobody's allowed down there by order of the King.")
        chatPlayer(neutral, "Oh, okay.")
    }

    private companion object {
        val SUBJECTS =
            listOf(
                "npc.misc_man_1",
                "npc.misc_man_2",
                "npc.misc_man_3",
                "npc.misc_man_4",
                "npc.misc_man_5",
                "npc.misc_man_6",
                "npc.misc_man_7",
                "npc.misc_woman_1",
                "npc.misc_woman_2",
                "npc.misc_woman_4",
                "npc.misc_woman_5",
                "npc.misc_woman_6",
            )
        val WORKERS =
            listOf(
                "npc.misc_fisherman",
                "npc.misc_gardener",
                "npc.misc_lumberjack",
                "npc.misc_miner",
            )
    }
}
