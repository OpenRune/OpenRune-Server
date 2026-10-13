package org.rsmod.content.areas.misc.deathsoffice

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.movement.RouteRequestCoord
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

/** Nonzero while the player is in their copy of the office. */
internal var Player.officeCoord by intVarp("varp.death_office_coord")

/**
 * Every visit to Death's Office is a private copy of it, with its own Death. Logging out inside one
 * brings the player back to a new copy, and dropped objs go when the copy does.
 */
@Singleton
class DeathsOffices
@Inject
constructor(
    private val regionRepo: RegionRepository,
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

    /** Lets the copy go but keeps [officeCoord], so the next login builds a new one. */
    fun logout(player: Player) {
        offices.remove(player)?.let(regionRepo::unprotect)
    }

    /** Faces Death and walks to the tile in front of his desk, where he talks across it. */
    fun approachDesk(player: Player) {
        val region = offices[player] ?: return
        death(player)?.let(player::faceNpc)
        player.routeRequest = RouteRequestCoord(region.normal[DESK_TILE])
    }

    /** Where [static] lies in [player]'s copy of the office, if they are in one. */
    fun local(player: Player, static: CoordGrid): CoordGrid? = offices[player]?.normal?.get(static)

    fun isAtDesk(player: Player): Boolean = player.coords == offices[player]?.normal?.get(DESK_TILE)

    fun death(player: Player): Npc? {
        val region = offices[player] ?: return null
        return npcRepo.findAll(region.normal[DEATH_TILE]).firstOrNull { it.type.isType(DEATH) }
    }

    internal companion object {
        const val DEATH = "npc.death_office_death"
        const val DROP_TRIGGER = "droptrigger.death_office"

        val DEATH_TILE = CoordGrid(3179, 5726)
        val DESK_TILE = CoordGrid(3176, 5726)

        // Where the first-death tutorial and a login inside the office put the player.
        val LOGIN_ARRIVAL = CoordGrid(3173, 5726)

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
