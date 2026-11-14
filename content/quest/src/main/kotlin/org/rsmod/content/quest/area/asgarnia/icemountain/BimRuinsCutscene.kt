package org.rsmod.content.quest.area.asgarnia.icemountain

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.music.MusicPlayer
import org.rsmod.api.player.output.CamShakeAxis
import org.rsmod.api.player.output.stopAmbience
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.table.MusicRow
import org.rsmod.content.quest.area.misthalin.SceneCamera
import org.rsmod.content.quest.area.misthalin.SceneDriftRate
import org.rsmod.content.quest.area.misthalin.SceneFadeDuration
import org.rsmod.content.quest.area.misthalin.SceneFadeTicks
import org.rsmod.content.quest.area.misthalin.SceneRegion
import org.rsmod.content.quest.area.misthalin.SceneSnapRate
import org.rsmod.content.quest.area.misthalin.beginScene
import org.rsmod.content.quest.area.misthalin.endScene
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.game.movement.RouteRequestCoord
import org.rsmod.game.region.Region
import org.rsmod.game.region.zone.RegionZoneCopy
import org.rsmod.game.scene.SceneBuildArea
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.collision.CollisionFlagMap

/**
 * Plays the Ruins of Camdozaal entrance exactly as OSRS does: three copied map blocks, the
 * entrance, the trapped corridor and the cavern, with the player transmogged into Marley for the
 * corridor and the guardian fight armed in the cavern copy.
 */
@Singleton
class BimRuinsCutscene
@Inject
constructor(
    private val regionRepo: RegionRepository,
    private val npcRepo: NpcRepository,
    private val locRepo: LocRepository,
    private val worldRepo: WorldRepository,
    private val musicPlayer: MusicPlayer,
    private val collision: CollisionFlagMap,
    private val guardian: BimGuardian,
) {
    private val scenes = HashMap<Long, RuinsRegions>()

    suspend fun play(access: ProtectedAccess) {
        with(access) {
            val player = player
            player.bimStage = BimStage.ENTRANCE_CUTSCENE
            release(player)

            val entranceRegion = regionRepo.add(RUINS_ENTRANCE)
            entranceRegion?.let(regionRepo::protect)
            val corridorRegion = regionRepo.add(RUINS_INTERIOR)
            corridorRegion?.let(regionRepo::protect)
            val cavernRegion = regionRepo.add(RUINS_INTERIOR)
            cavernRegion?.let(regionRepo::protect)

            val regions =
                RuinsRegions(entranceRegion, corridorRegion, cavernRegion)
            player.uuid?.let { scenes[it] = regions }

            val entrance = SceneRegion(entranceRegion)
            val corridor = SceneRegion(corridorRegion)
            val cavern = SceneRegion(cavernRegion)
            val corridorArea = corridorRegion?.let { corridorBuildArea(corridor, it) }
            val cast = mutableListOf<Npc>()
            var sceneStarted = false
            var armed = false

            try {
                beginScene(
                    vantage = entrance[ENTRANCE_VANTAGE],
                    faceAt = entrance[ENTRANCE_WILLOW],
                    camera = entrance.map(ENTRANCE_OPENING),
                    underFade = {
                        player.stopAmbience()
                        cast +=
                            spawn(
                                CHECKAL_CUTSCENE,
                                entrance[ENTRANCE_CHECKAL],
                                Direction.East,
                            )
                        cast +=
                            spawn(
                                BURNOTOF_CUTSCENE,
                                entrance[ENTRANCE_BURNOTOF],
                                Direction.NorthEast,
                            )
                        cast +=
                            spawn(
                                WILLOW_CUTSCENE,
                                entrance[ENTRANCE_WILLOW],
                                Direction.West,
                            )
                        cast +=
                            spawn(
                                MARLEY_CUTSCENE,
                                entrance[ENTRANCE_MARLEY],
                                Direction.SouthEast,
                            )
                    },
                )
                sceneStarted = true

                excavation(entrance, cast)
                corridor(corridor, corridorArea, cast)
                betrayal(cavern, cast)
                regions.releaseEntranceAndCorridor(regionRepo)
                player.bimStage = BimStage.GUARDIAN_FIGHT
                player.bimCheckal = BimCheckal.INSIDE
                player.bimMarley = BimMarley.INSIDE
                player.bimBurntof = BimBurntof.INSIDE
                guardian.enterScene(cavernRegion, cavern)
                guardian.ensureSpawned(player)
                armed = true
                restoreFightPresentation()

                startDialogue {
                    chatPlayer(
                        neutral,
                        "I can't believe they ran so quickly. I suppose I should take care of " +
                            "this thing.",
                    )
                }
            } catch (t: Throwable) {
                if (sceneStarted && !armed) {
                    endScene(returnTo = null, teardown = { cast.forEach(::despawn) })
                }
                throw t
            } finally {
                if (armed) {
                    player.uuid?.let(scenes::remove)
                } else {
                    release(player)
                }
            }
        }
    }

    fun release(player: Player) {
        player.sceneBuildArea = null
        val uuid = player.uuid ?: return
        scenes.remove(uuid)?.release(regionRepo)
    }

    private fun corridorBuildArea(interior: SceneRegion, region: Region): SceneBuildArea {
        val regionBase = ZoneKey.from(region.southWest)
        val center = ZoneKey.from(interior[CORRIDOR_VANTAGE])
        val centerX = center.x - regionBase.x
        val centerZ = center.z - regionBase.z
        val base = region.southWest.translate((centerX - 6) * 8, (centerZ - 6) * 8)
        val zones = LinkedHashMap<ZoneKey, RegionZoneCopy>()
        for (level in 0 until CoordGrid.LEVEL_COUNT) {
            for (x in 0 until INTERIOR_WIDTH) {
                for (z in 0 until INTERIOR_LENGTH) {
                    val dest =
                        ZoneKey(
                            regionBase.x + INTERIOR_DEST_X + x,
                            regionBase.z + INTERIOR_DEST_Z + z,
                            level,
                        )
                    val source = ZoneKey(INTERIOR_SOURCE_X + x, INTERIOR_SOURCE_Z + z, level)
                    zones[dest] = RegionZoneCopy(source, rotation = 0, flag = null)
                }
            }
        }
        return SceneBuildArea(base, center, reload = false, zones)
    }

    private fun ProtectedAccess.restoreFightPresentation() {
        ifClose()
        showEntityOps()
        restoreLastAcceptAid()
        maxDrawDistance(false)
        minimapReset()
        camModeReset()
        showTopLevel()
        showHealthHud()
        camReset()
        openTopLevelTabs()
        syncDrawDistance()
    }

    private suspend fun ProtectedAccess.excavation(
        entrance: SceneRegion,
        cast: MutableList<Npc>,
    ) {
        val checkal = cast[0]
        val burntof = cast[1]
        val willow = cast[2]
        val marley = cast[3]
        val ladOrLass = if (isBodyTypeA()) "lad" else "lass"
        val sirOrMadam = if (isBodyTypeA()) "sir" else "madam"

        checkal.faceSquare(entrance[ENTRANCE_WILLOW])
        burntof.faceSquare(entrance[ENTRANCE_WILLOW])
        willow.faceSquare(entrance[EXCAV_PLAYER_REGROUP_FACING])
        marley.faceSquare(entrance[ENTRANCE_WILLOW])

        startDialogue {
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                neutral,
                "Right, that's everyone gathered. Thank you, ${player.displayName}, for all your " +
                    "help.",
            )
            chatPlayer(happy, "Oh it was no pro...")
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                neutral,
                "Okay everyone, we've wasted enough time. Let's crack this door open. Burntof, " +
                    "you're up!",
            )
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                neutral,
                "Try not to blow the entire mountain up this time...",
            )
            chatNpcSpecific(
                "Burntof",
                BURNOTOF_CUTSCENE,
                drunk,
                "Yesh ma'am, let'sh get this rock out the way.",
            )
        }

        burntof.walk(entrance[EXCAV_BURNOTOF_CHARGE])
        burntof.anim(DWARF_BEND_SEQ)
        delay(CHARGE_WALK_TICKS)
        burntof.faceSquare(entrance[EXCAV_CHARGE_MARKER])
        burntof.anim(DWARF_BEND_SEQ)
        delay(CHARGE_BEND_TICKS)

        fadeOut(FAST_FADE_DURATION)
        delay(SceneFadeTicks)

        checkal.telejump(entrance[EXCAV_CHECKAL_COVER])
        willow.telejump(entrance[EXCAV_WILLOW_COVER])
        marley.telejump(entrance[EXCAV_MARLEY_COVER])
        checkal.faceSquare(entrance[EXCAV_CHECKAL_COVER_FACING])
        willow.faceSquare(entrance[EXCAV_CHECKAL_REGROUP])
        marley.faceSquare(entrance[EXCAV_MARLEY_COVER_FACING])
        burntof.faceSquare(entrance[ENTRANCE_VANTAGE])
        val marker = spawn(FUSE, entrance[EXCAV_CHARGE_MARKER])
        cast += marker
        frame(entrance, ENTRANCE_COVER)

        startDialogue {
            soundSynth(DYNAMITE_PREP_1)
            chatNpcSpecific(
                "Burntof",
                BURNOTOF_CUTSCENE,
                drunk,
                "Jusht got to plashe thish here...",
            )
            soundSynth(DYNAMITE_PREP_2)
            chatNpcSpecific(
                "Burntof",
                BURNOTOF_CUTSCENE,
                drunk,
                "... little bit of dyn-*hic*-amite here...",
            )
            soundSynth(DYNAMITE_PREP_3)
            chatNpcSpecific("Burntof", BURNOTOF_CUTSCENE, drunk, "... and there we go!")
            soundSynth(DYNAMITE_FUSE)
            chatNpcSpecific("Burntof", BURNOTOF_CUTSCENE, drunk, "Now we light the fushe!")
        }

        fadeIn()
        soundSynth(DYNAMITE_HISS)

        startDialogue {
            chatNpcSpecific(
                "Burntof",
                BURNOTOF_CUTSCENE,
                drunk,
                "What are you doin' standin' there?!",
            )
            chatPlayer(quiz, "What do you mean?")
            chatNpcSpecific(
                "Burntof",
                BURNOTOF_CUTSCENE,
                drunk,
                "Everyone'sh ran for cover $ladOrLass. You'd best do the shame.",
            )
            chatPlayer(quiz, "Wait, what?!")
            soundSynth(DYNAMITE_TIMER)
            marker.telejump(entrance[EXCAV_FUSE_SHUFFLES[0]])
            chatNpcSpecific("Burntof", BURNOTOF_CUTSCENE, drunk, "Clock'sh ticking $ladOrLass!")
        }

        dolly(entrance, ENTRANCE_COVER_CREEP, rate = 1)
        burntof.walk(entrance[EXCAV_BURNOTOF_RETREAT])
        runTo(entrance[EXCAV_PLAYER_COVER])
        marker.telejump(entrance[EXCAV_FUSE_SHUFFLES[1]])
        delay(FUSE_SHUFFLE_LEAD_TICKS)

        marker.telejump(entrance[EXCAV_FUSE_SHUFFLES[2]])
        delay(BLAST_LEAD_TICKS - FUSE_SHUFFLE_LEAD_TICKS)

        addExplosion(entrance[EXCAV_BLAST[1]])
        worldRepo.soundArea(
            entrance[EXCAV_BLAST_SOUND],
            BLAST_SOUND,
            delay = BLAST_SOUND_DELAY,
            radius = BLAST_SOUND_RADIUS,
        )
        camShake(CamShakeAxis.LEFT_RIGHT, random = BLAST_SHAKE, amplitude = 0, rate = 0)
        delay(2)

        addExplosion(entrance[EXCAV_BLAST[2]])
        worldRepo.soundArea(
            entrance[EXCAV_BLAST_SOUND_P2],
            BLAST_SOUND_P2,
            delay = BLAST_SOUND_DELAY,
            radius = BLAST_SOUND_RADIUS,
        )
        delay(2)

        EXCAV_BLAST.forEach { addExplosion(entrance[it]) }
        delay(1)

        despawn(marker)
        cast.remove(marker)
        fadeOut()
        delay(SceneFadeTicks)

        telejump(entrance[EXCAV_PLAYER_REGROUP])
        faceSquare(entrance[EXCAV_PLAYER_REGROUP_FACING])
        burntof.telejump(entrance[EXCAV_BURNOTOF_REGROUP])
        burntof.faceSquare(entrance[EXCAV_PLAYER_REGROUP_FACING])
        willow.telejump(entrance[EXCAV_WILLOW_REGROUP])
        willow.faceSquare(entrance[EXCAV_CREW_REGROUP_FACING])
        marley.telejump(entrance[EXCAV_MARLEY_REGROUP])
        marley.faceSquare(entrance[EXCAV_CREW_REGROUP_FACING])
        checkal.telejump(entrance[EXCAV_CHECKAL_REGROUP])
        checkal.faceSquare(entrance[EXCAV_BURNOTOF_REGROUP])

        locRepo.findExact(entrance[EXCAV_DOOR], LocShape.CentrepieceStraight)?.let {
            locRepo.del(it, SCENE_PROP_TICKS)
        }
        locRepo.add(
            entrance[EXCAV_DOOR],
            CUTSCENE_DOOR,
            SCENE_PROP_TICKS,
            LocAngle.North,
            LocShape.CentrepieceStraight,
        )
        val rubble =
            locRepo.add(
                entrance[EXCAV_RUBBLE],
                RUBBLE,
                SCENE_PROP_TICKS,
                LocAngle.North,
                LocShape.CentrepieceStraight,
            )
        camShakeResetAll()
        camReset()
        frame(entrance, ENTRANCE_OPENING)
        fadeIn()

        startDialogue {
            chatNpcSpecific("Marley", MARLEY_CUTSCENE, angry, "Bloomin' 'eck, Burntof!")
            chatNpcSpecific(
                "Checkal",
                CHECKAL_CUTSCENE,
                neutral,
                "That was a tad too explosive I think.",
            )
            chatPlayer(shocked, "My ears are still ringing!")
            chatNpcSpecific(
                "Burntof",
                BURNOTOF_CUTSCENE,
                drunk,
                "Quit yer moanin', it did the job didnae it?",
            )
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                neutral,
                "He's right, the rock's been broken up. Checkal, it's your turn.",
            )
            chatNpcSpecific(
                "Checkal",
                CHECKAL_CUTSCENE,
                neutral,
                "Of course $sirOrMadam, I will get to work.",
            )
        }

        checkal.faceSquare(entrance[EXCAV_WILLOW_REGROUP])
        burntof.faceSquare(entrance[EXCAV_RUBBLE])
        willow.faceSquare(entrance[EXCAV_RUBBLE])
        marley.faceSquare(entrance[EXCAV_RUBBLE])
        for ((index, mark) in EXCAV_HAULING.withIndex()) {
            checkal.walk(entrance[mark])
            delay(HAUL_WALK_TICKS)
            checkal.anim(HAUL_SEQ)
            delay(HAUL_REST_TICKS)
            when (index) {
                0 -> checkal.faceSquare(entrance[EXCAV_RUBBLE])
                EXCAV_HAULING.lastIndex ->
                    checkal.faceSquare(entrance[EXCAV_WILLOW_REGROUP])
            }
        }

        fadeOut(FAST_FADE_DURATION)
        startDialogue { mesbox("Checkal sets to work clearing out the rubble...") }

        locRepo.del(rubble, SCENE_PROP_TICKS)
        fadeIn()

        startDialogue {
            chatNpcSpecific(
                "Checkal",
                CHECKAL_CUTSCENE,
                neutral,
                "The work is done, Willow. My apologies if it took too long.",
            )
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                neutral,
                "Don't be sorry, Checkal. Gotta put those muscles to use somehow.",
            )
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                neutral,
                "Alright Marley, in you go. Let us know if you find anything.",
            )
            marley.facePathingEntitySquare(player)
            chatNpcSpecific(
                "Marley",
                MARLEY_CUTSCENE,
                neutral,
                "Right, in I go. Wish me luck ${player.displayName}.",
            )
            chatPlayer(neutral, "Try not to die...")
        }
    }

    private suspend fun ProtectedAccess.corridor(
        interior: SceneRegion,
        buildArea: SceneBuildArea?,
        cast: MutableList<Npc>,
    ) {
        fadeOut()
        delay(SceneFadeTicks)

        cast.forEach(::despawn)
        cast.clear()
        if (buildArea != null) {
            player.sceneBuildArea = buildArea
            player.buildArea = buildArea.base
            player.regionRebuildPending = true
        }
        telejump(interior[CORRIDOR_VANTAGE])
        transmog(MARLEY_CUTSCENE)
        rebuildAppearance()
        faceSquare(interior[CORRIDOR_ENTRY_FACING])
        val doorway =
            locRepo.add(
                interior[CORRIDOR_DOORWAY],
                CUTSCENE_EXIT,
                SCENE_PROP_TICKS,
                LocAngle.East,
                LocShape.CentrepieceStraight,
            )
        val wires =
            CORRIDOR_TRIPWIRES.map {
                locRepo.add(
                    interior[it],
                    TRIPWIRE,
                    SCENE_PROP_TICKS,
                    LocAngle.West,
                    LocShape.CentrepieceStraight,
                )
            }
        locRepo.findExact(interior[CAVERN_BAG], LocShape.CentrepieceDiagonal)?.let {
            locRepo.del(it, SCENE_PROP_TICKS)
        }
        frame(interior, CORRIDOR_OPENING)
        fadeIn()

        startDialogue {
            chatNpcSpecific(
                "Marley",
                MARLEY_CUTSCENE,
                quiz,
                "Right, let's see what hidden surprises you have in store for me.",
            )
        }
        walkTo(interior[CORRIDOR_WALK_MID])
        delay(1)
        faceSquare(interior[CORRIDOR_DOORWAY])
        dollyBoth(interior, CORRIDOR_SETTLE, rate = 1)

        startDialogue { chatNpcSpecific("Marley", MARLEY_CUTSCENE, quiz, "Hmm...") }

        anim(SEARCH_SEQ)
        soundSynth(DISARM_TRAP)
        startDialogue {
            chatNpcSpecific(
                "Marley",
                MARLEY_CUTSCENE,
                neutral,
                "That would have been nasty.",
            )
        }

        walkTo(interior[CORRIDOR_WIRE_TILE])
        dollyBoth(interior, CORRIDOR_FOLLOW, rate = 1)
        startDialogue {
            chatNpcSpecific(
                "Marley",
                MARLEY_CUTSCENE,
                laugh,
                "Is that it? I thought this place would be better protected.",
            )
        }

        exactMove(
            start = interior[CORRIDOR_WIRE_TILE],
            end = interior[CORRIDOR_STEP_OVER],
            delay1 = STEP_OVER_DELAY_1,
            delay2 = STEP_OVER_DELAY_2,
            dir = STEP_OVER_FACE_NORTH,
            teleportType = TeleportType.Exempt,
        )
        anim(STEP_OVER_SEQ, delay = STEP_OVER_DELAY_1)
        delay(STEP_OVER_TICKS)

        startDialogue {
            faceSquare(interior[CORRIDOR_FACE_SOUTH])
            anim(HAUL_SEQ)
            soundSynth(DISARM_FAIL_1)
            chatNpcSpecific("Marley", MARLEY_CUTSCENE, quiz, "Now to disarm it...")
            anim(HAUL_SEQ)
            soundSynth(DISARM_FAIL_2)
            chatNpcSpecific("Marley", MARLEY_CUTSCENE, quiz, "Maybe this will work?")
            anim(HAUL_SEQ)
            soundSynth(DISARM_FAIL_3)
            soundSynth(DISARM_TRAP)
            chatNpcSpecific("Marley", MARLEY_CUTSCENE, quiz, "That got it!")
        }

        wires.forEach { locRepo.del(it, SCENE_PROP_TICKS) }
        locRepo.del(doorway, SCENE_PROP_TICKS)

        player.bimStage = BimStage.INSIDE_CUTSCENE
        player.bimCheckal = BimCheckal.AT_ENTRANCE
        player.bimMarley = BimMarley.AT_ENTRANCE
        player.bimBurntof = BimBurntof.AT_ENTRANCE

        startDialogue {
            chatNpcSpecific(
                "Marley",
                MARLEY_CUTSCENE,
                neutral,
                "Alright guys! It's safe to come in! I think...",
            )
        }
    }

    private suspend fun ProtectedAccess.betrayal(
        interior: SceneRegion,
        cast: MutableList<Npc>,
    ) {
        fadeOut()
        delay(SceneFadeTicks)

        resetTransmog()
        rebuildAppearance()
        player.sceneBuildArea = null
        telejump(interior[CAVERN_VANTAGE])
        player.regionRebuildPending = true
        faceSquare(interior[CAVERN_PLAYER_FACING])

        locRepo.findExact(interior[CAVERN_DOOR_RIGHT], LocShape.CentrepieceStraight)?.let {
            locRepo.del(it, SCENE_PROP_TICKS)
        }
        locRepo.add(
            interior[CAVERN_DOOR_CLOSED],
            CLOSED_DOOR,
            SCENE_PROP_TICKS,
            LocAngle.West,
            LocShape.CentrepieceStraight,
        )
        locRepo.add(
            interior[CAVERN_BAG],
            BAG_CUTSCENE,
            SCENE_PROP_TICKS,
            LocAngle.South,
            LocShape.CentrepieceDiagonal,
        )
        CAVERN_PILLARS.forEach { (coords, angle) ->
            locRepo.add(
                interior[coords],
                PILLAR_CUTSCENE,
                SCENE_PROP_TICKS,
                angle,
                LocShape.CentrepieceStraight,
            )
        }

        val checkal =
            spawn(
                CHECKAL_CUTSCENE,
                interior[CAVERN_CHECKAL],
                Direction.West,
            )
        cast += checkal
        val marley =
            spawn(
                MARLEY_CUTSCENE,
                interior[CAVERN_MARLEY],
                Direction.SouthEast,
            )
        cast += marley
        val willow =
            spawn(
                WILLOW_CUTSCENE,
                interior[CAVERN_WILLOW],
                Direction.South,
            )
        cast += willow
        val burntof =
            spawn(
                BURNOTOF_CUTSCENE,
                interior[CAVERN_BURNOTOF],
                Direction.SouthEast,
            )
        cast += burntof
        val dormant = spawn(DORMANT_GUARDIAN, interior[CAVERN_GUARDIAN_MARK])
        cast += dormant
        checkal.faceSquare(interior[CAVERN_WILLOW])
        marley.faceSquare(interior[CAVERN_FACE_CENTER])
        willow.faceSquare(interior[CAVERN_FACE_CENTER])
        burntof.faceSquare(interior[CAVERN_FACE_CENTER])
        dormant.faceSquare(interior[CAVERN_GUARDIAN_FACING])

        frame(interior, CAVERN_OPENING)
        fadeIn()

        unlockMusic(RUINS_MUSIC)
        dolly(interior, CAVERN_RISE, rate = 2)
        lookAt(interior, CAVERN_RISE, rate = 1)

        startDialogue {
            chatPlayer(shocked, "Wow, this place is amazing. I can't believe this was hidden away.")
            chatPlayer(quiz, "I wonder how long it's been left untouched for.")
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                angry,
                "You're impressed? There's nothing here but dust and fallen rocks.",
            )
            burntof.say("Yaaa*hic*aawn")
            burntof.walk(interior[CAVERN_BURNOTOF_WANDERS[0]]) {
                burntof.faceSquare(interior[CAVERN_BURNOTOF_FACE_WEST])
            }
            chatPlayer(
                happy,
                "You don't find it exciting? As an archaeologist, I thought you would be " +
                    "thrilled at this discovery?",
            )
            dolly(interior, CAVERN_DRIFT_EAST, rate = 2)
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                angry,
                "Thrilled? What's there to be excited about? I don't see any gold, diamonds, " +
                    "rare gems or piles of lost treasure!",
            )
            burntof.say("Hmm...")
            burntof.walk(interior[CAVERN_BURNOTOF_WANDERS[1]]) {
                burntof.faceSquare(interior[CAVERN_BURNOTOF_FACE_SOUTH])
            }
            chatPlayer(
                quiz,
                "It's not about the money though, right? We've potentially discovered the long " +
                    "lost ruins of an ancient civilisation!",
            )
            dolly(interior, CAVERN_DRIFT_BACK, rate = 2)
            lookAt(interior, CAVERN_DRIFT_BACK, rate = 1)
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                angry,
                "Of course it's about the money. It's always been about money, you fool.",
            )
            chatPlayer(neutral, "I don't understand...")
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                neutral,
                "We're not a team of archaeologist, you idiot. We're grave robbers looking for " +
                    "their next score.",
            )
            burntof.say("Theshe markings look familiar...")
            burntof.walk(interior[CAVERN_BURNOTOF_WANDERS[2]])
            chatPlayer(
                neutral,
                "So this entire time, you've just been using me to help break into these ruins " +
                    "in hopes of finding someone else's treasures.",
            )
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                neutral,
                "Yep, and you were surprisingly easy to convince. I'll admit, you have been a " +
                    "tremendous help, ${player.displayName}.",
            )
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                neutral,
                "But your usefulness has come to an end I'm afraid. I can't simply let you leave " +
                    "here.",
            )
            chatPlayer(worried, "Why not?")
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                neutral,
                "I can't risk you running to the authorities and telling them about our little " +
                    "gang. We're wanted in quite a few large cities across Gielinor.",
            )
            burntof.say("I've sheen thish before...")
            burntof.walk(interior[CAVERN_BURNOTOF_WANDERS[3]]) {
                burntof.faceSquare(interior[CAVERN_WILLOW])
            }
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                neutral,
                "I'm afraid this is the end of the road for you, ${player.displayName}. This " +
                    "ruin is about to become your tomb.",
            )

            willow.faceSquare(interior[CAVERN_BURNOTOF_WANDERS[3]])
            checkal.faceSquare(interior[CAVERN_BURNOTOF_WANDERS[3]])
            marley.faceSquare(interior[CAVERN_BURNOTOF_WANDERS[3]])
            soundSynth(LEVER_SOUND)
            chatNpcSpecific("Burntof", BURNOTOF_CUTSCENE, drunk, "*hic* Woops...")
            chatNpcSpecific("Willow", WILLOW_CUTSCENE, shocked, "What was that noise?")
            chatNpcSpecific("Burntof", BURNOTOF_CUTSCENE, drunk, "Uh... it wasn't me!")
        }

        dollyBoth(interior, CAVERN_QUAKE, rate = 5)
        soundSynth(DUNGEON_RUMBLE)
        CAVERN_QUAKE_FALLS.forEach { (coords, clientDelay) ->
            spotanimMap(ROCKFALL_SPOTANIM, interior[coords], delay = clientDelay)
        }
        camShake(axis = CamShakeAxis.LEFT_RIGHT, random = QUAKE_SHAKE_STRONG, amplitude = 0, rate = 0)
        delay(QUAKE_BEAT)
        camShake(axis = CamShakeAxis.LEFT_RIGHT, random = QUAKE_SHAKE_MID, amplitude = 0, rate = 0)
        delay(QUAKE_BEAT)
        camShake(axis = CamShakeAxis.LEFT_RIGHT, random = QUAKE_SHAKE_SOFT, amplitude = 0, rate = 0)
        delay(QUAKE_BEAT)

        camShakeResetAll()
        camReset()
        frame(interior, CAVERN_QUAKE)

        startDialogue {
            chatNpcSpecific("Willow", WILLOW_CUTSCENE, worried, "Whats happening?!")
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                shocked,
                "It feels like the quaking has finally stopped...",
            )
        }

        despawn(dormant)
        cast.remove(dormant)
        val waking = spawn(GUARDIAN_CUTSCENE, interior[CAVERN_GUARDIAN_MARK])
        cast += waking
        waking.faceSquare(interior[CAVERN_VANTAGE])
        waking.anim(GUARDIAN_WAKE_SEQ)
        worldRepo.soundArea(
            interior[CAVERN_GUARDIAN_MARK],
            GUARDIAN_AWAKEN,
            radius = ARENA_SOUND_RADIUS,
        )
        dolly(interior, CAVERN_GUARDIAN_REVEAL, rate = 1)
        lookAt(interior, CAVERN_GUARDIAN_REVEAL, rate = 1)
        camShake(axis = CamShakeAxis.LEFT_RIGHT, random = QUAKE_SHAKE_SOFT, amplitude = 0, rate = 0)
        delay(GUARDIAN_WAKE_TICKS)

        camShakeResetAll()
        camReset()
        frame(interior, CAVERN_GUARDIAN_SETTLE)

        willow.faceSquare(interior[CAVERN_GUARDIAN_MARK])
        startDialogue {
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                worried,
                "Holy Zamorak! What is that thing?! Checkal, take it down!",
            )
            chatNpcSpecific(
                "Checkal",
                CHECKAL_CUTSCENE,
                worried,
                "I'm a muscle man, not a fighter.",
            )
            chatNpcSpecific(
                "Marley",
                MARLEY_CUTSCENE,
                worried,
                "I think we should get out of here, Willow!",
            )
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                worried,
                "You're right, Marley. Burntof, we're leaving!",
            )
        }

        cast.forEach(::despawn)
        cast.clear()
    }

    private fun ProtectedAccess.frame(r: SceneRegion, frame: SceneCamera) {
        val mapped = r.map(frame)
        camMoveToV3(
            mapped.eye,
            height = mapped.eyeHeight,
            rate = SceneSnapRate,
            rate2 = SceneSnapRate,
        )
        camLookAtV3(
            mapped.lookAt,
            height = mapped.lookAtHeight,
            rate = SceneSnapRate,
            rate2 = SceneSnapRate,
        )
    }

    private fun ProtectedAccess.dolly(
        r: SceneRegion,
        frame: SceneCamera,
        rate: Int = SceneDriftRate,
    ) {
        val mapped = r.map(frame)
        camMoveToV3(mapped.eye, height = mapped.eyeHeight, rate = rate, rate2 = rate)
    }

    private fun ProtectedAccess.lookAt(r: SceneRegion, frame: SceneCamera, rate: Int) {
        val mapped = r.map(frame)
        camLookAtV3(mapped.lookAt, height = mapped.lookAtHeight, rate = rate, rate2 = rate)
    }

    private fun ProtectedAccess.dollyBoth(r: SceneRegion, frame: SceneCamera, rate: Int) {
        dolly(r, frame, rate)
        lookAt(r, frame, rate)
    }

    private fun ProtectedAccess.fadeOut(duration: Int = SceneFadeDuration) {
        fadeOverlay(
            startColour = 0,
            startTransparency = 255,
            endColour = 0,
            endTransparency = 0,
            clientDuration = duration,
        )
    }

    private suspend fun ProtectedAccess.fadeIn(duration: Int = SceneFadeDuration) {
        fadeOverlay(
            startColour = 0,
            startTransparency = 0,
            endColour = 0,
            endTransparency = 255,
            clientDuration = duration,
        )
        delay(SceneFadeTicks)
        closeFadeOverlayNow()
    }

    private fun addExplosion(coords: CoordGrid) {
        locRepo.add(
            coords,
            EXPLOSION,
            EXPLOSION_TICKS,
            LocAngle.West,
            LocShape.CentrepieceStraight,
        )
    }

    private fun ProtectedAccess.unlockMusic(displayName: String) {
        val row = MusicRow.all().firstOrNull { it.displayname == displayName } ?: return
        musicPlayer.unlockAndPlay(player, row)
    }

    private fun ProtectedAccess.spotanimMap(spotanim: String, coords: CoordGrid, delay: Int) {
        worldRepo.spotanimMap(
            SpotanimType(spotanim.asRSCM(RSCMType.SPOTANIM)),
            coords,
            height = 0,
            delay = delay,
        )
    }

    private fun spawn(type: String, coords: CoordGrid, facing: Direction? = null): Npc {
        val npc = Npc(type, coords)
        facing?.let { npc.respawnDir = it }
        npc.mode = NpcMode.None
        npcRepo.add(npc, SCENE_LIFESPAN_TICKS)
        return npc
    }

    private fun despawn(npc: Npc) {
        if (npc.isSlotAssigned) {
            npcRepo.del(npc, Int.MAX_VALUE)
        }
    }

    private fun Npc.telejump(coords: CoordGrid) {
        PathingEntityCommon.telejump(this, collision, coords)
    }

    private fun ProtectedAccess.walkTo(dest: CoordGrid) {
        player.routeRequest = RouteRequestCoord(dest)
        player.tempMoveSpeed = MoveSpeed.Walk
    }

    private fun ProtectedAccess.runTo(dest: CoordGrid) {
        player.routeRequest = RouteRequestCoord(dest)
        player.tempMoveSpeed = MoveSpeed.Run
    }

    private class RuinsRegions(
        private val entrance: Region?,
        private val corridor: Region?,
        private val cavern: Region?,
    ) {
        fun releaseEntranceAndCorridor(regionRepo: RegionRepository) {
            entrance?.let(regionRepo::unprotect)
            corridor?.let(regionRepo::unprotect)
        }

        fun release(regionRepo: RegionRepository) {
            entrance?.let(regionRepo::unprotect)
            corridor?.let(regionRepo::unprotect)
            cavern?.let(regionRepo::unprotect)
        }
    }

    private companion object {
        const val CHECKAL_CUTSCENE = "npc.bim_cutscene_checkal"
        const val BURNOTOF_CUTSCENE = "npc.bim_cutscene_burntof"
        const val WILLOW_CUTSCENE = "npc.bim_cutscene_willow"
        const val MARLEY_CUTSCENE = "npc.bim_cutscene_marley"
        const val DORMANT_GUARDIAN = "npc.bim_golem_boss_inactive_cutscene"
        const val GUARDIAN_CUTSCENE = "npc.bim_golem_guardian_cutscene"
        const val FUSE = "npc.bim_invisnpc"

        const val CUTSCENE_DOOR = "loc.bim_door_cutscene"
        const val RUBBLE = "loc.bim_rubble_3"
        const val CUTSCENE_EXIT = "loc.bim_exit_cutscene"
        const val TRIPWIRE = "loc.bim_tripwire"
        const val CLOSED_DOOR = "loc.bim_ruins_door_closed"
        const val BAG_CUTSCENE = "loc.bim_willows_bag_cutscene"
        const val PILLAR_CUTSCENE = "loc.bim_boss_rock_cutscene"
        const val EXPLOSION = "loc.barbassault_egg_explosion"

        const val DWARF_BEND_SEQ = "seq.dwarf_bendover"
        const val HAUL_SEQ = "seq.human_pickupfloor"
        const val SEARCH_SEQ = "seq.human_pickpocket"
        const val STEP_OVER_SEQ = "seq.regicide_stepover"
        const val GUARDIAN_WAKE_SEQ = "seq.ancient_guardian_awaken"
        const val ROCKFALL_SPOTANIM = "spotanim.rockfall"

        const val DYNAMITE_PREP_1 = "synth.bim_dynamite_preparation_01"
        const val DYNAMITE_PREP_2 = "synth.bim_dynamite_preparation_02"
        const val DYNAMITE_PREP_3 = "synth.bim_dynamite_preparation_03"
        const val DYNAMITE_FUSE = "synth.bim_dynamite_lighting_fuse_02"
        const val DYNAMITE_HISS = "synth.bim_dynamite_hiss"
        const val DYNAMITE_TIMER = "synth.bim_dynamite_timer"
        const val BLAST_SOUND = "synth.bim_wall_dynamite_explosions"
        const val BLAST_SOUND_P2 = "synth.bim_wall_dynamite_explosions_p2"
        const val DISARM_TRAP = "synth.bim_disarm_trap"
        const val DISARM_FAIL_1 = "synth.bim_disarm_trap_failure_01"
        const val DISARM_FAIL_2 = "synth.bim_disarm_trap_failure_02"
        const val DISARM_FAIL_3 = "synth.bim_disarm_trap_failure_03"
        const val LEVER_SOUND = "synth.lever"
        const val DUNGEON_RUMBLE = "synth.bim_dungeon_rumble"
        const val GUARDIAN_AWAKEN = "synth.bim_ancient_guardian_awaken"

        const val RUINS_MUSIC = "The Ruins of Camdozaal"

        const val FAST_FADE_DURATION = 15
        const val SCENE_LIFESPAN_TICKS = 600
        const val SCENE_PROP_TICKS = 6000
        const val EXPLOSION_TICKS = 4

        const val CHARGE_WALK_TICKS = 3
        const val CHARGE_BEND_TICKS = 3
        const val FUSE_SHUFFLE_LEAD_TICKS = 9
        const val BLAST_LEAD_TICKS = 13
        const val BLAST_SOUND_DELAY = 5
        const val BLAST_SOUND_RADIUS = 15
        const val BLAST_SHAKE = 75
        const val HAUL_WALK_TICKS = 4
        const val HAUL_REST_TICKS = 2
        const val STEP_OVER_DELAY_1 = 20
        const val STEP_OVER_DELAY_2 = 50
        const val STEP_OVER_FACE_NORTH = 1024
        const val STEP_OVER_TICKS = 3
        const val QUAKE_SHAKE_STRONG = 50
        const val QUAKE_SHAKE_MID = 20
        const val QUAKE_SHAKE_SOFT = 5
        const val QUAKE_BEAT = 4
        const val GUARDIAN_WAKE_TICKS = 2
        const val ARENA_SOUND_RADIUS = 10
        const val INTERIOR_SOURCE_X = 366
        const val INTERIOR_SOURCE_Z = 719
        const val INTERIOR_DEST_X = 5
        const val INTERIOR_DEST_Z = 4
        const val INTERIOR_WIDTH = 6
        const val INTERIOR_LENGTH = 6

        val ENTRANCE_VANTAGE = CoordGrid(2995, 3495)
        val ENTRANCE_CHECKAL = CoordGrid(2994, 3494)
        val ENTRANCE_BURNOTOF = CoordGrid(2996, 3492)
        val ENTRANCE_WILLOW = CoordGrid(2997, 3494)
        val ENTRANCE_MARLEY = CoordGrid(2996, 3496)
        val ENTRANCE_OPENING = SceneCamera(CoordGrid(2990, 3490), 600, CoordGrid(2997, 3494), 400)
        val ENTRANCE_COVER = SceneCamera(CoordGrid(2989, 3501), 700, CoordGrid(2997, 3494), 350)
        val ENTRANCE_COVER_CREEP =
            SceneCamera(CoordGrid(2991, 3500), 800, CoordGrid(2997, 3494), 400)

        val EXCAV_BURNOTOF_CHARGE = CoordGrid(2998, 3494)
        val EXCAV_CHARGE_MARKER = CoordGrid(2999, 3494)
        val EXCAV_FUSE_SHUFFLES =
            listOf(
                CoordGrid(3000, 3494),
                CoordGrid(3000, 3493),
                CoordGrid(3000, 3494),
            )
        val EXCAV_CHECKAL_COVER = CoordGrid(2993, 3500)
        val EXCAV_CHECKAL_COVER_FACING = CoordGrid(2993, 3498)
        val EXCAV_WILLOW_COVER = CoordGrid(2997, 3488)
        val EXCAV_MARLEY_COVER = CoordGrid(2996, 3500)
        val EXCAV_MARLEY_COVER_FACING = CoordGrid(2996, 3499)
        val EXCAV_BURNOTOF_RETREAT = CoordGrid(2994, 3489)
        val EXCAV_PLAYER_COVER = CoordGrid(2991, 3501)
        val EXCAV_BURNOTOF_REGROUP = CoordGrid(2995, 3491)
        val EXCAV_WILLOW_REGROUP = CoordGrid(2997, 3492)
        val EXCAV_MARLEY_REGROUP = CoordGrid(2995, 3495)
        val EXCAV_CHECKAL_REGROUP = CoordGrid(2994, 3493)
        val EXCAV_PLAYER_REGROUP = CoordGrid(2996, 3496)
        val EXCAV_PLAYER_REGROUP_FACING = CoordGrid(2996, 3494)
        val EXCAV_CREW_REGROUP_FACING = CoordGrid(2995, 3493)
        val EXCAV_BLAST =
            listOf(
                CoordGrid(2998, 3492),
                CoordGrid(2998, 3493),
                CoordGrid(2998, 3494),
            )
        val EXCAV_BLAST_SOUND = CoordGrid(2996, 3493)
        val EXCAV_BLAST_SOUND_P2 = CoordGrid(2995, 3492)
        val EXCAV_DOOR = CoordGrid(2999, 3493)
        val EXCAV_RUBBLE = CoordGrid(2998, 3493)
        val EXCAV_HAULING =
            listOf(
                CoordGrid(2997, 3494),
                CoordGrid(2994, 3494),
                CoordGrid(2997, 3493),
            )

        val CORRIDOR_VANTAGE = CoordGrid(2952, 5763)
        val CORRIDOR_DOORWAY = CoordGrid(2951, 5761)
        val CORRIDOR_ENTRY_FACING = CoordGrid(2952, 5765)
        val CORRIDOR_WALK_MID = CoordGrid(2952, 5764)
        val CORRIDOR_WIRE_TILE = CoordGrid(2952, 5766)
        val CORRIDOR_STEP_OVER = CoordGrid(2952, 5767)
        val CORRIDOR_FACE_SOUTH = CoordGrid(2952, 5768)
        val CORRIDOR_TRIPWIRES = listOf(CoordGrid(2952, 5766), CoordGrid(2953, 5766))
        val CORRIDOR_OPENING = SceneCamera(CoordGrid(2954, 5771), 500, CoordGrid(2952, 5764), 250)
        val CORRIDOR_SETTLE = SceneCamera(CoordGrid(2954, 5772), 500, CoordGrid(2952, 5764), 250)
        val CORRIDOR_FOLLOW = SceneCamera(CoordGrid(2954, 5773), 500, CoordGrid(2952, 5766), 250)

        val CAVERN_VANTAGE = CoordGrid(2953, 5769)
        val CAVERN_PLAYER_FACING = CoordGrid(2953, 5778)
        val CAVERN_CHECKAL = CoordGrid(2956, 5770)
        val CAVERN_MARLEY = CoordGrid(2952, 5772)
        val CAVERN_WILLOW = CoordGrid(2953, 5771)
        val CAVERN_BURNOTOF = CoordGrid(2950, 5772)
        val CAVERN_BAG = CoordGrid(2950, 5769)
        val CAVERN_DOOR_CLOSED = CoordGrid(2951, 5780)
        val CAVERN_DOOR_RIGHT = CoordGrid(2954, 5780)
        val CAVERN_GUARDIAN_MARK = CoordGrid(2952, 5778)
        val CAVERN_GUARDIAN_FACING = CoordGrid(2952, 5777)
        val CAVERN_FACE_CENTER = CoordGrid(2953, 5770)
        val CAVERN_BURNOTOF_FACE_WEST = CoordGrid(2947, 5775)
        val CAVERN_BURNOTOF_FACE_SOUTH = CoordGrid(2951, 5779)
        val CAVERN_OPENING = SceneCamera(CoordGrid(2952, 5761), 200, CoordGrid(2952, 5768), 50)
        val CAVERN_RISE = SceneCamera(CoordGrid(2952, 5763), 800, CoordGrid(2952, 5771), 50)
        val CAVERN_DRIFT_EAST = SceneCamera(CoordGrid(2959, 5768), 800, CoordGrid(2952, 5771), 50)
        val CAVERN_DRIFT_BACK = SceneCamera(CoordGrid(2952, 5763), 800, CoordGrid(2952, 5773), 70)
        val CAVERN_QUAKE = SceneCamera(CoordGrid(2946, 5768), 700, CoordGrid(2953, 5774), 100)
        val CAVERN_GUARDIAN_REVEAL =
            SceneCamera(CoordGrid(2948, 5771), 700, CoordGrid(2953, 5778), 100)
        val CAVERN_GUARDIAN_SETTLE =
            SceneCamera(CoordGrid(2948, 5771), 700, CoordGrid(2952, 5778), 150)
        val CAVERN_BURNOTOF_WANDERS =
            listOf(
                CoordGrid(2949, 5775),
                CoordGrid(2951, 5778),
                CoordGrid(2951, 5775),
                CoordGrid(2953, 5775),
            )
        val CAVERN_PILLARS =
            listOf(
                CoordGrid(2947, 5771) to LocAngle.South,
                CoordGrid(2947, 5778) to LocAngle.South,
                CoordGrid(2958, 5771) to LocAngle.North,
                CoordGrid(2958, 5778) to LocAngle.North,
            )
        val CAVERN_QUAKE_FALLS =
            listOf(
                CoordGrid(2957, 5772) to 160,
                CoordGrid(2956, 5776) to 0,
                CoordGrid(2953, 5777) to 180,
                CoordGrid(2950, 5775) to 50,
                CoordGrid(2950, 5772) to 90,
            )

        val RUINS_ENTRANCE: RegionTemplate =
            RegionTemplate.create {
                copyAllLevels(372, 435) {
                    regionZoneX = 6
                    regionZoneZ = 6
                    zoneWidth = 4
                    zoneLength = 4
                }
            }

        val RUINS_INTERIOR: RegionTemplate =
            RegionTemplate.create {
                copyAllLevels(INTERIOR_SOURCE_X, INTERIOR_SOURCE_Z) {
                    regionZoneX = INTERIOR_DEST_X
                    regionZoneZ = INTERIOR_DEST_Z
                    zoneWidth = INTERIOR_WIDTH
                    zoneLength = INTERIOR_LENGTH
                }
            }
    }
}
