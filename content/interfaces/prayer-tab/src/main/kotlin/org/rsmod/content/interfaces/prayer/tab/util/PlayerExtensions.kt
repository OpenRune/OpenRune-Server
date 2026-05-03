package org.rsmod.content.interfaces.prayer.tab.util

import dev.openrune.types.varp.isType
import org.rsmod.api.config.constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.content.interfaces.prayer.tab.Prayer
import org.rsmod.game.entity.Player

internal var Player.drainCounter by intVarBit("varbit.prayer_drain_counter")

internal fun ProtectedAccess.enablePrayerStatRegen(prayer: Prayer) {
    when {
        prayer.enabled == "varbit.prayer_rapidrestore" -> {
            softTimer("timer.rapidrestore_regen", 100)
        }

        prayer.enabled == "varbit.prayer_rapidheal" -> {
            softTimer("timer.health_regen", constants.health_regen_interval / 2)
        }

        prayer.enabled == "varbit.prayer_preserve" -> {
            clearQueue("queue.preserve_activation")
            longQueueDiscard("queue.preserve_activation", 25)
        }
    }
}

internal fun ProtectedAccess.disablePrayerStatRegen(prayer: Prayer) {
    when {
        prayer.enabled == "varbit.prayer_rapidrestore" -> {
            clearSoftTimer("timer.rapidrestore_regen")
        }

        prayer.enabled == "varbit.prayer_rapidheal" -> {
            softTimer("timer.health_regen", constants.health_regen_interval)
        }

        prayer.enabled == "varbit.prayer_preserve" -> {
            clearQueue("queue.preserve_activation")
            softTimer("timer.stat_boost_restore", constants.stat_boost_restore_interval)
        }
    }
}

internal fun ProtectedAccess.enablePrayerDrain() {
    softTimer("timer.prayer_drain", 1)
}

internal fun ProtectedAccess.disablePrayerDrain() {
    player.drainCounter = 0
    clearSoftTimer("timer.prayer_drain")
}
