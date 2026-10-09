package org.rsmod.content.quest.area.lumbridge.xmarksthespot.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.lumbridge.xmarksthespot.XMarksTheSpotQuest
import org.rsmod.content.quest.area.lumbridge.xmarksthespot.XMarksTheSpotQuest.Companion.BOBS_SCROLL
import org.rsmod.content.quest.area.lumbridge.xmarksthespot.XMarksTheSpotQuest.Companion.CASKET
import org.rsmod.content.quest.area.lumbridge.xmarksthespot.XMarksTheSpotQuest.Companion.CIPHER_SCROLL
import org.rsmod.content.quest.area.lumbridge.xmarksthespot.XMarksTheSpotQuest.Companion.CLUE_ITEMS
import org.rsmod.content.quest.area.lumbridge.xmarksthespot.XMarksTheSpotQuest.Companion.LAMP
import org.rsmod.content.quest.area.lumbridge.xmarksthespot.XMarksTheSpotQuest.Companion.ORB
import org.rsmod.content.quest.area.lumbridge.xmarksthespot.XMarksTheSpotQuest.Companion.SCROLL_BOX
import org.rsmod.content.quest.area.lumbridge.xmarksthespot.XMarksTheSpotQuest.Companion.STAGE_ACCEPTED
import org.rsmod.content.quest.area.lumbridge.xmarksthespot.XMarksTheSpotQuest.Companion.STAGE_BOBS_CLUE
import org.rsmod.content.quest.area.lumbridge.xmarksthespot.XMarksTheSpotQuest.Companion.STAGE_CASKET
import org.rsmod.content.quest.area.lumbridge.xmarksthespot.XMarksTheSpotQuest.Companion.STAGE_CIPHER_CLUE
import org.rsmod.content.quest.area.lumbridge.xmarksthespot.XMarksTheSpotQuest.Companion.STAGE_DELIVERED
import org.rsmod.content.quest.area.lumbridge.xmarksthespot.XMarksTheSpotQuest.Companion.STAGE_MAP_CLUE
import org.rsmod.content.quest.area.lumbridge.xmarksthespot.XMarksTheSpotQuest.Companion.STAGE_NOT_STARTED
import org.rsmod.content.quest.area.lumbridge.xmarksthespot.XMarksTheSpotQuest.Companion.STAGE_ORB_CLUE
import org.rsmod.content.quest.area.lumbridge.xmarksthespot.lampUsed
import org.rsmod.content.quest.area.lumbridge.xmarksthespot.metVeosInKourend
import org.rsmod.content.quest.area.lumbridge.xmarksthespot.owns
import org.rsmod.content.quest.area.lumbridge.xmarksthespot.scrollBoxOwed
import org.rsmod.content.quest.manager.startQuestPrompt
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Veos
@Inject
constructor(private val xMarks: XMarksTheSpotQuest, private val objRepo: ObjRepository) :
    PluginScript() {

    private val quest
        get() = xMarks.quest

    override fun ScriptContext.startup() {
        onOpNpc1("npc.veos_lumbridge") { startDialogue(it.npc) { veosLumbridge() } }
    }

    private suspend fun Dialogue.veosLumbridge() {
        when (xMarks.stage(player)) {
            STAGE_NOT_STARTED -> beforeQuest()
            STAGE_ACCEPTED -> readyToHelp()
            in STAGE_BOBS_CLUE..STAGE_CIPHER_CLUE -> treasureHuntCheckIn(atSarim = false)
            else -> sendToShip()
        }
    }

    /** Port Sarim Veos: returns `false` when he has nothing quest-related to say. */
    suspend fun Dialogue.veosSarimQuest(): Boolean {
        when (xMarks.stage(player)) {
            STAGE_NOT_STARTED -> return false
            STAGE_ACCEPTED -> readyToHelp()
            in STAGE_BOBS_CLUE..STAGE_CIPHER_CLUE -> treasureHuntCheckIn(atSarim = true)
            STAGE_CASKET -> deliverCasket()
            STAGE_DELIVERED -> {
                chatNpc(happy, "Hello again.")
                rewardAndFarewell()
            }
            else -> return reclaimRewards()
        }
        return true
    }

    private suspend fun Dialogue.beforeQuest() {
        chatNpc(happy, "Hello there.")
        if (player.metVeosInKourend) {
            chatPlayer(happy, "Hello Veos. What brings you to Lumbridge?")
            chatNpc(
                neutral,
                "I'm here on a treasure hunt, of sorts. Back in Kourend I came across a scroll " +
                    "that I think will lead me to something of great value.",
            )
            explainBlocker()
            offerHelp()
            return
        }
        when (choice3("Who are you?", 1, "I'm looking for a quest.", 2, "I have to go.", 3)) {
            1 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(
                    happy,
                    "The name's Veos. I'm a treasure hunter from the wondrous Kingdom of Great " +
                        "Kourend.",
                )
                introduceKourend()
            }
            2 -> {
                chatNpc(
                    neutral,
                    "Hmmm. Well, now that you mention it, I could use a hand. The name's Veos. " +
                        "I'm a treasure hunter from the wondrous Kingdom of Great Kourend.",
                )
                introduceKourend()
            }
            3 -> chatPlayer(neutral, "I have to go.")
        }
    }

    private suspend fun Dialogue.introduceKourend() {
        chatPlayer(quiz, "Great Kourend? Where's that?")
        chatNpc(happy, "Far across the sea to the west. It's a truly magnificent place.")
        chatPlayer(quiz, "Interesting. So what brings you to Lumbridge?")
        chatNpc(
            neutral,
            "I'm on a hunt. A hunt for treasure. Back home in Great Kourend I came across a " +
                "scroll, and I believe it will lead me to something of great value.",
        )
        explainBlocker()
        offerHelp()
    }

    private suspend fun Dialogue.explainBlocker() {
        chatNpc(
            sad,
            "Unfortunately I've hit a bit of a wall. The scroll led me here, but I don't know the " +
                "area at all, so I'm not sure what to do next.",
        )
    }

    private suspend fun Dialogue.offerHelp() {
        if (!choice2("Can I help?", true, "Well good luck with it.", false)) {
            chatPlayer(neutral, "Well good luck with it.")
            return
        }
        chatPlayer(quiz, "Can I help?")
        chatNpc(
            neutral,
            "Hmmm. Maybe you can. You probably know these parts better than I do, so you might " +
                "be able to make sense of the scroll. I can reward you if you help.",
        )
        if (!startQuestPrompt(quest)) {
            chatPlayer(neutral, "I'm good thanks.")
            chatNpc(neutral, "Fair enough. I'll be here if you change your mind.")
            return
        }
        chatPlayer(happy, "Sounds good, what should I do?")
        with(xMarks) { access.setStage(STAGE_ACCEPTED) }
        handOverScroll()
    }

    private suspend fun Dialogue.readyToHelp() {
        chatNpc(quiz, "Ready to help with this treasure hunt?")
        chatPlayer(quiz, "What do you need me to do?")
        handOverScroll()
    }

    private suspend fun Dialogue.handOverScroll() {
        if (player.inv.freeSpace() == 0) {
            chatNpc(
                neutral,
                "First you'll need to make some room in your inventory. Once you have, you can " +
                    "take a look at this scroll.",
            )
            return
        }
        chatNpc(
            neutral,
            "Take this scroll. It should lead you to the treasure I'm after. Once you've found " +
                "it, meet me at my ship. It's docked at the northernmost pier in Port Sarim.",
        )
        access.invAddOrDrop(objRepo, BOBS_SCROLL)
        with(xMarks) { access.setStage(STAGE_BOBS_CLUE) }
        objbox(BOBS_SCROLL, "Veos hands you a scroll.")
        chatPlayer(happy, "Awesome. Anything else I should know?")
        chatNpc(
            neutral,
            "You'll probably want a spade - in my experience they're almost always needed when " +
                "hunting treasure. The general store should have one if you don't.",
        )
        if (player.metVeosInKourend) {
            chatNpc(
                neutral,
                "I'll be staying here a little longer before I head back to my ship. If you need " +
                    "a hand with anything, just let me know and I'll see what I can do.",
            )
        } else {
            chatNpc(neutral, "If you need any extra help, just let me know.")
        }
        chatPlayer(happy, "Okay, thanks Veos.")
        chatNpc(happy, "Good luck.")
    }

    private suspend fun Dialogue.treasureHuntCheckIn(atSarim: Boolean) {
        chatNpc(happy, "Hello there.")
        chatPlayer(happy, "Hello Veos.")
        if (returnLostClue()) {
            return
        }
        chatPlayer(neutral, "Let's talk about my quest.")
        chatNpc(quiz, "How's the treasure hunt going?")
        if (!choice2("I could do with some extra help.", true, "I'm still working on it.", false)) {
            chatPlayer(neutral, "I'm still working on it.")
            keepAtIt(atSarim)
            return
        }
        chatPlayer(neutral, "I could do with some extra help.")
        val stage = xMarks.stage(player)
        val shown = if (stage == STAGE_ORB_CLUE) "orb" else "scroll"
        mesbox("You show the $shown to Veos.")
        when (stage) {
            STAGE_BOBS_CLUE ->
                chatNpc(
                    confused,
                    "This is the scroll I gave you. I'm afraid I don't know how to solve it. " +
                        "Maybe look for someone called Bob?",
                )
            STAGE_MAP_CLUE ->
                chatNpc(neutral, "Looks like a map to me. I'd guess the X marks where you need to go.")
            STAGE_ORB_CLUE ->
                chatNpc(
                    neutral,
                    "Interesting, these are quite rare. They work using temperature - the closer " +
                        "you are, the hotter the orb gets.",
                )
            else ->
                chatNpc(
                    neutral,
                    "Ah, a cipher. I've seen these before. Try shifting the letters to the left " +
                        "or right - that normally does the trick.",
                )
        }
    }

    private suspend fun Dialogue.keepAtIt(atSarim: Boolean) {
        if (atSarim) {
            chatNpc(neutral, "Well, keep at it. Once you've found the treasure, bring it to me.")
        } else {
            chatNpc(
                neutral,
                "Well, keep at it. Once you've found the treasure, meet me at my ship. It's " +
                    "docked at the northernmost pier in Port Sarim.",
            )
        }
    }

    private suspend fun Dialogue.returnLostClue(): Boolean {
        val clue = CLUE_ITEMS[xMarks.stage(player)] ?: return false
        if (player.owns(clue)) {
            return false
        }
        val noun = if (clue == ORB) "orb" else "scroll"
        chatNpc(quiz, "I came across this $noun. Did you lose it by any chance?")
        access.invAddOrDrop(objRepo, clue)
        objbox(clue, "Veos hands you a $noun.")
        chatPlayer(happy, "I did, thanks Veos.")
        chatNpc(neutral, "Not to worry. Just try to be careful with it.")
        return true
    }

    private suspend fun Dialogue.sendToShip() {
        chatNpc(happy, "Hello there.")
        chatPlayer(happy, "Hello Veos.")
        if (xMarks.stage(player) == STAGE_CASKET && player.inv.count(CASKET) > 0) {
            chatPlayer(happy, "I found the treasure!")
            chatNpc(
                happy,
                "Excellent! Bring it to my ship and I'll take it off your hands. It's docked at " +
                    "the northernmost pier in Port Sarim.",
            )
            return
        }
        if (xMarks.stage(player) == STAGE_DELIVERED) {
            chatNpc(happy, "Come and see me at my ship in Port Sarim and I'll give you your reward.")
            return
        }
        keepAtIt(atSarim = false)
    }

    private suspend fun Dialogue.deliverCasket() {
        chatNpc(happy, "Hello there.")
        chatPlayer(happy, "Hello Veos.")
        chatNpc(quiz, "How's the treasure hunt going?")
        if (player.inv.count(CASKET) == 0) {
            chatPlayer(sad, "I found the treasure, but I don't seem to have it with me.")
            chatNpc(
                neutral,
                "Then you'd best go back to where you found it. Bring it to me once you have it.",
            )
            return
        }
        chatPlayer(happy, "I found the treasure!")
        chatNpc(happy, "Excellent. I'll take it off your hands now.")
        if (!hasRewardSpace(freedSlots = 1)) {
            chatNpc(
                neutral,
                "Before I do, you'll need to make some room in your inventory so I can give you " +
                    "your reward.",
            )
            return
        }
        if (access.invDel(access.inv, CASKET).failure) {
            return
        }
        with(xMarks) { access.setStage(STAGE_DELIVERED) }
        objbox(CASKET, "You give Veos the ancient casket.")
        chatNpc(
            happy,
            "Brilliant. This is exactly what I was looking for. Thank you so much for your help.",
        )
        chatPlayer(quiz, "No problem. So what is this treasure, anyway?")
        chatNpc(
            shifty,
            "Oh, err... nothing important. Just something that might be of use to me back in " +
                "Great Kourend.",
        )
        chatPlayer(confused, "If you say so...")
        rewardAndFarewell()
    }

    private suspend fun Dialogue.rewardAndFarewell() {
        if (!hasRewardSpace(freedSlots = 0)) {
            chatNpc(
                neutral,
                "I've got your reward here, but you'll need to make some room in your inventory " +
                    "first.",
            )
            return
        }
        chatNpc(
            neutral,
            "Anyway, I'd best be getting back to Great Kourend soon. I'm sure I'll be back here " +
                "before long though - there's always more treasure to be found.",
        )
        chatNpc(
            happy,
            "If you ever fancy visiting the kingdom, come and find me here. I'll happily take " +
                "you there - consider it an extra thank you for your help.",
        )
        chatPlayer(happy, "Sounds great, thanks Veos.")
        chatNpc(happy, "And as promised, here's your reward.")
        if (player.owns(SCROLL_BOX)) {
            player.scrollBoxOwed = true
        } else {
            access.invAdd(access.inv, SCROLL_BOX)
        }
        quest.completeQuest(access)
        xMarks.syncVeos(player)
    }

    private suspend fun Dialogue.reclaimRewards(): Boolean {
        val lamp = !player.lampUsed && !player.owns(LAMP)
        val scrollBox = player.scrollBoxOwed && !player.owns(SCROLL_BOX)
        if (!lamp && !scrollBox) {
            return false
        }
        if (player.inv.freeSpace() == 0) {
            chatNpc(
                neutral,
                "I've got something of yours here. Make some room in your inventory and I'll " +
                    "hand it over.",
            )
            return true
        }
        chatNpc(happy, "Ah, ${player.displayName}! I've got something here for you.")
        val obj = if (lamp) LAMP else SCROLL_BOX
        if (!lamp) {
            player.scrollBoxOwed = false
        }
        access.invAdd(access.inv, obj)
        objbox(obj, "Veos hands you ${if (lamp) "an antique lamp" else "a beginner scroll box"}.")
        return true
    }

    private fun Dialogue.hasRewardSpace(freedSlots: Int): Boolean {
        var needed = 1
        if (player.inv.count("obj.coins") == 0) needed++
        if (!player.owns(SCROLL_BOX)) needed++
        return player.inv.freeSpace() + freedSlots >= needed
    }
}
