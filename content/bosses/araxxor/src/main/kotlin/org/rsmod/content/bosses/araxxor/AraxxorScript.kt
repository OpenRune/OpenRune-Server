package org.rsmod.content.bosses.araxxor

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.instances.events.InstanceEndedEvent
import org.rsmod.api.instances.events.InstancePlayerLeaveEvent
import org.rsmod.api.instances.events.instanceEventId
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.plugin.scripts.ScriptContext

internal class AraxxorScript @Inject constructor(
    deps: BossDeps,
    private val controller: AraxxorController,
    private val death: NpcDeath,
) : BossPluginScript(deps) {
    override val spec = boss(AraxxorAssets.BOSS) {
        stats(attackRate = 6, aggressionRadius = 8)
        val attack = ability("attack") { include(external("araxxor.attack")) }
        phase("combat") { rotationSelector { +then(attack) } }
    }

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps, onModifyHit = { controller.modifyHit(npc, hit) })
        val spiders = boss(*AraxyteKind.entries.map { it.spider }.toTypedArray()) {
            stats(attackRate = 6, aggressionRadius = 8, retaliateOnHit = false)
            val attack = ability("attack") { include(external("araxxor.spider")) }
            phase("combat") { rotationSelector { +then(attack) } }
        }
        BossCombat.register(this, spiders, deps, onModifyHit = { controller.modifyHit(npc, hit) })
        deps.extensionRegistry.register("araxxor.spider") { _, npc, target, _ -> controller.spiderAttack(npc, target) }
        for (kind in AraxyteKind.entries) {
            onModifyNpcHit(checkNotNull(ServerCacheManager.getNpc(kind.egg.asRSCM()))) { controller.modifyHit(npc, hit) }
            for (symbol in listOf(kind.egg, kind.spider)) {
                onNpcQueue(checkNotNull(ServerCacheManager.getNpc(symbol.asRSCM())), "queue.death") {
                    if (!controller.minionDeath(npc)) {
                        death.deathNoDrops(this)
                    } else {
                        noneMode()
                        hideAllOps()
                        anim(when {
                            symbol == kind.egg -> "seq.egg_araxyte_death_01"
                            kind == AraxyteKind.MIRRORBACK -> "seq.npc_araxyte_mirror_back_death"
                            else -> "seq.npc_araxyte02_death"
                        })
                        delay(2)
                        controller.removeActor(npc)
                    }
                }
            }
        }
        deps.extensionRegistry.register("araxxor.attack") { _, npc, target, _ -> controller.attack(npc, target) }
        onEvent<GameLifecycle.LateCycle> { controller.tick() }
        onEvent<InstancePlayerLeaveEvent>(instanceEventId(AraxxorArena.KEY)) {
            if (isOwner) controller.end(instanceId)
        }
        onEvent<InstanceEndedEvent>(instanceEventId(AraxxorArena.KEY)) { controller.end(instanceId) }
        onEvent<NpcStateEvents.Delete> { controller.deleted(npc) }
        onNpcQueue(checkNotNull(ServerCacheManager.getNpc(AraxxorAssets.BOSS.asRSCM())), "queue.death") {
            if (!controller.owns(npc)) {
                death.deathWithDrops(this)
            } else if (controller.beginDeath(npc)) {
                noneMode()
                hideAllOps()
                anim(AraxxorAssets.DEATH)
                delay(6)
                val dropTile = npc.coords.translate(npc.size / 2, npc.size / 2)
                controller.finishDeath(npc) { death.spawnDrops(this, dropTile) }
            }
        }
        onOpNpc1(AraxxorAssets.CORPSE) { controller.harvest(player, it.npc) }
        onOpNpc3(AraxxorAssets.CORPSE) {
            mes("Destroy-for-pet rewards are not available yet. Harvest the corpse to claim this kill.")
        }
    }
}
