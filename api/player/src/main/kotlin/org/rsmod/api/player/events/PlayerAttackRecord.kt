package org.rsmod.api.player.events

import dev.openrune.types.ItemServerType
import org.rsmod.api.attr.AttributeKey
import org.rsmod.events.EventBus
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType

/**
 * What one attack has done so far, for [PlayerAttackEvents.AttackResult]. Exists only between
 * [beginAttackRecord] and [finishAttackRecord], and only if something subscribes, so rolls and hits
 * made outside an attack (such as a league's extra hit from a handler) are never recorded.
 */
public class PlayerAttackRecord
internal constructor(
    internal val target: PathingEntity?,
    internal val kind: AttackKind,
    internal val weapon: ItemServerType?,
    internal val spell: ItemServerType?,
) {
    public var cycles: Int = 0
    public var special: Boolean = false

    internal val rolls = ArrayList<AttackRoll>(1)
    internal val resources = ArrayList<ResourceSpend>(1)
    internal val hits = ArrayList<Hit>(1)

    internal val isEmpty: Boolean
        get() = !special && rolls.isEmpty() && resources.isEmpty() && hits.isEmpty()

    public fun addAccuracy(target: PathingEntity, type: HitType, success: Boolean) {
        rolls += AttackRoll(target, type, success, maxHit = 0, damage = 0)
    }

    /** Some attacks skip accuracy or roll it several times before one damage roll. */
    public fun addDamage(target: PathingEntity, type: HitType, maxHit: Int, damage: Int) {
        val roll = AttackRoll(target, type, success = true, maxHit, damage)
        val pending = rolls.indexOfLast { it.target === target && it.success && it.maxHit == 0 }
        if (pending == -1) {
            rolls += roll
        } else {
            rolls[pending] = roll
        }
    }

    public fun addResource(kind: ResourceKind, item: ItemServerType?, count: Int, saved: Boolean) {
        resources += ResourceSpend(kind, item, count, saved)
    }

    public fun addHit(hit: Hit) {
        hits += hit
    }
}

private val ATTACK_RECORD = AttributeKey<PlayerAttackRecord>(temp = true)

public val Player.attackRecord: PlayerAttackRecord?
    get() = attr[ATTACK_RECORD]

public fun EventBus.beginAttackRecord(
    player: Player,
    target: PathingEntity?,
    kind: AttackKind,
    weapon: ItemServerType?,
    spell: ItemServerType?,
) {
    if (unbound[PlayerAttackEvents.AttackResult::class.java] != null) {
        player.attr[ATTACK_RECORD] = PlayerAttackRecord(target, kind, weapon, spell)
    }
}

/** Clears the record first, so anything a handler does is not part of this attack. */
public fun EventBus.finishAttackRecord(player: Player) {
    val record = player.attackRecord ?: return
    player.discardAttackRecord()
    if (record.isEmpty) {
        return
    }
    val event =
        PlayerAttackEvents.AttackResult(
            player = player,
            target = record.target,
            kind = record.kind,
            weapon = record.weapon,
            spell = record.spell,
            special = record.special,
            cycles = record.cycles,
            rolls = record.rolls,
            resources = record.resources,
            hits = record.hits,
        )
    publish(event)
}

public fun Player.discardAttackRecord() {
    attr.remove(ATTACK_RECORD)
}
