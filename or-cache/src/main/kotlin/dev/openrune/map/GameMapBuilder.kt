package dev.openrune.map

import it.unimi.dsi.fastutil.bytes.Byte2IntOpenHashMap
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocZoneKey
import org.rsmod.map.zone.ZoneKey
import java.util.concurrent.ConcurrentHashMap

@DslMarker private annotation class GameMapBuilderDslMarker

@GameMapBuilderDslMarker
public class GameMapBuilder {
    public val zoneBuilders: ConcurrentHashMap<Int, ZoneBuilder> = ConcurrentHashMap()

    public val zoneCount: Int
        get() = zoneBuilders.size

    public fun computeIfAbsent(key: ZoneKey, value: () -> ZoneBuilder): ZoneBuilder =
        zoneBuilders.computeIfAbsent(key.packed) { value() }
}

@DslMarker private annotation class ZoneBuilderDslMarker

@ZoneBuilderDslMarker
public class ZoneBuilder {
    private val locs: Byte2IntOpenHashMap = Byte2IntOpenHashMap()
    public val linkBelowLocs: Byte2IntOpenHashMap = Byte2IntOpenHashMap()

    public fun add(x: Int, z: Int, layer: Int, entity: LocEntity) {
        val key = LocZoneKey(x, z, layer)
        locs[key.packed] = entity.packed
    }

    public fun addLinkBelow(x: Int, z: Int, layer: Int, entity: LocEntity) {
        val key = LocZoneKey(x, z, layer)
        linkBelowLocs[key.packed] = entity.packed
    }

    public fun build(): Byte2IntOpenHashMap = locs
}
