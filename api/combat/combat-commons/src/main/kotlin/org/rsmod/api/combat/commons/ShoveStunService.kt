package org.rsmod.api.combat.commons

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.queue.WorldQueueList

/**
 * The Shove special attack (Dragon spear, Zamorakian spear, Zamorakian hasta) stun. Per the wiki's
 * own "Stun (status)" page: "Targets affected by Shove will be fully stunned... There is no
 * immunity and thus the stuns from Shove can be chained" - deliberately no post-stun immunity
 * window here, matching that (torka's original file had an invented one-cycle player immunity that
 * doesn't exist in the real game).
 *
 * Scope limit, documented rather than silently under-delivered: a full stun blocks movement,
 * attacking, eating, equipment changes, and spellcasting. This service only drives
 * [Npc.movementLocked] for NPC targets (an existing, already-integrated movement-block flag) and
 * exposes [isStunned] for other code to check; nothing currently enforces the equivalent for
 * player targets (no existing generic "block all actions" flag to hook into), so a stunned player
 * can still act - only the knockback and the `isStunned` bookkeeping are guaranteed correct.
 */
@Singleton
public class ShoveStunService @Inject constructor(private val worldQueues: WorldQueueList) {
    private val stunnedUntil = WeakHashMap<PathingEntity, Int>()

    public fun isStunned(target: PathingEntity): Boolean {
        val until = stunnedUntil[target] ?: return false
        return ExpiringDurationMath.remaining(until, target.currentMapClock) > 0
    }

    public fun applyStun(target: PathingEntity, ticks: Int) {
        if (ticks <= 0) {
            return
        }
        val until = ExpiringDurationMath.untilClock(target.currentMapClock, ticks)
        stunnedUntil[target] = until
        if (target is Npc) {
            target.movementLocked = true
        }
        worldQueues.add(ticks) { clearIfStillExpired(target, until) }
    }

    private fun clearIfStillExpired(target: PathingEntity, expectedUntil: Int) {
        if (!ExpiringDurationMath.isStillCurrent(stunnedUntil[target], expectedUntil)) {
            return
        }
        stunnedUntil.remove(target)
        if (target is Npc) {
            target.movementLocked = false
        }
    }
}
