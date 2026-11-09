package org.rsmod.content.bosses.barrows

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

internal enum class BarrowsDoorway(val varbit: String, right: String, left: String) {
    NORTH_LONG_PASSAGE("varbit.barrows_door_a", "loc.barrows_door_a_r", "loc.barrows_door_a_l"),
    WEST_LONG_PASSAGE("varbit.barrows_door_b", "loc.barrows_door_b_r", "loc.barrows_door_b_l"),
    NORTH_WEST_TO_WEST("varbit.barrows_door_c", "loc.barrows_door_c_r", "loc.barrows_door_c_l"),
    NORTH_TO_NORTH_WEST("varbit.barrows_door_d", "loc.barrows_door_d_r", "loc.barrows_door_d_l"),
    NORTH_TO_CENTER("varbit.barrows_door_e", "loc.barrows_door_e_r", "loc.barrows_door_e_l"),
    NORTH_EAST_TO_NORTH("varbit.barrows_door_f", "loc.barrows_door_f_r", "loc.barrows_door_f_l"),
    EAST_TO_NORTH_EAST("varbit.barrows_door_g", "loc.barrows_door_g_r", "loc.barrows_door_g_l"),
    EAST_LONG_PASSAGE("varbit.barrows_door_h", "loc.barrows_door_h_r", "loc.barrows_door_h_l"),
    WEST_TO_CENTER("varbit.barrows_door_i", "loc.barrows_door_i_r", "loc.barrows_door_i_l"),
    EAST_TO_CENTER("varbit.barrows_door_j", "loc.barrows_door_j_r", "loc.barrows_door_j_l"),
    WEST_TO_SOUTH_WEST("varbit.barrows_door_k", "loc.barrows_door_k_r", "loc.barrows_door_k_l"),
    SOUTH_TO_CENTER("varbit.barrows_door_l", "loc.barrows_door_l_r", "loc.barrows_door_l_l"),
    SOUTH_EAST_TO_EAST("varbit.barrows_door_m", "loc.barrows_door_m_r", "loc.barrows_door_m_l"),
    SOUTH_WEST_TO_SOUTH("varbit.barrows_door_n", "loc.barrows_door_n_r", "loc.barrows_door_n_l"),
    SOUTH_TO_SOUTH_EAST("varbit.barrows_door_o", "loc.barrows_door_o_r", "loc.barrows_door_o_l"),
    SOUTH_LONG_PASSAGE("varbit.barrows_door_p", "loc.barrows_door_p_r", "loc.barrows_door_p_l");

    val locs: List<String> = listOf(right, left)

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
