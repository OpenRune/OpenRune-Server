package org.rsmod.content.bosses.zulrah

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.script.onNpcQueue
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class ZulrahDeathScript @Inject constructor(
    private val controller: ZulrahEncounterController,
    private val death: NpcDeath,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (symbol in ZulrahEncounterController.BOSS_FORMS) {
            val type = requireNotNull(ServerCacheManager.getNpc(symbol.asRSCM(RSCMType.NPC)))
            onNpcQueue(type, "queue.death") {
                if (!controller.owns(npc)) death.deathWithDrops(this)
                else if (controller.beginDeath(npc)) {
                    anim("seq.snakeboss_death")
                    delay(6)
                    controller.finishDeath(npc) { tile ->
                        death.spawnDrops(this, tile, dropRemains = false,
                            dropDuration = ZulrahEncounterController.LOOT_LIFETIME_TICKS)
                    }
                }
            }
        }
        for (symbol in listOf(ZulrahEncounterController.MELEE_SNAKE, ZulrahEncounterController.MAGIC_SNAKE)) {
            val type = requireNotNull(ServerCacheManager.getNpc(symbol.asRSCM(RSCMType.NPC)))
            onNpcQueue(type, "queue.death") {
                if (!controller.owns(npc)) death.deathNoDrops(this)
                else if (controller.beginDeath(npc)) {
                    anim("seq.snakeboss_pet_death")
                    delay(2)
                    controller.finishDeath(npc)
                }
            }
        }
    }
}
