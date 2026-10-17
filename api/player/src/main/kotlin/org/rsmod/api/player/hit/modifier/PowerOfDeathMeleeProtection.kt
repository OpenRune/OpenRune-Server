package org.rsmod.api.player.hit.modifier

import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.righthand
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isAnyType

/**
 * Power of Death (Staff of the Dead family): halves incoming melee damage for one minute after
 * activating, but only while a Staff of the Dead family weapon remains equipped - it is lost
 * immediately (not just on a timer) if the staff is swapped out, matching the wiki: "the effect
 * lasts for one minute... but will instantly end if the player takes damage while the staff is
 * not equipped."
 */
public object PowerOfDeathMeleeProtection {
    // 1 minute = 60s / 0.6s per cycle = 100 cycles.
    private const val DURATION_CYCLES: Int = 100

    private val activeUntil = AttributeKey<Int>(resetOnDeath = true, temp = true)

    public fun activate(player: Player) {
        player.attr[activeUntil] = player.currentMapClock + DURATION_CYCLES
    }

    public fun isActive(player: Player): Boolean {
        val until = player.attr[activeUntil] ?: return false
        if (player.currentMapClock >= until) {
            return false
        }
        return player.isWearingStaffOfTheDeadFamily()
    }

    private fun Player.isWearingStaffOfTheDeadFamily(): Boolean =
        righthand?.isAnyType(
            "obj.sotd",
            "obj.br_sotd",
            "obj.staff_of_light",
            "obj.staff_of_balance",
            "obj.toxic_sotd",
            "obj.toxic_sotd_charged",
            "obj.toxic_sotd_deadman",
            "obj.toxic_sotd_charged_deadman",
        ) == true
}
