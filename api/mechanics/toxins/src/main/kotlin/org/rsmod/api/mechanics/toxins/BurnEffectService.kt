package org.rsmod.api.mechanics.toxins

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.config.refs.done.hitmark_groups
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.queue.WorldQueueList

/**
 * Burn: a stacking damage-over-time status effect. Wiki: entities take 1 damage every 4 ticks; a
 * normal burn instance lasts 40 ticks (10 total damage). Up to 5 independent burn instances can be
 * active on the same target at once - the tick frequency never changes, but each tick deals damage
 * equal to the number of currently active stacks, and each stack still expires on its own
 * schedule. A 6th application while already at 5 stacks is discarded (the oldest is not replaced).
 *
 * Coded as typeless damage here (not ranged, despite the wiki calling it "coded as ranged damage"
 * for NPC damage-style-reduction purposes) specifically so Protect from Missiles doesn't reduce
 * it, matching the wiki's explicit carve-out ("The Protect from Missiles prayer will not reduce
 * burn damage"). NPC ranged-immunity interactions and burn severity tiers (Incendiary/Strong/
 * Normal/Weak) are not modeled - a lightweight, in-memory implementation with no buff-bar icon or
 * persistence across logout, matching the scope of this session's other timed-effect additions.
 */
@Singleton
public class BurnEffectService
@Inject
constructor(
    private val worldQueues: WorldQueueList,
    private val npcHitModifier: NpcHitModifier,
) {
    private val activeStacks = WeakHashMap<PathingEntity, MutableList<Int>>()

    /**
     * Consumes every active burn stack on [target] and returns the total damage they would have
     * dealt over their remaining lifetime (each stack "owes" its remaining ticks / [TICK_INTERVAL]
     * worth of 1-damage pulses). Ends the burn entirely - used by effects that convert unspent burn
     * damage into a one-off bonus (Eclipse atlatl's own special attack).
     */
    public fun consumeRemainingDamage(target: PathingEntity): Int {
        val stacks = activeStacks.remove(target) ?: return 0
        return BurnStacks.remainingDamage(stacks)
    }

    public fun apply(source: Player, target: PathingEntity) {
        val stacks = activeStacks.getOrPut(target) { mutableListOf() }
        if (!BurnStacks.canApply(stacks.size)) {
            return
        }
        val alreadyTicking = stacks.isNotEmpty()
        stacks += BURN_DURATION_TICKS
        if (!alreadyTicking) {
            scheduleTick(target)
        }
    }

    private fun scheduleTick(target: PathingEntity) {
        worldQueues.add(TICK_INTERVAL) {
            val stacks = activeStacks[target]
            if (stacks.isNullOrEmpty() || !target.isBurnable()) {
                activeStacks.remove(target)
                return@add
            }

            dealBurnDamage(target, damage = BurnStacks.damageForStackCount(stacks.size))

            val remaining = BurnStacks.tick(stacks)
            stacks.clear()
            stacks += remaining

            if (stacks.isEmpty()) {
                activeStacks.remove(target)
            } else {
                scheduleTick(target)
            }
        }
    }

    private fun dealBurnDamage(target: PathingEntity, damage: Int) {
        when (target) {
            is Player ->
                target.queueHit(
                    delay = 1,
                    type = HitType.Typeless,
                    damage = damage,
                    hitmark = hitmark_groups.burn,
                    modifier = NoopPlayerHitModifier,
                )
            is Npc ->
                target.queueHit(
                    delay = 1,
                    type = HitType.Typeless,
                    damage = damage,
                    modifier = npcHitModifier,
                    hitmark = hitmark_groups.burn,
                )
        }
    }

    private fun PathingEntity.isBurnable(): Boolean =
        when (this) {
            is Player -> isValidTarget()
            is Npc -> isValidTarget()
        }

    private companion object {
        const val TICK_INTERVAL: Int = BurnStacks.TICK_INTERVAL
        const val BURN_DURATION_TICKS: Int = BurnStacks.DURATION_TICKS
        const val MAX_STACKS: Int = BurnStacks.MAX_STACKS
    }
}

/** Pure burn-stack state transitions, kept separate from [PathingEntity]/[WorldQueueList] state. */
internal object BurnStacks {
    const val TICK_INTERVAL: Int = 4
    const val DURATION_TICKS: Int = 40
    const val MAX_STACKS: Int = 5

    /** A 6th application while already at [MAX_STACKS] is discarded, not replacing the oldest. */
    fun canApply(currentStackCount: Int): Boolean = currentStackCount < MAX_STACKS

    /** Damage dealt on a pulse is the number of currently active stacks, capped at [MAX_STACKS]. */
    fun damageForStackCount(stackCount: Int): Int = stackCount.coerceAtMost(MAX_STACKS)

    /** Decrements every stack by one pulse and drops any that have fully expired. */
    fun tick(stacks: List<Int>): List<Int> = stacks.map { it - TICK_INTERVAL }.filter { it > 0 }

    /** Each stack "owes" its remaining ticks worth of future 1-damage pulses. */
    fun remainingDamage(stacks: List<Int>): Int = stacks.sumOf { it / TICK_INTERVAL }
}
