package org.rsmod.content.minigames.giantsfoundry

import org.rsmod.api.enums.GiantsFoundryEnums
import org.rsmod.api.table.GiantsFoundryToolsRow

internal object FoundryFormulas {
    const val SCALE = 1000
    const val CRUCIBLE_CAPACITY = 28

    fun metalScore(counts: IntArray): Int {
        val top =
            counts.indices
                .filter { counts[it] > 0 }
                .sortedWith(compareByDescending<Int> { counts[it] }.thenByDescending { it })
                .take(2)
        val values = top.map { 10.0 * (it + 1) * counts[it] / CRUCIBLE_CAPACITY }
        val v1 = values.getOrElse(0) { 0.0 }
        val v2 = values.getOrElse(1) { 0.0 }
        val sum =
            Math.floor(10 * v1).toInt() + Math.floor(10 * v2).toInt() + Math.floor(v1 * v2).toInt()
        return sum / 10
    }

    /** Sections on the progress bar; mirrors clientscript `giants_foundry_section_count`. */
    fun sections(difficulty: Int): Int =
        when {
            difficulty < 20 -> 3
            difficulty < 60 -> 4
            difficulty < 90 -> 5
            difficulty < 120 -> 6
            else -> 7
        }

    /** Section the progress is in; mirrors clientscript `script6120` (`scale`). */
    fun section(completion: Int, difficulty: Int): Int =
        (completion * sections(difficulty) / SCALE).coerceAtMost(sections(difficulty) - 1)

    /** Mirrors clientscript `script6121`, which the HUD uses to highlight the usable tool. */
    fun window(tool: FoundryTool, difficulty: Int): IntRange {
        val third = SCALE / 3
        val width = third - difficulty * (third - third / 2) / 130
        val start =
            when (tool) {
                FoundryTool.Hammer -> third / 2 + third * 2 - width / 2
                FoundryTool.Grindstone -> third / 2 + third - width / 2
                FoundryTool.PolishingWheel -> third / 2 - width / 2
            }
        return start..(start + width)
    }

    fun sweetSpots(difficulty: Int): Int = if (sections(difficulty) >= 6) 3 else 2

    fun handInXp(quality: Int): Int = (quality * quality / 73 + quality * 3 / 2 + 1) * 30

    fun heatStep(ticks: Int): Int = heatSteps.getValue(ticks.coerceAtMost(lastHeatStep))

    const val FAST_HEAT_OFFSET = 10

    private val heatSteps
        get() = GiantsFoundryEnums.giants_foundry_heat_steps

    private val lastHeatStep by lazy { heatSteps.keys.max() }
}

internal enum class FoundryTool(private val dbrow: String) {
    Hammer("dbrow.giants_foundry_tool_hammer"),
    Grindstone("dbrow.giants_foundry_tool_grindstone"),
    PolishingWheel("dbrow.giants_foundry_tool_polishing_wheel");

    val row: GiantsFoundryToolsRow by lazy { GiantsFoundryToolsRow.getRow(dbrow) }

    companion object {
        fun forSeed(seed: Int): FoundryTool = entries.first { it.row.seed == seed }
    }
}
