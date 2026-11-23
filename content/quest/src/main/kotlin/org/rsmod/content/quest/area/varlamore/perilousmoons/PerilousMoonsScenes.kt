package org.rsmod.content.quest.area.varlamore.perilousmoons

import dev.openrune.types.MoveRestrict
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.Constants
import org.rsmod.api.player.output.CamShakeAxis
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.content.quest.area.misthalin.SceneCamera
import org.rsmod.content.quest.area.misthalin.SceneRegion
import org.rsmod.content.quest.area.misthalin.lockedScene
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.isZoneValid
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

@Singleton
class PerilousMoonsScenes
@Inject
constructor(
    private val regionRepo: RegionRepository,
    private val npcRepo: NpcRepository,
    private val locRepo: LocRepository,
    private val worldRepo: WorldRepository,
    private val collision: CollisionFlagMap,
) {
    suspend fun blacksmith(access: ProtectedAccess) {
        access.ifCloseChat()
        lateinit var smith: Npc
        access.privateScene(
            template = FORGE_TEMPLATE,
            vantage = FORGE_PLAYER,
            faceAt = FORGE_SMITH,
            camera = FORGE_CAMERA,
            returnTo = FORGE_PLAYER,
            setup = { scene, cast ->
                smith = cast.spawn(PmoonNpc.BLACKSMITH_CUTSCENE, scene[FORGE_SMITH])
                smith.faceSquare(scene[FORGE_PLAYER])
                for ((type, at, facing) in FORGE_WORKERS) {
                    cast.spawn(type, scene[at], wander = true).faceSquare(scene[facing])
                }
                for (prop in FORGE_PROPS) {
                    locRepo.add(scene[prop.coords], prop.type, SCENE_LIFESPAN, prop.angle, prop.shape)
                }
            },
        ) { scene, _ ->
            player.faceNpc(smith)
            delay(2)
            smith.walk(listOf(scene[FORGE_SMITH_EAST], scene[FORGE_FURNACE_STAND]))
            delay(4)
            smith.faceSquare(scene[FORGE_FURNACE])
            delay(2)
            repeat(FURNACE_PUMPS) {
                smith.anim("seq.human_furnace")
                soundSynth("synth.furnace")
                delay(2)
            }
            delay(1)
            smith.walk(listOf(scene[FORGE_SMITH_WEST], scene[FORGE_ANVIL_STAND]))
            delay(6)
            smith.faceSquare(scene[FORGE_ANVIL])
            delay(1)
            hammer(smith, scene, FORGE_SPARKS_FIRST, "synth.anvil01")
            delay(4)
            hammer(smith, scene, FORGE_SPARKS_SECOND, "synth.anvil03")
            delay(5)
            smith.walk(listOf(scene[FORGE_SMITH_WEST], scene[FORGE_SMITH]))
            delay(1)
        }
        access.player.resetFaceEntity()
    }

    private fun ProtectedAccess.hammer(
        smith: Npc,
        scene: SceneRegion,
        sparks: List<CoordGrid>,
        synth: String,
    ) {
        smith.anim("seq.human_smithing_noreplace")
        for ((index, coords) in sparks.withIndex()) {
            spotanimMap(worldRepo, "spotanim.levelup_anim", scene[coords], delay = SPARK_DELAYS[index])
        }
        soundSynth(synth)
    }

    suspend fun eclipseMoon(access: ProtectedAccess) {
        access.ifCloseChat()
        val returnTo = access.player.coords
        access.privateScene(
            template = ECLIPSE_TEMPLATE,
            vantage = ECLIPSE_PLAYER,
            faceAt = ECLIPSE_MOON_CENTRE,
            camera = ECLIPSE_CAMERAS.first(),
            returnTo = returnTo,
        ) { scene, cast ->
            val moon = cast.spawn(PmoonNpc.ECLIPSE_STAND_IN, scene[ECLIPSE_MOON])
            val zuma = cast.spawn(PmoonNpc.ZUMA_CUTSCENE, scene[ECLIPSE_ZUMA_START])
            soundSynth("synth.varlamore_nagua_cutscene_hit_01")
            zuma.walk(scene[ECLIPSE_ZUMA_STOP])
            cutTo(scene, ECLIPSE_CAMERAS[1])
            startDialogue { eclipseZumaApproaches() }
            cutTo(scene, ECLIPSE_CAMERAS[2])
            zuma.say("I am Zuma, a loyal ser-")
            delay(2)
            moon.anim("seq.npc_djinn_02_melee")
            delay(1)
            zuma.telejump(collision, scene[ECLIPSE_ZUMA_THROWN])
            zuma.say("Argh!")
            soundSynth("synth.myarm_flyback")
            soundSynth("synth.human_hit")
            cutTo(scene, ECLIPSE_CAMERAS[3])
            say("Zuma!")
            delay(2)
            zuma.say("Ahhh!")
            startDialogue { eclipseZumaFlees() }
            zuma.walk(scene[ECLIPSE_ZUMA_START])
            delay(2)
        }
    }

    /**
     * Eyatlalli rises with her 3x3 footprint centred on [centre]; a first meeting throws the player
     * clear to the tile off her south-west corner.
     */
    suspend fun eyatlalliAppears(access: ProtectedAccess, centre: CoordGrid, thrown: Boolean) {
        with(access) {
            val corner = centre.translate(-1, -1)
            val landing = corner.translate(-1, -1)
            if (thrown && collision.isZoneValid(landing)) {
                anim("seq.human_troll_flyback_merge")
                exactMove(player.coords, landing, 0, THROW_CYCLES, Constants.em_face_north)
            }
            val eyatlalli = Npc(PmoonNpc.EYATLALLI_DISCOVERED, corner)
            npcRepo.add(eyatlalli, DISCOVERY_LIFESPAN)
            eyatlalli.noneMode()
            eyatlalli.faceSquare(landing)
            eyatlalli.anim("seq.npc_djinn_05_spawn")
            soundSynth("synth.altar_crystal_chime_2")
            soundSynth("synth.myarm_flyback")
            delay(2)
            faceSquare(centre)
            try {
                startDialogue(eyatlalli) {
                    eyatlalliDiscovered()
                    access.shake()
                    icosahedronStirs()
                }
                eyatlalli.anim("seq.npc_djinn_05_fadeout")
                delay(FADEOUT_TICKS)
            } finally {
                if (eyatlalli.isSlotAssigned) npcRepo.del(eyatlalli, Int.MAX_VALUE)
            }
        }
    }

    suspend fun ProtectedAccess.shake() {
        for (axis in SHAKE_AXES) camShake(axis, random = 8, amplitude = 0, rate = 0)
        soundSynth("synth.rumbling", loops = RUMBLE_LOOPS)
        delay(SHAKE_TICKS)
        camShakeResetAll()
    }

    private suspend fun ProtectedAccess.privateScene(
        template: RegionTemplate,
        vantage: CoordGrid,
        faceAt: CoordGrid,
        camera: SceneCamera,
        returnTo: CoordGrid,
        setup: ProtectedAccess.(SceneRegion, Cast) -> Unit = { _, _ -> },
        body: suspend ProtectedAccess.(SceneRegion, Cast) -> Unit,
    ) {
        val region = regionRepo.add(template)
        region?.let(regionRepo::protect)
        val scene = SceneRegion(region)
        val cast = Cast()
        try {
            lockedScene(
                vantage = scene[vantage],
                faceAt = scene[faceAt],
                camera = scene.map(camera),
                returnTo = returnTo,
                underFade = { setup(scene, cast) },
                teardown = cast::clear,
            ) {
                body(scene, cast)
            }
        } finally {
            cast.clear()
            region?.let(regionRepo::unprotect)
        }
    }

    private fun ProtectedAccess.cutTo(scene: SceneRegion, camera: SceneCamera) {
        val mapped = scene.map(camera)
        camMoveToV3(mapped.eye, camera.eyeHeight, camera.rate, camera.rate2)
        camLookAtV3(mapped.lookAt, camera.lookAtHeight, camera.rate, camera.rate2)
    }

    private inner class Cast {
        private val npcs = mutableListOf<Npc>()

        fun spawn(type: String, coords: CoordGrid, wander: Boolean = false): Npc {
            val npc = Npc(type, coords)
            npcRepo.add(npc, SCENE_LIFESPAN)
            if (!wander) {
                npc.noneMode()
                npc.moveRestrict = MoveRestrict.PassThru
            }
            npcs += npc
            return npc
        }

        fun clear() {
            for (npc in npcs) {
                if (npc.isSlotAssigned) npcRepo.del(npc, Int.MAX_VALUE)
            }
            npcs.clear()
        }
    }

    private class Prop(
        val type: String,
        val coords: CoordGrid,
        val angle: LocAngle,
        val shape: LocShape = LocShape.CentrepieceStraight,
    )

    private companion object {
        private const val SCENE_LIFESPAN = 400
        private const val DISCOVERY_LIFESPAN = 500
        private const val FADEOUT_TICKS = 2
        private const val THROW_CYCLES = 60
        private const val SHAKE_TICKS = 4
        private const val RUMBLE_LOOPS = 90
        private const val SLOW_RATE = 3
        private const val FURNACE_PUMPS = 2
        private val SHAKE_AXES =
            listOf(CamShakeAxis.LEFT_RIGHT, CamShakeAxis.UP_DOWN, CamShakeAxis.FORWARDS_BACKWARDS)

        private val FORGE_TEMPLATE =
            RegionTemplate.create {
                copyAllLevels(176, 1192) {
                    zoneWidth = 11
                    zoneLength = 9
                }
            }
        private val FORGE_PLAYER = CoordGrid(1448, 9582, 1)
        private val FORGE_SMITH = CoordGrid(1448, 9583, 1)
        private val FORGE_SMITH_EAST = CoordGrid(1451, 9583, 1)
        private val FORGE_FURNACE_STAND = CoordGrid(1452, 9584, 1)
        private val FORGE_FURNACE = CoordGrid(1453, 9584, 1)
        private val FORGE_SMITH_WEST = CoordGrid(1447, 9584, 1)
        private val FORGE_ANVIL_STAND = CoordGrid(1446, 9585, 1)
        private val FORGE_ANVIL = CoordGrid(1446, 9586, 1)
        private val FORGE_SPARKS_FIRST =
            listOf(CoordGrid(1447, 9586, 1), CoordGrid(1445, 9585, 1), CoordGrid(1445, 9587, 1))
        private val FORGE_SPARKS_SECOND =
            listOf(CoordGrid(1444, 9585, 1), CoordGrid(1447, 9585, 1), CoordGrid(1447, 9586, 1))
        private val SPARK_DELAYS = listOf(0, 12, 21)
        private val FORGE_WORKERS =
            listOf(
                Triple(
                    "npc.cam_torum_worker_f_2",
                    CoordGrid(1446, 9585, 1),
                    CoordGrid(1444, 9583, 1),
                ),
                Triple(
                    "npc.cam_torum_worker_m_1",
                    CoordGrid(1450, 9586, 1),
                    CoordGrid(1450, 9584, 1),
                ),
            )
        private val FORGE_PROPS =
            listOf(
                Prop("loc.cam_torum_anvil_noop", CoordGrid(1446, 9582, 1), LocAngle.West),
                Prop("loc.cam_torum_anvil_noop", CoordGrid(1446, 9586, 1), LocAngle.West),
                Prop("loc.cam_torum_sacred_forge_noop", CoordGrid(1453, 9582, 1), LocAngle.North),
                Prop("loc.dungeonkit_imcando02_door01_2x1_noop", CoordGrid(1448, 9588, 1), LocAngle.East),
            )
        private val FORGE_CAMERA =
            SceneCamera(
                eye = CoordGrid(1448, 9580, 1),
                eyeHeight = 1400,
                lookAt = CoordGrid(1448, 9581, 1),
                lookAtHeight = 10,
            )

        private val ECLIPSE_TEMPLATE =
            RegionTemplate.create {
                copyAllLevels(182, 1200) {
                    zoneWidth = 7
                    zoneLength = 7
                }
            }
        private val ECLIPSE_PLAYER = CoordGrid(1478, 9632)
        private val ECLIPSE_MOON = CoordGrid(1486, 9630)
        private val ECLIPSE_MOON_CENTRE = CoordGrid(1488, 9632)
        private val ECLIPSE_ZUMA_START = CoordGrid(1481, 9632)
        private val ECLIPSE_ZUMA_STOP = CoordGrid(1485, 9632)
        private val ECLIPSE_ZUMA_THROWN = CoordGrid(1482, 9632)
        private val ECLIPSE_CAMERAS =
            listOf(
                SceneCamera(CoordGrid(1478, 9632), 875, CoordGrid(1493, 9632), 750),
                SceneCamera(CoordGrid(1475, 9632), 875, CoordGrid(1493, 9632), 750, SLOW_RATE, SLOW_RATE),
                SceneCamera(CoordGrid(1473, 9632), 800, CoordGrid(1493, 9632), 750, SLOW_RATE, SLOW_RATE),
                SceneCamera(CoordGrid(1469, 9632), 1200, CoordGrid(1488, 9632), 750, SLOW_RATE, SLOW_RATE),
            )
    }
}
