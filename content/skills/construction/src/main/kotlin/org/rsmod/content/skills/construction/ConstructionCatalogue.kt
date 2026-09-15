package org.rsmod.content.skills.construction

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.registry.loc.LocRegistryNormal
import org.rsmod.api.table.FurnitureRow
import org.rsmod.api.table.PohRoomRow
import org.rsmod.game.loc.LocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

private val logger = InlineLogger()

private const val LOC_PREFIX = "loc."
private const val OBJ_PREFIX = "obj."

class HotspotPart(
    val locId: Int,
    val localX: Int,
    val localZ: Int,
    val layer: Int,
    val shapeId: Int,
    val angleId: Int,
)

private class HotspotScan(val hotspots: List<HotspotDef>, val unplaced: List<String>)

class HotspotDef(val index: Int, val parts: List<HotspotPart>, val builds: List<FurnitureRow>) {
    val primary: HotspotPart
        get() = parts.first()
}

class DoorDef(
    val localX: Int,
    val localZ: Int,
    val locId: Int,
    /** Compass direction of the wall this doorway sits on: 0 north, 1 east, 2 south, 3 west. */
    val direction: Int,
)

class RoomDef(
    val row: PohRoomRow,
    val sourceZone: ZoneKey,
    val hotspots: List<HotspotDef>,
    val doors: List<DoorDef>,
    val unplaced: List<String>,
) {
    val id: Int
        get() = row.rowId

    val name: String
        get() = row.nameUppercase

    val cost: Int
        get() = row.cost

    val levelRequirement: Int
        get() = row.levelRequirement.firstOrNull()?.t1 ?: 1

    private val doorDirections: Set<Int> = row.doorLocations.mapTo(HashSet()) { it and 3 }

    fun doorsAfter(rotation: Int): Set<Int> =
        doorDirections.mapTo(HashSet()) { (it + rotation) and 3 }

    fun hotspot(index: Int): HotspotDef? = hotspots.firstOrNull { it.index == index }
}

/**
 * Reads every Construction definition out of the cache's `poh_room`, `poh_hotspot` and `furniture`
 * tables, so room costs, hotspots and build materials never need hand-authoring.
 *
 * The two things those tables do not carry are resolved by name instead:
 * - **where** a hotspot sits, from the numbered hotspot locs in the room's source map chunk;
 * - **what** a built piece of furniture looks like, from the loc sharing an internal name with the
 *   furniture's `model_obj`.
 */
@Singleton
class ConstructionCatalogue @Inject constructor(private val locReg: LocRegistryNormal) {
    private val locNames = HashMap<Int, String>()
    private val byId: Map<Int, RoomDef> by lazy { load() }

    fun all(): Collection<RoomDef> = byId.values

    fun room(id: Int): RoomDef? = byId[id]

    fun hotspotLocIds(): Set<Int> =
        byId.values.flatMapTo(HashSet()) { def ->
            def.hotspots.flatMap { spot -> spot.parts.map { it.locId } }
        }

    fun doorLocIds(): Set<Int> =
        byId.values.flatMapTo(HashSet()) { def -> def.doors.map { it.locId } }

    /**
     * The loc a built piece of furniture shows as. `dbtable.furniture` has no loc column; the link
     * is the shared internal name between the furniture's `model_obj` and its scenery loc, e.g.
     * `obj.poh_armchair_1` and `loc.poh_armchair_1`.
     */
    fun builtLocId(furniture: FurnitureRow): Int? {
        val objName = furniture.modelObj.internalName
        if (!objName.startsWith(OBJ_PREFIX)) {
            return null
        }
        val locName = LOC_PREFIX + objName.removePrefix(OBJ_PREFIX)
        val id = runCatching { RSCM.getRSCM(locName) }.getOrDefault(-1)
        return id.takeIf { it > 0 && ServerCacheManager.getObject(it) != null }
    }

    private fun load(): Map<Int, RoomDef> {
        val rooms =
            PohRoomRow.all()
                .mapNotNull { row ->
                    val offset = row.sourceOffset
                    if (offset.size < 2) {
                        return@mapNotNull null
                    }
                    val zone = sourceZoneOf(offset)
                    val chunk = locReg.findAll(zone).toList()
                    val base = zone.toCoords()
                    val scan = hotspotsOf(row, chunk, base.x, base.z)
                    RoomDef(
                        row = row,
                        sourceZone = zone,
                        hotspots = scan.hotspots,
                        doors = doorsOf(chunk, base.x, base.z),
                        unplaced = scan.unplaced,
                    )
                }
                .associateBy { it.id }
        report(rooms.values)
        return rooms
    }

    /**
     * `dbcol.poh_room:source_offset` is a **tile** offset from [TEMPLATE_BASE], not a zone key: the
     * values run 0, 8, 16 ... 56 across and 0 ... 64 up, an 8x9 grid of one-zone room chunks parked
     * in unreachable map space. [locateTemplates] re-derives the base if this stops matching.
     */
    private fun sourceZoneOf(offset: List<Int>): ZoneKey =
        ZoneKey.from(TEMPLATE_BASE.translate(offset[0], offset[1]))

    private fun hotspotsOf(
        row: PohRoomRow,
        chunk: List<LocInfo>,
        baseX: Int,
        baseZ: Int,
    ): HotspotScan {
        val rows = row.hotspot
        if (rows.isEmpty()) {
            return HotspotScan(emptyList(), emptyList())
        }
        val grouped = HashMap<Int, MutableList<HotspotPart>>()
        val named = ArrayList<Pair<LocInfo, String>>()
        val leftover = LinkedHashSet<String>()

        for (loc in chunk) {
            val name = locName(loc.id)
            val slot = HotspotNaming.slotOf(name) ?: HotspotNaming.overrideSlot(row.name, name)
            if (slot != null) {
                val index = slot - 1
                if (index in rows.indices) {
                    grouped.getOrPut(index) { ArrayList() } += part(loc, baseX, baseZ)
                } else {
                    leftover += name.removePrefix(LOC_PREFIX)
                }
                continue
            }
            if (HotspotNaming.isNamedHotspot(name)) {
                named += loc to name
                continue
            }
            if (!HotspotNaming.isDoor(name) && name.startsWith("${LOC_PREFIX}poh_")) {
                leftover += name.removePrefix(LOC_PREFIX)
            }
        }

        val unplaced = leftover
        if (named.isNotEmpty()) {
            val buildNames = rows.indices.associateWith { rows[it].builddata.map(FurnitureRow::name) }
            for ((loc, name) in named) {
                val index = HotspotNaming.matchSlot(name, buildNames)
                if (index == null) {
                    unplaced += name.removePrefix(LOC_PREFIX)
                    continue
                }
                grouped.getOrPut(index) { ArrayList() } += part(loc, baseX, baseZ)
            }
        }

        val hotspots =
            grouped
                .entries
                .sortedBy { it.key }
                .map { (index, parts) ->
                    HotspotDef(
                        index = index,
                        parts = parts.sortedWith(compareBy({ it.localZ }, { it.localX })),
                        builds = rows[index].builddata,
                    )
                }
        return HotspotScan(hotspots, unplaced.toList())
    }

    private fun doorsOf(chunk: List<LocInfo>, baseX: Int, baseZ: Int): List<DoorDef> =
        chunk.mapNotNull { loc ->
            if (!isDoor(loc.id)) {
                return@mapNotNull null
            }
            val localX = loc.coords.x - baseX
            val localZ = loc.coords.z - baseZ
            val direction = wallDirection(localX, localZ) ?: return@mapNotNull null
            DoorDef(localX, localZ, loc.id, direction)
        }

    private fun part(loc: LocInfo, baseX: Int, baseZ: Int): HotspotPart =
        HotspotPart(
            locId = loc.id,
            localX = loc.coords.x - baseX,
            localZ = loc.coords.z - baseZ,
            layer = loc.layer,
            shapeId = loc.shapeId,
            angleId = loc.angleId,
        )

    // Reverse name lookups are the expensive part of classifying a loc, and a full map sweep asks
    // about millions of them drawn from only a few thousand distinct ids.
    private fun locName(locId: Int): String =
        locNames.getOrPut(locId) { RSCM.getReverseMapping(RSCMType.LOC, locId) }

    private fun isDoor(locId: Int): Boolean = HotspotNaming.isDoor(locName(locId))

    private fun wallDirection(localX: Int, localZ: Int): Int? =
        when {
            localZ >= ZONE_MAX -> 0
            localX >= ZONE_MAX -> 1
            localZ <= 0 -> 2
            localX <= 0 -> 3
            else -> null
        }

    /**
     * Hotspot discovery reads the map rather than a table, so it is the one part of this that can
     * silently drift. Say what it found at boot instead of leaving it to be noticed in-game.
     */
    private fun report(rooms: Collection<RoomDef>) {
        if (rooms.isEmpty()) {
            logger.warn { "No house rooms found: dbtable.poh_room is empty in this cache." }
            return
        }
        val builds = rooms.flatMap { it.hotspots }.flatMap { it.builds }.distinctBy { it.rowId }
        val unresolved = builds.filter { builtLocId(it) == null }
        logger.info {
            "Loaded ${rooms.size} house rooms, " +
                "${rooms.sumOf { it.hotspots.size }} hotspots, " +
                "${rooms.sumOf { it.doors.size }} doorways, " +
                "${builds.size - unresolved.size}/${builds.size} furniture locs resolved."
        }
        if (rooms.all { it.hotspots.isEmpty() }) {
            logger.warn {
                "No house room chunk contained a hotspot. ${locateTemplates()}"
            }
            return
        }
        for (room in rooms) {
            val buildable = room.row.hotspot.indices.filter { room.row.hotspot[it].builddata.isNotEmpty() }
            val missing = buildable.map { it + 1 } - room.hotspots.map { it.index + 1 }
            if (missing.isEmpty()) {
                continue
            }
            val found = room.hotspots.count { it.index in buildable }
            logger.warn {
                "House room '${room.row.name}' at zone " +
                    "${room.sourceZone.x},${room.sourceZone.z} found $found of " +
                    "${buildable.size} buildable hotspots, missing slots $missing" +
                    if (room.unplaced.isEmpty()) " with no unplaced locs in its chunk."
                    else "; unplaced locs: ${room.unplaced.joinToString()}."
            }
        }
        if (unresolved.isNotEmpty()) {
            logger.warn {
                "${unresolved.size} furniture rows have no matching loc and cannot be shown once " +
                    "built: ${unresolved.take(UNRESOLVED_REPORT).joinToString { it.name }}"
            }
        }
    }

    /**
     * Sweeps the static map for the doorway hotspots every house room chunk carries, so a wrong
     * `source_offset` reading reports where the templates actually are instead of just failing.
     */
    private fun locateTemplates(): String {
        val found = ArrayList<String>()
        for (zoneX in 0..ZoneKey.X_BIT_MASK) {
            for (zoneZ in 0..ZoneKey.Z_BIT_MASK) {
                val zone = ZoneKey(zoneX, zoneZ, 0)
                val door = locReg.findAll(zone).firstOrNull { isDoor(it.id) } ?: continue
                found += "zone $zoneX,$zoneZ (tile ${door.coords.x},${door.coords.z})"
                if (found.size >= TEMPLATE_SCAN_REPORT) {
                    return "Doorway hotspots found at: ${found.joinToString()} ..."
                }
            }
        }
        return if (found.isEmpty()) {
            "A full map sweep found no `loc.poh_hotspot_door*` at all, so the room chunks are not " +
                "in this cache's static map."
        } else {
            "Doorway hotspots found at: ${found.joinToString()}"
        }
    }

    private companion object {
        const val ZONE_MAX = 7
        const val TEMPLATE_SCAN_REPORT = 8
        const val UNRESOLVED_REPORT = 10

        /** South-west tile of the house room template grid, verified against every room chunk. */
        val TEMPLATE_BASE = CoordGrid(1856, 7040, 0)
    }
}
