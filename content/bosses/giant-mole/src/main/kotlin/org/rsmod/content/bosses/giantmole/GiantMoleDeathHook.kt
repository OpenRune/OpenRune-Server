package org.rsmod.content.bosses.giantmole

import dev.openrune.rscm.RSCM
import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook

internal class GiantMoleDeathHook @Inject constructor() : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val npc = context.npc
        if (npc.id != RSCM.getRSCM("npc.mole_giant")) {
            return
        }
        npc.spawnCoords = GiantMoleRules.SPAWN
    }
}
