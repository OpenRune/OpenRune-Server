package org.rsmod.content.events.leaguetasks.regions

import dev.openrune.types.enums.enum
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.table.RegionDataRow
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

/**
 * League region access, keyed by `region_data:region_id` as the client's own unlock check
 * (`[proc,script3681]`) reads `varbit.league_area_selection_0..5`. Tiles outside every region
 * area, such as Death's office or the POH, are open to everyone.
 */
@Singleton
public class LeagueRegions @Inject constructor(private val areas: AreaChecker) {
    private val areaByRegion: Map<Int, Int> =
        enum<Int, Int>("league_region_areas").filterValuesNotNull().backing
    private val rowByRegion: Map<Int, RegionDataRow> =
        RegionDataRow.all().filter { it.regionId in areaByRegion }.associateBy { it.regionId }

    public fun isUnlocked(player: Player, region: Int): Boolean =
        SELECTIONS.any { player.vars[it] == region }

    /** The region that locks [coords] for [player], or null when it is open to them. */
    public fun lockedRegion(player: Player, coords: CoordGrid): RegionDataRow? {
        var locked: Int? = null
        for ((region, area) in areaByRegion) {
            if (!areas.inArea(area, coords)) {
                continue
            }
            if (isUnlocked(player, region)) {
                return null
            }
            locked = locked ?: region
        }
        return locked?.let(rowByRegion::get)
    }

    private companion object {
        val SELECTIONS =
            listOf(
                "varbit.league_area_selection_0",
                "varbit.league_area_selection_1",
                "varbit.league_area_selection_2",
                "varbit.league_area_selection_3",
                "varbit.league_area_selection_4",
                "varbit.league_area_selection_5",
            )
    }
}
