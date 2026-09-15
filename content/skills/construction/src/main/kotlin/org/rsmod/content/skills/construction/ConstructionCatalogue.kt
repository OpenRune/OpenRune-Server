package org.rsmod.content.skills.construction

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.registry.loc.LocRegistryNormal
import org.rsmod.api.table.FurnitureRow
import org.rsmod.api.table.PohHotspotRow
import org.rsmod.api.table.PohRoomRow
import org.rsmod.game.loc.LocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

private val logger = InlineLogger()

private const val LOC_PREFIX = "loc."
private const val OBJ_PREFIX = "obj."
private const val FURNITURE_LOC_TABLE = "/furniture-locs.tsv"

private val MATERIAL_WORDS =
    listOf("mahogany", "teak", "oak", "marble", "limestone", "gilded", "gold")

/** Renames between an obj and its loc that are a reordering rather than a new vocabulary. */
private val LOC_ALIASES = mapOf("poh_portal_nexus" to "poh_nexus_portal")

private class FurnitureBuild(
    val locs: List<Int>,
    val xp: Double?,
    /** Hotspot loc id -> the loc this piece puts on that part of the hotspot. */
    val parts: Map<Int, Int>,
)

/** `model_obj` -> what it builds. See `furniture-locs.tsv` for why this cannot be derived. */
private val FURNITURE_BUILDS: Map<Int, FurnitureBuild> by lazy {
    val stream =
        ConstructionCatalogue::class.java.getResourceAsStream(FURNITURE_LOC_TABLE)
            ?: error("Missing resource: $FURNITURE_LOC_TABLE")
    stream.bufferedReader().useLines { lines ->
        lines
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                val cells = line.split('	')
                if (cells.size < 2) {
                    return@mapNotNull null
                }
                val objId = cells[0].toIntOrNull() ?: return@mapNotNull null
                val locs = cells.getOrNull(2)?.split(',')?.mapNotNull(String::toIntOrNull).orEmpty()
                val parts =
                    cells
                        .getOrNull(3)
                        .orEmpty()
                        .split(',')
                        .mapNotNull { pair ->
                            val (spot, built) = pair.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
                            val spotId = spot.toIntOrNull() ?: return@mapNotNull null
                            val builtId = built.toIntOrNull() ?: return@mapNotNull null
                            spotId to builtId
                        }
                        .toMap()
                objId to FurnitureBuild(locs, cells.getOrNull(1)?.toDoubleOrNull(), parts)
            }
            .toMap()
    }
}

/** The xp a build awards, where the reference table knows it. */
fun furnitureXp(modelObjId: Int): Double? = FURNITURE_BUILDS[modelObjId]?.xp

/**
 * Hotspot loc -> the `model_obj`s built on it, read back out of the same pairs. This says which
 * hotspot a loc belongs to without going near its name, which the name conventions get wrong: the
 * formal garden's hedge runs on `poh_posh_garden_5end`/`_5mid`/`_5cor`, so reading that `5` as a
 * slot number files the whole hedge under the flower bed.
 */
private val HOTSPOT_FURNITURE: Map<Int, Set<Int>> by lazy {
    val out = HashMap<Int, MutableSet<Int>>()
    for ((objId, build) in FURNITURE_BUILDS) {
        for (hotspotLoc in build.parts.keys) {
            out.getOrPut(hotspotLoc) { HashSet() } += objId
        }
    }
    out
}

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

    /**
     * Which walls this room can join a neighbour on. `dbcol.poh_room:door_locations` is empty for
     * some rooms - the portal room among them, which made it unbuildable - so the doorways actually
     * found in its chunk stand in.
     */
    private val doorDirections: Set<Int> =
        row.doorLocations.mapTo(HashSet()) { it and 3 }.ifEmpty {
            doors.mapTo(HashSet()) { it.direction }
        }

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

    /** Every loc a built piece of furniture can show as, so "Remove" can be bound to them. */
    fun furnitureLocIds(): Set<Int> =
        byId.values.flatMapTo(HashSet()) { def ->
            def.hotspots.flatMap { spot -> spot.builds.flatMap(::builtLocIds) }
        }

    fun doorLocIds(): Set<Int> =
        byId.values.flatMapTo(HashSet()) { def -> def.doors.map { it.locId } }

    /**
     * Every loc a built piece of furniture can show as, the first being what a single-tile piece
     * places.
     *
     * `dbtable.furniture` has no loc column, and the obj and loc names are different vocabularies
     * rather than spelling variants - `obj.poh_armchair_1` builds `loc.poh_chair1`, `obj.poh_rug_1`
     * builds three locs - so only about 200 of 500 rows can be matched by name at all. The rest come
     * from [FURNITURE_LOCS], keyed by `model_obj`. Name matching stays as the fallback for rows
     * added to the cache since that table was transcribed.
     */
    fun builtLocIds(furniture: FurnitureRow): List<Int> {
        val mapped = FURNITURE_BUILDS[furniture.modelObj.id]?.locs?.filter(::locExists)
        if (!mapped.isNullOrEmpty()) {
            return mapped
        }
        return listOfNotNull(namedLocId(furniture))
    }

    fun builtLocId(furniture: FurnitureRow): Int? = builtLocIds(furniture).firstOrNull()

    /**
     * Resolves content added to the cache after [FURNITURE_LOCS] was transcribed, where the loc name
     * is a predictable variation on the obj's: the same words in the order the loc table happens to
     * use, a rotation variant, or one of several material variants. A trophy that has a teak and a
     * mahogany loc is told apart by the planks the row actually costs, rather than by guessing.
     */
    private fun namedLocId(furniture: FurnitureRow): Int? {
        val objName = furniture.modelObj.internalName
        if (!objName.startsWith(OBJ_PREFIX)) {
            return null
        }
        val stem = objName.removePrefix(OBJ_PREFIX)
        val aliased = LOC_ALIASES.entries.firstOrNull { stem.startsWith(it.key) }
        val materials =
            furniture.materials().mapNotNull { (material, _) ->
                MATERIAL_WORDS.firstOrNull { it in material }
            }
        val candidates =
            listOfNotNull(
                stem,
                aliased?.let { stem.replaceFirst(it.key, it.value) },
                *materials.map { "${stem}_$it" }.toTypedArray(),
                "${stem}_rot0",
            )
        return candidates.firstNotNullOfOrNull { name ->
            val id = runCatching { RSCM.getRSCM(LOC_PREFIX + name) }.getOrDefault(-1)
            id.takeIf { it > 0 && locExists(it) }
        }
    }

    private fun locExists(id: Int): Boolean = ServerCacheManager.getObject(id) != null

    /**
     * Which loc a piece puts on [part] of its hotspot, so a rug lays corners on the hotspot's corner
     * tiles rather than repeating one tile. The reference table pairs each hotspot loc id with the
     * loc built on it; a part it does not name falls back to the first loc, which is what every
     * single-tile piece uses.
     */
    fun builtLocFor(furniture: FurnitureRow, part: HotspotPart): Int? {
        val build = FURNITURE_BUILDS[furniture.modelObj.id]
        val paired = build?.parts?.get(part.locId)
        if (paired != null && locExists(paired)) {
            return paired
        }
        return builtLocIds(furniture).firstOrNull()
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
            val namedSlot = HotspotNaming.slotOf(name)
            val slot = tableSlot(loc.id, rows, namedSlot)
                ?: namedSlot
                ?: HotspotNaming.overrideSlot(row.name, name)
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

    /**
     * The 1-based slot whose furniture the reference pairs say is built on [locId].
     *
     * The pairs say which furniture a loc builds, which is what stops the formal garden's hedge
     * being read as the flower bed just because its locs are spelled `poh_posh_garden_5end`. Where
     * a room repeats a hotspot the pairs cannot finish the job: a parlour's three chair spaces all
     * offer the same chairs. There [ordinal] settles it, since the name numbers them in order - and
     * it need not agree with the row number, which is how the portal room works, its `poh_teleroom_1`
     * to `_3` filling rows 4 to 6.
     */
    private fun tableSlot(locId: Int, rows: List<PohHotspotRow>, ordinal: Int?): Int? {
        val furniture = HOTSPOT_FURNITURE[locId] ?: return null
        val matches =
            rows.indices.filter { i -> rows[i].builddata.any { it.modelObj.id in furniture } }
        if (matches.isEmpty()) {
            return null
        }
        if (matches.size == 1) {
            return matches.first() + 1
        }
        // Several slots offer this furniture, so the pairs cannot say which. Step in only where the
        // name points at a row that builds nothing, which means the name is not counting rows: the
        // portal room's `poh_teleroom_1`..`_3` fill rows 4 to 6, whose rows 1 to 3 are empty.
        val named = (ordinal ?: return null) - 1
        if (rows.getOrNull(named)?.builddata?.isNotEmpty() != false) {
            return null
        }
        return matches.getOrNull(named)?.plus(1)
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
        reportMultiPart(rooms)
        if (unresolved.isNotEmpty()) {
            logger.warn {
                "${unresolved.size} furniture rows have no matching loc and cannot be shown once " +
                    "built: ${unresolved.take(UNRESOLVED_REPORT).joinToString { it.name + '/' + it.modelObj.internalName }}"
            }
        }
    }

    /**
     * A multi-tile piece spreads its locs across the hotspot's parts by name. If that matching ever
     * collapses - every part taking the same loc, so a rug lays one tile repeated - it is invisible
     * until someone builds one, so the mapping is exercised here instead of only in a house.
     */
    private fun reportMultiPart(rooms: Collection<RoomDef>) {
        var checked = 0
        val collapsed = LinkedHashSet<String>()
        for (room in rooms) {
            for (spot in room.hotspots.filter { it.parts.size > 1 }) {
                for (furniture in spot.builds) {
                    if (builtLocIds(furniture).size < 2) {
                        continue
                    }
                    checked++
                    val perPart = spot.parts.mapNotNull { builtLocFor(furniture, it) }
                    if (perPart.distinct().size < 2) {
                        collapsed += "${room.row.name}/${furniture.name}"
                    }
                }
            }
        }
        if (collapsed.isEmpty()) {
            logger.info { "Multi-tile furniture: $checked pieces spread across their hotspot parts." }
            return
        }
        logger.warn {
            "${collapsed.size} of $checked multi-tile pieces put the same loc on every part and " +
                "will render as one repeated tile: ${collapsed.take(UNRESOLVED_REPORT).joinToString()}"
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
        const val UNRESOLVED_REPORT = 40

        /** South-west tile of the house room template grid, verified against every room chunk. */
        val TEMPLATE_BASE = CoordGrid(1856, 7040, 0)
    }
}
