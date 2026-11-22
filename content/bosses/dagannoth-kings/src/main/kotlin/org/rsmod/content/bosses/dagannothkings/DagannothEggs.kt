package org.rsmod.content.bosses.dagannothkings

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.hunt.HuntVis
import jakarta.inject.Inject
import org.rsmod.annotations.InternalApi
import org.rsmod.api.hunt.Hunt
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onAiTimer
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DagannothEggs
@Inject
constructor(
    private val hunt: Hunt,
    private val npcs: NpcRepository,
    private val world: WorldRepository,
) : PluginScript() {

    override fun ScriptContext.startup() {
        for (egg in EGGS) {
            onAiTimer(egg) { tick(npc) }
        }
    }

    @OptIn(InternalApi::class)
    private fun tick(npc: Npc) {
        if (npc.hidden) {
            return
        }
        val stage = npc.visType.id
        if (stage !in EGG_IDS) {
            return
        }
        when (stage) {
            EGG1_ID -> {
                val nearby = hunt.findPlayers(npc.coords, HATCH_RANGE, HuntVis.Off)
                if (nearby.none { npc.isWithinDistance(it, HATCH_RANGE) }) {
                    return
                }
                world.soundArea(npc.coords, "synth.dagganoth_egg_open", loops = 15, radius = 5)
                hatch(npc, EGG2, EGG1_TICKS)
            }

            EGG2_ID -> {
                hatch(npc, EGG3)
                spawn(npc)
            }

            EGG3_ID -> spawn(npc)
        }
    }

    @OptIn(InternalApi::class)
    private fun hatch(npc: Npc, next: String, delay: Int = 0) {
        val type = checkNotNull(ServerCacheManager.getNpc(next.asRSCM(RSCMType.NPC)))
        npc.transmog(type, Int.MAX_VALUE)
        npc.assignUid()
        npc.aiTimer = delay
    }

    private fun spawn(egg: Npc) {
        val type = checkNotNull(ServerCacheManager.getNpc(SPAWN.asRSCM(RSCMType.NPC)))
        npcs.despawn(egg, EGG_RESPAWN_TICKS)
        npcs.add(Npc(type, egg.coords), SPAWN_LIFETIME)
    }

    private companion object {
        const val EGG1 = "npc.dungeon_dagganoth_egg1"
        const val EGG2 = "npc.dungeon_dagganoth_egg2"
        const val EGG3 = "npc.dungeon_dagganoth_egg3"
        const val SPAWN = "npc.dagganoth_critter"

        const val HATCH_RANGE = 1
        const val EGG1_TICKS = 4
        const val EGG_RESPAWN_TICKS = 500
        const val SPAWN_LIFETIME = 500

        val EGGS = listOf(EGG1, EGG2, EGG3)

        val EGG1_ID = EGG1.asRSCM(RSCMType.NPC)
        val EGG2_ID = EGG2.asRSCM(RSCMType.NPC)
        val EGG3_ID = EGG3.asRSCM(RSCMType.NPC)
        val EGG_IDS = setOf(EGG1_ID, EGG2_ID, EGG3_ID)
    }
}
