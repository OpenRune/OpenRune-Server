package org.rsmod.api.player.events.skilling

import org.rsmod.api.player.events.prayer.PrayerSkillAction
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player

/**
 * Published when a skilling action has finished its core effects (for example item removed and XP
 * granted). Subscribe with [org.rsmod.api.script.onEvent] to attach cross-cutting behaviour such
 * as area-based bonuses without coupling every script to every rule.
 *
 * @param context Type-specific payload; add new subclasses of [SkillingActionContext] as needed.
 */
public data class SkillingActionCompleteEvent(
    public val player: Player,
    public val context: SkillingActionContext,
) : UnboundEvent

public sealed class SkillingActionContext {
    /** Prayer skill completions; see [PrayerSkillAction] for variants. */
    public data class Prayer(public val action: PrayerSkillAction) : SkillingActionContext()
}
