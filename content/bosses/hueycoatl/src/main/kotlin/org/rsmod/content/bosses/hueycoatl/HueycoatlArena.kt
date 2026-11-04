package org.rsmod.content.bosses.hueycoatl

import kotlin.math.ceil
import kotlin.math.floor
import org.rsmod.map.CoordGrid

internal object HueycoatlArena {
    fun inParticipation(origin: CoordGrid, coords: CoordGrid): Boolean {
        val dx = coords.x - origin.x
        val dz = coords.z - origin.z
        return coords.level == origin.level && dx in 0..30 && dz in 0..28
    }

    fun inHazard(origin: CoordGrid, coords: CoordGrid): Boolean {
        val dx = coords.x - origin.x
        val dz = coords.z - origin.z
        if (coords.level != origin.level) return false
        val body = dx in 12..27 && dz in 0..12
        val summit = dx in 0..15 && dz in 12..21
        return body || summit
    }

    fun inDropRange(origin: CoordGrid, coords: CoordGrid): Boolean {
        if (!inHazard(origin, coords)) return false
        val dx = coords.x - origin.x
        val dz = coords.z - origin.z
        if (dz >= 12 && dx >= 13) return false
        if (dz < 12 && dx >= 25) return false
        return true
    }

    fun requiredPrayers(teamSize: Int): Int =
        if (teamSize <= 0) 1 else maxOf(1, ceil((teamSize - 2) / 3.0).toInt())

    fun qualifies(
        teamSize: Int,
        inDropRange: Boolean,
        bodyDamage: Int,
        headDamage: Int,
        tailDamage: Int,
        neutralDamage: Int,
        directDamage: Int,
    ): Boolean {
        if (!inDropRange) return false
        if (directDamage == 0 && neutralDamage < 120) return false
        return if (teamSize <= 5) {
            bodyDamage >= 25 && headDamage + tailDamage >= 75
        } else {
            bodyDamage >= 25 || bodyDamage + headDamage + tailDamage >= 75
        }
    }

    fun lootShare(lump: Int, damage: Int, totalDamage: Int, bodyDamage: Int): Int {
        if (lump <= 0) return 0
        val total = totalDamage.coerceAtLeast(1)
        val raw = floor(lump.toDouble() * damage / total).toInt()
        val base = maxOf(1, raw)
        return if (bodyDamage < 25) maxOf(1, floor(base * 0.05).toInt()) else base
    }

    fun tailDamageCap(crushIsHighest: Boolean): Int = if (crushIsHighest) 9 else 4

    fun formatDuration(ticks: Int): String {
        val totalHundredths = ticks * 60
        val minutes = totalHundredths / 6000
        val seconds = (totalHundredths % 6000) / 100
        val hundredths = totalHundredths % 100
        return "%d:%02d.%02d".format(minutes, seconds, hundredths)
    }
}
