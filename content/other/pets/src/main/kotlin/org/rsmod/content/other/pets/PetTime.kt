package org.rsmod.content.other.pets

private const val TICKS_PER_MINUTE = 100

internal fun Int.ticksToMinutes(): Int = this / TICKS_PER_MINUTE

internal fun Int.minutesAsText(): String = "${this / 60} hour(s) ${this % 60} minutes"
