package org.rsmod.content.quest.area.morytania.priestinperil

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.annotations.InternalApi
import org.rsmod.api.config.constants
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.hook.PlayerPostTickHook
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.route.RayCastValidator
import org.rsmod.api.script.onAiTimer
import org.rsmod.api.script.onEvent
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.obj.Obj
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.module.PluginModule
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

class PriestInPerilModule : PluginModule() {
    override fun bind() {
        bindInstance<PriestInPeril>()
        bindInstance<PriestInPerilTravel>()
        addSetBinding<NpcDeathKillHook>(PriestInPerilKills::class.java)
        addSetBinding<PlayerPostTickHook>(PriestInPerilKills::class.java)
    }
}

@Singleton
class PriestInPerilKills
@Inject
constructor(
    private val quest: PriestInPeril,
    private val instances: InstanceManager,
    private val launcher: ProtectedAccessLauncher,
    private val objs: ObjRepository,
    private val players: PlayerRepository,
    private val interactions: AiPlayerInteractions,
    private val collision: CollisionFlagMap,
    private val clock: MapClock,
) : PluginScript(), NpcDeathKillHook, PlayerPostTickHook {
    // The reference tracks a still-owned ground drop; persistent key/quest flags use varbits.
    private val goldenDrops = mutableMapOf<Long, Obj>()

    override fun ScriptContext.startup() {
        for (monk in MONKS) {
            onEvent<NpcStateEvents.Spawn>(monk.asRSCM().toLong()) {
                if (monk == "npc.priestperilevilmonk3" && npc.coords != CoordGrid(3411, 3489, 0)) {
                    npc.noneMode()
                    npc.lifecycleDelCycle = clock.cycle + 1
                } else {
                    npc.setHunt(0)
                    npc.aiTimer(1)
                }
            }
            onAiTimer(monk) { hunt(npc) }
        }
    }

    @OptIn(InternalApi::class)
    private fun hunt(npc: Npc) {
        npc.aiTimer(2)
        if (npc.hasInteraction() || npc.isDelayed) return
        val target =
            players.findAll(ZoneKey.from(npc.coords), 1).firstOrNull {
                npc.coords.level == it.coords.level &&
                    npc.coords.chebyshevDistance(it.coords) <= 5 &&
                    quest.canMonkAggress(it, npc.isType("npc.priestperilevilmonk3")) &&
                    !it.isBusy &&
                    RayCastValidator(collision).hasLineOfSight(npc.coords, it.coords)
            } ?: return
        npc.opPlayer2(target, interactions)
    }

    override fun onPostTick(player: Player) {
        val drop = goldenDrops[player.observerUUID]
        val grounded = drop != null && objs.findAll(drop.coords).any { it === drop }
        val hidden = quest.keepMonkHidden(player, grounded)
        if (!grounded) goldenDrops.remove(player.observerUUID)
        val value = if (hidden) 1 else 0
        if (player.vars[MONK_DEAD] != value) VarPlayerIntMapSetter.set(player, MONK_DEAD, value)
    }

    @OptIn(InternalApi::class)
    override fun onKill(context: NpcDeathKillContext) {
        val hero = context.hero
        val npc = context.npc
        if (npc.isType("npc.priestperilguarddog")) {
            val session = instances.sessionForPlayer(hero) ?: return
            if (
                session.key != PriestInPerilTravel.GUARDIAN_INSTANCE ||
                    instances.instanceForNpc(npc) != session.id ||
                    quest.stage(hero) != PriestInPeril.GUARDIAN_REQUESTED
            )
                return
            launcher.launchLenient(hero) {
                if (quest.stage(player) == PriestInPeril.GUARDIAN_REQUESTED) {
                    quest.quest.setQuestStage(this, PriestInPeril.GUARDIAN_DEAD)
                    mes("The temple guardian is dead. I should report back to King Roald.")
                }
            }
        } else if (quest.dropsGoldenKey(hero, npc)) {
            val drop =
                objs.add(
                    checkNotNull(ServerCacheManager.getItem(PriestInPeril.GOLD_KEY.asRSCM())),
                    context.dropCoords,
                    hero.lootDropDuration ?: constants.lootdrop_duration,
                    hero,
                )
            hero.observerUUID?.let { goldenDrops[it] = drop }
            VarPlayerIntMapSetter.set(hero, MONK_DEAD, 1)
        }
    }

    companion object {
        private const val MONK_DEAD = "varbit.priestperilevilmonk_dead"
        private val MONKS =
            listOf(
                "npc.priestperilevilmonk1",
                "npc.priestperilevilmonk2",
                "npc.priestperilevilmonk3",
            )
    }
}
