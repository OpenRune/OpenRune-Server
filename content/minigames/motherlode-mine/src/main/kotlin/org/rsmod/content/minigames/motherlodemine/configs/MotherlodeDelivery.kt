package org.rsmod.content.minigames.motherlodemine.configs

import org.rsmod.api.delivery.DeliveryArrival
import org.rsmod.api.delivery.DeliveryLine
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

object MotherlodeDelivery {
    var Player.hasBiggerSackUnlocked by boolVarBit(MotherlodeConstants.VARBIT_BIGGER_SACK)

    var Player.sackTransmit by intVarBit(MotherlodeConstants.VARBIT_SACK_TRANSMIT)

    // Share one delivery line; the injected water-state callback controls its freeze.
    var waterStopped: (() -> Boolean)? = null

    var currentCycle: (() -> Int)? = null

    // motherlode_paydirt is a persistence key; do not rename it.
    val PAY_DIRT_LINE =
        DeliveryLine(
            id = "motherlode_paydirt",
            delayTicks = MotherlodeConstants.TRANSIT_TICKS,
            frozen = { waterStopped?.invoke() ?: false },
            pauseWhileFrozen = true,
            capacity = { player ->
                if (player.hasBiggerSackUnlocked) {
                    MotherlodeConstants.SACK_CAPACITY_BIG
                } else {
                    MotherlodeConstants.SACK_CAPACITY_NORMAL
                }
            },
            onArrival = { player, arrival ->
                val xp = MotherlodeConstants.arrivalXpFor(arrival.payload)
                if (xp > 0.0) {
                    player.statAdvance("stat.mining", xp)
                }

                player.sackTransmit = arrival.storedTotal.coerceIn(0, 255)

                notifyArrival(player, arrival)

                if (arrival.overflowed) {
                    player.mes("Your sack is full - some pay-dirt is lost.")
                }
            },
        )

    private fun notifyArrival(player: Player, arrival: DeliveryArrival) {
        if (arrival.stored <= 0) {
            return
        }
        val cycle = currentCycle?.invoke() ?: return
        if (player.motherlodeLastSackNotify == cycle + 1) {
            return
        }
        player.motherlodeLastSackNotify = cycle + 1
        val capacity =
            if (player.hasBiggerSackUnlocked) {
                MotherlodeConstants.SACK_CAPACITY_BIG
            } else {
                MotherlodeConstants.SACK_CAPACITY_NORMAL
            }
        val message =
            if (arrival.storedTotal >= capacity) {
                MotherlodeConstants.SACK_READY_FULL_MESSAGE
            } else {
                MotherlodeConstants.SACK_READY_MESSAGE
            }
        player.mes(message)
    }
}
