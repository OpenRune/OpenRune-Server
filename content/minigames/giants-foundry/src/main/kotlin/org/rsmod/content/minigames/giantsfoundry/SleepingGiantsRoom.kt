package org.rsmod.content.minigames.giantsfoundry

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.instances.InstanceAttributes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

internal const val DROP_TRIGGER = "droptrigger.sleeping_giants_room"

/**
 * The foundry as Sleeping Giants first shows it: a private copy of the real foundry (region
 * 52_179) with the stations swapped for their `_quest` versions (broken tools, and inspect-only
 * crucible, jig, lava pool, waterfall, storage, crate and chest). The cache places none of the
 * `_quest` locs on any map, so the copy is built per player. The wiki calls it an instance whose
 * dropped items are lost. https://oldschool.runescape.wiki/w/Sleeping_Giants
 */
@Singleton
class SleepingGiantsRoom
@Inject
constructor(
    private val regionRepo: RegionRepository,
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
) {
    private val rooms = HashMap<Player, Room>()

    private class Room(val region: Region) {
        var kovac: Npc? = null
    }

    fun kovacOf(player: Player): Npc? = rooms[player]?.kovac

    fun isInside(player: Player): Boolean =
        rooms[player]?.let { regionRepo.isValid(it.region) && player.coords in it.region } == true

    /**
     * Builds the room and moves the player in under a fade. With [intro] it opens as the quest's
     * cutscene: the cache's `_cutscene` locs (which have no ops) and Kovac's cutscene npc; [endIntro]
     * then swaps in the `_quest` locs. [onArrival] runs on the tick the player is moved in.
     */
    suspend fun enter(access: ProtectedAccess, intro: Boolean, onArrival: () -> Unit): Boolean {
        val player = access.player
        rooms.remove(player)?.let { discard(player, it) }
        val region = regionRepo.add(TEMPLATE) ?: return false
        regionRepo.protect(region)
        try {
            val room = Room(region)
            rooms[player] = room
            armDropWarning(player)
            if (intro) {
                playIntro(access, room, onArrival)
            } else {
                arrive(access, room, onArrival)
            }
        } finally {
            regionRepo.unprotect(region)
        }
        return true
    }

    private suspend fun arrive(access: ProtectedAccess, room: Room, onArrival: () -> Unit) {
        val region = room.region
        for (swap in SWAPS) {
            swapLoc(region, swap.coords, swap.real, swap.quest)
        }
        addQuestLocs(region)
        room.kovac = spawnQuestKovac(region)
        access.fadeOverlay(0, 255, 0, 0, FADE_CYCLES)
        access.delay(2)
        onArrival()
        access.telejump(region.normal[ARRIVAL])
        access.delay(1)
        access.fadeIn()
    }

    private suspend fun playIntro(access: ProtectedAccess, room: Room, onArrival: () -> Unit) {
        val region = room.region
        try {
            access.mesboxNp(" ")
            access.fadeOverlay(0, 255, 0, 0, FADE_CYCLES)
            access.clearHealthHud()
            access.delay(2)
            onArrival()
            access.camModeFixed()
            access.hideTopLevel()
            access.hideEntityOps()
            access.minimapHideMap()
            access.hideHealthHud()
            access.closeTopLevelTabsLenient()
            access.telejump(region.normal[INTRO_ENTRANCE_TILE])
            access.delay(1)
            room.kovac = spawnKovac(region, KOVAC_CUTSCENE, KOVAC_TILE, Direction.South)
            for (swap in SWAPS) {
                swapLoc(region, swap.coords, swap.real, swap.cutscene)
            }
            swapLoc(region, EXIT_TILE, EXIT, EXIT_CUTSCENE)
            for (tile in POWER_UNIT_TILES) {
                addLoc(region, tile, POWER_UNIT_QUEST)
            }
            access.camera(region, INTRO_SHOTS[0])
            access.delay(1)
            access.camera(region, INTRO_SHOTS[1])
            access.fadeOverlay(0, 0, 0, 255, FADE_CYCLES)
            access.delay(1)
            access.walk(region.normal[INTRO_PLAYER_TILE])
            access.delay(4)
            access.camera(region, INTRO_SHOTS[2])
            access.delay(8)
            access.anim(THINK_SEQ)
            access.camera(region, INTRO_SHOTS[3])
            access.delay(8)
            access.anim(THINK_SEQ)
            access.camera(region, INTRO_SHOTS[4])
            access.delay(8)
        } catch (t: Throwable) {
            endIntroNow(access)
            throw t
        }
    }

    /** The player walks back towards Kovac after taking in the room. */
    suspend fun introWalkBack(access: ProtectedAccess) {
        val region = rooms[access.player]?.region ?: return
        access.walk(region.normal[INTRO_RETURN_TILE])
        access.camera(region, INTRO_SHOTS[5])
        access.mesboxNp(" ")
        access.delay(2)
    }

    fun kovacLooksAtTools(player: Player) {
        val room = rooms[player] ?: return
        room.kovac?.faceSquare(room.region.normal[INTRO_TOOLS_TILE])
    }

    fun kovacToTripHammer(player: Player) {
        val room = rooms[player] ?: return
        val kovac = room.kovac ?: return
        val region = room.region
        kovac.walk(KOVAC_TRIP_HAMMER_PATH.map { region.normal[it] }) {
            kovac.faceSquare(region.normal[TRIP_HAMMER_FACE_TILE])
        }
    }

    /** Ends the cutscene: the quest's locs and Kovac replace the cutscene ones. */
    suspend fun endIntro(access: ProtectedAccess) {
        val room = rooms[access.player] ?: return
        try {
            access.mesboxNp(" ")
            access.fadeOverlay(0, 255, 0, 0, FADE_CYCLES)
            access.delay(2)
            access.restoreSceneState()
            swapToQuest(room)
            access.camReset()
            access.openTopLevelTabs()
            access.camForceAngle(INTRO_END_CAM_ANGLE, 0)
            access.delay(1)
            access.fadeOverlay(0, 0, 0, 255, FADE_CYCLES)
            access.ifCloseSub(MESSAGEBOX)
            access.delay(2)
            access.closeFadeOverlayNow()
            room.kovac?.facePlayer(access.player)
        } catch (t: Throwable) {
            endIntroNow(access)
            throw t
        }
    }

    /** [endIntro] without the fade, for when the cutscene is cut short. */
    fun endIntroNow(access: ProtectedAccess) {
        rooms[access.player]?.let { swapToQuest(it) }
        access.restoreSceneState()
        access.camReset()
        access.openTopLevelTabs()
        access.closeFadeOverlayNow()
    }

    private fun ProtectedAccess.restoreSceneState() {
        showEntityOps()
        minimapReset()
        camModeReset()
        showTopLevel()
        showHealthHud()
    }

    private fun swapToQuest(room: Room) {
        val current = room.kovac
        if (current != null && current.type.id == KOVAC.asRSCM(RSCMType.NPC)) {
            return
        }
        for (swap in SWAPS) {
            swapLoc(room.region, swap.coords, swap.cutscene, swap.quest)
        }
        swapLoc(room.region, EXIT_TILE, EXIT_CUTSCENE, EXIT)
        for ((tile, loc) in QUEST_ONLY_LOCS) {
            addLoc(room.region, tile, loc)
        }
        current?.let { npcRepo.del(it, Int.MAX_VALUE) }
        room.kovac = spawnQuestKovac(room.region)
    }

    private fun addQuestLocs(region: Region) {
        for (tile in POWER_UNIT_TILES) {
            addLoc(region, tile, POWER_UNIT_QUEST)
        }
        for ((tile, loc) in QUEST_ONLY_LOCS) {
            addLoc(region, tile, loc)
        }
    }

    private fun spawnQuestKovac(region: Region): Npc {
        val kovac = spawnKovac(region, KOVAC, KOVAC_TILE, Direction.North)
        kovac.faceSquare(region.normal[KOVAC_FACE_TILE])
        return kovac
    }

    /** Kovac stays put in the room so the player can find him to talk. */
    private fun spawnKovac(region: Region, type: String, tile: CoordGrid, dir: Direction): Npc {
        val kovac = Npc(type, region.normal[tile])
        kovac.respawnDir = dir
        npcRepo.add(kovac, Int.MAX_VALUE)
        kovac.noneMode()
        return kovac
    }

    /**
     * Moves the player from the room to the same tile of the real foundry, once the tools are
     * repaired.
     */
    suspend fun moveToFoundry(access: ProtectedAccess) {
        val room = rooms[access.player] ?: return
        access.mesboxNp(" ")
        access.fadeOverlay(0, 255, 0, 0, FADE_CYCLES)
        access.clearHealthHud()
        access.delay(1)
        access.minimapHideMap()
        access.delay(1)
        rooms.remove(access.player)
        access.telejump(room.realCoords(access.player.coords))
        discard(access.player, room)
        access.delay(1)
        access.minimapReset()
        access.fadeOverlay(0, 0, 0, 255, FADE_CYCLES)
        access.ifCloseSub(MESSAGEBOX)
        access.delay(2)
        access.closeFadeOverlayNow()
    }

    /** Forgets the room after the player walks out of it. */
    fun left(player: Player) {
        rooms.remove(player)?.let { discard(player, it) }
    }

    /** A player who logs out in the room logs back in outside the cave. */
    fun logout(player: Player) {
        val room = rooms.remove(player) ?: return
        if (player.coords in room.region) {
            player.attr[InstanceAttributes.LOGIN_EXIT_COORD] = OUTSIDE.packed
        }
        discard(player, room)
    }

    private fun discard(player: Player, room: Room) {
        room.kovac?.let { npcRepo.del(it, Int.MAX_VALUE) }
        if (player.dropTrigger == DROP_TRIGGER) {
            player.clearDropTrigger(DROP_TRIGGER)
        }
        warned.remove(player)
    }

    /*
     * Dropping in the room: since 25 September 2024 the quest warns about dropping items in the
     * foundry, because the room's floor items are lost once the player leaves it.
     * https://oldschool.runescape.wiki/w/Update:Varlamore:_The_Rising_Darkness_is_OUT_NOW
     * The first drop only warns (the trigger re-arms, which blocks it); dropping again goes
     * through, and the trigger re-arms for the next item.
     */

    private val warned = HashSet<Player>()

    fun armDropWarning(player: Player) {
        if (player.dropTrigger == null) {
            player.dropTrigger(DROP_TRIGGER)
        }
    }

    /** Handles the drop trigger; true when the drop should be blocked. */
    fun onDropAttempt(player: Player): Boolean {
        if (player in warned) {
            warned.remove(player)
            return false
        }
        warned += player
        return true
    }

    fun onDropped(player: Player) {
        if (isInside(player)) {
            armDropWarning(player)
        }
    }

    private fun Room.realCoords(coords: CoordGrid): CoordGrid =
        CoordGrid(
            coords.x - region.southWest.x - REGION_ZONE_OFFSET * ZONE_LENGTH + COPY_ORIGIN.x,
            coords.z - region.southWest.z - REGION_ZONE_OFFSET * ZONE_LENGTH + COPY_ORIGIN.z,
            coords.level,
        )

    private operator fun Region.contains(coords: CoordGrid): Boolean =
        coords.x in southWest.x until southWest.x + SMALL_REGION_LENGTH &&
            coords.z in southWest.z until southWest.z + SMALL_REGION_LENGTH

    private fun swapLoc(region: Region, tile: CoordGrid, from: String, into: String) {
        val coords = region.normal[tile]
        val fromId = from.asRSCM(RSCMType.LOC)
        val loc = locRepo.findAll(coords).firstOrNull { it.id == fromId } ?: return
        locRepo.add(coords, into, Int.MAX_VALUE, loc.angle, loc.shape)
    }

    private fun addLoc(region: Region, tile: CoordGrid, loc: String) {
        locRepo.add(region.normal[tile], loc, Int.MAX_VALUE, LocAngle.West, LocShape.CentrepieceStraight)
    }

    private suspend fun ProtectedAccess.fadeIn() {
        fadeOverlay(0, 0, 0, 255, FADE_CYCLES)
        delay(FADE_TICKS)
        closeFadeOverlayNow()
    }

    private fun ProtectedAccess.camera(region: Region, shot: Shot) {
        camMoveToV3(region.normal[shot.eye], height = shot.eyeHeight, rate = shot.eyeRate, rate2 = shot.eyeRate)
        camLookAtV3(region.normal[shot.lookAt], height = shot.lookAtHeight, rate = shot.lookAtRate, rate2 = shot.lookAtRate)
    }

    private class Shot(
        val eye: CoordGrid,
        val eyeHeight: Int,
        val eyeRate: Int,
        val lookAt: CoordGrid,
        val lookAtHeight: Int,
        val lookAtRate: Int,
    )

    private class LocSwap(
        val real: String,
        val coords: CoordGrid,
        val cutscene: String,
        val quest: String,
    )

    private companion object {
        const val KOVAC = "npc.giants_foundry_kovac_quest"
        val KOVAC_TILE = CoordGrid(3368, 11483, 0)
        val KOVAC_FACE_TILE = CoordGrid(3369, 11487, 0)
        val KOVAC_TRIP_HAMMER_PATH = listOf(CoordGrid(3368, 11490, 0), CoordGrid(3365, 11493, 0))
        val TRIP_HAMMER_FACE_TILE = CoordGrid(3365, 11497, 0)

        val ARRIVAL = FOUNDRY_INSIDE

        // UNVERIFIED: the tile outside the cave exit (guess)
        val OUTSIDE = CoordGrid(3361, 3149, 0)

        /** South-west corner of map square 52_179, the copied block. */
        val COPY_ORIGIN = CoordGrid(3328, 11456, 0)
        const val REGION_ZONE_OFFSET = 4
        const val ZONE_LENGTH = 8
        const val SQUARE_ZONES = 8
        const val SMALL_REGION_LENGTH = 128

        val TEMPLATE: RegionTemplate =
            RegionTemplate.create {
                copyAllLevels(COPY_ORIGIN.x / ZONE_LENGTH, COPY_ORIGIN.z / ZONE_LENGTH) {
                    regionZoneX = REGION_ZONE_OFFSET
                    regionZoneZ = REGION_ZONE_OFFSET
                    zoneWidth = SQUARE_ZONES
                    zoneLength = SQUARE_ZONES
                }
            }

        const val KOVAC_CUTSCENE = "npc.giants_foundry_kovac_quest_cutscene"
        const val EXIT = "loc.giants_foundry_exit"
        const val EXIT_CUTSCENE = "loc.giants_foundry_exit_cutscene"
        val EXIT_TILE = CoordGrid(3364, 11482, 0)
        const val MESSAGEBOX = "interface.messagebox"
        const val THINK_SEQ = "seq.emote_think"

        /**
         * Real foundry locs (map square 52_179) and the cutscene and quest versions that replace
         * each.
         */
        val SWAPS =
            listOf(
                LocSwap("loc.giants_foundry_trip_hammer", CoordGrid(3363, 11496, 0), "loc.giants_foundry_trip_hammer_broken_cutscene", "loc.giants_foundry_trip_hammer_quest_multi"),
                LocSwap("loc.giants_foundry_grindstone", CoordGrid(3361, 11491, 0), "loc.giants_foundry_grindstone_broken_cutscene", "loc.giants_foundry_grindstone_quest_multi"),
                LocSwap("loc.giants_foundry_polishing_wheel", CoordGrid(3362, 11484, 0), "loc.giants_foundry_polishing_wheel_broken_cutscene", "loc.giants_foundry_polishing_wheel_quest_multi"),
                LocSwap("loc.giants_foundry_crucible_multi", CoordGrid(3373, 11488, 0), "loc.giants_foundry_crucible_empty_cutscene", "loc.giants_foundry_crucible_empty_quest"),
                LocSwap("loc.giants_foundry_mould_jig", CoordGrid(3370, 11488, 0), "loc.giants_foundry_jig_empty_cutscene", "loc.giants_foundry_jig_empty_quest"),
                LocSwap("loc.giants_foundry_chest", CoordGrid(3375, 11493, 0), "loc.giants_foundry_chest_cutscene", "loc.giants_foundry_chest_quest"),
                LocSwap("loc.giants_foundry_lava_pool", CoordGrid(3372, 11497, 0), "loc.giants_foundry_lava_pool_cutscene", "loc.giants_foundry_lava_pool_quest"),
                LocSwap("loc.giants_foundry_waterfall", CoordGrid(3357, 11488, 0), "loc.giants_foundry_waterfall_cutscene", "loc.giants_foundry_waterfall_quest"),
                LocSwap("loc.giants_foundry_preform_storage", CoordGrid(3369, 11501, 0), "loc.giants_foundry_preform_storage_empty_cutscene", "loc.giants_foundry_preform_storage_empty_quest"),
                LocSwap("loc.giants_foundry_supply_box_multi", CoordGrid(3370, 11483, 0), "loc.giants_foundry_supply_box_cutscene", "loc.giants_foundry_supply_box_quest"),
            )

        /** Set with the cutscene locs and left in place for the quest. */
        const val POWER_UNIT_QUEST = "loc.giants_foundry_tool_powerunit_quest"
        val POWER_UNIT_TILES = listOf(CoordGrid(3362, 11487, 0), CoordGrid(3361, 11494, 0))

        val QUEST_ONLY_LOCS =
            listOf(
                CoordGrid(3359, 11489, 0) to "loc.giants_foundry_waterfall_quest_invis",
                CoordGrid(3365, 11497, 0) to "loc.giants_foundry_trip_hammer_broken_quest_invis",
            )

        const val FADE_CYCLES = 50
        const val FADE_TICKS = 2
        const val CAM_SNAP = 232
        const val INTRO_END_CAM_ANGLE = 250

        val INTRO_ENTRANCE_TILE = CoordGrid(3366, 11483, 0)
        val INTRO_PLAYER_TILE = CoordGrid(3367, 11489, 0)
        val INTRO_RETURN_TILE = CoordGrid(3367, 11485, 0)
        val INTRO_TOOLS_TILE = CoordGrid(3362, 11491, 0)

        val INTRO_SHOTS =
            listOf(
                Shot(CoordGrid(3367, 11487, 0), 500, CAM_SNAP, CoordGrid(3367, 11479, 0), 300, CAM_SNAP),
                Shot(CoordGrid(3367, 11492, 0), 500, 4, CoordGrid(3367, 11484, 0), 280, 3),
                Shot(CoordGrid(3369, 11477, 0), 700, 4, CoordGrid(3364, 11493, 0), 300, 4),
                Shot(CoordGrid(3384, 11490, 0), 700, 4, CoordGrid(3364, 11491, 0), 400, 4),
                Shot(CoordGrid(3380, 11504, 0), 900, 4, CoordGrid(3366, 11489, 0), 400, 2),
                Shot(CoordGrid(3371, 11493, 0), 700, CAM_SNAP, CoordGrid(3368, 11486, 0), 400, CAM_SNAP),
            )
    }
}
