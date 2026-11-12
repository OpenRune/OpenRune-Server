package org.rsmod.content.minigames.sepulchre

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.ObjectServerType
import org.rsmod.api.table.HallowedFloorsRow
import org.rsmod.map.CoordGrid

internal class Checkpoint(
    val southWest: CoordGrid,
    val northEast: CoordGrid,
    val respawn: CoordGrid,
) {
    fun contains(coords: CoordGrid): Boolean = coords.within(southWest, northEast)
}

internal class SepulchreFloor(
    val number: Int,
    val level: Int,
    val xpTenths: Int,
    val pb: String,
    val completions: String,
    val centre: CoordGrid,
    val starts: List<CoordGrid>,
    val southWest: CoordGrid,
    val northEast: CoordGrid,
    val damage: IntRange,
    val stairs: ObjectServerType?,
    val encounters: List<String>,
    val encounterChoices: List<String>,
    val checkpoints: List<Checkpoint>,
    val coffinMarks: IntRange,
    val lootTiers: List<Int>,
    val lockpickChance: Int,
    val clue: ItemServerType,
    val clueChance: Int,
    val page: ItemServerType,
    val pageFound: String,
    val petChance: Int,
    val treasureXpTenths: Int,
) {
    val isLast: Boolean
        get() = stairs == null

    fun contains(coords: CoordGrid): Boolean = coords.within(southWest, northEast)

    companion object {
        fun from(row: HallowedFloorsRow): SepulchreFloor {
            val (clue, clueChance) = row.clue.single()
            return SepulchreFloor(
                number = row.floor,
                level = row.level,
                xpTenths = row.xp,
                pb = varbit(row.pb),
                completions = RSCM.getReverseMapping(RSCMType.VARP, row.completions),
                centre = row.centre,
                starts = row.starts,
                southWest = row.bounds[0],
                northEast = row.bounds[1],
                damage = row.damage[0]..row.damage[1],
                stairs = row.stairs,
                encounters = row.encounters.map(::varbit),
                encounterChoices = row.encounterChoices.map(::varbit),
                checkpoints =
                    row.checkpoints.chunked(CHECKPOINT_TILES).map { (southWest, northEast, at) ->
                        Checkpoint(southWest, northEast, at)
                    },
                coffinMarks = row.coffinMarks[0]..row.coffinMarks[1],
                lootTiers = row.lootTiers,
                lockpickChance = row.lockpickChance,
                clue = clue,
                clueChance = clueChance,
                page = row.page,
                pageFound = varbit(row.pageFound),
                petChance = row.petChance,
                treasureXpTenths = row.treasureXp,
            )
        }

        private fun varbit(id: Int): String = RSCM.getReverseMapping(RSCMType.VARBIT, id)

        private const val CHECKPOINT_TILES = 3
    }
}

internal class SepulchreFloors(val all: List<SepulchreFloor>) {
    fun get(number: Int): SepulchreFloor = all.first { it.number == number }

    fun at(coords: CoordGrid): SepulchreFloor? = all.firstOrNull { it.contains(coords) }

    fun next(floor: SepulchreFloor): SepulchreFloor? =
        all.firstOrNull { it.number == floor.number + 1 }

    companion object {
        fun load(): SepulchreFloors =
            SepulchreFloors(
                HallowedFloorsRow.all().map(SepulchreFloor::from).sortedBy { it.number }
            )
    }
}

internal fun CoordGrid.within(southWest: CoordGrid, northEast: CoordGrid): Boolean =
    level in southWest.level..northEast.level &&
        x in southWest.x..northEast.x &&
        z in southWest.z..northEast.z
