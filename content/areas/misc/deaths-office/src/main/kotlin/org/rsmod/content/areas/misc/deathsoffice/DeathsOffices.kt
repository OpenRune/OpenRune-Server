package org.rsmod.content.areas.misc.deathsoffice

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

/** Where the player stands in the static office while in their copy of it; 0 outside. */
internal var Player.officeCoord by intVarp("varp.death_office_coord")

/**
 * Every visit to Death's Office is a private copy of it, with its own Death (live capture, rev 241:
 * `rebuild_region` on entry). Logging out inside one brings the player back to the same tile in a
 * new copy (live capture), and dropped objs go when the copy does.
 */
@Singleton
class DeathsOffices
@Inject
constructor(
    private val regionRepo: RegionRepository,
    private val regions: RegionRegistry,
    private val npcRepo: NpcRepository,
    private val collision: CollisionFlagMap,
) {
    private val offices = mutableMapOf<Player, Region>()

    /**
     * Moves [player] into a new copy of the office, at [static] in it. False only when every
     * instance slot is taken, which a full world (2,000 players, one copy each) can't do alone.
     */
    fun enter(player: Player, static: CoordGrid): Boolean {
        val region = regionRepo.add(TEMPLATE) ?: return false
        regionRepo.protect(region)
        npcRepo.add(Npc(DEATH, region.normal[DEATH_TILE]), Int.MAX_VALUE)
        offices.put(player, region)?.let(regionRepo::unprotect)
        player.officeCoord = static.packed
        if (player.dropTrigger == null) {
            player.dropTrigger(DROP_TRIGGER)
        }
        PathingEntityCommon.telejump(player, collision, region.normal[static])
        return true
    }

    fun exit(player: Player) {
        player.officeCoord = 0
        if (player.dropTrigger == DROP_TRIGGER) {
            player.clearDropTrigger(DROP_TRIGGER)
        }
        // The registry reclaims an unprotected copy once nobody is in it.
        offices.remove(player)?.let(regionRepo::unprotect)
    }

    /** Remembers where [player] logged out, for a new copy on their next login. */
    fun logout(player: Player) {
        val region = offices.remove(player) ?: return
        player.officeCoord = regions.normalizeCoords(player.coords).packed
        regionRepo.unprotect(region)
    }

    fun death(player: Player): Npc? {
        val region = offices[player] ?: return null
        return npcRepo.findAll(region.normal[DEATH_TILE]).firstOrNull { it.type.isType(DEATH) }
    }

    internal companion object {
        const val DEATH = "npc.death_office_death"
        const val DROP_TRIGGER = "droptrigger.death_office"

        // live capture, rev 241: Death stands here.
        val DEATH_TILE = CoordGrid(3179, 5726)

        // The office's map square (49, 89), copied whole.
        val TEMPLATE: RegionTemplate =
            RegionTemplate.create {
                copy(392, 712, 0) {
                    regionZoneX = 4
                    regionZoneZ = 4
                    zoneWidth = 8
                    zoneLength = 8
                }
            }
    }
}
