package dev.openrune.tools

import dev.openrune.cache.MAPS
import dev.openrune.codec.osrs.ObjectDecoder
import dev.openrune.filesystem.Cache
import dev.openrune.map.loc.MapLocDefinition
import dev.openrune.map.loc.MapLocListDecoder
import dev.openrune.map.util.InlineByteBuf
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ObjectServerType

/**
 * Prints every tile a loc is placed on, which the cache is the only source for: an obstacle's
 * landing tile cannot be read off the wiki and the reference servers only carry ids.
 *
 * Takes gameval symbols, raw ids, or one `x1:z1:x2:z2` box, and walks every map square's loc list
 * looking for matches.
 */
public object LocSpawnDumper {
    public fun dump(cache: Cache, serverCache: Cache, rev: Int, wanted: List<String>) {
        val types = HashMap<Int, ObjectServerType>()
        ObjectDecoder(rev).load(serverCache, types)

        val box = wanted.singleOrNull()?.let(::parseBox)
        val ids =
            if (box != null) {
                emptyMap()
            } else {
                wanted.associateBy {
                    if (it.toIntOrNull() != null) it.toInt() else it.asRSCM(RSCMType.LOC)
                }
            }
        if (ids.isEmpty() && box == null) {
            println("No locs given. Pass -Plocs=loc.name,12345 or -Plocs=x1:z1:x2:z2")
            return
        }

        println("loc\tid\tx\tz\tlevel\tangle\tshape\tname\tops")
        var found = 0
        for (squareX in 0 until SQUARES) {
            for (squareZ in 0 until SQUARES) {
                val encoded = cache.data(MAPS, (squareX shl 8) or squareZ, 1) ?: continue
                // A handful of squares hold a stub where the loc list should be; skip rather than
                // stop, since the dump is a survey and a missing square is visible in the count.
                val decoded =
                    runCatching { MapLocListDecoder.decode(InlineByteBuf(encoded)) }.getOrNull()
                        ?: continue
                for (packed in decoded.spawns.longIterator()) {
                    val loc = MapLocDefinition(packed)
                    val x = squareX * SQUARE_SIZE + loc.localX
                    val z = squareZ * SQUARE_SIZE + loc.localZ
                    if (box != null && !box.holds(x, z)) {
                        continue
                    }
                    val symbol = if (box != null) "-" else ids[loc.id] ?: continue
                    val type = types[loc.id]
                    val ops = (0 until 5).mapNotNull { type?.actions?.getOpOrNull(it) }.joinToString("|")
                    println(
                        "$symbol\t${loc.id}\t$x\t$z\t${loc.level}\t${loc.angle}\t${loc.shape}\t" +
                            "${type?.name}\t$ops"
                    )
                    found++
                }
            }
        }
        println("# $found placements")
    }

    /** `x1:z1:x2:z2` asks for every loc in a box instead of by id. */
    private fun parseBox(text: String): Box? {
        val parts = text.split(':').mapNotNull(String::toIntOrNull)
        return if (parts.size == 4) Box(parts[0], parts[1], parts[2], parts[3]) else null
    }

    private data class Box(val x1: Int, val z1: Int, val x2: Int, val z2: Int) {
        fun holds(x: Int, z: Int): Boolean = x in x1..x2 && z in z1..z2
    }

    private const val SQUARES = 256
    private const val SQUARE_SIZE = 64
}
