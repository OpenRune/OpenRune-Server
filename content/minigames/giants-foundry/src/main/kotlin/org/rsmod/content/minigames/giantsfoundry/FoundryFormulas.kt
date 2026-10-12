package org.rsmod.content.minigames.giantsfoundry

/**
 * Giants' Foundry maths. Temperature and progress are on the client's 0..1000 scale (varbits
 * `giants_foundry_preform_temperature` / `_completion`), quality on 0..255.
 */
internal object FoundryFormulas {
    const val SCALE = 1000
    const val CRUCIBLE_CAPACITY = 28

    /**
     * Metal score from the two most plentiful metals (ties go to the higher tier); tiers are
     * bronze 1 .. rune 6. https://oldschool.runescape.wiki/w/Giants%27_Foundry#Metal_score
     */
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

    /**
     * Temperature window `[start, start + width]` for [tool]; mirrors clientscript `script6121`,
     * which the HUD uses to highlight the usable tool.
     */
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

    /** Sweet spots per sword: 3 with six or more sections, otherwise 2. */
    fun sweetSpots(difficulty: Int): Int = if (sections(difficulty) >= 6) 3 else 2

    /**
     * Smithing XP for handing in a sword of quality [quality].
     * https://oldschool.runescape.wiki/w/Giants%27_Foundry#Rewards
     */
    fun handInXp(quality: Int): Int = (quality * quality / 73 + quality * 3 / 2 + 1) * 30

    /**
     * Heat added per tick at the lava pool (or removed at the waterfall) after [ticks] ticks of
     * the same action; "dunk"/"quench" start 10 steps in. Measured from the heat varbit by the
     * Easy Giants' Foundry RuneLite plugin (`HeatActionSolver.DX_1`, commit 9324c7f); the step
     * keeps growing past the logged values.
     */
    // UNVERIFIED: steps past the last logged value grow by 6 (an extrapolation).
    fun heatStep(ticks: Int): Int =
        if (ticks < HEAT_STEPS.size) {
            HEAT_STEPS[ticks]
        } else {
            HEAT_STEPS.last() + (ticks - HEAT_STEPS.lastIndex) * 6
        }

    const val FAST_HEAT_OFFSET = 10

    private val HEAT_STEPS =
        intArrayOf(
            7, 8, 9, 11, 13, 15, 17, 19, 21, 24, 27, 30, 33, 37, 41, 45, 49, 53, 57, 62, 67, 72,
            77, 83, 89, 95,
        )
}

/** The three refining stations; [seed] is the `giants_foundry_seed_*` varbit value. */
internal enum class FoundryTool(
    val seed: Int,
    /** Ticks between actions. */
    val period: Int,
    val progress: Int,
    val heat: Int,
    /** XP per successful action, in tenths. */
    val xpTenths: Int,
    val loc: String,
    /** The station's own animation while it works, if it has one. */
    val locSeq: String?,
) {
    // https://oldschool.runescape.wiki/w/Giants%27_Foundry#Refining_the_sword
    Hammer(0, 5, 20, -25, 45, "loc.giants_foundry_trip_hammer", "seq.giants_foundry_hammer01"),
    Grindstone(1, 2, 10, 15, 32, "loc.giants_foundry_grindstone", null),
    PolishingWheel(
        2,
        2,
        10,
        -17,
        33,
        "loc.giants_foundry_polishing_wheel",
        "seq.giants_foundry_polishing_wheel_spin",
    );

    companion object {
        fun forSeed(seed: Int): FoundryTool = entries.first { it.seed == seed }
    }
}
