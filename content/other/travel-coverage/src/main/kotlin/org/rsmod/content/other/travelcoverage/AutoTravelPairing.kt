package org.rsmod.content.other.travelcoverage

import org.rsmod.content.generic.locs.autotravel.TravelKind
import org.rsmod.content.generic.locs.autotravel.TravelLoc
import org.rsmod.content.generic.locs.autotravel.TravelTargets
import org.rsmod.content.generic.locs.autotravel.TravelType
import org.rsmod.map.CoordGrid

data class AutoTravelSuggestion(val symbol: String, val kind: TravelKind, val placements: Int)

data class TrapdoorSuggestion(val closed: String, val opened: String, val placements: Int)

data class AutoTravelResult(
    val travel: List<AutoTravelSuggestion>,
    val trapdoors: List<TrapdoorSuggestion>,
)

/**
 * Picks the staircases, gangplanks and trapdoors the click-time travel handler can serve: those
 * where at least half the placements have a far end within reach, as with ladders, since Jagex
 * copies whole buildings into instance template areas that hold only one end.
 */
object AutoTravelPairing {
    private const val NO_GROUP = 0xFFFF

    fun pair(
        placed: Map<Int, List<TravelLoc>>,
        unhandledIds: Set<Int>,
        type: (Int) -> TravelType?,
        contentGroup: (Int) -> Int,
        symbol: (Int) -> String,
        idOf: (String) -> Int?,
        locsAt: (CoordGrid) -> List<TravelLoc>,
    ): AutoTravelResult {
        val travel = mutableListOf<AutoTravelSuggestion>()
        val trapdoors = mutableListOf<TrapdoorSuggestion>()
        for (id in unhandledIds.sorted()) {
            val t = type(id) ?: continue
            if (!untagged(contentGroup(id))) continue
            val copies = placed[id].orEmpty()
            if (copies.isEmpty()) continue
            val kind = TravelTargets.kindOf(t)
            if (kind != null) {
                val reachable = copies.count { reaches(it, kind, locsAt, type) }
                if (reachable * 2 >= copies.size) travel += AutoTravelSuggestion(symbol(id), kind, copies.size)
                continue
            }
            if ("trapdoor" !in t.name.lowercase() || t.op(0) != "open") continue
            val opened =
                DoorPairing.openCandidates(symbol(id))
                    .mapNotNull { candidate -> idOf(candidate)?.let { candidate to it } }
                    .filter { (_, openId) ->
                        type(openId)?.op(0) == "climb-down" && untagged(contentGroup(openId))
                    }
            val open = opened.singleOrNull() ?: continue
            val reachable =
                copies.count { TravelTargets.counterpart(it, TravelKind.Down, false, locsAt, type) != null }
            if (reachable * 2 >= copies.size) trapdoors += TrapdoorSuggestion(symbol(id), open.first, copies.size)
        }
        return AutoTravelResult(travel, trapdoors)
    }

    private fun reaches(
        loc: TravelLoc,
        kind: TravelKind,
        locsAt: (CoordGrid) -> List<TravelLoc>,
        type: (Int) -> TravelType?,
    ): Boolean =
        when (kind) {
            TravelKind.Option ->
                TravelTargets.counterpart(loc, kind, true, locsAt, type) != null &&
                    TravelTargets.counterpart(loc, kind, false, locsAt, type) != null
            TravelKind.Down -> TravelTargets.counterpart(loc, kind, false, locsAt, type) != null
            else -> TravelTargets.counterpart(loc, kind, true, locsAt, type) != null
        }

    private fun untagged(group: Int): Boolean = group == -1 || group == NO_GROUP
}
