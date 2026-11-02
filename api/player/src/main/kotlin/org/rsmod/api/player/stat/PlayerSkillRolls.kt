package org.rsmod.api.player.stat

import org.rsmod.api.player.events.PlayerSkillRollEvents
import org.rsmod.game.entity.Player

/** Passes a roll through the roll events and returns the outcome to use. */
public fun Player.resolveSkillRoll(
    stat: String,
    chance: Double,
    rolled: Boolean,
    subject: String? = null,
): Boolean {
    val bus = PlayerSkillXP.eventBus ?: return rolled
    var success = rolled
    if (bus.unbound[PlayerSkillRollEvents.Modify::class.java] != null) {
        val modify = PlayerSkillRollEvents.Modify(this, stat, subject, chance, rolled)
        bus.publish(modify)
        success = modify.success
    }
    if (bus.unbound[PlayerSkillRollEvents.Result::class.java] != null) {
        bus.publish(PlayerSkillRollEvents.Result(this, stat, subject, chance, success))
    }
    return success
}
