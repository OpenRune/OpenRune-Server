package org.alter.game.fs

import dev.openrune.cache.*
import io.github.oshai.kotlinlogging.KotlinLogging
import org.alter.game.Server
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.collision.BLOCKED_TILE
import org.alter.game.model.collision.BRIDGE_TILE
import org.alter.game.model.entity.StaticObject
import org.alter.game.model.region.ChunkSet
import org.alter.game.service.xtea.XteaKeyService
import org.rsmod.routefinder.flag.CollisionFlag
import java.io.IOException

/**
 * A [DefinitionSet] is responsible for loading any relevant metadata found in
 * the game resources.
 *
 * @author Tom <rspsmods@gmail.com>
 */
class DefinitionSet {
    private var xteaService: XteaKeyService? = null

    fun loadRegions(
        world: World,
        chunks: ChunkSet,
        regions: IntArray,
    ) {

        val start = System.currentTimeMillis()

        var loaded = 0
        regions.forEach { region ->
            if (chunks.activeRegions.add(region)) {
                if (createRegion(world, region)) {
                    loaded++
                }
            }
        }
        logger.info { "Loaded $loaded regions in ${System.currentTimeMillis() - start}ms" }
    }

    /**
     * Creates an 8x8 [gg.rsmod.game.model.region.Chunk] region.
     */
    fun createRegion(
        world: World,
        id: Int,
    ): Boolean {
        if (xteaService == null) {
            xteaService = world.getService(XteaKeyService::class.java)
        }

        val x = id shr 8
        val y = id and 0xFF

        val mapData = Server.cache.data(MAPS, "m${x}_$y") ?: return false

        val baseX: Int = id shr 8 and 255 shl 6
        val baseZ: Int = id and 255 shl 6

        // Allocates all of the chunks within the region
        // TODO only allocate if the tiles are walkable.
        //val baseX = x * 64 // TODO perhaps just call cacheRegion.baseX
        //val baseZ = z * 64 // TODO perhaps just call cacheRegion.baseY
        for (cx in 0 until 8) {
            for (cz in 0 until 8) {
                val chunkBaseX = baseX + cx * 8
                val chunkBaseZ = baseZ + cz * 8
                for (level in 0 until 4) {
//                    world.collision.add(chunkBaseX, chunkBaseZ, level, 0)
                    world.collision.allocateIfAbsent(chunkBaseX, chunkBaseZ, level)
                }
            }
        }

        val blocked = hashSetOf<Tile>()
        val bridges = hashSetOf<Tile>()

        val tiles = loadTerrain(mapData)

        for (height in 0 until 4) {
            for (lx in 0 until 64) {
                for (ly in 0 until 64) {
                    val bridge = tiles[1][lx][ly].settings.toInt() and BRIDGE_TILE != 0
                    if (bridge) {
                        bridges.add(Tile(baseX + lx, baseZ + ly, height))
                    }
                    val blockedTile = tiles[height][lx][ly].settings.toInt() and BLOCKED_TILE != 0
                    if (blockedTile) {
                        val level = if (bridge) (height - 1) else height
                        if (level < 0) continue
                        blocked.add(Tile(baseX + lx, baseZ + ly, level))
                    }
                }
            }
        }

        /*
         * Apply the blocked tiles to the collision detection.
         */
        blocked.forEach { tile ->
            world.chunks.getOrCreate(tile)
            world.collision.add(tile.x, tile.z, tile.height, CollisionFlag.BLOCK_WALK)
        }
        /**
         * EDIT: turns out i was wrong. the assumption made here didn't pan out as expected. the bandos godwars room door ended up having different flags to before.
         *
         * instead, i just use
         * world.collisionFlags.applyUpdate(update)
         *
         * like in the other places, and that works
         *
         */

        if (xteaService == null) {
            /*
             * If we don't have an [XteaKeyService], then we assume we don't
             * need to decrypt the files through xteas. This means the objects
             * from each region has to be decrypted a different way.
             *
             * If this is the case, you need to use [gg.rsmod.game.model.region.Chunk.addEntity]
             * to add the object to the world for collision detection.
             */
            return true
        }

        val keys = xteaService?.get(id) ?: XteaKeyService.EMPTY_KEYS

        return runCatching {
            Server.cache.data(MAPS, "l${x}_$y", keys)?.let { landData ->
                logger.info { "Region: $id : Keys: ${keys.contentToString()}" }

                loadLocations(landData) { loc ->
                    Tile(baseX + loc.localX, baseZ + loc.localY, loc.height)
                        .takeUnless { it in bridges && loc.height == 0 }
                        ?.let { tile ->
                            val adjustedTile = if (tile in bridges) tile.transform(-1) else tile
                            val obj = StaticObject(loc.id, loc.type, loc.orientation, adjustedTile)
                            world.chunks.getOrCreate(adjustedTile).addEntity(world, obj, adjustedTile)
                        }
                }
            } != null
        }.onFailure { e ->
            logger.error(e) { "Could not decrypt map region $id." }
        }.getOrDefault(false)

    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
