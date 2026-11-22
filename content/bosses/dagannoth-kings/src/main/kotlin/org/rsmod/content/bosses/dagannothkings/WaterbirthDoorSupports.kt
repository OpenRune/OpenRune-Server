package org.rsmod.content.bosses.dagannothkings

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.repo.loc.LocRepository

internal class WaterbirthDoorSupports @Inject constructor(private val locs: LocRepository) :
    NpcDeathKillHook {

    override fun onKill(context: NpcDeathKillContext) {
        val npc = context.npc
        if (npc.type.id !in WEAK_DOOR_IDS) return
        val door = locs.findAll(npc.coords).firstOrNull { it.id == DOOR_LOC } ?: return
        val duration = npc.type.respawnRate.takeIf { it > 0 } ?: DOOR_RESPAWN_TICKS
        locs.del(door, duration)
    }

    private companion object {
        const val DOOR_LOC = 8967
        const val DOOR_RESPAWN_TICKS = 50

        val WEAK_DOOR_IDS =
            listOf(
                    "npc.dagannoth_weak_door_north",
                    "npc.dagannoth_weak_door_south",
                    "npc.dagannoth_weak_door_west",
                    "npc.dagannoth_weak_door_falling_north",
                    "npc.dagannoth_weak_door_falling_south",
                    "npc.dagannoth_weak_door_falling_west",
                    "npc.dagannoth_weak_door_fallen_north",
                    "npc.dagannoth_weak_door_fallen_south",
                    "npc.dagannoth_weak_door_fallen_west",
                )
                .map { it.asRSCM(RSCMType.NPC) }
                .toSet()
    }
}
