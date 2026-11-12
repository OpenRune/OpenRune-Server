package org.rsmod.content.generic.locs.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

/**
 * Doors, gates and trapdoors opened with the Thieving "Pick-lock" op; levels and xp are the wiki's.
 * [EXIT] is where a picked trapdoor drops the player; doors and gates hold [WALK_THROUGH] and the
 * player steps through them instead.
 */
object LockpickDoorsTable {
    const val LOC = 0
    const val LEVEL = 1
    const val XP_TENTHS = 2
    const val NEEDS_LOCKPICK = 3
    const val EXIT = 4
    val WALK_THROUGH = CoordGrid(0, 0, 0)

    fun doors() =
        dbTable("dbtable.lockpick_doors", serverOnly = true) {
            column("loc", LOC, VarType.LOC)
            column("level", LEVEL, VarType.INT)
            column("xp_tenths", XP_TENTHS, VarType.INT)
            column("needs_lockpick", NEEDS_LOCKPICK, VarType.BOOLEAN)
            column("exit", EXIT, VarType.COORDGRID)

            fun door(
                row: String,
                loc: String,
                level: Int,
                xpTenths: Int,
                lockpick: Boolean = false,
                exit: CoordGrid = WALK_THROUGH,
            ) {
                row(row) {
                    columnRSCM(LOC, loc)
                    column(LEVEL, level)
                    column(XP_TENTHS, xpTenths)
                    column(NEEDS_LOCKPICK, lockpick)
                    columnCoord(EXIT, exit)
                }
            }

            door("dbrow.lockpick_door_ardougne_coin_chest", "loc.picklock1", 1, 32)
            door("dbrow.lockpick_door_ardougne_nature_chest", "loc.picklock2", 16, 150)
            door("dbrow.lockpick_door_ardougne_sewer_gate_l", "loc.picklock3_l", 31, 250)
            door("dbrow.lockpick_door_ardougne_sewer_gate_r", "loc.picklock3_r", 31, 250)
            door("dbrow.lockpick_door_chaos_druid_tower", "loc.picklock4", 46, 375)
            door("dbrow.lockpick_door_ardougne_castle", "loc.picklock5", 61, 500)
            door("dbrow.lockpick_door_magic_axe_hut", "loc.toollock2", 23, 225, lockpick = true)
            door("dbrow.lockpick_door_pirates_hideout", "loc.toollock3", 39, 350, lockpick = true)
            door("dbrow.lockpick_door_yanille_dungeon", "loc.toollock5", 82, 500, lockpick = true)
            door(
                "dbrow.lockpick_door_ham_trapdoor",
                "loc.osf_trapdoor_closed",
                1,
                38,
                exit = CoordGrid(3149, 9652, 0),
            )
        }
}
