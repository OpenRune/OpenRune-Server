package org.rsmod.api.combat.commons

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.queue.WorldQueueList

/**
 * A shared "bind" duration tracker for melee weapons whose special attacks immobilize a target
 * and/or read how long a target is currently bound for (Zamorak godsword's Ice Cleave, Blue Moon
 * spear's Break Shackles).
 *
 * This is deliberately separate from the existing magic freeze-spell mechanic (Snare/Entangle/
 * Bind/Ice Barrage, [CombatEffects.freeze] / `Player.frozen`) - a target frozen by a spell won't
 * show a remaining bind duration here, and vice versa. Documented scope limit, not an oversight:
 * unifying the two would need deeper access into the existing player timer internals than these
 * two weapons' conversions needed. `Player.frozen` targets are still fully handled by the existing
 * mechanic for movement-blocking purposes; this service only tracks its own duration count and,
 * for NPCs (which have no equivalent flag), also drives [Npc.movementLocked].
 */
@Singleton
public class BindEffectService @Inject constructor(private val worldQueues: WorldQueueList) {
    private val boundUntil = WeakHashMap<PathingEntity, Int>()

    public fun bind(target: PathingEntity, ticks: Int) {
        if (ticks <= 0) {
            return
        }
        val until = ExpiringDurationMath.untilClock(target.currentMapClock, ticks)
        val current = boundUntil[target] ?: 0
        if (!ExpiringDurationMath.shouldExtend(until, current)) {
            return
        }
        boundUntil[target] = until
        if (target is Npc) {
            target.movementLocked = true
        }
        worldQueues.add(ticks) { clearIfStillExpired(target, until) }
    }

    public fun cyclesRemaining(target: PathingEntity): Int {
        val until = boundUntil[target] ?: return 0
        return ExpiringDurationMath.remaining(until, target.currentMapClock)
    }

    public fun breakBind(target: PathingEntity) {
        boundUntil.remove(target)
        if (target is Npc) {
            target.movementLocked = false
        }
    }

    private fun clearIfStillExpired(target: PathingEntity, expectedUntil: Int) {
        // A newer bind may have been applied (and its own callback already scheduled) after this
        // one was queued - only clear if nothing has extended the duration since.
        if (!ExpiringDurationMath.isStillCurrent(boundUntil[target], expectedUntil)) {
            return
        }
        boundUntil.remove(target)
        if (target is Npc) {
            target.movementLocked = false
        }
    }
}
