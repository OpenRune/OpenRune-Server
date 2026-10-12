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

    private class Room(val region: Region, val kovac: Npc)

    fun kovacOf(player: Player): Npc? = rooms[player]?.kovac

    fun isInside(player: Player): Boolean =
        rooms[player]?.let { regionRepo.isValid(it.region) && player.coords in it.region } == true

    /**
     * Builds the room and moves the player in under a fade. With [intro] the camera first looks
     * over the foundry, as in the quest's cutscene.
     */
    suspend fun enter(access: ProtectedAccess, intro: Boolean): Boolean {
        val player = access.player
        rooms.remove(player)?.let(::discard)
        val region = regionRepo.add(TEMPLATE) ?: return false
        regionRepo.protect(region)
        try {
            for (swap in SWAPS) {
                swapLoc(region, swap)
            }
            val kovac = Npc(KOVAC, region.normal[KOVAC_TILE])
            npcRepo.add(kovac, Int.MAX_VALUE)
            rooms[player] = Room(region, kovac)

            access.fadeOut()
            access.telejump(region.normal[ARRIVAL])
            kovac.faceSquare(region.normal[ARRIVAL])
            if (intro) {
                access.lookOver(region)
            }
            access.fadeIn()
            if (intro) {
                access.delay(INTRO_TICKS)
                access.camReset()
            }
        } finally {
            regionRepo.unprotect(region)
        }
        return true
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
        discard(room)
    }

    /** Forgets the room after the player walks out of it. */
    fun left(player: Player) {
        rooms.remove(player)?.let(::discard)
    }

    /** A player who logs out in the room logs back in outside the cave. */
    fun logout(player: Player) {
        val room = rooms.remove(player) ?: return
        if (player.coords in room.region) {
            player.attr[InstanceAttributes.LOGIN_EXIT_COORD] = OUTSIDE.packed
        }
        discard(room)
    }

    private fun discard(room: Room) {
        npcRepo.del(room.kovac, Int.MAX_VALUE)
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

    private fun swapLoc(region: Region, swap: LocSwap) {
        val coords = region.normal[swap.coords]
        val realId = swap.real.asRSCM(RSCMType.LOC)
        val loc = locRepo.findAll(coords).firstOrNull { it.id == realId } ?: return
        locRepo.add(coords, swap.quest, Int.MAX_VALUE, loc.angle, loc.shape)
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

    // UNVERIFIED: the wiki only says "a cutscene plays, showing a workshop too big for
    // regular-sized people to use"; this camera framing is a stand-in.
    private fun ProtectedAccess.lookOver(region: Region) {
        camMoveToV3(region.normal[INTRO_EYE], height = INTRO_EYE_HEIGHT, rate = CAM_SNAP, rate2 = CAM_SNAP)
        camLookAtV3(region.normal[INTRO_LOOK_AT], height = 0, rate = CAM_SNAP, rate2 = CAM_SNAP)
    }

    private class LocSwap(val real: String, val coords: CoordGrid, val quest: String)

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

        /** Real foundry locs (map square 52_179) and the quest version that replaces each. */
        val SWAPS =
            listOf(
                LocSwap("loc.giants_foundry_trip_hammer", CoordGrid(3363, 11496, 0), "loc.giants_foundry_trip_hammer_quest_multi"),
                LocSwap("loc.giants_foundry_grindstone", CoordGrid(3361, 11491, 0), "loc.giants_foundry_grindstone_quest_multi"),
                LocSwap("loc.giants_foundry_polishing_wheel", CoordGrid(3362, 11484, 0), "loc.giants_foundry_polishing_wheel_quest_multi"),
                LocSwap("loc.giants_foundry_crucible_multi", CoordGrid(3373, 11488, 0), "loc.giants_foundry_crucible_empty_quest"),
                LocSwap("loc.giants_foundry_mould_jig", CoordGrid(3370, 11488, 0), "loc.giants_foundry_jig_empty_quest"),
                LocSwap("loc.giants_foundry_chest", CoordGrid(3375, 11493, 1), "loc.giants_foundry_chest_quest"),
                LocSwap("loc.giants_foundry_lava_pool", CoordGrid(3372, 11497, 0), "loc.giants_foundry_lava_pool_quest"),
                LocSwap("loc.giants_foundry_waterfall", CoordGrid(3357, 11488, 0), "loc.giants_foundry_waterfall_quest"),
                LocSwap("loc.giants_foundry_preform_storage", CoordGrid(3369, 11501, 0), "loc.giants_foundry_preform_storage_empty_quest"),
                LocSwap("loc.giants_foundry_supply_box_multi", CoordGrid(3370, 11483, 0), "loc.giants_foundry_supply_box_quest"),
            )

        const val FADE_CYCLES = 50
        const val FADE_TICKS = 2
        const val CAM_SNAP = 232

        val INTRO_EYE = CoordGrid(3366, 11483, 0)
        const val INTRO_EYE_HEIGHT = 900
        val INTRO_LOOK_AT = CoordGrid(3367, 11493, 0)
        const val INTRO_TICKS = 4
    }
}
