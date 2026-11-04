package org.rsmod.api.player.events

import dev.openrune.types.ItemServerType
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.random.GameRandom
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
    internal val specialRequested: Boolean,
) {
    public var cycles: Int = 0
    public var special: Boolean = false

    public var accuracyMultiplier: Double = 1.0
        private set

    public var accuracyRolls: Int = 1
        private set

    public var neverMiss: Boolean = false
        private set

    public var prayerPenetration: Int = 0
        private set

    private var saveChance = 0
    private var specialSaveChance = 0
    private var cyclesChange = 0
    private var prepared = false

    public fun adjustedCycles(baseCycles: Int): Int =
        if (cyclesChange == 0) baseCycles else (baseCycles + cyclesChange).coerceAtLeast(1)

    public fun rollSave(random: GameRandom): Boolean =
        saveChance > 0 && random.of(maxExclusive = PERCENT) < saveChance

    public fun rollSpecialSave(random: GameRandom): Boolean =
        specialSaveChance > 0 && random.of(maxExclusive = PERCENT) < specialSaveChance

    internal val rolls = ArrayList<AttackRoll>(1)
    internal val resources = ArrayList<ResourceSpend>(1)
    internal val hits = ArrayList<AttackHit>(1)
    internal var projectile: AttackProjectile? = null
        private set

    internal val isEmpty: Boolean
        get() = !special && rolls.isEmpty() && resources.isEmpty() && hits.isEmpty()

    /** Some weapons set the delay a second time, so this only publishes the first time. */
    internal fun prepare(player: Player, events: EventBus, baseCycles: Int) {
        if (prepared) {
            return
        }
        prepared = true
        if (target == null || events.unbound[PlayerAttackEvents.AttackPrepare::class.java] == null) {
            return
        }
        val event =
            PlayerAttackEvents.AttackPrepare(
                player = player,
                target = target,
                kind = kind,
                weapon = weapon,
                spell = spell,
                special = specialRequested,
                cycles = baseCycles,
            )
        events.publish(event)
        cyclesChange = event.cycles - baseCycles
        accuracyMultiplier = event.accuracyMultiplier
        accuracyRolls = event.accuracyRolls.coerceAtLeast(1)
        neverMiss = event.neverMiss
        saveChance = event.saveChance
        specialSaveChance = event.specialSaveChance
        prayerPenetration = event.prayerPenetration
    }

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

    public fun addMissDamage(target: PathingEntity, damage: Int) {
        val miss = rolls.indexOfLast { it.target === target && !it.success }
        if (damage > 0 && miss != -1) {
            rolls[miss] = rolls[miss].copy(damage = damage)
        }
    }

    public fun addResource(kind: ResourceKind, item: ItemServerType?, count: Int, saved: Boolean) {
        resources += ResourceSpend(kind, item, count, saved)
    }

    public fun addHit(target: PathingEntity, hit: Hit, delay: Int) {
        hits += AttackHit(target, hit, delay)
    }

    public fun addProjectile(spotanim: Int, projanim: String) {
        if (projectile == null) {
            projectile = AttackProjectile(spotanim, projanim)
        }
    }

    private companion object {
        const val PERCENT = 100
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
    specialRequested: Boolean = false,
) {
    val observed =
        unbound[PlayerAttackEvents.AttackResult::class.java] != null ||
            unbound[PlayerAttackEvents.AttackPrepare::class.java] != null
    if (observed) {
        player.attr[ATTACK_RECORD] =
            PlayerAttackRecord(target, kind, weapon, spell, specialRequested)
    }
}

/** @return the delay to use, which is [baseCycles] unless an attack is being recorded. */
public fun EventBus.prepareAttackRecord(player: Player, baseCycles: Int): Int {
    val record = player.attackRecord ?: return baseCycles
    record.prepare(player, this, baseCycles)
    val cycles = record.adjustedCycles(baseCycles)
    record.cycles = cycles
    return cycles
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
            projectile = record.projectile,
        )
    publish(event)
}

public fun Player.discardAttackRecord() {
    attr.remove(ATTACK_RECORD)
}
