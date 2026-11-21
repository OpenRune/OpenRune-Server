package org.rsmod.content.skills.agility.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack
import org.rsmod.content.skills.agility.pack.arena.AgilityArenaTables

class AgilityPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> =
        listOf(
            AgilityCoursesTable.courses(),
            AgilityObstaclesTable.obstacles(),
            AgilityArenaTables.traps(),
            AgilityArenaTables.arena(),
            AgilityLapListTable.lapList(),
            AgilitySkullballTable.skullball(),
        ) + agilityRewardTables() + agilityTransformTables()
}
