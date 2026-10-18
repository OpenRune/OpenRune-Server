package org.rsmod.content.quest.area.lumbridge

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.content.quest.manager.startQuestPrompt
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Sheep Shearer. Progress is varp 179 (`varp.sheep`), as in OSRS and as
 * Quest Helper reads it: 0 not started, 1 started, 1 + the balls of wool handed in so far, 21
 * complete. Shearing (`Sheep`), spinning (the crafting skill's skillmulti on the castle wheel) and
 * the castle stairs are generic content; this script is Fred, The Thing and the hand-in.
 */
class SheepShearer @Inject constructor(private val objRepo: ObjRepository) :
    QuestScript(
        "quest_sheepshearer",
        "varp.sheep",
        rewards {
            xp("stat.crafting", 150.0)
            item("obj.coins", 60, label = "60 Coins")
        },
        ItemRewardDisplay("obj.ball_of_wool", zoom = 200),
        questVarbit = "varbit.sheep_shearer_progress",
    ) {

    private var Player.sawTheThing by boolVarBit("varbit.sheep_shearer_saw_thing")

    override fun ScriptContext.init() {
        check(quest.maxSteps == COMPLETE_STAGE) {
            "Sheep Shearer end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the last ball of wool completes at $COMPLETE_STAGE."
        }

        onOpNpc1("npc.fred_the_farmer") { startDialogue(it.npc) { fredDialogue() } }
        onOpNpc1("npc.sheep_shearer_the_thing") { shearTheThing() }
        onOpNpc3("npc.sheep_shearer_the_thing") { mes("Baa!") }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Fred the Farmer</col> at his farm, " +
            "<col=800000>north-west of Lumbridge</col>."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            description("I have spoken to <red>Fred the Farmer</red>.")
            objective("I need to collect <red>${remaining(player.player)}</red> more <red>balls of wool</red>.") {
                hasItem("ball_of_wool", "I have some <red>balls of wool</red> to give to Fred.")
                    .preserveObjective(strikeObjective = false)
            }
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line("I brought Fred the Farmer 20 balls of wool, which I sheared from his sheep and spun on a spinning wheel.")
            line("He paid me for my trouble.")
        }

    private fun stage(player: Player): Int = quest.getQuestStage(player)

    private fun delivered(player: Player): Int =
        (stage(player) - STARTED_STAGE).coerceIn(0, BALLS_NEEDED)

    private fun remaining(player: Player): Int = BALLS_NEEDED - delivered(player)

    private fun ballsOfWool(player: Player): Int = player.inv.count(BALL_OF_WOOL)

    private fun ballsText(n: Int): String = if (n == 1) "1 ball" else "$n balls"

    private suspend fun ProtectedAccess.shearTheThing() {
        if (SHEARS !in inv) {
            mes("You need a pair of shears to shear the sheep.")
            return
        }
        anim("seq.human_shearing")
        mes("The... whatever it is... manages to get away from you!")
        if (quest.isQuestInProgress(player)) player.sawTheThing = true
    }

    private suspend fun Dialogue.fredDialogue() {
        when {
            quest.isQuestCompleted(player) -> afterQuest()
            quest.isQuestInProgress(player) -> duringQuest()
            else -> beforeQuest()
        }
    }

    private suspend fun Dialogue.beforeQuest() {
        chatNpc(angry, "What are you doing on my land? You're not the one who keeps leaving all my gates open and letting out all my sheep, are you?")
        when (choice3("I'm looking for a quest.", 1, "I'm looking for something to kill.", 2, "I'm lost.", 3)) {
            1 -> offerQuest()
            2 -> lookingToKill()
            3 -> lost()
        }
    }

    private suspend fun Dialogue.lookingToKill() {
        chatPlayer(neutral, "I'm looking for something to kill.")
        chatNpc(angry, "What, on my land? Leave my livestock alone you scoundrel!")
    }

    private suspend fun Dialogue.lost() {
        chatPlayer(confused, "I'm lost.")
        chatNpc(neutral, "How can you be lost? Just follow the road east and south. You'll end up in Lumbridge fairly quickly.")
    }

    private suspend fun Dialogue.offerQuest() {
        chatPlayer(neutral, "I'm looking for a quest.")
        chatNpc(neutral, "You're after a quest, you say? Actually, I could do with a bit of help.")
        chatNpc(neutral, "My sheep are getting mighty woolly. I'd be much obliged if you could shear them. And while you're at it, spin the wool for me too.")
        chatNpc(neutral, "Yes, that's it. Bring me 20 balls of wool. And I'm sure I could sort out some sort of payment. Of course, there's the small matter of The Thing.")
        chatPlayer(quiz, "What do you mean, The Thing?")
        chatNpc(worried, "Well now, no one has ever seen The Thing. That's why we call it The Thing, 'cos we don't know what it is.")
        chatNpc(worried, "Some say it's a black hearted shapeshifter, hungering for the souls of hard working decent folk like me. Others say it's just a sheep.")
        chatNpc(angry, "Well I don't have all day to stand around and gossip. Are you going to shear my sheep or what!")
        if (!startQuestPrompt(quest)) {
            chatPlayer(neutral, "No, I'll give it a miss.")
            chatNpc(neutral, "Suit yourself.")
            return
        }
        chatPlayer(happy, "Yes, okay. I can do that.")
        quest.advanceQuestStage(access)
        if (ballsOfWool(player) >= BALLS_NEEDED) {
            chatPlayer(happy, "In fact Fred, funnily enough, I actually have 20 balls of wool already on me.")
            chatNpc(angry, "Have you been shearing my sheep without permission!?")
            chatPlayer(shifty, "No! Well, maybe... They just looked a little wooly! Surely you like a shave once in a while, too?")
            chatNpc(angry, "It's rude to shave another person without permission - don't be coming at me with them shears!")
            chatPlayer(sad, "I'm sorry, I'll ask permission next time.")
            chatNpc(neutral, "I guess no real 'arm was done. Hand the balls over and we can put this whole thing behind us.")
            handIn()
            return
        }
        chatNpc(quiz, "Good! Now one more thing, do you actually know how to shear a sheep?")
        chatPlayer(confused, "Err. No, I don't know actually.")
        howToShear()
        spinningHelp()
    }

    private suspend fun Dialogue.howToShear() {
        if (SHEARS in player.inv) {
            chatNpc(happy, "Well, you're half way there already! You have a set of shears in your inventory. Just use those on a Sheep to shear it.")
            chatPlayer(quiz, "That's all I have to do?")
            chatNpc(neutral, "Well once you've collected some wool you'll need to spin it into balls.")
            return
        }
        chatNpc(neutral, "Well, first things first, you need a pair of shears. I've got some here you can use.")
        access.invAddOrDrop(objRepo, SHEARS)
        objbox(SHEARS, "Fred gives you a set of sharp shears.")
        chatNpc(neutral, "You just need to go and use them on the sheep out in my field.")
        chatPlayer(happy, "Sounds easy!")
        chatNpc(laugh, "That's what they all say!")
        chatNpc(neutral, "Some of the sheep don't like it too much... Persistence is the key.")
        chatNpc(neutral, "Once you've collected some wool you can spin it into balls.")
    }

    private suspend fun Dialogue.spinningHelp(askFirst: Boolean = true) {
        if (askFirst) {
            chatNpc(quiz, "Do you know how to spin wool?")
            chatPlayer(sad, "I don't know how to spin wool, sorry.")
            chatNpc(happy, "Don't worry, it's quite simple!")
        }
        chatNpc(neutral, "The nearest Spinning Wheel can be found on the first floor of Lumbridge Castle.")
        chatNpc(neutral, "To get to Lumbridge Castle just follow the road east.")
        mesbox("This icon denotes a Spinning Wheel on the world map.")
        chatPlayer(happy, "Thank you!")
    }

    private suspend fun Dialogue.duringQuest() {
        chatNpc(angry, "What are you doing on my land?")
        if (player.sawTheThing) {
            when (choice2("I need to talk to you about shearing these sheep!", 1, "Fred! Fred! I've seen The Thing!", 2)) {
                1 -> shearingTalk()
                2 -> sawTheThingTalk()
            }
            return
        }
        shearingTalk()
    }

    private suspend fun Dialogue.shearingTalk() {
        chatPlayer(neutral, "I need to talk to you about shearing these sheep!")
        chatNpc(quiz, "Oh. How are you doing getting those balls of wool?")
        if (ballsOfWool(player) > 0) {
            chatPlayer(happy, "I have some.")
            chatNpc(neutral, "Give 'em here then.")
            handIn()
            return
        }
        chatPlayer(quiz, "How many more do I need to give you?")
        chatNpc(neutral, "You need to collect ${remaining(player)} more ${if (remaining(player) == 1) "ball" else "balls"} of wool.")
        if (player.inv.count(WOOL) > 0) {
            chatPlayer(neutral, "I've got some wool. I've not managed to make it into a ball though.")
            chatNpc(neutral, "Well go find a spinning wheel then. You can find one on the first floor of Lumbridge Castle, just walk east on the road outside my house and you'll find Lumbridge.")
            return
        }
        chatPlayer(sad, "I haven't got any at the moment.")
        chatNpc(neutral, "Ah well at least you haven't been eaten. You know what you're doing, right?")
        when (choice3("How do I shear sheep, again?", 1, "Remind me how to spin wool.", 2, "Yeah, I think so.", 3)) {
            1 -> {
                chatPlayer(quiz, "How do I shear sheep, again?")
                howToShear()
                chatNpc(quiz, "Do you know how to spin wool?")
                when (choice2("Yes, I know how to spin wool.", 1, "I don't know how to spin wool, sorry.", 2)) {
                    1 -> {
                        chatPlayer(happy, "Yes, I know how to spin wool.")
                        chatNpc(happy, "Great!")
                    }
                    2 -> {
                        chatPlayer(sad, "I don't know how to spin wool, sorry.")
                        chatNpc(happy, "Don't worry, it's quite simple!")
                        spinningHelp(askFirst = false)
                    }
                }
            }
            2 -> {
                chatPlayer(quiz, "Remind me how to spin wool.")
                spinningHelp(askFirst = false)
            }
            3 -> {
                chatPlayer(happy, "Yeah, I think so.")
                chatNpc(neutral, "You can get to it, then!")
            }
        }
    }

    /** Takes up to the remaining balls of wool; the last one completes the quest. */
    private suspend fun Dialogue.handIn() {
        val given = minOf(ballsOfWool(player), remaining(player))
        if (given <= 0) return
        access.invDel(access.inv, BALL_OF_WOOL, given)
        val stage = stage(player) + given
        mesbox("You give Fred ${ballsText(given)} of wool.")
        if (stage >= COMPLETE_STAGE) {
            chatPlayer(happy, "That's the last of them.")
            chatNpc(happy, "I guess I'd better pay you then.")
            player.sawTheThing = false
            quest.setQuestStage(access, COMPLETE_STAGE)
            return
        }
        quest.setQuestStage(access, stage)
        chatPlayer(neutral, "That's all I've got so far.")
        chatNpc(neutral, "I need ${remaining(player)} more before I can pay you.")
        chatPlayer(neutral, "Ok I'll work on it.")
    }

    private suspend fun Dialogue.sawTheThingTalk() {
        chatPlayer(shocked, "Fred! Fred! I've seen The Thing!")
        chatNpc(shocked, "You ... you actually saw it?")
        chatNpc(shocked, "Run for the hills! ${player.displayName} grab as many chickens as you can! We have to ...")
        chatPlayer(angry, "Fred!")
        chatNpc(shocked, "... flee! Oh, woe is me! The shapeshifter is coming! We're all ...")
        chatPlayer(angry, "FRED!")
        chatNpc(confused, "... doomed. What!")
        chatPlayer(neutral, "It's not a shapeshifter or any other kind of monster!")
        chatNpc(quiz, "Well then what is it?")
        chatPlayer(neutral, "Well ... it's just two Penguins; Penguins disguised as a sheep.")
        chatNpc(confused, "...")
        chatNpc(confused, "Have you been out in the sun too long?")
    }

    private suspend fun Dialogue.afterQuest() {
        chatNpc(angry, "What are you doing on my land?")
        when (choice3("I'm looking for something to kill.", 1, "I'm lost.", 2, "Fred! Fred! I've seen The Thing!", 3)) {
            1 -> lookingToKill()
            2 -> lost()
            3 -> sawTheThingTalk()
        }
    }

    private companion object {
        const val STARTED_STAGE = 1
        const val BALLS_NEEDED = 20
        const val COMPLETE_STAGE = STARTED_STAGE + BALLS_NEEDED

        const val SHEARS = "obj.shears"
        const val WOOL = "obj.wool"
        const val BALL_OF_WOOL = "obj.ball_of_wool"
    }
}
