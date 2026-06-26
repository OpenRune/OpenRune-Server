package dev.openrune.tables

import dev.openrune.definition.dbtables.DBRowBuilder
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import org.rsmod.map.CoordGrid

fun DBRowBuilder.columnCoord(id: Int, coord: CoordGrid) {
    column(id, coord.packed)
}

object InstanceSettingsTable {

    const val KEY = 0
    const val EXIT_COORD = 1
    const val FEE = 2
    const val MAX_PLAYERS = 3
    const val TIME_LIMIT_MINUTES = 4
    const val GRACE_MINUTES = 5
    const val BOSS_NPC = 6
    const val BOSS_NAME = 7
    const val RECOMMENDED_COMBAT = 8
    const val TEAM_SIZE = 9
    const val LOOT_MULTIPLIER = 10
    const val DESCRIPTION = 11
    const val ENTER_COORD = 12
    const val ENTER_OBJECT = 13
    const val EXIT_OBJECT = 14

    fun instanceSettings() = dbTable("dbtable.instance_settings", serverOnly = true) {
        column("key", KEY, VarType.STRING)
        column("exit_coord", EXIT_COORD, VarType.COORDGRID)
        column("fee", FEE, VarType.INT)
        column("max_players", MAX_PLAYERS, VarType.INT)
        column("time_limit_minutes", TIME_LIMIT_MINUTES, VarType.INT)
        column("grace_minutes", GRACE_MINUTES, VarType.INT)
        column("boss_npc", BOSS_NPC, VarType.NPC)
        column("boss_name", BOSS_NAME, VarType.STRING)
        column("recommended_combat", RECOMMENDED_COMBAT, VarType.INT)
        column("team_size", TEAM_SIZE, VarType.INT)
        column("loot_multiplier", LOOT_MULTIPLIER, VarType.STRING)
        column("description", DESCRIPTION, VarType.STRING)
        column("enter_coord", ENTER_COORD, VarType.COORDGRID)
        column("enter_object", ENTER_OBJECT, VarType.LOC)
        column("exit_object", EXIT_OBJECT, VarType.LOC)

        row("dbrow.instance_scurrius") {
            column(KEY, "scurrius")
            columnCoord(EXIT_COORD, CoordGrid(3281, 9870))
            columnCoord(ENTER_COORD, CoordGrid(3290, 9868))
            column(FEE, 0)
            column(MAX_PLAYERS, 20)
            column(TIME_LIMIT_MINUTES, 0)
            column(GRACE_MINUTES, 10)
            columnRSCM(BOSS_NPC, "npc.rat_boss_normal","npc.rat_boss_instance")
            column(BOSS_NAME, "Scurrius")
            column(RECOMMENDED_COMBAT, 60,90)
            column(TEAM_SIZE, 20)
            column(LOOT_MULTIPLIER, "x1.0")
            column(DESCRIPTION, "King of the rats.")
            columnRSCM(ENTER_OBJECT, "loc.rat_boss_entrance")
            columnRSCM(EXIT_OBJECT, "loc.rat_boss_exit")
        }
    }
}
