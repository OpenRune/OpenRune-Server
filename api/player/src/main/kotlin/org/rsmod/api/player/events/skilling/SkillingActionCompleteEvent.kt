package org.rsmod.api.player.events.skilling

import org.rsmod.api.player.events.prayer.PrayerSkillAction
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent

public data class SkillingActionCompleteEvent(
    override val player: Player,
    public val context: SkillingActionContext,
) : UnboundEvent, PlayerEvent

public sealed class SkillingActionContext {
    public data class Prayer(public val action: PrayerSkillAction) : SkillingActionContext()

    public data class Product(
        public val skill: String,
        public val item: String,
        public val count: Int,
        public val experienceGranted: Double,
        public val source: SkillingProductSource,
        public val isBonus: Boolean = false,
    ) : SkillingActionContext()
}
