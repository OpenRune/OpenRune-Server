package org.rsmod.api.npc.respawn

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc

/** Game-thread only. The HUD reads snapshots of real scheduled deaths/resets, never estimates. */
@Singleton
public class BossRespawnTimers @Inject constructor(private val clock: MapClock) {
    public data class Snapshot(
        val key: String,
        val npcIndex: Int,
        val npcId: Int,
        val name: String,
        val remainingTicks: Int,
    )

    private data class Pending(
        val key: String,
        val npcIndex: Int,
        val npcId: Int,
        val name: String,
        val dueCycle: Int,
        val retainAfterDelete: Boolean,
    )

    private val pending: MutableMap<Npc, Pending> = IdentityHashMap()
    private var sequence: Long = 0

    public fun schedule(npc: Npc, ticks: Int, retainAfterDelete: Boolean = false): Int {
        require(ticks > 0)
        prune()
        val dueCycle = clock + ticks
        pending[npc] = Pending(
            "${clock.cycle}:${++sequence}", npc.slotId, npc.visType.id,
            npc.visType.name.takeIf { it.isNotBlank() } ?: npc.type.name,
            dueCycle, retainAfterDelete,
        )
        return dueCycle
    }

    public fun cancel(npc: Npc) { pending.remove(npc) }

    public fun snapshot(): List<Snapshot> {
        prune()
        return pending.values.map {
            Snapshot(it.key, it.npcIndex, it.npcId, it.name, it.dueCycle - clock.cycle)
        }
    }

    private fun prune() {
        pending.entries.removeIf { (npc, timer) ->
            clock.cycle >= timer.dueCycle || (!timer.retainAfterDelete && !npc.isSlotAssigned)
        }
    }
}
