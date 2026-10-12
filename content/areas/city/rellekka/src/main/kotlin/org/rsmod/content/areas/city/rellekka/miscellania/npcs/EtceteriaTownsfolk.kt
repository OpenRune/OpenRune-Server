package org.rsmod.content.areas.city.rellekka.miscellania.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.areas.city.rellekka.fremennikName
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:managing_miscellania] approval percentage after Royal Trouble and
// "The storerooms are full" lines
class EtceteriaTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.misc_queen_sigrid") { startDialogue(it.npc) { queenSigrid() } }
        for ((etceterian, belongings) in ETCETERIANS) {
            onOpNpc1(etceterian) { startDialogue(it.npc) { etceterian(belongings) } }
        }
        onOpNpc1("npc.misc_etc_carpenter") { startDialogue(it.npc) { carpenterKjallak() } }
        onOpNpc1("npc.misc_etc_farmer") { startDialogue(it.npc) { farmerFromund() } }
        for (worker in ETCETERIANS.keys + KINGDOM_WORKERS) {
            onOpNpc3(worker) { startDialogue(it.npc) { checkApproval() } }
        }
        onOpNpc1("npc.etc_guard1") { startDialogue(it.npc) { skraeling() } }
        onOpNpc1("npc.etc_guard2") { startDialogue(it.npc) { skraeling() } }
        onOpNpc1("npc.royal_etc_guard") { startDialogue(it.npc) { dungeonGuard() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Queen_Sigrid
    // TODO(content): [quest:throne_of_miscellania] in-quest dialogue
    // TODO(content): [quest:royal_trouble] in-quest dialogue
    private suspend fun Dialogue.queenSigrid() {
        if (QuestRequirements.isOnQuest(player, THRONE_OF_MISCELLANIA) ||
            QuestRequirements.isOnQuest(player, ROYAL_TROUBLE)) {
            return
        }
        if (QuestRequirements.hasCompleted(player, THRONE_OF_MISCELLANIA)) {
            queenSigridBeforeRoyalTrouble()
            return
        }
        chatNpc(neutral, "Hello.")
        chatPlayer(worried, "Er...Hello, Your Majesty.")
        chatNpc(
            neutral,
            "Have you come for a particular reason? I am a busy Queen, you know. I had to " +
                "attend the opening of a new orphanage this morning.",
        )
        chatPlayer(neutral, "Yes, I...")
        chatNpc(
            happy,
            "And this afternoon I shall be receiving the Embassador of Karamja and " +
                "accompanying him on his week-long tour of Etceteria.",
        )
        chatPlayer(confused, "Er, I...")
        chatNpc(
            happy,
            "Then this evening we shall be putting on a grand ball! All of the kingdom's " +
                "nobility will be present!",
        )
        chatPlayer(neutral, "If I m...")
        chatNpc(
            sad,
            "Oh, who am I kidding...nothing ever happens here. I always wanted to be a princess " +
                "when I was a girl. If I'd been told it would be like this, I wouldn't have " +
                "bothered.",
        )
        chatPlayer(shocked, "Your Majesty!")
        chatNpc(quiz, "Yes? Did you want something?")
        chatPlayer(neutral, "...actually, no.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Queen_Sigrid (before starting Royal Trouble)
    // TODO(content): [verify:in_game] the wiki has no dialogue after Royal Trouble; this one is
    //  used then too
    private suspend fun Dialogue.queenSigridBeforeRoyalTrouble() {
        chatPlayer(neutral, "Your Majesty...")
        chatNpc(angry, "Vargas, you irritating fool. 'Warmongering harpy', indeed!")
        chatPlayer(confused, "Sorry?")
        chatNpc(neutral, "Oh, I apologise. I was merely thinking out loud.")
        chatPlayer(quiz, "How is your kingdom faring?")
        chatNpc(neutral, "It could be better. Now, if you don't mind, I'm rather busy.")
        chatPlayer(neutral, "Very well, your Majesty.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Haming (same for Arnor, Ashild, Helga,
    // Matilda and Moldof)
    private suspend fun Dialogue.etceterian(belongings: String) {
        chatPlayer(neutral, "Excuse me...")
        chatNpc(neutral, "Can I help you?")
        if (QuestRequirements.hasCompleted(player, ROYAL_TROUBLE)) {
            chatNpc(
                happy,
                "Thank you for finding my $belongings for me. Did you find everyone's belongings?",
            )
            chatPlayer(happy, "I did! Thank you for the reward.")
            chatNpc(happy, "You're more than welcome to it, for getting our belongings back.")
            return
        }
        when (access.random.of(5)) {
            0 -> {
                chatPlayer(quiz, "Can you tell me anything about the castle?")
                chatNpc(neutral, "There aren't many guards in it.")
                chatNpc(neutral, "We don't have an army, you see.")
            }
            1 -> {
                chatPlayer(quiz, "How's the weather doing?")
                chatNpc(neutral, "Could be better.")
            }
            2 -> {
                chatPlayer(quiz, "How's life in Etceteria?")
                chatNpc(sad, "*sigh* Times are tough.")
                chatNpc(neutral, "Though they're getting better.")
            }
            3 -> {
                chatPlayer(quiz, "What's it like being an Etceterian?")
                chatNpc(neutral, "Fairly pleasant, though it gets cold in the winter.")
            }
            else -> {
                chatPlayer(happy, "Nice town you have here.")
                chatNpc(happy, "Thank you.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Carpenter_Kjallak
    private suspend fun Dialogue.carpenterKjallak() {
        if (QuestRequirements.hasCompleted(player, ROYAL_TROUBLE)) {
            chatPlayer(quiz, "Good day. What are you doing?")
            chatNpc(
                neutral,
                "I'm cutting down mahogany and teak trees to make into furniture. It takes a " +
                    "while.",
            )
            chatPlayer(happy, "Good luck with that.")
            chatNpc(happy, "Thank you, Your Royal Highness.")
            return
        }
        chatNpc(
            angry,
            "Hey! This is the Queen's forest. You'll need her approval to make use of it.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Farmer_Fromund
    // TODO(content): [verify:in_game] the transcript has one standard dialogue; check whether it
    // differs before Royal Trouble
    private suspend fun Dialogue.farmerFromund() {
        chatPlayer(quiz, "Good day. What are you doing?")
        chatNpc(
            neutral,
            "I'm tending to these farming patches. They need a little more work before I can " +
                "plant anything.",
        )
        when (choice2("Good luck with that.", 1, "What's wrong with the patches?", 2)) {
            1 -> {
                chatPlayer(happy, "Good luck with that.")
                chatNpc(happy, "Thank you, ${fremennikName()}.")
            }
            2 -> {
                chatPlayer(quiz, "What's wrong with the patches?")
                chatNpc(
                    neutral,
                    "The centre patch is full of weeds, and as soon as I clear them they grow " +
                        "back. The south patch is always far too dry. And the soil in the north " +
                        "patch is terrible; it needs a good dose of compost.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Haming (Check-Approval before Royal Trouble)
    // TODO(content): [verify:in_game] Arnor's, Ashild's, Helga's, Moldof's and Kjallak's
    // transcripts only give the after-quest reply; Haming's is used for them too
    private suspend fun Dialogue.checkApproval() {
        chatPlayer(quiz, "Can you tell me how the kingdom approves of me?")
        chatNpc(angry, "Kingdom? Etceteria is ruled by a Queen and we don't approve of you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Skraeling_(Etceteria)
    private suspend fun Dialogue.skraeling() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Good day.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Guard_(Etceteria)
    private suspend fun Dialogue.dungeonGuard() {
        if (QuestRequirements.hasCompleted(player, ROYAL_TROUBLE)) {
            chatNpc(
                neutral,
                "I'm surprised you managed to get out of there without breaking any bones.",
            )
            chatNpc(
                neutral,
                "Still, the Queen says it should be safe, and I'm to let you go back down if you " +
                    "want.",
            )
            return
        }
        chatPlayer(quiz, "What are you guarding here?")
        chatNpc(
            neutral,
            "The area under the palace is going to be excavated soon, and I'm guarding the " +
                "entrance to it.",
        )
        chatPlayer(quiz, "Oh, I see. Can I go down there?")
        chatNpc(angry, "Of course not!")
        chatNpc(
            neutral,
            "There's not even a rope to let you down there. You'd break both your legs if you " +
                "tried it.",
        )
    }

    private companion object {
        const val THRONE_OF_MISCELLANIA = "quest_throneofmiscellania"
        const val ROYAL_TROUBLE = "quest_royaltrouble"

        val ETCETERIANS =
            mapOf(
                "npc.misc_etc_man_1" to "sword",
                "npc.misc_etc_man_2" to "plates",
                "npc.misc_etc_man_3" to "arrows",
                "npc.misc_etc_woman_1" to "shield",
                "npc.misc_etc_woman_2" to "hat",
                "npc.misc_etc_woman_3" to "tools",
            )
        val KINGDOM_WORKERS = listOf("npc.misc_etc_carpenter", "npc.misc_etc_farmer")
    }
}
