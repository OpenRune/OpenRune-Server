package org.rsmod.api.player.stat

import org.rsmod.api.player.events.PlayerSkillAttemptEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

/** @return the ticks between attempts at a skill action, at least 1, normally [cycles]. */
public fun Player.skillCycles(stat: String, cycles: Int, subject: String? = null): Int {
    val event = publishSkillAttempt(stat, subject, cycles = cycles) ?: return cycles
    return event.cycles.coerceAtLeast(1)
}

/** @return the tool to use, normally [found], which a handler can replace or provide. */
public fun Player.skillTool(stat: String, found: InvObj?, subject: String? = null): InvObj? {
    val event = publishSkillAttempt(stat, subject, tool = found) ?: return found
    return event.tool
}

/** Only ask once the inventory is known to be full, so handlers run no more than needed. */
public fun Player.skillNeedsSpace(stat: String, subject: String? = null): Boolean =
    publishSkillAttempt(stat, subject)?.needsSpace ?: true

public fun Player.skillNeedsBait(stat: String, subject: String? = null): Boolean =
    publishSkillAttempt(stat, subject)?.needsBait ?: true

public fun Player.skillCanBeCaught(stat: String, subject: String? = null): Boolean =
    publishSkillAttempt(stat, subject)?.canBeCaught ?: true

private fun Player.publishSkillAttempt(
    stat: String,
    subject: String?,
    tool: InvObj? = null,
    cycles: Int = 0,
): PlayerSkillAttemptEvent? {
    val bus = PlayerSkillXP.eventBus ?: return null
    if (bus.unbound[PlayerSkillAttemptEvent::class.java] == null) {
        return null
    }
    val event = PlayerSkillAttemptEvent(this, stat, subject, tool, cycles)
    bus.publish(event)
    return event
}
