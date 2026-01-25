package org.alter.game.task

import net.rsprot.crypto.xtea.XteaKey
import net.rsprot.protocol.game.outgoing.info.util.BuildArea
import net.rsprot.protocol.game.outgoing.info.util.isEmpty
import net.rsprot.protocol.game.outgoing.info.util.safeReleaseOrThrow
import net.rsprot.protocol.game.outgoing.map.RebuildNormal
import net.rsprot.protocol.game.outgoing.map.RebuildRegion
import net.rsprot.protocol.game.outgoing.map.util.RebuildRegionZone
import org.alter.game.model.Coordinate
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.model.instance.InstancedChunkSet
import org.alter.game.model.region.Chunk
import org.alter.game.service.GameService

/**
 * A [GameTask] that is responsible for sending [org.alter.game.model.entity.Pawn]
 * data to [org.alter.game.model.entity.Pawn]s.
 *
 * @author Tom <rspsmods@gmail.com>
 */
class SequentialSynchronizationTask : GameTask {
    override fun execute(
        world: World,
        service: GameService,
    ) {
        val worldPlayers = world.players
        val worldNpcs = world.npcs

        worldPlayers.forEach(Player::playerCoordCycleTask)

        world.network.infoProtocols.update()


        world.players.forEach {
            if (it.entityType.isHumanControlled && it.initiated) {
                val infos = it.infos
                val infoPackets = infos.getPackets()
                val rootPackets = infoPackets.rootWorldInfoPackets
                // First start off by updating the root world.
                it.write(rootPackets.activeWorld)
                it.write(rootPackets.npcUpdateOrigin)

                rootPackets.worldEntityInfo.getOrNull()?.apply {
                    it.write(this)
                }

                rootPackets.playerInfo.getOrNull()?.apply {
                    it.write(this)
                }

                // OSRS seems to omit sending npc info packets if there are 0 readable
                // bytes in it. It is important to however invoke safeRelease() on the packet
                // if you do not submit it into Session, as it will otherwise leak.
                val rootNpcInfoEmpty = rootPackets.npcInfo.isEmpty()
                if (!rootNpcInfoEmpty) {
                    rootPackets.npcInfo.getOrNull()?.apply {
                        it.write(this)
                    }
                } else {
                    rootPackets.npcInfo.safeReleaseOrThrow()
                }

                // At this stage, you should submit all the zone packets from your server.
                // The active world is already set to the root world, so it is just a matter
                // of sending packets like UpdateZoneFullFollows, UpdateZonePartialEnclosed
                // and so on.
                it.postCycle()

                // Lastly, set the active world back to the root world. This is important
                // for some client functionality, such as client-sided pathfinding.
                it.write(rootPackets.activeWorld)
            }
        }

        for (n in worldNpcs.entries) {
            n?.npcPostSynchronizationTask()
        }
    }
}

fun Player.playerPreSynchronizationTask() {
    val pawn = this
    pawn.movementQueue.cycle()
    val last = pawn.lastKnownRegionBase
    val current = pawn.tile
    if (last == null || shouldRebuildRegion(last, current)) {
        val regionX = ((current.x shr 3) - (Chunk.MAX_VIEWPORT shr 4)) shl 3
        val regionZ = ((current.z shr 3) - (Chunk.MAX_VIEWPORT shr 4)) shl 3
        // @TODO UpdateZoneFullFollowsMessage
        pawn.lastKnownRegionBase = Coordinate(regionX, regionZ, current.height)
        val xteaService = pawn.world.xteaKeyService!!
        val instance = pawn.world.instanceAllocator.getMap(current)
        val rebuildMessage =
            when {
                instance != null -> {
                    RebuildRegion(
                        current.x shr 3,
                        current.z shr 3,
                        true,
                        object : RebuildRegion.RebuildRegionZoneProvider {
                            override fun provide(
                                zoneX: Int,
                                zoneZ: Int,
                                level: Int,
                            ): RebuildRegionZone? {
                                val coord = InstancedChunkSet.getCoordinates(zoneX, zoneZ, level)
                                val chunk = instance.chunks.values[coord] ?: return null
                                return RebuildRegionZone(
                                    chunk.zoneX,
                                    chunk.zoneZ,
                                    chunk.height,
                                    chunk.rot,
                                    XteaKey.ZERO,
                                )
                            }
                        },
                    )
                }
                else -> RebuildNormal(current.x shr 3, current.z shr 3, -1, xteaService)
            }
        pawn.buildArea =
            BuildArea((current.x ushr 3) - 6, (current.z ushr 3) - 6).apply {
                pawn.infos.updateRootBuildArea(this)
            }
        pawn.write(rebuildMessage)
    }
}

private fun shouldRebuildRegion(
    old: Coordinate,
    new: Tile,
): Boolean {
    val dx = new.x - old.x
    val dz = new.z - old.z

    return dx <= Player.NORMAL_VIEW_DISTANCE || dx >= Chunk.MAX_VIEWPORT - Player.NORMAL_VIEW_DISTANCE - 1 ||
        dz <= Player.NORMAL_VIEW_DISTANCE || dz >= Chunk.MAX_VIEWPORT - Player.NORMAL_VIEW_DISTANCE - 1
}

fun Npc.npcPreSynchronizationTask() {
    val pawn = this
    pawn.movementQueue.cycle()
}

fun Npc.npcPostSynchronizationTask() {
    val pawn = this
    val oldTile = pawn.lastTile
    val moved = oldTile == null || !oldTile.sameAs(pawn.tile)

    if (moved) {
        pawn.lastTile = pawn.tile
    }
    pawn.moved = false
    pawn.steps = null
}

/**
 * Updates the coords for all players within the rsprot library. This is run after processing to properly account for
 * displacement effects [dspear, etc]
 */
fun Player.playerCoordCycleTask() {
    this.infos.updateRootCoord(this.tile.height, this.tile.x, this.tile.z)
}
