package org.rsmod.content.bosses.zulrah

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.instances.events.InstanceEndedEvent
import org.rsmod.api.instances.events.InstancePlayerLeaveEvent
import org.rsmod.api.instances.events.instanceEventId
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcQueue
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.plugin.scripts.ScriptContext

internal class ZulrahEncounterScript @Inject constructor(
    deps: BossDeps,
    private val death: NpcDeath,
    private val controller: ZulrahEncounterController,
) : BossPluginScript(deps) {
    override val spec = ZulrahSpec.boss

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps, onModifyHit = { controller.modifyHit(npc, hit) })
        BossCombat.register(this, ZulrahSpec.snakelings, deps,
            onModifyHit = { controller.modifyHit(npc, hit) })

        deps.extensionRegistry.register("zulrah.emerge") { _, npc, _, params ->
            controller.emerge(npc, params as ZulrahRoutineEvent)
        }
        deps.extensionRegistry.register("zulrah.dive") { _, npc, _, _ ->
            controller.dive(npc)
        }
        deps.extensionRegistry.register("zulrah.tail_attack") { _, npc, _, params ->
            controller.tailWindup(npc, params as ZulrahTailAttack)
        }
        deps.extensionRegistry.register("zulrah.attack") { _, npc, _, params ->
            controller.launchAttack(npc, params as ZulrahRoutineEvent)
        }
        deps.extensionRegistry.register("zulrah.hazard") { _, npc, _, params ->
            controller.launchHazard(npc, params as ZulrahRoutineEvent)
        }
        deps.extensionRegistry.register("zulrah.snake_attack") { _, npc, target, _ ->
            controller.snakeAttack(npc, target)
        }

        onEvent<InstancePlayerLeaveEvent>(instanceEventId(ZulrahIsland.KEY)) {
            if (isOwner) controller.end(instanceId)
        }
        onEvent<InstanceEndedEvent>(instanceEventId(ZulrahIsland.KEY)) {
            controller.end(instanceId)
        }
        onEvent<GameLifecycle.LateCycle> {
            controller.tick(deps.mapClock.cycle - 1)
        }
        onEvent<NpcStateEvents.Delete> { controller.deleted(npc) }

        for (symbol in ZulrahEncounterController.BOSS_FORMS) {
            val type = requireNotNull(ServerCacheManager.getNpc(symbol.asRSCM(RSCMType.NPC)))
            onNpcQueue(type, "queue.death") {
                if (!controller.owns(npc)) {
                    death.deathWithDrops(this)
                } else if (controller.beginDeath(npc)) {
                    anim("seq.snakeboss_death")
                    delay(6)
                    controller.finishDeath(npc) { dropCoords ->
                        death.spawnDrops(this, dropCoords)
                    }
                }
            }
        }
        for (symbol in listOf(ZulrahEncounterController.MELEE_SNAKE,
            ZulrahEncounterController.MAGIC_SNAKE)) {
            val type = requireNotNull(ServerCacheManager.getNpc(symbol.asRSCM(RSCMType.NPC)))
            onNpcQueue(type, "queue.death") {
                if (!controller.owns(npc)) {
                    death.deathNoDrops(this)
                } else if (controller.beginDeath(npc)) {
                    anim("seq.snakeboss_pet_death")
                    delay(2)
                    controller.finishDeath(npc)
                }
            }
        }
    }
}
