package org.rsmod.content.bosses.kraken

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.or2.central.account.Rights
import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.death.NpcDeathRewards
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class KrakenScript @Inject constructor(
    private val deps: BossDeps,
    private val controller: KrakenController,
    private val death: NpcDeath,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("krakentest") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Temporarily test Kraken without changing your Slayer task; use off to disable"
            cheat { controller.setTesting(player, args.singleOrNull()?.equals("off", true) != true) }
        }
        onOpLoc1("loc.slayer_cave_kraken_boss_entrance") {
            if (controller.allowed(player)) {
                telejump(CoordGrid(2280, 10022), TeleportType.Exempt)
            } else {
                mes("You need level 87 Slayer and a cave kraken task to enter.")
            }
        }
        onOpLoc1("loc.slayer_cave_kraken_boss_exit") {
            telejump(CoordGrid(2280, 10016), TeleportType.Exempt)
            controller.tick()
        }
        for (kind in KrakenKind.entries) {
            val spec = boss(kind.active) {
                stats(attackRate = kind.rate, aggressionRadius = 10)
                val attack = ability("attack") { include(external("kraken.attack")) }
                phase("combat", lockMovement = true) { rotationSelector { +then(attack) } }
            }
            BossCombat.register(this, spec, deps, onModifyHit = { controller.modify(npc, hit) })
            onOpNpc2(kind.pool) { controller.disturb(player, it.npc) }
            onApNpc2(kind.pool) {
                if (isWithinDistance(it.npc, 7)) controller.disturb(player, it.npc) else apRange(7)
            }
            val pool = checkNotNull(ServerCacheManager.getNpc(kind.poolId))
            val explosive = checkNotNull(ServerCacheManager.getItem("obj.fishing_explosive".asRSCM()))
            onOpNpcU(pool, explosive) { throwExplosive(it.npc, it.invSlot) }
            onApNpcU(pool, explosive) {
                if (isWithinDistance(it.npc, 7)) throwExplosive(it.npc, it.invSlot) else apRange(7)
            }
            onModifyNpcHit(pool) { hit.damage = 0 }
            onNpcQueue(checkNotNull(ServerCacheManager.getNpc(kind.activeId)), "queue.death") {
                val actor = controller.beginDeath(npc)
                if (actor == null) {
                    death.deathWithDrops(this)
                } else {
                    noneMode()
                    hideAllOps()
                    anim(kind.death)
                    delay(checkNotNull(ServerCacheManager.getAnim(kind.death.asRSCM())))
                    if (kind != KrakenKind.TENTACLE) {
                        death.spawnDrops(this, actor.owner.coords, NpcDeathRewards(includeRemains = false))
                    }
                    controller.finishDeath(actor)
                }
            }
        }
        deps.extensionRegistry.register("kraken.attack") { _, npc, target, _ -> controller.attack(npc, target) }
        onEvent<NpcStateEvents.Create> { controller.created(npc) }
        onEvent<NpcStateEvents.Respawn> { controller.created(npc) }
        onEvent<NpcStateEvents.Delete> { controller.deleted(npc) }
        onEvent<GameLifecycle.LateCycle> { controller.tick() }
    }

    private fun ProtectedAccess.throwExplosive(npc: Npc, slot: Int) {
        if (player.inv[slot]?.id != "obj.fishing_explosive".asRSCM()) return
        if (controller.disturb(player, npc, explosive = true)) {
            anim("seq.human_chinchompa_attack")
            invDel(player.inv, "obj.fishing_explosive", slot = slot)
        }
    }
}
