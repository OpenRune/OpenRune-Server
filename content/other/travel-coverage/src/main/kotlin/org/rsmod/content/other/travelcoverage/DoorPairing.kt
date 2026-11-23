package org.rsmod.content.other.travelcoverage

/** The parts of a loc type the door pairing reads; [symbol] is its `loc.` gameval. */
data class DoorType(val symbol: String, val name: String, val ops: List<String>, val contentGroup: Int)

enum class DoorKind(val closed: String, val opened: String) {
    Single("content.closed_single_door", "content.opened_single_door"),
    DoorLeft("content.closed_left_door", "content.opened_left_door"),
    DoorRight("content.closed_right_door", "content.opened_right_door"),
    GateLeft("content.closed_left_picketgate", "content.opened_left_picketgate"),
    GateRight("content.closed_right_picketgate", "content.opened_right_picketgate"),
}

data class DoorSuggestion(val closed: String, val opened: String, val kind: DoorKind)

/**
 * Pairs closed doors and gates with the open model they swap to, so the shared door scripts can
 * serve them. Jagex names the open model after the closed one with "open" worked in somewhere
 * (`poordoor` / `poordooropen`, `bankdoor_r` / `openbankdoor_r`), and that is the only link the
 * cache offers; a door with no such twin, or more than one, is left for a hand-written handler.
 *
 * Halves of a double door are told apart by a left/right suffix that has a matching twin, and
 * double gates go to the picket gate script because they swing differently.
 */
object DoorPairing {
    private const val NO_GROUP = 0xFFFF
    private val SIDE_SUFFIXES = listOf("_left" to "_right", "left" to "right", "_l" to "_r", "l" to "r")
    private val OPEN_INSERTS = listOf("open", "_open", "open_")
    private val WORD_SWAPS = listOf("closed" to "open", "shut" to "open", "closed" to "opened")

    fun pair(closedSymbols: Collection<String>, types: Map<String, DoorType>): List<DoorSuggestion> {
        val closed = closedSymbols.mapNotNull(types::get).filter(::isUntaggedClosedDoor)
        val closedNames = closed.mapTo(HashSet()) { it.symbol }
        val matches =
            closed.mapNotNull { door ->
                val opened =
                    openCandidates(door.symbol)
                        .mapNotNull(types::get)
                        .filter(::isUntaggedOpenDoor)
                        .distinctBy { it.symbol }
                opened.singleOrNull()?.let { door to it }
            }
        val openedUses = matches.groupingBy { it.second.symbol }.eachCount()
        return matches
            .filter { (_, opened) -> openedUses[opened.symbol] == 1 }
            .map { (door, opened) -> DoorSuggestion(door.symbol, opened.symbol, kindOf(door, closedNames)) }
    }

    fun openCandidates(symbol: String): Set<String> {
        val prefix = symbol.substringBefore('.') + "."
        val bare = symbol.removePrefix(prefix)
        val out = LinkedHashSet<String>()
        for (index in 0..bare.length) {
            for (insert in OPEN_INSERTS) {
                out += prefix + bare.substring(0, index) + insert + bare.substring(index)
            }
        }
        for ((from, to) in WORD_SWAPS) {
            if (from in bare) out += prefix + bare.replace(from, to)
        }
        out -= symbol
        return out
    }

    private fun kindOf(door: DoorType, closedNames: Set<String>): DoorKind {
        val gate = "gate" in door.name.lowercase()
        for ((left, right) in SIDE_SUFFIXES) {
            if (door.symbol.endsWith(left) && door.symbol.dropLast(left.length) + right in closedNames) {
                return if (gate) DoorKind.GateLeft else DoorKind.DoorLeft
            }
            if (door.symbol.endsWith(right) && door.symbol.dropLast(right.length) + left in closedNames) {
                return if (gate) DoorKind.GateRight else DoorKind.DoorRight
            }
        }
        return DoorKind.Single
    }

    private fun untagged(type: DoorType): Boolean =
        type.contentGroup == -1 || type.contentGroup == NO_GROUP

    private fun firstOp(type: DoorType): String = type.ops.firstOrNull()?.trim()?.lowercase().orEmpty()

    private fun isUntaggedClosedDoor(type: DoorType): Boolean = untagged(type) && firstOp(type) == "open"

    private fun isUntaggedOpenDoor(type: DoorType): Boolean = untagged(type) && firstOp(type) == "close"
}
