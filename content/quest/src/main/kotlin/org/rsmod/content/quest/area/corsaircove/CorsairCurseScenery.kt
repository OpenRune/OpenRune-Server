package org.rsmod.content.quest.area.corsaircove

import org.rsmod.api.config.constants
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.dialogue.mesanims
import org.rsmod.api.player.hook.SpadeDigHook
import org.rsmod.api.player.midiSong
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.content.quest.manager.Quest
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

internal class CorsairCurseScenery(
    private val quest: Quest,
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val collision: CollisionFlagMap,
    private val worldRepo: WorldRepository,
    private val cutscene: CorsairCurseCutscene,
) : SpadeDigHook {

    override fun claims(player: Player): Boolean =
        player.coords.level == CorsairCurseRefs.SandCoords.level &&
            player.coords in dollDigTiles() &&
            player.vars["varbit.corscurs_cook"] == CorsairCurseCounts.GnocciInterviewed

    override suspend fun ProtectedAccess.dig() {
        digForDoll()
    }

    fun ScriptContext.register() {
        onOpLoc1(CorsairCurseRefs.Telescope) { useTelescope() }

        onOpLocU(CorsairCurseRefs.Driftwood, CorsairCurseRefs.Tinderbox) { lightDriftwood() }

        onOpLoc1(CorsairCurseRefs.SpareTinderbox) { takeTinderbox(it.loc) }
        onOpLoc1(CorsairCurseRefs.SandWithSpade) { takeSpade(it.loc) }
    }

    private suspend fun ProtectedAccess.useTelescope() {
        arriveDelay()

        when {
            corscursCabinBoy == CorsairCurseCounts.ColinInterviewed -> observeTheOgre()
            corscursCabinBoy >= CorsairCurseCounts.ColinTelescopeSeen -> {
                startDialogue {
                    chatPlayer(
                        laugh,
                        "I've seen enough of that ogre already. Colin might be relieved to hear " +
                            "it wasn't a mermaid.",
                    )
                }
            }
            else -> mes(NoReasonToLook)
        }
    }

    private suspend fun ProtectedAccess.observeTheOgre() {
        startDialogue { chatPlayer(confused, "Let's see if I can find that mermaid he saw...") }

        anim(CorsairCurseRefs.TelescopeAnim)
        player.midiSong(CorsairCurseRefs.StopMusic, fadeOutSpeed = TelescopeMusicFade)

        cutscene.telescopeView(this)

        corscursCabinBoy = CorsairCurseCounts.ColinTelescopeSeen

        val line =
            if (player.hasCompletedBigChompy()) {
                "That doesn't look anything like a mermaid. That's Bugs, the son of that chompy " +
                    "hunter, Rantz!"
            } else {
                "That doesn't look anything like a mermaid. That's an ogre!"
            }
        startDialogue {
            chatPlayer(laugh, line)
        }

        closeFadeOverlayNow()

        checkTheoriesRuledOut()
    }

    private suspend fun ProtectedAccess.digForDoll() {
        if (corscursCook != CorsairCurseCounts.GnocciInterviewed) {
            return
        }

        faceSquare(CorsairCurseRefs.SandCoords)

        var searched = false
        startDialogue {
            chatPlayer(worried, "Okay, he said he buried the possessed doll in the sand...")
            searched =
                choice2(
                    "Search for the possessed doll and face the consequences.",
                    true,
                    "Leave the thing alone.",
                    false,
                )
        }

        if (!searched) {
            resetAnim()
            startDialogue {
                chatPlayer(neutral, "Actually, it sounds scary.")
            }
            return
        }

        corscursCook = CorsairCurseCounts.GnocciDollDug

        val line =
            if (statBase(CraftingStat) >= CraftingLevelForClockwork) {
                "That's not possessed. That sounds more like clockwork. In fact I've got the " +
                    "Crafting level to make those."
            } else {
                "That's not possessed. That sounds more like clockwork."
            }

        val doll = spawnDoll()
        soundSynth(CorsairCurseRefs.ClockworkSound)

        for ((index, dest) in DollRoute.withIndex()) {
            delay(1)

            faceSquare(doll.coords)
            doll.teleport(collision, dest)
            soundSynth(CorsairCurseRefs.ClockworkSound)

            if (index == DollLineIndex) {
                chatPlayerNoContinue(mesanims.confused, line)
            }
        }

        delay(1)
        doll.walk(DollFinalStep)
        delay(1)

        resetAnim()

        despawnDoll(doll)
        spotanimMap(worldRepo, CorsairCurseRefs.SplashSpotanim, DollSplashCoords)
        soundSynth(CorsairCurseRefs.SplashSound)

        chatPlayer(mesanims.confused, line)
    }

    private fun spawnDoll(): Npc {
        val doll = Npc(CorsairCurseRefs.Doll, GnocciScene.BurialMark)
        doll.respawnDir = Direction.NorthEast
        npcRepo.add(doll, DollLifespanTicks)
        doll.noneMode()
        doll.faceSquare(DollRoute[DollLineIndex])
        return doll
    }

    private fun despawnDoll(doll: Npc) {
        if (!doll.isSlotAssigned) {
            return
        }
        npcRepo.del(doll, Int.MAX_VALUE)
    }

    private suspend fun ProtectedAccess.lightDriftwood() {
        arriveDelay()

        val stage = quest.getQuestStage(player)
        if (stage < CorsairCurseStage.IthoiChallenged) {
            startDialogue {
                chatPlayer(
                    neutral,
                    "Maybe I should try to get more information out of him before trying to make " +
                        "his hut catch fire.",
                )
            }
            return
        }
        if (stage != CorsairCurseStage.IthoiChallenged) {
            mes(constants.dm_default, ChatType.Engine)
            return
        }

        anim(LightFireAnim)
        mes("You light the driftwood...", ChatType.Mesbox)

        delay(FireCatchTicks)
        resetAnim()
        quest.setQuestStage(this, CorsairCurseStage.DriftwoodBurning)
        soundSynth(CorsairCurseRefs.FireLitSound)

        // The stage write extinguishes the pile through its multiloc; it belongs inside the scene beat.
        cutscene.driftwoodScene(this) { quest.setQuestStage(this, CorsairCurseStage.IthoiExposed) }

        startDialogue {
            chatPlayer(
                shocked,
                "Well, I guess that proves he's well enough to get up! And apparently he knows " +
                    "some magic too.",
            )
        }
    }

    private suspend fun ProtectedAccess.takeTinderbox(loc: BoundLocInfo) {
        if (loc.coords != CorsairCurseRefs.SpareTinderboxCoords) {
            return
        }
        arriveDelay()

        anim(TakeAnim)
        delay(1)

        if (invAdd(inv, CorsairCurseRefs.Tinderbox, 1).failure) {
            mes(constants.dm_take_invspace)
            return
        }
        soundSynth(CorsairCurseRefs.TakeTinderboxSound)
    }

    private suspend fun ProtectedAccess.takeSpade(loc: BoundLocInfo) {
        if (loc.coords != CorsairCurseRefs.SandWithSpadeCoords) {
            return
        }
        arriveDelay()

        anim(TakeSpadeAnim)

        if (invAdd(inv, CorsairCurseRefs.Spade, 1).failure) {
            mes(constants.dm_take_invspace)
            return
        }
        soundSynth(CorsairCurseRefs.TakeSpadeSound)
        locRepo.change(loc, CorsairCurseRefs.SandWithoutSpade, duration = SpadeRespawnTicks)
    }

    private fun dollDigTiles(): List<CoordGrid> {
        val centre = CorsairCurseRefs.SandCoords
        return buildList {
            for (dx in -1..1) {
                for (dz in -1..1) {
                    add(CoordGrid(centre.x + dx, centre.z + dz, centre.level))
                }
            }
        }
    }

    private companion object {
        private const val NoReasonToLook = "I have no reason to do that."

        private const val TelescopeMusicFade = 50

        private const val TakeAnim = "seq.human_pickupfloor"

        private const val TakeSpadeAnim = "seq.human_pickuptable"

        private const val LightFireAnim = "seq.human_createfire"

        private val DollRoute: List<CoordGrid> =
            listOf(
                CoordGrid(2505, 2840, 0),
                CoordGrid(2506, 2841, 0),
                CoordGrid(2507, 2842, 0),
                CoordGrid(2508, 2842, 0),
                CoordGrid(2509, 2841, 0),
                CoordGrid(2509, 2840, 0),
                CoordGrid(2509, 2839, 0),
                CoordGrid(2509, 2838, 0),
            )

        private val DollFinalStep: CoordGrid = CoordGrid(2509, 2839, 0)

        private val DollSplashCoords: CoordGrid = CoordGrid(2509, 2838, 0)

        private const val DollLineIndex = 2

        private const val DollLifespanTicks = 100

        private const val FireCatchTicks = 2

        private const val SpadeRespawnTicks = 13

        private const val CraftingStat = "stat.crafting"

        private const val CraftingLevelForClockwork = 18
    }
}
