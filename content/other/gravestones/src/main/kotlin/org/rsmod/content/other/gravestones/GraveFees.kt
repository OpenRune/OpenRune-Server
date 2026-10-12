package org.rsmod.content.other.gravestones

/**
 * Grave reclaim fees (https://oldschool.runescape.wiki/w/Grave): free under 100k, then 1,000,
 * 10,000 or 100,000 coins per item depending on its value band, capped at 500,000 in total.
 * Ironmen pay half. The client works out the same numbers in `gravestone_generic_window_set` and
 * `script1797` (osrs-dumps), which this follows, including its integer halving.
 */
internal object GraveFees {
    const val FREE_UNDER: Long = 100_000
    private const val BAND_1M: Long = 1_000_000
    private const val BAND_10M: Long = 10_000_000

    private const val FEE_UNDER_1M = 1_000L
    private const val FEE_UNDER_10M = 10_000L
    private const val FEE_FROM_10M = 100_000L
    private const val FEE_CAP = 500_000L

    fun isPaid(value: Long): Boolean = value >= FREE_UNDER

    /** The fee for one grave slot of [count] objs worth [value] each. */
    fun slotFee(value: Long, count: Int, ironman: Boolean): Long {
        val each =
            when {
                value >= BAND_10M -> FEE_FROM_10M
                value >= BAND_1M -> FEE_UNDER_10M
                value >= FREE_UNDER -> FEE_UNDER_1M
                else -> return 0
            }
        return discount(each * count, ironman).coerceAtMost(discount(FEE_CAP, ironman))
    }

    fun totalFee(slots: List<Pair<Long, Int>>, ironman: Boolean): Int =
        slots
            .sumOf { (value, count) -> slotFee(value, count, ironman) }
            .coerceAtMost(discount(FEE_CAP, ironman))
            .toInt()

    /**
     * The `gravestone_feethreshold_*` varbit for [threshold]: one past the first occupied slot
     * worth less than [threshold], or 0 when no occupied slot is. The client counts the slots
     * before it as the pricier bands.
     */
    fun threshold(values: List<Long?>, threshold: Long): Int {
        val index = values.indexOfFirst { it != null && it < threshold }
        return if (index == -1) 0 else index + 1
    }

    fun thresholds(values: List<Long?>): Triple<Int, Int, Int> =
        Triple(
            threshold(values, FREE_UNDER),
            threshold(values, BAND_1M),
            threshold(values, BAND_10M),
        )

    private fun discount(fee: Long, ironman: Boolean): Long = if (ironman) fee / 2 else fee
}
