package org.rsmod.content.quest.area.corsaircove

import org.rsmod.api.config.constants
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.midiJingle
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.forcedWalk
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.quest.manager.Quest
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

internal class CorsairCurseFerry(
    private val quest: Quest,
    private val fight: CorsairCurseFight,
    private val dialogue: CorsairCurseDialogue,
) {

    fun ScriptContext.register() {
        onOpLoc1(CorsairCurseRefs.CoveGangplank) { crossPlank(it.loc) }
        onOpLoc1(CorsairCurseRefs.RimmingtonPlankOn) { crossPlank(it.loc) }
        onOpLoc1(CorsairCurseRefs.RimmingtonPlankOff) { crossPlank(it.loc) }

        for (npcType in Helmsmen) {
            onOpNpc1(npcType) {
                val stage = quest.getQuestStage(player)
                // Crossroads Tock's multinpc resolves to this leaf id before the player sails.
                if (npcType == CorsairCurseRefs.Captain1Op && stage <= CorsairCurseStage.Started) {
                    with(dialogue) { talkToCrossroadsTock(it.npc) }
                } else {
                    startDialogue(it.npc) { helmsman() }
                }
            }
            onOpNpc3(npcType) { travelOp() }
        }
    }

    private suspend fun ProtectedAccess.crossPlank(loc: BoundLocInfo) {
        val crossing = CROSSINGS.firstOrNull { it.covers(loc.coords) }
        if (crossing == null) {
            mes(constants.dm_default, ChatType.Engine)
            return
        }

        val route = crossing.route(player.coords).dropWhile { it.sameColumn(player.coords) }
        if (route.isEmpty()) {
            return
        }

        val nearLeg = route.takeWhile { it.level == player.coords.level }
        val farLeg = route.drop(nearLeg.size)

        if (nearLeg.isNotEmpty()) {
            forcedWalk(nearLeg, crossTiles = tiles(nearLeg))
        }

        if (farLeg.isNotEmpty()) {
            telejump(farLeg.first())
            val remainder = farLeg.drop(1)
            if (remainder.isNotEmpty()) {
                forcedWalk(remainder, crossTiles = tiles(remainder))
            }
        }
    }

    private fun CoordGrid.sameColumn(other: CoordGrid): Boolean = x == other.x && z == other.z

    private fun ProtectedAccess.tiles(route: List<CoordGrid>): Int {
        var total = 0
        var from = player.coords
        for (waypoint in route) {
            total += from.chebyshevDistance(waypoint)
            from = waypoint
        }
        return maxOf(1, total)
    }

    private suspend fun Dialogue.helmsman() {
        val stage = quest.getQuestStage(player)
        when {
            stage >= CorsairCurseStage.Complete -> cabinBoyFerry()
            stage >= CorsairCurseStage.IthoiSlain -> tockFinale()
            stage >= CorsairCurseStage.KillOrdered -> tockAfterKillOrder()
            stage >= CorsairCurseStage.IthoiExposed -> tockOrdersTheKill()
            stage >= CorsairCurseStage.AtTheCove -> tockInvestigationHub(stage)
            stage >= CorsairCurseStage.TockSailedAhead -> tockFirstBoarding()
            else -> chatNpc(neutral, "Arrr.")
        }
    }

    private suspend fun Dialogue.tockFirstBoarding() {
        chatNpc(
            happy,
            "Arrr, ye've found my ship - good stuff. I used to command a much larger vessel, " +
                "sailing the seas with my crew of Corsairs, but now we be settled in Corsair Cove.",
        )
        chatNpc(
            sad,
            "My Corsairs be a good crew, but this curse has laid them low, all except me. Sickly, " +
                "they be. Too weak to stand. Ye'll see fer yerself soon enough.",
        )
        chatNpc(
            neutral,
            "There be four of 'em afflicted by the curse - Gnocci, Ithoi, Arsen and young Colin. " +
                "All taken to their beds, a- mewlin' and a-pukin' like so many babbies.",
        )
        chatNpc(
            worried,
            "I've another crewman, Francois, but he be away on a mission. I hope he " + "be spared!",
        )
        chatNpc(
            quiz,
            "Now be ye ready to voyage to Corsair Cove to help 'em? I can sail ye back here any " +
                "time ye likes.",
        )

        val sail = choice2(ReadyToSail, true, "Not just now, thanks.", false)
        if (!sail) {
            chatPlayer(neutral, "Not just now, thanks.")
            chatNpc(
                neutral,
                "That's fair enough, young $ladOrLass, but perhaps ye'll return to help me soon...",
            )
            return
        }

        chatPlayer(happy, ReadyToSail)

        quest.setQuestStage(access, CorsairCurseStage.AtTheCove)
        access.sailTo(CorsairCurseRefs.CoveShipDeck, "Corsair Cove")
    }

    private suspend fun Dialogue.tockInvestigationHub(stage: Int) {
        when (stage) {
            CorsairCurseStage.TheoriesRuledOut -> {
                tockHearsTheories()
                return
            }
            in CorsairCurseStage.TockToldOfDinner..CorsairCurseStage.GnocciNamedIthoi -> {
                tockRepeatsDinner()
                return
            }
            CorsairCurseStage.ArsenNamedMotive -> {
                tockFiringIthoi()
                return
            }
            in CorsairCurseStage.IthoiConfessed..CorsairCurseStage.DriftwoodBurning -> {
                tockIthoiConfessed()
                return
            }
        }

        while (true) {
            chatNpc(quiz, "Have ye any idea about how to lift the curse?")

            val relicReady = access.corscursThief == CorsairCurseCounts.ArsenInterviewed
            val allSpoken = allCrewSpoken
            val anySpoken = anyCrewSpoken

            val choice =
                when {
                    relicReady -> choice3(AskForRelic, 1, travelOptionText(), 2, TalkLater, 3)
                    allSpoken -> {
                        choice3(TheoriesHint, 1, travelOptionText(), 2, TalkLater, 3)
                    }
                    anySpoken -> choice3(SpokenToSome, 1, travelOptionText(), 2, TalkLater, 3)
                    else -> choice2(travelOptionText(), 2, TalkLater, 3)
                }

            when (choice) {
                1 -> {
                    when {
                        relicReady -> {
                            tockHandsOverRelic()
                            return
                        }
                        allSpoken -> {
                            tockTheoriesHint()
                            return
                        }
                        else -> tockSpeakToAll()
                    }
                }
                2 -> {
                    chatPlayer(neutral, travelOptionText())
                    access.sailAway()
                    return
                }
                else -> {
                    chatPlayer(neutral, TalkLater)
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.tockTheoriesHint() {
        chatPlayer(quiz, TheoriesHint)
        chatNpc(
            neutral,
            "Arrr, that be likely enough. Ithoi tells me there be many things in this world that " +
                "can bring a curse upon an unwary seafarer.",
        )
        chatNpc(
            sad,
            "Ye can maybe solve the problem fer each victim, an' see if it lifts the curse.",
        )
    }

    private suspend fun Dialogue.tockSpeakToAll() {
        chatPlayer(quiz, SpokenToSome)
        chatNpc(
            happy,
            "Arrr! Make sure ye speaks to 'em all. They'll be in their huts, over to the west. " +
                "Four of them, there are - Gnocci, Ithoi, Arsen and young Colin.",
        )
    }

    private suspend fun Dialogue.tockRepeatsDinner() {
        chatNpc(quiz, "Have ye any idea about how to lift the curse?")
        when (choice3(DinnerLead, 1, travelOptionText(), 2, TalkLater, 3)) {
            1 -> {
                chatPlayer(quiz, DinnerLead)
                chatNpc(
                    quiz,
                    "Arrr. I'd gone away for a parlay with Ithoi; we came back ter find the crew " +
                        "all a-pukin' up their dinner. Then Ithoi felt ill too, though he'd not " +
                        "eaten with 'em, so it can't be the food.",
                )
                chatPlayer(
                    neutral,
                    "I see. You're saying it began after they'd all eaten, but Ithoi felt ill even " +
                        "though he'd not had any of the food.",
                )
                chatNpc(sad, "Arrr, that be right.")
                chatPlayer(quiz, "That meal must have been interesting...")
            }
            2 -> {
                chatPlayer(neutral, travelOptionText())
                access.sailAway()
            }
            else -> chatPlayer(neutral, TalkLater)
        }
    }

    private suspend fun Dialogue.tockFiringIthoi() {
        chatNpc(quiz, "Have ye any idea about how to lift the curse?")
        when (choice3(FiringIthoi, 1, travelOptionText(), 2, TalkLater, 3)) {
            1 -> {
                chatPlayer(quiz, FiringIthoi)
                chatNpc(
                    neutral,
                    "Arrr, maybe I am, but I'm sure that be nothing to do wi' the curse. Ithoi's " +
                        "afflicted by it too.",
                )
            }
            2 -> {
                chatPlayer(neutral, travelOptionText())
                access.sailAway()
            }
            else -> chatPlayer(neutral, TalkLater)
        }
    }

    private suspend fun Dialogue.tockIthoiConfessed() {
        chatNpc(quiz, "Have ye any idea about how to lift the curse?")
        when (choice3(IthoiConfessed, 1, travelOptionText(), 2, TalkLater, 3)) {
            1 -> {
                chatPlayer(angry, "Ithoi admits he poisoned the crew! He's faked this whole curse!")
                chatNpc(
                    shocked,
                    "That can't be! Ithoi's sick, an' he may be babbling madness from his fevered " +
                        "brain. Ye'll have to prove he's not really sick.",
                )
            }
            2 -> {
                chatPlayer(neutral, travelOptionText())
                access.sailAway()
            }
            else -> chatPlayer(neutral, TalkLater)
        }
    }

    private suspend fun Dialogue.tockHandsOverRelic() {
        chatPlayer(neutral, AskForRelic)
        chatNpc(
            sad,
            "Arrr, that he did. I'd sent him to explore the caves west of the cove, an' he brings " +
                "me back a bone lump, saying he stole it from the ogre chief.",
        )
        chatPlayer(
            neutral,
            "He thinks the ogres have cursed you all because he stole one of their sacred relics, " +
                "and he wants me to take it back to them now.",
        )
        chatNpc(
            confused,
            "Arrr, I've no use fer the silly thing. If it'll get the curse lifted, ye can take it " +
                "right to that ogre chief an' give it back.",
        )

        if (!access.takeRelicFromTock()) {
            return
        }
        mesbox("Captain Tock gives you the loot that Arsen stole from the ogre chief.")
    }

    private suspend fun Dialogue.tockHearsTheories() {
        chatPlayer(confused, RuledOutTheories)
        chatPlayer(
            shifty,
            "Cabin-boy Colin thought he'd upset a mermaid, but there wasn't a " +
                "mermaid, just a wet ogre.",
        )
        chatPlayer(
            confused,
            "Arsen the Thief thought he'd upset the ogre shamans by stealing a sacred ogre relic, " +
                "but it was just a tooth-pick, and the ogres didn't care.",
        )
        chatPlayer(
            bored,
            "Ithoi the Navigator said there were lots of things that could bring a curse on a " +
                "crew, and this could be any of them.",
        )
        chatPlayer(
            laugh,
            "And Gnocci the Cook thought he'd awoken a demon in a possessed doll, but it wasn't " +
                "possessed at all. It was clockwork.",
        )
        chatNpc(
            sad,
            "Arrr, any sufficiently advanced technology be indistinguishable from demonic " +
                "possession, that it be.",
        )

        val ask = choice2("So what do I do now?", true, "Can I give up and go home?", false)
        if (!ask) {
            chatPlayer(quiz, "Can I give up and go home?")
            chatNpc(neutral, "If ye wants a ride back to Rimmington, I'll take ye there any time.")
            return
        }

        chatPlayer(quiz, "So what do I do now?")

        quest.setQuestStage(access, CorsairCurseStage.TockToldOfDinner)

        chatNpc(
            quiz,
            "Arrr, I don't rightly know. It began when I'd gone away for a parlay with Ithoi; we " +
                "came back ter find the crew all a-pukin' up their dinner. Then Ithoi felt ill " +
                "too, though he'd not eaten with 'em, so it can't be the food.",
        )
        chatPlayer(
            neutral,
            "I see. You're saying it began after they'd all eaten, but Ithoi felt ill even though " +
                "he'd not had any of the food.",
        )
        chatNpc(sad, "Arrr, that be right.")
        chatPlayer(quiz, "That meal must have been interesting...")
    }

    private suspend fun Dialogue.tockOrdersTheKill() {
        chatNpc(quiz, WhatsHappening)

        val choice = choice3(SeenIthoiRunning, 1, travelOptionText(), 2, TalkLater, 3)
        when (choice) {
            2 -> {
                chatPlayer(neutral, travelOptionText())
                access.sailAway()
                return
            }
            3 -> {
                chatPlayer(neutral, TalkLater)
                return
            }
        }

        chatPlayer(angry, SeenIthoiRunning)

        quest.setQuestStage(access, CorsairCurseStage.KillOrdered)

        chatNpc(
            angry,
            "Arrr! So he was never sick at all! An' here's me been feeling sorry for him. A " +
                "trickster he be indeed! Kill him fer me.",
        )

        fight.placeIthoi(access.player)

        tockKillObjections()
    }

    private suspend fun Dialogue.tockKillObjections() {
        when (
            choice3(
                "Why don't you kill him yourself?",
                1,
                "I don't believe in capital punishment.",
                2,
                "I'll be back.",
                3,
            )
        ) {
            1 -> {
                chatPlayer(quiz, "Why don't you kill him yourself?")
                chatNpc(
                    neutral,
                    "A captain shouldn't leave his ship in a time of crisis. Rule of the sea, " +
                        "that is. Even if the crisis ain't exactly on the ship.",
                )
            }
            2 -> {
                chatPlayer(neutral, "I don't believe in capital punishment.")
                chatNpc(angry, "I do.")
            }
            else -> chatPlayer(angry, "I'll be back.")
        }
    }

    private suspend fun Dialogue.tockAfterKillOrder() {
        chatNpc(quiz, WhatsHappening)
        fight.placeIthoi(access.player)
        tockKillObjections()
    }

    private suspend fun Dialogue.tockFinale() {
        chatNpc(quiz, WhatsHappening)

        val choice = choice3(KilledIthoi, 1, travelOptionText(), 2, TalkLater, 3)
        when (choice) {
            2 -> {
                chatPlayer(neutral, travelOptionText())
                access.sailAway()
                return
            }
            3 -> {
                chatPlayer(neutral, TalkLater)
                return
            }
        }

        chatPlayer(angry, KilledIthoi)
        chatNpc(
            angry,
            "Thank ye fer dealing with that serpent. I suppose the rest of my crew will feel " +
                "better now ye've proven there be no curse on them.",
        )
        chatNpc(shifty, "Most of them deleterious effects probably be psychosomatic anyway, arrr!")
        chatNpc(happy, tockThanks)
        chatNpc(happy, TockBankAccess)

        quest.completeQuest(access)
    }

    private suspend fun Dialogue.cabinBoyFerry() {
        if (access.atRimmington()) {
            chatNpc(quiz, "Hello again. Coming back to Corsair Cove?")
            val sail = choice2("Yes please.", true, "Not just now, thanks.", false)
            if (!sail) {
                chatPlayer(neutral, "Not just now, thanks.")
                return
            }
            chatPlayer(neutral, "Yes please.")
            access.sailTo(CorsairCurseRefs.CoveShipDeck, "Corsair Cove")
            return
        }

        chatNpc(
            quiz,
            "The Captain's letting me command his boat now I'm fully recovered. Did you " +
                "want to leave?",
        )

        val sail = choice2(travelOptionText(), true, TalkLater, false)
        if (!sail) {
            chatPlayer(neutral, TalkLater)
            return
        }

        chatPlayer(bored, travelOptionText())
        access.sailAway()
    }

    private val Dialogue.allCrewSpoken: Boolean
        get() =
            access.corscursNavigator >= CorsairCurseCounts.IthoiInterviewed &&
                access.corscursCabinBoy >= CorsairCurseCounts.ColinInterviewed &&
                access.corscursCook >= CorsairCurseCounts.GnocciInterviewed &&
                access.corscursThief >= CorsairCurseCounts.ArsenInterviewed

    private val Dialogue.anyCrewSpoken: Boolean
        get() =
            access.corscursNavigator >= CorsairCurseCounts.IthoiInterviewed ||
                access.corscursCabinBoy >= CorsairCurseCounts.ColinInterviewed ||
                access.corscursCook >= CorsairCurseCounts.GnocciInterviewed ||
                access.corscursThief >= CorsairCurseCounts.ArsenInterviewed

    private suspend fun ProtectedAccess.travelOp() {
        if (quest.getQuestStage(player) < CorsairCurseStage.AtTheCove) {
            return
        }
        sailAway()
    }

    private suspend fun ProtectedAccess.sailAway() {
        if (atRimmington()) {
            sailTo(CorsairCurseRefs.CoveShipDeck, "Corsair Cove")
        } else {
            sailTo(CorsairCurseRefs.RimmingtonShipDeck, "Rimmington")
        }
    }

    private suspend fun ProtectedAccess.sailTo(destination: CoordGrid, name: String) {
        try {
            mesboxNp("The ship sails to $name...")

            player.midiJingle(CorsairCurseRefs.SailingJingle)
            fadeOverlay(
                startColour = 0,
                startTransparency = 255,
                endColour = 0,
                endTransparency = 0,
                clientDuration = FadeDuration,
            )
            clearHealthHud()

            delay(TeleportDelayTicks)
            telejump(destination)

            delay(1)
            fadeOverlay(
                startColour = 0,
                startTransparency = 0,
                endColour = 0,
                endTransparency = 255,
                clientDuration = FadeDuration,
            )
            minimapReset()
            clearHealthHud()

            delay(FadeCloseTicks)
            ifCloseSub(MessageBox)
            closeFadeOverlayNow()
        } catch (cancelled: Throwable) {
            telejump(destination)
            ifCloseSub(MessageBox)
            closeFadeOverlayNow()
            throw cancelled
        }
    }

    private fun ProtectedAccess.atRimmington(): Boolean = player.coords.x > PortBoundaryX

    private fun Dialogue.travelOptionText(): String = access.travelOptionText()

    private fun ProtectedAccess.travelOptionText(): String =
        if (atRimmington()) "Please take me to Corsair Cove."
        else "Please take me back to Rimmington."

    private data class PlankCrossing(val x: Int, val plankZ: IntRange, val tiles: List<CoordGrid>) {
        fun covers(coords: CoordGrid): Boolean = coords.x == x && coords.z in plankZ

        fun route(here: CoordGrid): List<CoordGrid> = buildList {
            if (here.x != x) {
                add(CoordGrid(x, here.z, here.level))
            }
            val goingUp = here.z <= plankZ.first
            addAll(if (goingUp) tiles else tiles.reversed())
        }
    }

    private companion object {
        private val Helmsmen =
            listOf(
                CorsairCurseRefs.FerryRimmington,
                CorsairCurseRefs.FerryCoveDocks,
                CorsairCurseRefs.Captain1Op,
                CorsairCurseRefs.Captain2Ops,
                CorsairCurseRefs.CabinBoy2Ops,
            )

        private val CROSSINGS =
            listOf(
                PlankCrossing(
                    x = 2578,
                    plankZ = 2838..2839,
                    tiles =
                        listOf(
                            CoordGrid(2578, 2837, 1),
                            CoordGrid(2578, 2838, 1),
                            CoordGrid(2578, 2839, 0),
                            CoordGrid(2578, 2840, 0),
                        ),
                ),
                PlankCrossing(
                    x = 2909,
                    plankZ = 3227..3228,
                    tiles =
                        listOf(
                            CoordGrid(2909, 3226, 0),
                            CoordGrid(2909, 3227, 0),
                            CoordGrid(2909, 3228, 1),
                            CoordGrid(2909, 3229, 1),
                        ),
                ),
            )

        private const val PortBoundaryX = 2700

        private const val ReadyToSail = "Okay, I'm ready go to Corsair Cove."

        private const val AskForRelic = "Arsen says he gave you a sacred ogre relic."

        private const val RuledOutTheories = "I've ruled out all the Corsairs' theories..."

        private const val TheoriesHint = "They all have different theories about the curse."

        private const val SpokenToSome = "I've spoken to some of your crew."

        private const val DinnerLead = "You said it began after they'd all eaten."

        private const val FiringIthoi = "I hear you're thinking of firing Ithoi."

        private const val IthoiConfessed = "Ithoi admits he poisoned the crew!"

        private const val SeenIthoiRunning = "I've seen Ithoi running around. He's not sick at all."

        private const val KilledIthoi = "I've killed Ithoi for poisoning your crew."

        private const val TalkLater = "I'll talk to you later."

        private const val WhatsHappening = "So what's happening now?"

        private const val TeleportDelayTicks = 3

        private const val FadeCloseTicks = 2

        private const val FadeDuration = 50

        private const val MessageBox = "interface.messagebox"
    }
}
