package org.rsmod.api.bosses.runtime

internal fun ceilingImpactTicks(endTime: Int): Int = maxOf(1, (endTime + 29) / 30) + 1
