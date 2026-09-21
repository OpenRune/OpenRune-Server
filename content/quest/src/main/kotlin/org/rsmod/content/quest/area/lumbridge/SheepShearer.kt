package org.rsmod.content.quest.area.lumbridge

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.menu
import org.rsmod.content.quest.manager.rewards
import org.rsmod.content.quest.manager.startQuestPrompt
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.ScriptContext

class SheepShearer :
    QuestScript(
        "quest_sheepshearer",
        "varp.sheep",
        rewards {
            xp("stat.crafting", 150.0)
            item("obj.coins", 60)
        },
        ItemRewardDisplay("obj.shears"),
    ) {

    override fun ScriptContext.init() {
        onOpNpc1("npc.fred_the_farmer") { startDialogue(it.npc) { fred(it.npc) } }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Fred the Farmer</col> at his farm north-west of " +
            "<col=800000>Lumbridge</col>."

    override fun questLog(player: ProtectedAccess): String =
        questJournal(player) {
            line("I asked Farmer Fred, near Lumbridge, for a quest. Fred said he'd pay me for shearing his sheep for him!")
            line("Fred gave me some shears to use, to get the job done.")
            line("Farmer Fred said there was a spinning wheel in Lumbridge castle I could use to make the balls of wool.")
            line("I need to collect ${ballsNeeded(access)} more balls of wool.")
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line("I brought Farmer Fred 20 balls of wool, and he paid me for it.")
        }

    private fun ballsGiven(access: ProtectedAccess): Int =
        (quest.getQuestStage(access.player) - STARTED).coerceAtLeast(0)

    private fun ballsNeeded(access: ProtectedAccess): Int = REQUIRED_BALLS - ballsGiven(access)

    private suspend fun Dialogue.fred(npc: Npc) {
        when {
            quest.isQuestCompleted(player) -> fredAfterQuest()
            quest.isQuestInProgress(player) -> fredDuringQuest()
            else -> fredStart()
        }
    }

    private suspend fun Dialogue.fredStart() {
        chatNpc(
            angry,
            "What are you doing on my land? You're not the one who keeps leaving all my gates " +
                "open and letting out all my sheep, are you?",
        )
        when (
            menu(
                "I'm looking for a quest." to 1,
                "I'm looking for something to kill." to 2,
                "I'm lost." to 3,
            )
        ) {
            1 -> offerQuest()
            2 -> lookingToKill()
            3 -> lost()
        }
    }

    private suspend fun Dialogue.offerQuest() {
        chatPlayer(neutral, "I'm looking for a quest.")
        chatNpc(neutral, "You're after a quest, you say? Actually, I could do with a bit of help.")
        chatNpc(
            neutral,
            "My sheep are getting mighty woolly. I'd be much obliged if you could shear them. " +
                "And while you're at it, spin the wool for me too.",
        )
        chatNpc(
            happy,
            "Yes, that's it. Bring me 20 balls of wool. And I'm sure I could sort out some sort " +
                "of payment. Of course, there's the small matter of The Thing.",
        )
        if (player.inv.count(BALL_OF_WOOL) >= REQUIRED_BALLS) {
            alreadyHaveWool()
            return
        }
        chatPlayer(quiz, "What do you mean, The Thing?")
        chatNpc(
            shifty,
            "Well now, no one has ever seen The Thing. That's why we call it The Thing, 'cos we " +
                "don't know what it is.",
        )
        chatNpc(
            worried,
            "Some say it's a black hearted shapeshifter, hungering for the souls of hard working " +
                "decent folk like me. Others say it's just a sheep.",
        )
        chatNpc(
            angry,
            "Well I don't have all day to stand around and gossip. Are you going to shear my " +
                "sheep or what!",
        )
        if (!startQuestPrompt(quest)) {
            chatPlayer(neutral, "No, I'll give it a miss.")
            chatNpc(neutral, "Suit yourself.")
            return
        }
        chatPlayer(happy, "Yes, okay. I can do that.")
        quest.advanceQuestStageTo(access, STARTED)
        chatNpc(neutral, "Good! Now one more thing, do you actually know how to shear a sheep?")
        chatPlayer(neutral, "Err. No, I don't know actually.")
        explainShearing()
        chatNpc(quiz, "Do you know how to spin wool?")
        chatPlayer(neutral, "I don't know how to spin wool, sorry.")
        chatNpc(neutral, "Don't worry, it's quite simple!")
        explainSpinning()
    }

    private suspend fun Dialogue.alreadyHaveWool() {
        chatPlayer(
            happy,
            "In fact Fred, funnily enough, I actually have 20 balls of wool already on me.",
        )
        chatNpc(angry, "Have you been shearing my sheep without permission!?")
        chatPlayer(
            shifty,
            "No! Well, maybe... They just looked a little wooly! Surely you like a shave once in " +
                "a while, too?",
        )
        chatNpc(
            angry,
            "It's rude to shave another person without permission - don't be coming at me with " +
                "them shears!",
        )
        chatPlayer(sad, "I'm sorry, I'll ask permission next time.")
        chatNpc(
            neutral,
            "I guess no real 'arm was done. Hand the balls over and we can put this whole thing " +
                "behind us.",
        )
        quest.advanceQuestStageTo(access, STARTED)
        handInWool()
    }

    private suspend fun Dialogue.explainShearing() {
        if (player.inv.count(SHEARS) > 0) {
            chatNpc(
                happy,
                "Well, you're half way there already! You have a set of shears in your " +
                    "inventory. Just use those on a Sheep to shear it.",
            )
            chatPlayer(neutral, "That's all I have to do?")
            chatNpc(neutral, "Well once you've collected some wool you'll need to spin it into balls.")
            return
        }
        chatNpc(
            neutral,
            "Well, first things first, you need a pair of shears. I've got some here you can use.",
        )
        access.invAdd(access.inv, SHEARS)
        objbox(SHEARS, "Fred gives you a set of sharp shears.")
        chatNpc(neutral, "You just need to go and use them on the sheep out in my field.")
        chatPlayer(happy, "Sounds easy!")
        chatNpc(laugh, "That's what they all say!")
        chatNpc(neutral, "Some of the sheep don't like it too much... Persistence is the key.")
        chatNpc(neutral, "Once you've collected some wool you can spin it into balls.")
    }

    private suspend fun Dialogue.explainSpinning() {
        chatNpc(
            neutral,
            "The nearest Spinning Wheel can be found on the first floor of Lumbridge Castle.",
        )
        chatNpc(neutral, "To get to Lumbridge Castle just follow the road east.")
        objbox("obj.crafting_icon_dummy", "This icon denotes a Spinning Wheel on the world map.")
        chatPlayer(happy, "Thank you!")
    }

    private suspend fun Dialogue.fredDuringQuest() {
        chatNpc(angry, "What are you doing on my land?")
        chatPlayer(happy, "I need to talk to you about shearing these sheep!")
        chatNpc(neutral, "Oh. How are you doing getting those balls of wool?")
        if (player.inv.count(BALL_OF_WOOL) > 0) {
            chatPlayer(happy, "I have some.")
            chatNpc(neutral, "Give 'em here then.")
            handInWool()
            return
        }
        chatPlayer(quiz, "How many more do I need to give you?")
        chatNpc(neutral, "You need to collect ${ballsNeeded(access)} more balls of wool.")
        if (player.inv.count(WOOL) > 0) {
            chatPlayer(
                neutral,
                "I've got some wool. I've not managed to make it into a ball though.",
            )
            chatNpc(
                neutral,
                "Well go find a spinning wheel then. You can find one on the first floor of " +
                    "Lumbridge Castle, just walk east on the road outside my house and you'll " +
                    "find Lumbridge.",
            )
            return
        }
        chatPlayer(sad, "I haven't got any at the moment.")
        chatNpc(
            neutral,
            "Ah well at least you haven't been eaten. You know what you're doing, right?",
        )
        when (
            menu(
                "How do I shear sheep, again?" to 1,
                "Remind me how to spin wool." to 2,
                "Yeah, I think so." to 3,
            )
        ) {
            1 -> {
                chatPlayer(quiz, "How do I shear sheep, again?")
                explainShearing()
                chatNpc(quiz, "Do you know how to spin wool?")
                val knows =
                    menu(
                        "Yes, I know how to spin wool." to true,
                        "I don't know how to spin wool, sorry." to false,
                    )
                if (knows) {
                    chatPlayer(happy, "Yes, I know how to spin wool.")
                    chatNpc(happy, "Great!")
                } else {
                    chatPlayer(neutral, "I don't know how to spin wool, sorry.")
                    chatNpc(neutral, "Don't worry, it's quite simple!")
                    explainSpinning()
                }
            }
            2 -> {
                chatPlayer(quiz, "Remind me how to spin wool.")
                explainSpinning()
            }
            3 -> {
                chatPlayer(happy, "Yeah, I think so.")
                chatNpc(neutral, "You can get to it, then!")
            }
        }
    }

    private suspend fun Dialogue.handInWool() {
        val given = minOf(player.inv.count(BALL_OF_WOOL), ballsNeeded(access))
        if (given <= 0) return
        access.invDel(access.inv, BALL_OF_WOOL, given)
        val remaining = ballsNeeded(access) - given
        if (remaining > 0) {
            quest.advanceQuestStage(access, given)
            val noun = if (given == 1) "ball" else "balls"
            mesbox("You give Fred $given $noun of wool.")
            chatPlayer(neutral, "That's all I've got so far.")
            chatNpc(neutral, "I need $remaining more before I can pay you.")
            chatPlayer(neutral, "Ok I'll work on it.")
            return
        }
        chatPlayer(happy, "That's the last of them.")
        chatNpc(sad, "I guess I'd better pay you then.")
        quest.advanceQuestStage(access, given)
    }

    private suspend fun Dialogue.fredAfterQuest() {
        chatNpc(angry, "What are you doing on my land?")
        when (menu("I'm looking for something to kill." to 1, "I'm lost." to 2)) {
            1 -> lookingToKill()
            2 -> lost()
        }
    }

    private suspend fun Dialogue.lookingToKill() {
        chatPlayer(neutral, "I'm looking for something to kill.")
        chatNpc(angry, "What, on my land? Leave my livestock alone you scoundrel!")
    }

    private suspend fun Dialogue.lost() {
        chatPlayer(confused, "I'm lost.")
        chatNpc(
            neutral,
            "How can you be lost? Just follow the road east and south. You'll end up in " +
                "Lumbridge fairly quickly.",
        )
    }

    private companion object {
        const val STARTED = 1
        const val REQUIRED_BALLS = 20
        const val BALL_OF_WOOL = "obj.ball_of_wool"
        const val WOOL = "obj.wool"
        const val SHEARS = "obj.shears"
    }
}
