package org.rsmod.content.quest.area.lumbridge

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.content.quest.area.misthalin.SceneCamera
import org.rsmod.content.quest.area.misthalin.SceneCreepRate
import org.rsmod.content.quest.area.misthalin.SceneRegion
import org.rsmod.content.quest.area.misthalin.lockedScene
import org.rsmod.content.quest.manager.Quest
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.proj.ProjAnim
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

internal object ReleaseScene {
    val Vantage: CoordGrid = CoordGrid(3248, 3192)

    val GhostTile: CoordGrid = CoordGrid(3248, 3193)

    val GateTile: CoordGrid = CoordGrid(3247, 3193)
    val GateOpenTile: CoordGrid = CoordGrid(3246, 3193)

    val ReturnTile: CoordGrid = CoordGrid(3247, 3193)

    val SoulWest: CoordGrid = CoordGrid(3244, 3193)
    val SoulSouth: CoordGrid = CoordGrid(3244, 3190)
    val SoulRiver: CoordGrid = CoordGrid(3253, 3181)

    val OpeningCamera: SceneCamera =
        SceneCamera(eye = CoordGrid(3252, 3193), eyeHeight = 350, lookAt = GateOpenTile, 200)

    val FollowCamera: SceneCamera =
        SceneCamera(
            eye = CoordGrid(3240, 3193),
            eyeHeight = 900,
            lookAt = CoordGrid(3243, 3191),
            lookAtHeight = 25,
        )

    val RiverCamera: SceneCamera =
        SceneCamera(
            eye = FollowCamera.eye,
            eyeHeight = FollowCamera.eyeHeight,
            lookAt = SoulRiver,
            lookAtHeight = 25,
        )
}

internal class TheRestlessGhostCutscene(
    private val quest: Quest,
    private val regionRepo: RegionRepository,
    private val npcRepo: NpcRepository,
    private val locRepo: LocRepository,
    private val worldRepo: WorldRepository,
) {

    // Release held regions on login too; removal may bypass the scene finally block.
    private val sceneRegions = HashMap<Long, Region>()

    fun ScriptContext.register() {
        onEvent<SessionStateEvent.PrepareLogin> { restoreRestlessGhostLogin(player) }
        onPlayerLogin { discardLeakedRegion(player) }
    }

    private fun discardLeakedRegion(player: Player) {
        val region = player.uuid?.let(sceneRegions::remove) ?: return
        regionRepo.unprotect(region)
    }

    suspend fun release(access: ProtectedAccess) {
        lateinit var ghost: Npc

        access.closeChat()

        access.scene(
            template = TheRestlessGhostRegions.Graveyard,
            vantage = ReleaseScene.Vantage,
            faceAt = ReleaseScene.GhostTile,
            camera = ReleaseScene.OpeningCamera,
            returnTo = ReleaseScene.ReturnTile,
            underFade = { r ->
                ghost = spawn(r[ReleaseScene.GhostTile])
                ghost.faceSquare(r[ReleaseScene.Vantage])
                openGate(r)
            },
        ) { r ->
            startDialogue {
                ghost.say(FirstSay)
                chatNpcSpecificNp(GhostTitle, GhostType, silent, ReleaseLine)
                delay(SecondSayTicks)

                ghost.say(SecondSay)
                delay(VanishTicks)

                ghost.anim(VanishAnim)
                access.soundSynth(VanishSound)
                soulFlight(r[ReleaseScene.GhostTile], r[ReleaseScene.SoulWest], SoulLegTicks30)
                delay(SoulLegTicks)

                soulFlight(r[ReleaseScene.SoulWest], r[ReleaseScene.SoulSouth], SoulLegTicks30)
                access.frame(r, ReleaseScene.FollowCamera, CaptureFrameRate)
                delay(SoulLegTicks)

                soulFlight(r[ReleaseScene.SoulSouth], r[ReleaseScene.SoulRiver], SoulRiverTicks30)
                access.camLookAtV3(
                    r[ReleaseScene.RiverCamera.lookAt],
                    height = ReleaseScene.RiverCamera.lookAtHeight,
                    rate = SceneCreepRate,
                    rate2 = SceneCreepRate,
                )
                delay(HoldTicks)
            }
        }

        quest.completeQuest(access)
        access.midiJingle(CompletionJingle)
    }

    private fun ProtectedAccess.openGate(r: SceneRegion) {
        val gate = locRepo.findExact(r[ReleaseScene.GateTile], LocShape.WallStraight)
        if (gate != null) {
            locRepo.del(gate, SceneLifespan)
        }
        locRepo.add(
            r[ReleaseScene.GateOpenTile],
            OpenGate,
            SceneLifespan,
            LocAngle.North,
            LocShape.WallStraight,
        )
    }

    private fun soulFlight(from: CoordGrid, to: CoordGrid, endTime: Int) {
        worldRepo.projAnim(
            ProjAnim(
                spotanim = SoulSpotanim.asRSCM(RSCMType.SPOTANIM),
                startHeight = SoulHeight,
                endHeight = SoulHeight,
                startTime = 0,
                endTime = endTime,
                angle = SoulAngle,
                progress = 0,
                sourceIndex = 0,
                targetIndex = 0,
                startCoord = from,
                endCoord = to,
            )
        )
    }

    private fun ProtectedAccess.frame(r: SceneRegion, camera: SceneCamera, rate: Int) {
        val mapped = r.map(camera)
        camMoveToV3(mapped.eye, height = mapped.eyeHeight, rate = rate, rate2 = rate)
        camLookAtV3(mapped.lookAt, height = mapped.lookAtHeight, rate = rate, rate2 = rate)
    }

    private suspend fun ProtectedAccess.scene(
        template: RegionTemplate,
        vantage: CoordGrid,
        faceAt: CoordGrid,
        camera: SceneCamera,
        returnTo: CoordGrid,
        underFade: suspend ProtectedAccess.(SceneRegion) -> Unit = {},
        body: suspend ProtectedAccess.(SceneRegion) -> Unit,
    ) {
        val access = this
        val region = regionRepo.add(template)
        region?.let(regionRepo::protect)
        val scene = SceneRegion(region)

        // Persist the exit on entry so the next login rebuild uses a valid world coordinate.
        player.restlessGhostReturnTile = returnTo.packed
        player.uuid?.let { uuid -> region?.let { sceneRegions[uuid] = it } }

        try {
            lockedScene(
                vantage = scene[vantage],
                faceAt = scene[faceAt],
                camera = scene.map(camera),
                returnTo = returnTo,
                underFade = { access.underFade(scene) },
            ) {
                body(scene)
            }
        } finally {
            player.restlessGhostReturnTile = 0
            player.uuid?.let(sceneRegions::remove)

            // Unprotect, never delete - the registry reclaims an unprotected small region once it
            // is inactive, so it is never pulled out from under a player still standing in it.
            region?.let(regionRepo::unprotect)
        }
    }

    private fun spawn(coords: CoordGrid): Npc {
        val npc = Npc(GhostType, coords)
        npcRepo.add(npc, SceneLifespan)
        npc.noneMode()
        return npc
    }

    private fun ProtectedAccess.closeChat() {
        ifCloseSub("interface.chat_left")
        ifCloseSub("interface.chat_right")
    }

    private companion object {

        const val GhostType = "npc.ghostx"
        const val GhostTitle = "Restless ghost"
        const val OpenGate = "loc.poordooropen"

        const val VanishAnim = "seq.restless_ghost_vanish"
        const val VanishSound = "synth.shade_sigh"
        const val SoulSpotanim = "spotanim.restless_ghost_travel_spotanim"

        const val ReleaseLine = "Release! Thank you stranger.."
        const val FirstSay = "Release! Thank you"
        const val SecondSay = "stranger.."

        const val CompletionJingle = "jingle.mastermindless"

        const val CaptureFrameRate = 100

        const val SoulHeight = 50
        const val SoulAngle = 30
        const val SoulLegTicks30 = 60
        const val SoulRiverTicks30 = 95

        const val SecondSayTicks = 4
        const val VanishTicks = 4
        const val SoulLegTicks = 2
        const val HoldTicks = 5

        const val SceneLifespan = 200
    }
}
