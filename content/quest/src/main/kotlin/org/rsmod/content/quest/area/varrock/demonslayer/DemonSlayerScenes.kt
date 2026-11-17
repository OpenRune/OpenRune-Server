package org.rsmod.content.quest.area.varrock.demonslayer

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.output.CamShakeAxis
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.content.quest.area.misthalin.SceneCamera
import org.rsmod.content.quest.area.misthalin.SceneFadeDuration
import org.rsmod.content.quest.area.misthalin.SceneRegion
import org.rsmod.content.quest.area.misthalin.SceneSnapRate
import org.rsmod.content.quest.area.misthalin.lockedScene
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

interface DemonSlayerScenes {
    fun crystalBallStart(access: ProtectedAccess, aris: Npc)

    fun crystalBallGaze(aris: Npc)

    fun crystalBallEnd(access: ProtectedAccess, aris: Npc)

    suspend fun wallyFlashback(access: ProtectedAccess, quest: DemonSlayerQuest)

    suspend fun silverlightHandover(access: ProtectedAccess, prysin: Npc, quest: DemonSlayerQuest)

    suspend fun traibornRitual(access: ProtectedAccess, traiborn: Npc)

    fun releaseLeakedRegion(player: Player)
}

internal var Player.demonSlayerReturnTile by intVarp("varp.demon_slayer_return_tile")

/** Instanced copy of the stone circle south of Varrock, shared by the flashback and the fight. */
internal object StoneCircle {
    val Template: RegionTemplate =
        RegionTemplate.create {
            copy(ZONE_X, ZONE_Z, 0) {
                regionZoneX = OFFSET_ZONES
                regionZoneZ = OFFSET_ZONES
                zoneWidth = BLOCK_ZONES
                zoneLength = BLOCK_ZONES
            }
        }

    val Table = CoordGrid(3227, 3369, 0)

    fun contains(coords: CoordGrid): Boolean =
        coords.level == 0 &&
            coords.x in ZONE_X * 8 until (ZONE_X + BLOCK_ZONES) * 8 &&
            coords.z in ZONE_Z * 8 until (ZONE_Z + BLOCK_ZONES) * 8

    fun toStatic(region: Region, coords: CoordGrid): CoordGrid {
        val anchor = region.normal[Table]
        return CoordGrid(coords.x - anchor.x + Table.x, coords.z - anchor.z + Table.z, 0)
    }

    private const val ZONE_X = 400
    private const val ZONE_Z = 418
    private const val BLOCK_ZONES = 7
    private const val OFFSET_ZONES = 4
}

internal class DemonSlayerCutscenes
@Inject
constructor(
    private val regionRepo: RegionRepository,
    private val locRepo: LocRepository,
    private val worldRepo: WorldRepository,
    private val collision: CollisionFlagMap,
) : DemonSlayerScenes {

    private val sceneRegions = HashMap<Long, Region>()

    override fun crystalBallStart(access: ProtectedAccess, aris: Npc) {
        aris.faceSquare(CRYSTAL_BALL)
        aris.spotanim("spotanim.qip_ds_crystallball_spotanim_start")
        aris.anim("seq.qip_ds_attaining_crystalball")
        crystalBall()?.let { worldRepo.locAnim(it, "seq.qip_ds_crystalball_leave") }
        access.soundSynth("synth.ds_crystal_ball_start")
    }

    override fun crystalBallGaze(aris: Npc) {
        aris.spotanim("spotanim.qip_ds_crystallball_spotanim_middle")
    }

    override fun crystalBallEnd(access: ProtectedAccess, aris: Npc) {
        aris.spotanim("spotanim.qip_ds_crystallball_spotanim_end")
        aris.anim("seq.qip_ds_returning_crystalball")
        crystalBall()?.let { worldRepo.locAnim(it, "seq.qip_ds_crystalball_return") }
        access.soundSynth("synth.ds_crystal_ball_end")
    }

    private fun crystalBall() = locRepo.findAll(CRYSTAL_BALL).firstOrNull()

    override suspend fun wallyFlashback(access: ProtectedAccess, quest: DemonSlayerQuest) {
        val player = access.player
        val returnTo = player.coords
        val region = regionRepo.add(StoneCircle.Template)
        region?.let(regionRepo::protect)
        val scene = SceneRegion(region)
        player.demonSlayerReturnTile = returnTo.packed
        player.uuid?.let { uuid -> region?.let { sceneRegions[uuid] = it } }
        try {
            access.lockedScene(
                vantage = scene[WALLY_START],
                faceAt = scene[StoneCircle.Table],
                camera = scene.map(WALLY_CAMERA),
                returnTo = returnTo,
                underFade = {
                    access.transmog(WALLY)
                    access.midiSong("midi.wally_cutscene")
                },
                teardown = { access.resetTransmog() },
            ) {
                flashback(scene, quest)
            }
        } finally {
            player.demonSlayerReturnTile = 0
            player.uuid?.let(sceneRegions::remove)
            region?.let(regionRepo::unprotect)
        }
    }

    private suspend fun ProtectedAccess.flashback(scene: SceneRegion, quest: DemonSlayerQuest) {
        camShake(CamShakeAxis.UP_DOWN, 0, 10, 10)
        camShake(CamShakeAxis.PAN_LEFT_RIGHT, 0, 90, 1)
        soundSynth(RUMBLING)
        startDialogue {
            chatNpcSpecific(ARIS_TITLE, ARIS, neutral, "Wally managed to arrive at the stone " +
                "circle just as Delrith was summoned by a cult of chaos druids...")
        }
        telejump(scene[WALLY_ARRIVAL])
        camLookAt(scene[WALLY_STRIKE], height = 200, rate = 2, rate2 = 10)
        camLookAt(scene[WALLY_STRIKE], height = 100, rate = 1, rate2 = 10)
        camShakeReset(CamShakeAxis.PAN_LEFT_RIGHT)
        soundSynth(RUMBLING)
        startDialogue { chatNpcSpecificNp(WALLY_TITLE, WALLY, angry, "Die, foul demon!") }
        playerRun(scene[WALLY_STRIKE])
        delay(1)
        faceSquare(scene[StoneCircle.Table])
        anim("seq.qip_ds_wally_cutscene")
        soundSynth("synth.ds_wally_sword", delay = 10)
        delay(4)
        camMoveTo(scene[StoneCircle.Table], height = 100, rate = 2, rate2 = 10)
        camShake(CamShakeAxis.UP_DOWN, 0, 10, 5)
        camShake(CamShakeAxis.PAN_LEFT_RIGHT, 0, 2, 50)
        soundSynth(RUMBLING)
        val words = quest.incantationWords(player).joinToString("... ")
        startDialogue {
            chatNpcSpecific(WALLY_TITLE, WALLY, confused, "Now, what was that incantation again?")
            chatNpcSpecific(WALLY_TITLE, WALLY, angry, "$words!")
        }
        fadeOverlay(
            startColour = 0,
            startTransparency = 255,
            endColour = 0,
            endTransparency = 0,
            clientDuration = SceneFadeDuration,
        )
        ifCloseChat()
        delay(4)
        closeFadeOverlayNow()
        anim("seq.qip_ds_recieving_silverlight")
        spotanim("spotanim.qip_ds_sword_glint_spotanim")
        camShakeResetAll()
        soundSynth(RUMBLING)
        soundSynth("synth.equip_silverlight")
        midiJingle("jingle.quest_complete_1")
        camMoveTo(scene[WALLY_ARRIVAL], height = 500, rate = SceneSnapRate, rate2 = SceneSnapRate)
        camLookAt(scene[WALLY_STRIKE], height = 200, rate = SceneSnapRate, rate2 = SceneSnapRate)
        startDialogue {
            chatNpcSpecific(WALLY_TITLE, WALLY, happy, "I am the greatest demon slayer EVER!")
            chatNpcSpecific(ARIS_TITLE, ARIS, neutral, "By reciting the correct magical " +
                "incantation, and thrusting Silverlight into Delrith while he was newly " +
                "summoned, Wally was able to imprison Delrith in the stone block in the centre " +
                "of the circle.")
        }
    }

    override suspend fun silverlightHandover(
        access: ProtectedAccess,
        prysin: Npc,
        quest: DemonSlayerQuest,
    ) {
        access.ifCloseChat()
        prysin.walk(CASE_FRONT)
        access.delay(2)
        prysin.telejump(collision, CASE_TILE)
        access.delay(1)
        prysin.faceSquare(CASE_FACING)
        access.delay(1)
        prysin.anim("seq.demon_silverlight_open", delay = 19)
        access.soundSynth("synth.cupboard_open", delay = 19)
        access.delay(5)
        locRepo.findAll(CASE_TILE).firstOrNull { it.id == CASE.asRSCM(RSCMType.LOC) }?.let {
            worldRepo.locAnim(it, "seq.qip_ds_silverlight_case_open")
        }
        access.soundSynth("synth.casket_open")
        access.delay(3)
        if (!quest.takeKeysForSilverlight(access.player)) {
            prysin.telejump(collision, PRYSIN_HOME)
            return
        }
        prysin.anim("seq.demon_silverlight_take")
        access.delay(3)
        prysin.facePlayer(access.player)
        prysin.anim("seq.qip_ds_presenting_silverlight_start")
        prysin.transmog(checkNotNull(ServerCacheManager.getNpc(PRYSIN_HOLDING.asRSCM(RSCMType.NPC))), 3)
        access.delay(1)
        prysin.anim("seq.qip_ds_presenting_sword_middle")
        access.delay(1)
        access.anim("seq.qip_ds_recieving_silverlight")
        access.spotanim("spotanim.qip_ds_sword_glint_spotanim")
        prysin.anim("seq.qip_ds_presenting_sword_end")
        prysin.resetTransmog()
        access.soundSynth("synth.equip_silverlight")
        try {
            access.startDialogue(prysin) {
                objbox(DemonSlayerQuest.SILVERLIGHT, DemonSlayerQuest.KEY_ZOOM,
                    "Sir Prysin hands you a very shiny sword.")
                chatNpc(neutral, "That sword belonged to my great-grandfather. Make sure you " +
                    "treat it with respect!")
                chatNpc(angry, "Now go kill that demon!")
            }
        } finally {
            prysin.telejump(collision, PRYSIN_HOME)
        }
    }

    override suspend fun traibornRitual(access: ProtectedAccess, traiborn: Npc) {
        traiborn.faceSquare(WARDROBE)
        traiborn.spotanim("spotanim.qip_ds_bone_spotanim")
        traiborn.anim("seq.qip_ds_bones_wizard_anim")
        val wardrobe =
            locRepo.add(
                WARDROBE,
                "loc.qip_ds_wizards_key_wardrobe_magic",
                WARDROBE_TICKS,
                LocAngle.South,
                LocShape.CentrepieceDiagonal,
            )
        access.soundSynth("synth.ds_bone_spell")
        access.startDialogue(traiborn) {
            chatNpcSpecificNp("Wizard Traiborn", "npc.traiborn", neutral, "Wings of dark and " +
                "colour too,<br>Spreading in the morning dew;<br>Locked away I have a key;<br>" +
                "Return it now, please, unto me.")
        }
        access.soundSynth("synth.ds_cupboard_appear")
        access.delay(2)
        access.ifCloseChat()
        access.anim("seq.human_openchest")
        access.soundSynth("synth.chest_open")
        access.delay(2)
        worldRepo.locAnim(wardrobe, "seq.qip_ds_wardrobe_disappear")
        access.soundSynth("synth.ds_cupboard_dissappear")
    }

    override fun releaseLeakedRegion(player: Player) {
        val region = player.uuid?.let(sceneRegions::remove) ?: return
        regionRepo.unprotect(region)
    }

    private companion object {
        const val ARIS = "npc.aris"
        const val ARIS_TITLE = "Aris"
        const val WALLY = "npc.qip_ds_wally"
        const val WALLY_TITLE = "Wally"
        const val RUMBLING = "synth.rumbling"
        const val CASE = "loc.qip_ds_silverlight_case"
        const val PRYSIN_HOLDING = "npc.sir_prysin_silverlight"
        const val WARDROBE_TICKS = 14

        val CRYSTAL_BALL = CoordGrid(3203, 3425, 0)

        val WALLY_START = CoordGrid(3225, 3371, 0)
        val WALLY_ARRIVAL = CoordGrid(3225, 3363, 0)
        val WALLY_STRIKE = CoordGrid(3227, 3367, 0)
        val WALLY_CAMERA =
            SceneCamera(
                eye = CoordGrid(3227, 3369, 0),
                eyeHeight = 300,
                lookAt = CoordGrid(3229, 3367, 0),
                lookAtHeight = 250,
            )

        val CASE_FRONT = CoordGrid(3204, 3470, 0)
        val CASE_TILE = CoordGrid(3204, 3469, 0)
        val CASE_FACING = CoordGrid(3204, 3468, 0)
        val PRYSIN_HOME = CoordGrid(3204, 3473, 0)

        val WARDROBE = CoordGrid(3111, 3163, 1)
    }
}
