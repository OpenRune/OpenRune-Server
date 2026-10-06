package org.rsmod.api.stats.plugin.levelup

import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.tables.LevelUpRule
import dev.openrune.types.StatType
import org.rsmod.api.table.StatComponentsRow

internal class LevelUpStat(row: StatComponentsRow) {
    val stat: StatType = row.stat
    val label: String = row.levelupLabel
    val layer: ComponentType = row.levelupLayer
    val levelPrefix: String = row.levelupPrefix ?: "Your $label level is now"
    val jingle: LevelUpJingle =
        LevelUpJingle(
            LevelUpRule.of(row.levelupRule),
            row.levelupJingle,
            row.levelupJingleAlt,
            row.levelupThreshold,
        )

    companion object {
        const val COMBAT_LABEL: String = "Combat"
        const val COMBAT_LAYER: String = "component.levelup_display:combat"
        const val COMBAT_JINGLE: String = "jingle.combat_level_up"

        val all: List<LevelUpStat> by lazy { StatComponentsRow.all().map(::LevelUpStat) }

        private val byStat: Map<Int, LevelUpStat> by lazy { all.associateBy { it.stat.id } }

        fun of(stat: StatType): LevelUpStat? = byStat[stat.id]
    }
}
