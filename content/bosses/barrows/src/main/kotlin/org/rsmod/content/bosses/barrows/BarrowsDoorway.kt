package org.rsmod.content.bosses.barrows

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

internal enum class BarrowsDoorway(val varbit: String) {
    NORTH_LONG_PASSAGE("varbit.barrows_door_a"),
    WEST_LONG_PASSAGE("varbit.barrows_door_b"),
    NORTH_WEST_TO_WEST("varbit.barrows_door_c"),
    NORTH_TO_NORTH_WEST("varbit.barrows_door_d"),
    NORTH_TO_CENTER("varbit.barrows_door_e"),
    NORTH_EAST_TO_NORTH("varbit.barrows_door_f"),
    EAST_TO_NORTH_EAST("varbit.barrows_door_g"),
    EAST_LONG_PASSAGE("varbit.barrows_door_h"),
    WEST_TO_CENTER("varbit.barrows_door_i"),
    EAST_TO_CENTER("varbit.barrows_door_j"),
    WEST_TO_SOUTH_WEST("varbit.barrows_door_k"),
    SOUTH_TO_CENTER("varbit.barrows_door_l"),
    SOUTH_EAST_TO_EAST("varbit.barrows_door_m"),
    SOUTH_WEST_TO_SOUTH("varbit.barrows_door_n"),
    SOUTH_TO_SOUTH_EAST("varbit.barrows_door_o"),
    SOUTH_LONG_PASSAGE("varbit.barrows_door_p");

    val locs: List<String> =
        varbit.removePrefix("varbit.barrows_door_").let { letter ->
            listOf("loc.barrows_door_${letter}_r", "loc.barrows_door_${letter}_l")
        }

    companion object {
        val CENTER = listOf(EAST_TO_CENTER, SOUTH_TO_CENTER, WEST_TO_CENTER, NORTH_TO_CENTER)

        private val byLocId: Map<Int, BarrowsDoorway> by lazy {
            entries.flatMap { door -> door.locs.map { it.asRSCM(RSCMType.LOC) to door } }.toMap()
        }

        fun forLoc(locId: Int): BarrowsDoorway? = byLocId[locId]
    }
}

internal val BarrowsCorner.doorways: List<BarrowsDoorway>
    get() =
        when (this) {
            BarrowsCorner.SOUTH_WEST ->
                listOf(
                    BarrowsDoorway.WEST_LONG_PASSAGE,
                    BarrowsDoorway.SOUTH_LONG_PASSAGE,
                    BarrowsDoorway.WEST_TO_SOUTH_WEST,
                    BarrowsDoorway.SOUTH_WEST_TO_SOUTH,
                )
            BarrowsCorner.NORTH_WEST ->
                listOf(
                    BarrowsDoorway.NORTH_LONG_PASSAGE,
                    BarrowsDoorway.WEST_LONG_PASSAGE,
                    BarrowsDoorway.NORTH_WEST_TO_WEST,
                    BarrowsDoorway.NORTH_TO_NORTH_WEST,
                )
            BarrowsCorner.NORTH_EAST ->
                listOf(
                    BarrowsDoorway.NORTH_LONG_PASSAGE,
                    BarrowsDoorway.EAST_LONG_PASSAGE,
                    BarrowsDoorway.EAST_TO_NORTH_EAST,
                    BarrowsDoorway.NORTH_EAST_TO_NORTH,
                )
            BarrowsCorner.SOUTH_EAST ->
                listOf(
                    BarrowsDoorway.SOUTH_LONG_PASSAGE,
                    BarrowsDoorway.EAST_LONG_PASSAGE,
                    BarrowsDoorway.SOUTH_TO_SOUTH_EAST,
                    BarrowsDoorway.SOUTH_EAST_TO_EAST,
                )
        }

internal var Player.openCornerDoorIndex by intVarBit("varbit.barrows_open_corner_door")
internal var Player.openCenterDoorIndex by intVarBit("varbit.barrows_open_center_door")

internal fun Player.shiftDoorways() {
    openCornerDoorIndex = corner.doorways.indices.random()
    openCenterDoorIndex = BarrowsDoorway.CENTER.indices.random()
}

internal val Player.shutDoorways: Set<BarrowsDoorway>
    get() {
        val cornerDoors = corner.doorways
        val openCorner = cornerDoors[openCornerDoorIndex.coerceIn(cornerDoors.indices)]
        val openCenter =
            BarrowsDoorway.CENTER[openCenterDoorIndex.coerceIn(BarrowsDoorway.CENTER.indices)]
        return (cornerDoors.filter { it != openCorner } +
                BarrowsDoorway.CENTER.filter { it != openCenter })
            .toSet()
    }

internal fun Player.refreshDoors(picking: Boolean) {
    val shut = if (picking) emptySet() else shutDoorways
    for (doorway in BarrowsDoorway.entries) {
        VarPlayerIntMapSetter.set(this, doorway.varbit, if (doorway in shut) 1 else 0)
    }
}
