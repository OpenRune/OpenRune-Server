package org.rsmod.content.skills.construction

const val HOUSE_GRID: Int = 8
const val HOUSE_LEVELS: Int = 3

/**
 * Zones between the region's edge and the grid, so the blank ring live sends around the upper
 * floor has somewhere to go.
 */
const val HOUSE_GRID_OFFSET: Int = 1

const val LEVEL_DUNGEON: Int = 0
const val LEVEL_GROUND: Int = 1
const val LEVEL_UPSTAIRS: Int = 2

/** A loc angle or a room rotation turned a further [turns] quarter turns clockwise. */
fun quarterTurns(angle: Int, turns: Int): Int = (angle + turns) and 3

/** A compass direction, 0 north and clockwise from there. */
@JvmInline
value class Direction(val value: Int) {
    fun turned(rotation: Int): Direction = Direction(quarterTurns(value, rotation))

    val opposite: Direction
        get() = turned(2)
}

/** A room's place in the house grid, packed so it can key the layout and survive a save. */
@JvmInline
value class RoomSlot(val packed: Int) {
    val level: Int
        get() = packed shr 8

    val x: Int
        get() = (packed shr 4) and 0xF

    val z: Int
        get() = packed and 0xF

    fun neighbour(direction: Direction): RoomSlot? =
        when (direction.value) {
            0 -> of(level, x, z + 1)
            1 -> of(level, x + 1, z)
            2 -> of(level, x, z - 1)
            else -> of(level, x - 1, z)
        }

    fun above(): RoomSlot? = of(level + 1, x, z)

    companion object {
        fun of(level: Int, x: Int, z: Int): RoomSlot? {
            val inGrid =
                level in 0 until HOUSE_LEVELS && x in 0 until HOUSE_GRID && z in 0 until HOUSE_GRID
            return if (inGrid) RoomSlot((level shl 8) or (x shl 4) or z) else null
        }
    }
}

data class PlacedRoom(val room: Int, val rotation: Int)

/**
 * A player's house as a grid of rooms plus the furniture built into them, furniture being keyed by
 * its room's slot and the index of a hotspot in that room's `dbcol.poh_room:hotspot` list.
 */
class HouseLayout(
    val rooms: MutableMap<RoomSlot, PlacedRoom> = LinkedHashMap(),
    val furniture: MutableMap<Long, Int> = LinkedHashMap(),
    /**
     * The loc a piece was built as, where the furniture row alone does not say. A portal is one row
     * whichever destination it leads to, and the destination is the loc.
     */
    val variants: MutableMap<Long, Int> = LinkedHashMap(),
) {
    fun placed(slot: RoomSlot): PlacedRoom? = rooms[slot]

    fun place(slot: RoomSlot, room: Int, rotation: Int) {
        rooms[slot] = PlacedRoom(room, quarterTurns(rotation, 0))
    }

    fun remove(slot: RoomSlot) {
        rooms.remove(slot)
        furniture.keys.removeIf { furnitureSlot(it) == slot }
        variants.keys.removeIf { furnitureSlot(it) == slot }
    }

    /** Why the room at [slot] cannot be taken out, or null when it can. */
    fun removalRefusal(slot: RoomSlot): String? {
        if (slot !in rooms) {
            return "There is no room there to remove."
        }
        if (slot.level == LEVEL_GROUND && rooms.keys.count { it.level == LEVEL_GROUND } == 1) {
            return "You can't remove the last room on the ground floor."
        }
        if (slot.above()?.let { it in rooms } == true) {
            return "You need to remove the room above it first."
        }
        return null
    }

    fun built(slot: RoomSlot, hotspot: Int): Int? = furniture[furnitureKey(slot, hotspot)]

    fun build(slot: RoomSlot, hotspot: Int, row: Int, variant: Int? = null) {
        val key = furnitureKey(slot, hotspot)
        furniture[key] = row
        if (variant != null) {
            variants[key] = variant
        } else {
            variants.remove(key)
        }
    }

    fun variant(slot: RoomSlot, hotspot: Int): Int? = variants[furnitureKey(slot, hotspot)]

    fun demolish(slot: RoomSlot, hotspot: Int) {
        val key = furnitureKey(slot, hotspot)
        furniture.remove(key)
        variants.remove(key)
    }

    fun encode(): String {
        val roomText = rooms.entries.joinToString(",") { (slot, room) ->
            "${slot.packed}:${room.room}:${room.rotation}"
        }
        val furnitureText = furniture.entries.joinToString(",") { (key, row) ->
            val variant = variants[key]
            val suffix = if (variant != null) ":$variant" else ""
            "${furnitureSlot(key).packed}:${furnitureHotspot(key)}:$row$suffix"
        }
        return "$VERSION|$roomText|$furnitureText"
    }

    companion object {
        const val VERSION: Int = 1

        private fun furnitureKey(slot: RoomSlot, hotspot: Int): Long =
            (slot.packed.toLong() shl 32) or hotspot.toLong()

        private fun furnitureSlot(key: Long): RoomSlot = RoomSlot((key ushr 32).toInt())

        private fun furnitureHotspot(key: Long): Int = (key and 0xFFFFFFFFL).toInt()

        fun decode(text: String?): HouseLayout {
            val layout = HouseLayout()
            val fields = text?.split('|') ?: return layout
            if (fields.size < 3 || fields[0].toIntOrNull() != VERSION) {
                return layout
            }
            for (entry in fields[1].split(',')) {
                val parts = entry.split(':')
                if (parts.size != 3) {
                    continue
                }
                val slot = parts[0].toIntOrNull() ?: continue
                val room = parts[1].toIntOrNull() ?: continue
                val rotation = parts[2].toIntOrNull() ?: continue
                layout.place(RoomSlot(slot), room, rotation)
            }
            // A fourth field is the built loc, written only for pieces whose row does not name it.
            for (entry in fields[2].split(',')) {
                val parts = entry.split(':')
                if (parts.size < 3) {
                    continue
                }
                val slot = parts[0].toIntOrNull()?.let(::RoomSlot) ?: continue
                val hotspot = parts[1].toIntOrNull() ?: continue
                val row = parts[2].toIntOrNull() ?: continue
                val variant = parts.getOrNull(3)?.toIntOrNull()
                if (slot in layout.rooms) {
                    layout.build(slot, hotspot, row, variant)
                }
            }
            return layout
        }
    }
}
