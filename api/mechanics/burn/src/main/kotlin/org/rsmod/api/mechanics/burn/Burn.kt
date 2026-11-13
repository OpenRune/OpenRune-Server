package org.rsmod.api.mechanics.burn

import jakarta.inject.Inject
import org.rsmod.api.config.refs.done.hitmark_groups
import org.rsmod.api.config.refs.params
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType

/**
 * Up to [MAX_STACKS] independent burns, each lasting [STACK_TICKS] ticks and dealing one damage
 * every [TICK_INTERVAL] ticks on its own schedule. The stacks' remaining ticks are packed into one
 * var, [BITS_PER_STACK] bits each.
 */
public class Burn @Inject constructor(private val npcModifier: NpcHitModifier) {
    /** `false` when the target is immune or already carries [MAX_STACKS] burns. */
    public fun inflict(target: PathingEntity): Boolean {
        if (target is Npc && (target.visType.paramOrNull(params.burn_immunity) ?: 0) > 0) {
            return false
        }
        val stacks = read(target)
        val slot = (0 until MAX_STACKS).firstOrNull { remainingTicks(stacks, it) == 0 } ?: return false
        write(target, stacks or (STACK_TICKS shl (slot * BITS_PER_STACK)))
        if (stacks == 0) {
            startTimer(target)
        }
        return true
    }

    /** Damage the burns on [target] have yet to deal. */
    public fun remainingDamage(target: PathingEntity): Int {
        val stacks = read(target)
        return (0 until MAX_STACKS).sumOf { (remainingTicks(stacks, it) + TICK_INTERVAL - 1) / TICK_INTERVAL }
    }

    /** Clears every burn on [target] and returns the damage they had yet to deal. */
    public fun consume(target: PathingEntity): Int {
        val remaining = remainingDamage(target)
        clear(target)
        return remaining
    }

    public fun clear(target: PathingEntity) {
        write(target, 0)
        when (target) {
            is Npc -> target.clearTimer(NPC_TIMER)
            is Player -> target.clearTimer(PLAYER_TIMER)
        }
    }

    public fun tick(target: PathingEntity) {
        val stacks = read(target)
        if (stacks == 0 || target.isDead()) {
            clear(target)
            return
        }

        var damage = 0
        var next = 0
        for (slot in 0 until MAX_STACKS) {
            val before = remainingTicks(stacks, slot)
            if (before == 0) {
                continue
            }
            val remaining = before - 1
            if (remaining % TICK_INTERVAL == 0) {
                damage++
            }
            next = next or (remaining shl (slot * BITS_PER_STACK))
        }

        if (damage > 0) {
            queueDamage(target, damage)
        }
        write(target, next)
        if (next != 0) {
            startTimer(target)
        }
    }

    private fun queueDamage(target: PathingEntity, damage: Int) {
        when (target) {
            is Npc ->
                target.queueHit(
                    delay = 1,
                    type = HitType.Typeless,
                    damage = damage,
                    modifier = npcModifier,
                    hitmark = hitmark_groups.burn,
                )
            is Player ->
                target.queueHit(
                    delay = 1,
                    type = HitType.Typeless,
                    damage = damage,
                    modifier = NoopPlayerHitModifier,
                    hitmark = hitmark_groups.burn,
                )
        }
    }

    private fun PathingEntity.isDead(): Boolean =
        when (this) {
            is Npc -> hitpoints <= 0
            is Player -> hitpoints <= 0
        }

    private fun startTimer(target: PathingEntity) {
        when (target) {
            is Npc -> target.timer(NPC_TIMER, 1)
            is Player -> target.timer(PLAYER_TIMER, 1)
        }
    }

    private fun read(target: PathingEntity): Int =
        when (target) {
            is Npc -> target.vars[NPC_VAR]
            is Player -> target.vars[PLAYER_VAR]
        }

    private fun write(target: PathingEntity, value: Int) {
        when (target) {
            is Npc -> target.vars[NPC_VAR] = value
            is Player -> VarPlayerIntMapSetter.set(target, PLAYER_VAR, value)
        }
    }

    private fun remainingTicks(stacks: Int, slot: Int): Int =
        (stacks shr (slot * BITS_PER_STACK)) and STACK_MASK

    public companion object {
        public const val NPC_TIMER: String = "timer.burn_npc"
        public const val PLAYER_TIMER: String = "timer.burn_player"
        private const val NPC_VAR = "varn.burn_stacks"
        private const val PLAYER_VAR = "varp.burn_stacks"
        private const val MAX_STACKS = 5
        private const val STACK_TICKS = 40
        private const val TICK_INTERVAL = 4
        private const val BITS_PER_STACK = 6
        private const val STACK_MASK = (1 shl BITS_PER_STACK) - 1
    }
}
