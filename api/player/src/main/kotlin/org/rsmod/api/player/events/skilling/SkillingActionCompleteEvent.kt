package org.rsmod.api.player.events.skilling

import dev.openrune.types.ItemServerType
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

    /** Published once the spell's runes are paid for, before its effect happens. */
    public data class SpellCast(public val spell: ItemServerType) : SkillingActionContext()

    public data class Alchemy(
        public val item: ItemServerType,
        public val high: Boolean,
        public val coins: Int,
    ) : SkillingActionContext()

    public data class Firemaking(
        public val log: String,
        public val experienceGranted: Double,
        public val usedBow: Boolean = false,
        public val campfire: Boolean = false,
    ) : SkillingActionContext()

    public data class Consumed(
        public val item: ItemServerType,
        public val potion: Boolean,
    ) : SkillingActionContext()
}
