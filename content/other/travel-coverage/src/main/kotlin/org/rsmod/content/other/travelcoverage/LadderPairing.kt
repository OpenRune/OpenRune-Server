package org.rsmod.content.other.travelcoverage

import org.rsmod.map.CoordGrid

/** The parts of a loc type the pairing reads; [ops] holds the five op texts, blank if unset. */
data class LadderType(val name: String, val ops: List<String>, val contentGroup: Int)

enum class LadderTag(val content: String) {
    Up("content.ladder_up"),
    Down("content.ladder_down"),
    Option("content.ladder_option"),
    DungeonUp("content.dungeonladder_up"),
    DungeonDown("content.dungeonladder_down"),
}

data class LadderSuggestion(val locId: Int, val tag: LadderTag, val placements: Int)

data class LadderSkip(val locId: Int, val name: String, val reason: String)

data class LadderPairingResult(
    val suggestions: List<LadderSuggestion>,
    val skipped: List<LadderSkip>,
)

/**
 * Decides which unhandled ladders the shared ladder scripts can already serve. Those scripts move
 * the player one level up or down on the same tile, or 6400 tiles between the surface and its
 * underground copy, so a ladder qualifies when at least half of its placements have a matching
 * ladder exactly there and none of them disagree on which kind. Jagex copies whole buildings into
 * instance template areas where the matching ladder is often not placed, so requiring every
 * placement to match would leave out the very ladders players use.
 */
object LadderPairing {
    private const val NO_GROUP = 0xFFFF
    private const val DUNGEON_OFFSET = 6400
    private const val SEARCH_RADIUS = 2
    private const val MAX_LEVEL = 3

    private enum class Layout { Up, Down, Option }

    private enum class Mode { Level, Dungeon }

    fun pair(
        placed: List<PlacedLoc>,
        unhandledIds: Set<Int>,
        type: (Int) -> LadderType?,
    ): LadderPairingResult {
        val byTile = HashMap<Long, MutableList<Int>>()
        for (loc in placed) {
            byTile.getOrPut(tileKey(loc.coords)) { mutableListOf() } += loc.id
        }
        val placementsById = placed.groupBy({ it.id }, { it.coords })

        val suggestions = mutableListOf<LadderSuggestion>()
        val skipped = mutableListOf<LadderSkip>()
        for (id in unhandledIds.sorted()) {
            val ladder = type(id) ?: continue
            if (!isLadder(ladder)) continue
            val layout = layoutOf(ladder)
            if (layout == null) {
                skipped += LadderSkip(id, ladder.name, "options are not a plain up/down layout")
                continue
            }
            if (ladder.contentGroup != -1 && ladder.contentGroup != NO_GROUP) {
                skipped += LadderSkip(id, ladder.name, "already has content group ${ladder.contentGroup}")
                continue
            }
            val coords = placementsById[id].orEmpty()
            val modes = coords.map { modeAt(it, layout, byTile, type) }
            val decided = modes.filterNotNull().distinct()
            val matched = modes.count { it != null }
            when {
                matched * 2 < modes.size || decided.isEmpty() ->
                    skipped += LadderSkip(id, ladder.name, missingReason(modes))
                decided.size > 1 ->
                    skipped += LadderSkip(id, ladder.name, "placements disagree on level vs dungeon")
                else -> {
                    val tag = tagFor(layout, decided.single()!!)
                    if (tag == null) {
                        skipped += LadderSkip(id, ladder.name, "middle ladder into a dungeon")
                    } else {
                        suggestions += LadderSuggestion(id, tag, coords.size)
                    }
                }
            }
        }
        return LadderPairingResult(suggestions, skipped)
    }

    private fun missingReason(modes: List<Mode?>): String {
        val missing = modes.count { it == null }
        return "$missing of ${modes.size} placements have no matching ladder above or below"
    }

    private fun tagFor(layout: Layout, mode: Mode): LadderTag? =
        when (mode) {
            Mode.Level ->
                when (layout) {
                    Layout.Up -> LadderTag.Up
                    Layout.Down -> LadderTag.Down
                    Layout.Option -> LadderTag.Option
                }
            Mode.Dungeon ->
                when (layout) {
                    Layout.Up -> LadderTag.DungeonUp
                    Layout.Down -> LadderTag.DungeonDown
                    Layout.Option -> null
                }
        }

    private fun modeAt(
        at: CoordGrid,
        layout: Layout,
        byTile: Map<Long, List<Int>>,
        type: (Int) -> LadderType?,
    ): Mode? {
        val goesUp = layout != Layout.Down
        val goesDown = layout != Layout.Up
        val upLevel = at.level + 1
        val downLevel = at.level - 1
        val levelUpOk = !goesUp || (upLevel <= MAX_LEVEL &&
            hasNear(byTile, type, at.x, at.z, upLevel) { canGoDown(it) })
        val levelDownOk = !goesDown || (downLevel >= 0 &&
            hasNear(byTile, type, at.x, at.z, downLevel) { canGoUp(it) })
        if (levelUpOk && levelDownOk) return Mode.Level
        if (layout == Layout.Option) return null

        val dungeonZ = if (layout == Layout.Up) at.z - DUNGEON_OFFSET else at.z + DUNGEON_OFFSET
        if (dungeonZ < 0) return null
        val wanted: (LadderType) -> Boolean =
            if (layout == Layout.Up) ::canGoDown else ::canGoUp
        return if (hasNear(byTile, type, at.x, dungeonZ, at.level, wanted)) Mode.Dungeon else null
    }

    private fun hasNear(
        byTile: Map<Long, List<Int>>,
        type: (Int) -> LadderType?,
        x: Int,
        z: Int,
        level: Int,
        matches: (LadderType) -> Boolean,
    ): Boolean {
        for (dx in -SEARCH_RADIUS..SEARCH_RADIUS) {
            for (dz in -SEARCH_RADIUS..SEARCH_RADIUS) {
                val ids = byTile[tileKey(x + dx, z + dz, level)] ?: continue
                if (ids.any { id -> type(id)?.let(matches) == true }) return true
            }
        }
        return false
    }

    private fun ops(type: LadderType): List<String> =
        (0 until 5).map { type.ops.getOrNull(it)?.trim()?.lowercase().orEmpty() }

    private fun isLadder(type: LadderType): Boolean = "ladder" in type.name.lowercase()

    private fun canGoUp(type: LadderType): Boolean {
        val ops = ops(type)
        return "climb-up" in ops || "climb" in ops || "climb up" in ops
    }

    private fun canGoDown(type: LadderType): Boolean {
        val ops = ops(type)
        return "climb-down" in ops || "climb" in ops || "climb down" in ops
    }

    private fun layoutOf(type: LadderType): Layout? {
        val ops = ops(type)
        return when {
            ops[0] == "climb-up" && "climb-down" !in ops -> Layout.Up
            ops[0] == "climb-down" && "climb-up" !in ops -> Layout.Down
            ops[0] == "climb" && ops[1] == "climb-up" && ops[2] == "climb-down" -> Layout.Option
            else -> null
        }
    }

    private fun tileKey(coords: CoordGrid): Long = tileKey(coords.x, coords.z, coords.level)

    private fun tileKey(x: Int, z: Int, level: Int): Long =
        (level.toLong() shl 40) or (x.toLong() shl 20) or z.toLong()
}
