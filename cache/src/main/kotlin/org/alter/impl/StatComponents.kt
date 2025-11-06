package org.alter.impl

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

    fun statsComponents() = dbTable("tables.stat_components") {

        column("component", COL_COMPONENT, VarType.COMPONENT)
        column("stat", COL_STAT, VarType.STAT)
        column("bit", COL_BIT, VarType.INT)

        val skillsWithBits = listOf(
            StatRow("dbrows.agility_stat", "components.stats:agility", "stats.agility", 8),
            StatRow("dbrows.attack_stat", "components.stats:attack", "stats.attack", 1),
            StatRow("dbrows.construction_stat", "components.stats:construction", "stats.construction", 22),
            StatRow("dbrows.cooking_stat", "components.stats:cooking", "stats.cooking", 16),
            StatRow("dbrows.crafting_stat", "components.stats:crafting", "stats.crafting", 11),
            StatRow("dbrows.defence_stat", "components.stats:defence", "stats.defence", 5),
            StatRow("dbrows.farming_stat", "components.stats:farming", "stats.farming", 21),
            StatRow("dbrows.firemaking_stat", "components.stats:firemaking", "stats.firemaking", 17),
            StatRow("dbrows.fishing_stat", "components.stats:fishing", "stats.fishing", 15),
            StatRow("dbrows.fletching_stat", "components.stats:fletching", "stats.fletching", 19),
            StatRow("dbrows.herblore_stat", "components.stats:herblore", "stats.herblore", 9),
            StatRow("dbrows.hitpoints_stat", "components.stats:hitpoints", "stats.hitpoints", 6),
            StatRow("dbrows.hunter_stat", "components.stats:hunter", "stats.hunter", 23),
            StatRow("dbrows.magic_stat", "components.stats:magic", "stats.magic", 4),
            StatRow("dbrows.mining_stat", "components.stats:mining", "stats.mining", 13),
            StatRow("dbrows.prayer_stat", "components.stats:prayer", "stats.prayer", 7),
            StatRow("dbrows.ranged_stat", "components.stats:ranged", "stats.ranged", 3),
            StatRow("dbrows.runecraft_stat", "components.stats:runecraft", "stats.runecraft", 12),
            StatRow("dbrows.slayer_stat", "components.stats:slayer", "stats.slayer", 20),
            StatRow("dbrows.smithing_stat", "components.stats:smithing", "stats.smithing", 14),
            StatRow("dbrows.strength_stat", "components.stats:strength", "stats.strength", 2),
            StatRow("dbrows.thieving_stat", "components.stats:thieving", "stats.thieving", 10),
            StatRow("dbrows.woodcutting_stat", "components.stats:woodcutting", "stats.woodcutting", 18),
            StatRow("dbrows.sailing_stat", "components.stats:sailing", "stats.sailing", 19)
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