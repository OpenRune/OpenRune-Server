package org.rsmod.content.drops

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import dtx.core.with
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.config.constants
import org.rsmod.api.death.NpcDeathDropContext
import org.rsmod.api.death.NpcDeathDropHook
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.death.NpcDropReceiveHook
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.droptable.KillRollContext
import org.rsmod.api.droptable.rollCount
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.events.PlayerNpcDropEvents
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.map.CoordGrid

@Singleton
public class NpcDropTableKillHook
@Inject
constructor(
    private val registry: DropTableRegistry,
    private val areaChecker: AreaChecker,
    private val objRepo: ObjRepository,
    private val random: GameRandom,
    private val deathDropHooks: Set<NpcDeathDropHook>,
    private val receiveHooks: Set<NpcDropReceiveHook>,
    private val eventBus: EventBus,
) : NpcDeathKillHook {

    override fun onKill(context: NpcDeathKillContext) {
        val table = registry.forNpc(context.npc, areaChecker) ?: return
        if (deathDropHooks.any { it.suppressesDropTable(context) }) {
            return
        }

        val player = context.hero
        val duration = player.lootDropDuration ?: constants.lootdrop_duration
        val dropCoords = context.dropCoords

        val npc = context.npc
        when (
            val result =
                table
                    .roll(
                        player,
                        ArgMap(
                            KillRollContext.npc with npc,
                            KillRollContext.areaChecker with areaChecker,
                        ),
                    ).flatten()
        ) {
            is RollResult.Nothing -> Unit
            is RollResult.Single ->
                spawnDrop(
                    result.result,
                    dropCoords,
                    duration,
                    player,
                    npc,
                    context.lootTrackerEventId,
                )
            is RollResult.ListOf ->
                result.results.forEach { drop ->
                    spawnDrop(drop, dropCoords, duration, player, npc, context.lootTrackerEventId)
                }
        }
    }

    private fun spawnDrop(
        drop: DropRollItem,
        coords: CoordGrid,
        duration: Int,
        receiver: Player,
        npc: Npc,
        eventId: Int,
    ) {
        if (drop.isNothing) {
            return
        }
        if (!drop.condition(receiver)) {
            return
        }
        drop.killCondition?.let { killCondition ->
            if (!killCondition(receiver, npc, areaChecker)) {
                return
            }
        }
        val prepare =
            PlayerNpcDropEvents.Prepare(
                player = receiver,
                npc = npc,
                item = drop.transformObj(receiver) ?: drop.obj,
                count = drop.rollCount(random),
            )
        eventBus.publish(prepare)
        if (!prepare.cancelled && prepare.count > 0) {
            deliver(prepare, coords, duration, eventId)
        }
        for (bonus in drop.bonusDrops) {
            spawnDrop(bonus, coords, duration, receiver, npc, eventId)
        }
    }

    private fun deliver(
        prepare: PlayerNpcDropEvents.Prepare,
        coords: CoordGrid,
        duration: Int,
        eventId: Int,
    ) {
        val receiver = prepare.player
        val npc = prepare.npc
        val obj = prepare.item
        val count = prepare.count
        CollectionLog.grant(receiver, obj, count)
        if (
            !receiveHooks.any { it.tryReceive(receiver, obj, count) } &&
                !consumedByHook(obj, count, coords, duration, receiver)
        ) {
            if (!receiver.addToDestination(prepare.destination, obj, count)) {
                objRepo.add(obj, coords, duration, receiver, count)
            }
            val objId = obj.asRSCM(RSCMType.OBJ)
            ClientScripts.lootTrackerAddLoot(receiver, npc.id, eventId, objId, count)
        }
        if (eventBus.unbound[PlayerNpcDropEvents.Dropped::class.java] != null) {
            eventBus.publish(PlayerNpcDropEvents.Dropped(receiver, npc, obj, count))
        }
    }

    private fun Player.addToDestination(destination: Inventory?, obj: String, count: Int): Boolean =
        destination != null && invAdd(destination, obj, count, ignoreVirtualStorage = true).success

    private fun consumedByHook(
        obj: String,
        count: Int,
        coords: CoordGrid,
        duration: Int,
        receiver: Player,
    ): Boolean {
        if (count != 1) {
            return false
        }
        val type = ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)) ?: return false
        val context = NpcDeathDropContext(receiver, type, coords, duration, objRepo)
        return deathDropHooks.any { it.tryConsume(context) }
    }
}
