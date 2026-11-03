package org.rsmod.api.player.events

import dev.openrune.types.ItemServerType
import org.rsmod.events.EventBus
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.player.PlayerEvent
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType

/** Events about a player's outgoing attacks. [PlayerEvent.player] is always the attacker. */
public class PlayerAttackEvents {
    /**
     * One attack finished, after its handler returned. Hits have been queued but not landed, so
     * [hits] may still be capped by the target's hitpoints: use [HitImpact] for what was dealt.
     *
     * [target] is null for an instant special. [rolls] has one entry per accuracy or damage roll, so
     * a Scythe or a Barrage has several. [cycles] is the final delay until the next attack.
     * Publishes nothing if the attack did nothing, such as while waiting on the attack delay.
     */
    public class AttackResult(
        override val player: Player,
        public val target: PathingEntity?,
        public val kind: AttackKind,
        public val weapon: ItemServerType?,
        public val spell: ItemServerType?,
        public val special: Boolean,
        public val cycles: Int,
        public val rolls: List<AttackRoll>,
        public val resources: List<ResourceSpend>,
        public val hits: List<Hit>,
    ) : UnboundEvent, PlayerEvent

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

public enum class AttackKind {
    Melee,
    Ranged,
    Spell,
    Staff,
    Instant,
}

public enum class ResourceKind {
    Ammo,
    Rune,
    Charge,
    SpecialEnergy,
}

/** [damage] is 0 when [success] is false. [maxHit] is 0 if accuracy passed but nothing rolled. */
public data class AttackRoll(
    public val target: PathingEntity,
    public val type: HitType,
    public val success: Boolean,
    public val maxHit: Int,
    public val damage: Int,
)

/** [item] is the ammo, rune or charged weapon, and null for special energy. */
public data class ResourceSpend(
    public val kind: ResourceKind,
    public val item: ItemServerType?,
    public val count: Int,
    public val saved: Boolean,
)

public fun EventBus.publishAttackImpact(target: PathingEntity, hit: Hit, players: PlayerList) {
    if (!hit.isFromPlayer || unbound[PlayerAttackEvents.HitImpact::class.java] == null) {
        return
    }
    val source = hit.resolvePlayerSource(players) ?: return
    publish(PlayerAttackEvents.HitImpact(source, target, hit))
}
