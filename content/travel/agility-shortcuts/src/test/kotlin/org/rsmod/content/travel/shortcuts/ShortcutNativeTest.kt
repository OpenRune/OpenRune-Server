package org.rsmod.content.travel.shortcuts

import dev.openrune.ServerCacheManager
import dev.openrune.cache.MAPS
import dev.openrune.map.GameMapBuilder
import dev.openrune.map.GameMapDecoder
import dev.openrune.map.loc.MapLocListDecoder
import dev.openrune.map.tile.MapTileDecoder
import dev.openrune.map.util.InlineByteBuf
import dev.openrune.rscm.RSCM.asRSCM
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.square.MapSquareKey
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

internal val shortcutTestCollision = CollisionFlagMap()

@ResourceLock("ServerCacheManager")
internal class ShortcutNativeTest {
    @BeforeEach fun nativeCache() { ServerCacheManager.init(240).close() }

    @Test fun `all configured shortcut options and animations are native and handlers register`() {
        val script = ShortcutScript(shortcutTestCollision, XpModifiers(emptySet()))
        val context = ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache())
        with(script) { context.startup() }
        with(ShortcutSetupScript(org.rsmod.api.inv.storage.PlayerItemStorage(emptySet()), XpModifiers(emptySet()))) { context.startup() }
        for (r in ShortcutRoutes.all) assertTrue(ServerCacheManager.getAnim(r.animation.asRSCM()) != null, r.animation)
    }

    @Test fun `every configured landing is walkable in the accepted native map`() {
        val cache = ServerCacheManager.init(240)
        try {
            val map = shortcutTestCollision
            val squares = ShortcutRoutes.all.flatMap { listOf(it.origin, it.destination) }.map { MapSquareKey(it.x shr 6, it.z shr 6) }.distinct()
            for (square in squares) {
                val group = square.x shl 8 or square.z
                val tile = MapTileDecoder.decode(InlineByteBuf(checkNotNull(cache.data(MAPS, group, 0)) { "$square" }))
                val locs = MapLocListDecoder.decode(InlineByteBuf(cache.data(MAPS, group, 1)!!))
                for (l in 0..3) for (x in square.x * 64..square.x * 64 + 63 step 8) for (z in square.z * 64..square.z * 64 + 63 step 8) map.allocateIfAbsent(x, z, l)
                GameMapDecoder.putMaps(map, square, tile)
                GameMapDecoder.putLocs(GameMapBuilder(), map, square, tile, locs)
            }
            val blocked = ShortcutRoutes.all.filter { it.loc !in ShortcutRoutes.external }.filter { val d = it.destination; map[d.x, d.z, d.level] and BLOCKED != 0 }
            Files.writeString(Path.of("../shortcut-blocked-landings.txt"), blocked.joinToString("\n") { "${it.loc}\t${it.origin}\t${it.destination}" })
            Files.writeString(Path.of("../shortcut-blocked-neighbours.tsv"), blocked.joinToString("\n") { r ->
                val d = r.destination; "${r.loc}\t${r.origin}\t$d\t" + (-3..3).flatMap { dx -> (-3..3).map { dz -> "$dx,$dz:${map[d.x + dx,d.z + dz,d.level].toString(16)}" } }.joinToString(";")
            })
            assertTrue(blocked.isEmpty(), "${blocked.size} blocked landings: ${blocked.take(8)}")
        } finally { cache.close() }
    }
    companion object { const val BLOCKED = CollisionFlag.LOC or CollisionFlag.GROUND_DECOR or CollisionFlag.BLOCK_WALK }
}
