package org.rsmod.content.quest.area.varrock.dragonslayer

import jakarta.inject.Inject
import org.rsmod.api.player.output.CamShakeAxis
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.content.quest.area.misthalin.SceneCamera
import org.rsmod.content.quest.area.misthalin.SceneRegion
import org.rsmod.content.quest.area.misthalin.lockedScene
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.STAGE_CRANDOR
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.STAGE_NED_HIRED
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.shipFullyFixed
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.shipOneThird
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.shipTwoThirds
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid

/**
 * The voyage to Crandor: Ned sails the Lady Lumbridge out, Elvarg attacks and the ship runs
 * aground. Played in a private copy of the ship-at-sea map. The stage moves to Crandor before
 * the scene starts, so a player who logs out mid-voyage still wakes up on the beach.
 */
class DragonSlayerVoyage
@Inject
constructor(
    private val quest: DragonSlayerQuest,
    private val regionRepo: RegionRepository,
    private val npcRepo: NpcRepository,
    private val worldRepo: WorldRepository,
) {
    suspend fun sail(access: ProtectedAccess) {
        val player = access.player
        val firstVoyage = quest.stage(player) == STAGE_NED_HIRED
        player.shipOneThird = 0
        player.shipTwoThirds = 0
        player.shipFullyFixed = 0
        if (firstVoyage) {
            quest.setStage(access, STAGE_CRANDOR)
        }
        val region = regionRepo.add(TEMPLATE)
        region?.let(regionRepo::protect)
        val scene = SceneRegion(region)
        val crew = mutableListOf<Npc>()
        try {
            access.lockedScene(
                vantage = scene[PLAYER_DECK],
                faceAt = scene[BOW],
                camera = scene.map(CAMERA),
                returnTo = CRANDOR_BEACH,
                underFade = {
                    crew += spawn(NED_CUTSCENE, scene[NED_DECK])
                    if (firstVoyage) crew += spawn(JENKINS_CUTSCENE, scene[JENKINS_DECK])
                    access.midiSong(SAILING_MUSIC)
                },
                teardown = { crew.forEach { if (it.isSlotAssigned) npcRepo.del(it, Int.MAX_VALUE) } },
            ) {
                voyage(scene, crew.firstOrNull { it.isType(JENKINS_CUTSCENE) })
            }
        } finally {
            region?.let(regionRepo::unprotect)
        }
    }

    private fun spawn(type: String, coords: CoordGrid): Npc {
        val npc = Npc(type, coords)
        npcRepo.add(npc, CREW_LIFESPAN)
        npc.movementLocked = true
        return npc
    }

    private suspend fun ProtectedAccess.voyage(scene: SceneRegion, jenkins: Npc?) {
        startDialogue {
            chatNpcSpecific(CAPTAIN_NED, NED_CUTSCENE, happy, "Ah, it's good to feel the salt spray on my face once again!")
            chatNpcSpecific(
                CAPTAIN_NED,
                NED_CUTSCENE,
                happy,
                "And this is a mighty fine ship. She don't look much, but she handles like a dream.",
            )
            chatPlayer(quiz, "How much longer until we reach Crandor?")
            chatNpcSpecific(
                CAPTAIN_NED,
                NED_CUTSCENE,
                neutral,
                "Not long now! According to the chart, we'd be able to see Crandor if it wasn't for " +
                    "those clouds on the horizon.",
            )
        }
        ifOpenOverlay(CLOUDS)
        startDialogue {
            mesbox("Clouds surround the ship.")
            if (jenkins != null) {
                chatNpcSpecific(
                    JENKINS_NAME,
                    JENKINS_CUTSCENE,
                    worried,
                    "Looks like there's a storm coming up, cap'n. Soon we won't be able to see " +
                        "anything!",
                )
            }
            chatNpcSpecific(CAPTAIN_NED, NED_CUTSCENE, sad, "Oh, well. The weather had been so good up until now.")
        }
        ifOpenOverlay(ELVARG_FLY)
        soundSynth(FLAP_WINGS)
        delay(ELVARG_PASS_TICKS)
        ifCloseSub(ELVARG_FLY)
        startDialogue {
            chatPlayer(shocked, "Did you see that?")
            chatNpcSpecific(CAPTAIN_NED, NED_CUTSCENE, confused, "See what?")
            chatPlayer(worried, "I thought I saw something up above us.")
        }
        rainFire(scene)
        startDialogue { chatNpcSpecific(CAPTAIN_NED, NED_CUTSCENE, shocked, "It's the dragon!") }
        ifOpenOverlay(ELVARG_SWOOP)
        soundSynth(DRAGON_SCREAM)
        rainFire(scene)
        if (jenkins != null) {
            jenkins.anim(JENKINS_SCARED)
            delay(1)
            jenkins.anim(JENKINS_DEATH)
            delay(JENKINS_DEATH_TICKS)
            if (jenkins.isSlotAssigned) npcRepo.del(jenkins, Int.MAX_VALUE)
        }
        ifCloseSub(ELVARG_SWOOP)
        startDialogue {
            chatNpcSpecific(CAPTAIN_NED, NED_CUTSCENE, worried, "We're going to sink!")
            chatPlayer(shocked, "Look! Land ahead!")
            chatNpcSpecific(CAPTAIN_NED, NED_CUTSCENE, shocked, "We're going to crash!")
        }
        soundSynth(CRASH_SOUND)
        camShake(CamShakeAxis.UP_DOWN, 0, CRASH_SHAKE, CRASH_SHAKE)
        ifCloseSub(CLOUDS)
        startDialogue {
            mesbox("<col=800000>CRASH!</col>")
            mesbox("You are knocked unconscious and later awake on an ash-strewn beach.")
        }
    }

    private suspend fun ProtectedAccess.rainFire(scene: SceneRegion) {
        soundSynth(DRAGON_BREATH)
        for (tile in FIRE_TILES) {
            spotanimMap(worldRepo, FIRE_SPOTANIM, scene[tile])
        }
        delay(FIRE_TICKS)
    }

    private companion object {
        val TEMPLATE: RegionTemplate =
            RegionTemplate.create {
                copyAllLevels(SHIP_ZONE_X, SHIP_ZONE_Z) {
                    regionZoneX = 4
                    regionZoneZ = 4
                    zoneWidth = 4
                    zoneLength = 3
                }
            }

        const val SHIP_ZONE_X = 235
        const val SHIP_ZONE_Z = 608

        val PLAYER_DECK = CoordGrid(1891, 4870, 1)
        val NED_DECK = CoordGrid(1893, 4870, 1)
        val JENKINS_DECK = CoordGrid(1889, 4870, 1)
        val BOW = CoordGrid(1896, 4870, 1)
        val CAMERA =
            SceneCamera(
                eye = CoordGrid(1884, 4876, 1),
                eyeHeight = 600,
                lookAt = CoordGrid(1891, 4870, 1),
                lookAtHeight = 200,
            )
        val FIRE_TILES =
            listOf(CoordGrid(1890, 4869, 1), CoordGrid(1892, 4871, 1), CoordGrid(1894, 4870, 1))

        val CRANDOR_BEACH = CoordGrid(2851, 3239, 0)

        const val NED_CUTSCENE = "npc.dragonslayer_ned_cutscene"
        const val JENKINS_CUTSCENE = "npc.dragonslayer_jenkins_cutscene"
        const val CAPTAIN_NED = "Captain Ned"
        const val JENKINS_NAME = "Cabin Boy Jenkins"
        const val CREW_LIFESPAN = 300

        const val CLOUDS = "interface.dragon_slayer_qip_clouds"
        const val ELVARG_FLY = "interface.dragon_slayer_qip_elvarg_fly"
        const val ELVARG_SWOOP = "interface.dragon_slayer_qip_elvarg"
        const val FIRE_SPOTANIM = "spotanim.dragon_slayer_qip_fire_spotanim"
        const val JENKINS_SCARED = "seq.dragon_slayer_qip_jen_scared"
        const val JENKINS_DEATH = "seq.human_death"
        const val JENKINS_DEATH_TICKS = 2
        const val ELVARG_PASS_TICKS = 4
        const val FIRE_TICKS = 2
        const val CRASH_SHAKE = 10

        const val SAILING_MUSIC = "midi.dangerous_road"
        const val FLAP_WINGS = "synth.dragonslayer_flapwings"
        const val DRAGON_SCREAM = "synth.dragonslayer_dragonscream"
        const val DRAGON_BREATH = "synth.dragonslayer_dragonbreath"
        const val CRASH_SOUND = "synth.dragonslayer_crash_ship"
    }
}
