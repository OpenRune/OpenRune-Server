package org.rsmod.content.bosses.araxxor

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

@Singleton
class AraxxorLair @Inject constructor(
    private val deps: BossDeps,
    private val manager: InstanceManager,
) {
    fun spawnBoss(player: Player, session: InstanceSession) {
        if (manager.sessionsForKey(INSTANCE_KEY).none { it === session }) return
        val coords = manager.resolveCoord(session, SPAWN_TILE) ?: return
        val type = ServerCacheManager.getNpc(ARAXXOR.asRSCM(RSCMType.NPC)) ?: return
        val boss = Npc(type, coords)
        deps.npcRepo.add(boss, Int.MAX_VALUE)
        boss.respawns = false
        if (!manager.registerSessionNpc(player, boss)) manager.attachNpc(session.id, boss)
    }
}
