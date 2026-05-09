package dev.openrune.tables

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

data class StatRow(
    val rowName: String,
    val componentId: String,
    val statString: String,
    val bit: Int
)

object StatComponents {

    const val COL_COMPONENT = 0
    const val COL_STAT = 1
    const val COL_BIT = 2

    fun statsComponents() = dbTable("dbtable.stat_components", serverOnly = true) {

        column("component", COL_COMPONENT, VarType.COMPONENT)
        column("stat", COL_STAT, VarType.STAT)
        column("bit", COL_BIT, VarType.INT)

        val skillsWithBits = listOf(
            StatRow("dbrow.agility_stat", "component.stats:agility", "stat.agility", 8),
            StatRow("dbrow.attack_stat", "component.stats:attack", "stat.attack", 1),
            StatRow("dbrow.construction_stat", "component.stats:construction", "stat.construction", 22),
            StatRow("dbrow.cooking_stat", "component.stats:cooking", "stat.cooking", 16),
            StatRow("dbrow.crafting_stat", "component.stats:crafting", "stat.crafting", 11),
            StatRow("dbrow.defence_stat", "component.stats:defence", "stat.defence", 5),
            StatRow("dbrow.farming_stat", "component.stats:farming", "stat.farming", 21),
            StatRow("dbrow.firemaking_stat", "component.stats:firemaking", "stat.firemaking", 17),
            StatRow("dbrow.fishing_stat", "component.stats:fishing", "stat.fishing", 15),
            StatRow("dbrow.fletching_stat", "component.stats:fletching", "stat.fletching", 19),
            StatRow("dbrow.herblore_stat", "component.stats:herblore", "stat.herblore", 9),
            StatRow("dbrow.hitpoints_stat", "component.stats:hitpoints", "stat.hitpoints", 6),
            StatRow("dbrow.hunter_stat", "component.stats:hunter", "stat.hunter", 23),
            StatRow("dbrow.magic_stat", "component.stats:magic", "stat.magic", 4),
            StatRow("dbrow.mining_stat", "component.stats:mining", "stat.mining", 13),
            StatRow("dbrow.prayer_stat", "component.stats:prayer", "stat.prayer", 7),
            StatRow("dbrow.ranged_stat", "component.stats:ranged", "stat.ranged", 3),
            StatRow("dbrow.runecraft_stat", "component.stats:runecraft", "stat.runecrafting", 12),
            StatRow("dbrow.slayer_stat", "component.stats:slayer", "stat.slayer", 20),
            StatRow("dbrow.smithing_stat", "component.stats:smithing", "stat.smithing", 14),
            StatRow("dbrow.strength_stat", "component.stats:strength", "stat.strength", 2),
            StatRow("dbrow.thieving_stat", "component.stats:thieving", "stat.thieving", 10),
            StatRow("dbrow.woodcutting_stat", "component.stats:woodcutting", "stat.woodcutting", 18),
            StatRow("dbrow.sailing_stat", "component.stats:sailing", "stat.sailing", 24)
        )

        skillsWithBits.forEach { row ->
            row(row.rowName) {
                columnRSCM(COL_COMPONENT, row.componentId)
                columnRSCM(COL_STAT, row.statString)
                column(COL_BIT, row.bit)
            }
        }
    }
}
