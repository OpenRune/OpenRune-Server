package org.rsmod.content.events.leaguetasks.regions

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.enums.enum
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.RegionDataRow
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

/**
 * League region access, keyed by `region_data:region_id` as the client's own unlock check
 * (`[proc,script3681]`) reads `varbit.league_area_selection_0..5`. Tiles outside every region
 * area, such as Death's office or the POH, are open to everyone.
 *
 * Selection slot N opens once `varbit.league_total_tasks_completed` reaches the N-th threshold of
 * `enum.league_area_unlock_thresholds`, the same walk the client does in `[proc,script3682]`.
 */
@Singleton
public class LeagueRegions @Inject constructor(private val areas: AreaChecker) {
    private val areaByRegion: Map<Int, Int> =
        enum<Int, Int>("league_region_areas").filterValuesNotNull().backing
    private val rowByRegion: Map<Int, RegionDataRow> =
        RegionDataRow.all().filter { it.regionId in areaByRegion }.associateBy { it.regionId }
    private val unlockSpotanims: Map<Int, String> =
        enum<Int, Int>("league_area_unlock_spotanims").filterValuesNotNull().backing.mapValues {
            RSCM.getReverseMapping(RSCMType.SPOTANIM, it.value)
        }
    private val slotThresholds: List<Int> = loadSlotThresholds()

    public val selectable: Collection<RegionDataRow>
        get() = rowByRegion.values

    public fun region(regionId: Int): RegionDataRow? = rowByRegion[regionId]

    public fun unlockSpotanim(regionId: Int): String? = unlockSpotanims[regionId]

    public fun isUnlocked(player: Player, region: Int): Boolean =
        SELECTIONS.any { player.vars[it] == region }

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

    public fun pendingSlot(player: Player): Int? {
        val total = player.vars["varbit.league_total_tasks_completed"]
        for ((slot, threshold) in slotThresholds.withIndex()) {
            if (total < threshold) {
                return null
            }
            if (player.vars[SELECTIONS[slot]] == 0) {
                return slot
            }
        }
        return null
    }

    public fun allSlotsUsed(player: Player): Boolean =
        slotThresholds.indices.all { player.vars[SELECTIONS[it]] != 0 }

    public fun forcedRegion(player: Player, slot: Int): Int? =
        if (player.vars["varbit.league_type"] == DEMONIC_PACTS) {
            DEMONIC_PACTS_FORCED.getOrNull(slot)
        } else {
            MISTHALIN.takeIf { slot == 0 }
        }

    public fun isHidden(player: Player, region: Int): Boolean =
        player.vars["varbit.league_type"] == DEMONIC_PACTS && region == MISTHALIN

    public fun select(player: Player, slot: Int, region: Int) {
        VarPlayerIntMapSetter.set(player, SELECTIONS[slot], region)
    }

    private companion object {
        const val DEMONIC_PACTS = 6
        const val MISTHALIN = 1
        val DEMONIC_PACTS_FORCED = listOf(21, 2)

        val SELECTIONS =
            listOf(
                "varbit.league_area_selection_0",
                "varbit.league_area_selection_1",
                "varbit.league_area_selection_2",
                "varbit.league_area_selection_3",
                "varbit.league_area_selection_4",
                "varbit.league_area_selection_5",
            )

        fun loadSlotThresholds(): List<Int> {
            val tasksParam = "param.league_area_unlock_tasks".asRSCM(RSCMType.PARAM)
            return enum<Int, Int>("league_area_unlock_thresholds")
                .filterValuesNotNull()
                .backing
                .toSortedMap()
                .values
                .mapNotNull { ServerCacheManager.getStruct(it)?.params?.get(tasksParam) as? Int }
        }
    }
}
