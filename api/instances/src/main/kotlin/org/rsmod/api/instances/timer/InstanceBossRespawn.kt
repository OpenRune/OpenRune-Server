package org.rsmod.api.instances.timer

import org.rsmod.annotations.InternalApi
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.SessionState
import org.rsmod.game.entity.Npc

internal data class InstanceBossRespawn(val bossName: String, val deadlineTick: Int) {
    companion object {
        @OptIn(InternalApi::class)
        fun next(
            session: InstanceSession,
            npcs: List<Npc>,
            currentTick: Int,
        ): InstanceBossRespawn? {
            if (session.arenaExpired || session.state != SessionState.Active) return null
            val bossTypes = session.spec.bossNpcs?.takeIf { it.isNotEmpty() } ?: return null
            val next = npcs.asSequence()
                .filter { npc ->
                    npc.isSlotAssigned && npc.respawns && npc.hidden &&
                        npc.lifecycleRespawnCycle > currentTick &&
                        bossTypes.any { boss ->
                            npc.type == boss || npc.visType == boss ||
                                npc.isType(boss.internalName) || npc.isVisType(boss.internalName)
                        }
                }
                .minWithOrNull(compareBy<Npc> { it.lifecycleRespawnCycle }.thenBy { it.slotId })
                ?: return null
            return InstanceBossRespawn(next.visType.name, next.lifecycleRespawnCycle)
        }
    }
}
