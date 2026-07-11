package org.rsmod.api.instances

import org.rsmod.api.instances.region.InstancePlacement
import org.rsmod.game.damage.DamageContributions

class InstanceSession(
    val id: InstanceId,
    val regionIds: MutableSet<Int>,
    val owner: Long,
    val key: String,
    val spec: InstanceSpec,
    val placement: InstancePlacement,
    var access: InstanceAccess,
    val isServerOwned: Boolean = false,
) {
    val damageContributions = DamageContributions()

    val occupants = mutableSetOf<Long>()

    val timeWarningsSent = mutableSetOf<Int>()
    val graceWarningsSent = mutableSetOf<Int>()

    var gracePending = false
        private set

    var arenaExpired = false
        private set

    var bossFightStartTick: Int? = null

    var state: SessionState = SessionState.Active
        private set

    var startedAtTick = 0
        private set

    fun markStarted(currentTick: Int) {
        startedAtTick = currentTick
        timeWarningsSent.clear()
        graceWarningsSent.clear()
        gracePending = false
    }

    fun elapsedTicks(currentTick: Int): Int =
        currentTick - startedAtTick

    fun remainingTicks(currentTick: Int): Int? =
        spec.timeLimitTicks?.minus(elapsedTicks(currentTick))

    fun graceRemainingTicks(currentTick: Int): Int? =
        (state as? SessionState.Grace)?.deadlineTick?.minus(currentTick)

    fun addOccupant(player: Long) {
        occupants += player
        if (state is SessionState.Reclaim) {
            state = SessionState.Active
        }
    }

    fun removeOccupant(player: Long, currentTick: Int) {
        occupants -= player
        if (
            !isServerOwned &&
                occupants.isEmpty() &&
                state !is SessionState.Grace &&
                !spec.destroyWhenEmpty
        ) {
            state = SessionState.Reclaim(currentTick + spec.reclaimTicks)
        }
    }

    fun enterGrace(currentTick: Int) {
        state = SessionState.Grace(currentTick + spec.graceTicks)
        graceWarningsSent.clear()
        gracePending = false
    }

    fun markGracePending(): Boolean =
        if (gracePending) {
            false
        } else {
            gracePending = true
            true
        }

    fun markArenaExpired(): Boolean =
        if (arenaExpired) {
            false
        } else {
            arenaExpired = true
            true
        }

    fun resetState() {
        state = SessionState.Active
        arenaExpired = false
        gracePending = false
        bossFightStartTick = null
        timeWarningsSent.clear()
        graceWarningsSent.clear()
        damageContributions.clear()
    }

    fun isExpired(currentTick: Int): Boolean =
        !isServerOwned &&
            state is SessionState.Reclaim &&
            occupants.isEmpty() &&
            currentTick >= (state as SessionState.Reclaim).deadlineTick

    fun isFull(): Boolean =
        occupants.size >= spec.maxPlayers

    fun registerRegion(regionId: Int) {
        regionIds += regionId
    }
}
