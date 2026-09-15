package org.rsmod.content.skills.construction

const val HOUSE_GRID: Int = 8
const val HOUSE_LEVELS: Int = 4

const val LEVEL_DUNGEON: Int = 0
const val LEVEL_GROUND: Int = 1

fun slotKey(level: Int, x: Int, z: Int): Int = (level shl 8) or (x shl 4) or z

fun slotLevel(slot: Int): Int = slot shr 8

fun slotX(slot: Int): Int = (slot shr 4) and 0xF

fun slotZ(slot: Int): Int = slot and 0xF

fun furnitureKey(slot: Int, hotspot: Int): Long =
    (slot.toLong() shl 32) or hotspot.toLong()

fun furnitureSlot(key: Long): Int = (key ushr 32).toInt()

fun furnitureHotspot(key: Long): Int = (key and 0xFFFFFFFFL).toInt()

data class PlacedRoom(val room: Int, val rotation: Int)

/**
 * A player's house as a grid of rooms plus the furniture built into them.
 *
 * Rooms are keyed by [slotKey]; furniture by [furnitureKey], pairing a room slot with the index of
 * a hotspot in that room's `dbcol.poh_room:hotspot` list.
 */
class HouseLayout(
    val rooms: MutableMap<Int, PlacedRoom> = LinkedHashMap(),
    val furniture: MutableMap<Long, Int> = LinkedHashMap(),
    /**
     * The loc a piece was built as, where the furniture row alone does not say. A portal is one row
     * whichever destination it leads to, and the destination is the loc.
     */
    val variants: MutableMap<Long, Int> = LinkedHashMap(),
) {
    fun placed(slot: Int): PlacedRoom? = rooms[slot]

    fun place(slot: Int, room: Int, rotation: Int) {
        rooms[slot] = PlacedRoom(room, rotation.and(3))
    }

    fun remove(slot: Int) {
        rooms.remove(slot)
        furniture.keys.removeIf { furnitureSlot(it) == slot }
        variants.keys.removeIf { furnitureSlot(it) == slot }
    }

    fun built(slot: Int, hotspot: Int): Int? = furniture[furnitureKey(slot, hotspot)]

    fun build(slot: Int, hotspot: Int, row: Int, variant: Int? = null) {
        val key = furnitureKey(slot, hotspot)
        furniture[key] = row
        if (variant != null) {
            variants[key] = variant
        } else {
            variants.remove(key)
        }
    }

    fun variant(slot: Int, hotspot: Int): Int? = variants[furnitureKey(slot, hotspot)]

    fun demolish(slot: Int, hotspot: Int) {
        val key = furnitureKey(slot, hotspot)
        furniture.remove(key)
        variants.remove(key)
    }

    fun encode(): String {
        val roomText = rooms.entries.joinToString(",") { (slot, room) ->
            "$slot:${room.room}:${room.rotation}"
        }
        val furnitureText = furniture.entries.joinToString(",") { (key, row) ->
            val variant = variants[key]
            val suffix = if (variant != null) ":$variant" else ""
            "${furnitureSlot(key)}:${furnitureHotspot(key)}:$row$suffix"
        }
        return "$VERSION|$roomText|$furnitureText"
    }

    companion object {
        const val VERSION: Int = 1

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
                layout.place(slot, room, rotation)
            }
            // A fourth field is the built loc, written only for pieces whose row does not name it.
            for (entry in fields[2].split(',')) {
                val parts = entry.split(':')
                if (parts.size < 3) {
                    continue
                }
                val slot = parts[0].toIntOrNull() ?: continue
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

fun inGrid(level: Int, x: Int, z: Int): Boolean =
    level in 0 until HOUSE_LEVELS && x in 0 until HOUSE_GRID && z in 0 until HOUSE_GRID

/** Room slot directly adjacent to [slot] in the given compass [direction] (0 = north, clockwise). */
fun neighbour(slot: Int, direction: Int): Int? {
    val level = slotLevel(slot)
    val x = slotX(slot) + when (direction) { 1 -> 1; 3 -> -1; else -> 0 }
    val z = slotZ(slot) + when (direction) { 0 -> 1; 2 -> -1; else -> 0 }
    return if (inGrid(level, x, z)) slotKey(level, x, z) else null
}
