package org.rsmod.content.quest.area.lumbridge

import jakarta.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.hook.SpadeDigHook
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestJournalBuilder
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.chooseLampSkill
import org.rsmod.content.quest.manager.rewards
import org.rsmod.content.quest.manager.startQuestPrompt
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class XMarksTheSpot @Inject constructor(private val objRepo: ObjRepository) :
    QuestScript(
        QuestKey,
        "varp.cluequest_main",
        rewards {
            item("obj.coins", CoinReward, label = "$CoinReward Coins")
            item(Lamp, label = "An antique lamp")
            extra("A beginner scroll box")
        },
        ItemRewardDisplay(Casket, zoom = 400),
        questVarbit = "varbit.cluequest",
    ),
    SpadeDigHook {

    private var Player.veosLumbridgeVis by intVarBit("varbit.veos_lumbridge_vis")
    private var Player.veosSarimVis by intVarBit("varbit.veos_sarim_vis")
    private var Player.orbPreviousDistance by intVarBit("varbit.cluequest_prev_distance")
    private var Player.lampUsed by boolVarBit("varbit.cluequest_lamp_reward")
    private var Player.scrollBoxOwed by boolVarBit("varbit.cluequest_clue_reward")
    private var Player.metVeosInKourend by boolVarBit("varbit.cluequest_veos_already_met")

    override fun ScriptContext.init() {
        check(quest.maxSteps == CompleteStage) {
            "X Marks the Spot end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the script completes at $CompleteStage."
        }

        onPlayerLogin { syncVeos(player) }

        onOpNpc1("npc.veos_lumbridge") { startDialogue(it.npc) { veosLumbridge() } }

        onOpHeld1(BobsScroll) { readClue(BobsClueText) }
        onOpHeld1(MapScroll) { ifOpenMainModal("interface.cluequest_map") }
        onOpHeld1(Orb) { feelOrb() }
        onOpHeld1(CipherScroll) { readClue(CipherClueText) }
        onOpHeld1(Casket) { startDialogue { chatPlayer(neutral, CasketWarning) } }
        onOpHeld1(Lamp) { rubLamp() }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Veos</col> in <col=800000>The Sheared Ram</col> pub in " +
            "<col=800000>Lumbridge</col>."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            description(
                "<red>Veos</red>, a treasure hunter from the Kingdom of <red>Great Kourend</red>, " +
                    "has asked me to help him follow a treasure scroll he found."
            )
            objective(
                "I need to make some room in my inventory so that <red>Veos</red> can give me " +
                    "his treasure scroll."
            ) {
                visibleWhen { quest.getQuestStage(access.player) == AcceptedStage }
            }
            step(
                BobsClueStage,
                "The scroll describes a walk taken by a man named <red>Bob</red> in " +
                    "<red>Lumbridge</red>. I should retrace his steps and dig where he buried " +
                    "his treasure.",
            )
            step(
                MapClueStage,
                "I dug up a scroll with a <red>map</red> on it. I should dig where the " +
                    "<red>X</red> marks the spot.",
            )
            step(
                OrbClueStage,
                "I dug up a <red>mysterious orb</red>. It grows hotter the closer I get to the " +
                    "next place to dig.",
            )
            step(
                CipherClueStage,
                "I dug up a scroll written in a strange <red>cipher</red>. Shifting the letters " +
                    "might reveal where to dig.",
            )
            step(
                CasketStage,
                "I dug up an <red>ancient casket</red>. I should take it to <red>Veos</red> at " +
                    "his ship on the northernmost pier in <red>Port Sarim</red>.",
            )
            objective("I gave Veos the casket. I should speak to him to collect my reward.") {
                visibleWhen { quest.getQuestStage(access.player) == DeliveredStage }
            }
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line(
                "I helped Veos, a treasure hunter from Great Kourend, follow a treasure scroll " +
                    "from Lumbridge to Draynor Village."
            )
            line(
                "At the end of the trail I dug up an ancient casket and took it to Veos in Port " +
                    "Sarim, who rewarded me for my help."
            )
        }

    private fun QuestJournalBuilder.step(from: Int, text: String) =
        objective(text) {
            visibleWhen { quest.getQuestStage(access.player) >= from }
            stageAtLeast(from + 1, text).strike()
        }

    private fun stage(player: Player): Int = quest.getQuestStage(player)

    fun syncVeos(player: Player) {
        val stage = stage(player)
        player.veosLumbridgeVis = if (stage >= CompleteStage) 0 else VeosLumbridgeVisible
        player.veosSarimVis =
            when {
                QuestRequirements.hasCompleted(player, QuestKey) -> VeosSarimTravel
                stage >= AcceptedStage -> VeosSarimVisible
                else -> 0
            }
    }

    private fun ProtectedAccess.setStage(stage: Int) {
        quest.setQuestStage(this, stage)
        syncVeos(player)
    }

    private suspend fun Dialogue.veosLumbridge() {
        when (stage(player)) {
            NotStartedStage -> beforeQuest()
            AcceptedStage -> readyToHelp()
            in BobsClueStage..CipherClueStage -> treasureHuntCheckIn(atSarim = false)
            else -> sendToShip()
        }
    }

    /** Port Sarim Veos: returns `false` when he has nothing quest-related to say. */
    suspend fun Dialogue.veosSarimQuest(): Boolean {
        when (stage(player)) {
            NotStartedStage -> return false
            AcceptedStage -> readyToHelp()
            in BobsClueStage..CipherClueStage -> treasureHuntCheckIn(atSarim = true)
            CasketStage -> deliverCasket()
            DeliveredStage -> {
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
        access.setStage(AcceptedStage)
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
        access.invAddOrDrop(objRepo, BobsScroll)
        access.setStage(BobsClueStage)
        objbox(BobsScroll, "Veos hands you a scroll.")
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
        val stage = stage(player)
        val shown = if (stage == OrbClueStage) "orb" else "scroll"
        mesbox("You show the $shown to Veos.")
        when (stage) {
            BobsClueStage ->
                chatNpc(
                    confused,
                    "This is the scroll I gave you. I'm afraid I don't know how to solve it. " +
                        "Maybe look for someone called Bob?",
                )
            MapClueStage ->
                chatNpc(neutral, "Looks like a map to me. I'd guess the X marks where you need to go.")
            OrbClueStage ->
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
        val clue = ClueItems[stage(player)] ?: return false
        if (player.owns(clue)) {
            return false
        }
        val noun = if (clue == Orb) "orb" else "scroll"
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
        if (stage(player) == CasketStage && player.inv.count(Casket) > 0) {
            chatPlayer(happy, "I found the treasure!")
            chatNpc(
                happy,
                "Excellent! Bring it to my ship and I'll take it off your hands. It's docked at " +
                    "the northernmost pier in Port Sarim.",
            )
            return
        }
        if (stage(player) == DeliveredStage) {
            chatNpc(happy, "Come and see me at my ship in Port Sarim and I'll give you your reward.")
            return
        }
        keepAtIt(atSarim = false)
    }

    private suspend fun Dialogue.deliverCasket() {
        chatNpc(happy, "Hello there.")
        chatPlayer(happy, "Hello Veos.")
        chatNpc(quiz, "How's the treasure hunt going?")
        if (player.inv.count(Casket) == 0) {
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
        if (access.invDel(access.inv, Casket).failure) {
            return
        }
        access.setStage(DeliveredStage)
        objbox(Casket, "You give Veos the ancient casket.")
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
        if (player.owns(ScrollBox)) {
            player.scrollBoxOwed = true
        } else {
            access.invAdd(access.inv, ScrollBox)
        }
        quest.completeQuest(access)
        syncVeos(player)
    }

    private suspend fun Dialogue.reclaimRewards(): Boolean {
        val lamp = !player.lampUsed && !player.owns(Lamp)
        val scrollBox = player.scrollBoxOwed && !player.owns(ScrollBox)
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
        val obj = if (lamp) Lamp else ScrollBox
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
        if (!player.owns(ScrollBox)) needed++
        return player.inv.freeSpace() + freedSlots >= needed
    }

    private fun Player.owns(obj: String): Boolean =
        inv.count(obj) > 0 || (invMap["inv.bank"]?.count(obj) ?: 0) > 0

    override fun claims(player: Player): Boolean {
        val coords = player.coords
        return when (stage(player)) {
            BobsClueStage -> coords == BobsSpot
            MapClueStage -> coords == CastleSpot
            OrbClueStage -> coords.level == 0 && orbDistance(coords) <= OrbDigRadius
            CipherClueStage -> coords == PigPenSpot
            CasketStage -> coords == PigPenSpot && !player.owns(Casket)
            else -> false
        }
    }

    override suspend fun ProtectedAccess.dig() {
        when (stage(player)) {
            BobsClueStage ->
                findClue(BobsScroll, MapScroll, MapClueStage, "You dig up another treasure scroll.")
            MapClueStage -> findClue(MapScroll, Orb, OrbClueStage, "You dig up a mysterious orb.")
            OrbClueStage ->
                findClue(Orb, CipherScroll, CipherClueStage, "You dig up another treasure scroll.")
            CipherClueStage -> unearthCasket()
            CasketStage -> {
                invAddOrDrop(objRepo, Casket)
                startDialogue { objbox(Casket, "You dig up the ancient casket again.") }
            }
        }
    }

    private suspend fun ProtectedAccess.findClue(old: String, new: String, next: Int, text: String) {
        if (inv.count(old) > 0) {
            invDel(inv, old)
        }
        invAddOrDrop(objRepo, new)
        setStage(next)
        startDialogue { objbox(new, text) }
    }

    private suspend fun ProtectedAccess.unearthCasket() {
        if (inv.count(CipherScroll) > 0) {
            invDel(inv, CipherScroll)
        }
        invAddOrDrop(objRepo, Casket)
        setStage(CasketStage)
        startDialogue {
            objbox(
                Casket,
                "You dig up an ancient casket. As you do, you hear a faint whispering, but you " +
                    "can't make out the words...",
            )
            chatPlayer(confused, "Hmmmm... Must have been the wind.")
            chatPlayer(
                neutral,
                "Anyway, this must be the treasure Veos is after. I should take it to him. If I " +
                    "remember right, his ship is docked at the northernmost pier in Port Sarim.",
            )
        }
    }

    private fun ProtectedAccess.readClue(text: String) {
        ifOpenMainModal("interface.trail_cluetext")
        ifSetText("component.trail_cluetext:text", text)
    }

    private fun ProtectedAccess.feelOrb() {
        if (stage(player) != OrbClueStage) {
            mes("The orb is cold and lifeless.")
            return
        }
        val distance = if (player.coords.level == 0) orbDistance(player.coords) else IceCold
        val temperature = OrbTemperatures.first { distance >= it.first }.second
        val previous = player.orbPreviousDistance - 1
        player.orbPreviousDistance = min(distance, MaxStoredDistance - 1) + 1
        val comparison =
            when {
                distance <= OrbDigRadius || previous < 0 -> ""
                distance < previous -> ", and warmer than last time"
                distance > previous -> ", and colder than last time"
                else -> ", and the same temperature as last time"
            }
        mes("The orb is $temperature$comparison.")
    }

    private suspend fun ProtectedAccess.rubLamp() {
        val stat = chooseLampSkill("Choose the stat you wish to be advanced!") ?: return
        if (invDel(inv, Lamp).failure) {
            return
        }
        player.lampUsed = true
        statAdvance(stat, LampXp)
        mes("Your wish has been granted!")
    }

    private fun orbDistance(coords: CoordGrid): Int =
        max(abs(coords.x - OrbSpot.x), abs(coords.z - OrbSpot.z))

    private companion object {
        private const val QuestKey = "quest_xmarksthespot"

        private const val NotStartedStage = 0
        private const val AcceptedStage = 1
        private const val BobsClueStage = 2
        private const val MapClueStage = 3
        private const val OrbClueStage = 4
        private const val CipherClueStage = 5
        private const val CasketStage = 6
        private const val DeliveredStage = 7
        private const val CompleteStage = 8

        private const val BobsScroll = "obj.cluequest_clue1"
        private const val MapScroll = "obj.cluequest_clue2"
        private const val Orb = "obj.cluequest_clue3"
        private const val CipherScroll = "obj.cluequest_clue4"
        private const val Casket = "obj.cluequest_casket"
        private const val Lamp = "obj.cluequest_lamp"
        private const val ScrollBox = "obj.league_clue_box_beginner"

        private val ClueItems =
            mapOf(
                BobsClueStage to BobsScroll,
                MapClueStage to MapScroll,
                OrbClueStage to Orb,
                CipherClueStage to CipherScroll,
            )

        private const val CoinReward = 200
        private const val LampXp = 300.0

        private const val VeosLumbridgeVisible = 2
        private const val VeosSarimVisible = 3
        private const val VeosSarimTravel = 4

        private val BobsSpot = CoordGrid(3230, 3209, 0)
        private val CastleSpot = CoordGrid(3203, 3212, 0)
        private val OrbSpot = CoordGrid(3109, 3264, 0)
        private val PigPenSpot = CoordGrid(3078, 3259, 0)

        private const val OrbDigRadius = 3
        private const val IceCold = 500
        private const val MaxStoredDistance = 8191

        private val OrbTemperatures =
            listOf(
                IceCold to "ice cold",
                200 to "very cold",
                150 to "cold",
                100 to "warm",
                70 to "hot",
                30 to "very hot",
                OrbDigRadius + 1 to "incredibly hot",
                0 to "visibly shaking",
            )

        private const val BobsClueText =
            "A man named Bob lives in the town of Lumbridge. Leaving his house by the door, he " +
                "walks 1 step east, 7 steps north, 5 steps west and 1 step south. There he digs " +
                "a hole and buries his treasure."

        private const val CipherClueText = "ESBZOPS QJH QFO"

        private const val CasketWarning =
            "I don't think Veos would want me opening his treasure. I should take it to him. If " +
                "I remember right, his ship is docked at the northernmost pier in Port Sarim."
    }
}
