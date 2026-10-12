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

    private class Room(val region: Region, var kovac: Npc)

    fun kovacOf(player: Player): Npc? = rooms[player]?.kovac

    fun isInside(player: Player): Boolean =
        rooms[player]?.let { regionRepo.isValid(it.region) && player.coords in it.region } == true

    /**
     * Builds the room and moves the player in under a fade. With [intro] it opens as the quest's
     * cutscene: the cache's `_cutscene` locs (which have no ops) and Kovac's cutscene npc, seen
     * from a high camera; [endIntro] then swaps in the `_quest` locs.
     */
    suspend fun enter(access: ProtectedAccess, intro: Boolean): Boolean {
        val player = access.player
        rooms.remove(player)?.let { discard(player, it) }
        val region = regionRepo.add(TEMPLATE) ?: return false
        regionRepo.protect(region)
        try {
            for (swap in SWAPS) {
                swapLoc(region, swap.coords, swap.real, if (intro) swap.cutscene else swap.quest)
            }
            if (intro) {
                swapLoc(region, EXIT_TILE, EXIT, EXIT_CUTSCENE)
            }
            val kovac =
                if (intro) spawnKovac(region, KOVAC_CUTSCENE, INTRO_KOVAC_TILE)
                else spawnKovac(region, KOVAC, KOVAC_TILE)
            rooms[player] = Room(region, kovac)
            armDropWarning(player)

            access.fadeOut()
            if (intro) {
                access.telejump(region.normal[INTRO_PLAYER_TILE])
                access.lookOver(region)
                access.faceSquare(region.normal[INTRO_LOOK_AT])
                kovac.faceSquare(region.normal[INTRO_PLAYER_TILE])
            } else {
                access.telejump(region.normal[ARRIVAL])
                kovac.faceSquare(region.normal[ARRIVAL])
            }
            access.fadeIn()
        } finally {
            regionRepo.unprotect(region)
        }
        return true
    }

    /** Ends the cutscene: the quest's locs and Kovac replace the cutscene ones. */
    suspend fun endIntro(access: ProtectedAccess) {
        val room = rooms[access.player] ?: return
        access.fadeOut()
        swapToQuest(room, access.player)
        access.camReset()
        access.fadeIn()
    }

    /** [endIntro] without the fade, for when the cutscene's dialogue is cut short. */
    fun endIntroNow(access: ProtectedAccess) {
        val room = rooms[access.player] ?: return
        swapToQuest(room, access.player)
        access.camReset()
        access.closeFadeOverlayNow()
    }

    private fun swapToQuest(room: Room, player: Player) {
        if (room.kovac.type.id == KOVAC.asRSCM(RSCMType.NPC)) {
            return
        }
        for (swap in SWAPS) {
            swapLoc(room.region, swap.coords, swap.cutscene, swap.quest)
        }
        swapLoc(room.region, EXIT_TILE, EXIT_CUTSCENE, EXIT)
        npcRepo.del(room.kovac, Int.MAX_VALUE)
        room.kovac = spawnKovac(room.region, KOVAC, KOVAC_TILE)
        room.kovac.faceSquare(player.coords)
    }

    /** Kovac stays put in the room so the player can find him to talk. */
    private fun spawnKovac(region: Region, type: String, tile: CoordGrid): Npc {
        val kovac = Npc(type, region.normal[tile])
        npcRepo.add(kovac, Int.MAX_VALUE)
        kovac.noneMode()
        return kovac
    }

    /**
     * Moves the player from the room to the same tile of the real foundry, once the tools are
     * repaired.
     */
    // UNVERIFIED: how the quest moves the player to the real foundry isn't documented; the wiki
    // only warns that items dropped in the room are lost once Kovac is spoken to.
    suspend fun moveToFoundry(access: ProtectedAccess) {
        val room = rooms.remove(access.player) ?: return
        val dest = room.realCoords(access.player.coords)
        access.fadeOut()
        access.telejump(dest)
        access.fadeIn()
        discard(access.player, room)
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
        npcRepo.del(room.kovac, Int.MAX_VALUE)
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

    private suspend fun ProtectedAccess.fadeOut() {
        fadeOverlay(0, 255, 0, 0, FADE_CYCLES)
        delay(FADE_TICKS)
    }

    private suspend fun ProtectedAccess.fadeIn() {
        fadeOverlay(0, 0, 0, 255, FADE_CYCLES)
        delay(FADE_TICKS)
        closeFadeOverlayNow()
    }

    /**
     * The cutscene's last frame (wiki File:Sleeping Giants - entering the Giants' Foundry.png):
     * Kovac just inside the cave entrance, the player mid-floor, and a high camera looking
     * across the lava toward the waterfall and the broken tools.
     */
    // UNVERIFIED: one screenshot is the only source; the camera path and these tiles are read
    // off it.
    private fun ProtectedAccess.lookOver(region: Region) {
        camMoveToV3(region.normal[INTRO_EYE], height = INTRO_EYE_HEIGHT, rate = CAM_SNAP, rate2 = CAM_SNAP)
        camLookAtV3(region.normal[INTRO_LOOK_AT], height = 0, rate = CAM_SNAP, rate2 = CAM_SNAP)
    }

    private class LocSwap(
        val real: String,
        val coords: CoordGrid,
        val cutscene: String,
        val quest: String,
    )

    private companion object {
        const val KOVAC = "npc.giants_foundry_kovac_multi"

        /** Kovac's spawn in the real foundry (`giants_foundry.toml`). */
        val KOVAC_TILE = CoordGrid(3369, 11486, 0)

        val ARRIVAL = FOUNDRY_INSIDE

        // UNVERIFIED: the Minigame Teleport tile, as for the foundry's exit.
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
                LocSwap("loc.giants_foundry_chest", CoordGrid(3375, 11493, 1), "loc.giants_foundry_chest_cutscene", "loc.giants_foundry_chest_quest"),
                LocSwap("loc.giants_foundry_lava_pool", CoordGrid(3372, 11497, 0), "loc.giants_foundry_lava_pool_cutscene", "loc.giants_foundry_lava_pool_quest"),
                LocSwap("loc.giants_foundry_waterfall", CoordGrid(3357, 11488, 0), "loc.giants_foundry_waterfall_cutscene", "loc.giants_foundry_waterfall_quest"),
                LocSwap("loc.giants_foundry_preform_storage", CoordGrid(3369, 11501, 0), "loc.giants_foundry_preform_storage_empty_cutscene", "loc.giants_foundry_preform_storage_empty_quest"),
                LocSwap("loc.giants_foundry_supply_box_multi", CoordGrid(3370, 11483, 0), "loc.giants_foundry_supply_box_cutscene", "loc.giants_foundry_supply_box_quest"),
            )

        const val FADE_CYCLES = 50
        const val FADE_TICKS = 2
        const val CAM_SNAP = 232

        val INTRO_KOVAC_TILE = CoordGrid(3365, 11484, 0)
        val INTRO_PLAYER_TILE = CoordGrid(3367, 11489, 0)
        val INTRO_EYE = CoordGrid(3377, 11493, 0)
        const val INTRO_EYE_HEIGHT = 1200
        val INTRO_LOOK_AT = CoordGrid(3360, 11488, 0)
    }
}
