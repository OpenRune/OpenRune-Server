package org.rsmod.content.bosses.dagannothkings

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.hunt.HuntVis
import jakarta.inject.Inject
import org.rsmod.annotations.InternalApi
import org.rsmod.api.hunt.Hunt
import org.rsmod.api.script.onAiTimer
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class RockLobsters @Inject constructor(private val hunt: Hunt) : PluginScript() {
    override fun ScriptContext.startup() {
        onAiTimer(DORMANT) { wake(npc) }
    }

    @OptIn(InternalApi::class)
    private fun wake(npc: Npc) {
        if (npc.visType.id != DORMANT_ID) {
            return
        }
        val reach = WAKE_RANGE + npc.size - 1
        val nearby = hunt.findPlayers(npc.coords, reach, HuntVis.LineOfSight)
        if (nearby.none { npc.isWithinDistance(it, WAKE_RANGE) }) {
            return
        }
        val awake = checkNotNull(ServerCacheManager.getNpc(AWAKE.asRSCM(RSCMType.NPC)))
        npc.transmog(awake, Int.MAX_VALUE)
        npc.copyStats(awake)
        npc.assignUid()
        npc.anim("seq.dagannoth_rock_lobster_spring_up")
        npc.delay(1)
        val mode = checkNotNull(ServerCacheManager.getHunt(AGGRESSIVE_MELEE))
        npc.setHuntMode(mode)
        npc.setHunt(npc.type.huntRange)
    }

    private companion object {
        const val DORMANT = "npc.dagannoth_rock_lobster_2x2_loc"
        const val AWAKE = "npc.dagannoth_rock_lobster_2x2"
        const val WAKE_RANGE = 4
        const val AGGRESSIVE_MELEE = 6

        val DORMANT_ID = DORMANT.asRSCM(RSCMType.NPC)
    }
}
