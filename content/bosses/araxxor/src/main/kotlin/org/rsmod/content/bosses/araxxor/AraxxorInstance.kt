package org.rsmod.content.bosses.araxxor

import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceEnterTransition
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.instances.withInstanceEnterTransition
import org.rsmod.plugin.scripts.ScriptContext

private const val SPAWN_DELAY_TICKS = 5

class AraxxorInstance
@Inject
constructor(
    registry: BossInstanceRegistry,
    private val deps: BossDeps,
    private val lair: AraxxorLair,
) : InstanceScript(registry) {

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
        onEnterObject {
            if (manager.sessionForPlayer(player) != null) defaultLeaveFlow() else defaultInstanceEntry()
        }
        onExitObject { defaultLeaveFlow() }
    }

    private companion object {
        private const val ARENA_REGION = 14489
        private const val TUNNEL_REGION = 14745

        private val INSTANCE =
            InstanceArea.copyRegions(regionIds = listOf(ARENA_REGION, TUNNEL_REGION))
    }
}
