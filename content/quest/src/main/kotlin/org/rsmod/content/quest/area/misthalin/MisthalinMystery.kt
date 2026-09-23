package org.rsmod.content.quest.area.misthalin

import dev.openrune.types.MesAnimType
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onGameStartup
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onPlayerCoordsChanged
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

class MisthalinMystery
@Inject
constructor(
    private val regionRepo: RegionRepository,
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val objRepo: ObjRepository,
    private val worldRepo: WorldRepository,
    private val playerRepo: PlayerRepository,
    private val collision: CollisionFlagMap,
    private val protectedAccess: ProtectedAccessLauncher,
) :
    QuestScript(
        "quest_misthalinmystery",
        "varp.mistmyst_main",
        rewards {
            xp("stat.crafting", 600.0)
            item("obj.uncut_ruby", label = "An Uncut Ruby")
            item("obj.uncut_emerald", label = "An Uncut Emerald")
            item("obj.uncut_sapphire", label = "An Uncut Sapphire")
        },
        ItemRewardDisplay("obj.mistmyst_cutscene_knife"),
        questVarbit = "varbit.mistmyst_progress",
    ) {

    private val cutscenes =
        MisthalinCutscenes(quest, regionRepo, locRepo, npcRepo, objRepo, collision)
    private val puzzles = MisthalinPuzzles(quest)
    private val manor = MisthalinManor(quest, locRepo, cutscenes, puzzles)
    private val boss =
        MisthalinBossFight(
            quest,
            npcRepo,
            objRepo,
            locRepo,
            regionRepo,
            worldRepo,
            playerRepo,
            protectedAccess,
            collision,
        )

    override fun ScriptContext.init() {
        check(quest.maxSteps == MisthalinStage.Complete) {
            "Misthalin Mystery end state is ${quest.maxSteps} in the cache, but " +
                "MisthalinStage.Complete is ${MisthalinStage.Complete}."
        }

        cutscenes.assertSceneCoords()
        onEvent<SessionStateEvent.PrepareLogin> { restoreMisthalinLogin(player) }

        with(cutscenes) { register() }

        onOpNpc1("npc.mistmyst_abigale") { faceAndTalk(it.npc) { shore() } }

        onOpNpc1("npc.mistmyst_hewey") {
            speakerAt(it.npc)?.let { her -> faceAndTalk(her) { shore() } }
        }
        onOpNpc1("npc.mistmyst_mandy_post") { faceAndTalk(it.npc) { mandyOutside() } }

        onOpLoc1("loc.mistmyst_boat_lumbridge") { rowToIsland() }
        onOpLoc1("loc.mistmyst_boat_island") { rowToMainland() }

        onPlayerCoordsChanged { player.checkShadyFigureTrigger() }

        onGameStartup { spawnGodswordProp() }

        manor.register(this)
        puzzles.register(this)
        boss.register(this)
    }

    private fun speakerAt(clicked: Npc): Npc? =
        npcRepo.findAll(ZoneKey.from(clicked.coords), zoneRadius = 1).firstOrNull {
            it.isType("npc.mistmyst_abigale")
        }

    private suspend fun ProtectedAccess.faceAndTalk(npc: Npc, body: suspend Dialogue.() -> Unit) {
        npcPlayerFaceClose(npc)

        faceSquare(npc.coords)

        startDialogue(npc, conversation = body)
    }

    private fun spawnGodswordProp() {
        objRepo.add("obj.mistmyst_bgs_prop", GodswordProp, duration = Int.MAX_VALUE)
    }

    override fun subTitle(): String =
        "talking to <col=800000>Abigale<col=000080> by the shore in " +
            "<col=800000>Lumbridge Swamp<col=000080>."

    override fun questLog(player: ProtectedAccess): String =
        questJournal(player) {
            val stage = quest.getQuestStage(player.player)

            strike(
                "I have spoken to Abigale, she and her friends were attacked on an island just off " +
                    "the coast of Lumbridge swamp."
            )

            val onIsland = player.player.coords.isOnMisthalinIsland()

            if (stage < MisthalinStage.SidDead && !onIsland) {
                line(
                    "I agreed to sail to the island in search of the attacker, and her friends that " +
                        "were left behind. Abigale says the <col=800000>boat<col=000080> she used to " +
                        "escape can be found on the <col=800000>South-Eastern shore of Lumbridge " +
                        "swamp<col=000080>."
                )
                return@questJournal
            }

            strike(
                "Using the boat in Lumbridge swamp, I found the island where Abigale and her friends " +
                    "were attacked."
            )

            if (stage < MisthalinStage.BucketFilled) {
                line("I need to stop the killer, I'll have to find a way into the manor.")
                return@questJournal
            }

            strike(
                "I witnessed the killer claim their first victim - Sid - around the West side of the " +
                    "manor."
            )

            if (stage < MisthalinStage.HeweyDead) {
                line("I need to stop the killer, I'll have to find a way into the manor.")
                return@questJournal
            }

            strike("The killer left me a series of clues to solve, leading me to a final showdown.")
            strike(
                "I confronted the killer, and discovered that it was in fact Abigale and Hewey " +
                    "working together."
            )

            if (stage < MisthalinStage.MandyWaiting) {
                line(
                    "They turned on one another, and Hewey was killed. I should find a way to " +
                        "<col=800000>take advantage of the situation<col=000080>."
                )
                return@questJournal
            }

            strike("With help from Mandy, one of the apparent victims, I defeated the killers.")
            line("Mandy left to tend to her wounds, I should meet her outside the manor.")
        }

    override fun completedLog(player: ProtectedAccess): String =
        listOf(
                "<str>I have spoken to Abigale, she and her friends were attacked on an island " +
                    "just off the coast of Lumbridge swamp.</str>",
                "<str>Using the boat in Lumbridge swamp, I found the island where Abigale and " +
                    "her friends were attacked.</str>",
                "<str>I witnessed the killer claim their first victim - Sid - around the West " +
                    "side of the manor.</str>",
                "<str>The killer left me a series of clues to solve, leading me to a final " +
                    "showdown.</str>",
                "<str>With help from Mandy, one of the apparent victims, I defeated the " +
                    "killers.</str>",
                "<col=ff0000>QUEST COMPLETE!",
                "",
                "I've defeated the killers and been rewarded with some " +
                    "<col=800000>uncut gems<col=000080>, <col=800000>1 Quest Point<col=000080> " +
                    "and <col=800000>600 Crafting XP<col=000080>.",
            )
            .joinToString("\n")

    private suspend fun Dialogue.shore() {
        if (quest.isQuestCompleted(player)) {
            abigale(sad, "Thank you for what you did on that island.")
            return
        }

        val stage = quest.getQuestStage(player)

        if (stage >= MisthalinStage.Briefed) {
            abigale(sad, "That psycho is still out there, you have to bring them to justice.")
            return
        }

        if (stage == MisthalinStage.Accepted) {
            briefing()
            return
        }

        abigale(sad, "Help! Help!")

        val start =
            choice2("Yes.", true, "No.", false, title = "Start the Misthalin Mystery quest?")
        if (!start) {
            return
        }

        chatPlayer(confused, "What has happened here?")
        abigale(
            sad,
            "We were invited to a house party on an island not far from here... Something felt " +
                "wrong about the whole thing but we went anyway...",
        )
        abigale(
            sad,
            "The house seemed pretty creepy, but everything was going fine, we were all having a " +
                "good time...",
        )
        abigale(sad, "...and that's when we got attacked...")
        hewey(sad, "I... I tried to save her...")
        abigale(
            sad,
            "Hewey bought me enough time to find an old boat. I went back to get him but it was " +
                "too late. I'm scared that he won't make it.",
        )

        quest.setQuestStage(access, MisthalinStage.Accepted)
        abigale(sad, "You have to do something... they can't get away with this!")

        briefing()
    }

    private suspend fun Dialogue.briefing() {
        chatPlayer(confused, "What do you want me to do?")
        abigale(sad, "That psycho is still out there, you have to bring them to justice.")
        abigale(
            sad,
            "The boat we escaped on is just over there, you can use it to get to the island.",
        )
        chatPlayer(neutral, "Okay, I'll see what I can do.")
        quest.setQuestStage(access, MisthalinStage.Briefed)
    }

    private suspend fun Dialogue.abigale(mesanim: MesAnimType, text: String) =
        chatNpcSpecific("Abigale", "npc.mistmyst_abigale", mesanim, text)

    private suspend fun Dialogue.hewey(mesanim: MesAnimType, text: String) =
        chatNpcSpecific("Hewey", "npc.mistmyst_hewey", mesanim, text)

    private suspend fun ProtectedAccess.rowToIsland() {
        if (quest.isQuestNotStarted(player)) {
            mes("I have no reason to go out to that island.")
            return
        }
        row(MisthalinCoords.IslandLanding, "You row through the fog to the nearby island.")
    }

    private suspend fun ProtectedAccess.rowToMainland() {
        row(MisthalinCoords.LumbridgeShore, "You row through the fog back to Lumbridge Swamp.")
    }

    private suspend fun ProtectedAccess.row(dest: CoordGrid, arrival: String) {
        try {
            delay(BoatFadeDelay)
            fadeOverlay(
                startColour = 0,
                startTransparency = 255,
                endColour = 0,
                endTransparency = 0,
                clientDuration = 50,
            )
            delay(BoatCrossingTicks)

            spam(arrival)
            telejump(dest)
            rebuildAppearance()
            delay(BoatFadeInDelay)

            fadeOverlay(
                startColour = 0,
                startTransparency = 0,
                endColour = 0,
                endTransparency = 255,
                clientDuration = 50,
            )

            // Close the fade immediately while protected; a queued close cannot run until access is released.
            delay(FadeTicks)
            closeFadeOverlayNow()
        } catch (cancelled: Throwable) {
            closeFadeOverlayNow()
            throw cancelled
        }
    }

    private fun Player.checkShadyFigureTrigger() {
        if (coords.level != 0 || coords.x !in ShadyFigureX || coords.z !in ShadyFigureZ) {
            return
        }
        val stage = quest.getQuestStage(this)
        if (stage !in MisthalinStage.Accepted..MisthalinStage.Briefed) {
            return
        }
        protectedAccess.launch(this) { quest.setQuestStage(this, MisthalinStage.SeenShadyFigure) }
    }

    private suspend fun Dialogue.mandyOutside() {
        if (quest.isQuestCompleted(player)) {
            chatNpc(happy, "Thanks again for your help, ${player.displayName}. Stay safe.")
            chatPlayer(happy, "You too, Mandy.")
            return
        }

        chatNpc(happy, "Oh hi, ${player.displayName}.")
        chatPlayer(happy, "Hi Mandy. How are you feeling now?")
        chatNpc(happy, "Much better thanks, and lucky to be alive, thanks to you in no small part.")
        chatPlayer(happy, "Well we saved the day as a team, so thank you too.")
        chatNpc(
            happy,
            "Here, I found these in the wardrobe when I was seeing to my wound. I don't want them " +
                "so please, take them...",
        )

        access.mistmystXpAwarded = true

        quest.completeQuest(access)
    }

    private companion object {
        private const val BoatFadeDelay = 1
        private const val BoatCrossingTicks = 2
        private const val BoatFadeInDelay = 1

        /** Ticks to let a 50-client-cycle (1s) fade finish before closing the overlay. */
        private const val FadeTicks = 2

        private val ShadyFigureX = 1610..1632
        private val ShadyFigureZ = 4805..4822

        private val GodswordProp = CoordGrid(1626, 4843, 0)
    }
}

internal fun CoordGrid.isOnMisthalinIsland(): Boolean =
    level == 0 && x in 1600..1663 && z in 4800..4863
