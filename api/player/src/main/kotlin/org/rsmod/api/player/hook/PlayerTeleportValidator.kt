package org.rsmod.api.player.hook

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.game.entity.Player
import org.rsmod.game.queue.WorldQueueList

public class PlayerTeleportValidator
@Inject
constructor(
    private val hooks: Set<@JvmSuppressWildcards PlayerTeleportValidateHook>,
    private val delays: Set<@JvmSuppressWildcards PlayerTeleportDelayHook> = emptySet(),
    private val queues: WorldQueueList? = null,
) {
    private class Pending(val player: Player, val uid: Int, val origin: org.rsmod.map.CoordGrid,
        val type: TeleportType, val areas: AreaChecker, val action: () -> Unit, val expires: Int)
    private val pending = java.util.WeakHashMap<Player, Pending>()
    public fun cancel(player: Player) { pending.remove(player); delays.forEach { it.clear(player) } }
    public fun begin(player: Player, type: TeleportType, areas: AreaChecker) {
        cancel(player)
        if (type != TeleportType.Exempt) delays.forEach { it.begin(player, type, areas) }
    }

    /** Returns true when the native output has been scheduled for later activation. */
    public fun defer(player: Player, type: TeleportType, areas: AreaChecker, action: () -> Unit): Boolean {
        pending.remove(player)
        val ticks = if (type == TeleportType.Exempt) 0 else remaining(player, type, areas)
        if (ticks <= 0 || queues == null) { delays.forEach { it.clear(player) }; return false }
        val task = Pending(player, player.uid.packed, player.coords, type, areas, action, player.currentMapClock + 100)
        pending[player] = task
        schedule(task, ticks)
        return true
    }
    private fun remaining(player: Player, type: TeleportType, areas: AreaChecker): Int =
        delays.maxOfOrNull { it.remaining(player, type, areas) }?.coerceIn(0, 100) ?: 0
    private fun schedule(task: Pending, ticks: Int) {
        task.player.actionDelay = maxOf(task.player.actionDelay, task.player.currentMapClock + ticks)
        checkNotNull(queues).add(ticks) {
            val player = task.player
            if (pending[player] !== task) return@add
            if (player.pendingLogout || player.uid.packed != task.uid || player.hitpoints == 0 || player.coords != task.origin || player.currentMapClock > task.expires) { cancel(player); return@add }
            val denial = validate(player, task.type, task.areas)
            if (denial != null) { cancel(player); player.mes(denial); return@add }
            val remaining = remaining(player, task.type, task.areas)
            if (remaining > 0) schedule(task, remaining)
            else { cancel(player); task.action() }
        }
    }

    public fun validate(
        player: Player,
        type: TeleportType,
        areaChecker: AreaChecker,
    ): String? {
        if (type == TeleportType.Exempt) {
            return null
        }
        for (hook in hooks) {
            val denial = hook.validate(player, type, areaChecker)
            if (denial != null) {
                return denial
            }
        }
        return null
    }
}
