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
     * An attack passed its checks and is about to happen. Published once per attack, not while the
     * player is waiting on the attack delay. The settings apply to every hit and victim of the
     * attack, so a Barrage uses them for all its targets: put per-hit effects on [DamageRoll].
     *
     * [special] means the player asked for a special, which can still fail its energy check.
     * [AttackResult.special] is the confirmed value. [accuracyMultiplier] applies on top of any the
     * attack has, [accuracyRolls] succeeds if any roll does, and [neverMiss] skips the roll. Extra
     * attacks a handler makes later are not part of this attack and do not get these settings.
     *
     * [saveChance] is a percentage rolled separately for each ammo use, each charge use and each
     * rune type in a spell, and [specialSaveChance] for special energy. A saved resource is not
     * spent, and [AttackResult.resources] reports it, which is where follow-up effects belong.
     * Instant specials do not publish this, so they never save energy.
     *
     * [cycles] starts as the attack's own delay. The change a handler makes also applies if the
     * weapon sets its own delay later, as Tumeken's shadow does, and the result is never below 1.
     */
    public class AttackPrepare(
        override val player: Player,
        public val target: PathingEntity,
        public val kind: AttackKind,
        public val weapon: ItemServerType?,
        public val spell: ItemServerType?,
        public val special: Boolean,
        public var cycles: Int,
        public var accuracyMultiplier: Double = 1.0,
        public var accuracyRolls: Int = 1,
        public var neverMiss: Boolean = false,
        public var saveChance: Int = 0,
        public var specialSaveChance: Int = 0,
    ) : UnboundEvent, PlayerEvent

    /**
     * Published before each damage roll, and for each miss with [missed] true. The engine rolls
     * [minHit]..[maxHit] unless [damage] is set, so a handler that needs the outcome can roll for
     * itself. A miss stays 0 unless [damage] is set.
     *
     * This fires for any roll through `PlayerAttackManager`, including extra attacks a handler makes,
     * so a handler that attacks from here must guard against triggering itself. A magic miss still
     * plays splash visuals and grants no xp, whatever [damage] is set to. The dark bow special rolls
     * for itself and does not publish this.
     */
    public class DamageRoll(
        override val player: Player,
        public val target: PathingEntity,
        public val type: HitType,
        public var minHit: Int,
        public var maxHit: Int,
        public val missed: Boolean,
        public var damage: Int? = null,
    ) : UnboundEvent, PlayerEvent

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

/**
 * [damage] on a failed roll is 0 unless a [PlayerAttackEvents.DamageRoll] handler set it. [maxHit] is
 * 0 if accuracy passed but nothing rolled.
 */
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
