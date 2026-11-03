package org.rsmod.api.player.events

import org.rsmod.events.EventBus
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.player.PlayerEvent
import org.rsmod.game.hit.Hit

/** Events about a player's outgoing attacks. [PlayerEvent.player] is always the attacker. */
public class PlayerAttackEvents {
    /**
     * A hit from [player] landed on [target], after the target's own hit events. It is published for
     * every player-sourced hit, not only those from an attack, and not if the player has logged out.
     *
     * On an npc, `hit.damage` is what was dealt: capped by its remaining hitpoints. On a player it is
     * the damage before that cap.
     */
    public data class HitImpact(
        override val player: Player,
        public val target: PathingEntity,
        public val hit: Hit,
    ) : UnboundEvent, PlayerEvent
}

public fun EventBus.publishAttackImpact(target: PathingEntity, hit: Hit, players: PlayerList) {
    if (!hit.isFromPlayer || unbound[PlayerAttackEvents.HitImpact::class.java] == null) {
        return
    }
    val source = hit.resolvePlayerSource(players) ?: return
    publish(PlayerAttackEvents.HitImpact(source, target, hit))
}
