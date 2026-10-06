package org.rsmod.content.minigames.gauntlet.layout

enum class ResourceKind(val locName: String) {
    DEPOSIT("rock"),
    PHREN("tree"),
    FISHING("pond"),
    GRYM("herb"),
    LINUM("fibre"),
}

data class Tile(val x: Int, val z: Int)

class TemplateSlots(val resources: Map<ResourceKind, List<Tile>>) {
    val all: List<Tile> = resources.values.flatten().distinct()

    fun of(kind: ResourceKind): List<Tile> = resources[kind].orEmpty()
}

class RoomSlots(private val templates: Map<Pair<RoomKind, Int>, TemplateSlots>) {
    operator fun get(kind: RoomKind, variant: Int): TemplateSlots =
        templates[kind to variant] ?: EMPTY

    companion object {
        private val EMPTY = TemplateSlots(emptyMap())
        private const val SPARSE_TILES = 6
        private const val NORMAL_BASE = 232
        private const val CORRUPTED_BASE = 240
        private const val FIRST_VARIANT_Z = 704

        fun parse(lines: List<String>): RoomSlots {
            val raw = mutableMapOf<Pair<RoomKind, Int>, MutableMap<ResourceKind, MutableSet<Tile>>>()
            var current: MutableMap<ResourceKind, MutableSet<Tile>>? = null
            for (line in lines) {
                val text = line.trim()
                when {
                    text.startsWith("ROOM") -> current = templateOf(text, raw)
                    text.startsWith("loc:") -> current?.let { readLoc(text, it) }
                }
            }
            val templates = raw.mapValues { (_, slots) -> TemplateSlots(slots.mapValues { it.value.toList() }) }
            return RoomSlots(fillSparse(templates))
        }

        private fun templateOf(
            header: String,
            raw: MutableMap<Pair<RoomKind, Int>, MutableMap<ResourceKind, MutableSet<Tile>>>,
        ): MutableMap<ResourceKind, MutableSet<Tile>>? {
            val (zoneX, zoneZ) = header.substringAfter("zone=").split(',').map { it.trim().toInt() }
            val offset = zoneX - if (zoneX >= CORRUPTED_BASE) CORRUPTED_BASE else NORMAL_BASE
            val kind =
                when (offset) {
                    0 -> RoomKind.MIDDLE
                    2 -> RoomKind.EDGE
                    4 -> RoomKind.CORNER
                    else -> return null
                }
            val variant = (zoneZ - FIRST_VARIANT_Z) / 2
            return raw.getOrPut(kind to variant) { mutableMapOf() }
        }

        private fun readLoc(text: String, into: MutableMap<ResourceKind, MutableSet<Tile>>) {
            val name = text.substringAfter("loc:").substringBefore(' ')
            val kind = ResourceKind.entries.firstOrNull { it.locName == name } ?: return
            val tiles = text.substringAfter(':', "").substringAfter(':').trim()
            for (pair in tiles.split(' ').filter { it.isNotEmpty() }) {
                val (x, z) = pair.split(',').map { it.toInt() }
                into.getOrPut(kind) { mutableSetOf() } += Tile(x, z)
            }
        }

        private fun fillSparse(
            templates: Map<Pair<RoomKind, Int>, TemplateSlots>
        ): Map<Pair<RoomKind, Int>, TemplateSlots> {
            val result = templates.toMutableMap()
            for (kind in listOf(RoomKind.MIDDLE, RoomKind.EDGE, RoomKind.CORNER)) {
                val ofKind = templates.filterKeys { it.first == kind }.values
                val union =
                    ResourceKind.entries.associateWith { resource ->
                        ofKind.flatMap { it.of(resource) }.distinct()
                    }
                for (variant in 0 until GauntletLayout.VARIANTS) {
                    val existing = templates[kind to variant]
                    if (existing == null || existing.all.size < SPARSE_TILES) {
                        result[kind to variant] = TemplateSlots(union)
                    }
                }
            }
            return result
        }
    }
}
