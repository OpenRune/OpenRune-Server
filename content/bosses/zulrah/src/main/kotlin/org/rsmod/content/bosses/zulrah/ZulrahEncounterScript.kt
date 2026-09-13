package org.rsmod.content.bosses.zulrah

import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.instances.events.InstanceEndedEvent
import org.rsmod.api.instances.events.InstancePlayerJoinEvent
import org.rsmod.api.instances.events.instanceEventId
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.plugin.scripts.ScriptContext

internal class ZulrahEncounterScript @Inject constructor(
    private val controller: ZulrahEncounterController,
    deps: BossDeps,
) : BossPluginScript(deps) {
    override val spec = ZulrahSpec.boss

    override fun ScriptContext.startup() {
        registerEncounter(spec)
    }

    internal fun ScriptContext.registerEncounter(bossSpec: BossSpec) {
        BossCombat.register(this, bossSpec, deps, onModifyHit = { controller.modifyHit(npc, hit) })
        BossCombat.register(this, ZulrahSpec.snakelings, deps,
            onModifyHit = { controller.modifyHit(npc, hit) })
        deps.extensionRegistry.register("zulrah.emerge") { _, npc, _, params ->
            controller.emerge(npc, params as ZulrahRoutineEvent)
        }
        deps.extensionRegistry.register("zulrah.dive") { _, npc, _, _ -> controller.dive(npc) }
        deps.extensionRegistry.register("zulrah.tail_attack") { _, npc, _, params ->
            controller.tailWindup(npc, params as ZulrahTailAttack)
        }
        deps.extensionRegistry.register("zulrah.attack") { _, npc, _, params ->
            controller.launchAttack(npc, params as ZulrahRoutineEvent)
        }
        deps.extensionRegistry.register("zulrah.hazard") { _, npc, _, params ->
            controller.launchHazard(npc, params as ZulrahRoutineEvent)
        }
        deps.extensionRegistry.register("zulrah.evidence_limit") { _, npc, _, _ -> controller.evidenceLimit(npc) }
        deps.extensionRegistry.register("zulrah.snake_attack") { _, npc, target, _ ->
            controller.snakeAttack(npc, target)
        }
        onEvent<InstancePlayerJoinEvent>(instanceEventId(ZulrahIsland.KEY)) {
            controller.enter(player, instanceId)
        }
        onEvent<InstanceEndedEvent>(instanceEventId(ZulrahIsland.KEY)) { controller.end(instanceId) }
        onEvent<GameLifecycle.LateCycle> { controller.damageClouds(deps.mapClock.cycle - 1) }
        onEvent<NpcStateEvents.Delete> { controller.deleted(npc) }
    }
}
