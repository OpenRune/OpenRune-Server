package org.rsmod.content.skills.farming

import java.util.concurrent.ThreadLocalRandom
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

object FarmingClock {
    private const val OFFSET = "varp.farming_growth_offset"
    private const val INITIALIZED = 32
    internal const val MINUTE_MILLIS = 60_000L

    fun offset(player: Player): Int {
        val saved = player.vars[OFFSET]
        if (saved and INITIALIZED != 0) return saved and 31
        val offset = ThreadLocalRandom.current().nextInt(32)
        VarPlayerIntMapSetter.set(player, OFFSET, INITIALIZED or offset)
        return offset
    }

    fun cycle(player: Player, minutes: Int, millis: Long): Long =
        cycle(offset(player), minutes, millis)

    internal fun cycle(offset: Int, minutes: Int, millis: Long): Long {
        require(minutes > 0)
        return Math.floorDiv(Math.floorDiv(millis, MINUTE_MILLIS) + offset, minutes.toLong())
    }

    internal fun ticksUntilNext(player: Player, minutes: Int, millis: Long): Int {
        val next = (cycle(player, minutes, millis) + 1) * minutes * MINUTE_MILLIS -
            offset(player) * MINUTE_MILLIS
        return ((next - millis + 599) / 600).toInt().coerceAtLeast(1)
    }
}
