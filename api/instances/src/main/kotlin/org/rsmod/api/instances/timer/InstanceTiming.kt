package org.rsmod.api.instances.timer

import org.rsmod.api.instances.INSTANCE_TICKS_PER_MINUTE
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList

internal object InstanceTiming {

    fun warningThresholds(totalTicks: Int): List<Int> {
        if (totalTicks <= 0) return emptyList()
        return buildList {
                add(totalTicks / 2)
                add(totalTicks / 4)
                add(totalTicks / 8)
                add(INSTANCE_TICKS_PER_MINUTE)
                add(INSTANCE_TICKS_PER_MINUTE / 2)
            }
            .filter { it in 1..totalTicks }
            .distinct()
            .sortedDescending()
    }

    fun notifyThresholds(
        remainingTicks: Int,
        totalTicks: Int,
        sent: MutableSet<Int>,
        players: Iterable<Player>,
        message: (String) -> String,
    ) {
        warningThresholds(totalTicks)
            .filter { remainingTicks <= it && sent.add(it) }
            .forEach { threshold ->
                val text = message(formatRemaining(threshold))
                players.forEach { it.mes(text, ChatType.Engine) }
            }
    }

    fun formatRemaining(ticks: Int): String {
        if (ticks >= INSTANCE_TICKS_PER_MINUTE) {
            val minutes = ticks / INSTANCE_TICKS_PER_MINUTE
            return "$minutes minute${if (minutes == 1) "" else "s"}"
        }

        val seconds = ticks * 60 / INSTANCE_TICKS_PER_MINUTE
        return "$seconds second${if (seconds == 1) "" else "s"}"
    }

    fun playersIn(session: InstanceSession, playerList: PlayerList) =
        session.occupants.mapNotNull { uuid -> playerList.firstOrNull { it.uuid == uuid } }
}
