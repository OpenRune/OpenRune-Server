package org.rsmod.content.bosses.araxxor

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceEnterTransition
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.instances.withInstanceEnterTransition
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.travel.maptransports.MapTransportTable
import org.rsmod.plugin.scripts.ScriptContext

private const val SPAWN_DELAY_TICKS = 5

class AraxxorInstance
@Inject
constructor(
    registry: BossInstanceRegistry,
    private val deps: BossDeps,
    private val lair: AraxxorLair,
    private val transports: MapTransportTable,
) : InstanceScript(registry) {
    private val bossTunnelId = BOSS_TUNNEL.asRSCM(RSCMType.LOC)

    override fun settingsRow(): String = "dbrow.instance_araxxor"

    override fun area(): InstanceArea = INSTANCE

    override fun runsPreludeOnFreshRun(): Boolean = true

    override fun ScriptContext.configure() {
        onEnterPrelude { result, enter ->
            val session =
                when (result) {
                    is InstanceManager.Result.Created -> result.session
                    is InstanceManager.Result.Joined -> result.session
                    else -> return@onEnterPrelude
                }
            val spawnBoss = result.isFreshRun()
            withInstanceEnterTransition(InstanceEnterTransition(), enter)
            if (spawnBoss) {
                deps.worldQueues.add(SPAWN_DELAY_TICKS) {
                    if (manager.sessionForPlayer(player) == session) lair.spawnBoss(player, session)
                }
            }
        }
        onEnterObject { enterOrLeave() }
        onOpLoc1(CAVE_TUNNEL) {
            if (it.loc.id == bossTunnelId) {
                enterOrLeave()
            } else {
                with(transports) { travel(it.vis.id, op = 1) }
            }
        }
        onExitObject { defaultLeaveFlow() }
        onInstancePlayerJoin { player.npcViewDistance = NPC_VIEW_DISTANCE }
        onInstancePlayerLeave { player.npcViewDistance = null }
    }

    private suspend fun ProtectedAccess.enterOrLeave() {
        if (manager.sessionForPlayer(player) != null) defaultLeaveFlow() else defaultInstanceEntry()
    }

    private companion object {
        private const val CAVE_TUNNEL = "loc.araxxor_cave_outer_tunnel_op"
        private const val BOSS_TUNNEL = "loc.araxxor_boss_tunnel_multi"
        private const val NPC_VIEW_DISTANCE = 40
        private const val ARENA_REGION = 14489
        private const val TUNNEL_REGION = 14745

        private val INSTANCE =
            InstanceArea.copyRegions(regionIds = listOf(ARENA_REGION, TUNNEL_REGION))
    }
}
