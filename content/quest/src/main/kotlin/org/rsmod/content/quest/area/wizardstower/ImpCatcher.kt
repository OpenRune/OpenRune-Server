package org.rsmod.content.quest.area.wizardstower

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.menu
import org.rsmod.content.quest.manager.rewards
import org.rsmod.content.quest.manager.startQuestPrompt
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class ImpCatcher :
    QuestScript(
        "quest_impcatcher",
        "varp.imp",
        rewards {
            xp("stat.magic", 875.0)
            extra("An Amulet of Accuracy")
        },
        ItemRewardDisplay(AMULET),
    ) {

    override fun ScriptContext.init() {
        onOpNpc1("npc.wizard_mizgog") { startDialogue(it.npc) { mizgog() } }
        onOpNpc3("npc.wizard_mizgog") { startDialogue(it.npc) { purchaseAmulet() } }
        onOpNpc1("npc.wizard_grayzag") { startDialogue(it.npc) { grayzag() } }
    }

    private suspend fun Dialogue.grayzag() {
        when {
            quest.isQuestCompleted(player) -> {
                chatNpc(angry, "So you think finding those beads makes you clever, do you?")
                chatPlayer(happy, "Well yes, actually.")
                chatNpc(
                    angry,
                    "Well you'd better just watch your back, because when you least expect it " +
                        "I'll be there. You shouldn't go sticking your nose into other people's " +
                        "affairs, meddler.",
                )
            }
            quest.isQuestInProgress(player) ->
                chatNpc(
                    laugh,
                    "You're a fool, ${player.displayName}. Do you really think you'll find four " +
                        "imps out of thousands? Good luck. Ha!",
                )
            else -> chatNpc(angry, "Not now, I'm trying to concentrate on a very difficult spell!")
        }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Wizard Mizgog</col> on the top floor of the " +
            "<col=800000>Wizards' Tower</col>."

    override fun questLog(player: ProtectedAccess): String =
        questJournal(player) {
            line("<red>Wizard Mizgog</red> has asked me to recover his four magical beads from the imps that stole them.")
            line("He needs a <red>red</red>, <red>yellow</red>, <red>black</red> and <red>white</red> bead.")
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line("I recovered Wizard Mizgog's four magical beads from the imps and he gave me an Amulet of Accuracy.")
        }

    private fun beadCount(player: Player): Int = BEADS.count { player.inv.count(it) > 0 }

    private fun hasAllBeads(player: Player): Boolean = beadCount(player) == BEADS.size

    private suspend fun Dialogue.mizgog() {
        when {
            quest.isQuestCompleted(player) -> afterQuest()
            quest.isQuestInProgress(player) -> duringQuest()
            else -> beforeQuest()
        }
    }

    private suspend fun Dialogue.beforeQuest() {
        chatPlayer(neutral, "Give me a quest!")
        chatNpc(happy, "Give me a quest what?")
        when (
            menu(
                "Give me a quest please." to 1,
                "Give me a quest or else!" to 2,
                "Just stop messing around and give me a quest!" to 3,
            )
        ) {
            1 -> offerQuest()
            2 -> {
                chatPlayer(angry, "Give me a quest or else!")
                chatNpc(happy, "Or else what? You'll attack me?")
                chatNpc(laugh, "Hahaha!")
            }
            3 -> {
                chatPlayer(angry, "Just stop messing around and give me a quest!")
                chatNpc(happy, "Ah now you're assuming I have one to give.")
            }
        }
    }

    private suspend fun Dialogue.offerQuest() {
        chatPlayer(neutral, "Give me a quest please.")
        chatNpc(happy, "Well seeing as you asked nicely... I could do with some help.")
        chatNpc(
            sad,
            "The wizard Grayzag next door decided he didn't like me so he enlisted an army of " +
                "hundreds of imps.",
        )
        chatNpc(
            sad,
            "These imps stole all sorts of my things. Most of these things I don't really care " +
                "about, just eggs and balls of string and things.",
        )
        chatNpc(
            sad,
            "But they stole my four magical beads. There was a red one, a yellow one, a black " +
                "one, and a white one.",
        )
        chatNpc(
            sad,
            "These imps have now spread out all over the kingdom. Could you get my beads back " +
                "for me?",
        )
        if (!startQuestPrompt(quest)) {
            chatPlayer(neutral, "I've better things to do than chase imps.")
            chatNpc(
                angry,
                "Well if you're not interested in the quests I have to give you, don't waste my " +
                    "time by asking me for them.",
            )
            return
        }
        quest.advanceQuestStageTo(access, STARTED)
        if (!hasAllBeads(player)) {
            chatPlayer(neutral, "I'll try.")
            chatNpc(happy, "That's great, thank you.")
            return
        }
        chatPlayer(shocked, "Well this is a surprising turn of events!")
        chatNpc(happy, "What?")
        chatPlayer(happy, "Well I just so happen to have all of those beads on me!")
        chatNpc(
            angry,
            "Are you saying that you stole my beads all this time and I've been blaming these " +
                "imps!?",
        )
        chatPlayer(
            worried,
            "No, not at all! I just found them throughout my travels and presumed somebody would " +
                "need them at some point.",
        )
        chatNpc(angry, "Bah! Fine.")
        handInBeads()
    }

    private suspend fun Dialogue.duringQuest() {
        chatNpc(neutral, "So how are you doing finding my beads?")
        when {
            hasAllBeads(player) -> {
                chatPlayer(happy, "I've got all four beads. It was hard work I can tell you.")
                handInBeads()
            }
            beadCount(player) > 0 -> {
                chatPlayer(neutral, "I have found some of your beads.")
                chatNpc(
                    neutral,
                    "Come back when you have them all. The colour of the four beads that I need " +
                        "are red, yellow, black, and white. Go chase some imps!",
                )
            }
            else -> {
                chatPlayer(sad, "I've not found any yet.")
                chatNpc(
                    angry,
                    "Well get on with it. I've lost a white bead, a red bead, a black bead, and a " +
                        "yellow bead. Go kill some imps!",
                )
            }
        }
    }

    private suspend fun Dialogue.handInBeads() {
        chatNpc(
            happy,
            "Give them here and I'll check that they really are MY beads, before I give you your " +
                "reward. You'll like it, it's an amulet of accuracy.",
        )
        for (bead in BEADS) {
            access.invDel(access.inv, bead)
        }
        mesbox("You give four coloured beads to Wizard Mizgog.")
        access.camLookAt(CAMERA_LOOK, height = 350, rate = 232, rate2 = 100)
        access.camMoveTo(CAMERA_POSITION, height = 775, rate = 232, rate2 = 100)
        access.delay(16)
        access.invAdd(access.inv, AMULET)
        access.mes("The wizard hands you an amulet.")
        access.camReset()
        quest.advanceQuestStageTo(access, COMPLETE)
    }

    private suspend fun Dialogue.afterQuest() {
        when (
            menu(
                "Got any more quests?" to 1,
                "Do you know any interesting spells you could teach me?" to 2,
                "Have you got another one of those fancy schmancy amulets?" to 3,
            )
        ) {
            1 -> {
                chatPlayer(neutral, "Got any more quests?")
                chatNpc(neutral, "No, everything is good with the world today.")
            }
            2 -> {
                chatPlayer(neutral, "Do you know any interesting spells you could teach me?")
                chatNpc(
                    laugh,
                    "I don't think so, the type of magic I study involves years of meditation " +
                        "and research.",
                )
            }
            3 -> anotherAmulet()
        }
    }

    private suspend fun Dialogue.anotherAmulet() {
        chatPlayer(quiz, "Have you got another one of those fancy schmancy amulets?")
        chatNpc(
            neutral,
            "I have a few spare. I'd like one of each coloured bead again in return, though! " +
                "Black, white, yellow and red.",
        )
        if (!hasAllBeads(player)) {
            chatPlayer(
                quiz,
                "I don't have them all on me at the moment. I'll come back when I have them!",
            )
            chatNpc(neutral, "Very well. See you soon!")
            return
        }
        val trade = menu("I have them with me!" to true, "Maybe later." to false)
        if (!trade) {
            chatPlayer(neutral, "Maybe later.")
            chatNpc(neutral, "Perhaps some other time, then.")
            return
        }
        chatPlayer(happy, "I have them with me! Here you go.")
        exchangeBeads()
        chatPlayer(happy, "Thanks, Mizgog!")
        chatPlayer(quiz, "What are you going to do with all of these extra beads, anyway?")
        chatNpc(shifty, "You don't want to know. Take care!")
    }

    private suspend fun Dialogue.purchaseAmulet() {
        if (!quest.isQuestCompleted(player)) {
            mizgog()
            return
        }
        if (!hasAllBeads(player)) {
            chatNpc(
                quiz,
                "You don't seem to have one of each coloured bead on you at the moment. Return " +
                    "when you have a black, white, yellow and red in your backpack!",
            )
            return
        }
        exchangeBeads()
    }

    private suspend fun Dialogue.exchangeBeads() {
        for (bead in BEADS) {
            access.invDel(access.inv, bead)
        }
        access.invAdd(access.inv, AMULET)
        objbox(
            AMULET,
            "Mizgog removes the beads from your backpack and gives you another Amulet of accuracy.",
        )
    }

    private companion object {
        const val STARTED = 1
        const val COMPLETE = 2
        const val AMULET = "obj.amulet_of_accuracy"
        val BEADS = listOf("obj.red_bead", "obj.yellow_bead", "obj.black_bead", "obj.white_bead")
        val CAMERA_LOOK = CoordGrid(3103, 3162, 2)
        val CAMERA_POSITION = CoordGrid(3103, 3161, 2)
    }
}
