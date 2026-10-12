package org.rsmod.content.areas.city.taverley.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class TaverleyTownsfolk @Inject constructor(private val objRepo: ObjRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.ballboy") { startDialogue(it.npc) { boy() } }
        onOpNpc1("npc.eadgar_druid_washing") { startDialogue(it.npc) { tegid() } }
        onOpNpc1("npc.ladyofthelake") { startDialogue(it.npc) { ladyOfTheLake() } }
        onOpNpc1("npc.grim_sylas") { startDialogue(it.npc) { sylas() } }
        onOpNpc1("npc.zep_assist_tav") { startDialogue(it.npc) { assistantStan() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Boy
    // TODO(content): [quest:witchs_house] "Start the Witch's House quest?" offer, start and
    //  in-progress dialogue
    private suspend fun Dialogue.boy() {
        if (QuestRequirements.hasCompleted(player, WITCHS_HOUSE)) {
            chatNpc(happy, "Thank you for getting my ball back!")
            return
        }
        chatPlayer(happy, "Hello young man.")
        mesbox("The boy sobs.")
        val option =
            choice2(
                "What's the matter?",
                1,
                "Well if you're not going to answer, I'll go.",
                2,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What's the matter?")
                chatNpc(
                    sad,
                    "I've kicked my ball over that hedge, into that garden! The old lady who " +
                        "lives there is scary... She's locked the ball in her wooden shed! Can " +
                        "you get my ball back for me please?",
                )
            }
            2 -> {
                chatPlayer(neutral, "Well if you're not going to answer, I'll go.")
                mesbox("The boy sniffs slightly.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Tegid
    private suspend fun Dialogue.tegid() {
        chatPlayer(happy, "So, you're doing laundry, eh?")
        chatNpc(angry, "Yes. What is it to you?")
        chatPlayer(happy, "Nice day for it.")
        chatNpc(neutral, "I suppose it is.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:The_Lady_of_the_Lake
    // TODO(content): [system:treasure_trails] easy clue reward
    private suspend fun Dialogue.ladyOfTheLake() {
        val title = if (access.isBodyTypeA()) "sir" else "madam"
        chatNpc(neutral, "Good day to you $title.")
        val excalibur =
            QuestRequirements.hasCompleted(player, MERLINS_CRYSTAL) &&
                !playerContainsObj(EXCALIBUR)
        val option =
            if (excalibur) {
                choice3("Who are you?", 1, "Good day.", 2, "I seek the sword Excalibur.", 3)
            } else {
                choice2("Who are you?", 1, "Good day.", 2)
            }
        when (option) {
            1 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(neutral, "I am the Lady of the Lake.")
            }
            2 -> chatPlayer(neutral, "Good day.")
            3 -> ladyOfTheLakeExcalibur()
        }
    }

    // TODO(content): [verify:in_game] the wiki says nothing about a full inventory; this drops
    //  Excalibur
    private suspend fun Dialogue.ladyOfTheLakeExcalibur() {
        chatPlayer(quiz, "I seek the sword Excalibur.")
        chatNpc(neutral, "Aye, I have that artefact in my possession.")
        chatNpc(neutral, "'Tis very valuable, and not an artefact to be given away lightly.")
        chatNpc(
            neutral,
            "...But you have already proved thyself to be worthy of wielding it once already. I " +
                "shall return it to you if you can prove yourself to still be worthy.",
        )
        chatPlayer(quiz, "...And how can I do that?")
        chatNpc(neutral, "Why, by proving yourself to be above material goods.")
        chatNpc(neutral, "500 gold coins ought to do it.")
        if (access.inv.count(COINS) < EXCALIBUR_PRICE) {
            chatPlayer(sad, "I don't have that kind of money...")
            chatNpc(neutral, "Well, come back when you do.")
            return
        }
        chatPlayer(neutral, "Ok, here you go.")
        if (!player.invTakeFee(EXCALIBUR_PRICE)) {
            return
        }
        player.invAddOrDrop(objRepo, EXCALIBUR)
        chatNpc(
            happy,
            "You are still worthy to wield Excalibur! And thanks for the cash! I felt like " +
                "getting a new haircut!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sylas
    // https://oldschool.runescape.wiki/w/Transcript:Grim_Tales (without the requirements)
    // TODO(content): [quest:grim_tales] quest start and in-progress dialogue
    private suspend fun Dialogue.sylas() {
        if (QuestRequirements.hasCompleted(player, GRIM_TALES)) {
            sylasAfterQuest()
            return
        }
        chatPlayer(happy, "Hello there. My name's ${player.displayName}.")
        chatNpc(
            neutral,
            "Ahhh, good day young adventurer. I can't stop to talk right now I'm afraid.",
        )
        mesbox("You do not meet all of the requirements to start the Grim Tales quest.")
    }

    private suspend fun Dialogue.sylasAfterQuest() {
        chatPlayer(happy, "Hello Sylas.")
        chatNpc(happy, "Ah, ${player.displayName}! Good to see you again.")
        chatPlayer(quiz, "How is the collection going now?")
        chatNpc(
            happy,
            "Thanks to you finding the golden goblin for me, I am the envy of the collector's " +
                "group. I can't thank you enough.",
        )
        if (access.inv.count(DWARVEN_HELMET) > 0) {
            return
        }
        chatPlayer(sad, "I think I've lost Rupert's helmet.")
        chatNpc(
            neutral,
            "Not to worry, ${player.displayName} Go and speak to Rupert. These items have a " +
                "strange way of returning to their owners sometimes!",
        )
        chatPlayer(happy, "Ok, I will. Thanks.")
        chatNpc(happy, "No, no. Thanks for everything you've done!")
        chatPlayer(happy, "I'm just happy that I could help. Goodbye!")
        chatNpc(happy, "Goodbye.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Assistant_Stan
    // TODO(content): [system:balloon_transport] "Yes." opens the balloon map (log checks and
    //  the Entrana weapon search)
    private suspend fun Dialogue.assistantStan() {
        chatNpc(
            neutral,
            "Do you want to use the balloon? Just so you know, some locations require special " +
                "logs and high Firemaking skills.",
        )
        when (choice3("Yes.", 1, "No.", 2, "Who are you?", 3)) {
            1 -> Unit
            2 -> chatPlayer(neutral, "No.")
            3 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(
                    neutral,
                    "I am Stan. Auguste hired me to look after this balloon. I make sure people " +
                        "are prepared to fly.",
                )
                chatNpc(neutral, "Do you want to use the balloon?")
                when (choice2("Yes.", 1, "No.", 2)) {
                    1 -> Unit
                    2 -> chatPlayer(neutral, "No.")
                }
            }
        }
    }

    private companion object {
        const val WITCHS_HOUSE = "quest_witchshouse"
        const val MERLINS_CRYSTAL = "quest_merlinscrystal"
        const val GRIM_TALES = "quest_grimtales"

        const val COINS = "obj.coins"
        const val EXCALIBUR = "obj.excalibur"
        const val DWARVEN_HELMET = "obj.grim_wear_helmet"

        // https://oldschool.runescape.wiki/w/Transcript:The_Lady_of_the_Lake: 500 gold coins
        const val EXCALIBUR_PRICE = 500
    }
}
