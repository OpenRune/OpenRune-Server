package dev.openrune.tables

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

data class LevelUpRow(
    val label: String,
    val layer: String,
    val jingle: String,
    val rule: LevelUpRule,
    val alternateJingle: String? = null,
    val threshold: Int? = null,
    val prefix: String? = null
)

data class StatRow(
    val rowName: String,
    val componentId: String,
    val statString: String,
    val bit: Int,
    val levelUp: LevelUpRow
)

object StatComponents {

    const val COL_COMPONENT = 0
    const val COL_STAT = 1
    const val COL_BIT = 2
    const val COL_LEVELUP_LABEL = 3
    const val COL_LEVELUP_LAYER = 4
    const val COL_LEVELUP_JINGLE = 5
    const val COL_LEVELUP_JINGLE_ALT = 6
    const val COL_LEVELUP_RULE = 7
    const val COL_LEVELUP_THRESHOLD = 8
    const val COL_LEVELUP_PREFIX = 9

    private fun levelUp(
        label: String,
        rule: LevelUpRule,
        jingle: String,
        alternateJingle: String? = null,
        threshold: Int? = null,
        prefix: String? = null
    ) = LevelUpRow(
        label, "component.levelup_display:${label.lowercase()}", "jingle.$jingle", rule,
        alternateJingle?.let { "jingle.$it" }, threshold, prefix
    )

    private fun unlocks(label: String, jingle: String) =
        levelUp(label, LevelUpRule.Unlocks, jingle, "${jingle}2")

    fun statsComponents() = dbTable("dbtable.stat_components", serverOnly = true) {

        column("component", COL_COMPONENT, VarType.COMPONENT)
        column("stat", COL_STAT, VarType.STAT)
        column("bit", COL_BIT, VarType.INT)
        column("levelup_label", COL_LEVELUP_LABEL, VarType.STRING)
        column("levelup_layer", COL_LEVELUP_LAYER, VarType.COMPONENT)
        column("levelup_jingle", COL_LEVELUP_JINGLE, VarType.INT)
        column("levelup_jingle_alt", COL_LEVELUP_JINGLE_ALT, VarType.INT)
        column("levelup_rule", COL_LEVELUP_RULE, VarType.INT)
        column("levelup_threshold", COL_LEVELUP_THRESHOLD, VarType.INT)
        column("levelup_prefix", COL_LEVELUP_PREFIX, VarType.STRING)

        val skillsWithBits = listOf(
            StatRow("dbrow.agility_stat", "component.stats:agility", "stat.agility", 8,
                levelUp("Agility", LevelUpRule.Single, "advance_agility")),
            StatRow("dbrow.attack_stat", "component.stats:attack", "stat.attack", 1,
                unlocks("Attack", "advance_attack")),
            StatRow("dbrow.construction_stat", "component.stats:construction", "stat.construction", 22,
                levelUp("Construction", LevelUpRule.EveryTenth, "advance_carpentry", "advance_carpentry2")),
            StatRow("dbrow.cooking_stat", "component.stats:cooking", "stat.cooking", 16,
                unlocks("Cooking", "advance_cooking")),
            StatRow("dbrow.crafting_stat", "component.stats:crafting", "stat.crafting", 11,
                unlocks("Crafting", "advance_crafting")),
            StatRow("dbrow.defence_stat", "component.stats:defence", "stat.defence", 5,
                unlocks("Defence", "advance_defense")),
            StatRow("dbrow.farming_stat", "component.stats:farming", "stat.farming", 21,
                levelUp("Farming", LevelUpRule.Unlocks, "farming_levelup", "farming_levelup_2")),
            StatRow("dbrow.firemaking_stat", "component.stats:firemaking", "stat.firemaking", 17,
                unlocks("Firemaking", "advance_firemarking")),
            StatRow("dbrow.fishing_stat", "component.stats:fishing", "stat.fishing", 15,
                unlocks("Fishing", "advance_fishing")),
            StatRow("dbrow.fletching_stat", "component.stats:fletching", "stat.fletching", 19,
                unlocks("Fletching", "advance_fletching")),
            StatRow("dbrow.herblore_stat", "component.stats:herblore", "stat.herblore", 9,
                unlocks("Herblore", "advance_herblaw")),
            StatRow("dbrow.hitpoints_stat", "component.stats:hitpoints", "stat.hitpoints", 6,
                levelUp("Hitpoints", LevelUpRule.FromLevel, "advance_hitpoints", "advance_hitpoints2",
                    threshold = 50, prefix = "Your Hitpoints are now")),
            StatRow("dbrow.hunter_stat", "component.stats:hunter", "stat.hunter", 23,
                levelUp("Hunter", LevelUpRule.Parity, "advance_hunting", "advance_hunting2")),
            StatRow("dbrow.magic_stat", "component.stats:magic", "stat.magic", 4,
                unlocks("Magic", "advance_magic")),
            StatRow("dbrow.mining_stat", "component.stats:mining", "stat.mining", 13,
                unlocks("Mining", "advance_mining")),
            StatRow("dbrow.prayer_stat", "component.stats:prayer", "stat.prayer", 7,
                unlocks("Prayer", "advance_prayer")),
            StatRow("dbrow.ranged_stat", "component.stats:ranged", "stat.ranged", 3,
                unlocks("Ranged", "advance_ranged")),
            StatRow("dbrow.runecraft_stat", "component.stats:runecraft", "stat.runecrafting", 12,
                unlocks("Runecraft", "advance_runecraft")),
            StatRow("dbrow.slayer_stat", "component.stats:slayer", "stat.slayer", 20,
                unlocks("Slayer", "advance_slayer")),
            StatRow("dbrow.smithing_stat", "component.stats:smithing", "stat.smithing", 14,
                levelUp("Smithing", LevelUpRule.GuideList, "advance_smithing", "advance_smithing2")),
            StatRow("dbrow.strength_stat", "component.stats:strength", "stat.strength", 2,
                levelUp("Strength", LevelUpRule.FromLevel, "advance_strength", "advance_strength2",
                    threshold = 50)),
            StatRow("dbrow.thieving_stat", "component.stats:thieving", "stat.thieving", 10,
                unlocks("Thieving", "advance_thieving")),
            StatRow("dbrow.woodcutting_stat", "component.stats:woodcutting", "stat.woodcutting", 18,
                unlocks("Woodcutting", "advance_woodcutting")),
            StatRow("dbrow.sailing_stat", "component.stats:sailing", "stat.sailing", 24,
                levelUp("Sailing", LevelUpRule.MaxLevel, "advance_sailing", "advance_sailing2"))
        )

        skillsWithBits.forEach { row ->
            row(row.rowName) {
                columnRSCM(COL_COMPONENT, row.componentId)
                columnRSCM(COL_STAT, row.statString)
                column(COL_BIT, row.bit)

                val levelUp = row.levelUp
                column(COL_LEVELUP_LABEL, levelUp.label)
                columnRSCM(COL_LEVELUP_LAYER, levelUp.layer)
                columnRSCM(COL_LEVELUP_JINGLE, levelUp.jingle)
                levelUp.alternateJingle?.let { columnRSCM(COL_LEVELUP_JINGLE_ALT, it) }
                column(COL_LEVELUP_RULE, levelUp.rule.id)
                levelUp.threshold?.let { column(COL_LEVELUP_THRESHOLD, it) }
                levelUp.prefix?.let { column(COL_LEVELUP_PREFIX, it) }
            }
        }
    }
}
