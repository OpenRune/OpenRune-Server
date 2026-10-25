package org.rsmod.content.raids.toa.raid

import org.rsmod.api.random.GameRandom

internal fun <T> GameRandom.shuffled(list: List<T>): List<T> {
    val copy = list.toMutableList()
    for (i in copy.lastIndex downTo 1) {
        val j = of(maxExclusive = i + 1)
        val swap = copy[i]
        copy[i] = copy[j]
        copy[j] = swap
    }
    return copy
}
