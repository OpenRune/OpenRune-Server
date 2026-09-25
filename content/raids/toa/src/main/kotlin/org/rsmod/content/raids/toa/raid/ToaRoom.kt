package org.rsmod.content.raids.toa.raid

import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.api.repo.region.RegionStaticTemplate

/**
 * The raid's rooms. Each is one map square (8x8 zones) of the static map,
 * copied on all 4 levels into its own instance region.
 *
 * Source: Offline_Scape EncounterType (chunkX / chunkY = zone coords).
 * Confirmed by Jesse's capture: entering the raid rebuilds a region copying
 * (3520, 5120)-(3583, 5183) levels 0-3, and entering the Scabaras path
 * rebuilds a *separate* region copying (3520, 5248)-(3583, 5311).
 */
enum class ToaRoom(val zoneX: Int, val zoneZ: Int) {
    MAIN_HALL(440, 640),
    REWARD_ROOM(456, 640),
    WARDENS_FIRST_ROOM(472, 640),
    WARDENS_SECOND_ROOM(488, 640),
    SCABARAS_PUZZLE(440, 656),
    HET_PUZZLE(456, 656),
    APMEKEN_PUZZLE(472, 656),
    CRONDIS_PUZZLE(488, 656),
    SCABARAS_BOSS(440, 672),
    HET_BOSS(456, 672),
    APMEKEN_BOSS(472, 672),
    CRONDIS_BOSS(488, 672);

    /**
     * The room's instance template: its map square on every level, placed at
     * zone (4, 4) of a small (16x16-zone) region so the room is surrounded by
     * empty void, like vanilla. The template is only a recipe; each
     * `RegionRepository.add(template)` builds a fresh copy.
     */
    val template: RegionStaticTemplate by lazy {
        RegionTemplate.create {
            copyAllLevels(zoneX, zoneZ) {
                regionZoneX = 4
                regionZoneZ = 4
                zoneWidth = ROOM_ZONE_LENGTH
                zoneLength = ROOM_ZONE_LENGTH
            }
        }
    }

    private companion object {
        /** One map square = 8 zones = 64 tiles. */
        const val ROOM_ZONE_LENGTH = 8
    }
}
