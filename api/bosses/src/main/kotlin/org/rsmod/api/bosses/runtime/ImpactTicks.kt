package org.rsmod.api.bosses.runtime

internal const val CLIENT_CYCLES_PER_TICK = 30

internal fun ceilingImpactTicks(endTime: Int): Int = maxOf(1, (endTime + 29) / 30) + 1
