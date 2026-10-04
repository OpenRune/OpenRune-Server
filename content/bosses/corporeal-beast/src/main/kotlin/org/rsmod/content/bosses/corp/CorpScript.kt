package org.rsmod.content.bosses.corp

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
import org.rsmod.api.npc.respawn.BossRespawnPolicy
import org.rsmod.api.npc.respawn.BossRespawnTimers
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.ironman.isAnyIronman
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.*
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class CorpScript @Inject constructor(
    private val deps: BossDeps,
    private val controller: CorpController,
    private val death: NpcDeath,
    private val respawns: BossRespawnTimers,
    private val access: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val spec = boss(CorpRules.BOSS) {
            stats(attackRate = 4, aggressionRadius = 16)
            val attack = ability("attack") { include(external("corp.attack")) }
            phase("combat") { rotationSelector { +then(attack) } }
        }
        BossCombat.register(this, spec, deps, onHit = { controller.damaged(npc, hit) })
        deps.extensionRegistry.register("corp.attack") { _, npc, target, _ -> controller.attack(npc, target) }
        onCommand("testcorp") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Teleport to the Corporeal Beast pre-lair"
            cheat { access.launch(player) { telejump(if (player.isAnyIronman && player.combatLevel >= 90) CorpRules.LOBBY.translate(0, 128) else CorpRules.LOBBY, TeleportType.Exempt) } }
        }
        onOpLoc1("loc.corp_beast_entrance") {
            val z = if (player.coords.x < 2976) { if (player.isAnyIronman && player.combatLevel >= 90) 4382 else 4254 } else if (player.coords.z >= 4352) 4382 else 4254
            telejump(CoordGrid(if (player.coords.x < 2976) 2976 else 2970, z, 2), TeleportType.Exempt)
        }
        onOpLoc2("loc.corp_beast_entrance") {
            val z = if (player.coords.x < 2976) { if (player.isAnyIronman && player.combatLevel >= 90) 4382 else 4254 } else if (player.coords.z >= 4352) 4382 else 4254
            mes("There are ${controller.occupants(CoordGrid(2993,z,2))} players inside the lair.")
        }
        onOpLoc1("loc.corp_cave_exit") { telejump(CoordGrid(3206, 3683, 0), TeleportType.Exempt) }
        onOpLoc1("loc.corp_cave_entrance") { telejump(if (player.isAnyIronman && player.combatLevel >= 90) CorpRules.LOBBY.translate(0, 128) else CorpRules.LOBBY, TeleportType.Exempt) }
        onEvent<NpcStateEvents.Create> { controller.created(npc) }
        onEvent<NpcStateEvents.Respawn> { controller.created(npc) }
        onEvent<NpcStateEvents.Delete> { controller.deleted(npc) }
        onEvent<GameLifecycle.LateCycle> { controller.tick() }
        onNpcQueue(checkNotNull(ServerCacheManager.getNpc(CorpRules.BOSS.asRSCM())), "queue.death") {
            controller.dying(npc)
            walk(coords)
            noneMode()
            hideAllOps()
            anim("seq.corpbeast_death")
            delay(checkNotNull(ServerCacheManager.getAnim("seq.corpbeast_death".asRSCM())))
            if (controller.canReward(npc)) death.spawnDrops(this, npc.coords.translate(2, 2), NpcDeathRewards(includeRemains = false))
            if (npc.respawns) {
                deps.npcRepo.despawn(npc, BossRespawnPolicy.OTHER_BOSS_TICKS)
                respawns.schedule(npc, BossRespawnPolicy.OTHER_BOSS_TICKS)
            } else deps.npcRepo.del(npc, Int.MAX_VALUE)
        }
        onNpcQueue(checkNotNull(ServerCacheManager.getNpc(CorpRules.CORE.asRSCM())), "queue.death") {
            controller.deleted(npc)
            noneMode()
            deps.npcRepo.del(npc, Int.MAX_VALUE)
        }
    }
}
