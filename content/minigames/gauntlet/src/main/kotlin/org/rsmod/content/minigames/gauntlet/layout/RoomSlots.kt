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

        fun fromTemplates(
            templates: List<Triple<RoomKind, Int, Map<ResourceKind, List<Tile>>>>
        ): RoomSlots =
            build(
                templates.associate { (kind, variant, tiles) ->
                    (kind to variant) to
                        tiles.mapValues { it.value.toMutableSet() }.toMutableMap()
                }
            )

        private fun build(
            raw: Map<Pair<RoomKind, Int>, Map<ResourceKind, Set<Tile>>>
        ): RoomSlots {
            val templates =
                raw.mapValues { (_, slots) -> TemplateSlots(slots.mapValues { it.value.toList() }) }
            return RoomSlots(fillSparse(templates))
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
