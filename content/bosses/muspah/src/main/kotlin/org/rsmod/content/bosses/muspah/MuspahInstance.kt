package org.rsmod.content.bosses.muspah

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bossbar.plugin.BossHpBarScript
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceEnterTransition
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.withInstanceEnterTransition
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class MuspahInstance
@Inject
constructor(
    registry: BossInstanceRegistry,
    private val deps: BossDeps,
    private val bossHpBar: BossHpBarScript,
    private val aiPlayerInteractions: AiPlayerInteractions,
) : InstanceScript(registry) {

    override fun settingsRow(): String = "dbrow.instance_muspah"

    override fun area(): InstanceArea = INSTANCE

    override fun ScriptContext.configure() {
        onEnterPrelude { result, enter ->
            withInstanceEnterTransition(InstanceEnterTransition(), enter)
            if (result is InstanceManager.Result.Created) {
                spawnMuspah(player, result.session)
            }
        }
        onEnterObject { defaultInstanceEntry() }
        onExitObject { defaultLeaveFlow() }
    }

    private fun spawnMuspah(player: Player, session: InstanceSession) {
        val coords = manager.resolveCoord(session, SPAWN_TEMPLATE_COORD) ?: return
        val type = ServerCacheManager.getNpc(MUSPAH_NPC_ID) ?: return
        val npc = Npc(type, coords)
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        manager.registerSessionNpc(player, npc)
        bossHpBar.onOpen(player, npc)
        npc.apPlayer2(player, aiPlayerInteractions)
    }

    private companion object {
        private val MUSPAH_NPC_ID = "npc.muspah".asRSCM(RSCMType.NPC)

        private val SPAWN_TEMPLATE_COORD = CoordGrid(2846, 4258, 0)

        private val INSTANCE = InstanceArea.copyRegions(centerRegionId = 11330)
    }
}
