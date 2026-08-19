package org.rsmod.content.other.commands

import jakarta.inject.Inject
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.registry.worldentity.WorldEntityRegistry
import org.rsmod.api.registry.worldentity.isSuccess
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.WorldEntity
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AdminWorldEntityDebugCommands
@Inject
constructor(
    private val worldEntityRegistry: WorldEntityRegistry,
    private val regionRepo: RegionRepository,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    private var lastSpawned: WorldEntity? = null

    override fun ScriptContext.startup() {
        onCommand("spawnboat", "Spawn a debug raft world entity at your coords", ::spawnBoat) {
            invalidArgs = "Use as ::spawnboat"
        }
        onCommand("moveboat", "Move the last debug world entity", ::moveBoat) {
            invalidArgs =
                "Use as ::moveboat dTilesX dTilesZ [angle] [jump] " +
                    "(ex: ::moveboat 2 0 or ::moveboat 5 5 512 1)"
        }
        onCommand("delboat", "Delete the last debug world entity", ::delBoat)
        onCommand("boardboat", "Teleport aboard the last debug world entity", ::boardBoat)
        onCommand("exitboat", "Teleport off the last debug world entity", ::exitBoat)
    }

    private fun spawnBoat(cheat: Cheat) =
        with(cheat) {
            // Copy the raft deck template (all 4 levels) into an instanced region. The hull
            // model is baked into the template zone, so the zone copy alone renders the boat.
            val template =
                RegionTemplate.create {
                    copyAllLevels(RAFT_TEMPLATE_ZONE_X, RAFT_TEMPLATE_ZONE_Z) {
                        zoneWidth = RAFT_SIZE_ZONES
                        zoneLength = RAFT_SIZE_ZONES
                    }
                }
            val region = regionRepo.add(template)
            if (region == null) {
                player.mes("Could not allocate a deck region for the world entity.")
                return@with
            }
            // Protect the region: empty regions (no players inside) are otherwise reclaimed
            // by the inactive-region sweep on the next region registration.
            regionRepo.protect(region)
            val swZone = ZoneKey.from(region.southWest)
            val entity =
                WorldEntity(
                    id = RAFT_WE_TYPE_ID,
                    sizeX = RAFT_SIZE_ZONES,
                    sizeZ = RAFT_SIZE_ZONES,
                    southWestZoneX = swZone.x,
                    southWestZoneZ = swZone.z,
                    fineX = WorldEntity.tileToFine(player.x + SPAWN_OFFSET_TILES),
                    fineZ = WorldEntity.tileToFine(player.z),
                    projectedLevel = player.level,
                    activeLevel = RAFT_DECK_LEVEL,
                )
            entity.region = region
            val result = worldEntityRegistry.add(entity)
            if (!result.isSuccess()) {
                regionRepo.unprotect(region)
                player.mes("Could not spawn world entity: $result")
                return@with
            }
            lastSpawned = entity
            player.mes("Spawned raft world entity: slot=${entity.slotId}, coords=${entity.coords}")
        }

    private fun moveBoat(cheat: Cheat) =
        with(cheat) {
            val entity = lastSpawned
            if (entity == null) {
                player.mes("No debug world entity spawned.")
                return@with
            }
            val dx = args[0].toInt() * WorldEntity.FINE_UNITS_PER_TILE
            val dz = args[1].toInt() * WorldEntity.FINE_UNITS_PER_TILE
            val jump = (args.getOrNull(3)?.toInt() ?: 0) != 0
            entity.updateCoord(
                level = entity.projectedLevel,
                fineX = entity.fineX + dx,
                fineZ = entity.fineZ + dz,
                teleport = jump,
            )
            args.getOrNull(2)?.toInt()?.let(entity::updateAngle)
            player.mes("Moved world entity to ${entity.coords} (angle=${entity.angle}).")
        }

    private fun boardBoat(cheat: Cheat) =
        with(cheat) {
            val entity = lastSpawned
            if (entity == null) {
                player.mes("No debug world entity spawned.")
                return@with
            }
            val region = entity.region
            if (region == null) {
                player.mes("World entity has no deck region.")
                return@with
            }
            val deck =
                CoordGrid(
                    region.southWest.x + RAFT_BOARD_DX,
                    region.southWest.z + RAFT_BOARD_DZ,
                    RAFT_DECK_LEVEL,
                )
            protectedAccess.launch(player) {
                player.mes("You board your boat.")
                telejump(deck, TeleportType.Exempt)
            }
        }

    private fun exitBoat(cheat: Cheat) =
        with(cheat) {
            val dest = player.lastKnownNormalCoord
            protectedAccess.launch(player) {
                player.mes("You disembark.")
                telejump(dest, TeleportType.Exempt)
            }
        }

    private fun delBoat(cheat: Cheat) =
        with(cheat) {
            val entity = lastSpawned
            if (entity == null) {
                player.mes("No debug world entity spawned.")
                return@with
            }
            val result = worldEntityRegistry.del(entity)
            // Unprotect the deck region; the registry's inactive-region sweep reclaims it
            // (same convention as InstanceManager teardown).
            entity.region?.let(regionRepo::unprotect)
            entity.region = null
            lastSpawned = null
            player.mes("Deleted world entity: $result")
        }

    private companion object {
        private const val SPAWN_OFFSET_TILES = 3

        /** Cache config `worldentity_1` (rev-240 `dump.worldentity`: minimap_boat_raft). */
        private const val RAFT_WE_TYPE_ID = 1

        /** Raft deck template zone - tiles (3840, 6456); hull baked into the static map. */
        private const val RAFT_TEMPLATE_ZONE_X = 480
        private const val RAFT_TEMPLATE_ZONE_Z = 807
        private const val RAFT_SIZE_ZONES = 1

        /**
         * Deck entities live on level 1 (`dump.worldentity` `mainlevel=1`; SAW(entity, 1) in
         * reference captures).
         */
        private const val RAFT_DECK_LEVEL = 1

        /**
         * Raft board tile: `board_dest` template (3843, 6460, 1) minus template base
         * (3840, 6456) = offsets (3, 4). The raft boards on its helm tile.
         */
        private const val RAFT_BOARD_DX = 3
        private const val RAFT_BOARD_DZ = 4
    }
}
