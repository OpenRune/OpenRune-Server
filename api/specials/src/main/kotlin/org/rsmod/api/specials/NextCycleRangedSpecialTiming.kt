package org.rsmod.api.specials

import java.util.Collections
import java.util.WeakHashMap
import org.rsmod.game.entity.Player

/**
 * Coordinates ranged specials that are armed now but must execute on the following game cycle, even
 * if the player is otherwise attack-delayed.
 *
 * A pending entry is consumed only on its exact due cycle. Cancelling the special, switching the
 * weapon, or reaching the combat handler too late leaves normal attack-delay handling intact.
 */
public object NextCycleRangedSpecialTiming {
    private val pending: MutableMap<Player, PendingSpecial> =
        Collections.synchronizedMap(WeakHashMap())

    /** Arms [weaponId]'s next-cycle timing from the player's current map clock. */
    public fun schedule(player: Player, weaponId: Int) {
        pending[player] = PendingSpecial(weaponId = weaponId, dueCycle = player.currentMapClock + 1)
    }

    /** Clears any armed next-cycle special timing for [player]. */
    public fun cancel(player: Player) {
        pending.remove(player)
    }

    /**
     * Resolves the timing state for the current ranged combat pass.
     *
     * [Resolution.Wait] defers a normal-ready attack until the following cycle. On that cycle,
     * [Resolution.BypassAttackDelay] permits this one special attack through the normal guard.
     */
    public fun resolve(player: Player, weaponId: Int, specialSelected: Boolean): Resolution =
        synchronized(pending) {
            val scheduled = pending[player] ?: return@synchronized Resolution.None
            if (
                !specialSelected ||
                    scheduled.weaponId != weaponId ||
                    player.currentMapClock > scheduled.dueCycle
            ) {
                pending.remove(player)
                return@synchronized Resolution.None
            }

            if (player.currentMapClock < scheduled.dueCycle) {
                return@synchronized Resolution.Wait
            }

            pending.remove(player)
            Resolution.BypassAttackDelay
        }

    public enum class Resolution {
        None,
        Wait,
        BypassAttackDelay,
    }

    private data class PendingSpecial(val weaponId: Int, val dueCycle: Int)
}
