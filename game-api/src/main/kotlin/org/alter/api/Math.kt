package org.alter.api

import kotlin.math.floor
import kotlin.random.Random

private val STATIC_RANDOM = Random.Default  // shared instance

fun success(low: Int, high: Int, level: Int, maxLevel: Int): Boolean {
    val rate = successRate(low, high, level, maxLevel)
    return rate > STATIC_RANDOM.nextDouble()
}

fun successRate(low: Int, high: Int, level: Int, maxLevel: Int): Double {
    val lowRate = (low * (maxLevel - level)) / (maxLevel - 1.0)
    val highRate = (high * (level - 1)) / (maxLevel - 1.0)
    return (1.0 + floor(lowRate + highRate + 0.5)) / 256.0
}