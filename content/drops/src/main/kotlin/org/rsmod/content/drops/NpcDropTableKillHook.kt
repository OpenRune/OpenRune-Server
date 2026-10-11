package org.rsmod.content.drops

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import dtx.core.with
import dtx.rs.RSDropTable
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.config.constants
import org.rsmod.api.death.NpcDeathDropContext
import org.rsmod.api.death.NpcDeathDropHook
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.death.NpcLootQuantityHook
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.droptable.KillRollContext
import org.rsmod.api.droptable.rollCount
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

@Singleton
public class NpcDropTableKillHook
@Inject
constructor(
    private val collectionLog: CollectionLog,
    private val registry: DropTableRegistry,
    private val areaChecker: AreaChecker,
    private val objRepo: ObjRepository,
    private val random: GameRandom,
    private val deathDropHooks: Set<NpcDeathDropHook>,
    private val quantityHooks: Set<@JvmSuppressWildcards NpcLootQuantityHook> = emptySet(),
) : NpcDeathKillHook {

    private fun table(context: NpcDeathKillContext): RSDropTable<Player, DropRollItem>? {
        val overrideNpc = context.rewards.tableNpc
        return if (overrideNpc != null) registry.forNpcType(overrideNpc, context.npc.coords, areaChecker)
            else registry.forNpc(context.npc, areaChecker)
    }
    private fun roll(context: NpcDeathKillContext, table: RSDropTable<Player, DropRollItem>): RollResult<DropRollItem> =
        table.roll(context.hero, ArgMap(KillRollContext.npc with context.npc,
            KillRollContext.elapsedTicks with context.rewards.elapsedTicks,
            KillRollContext.areaChecker with areaChecker)).flatten()

    override fun prepare(context: NpcDeathKillContext): () -> Unit {
        val table = table(context)
        if (table == null || !table.rollBeforeProgression) return { onKill(context) }
        val result = roll(context, table)
        return { deliver(context, result) }
    }
    override fun onKill(context: NpcDeathKillContext) {
        val table = table(context) ?: return
        deliver(context, roll(context, table))
    }
    private fun deliver(context: NpcDeathKillContext, result: RollResult<DropRollItem>) {
        val player = context.hero
        val duration = player.lootDropDuration ?: constants.lootdrop_duration
        val dropCoords = context.dropCoords
        val npc = context.npc
        when (result) {
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
        val obj = drop.transformObj(receiver) ?: drop.obj
        val count = drop.rollCount(random)
        collectionLog.grant(receiver, obj, count, source = npc.name)
        val definition = checkNotNull(ServerCacheManager.getItem(obj.asRSCM()))
        var remaining = count
        for (hook in quantityHooks) remaining = hook.remaining(receiver, npc, definition, remaining).coerceIn(0, remaining)
        val consumed = remaining > 0 && consumedByHook(obj, remaining, coords, duration, receiver)
        if (remaining > 0 && !consumed) objRepo.add(obj, coords, duration, receiver, remaining)
        if (!consumed) ClientScripts.lootTrackerAddLoot(receiver, npc.id, eventId, definition.id, count)
        for (bonus in drop.bonusDrops) {
            spawnDrop(bonus, coords, duration, receiver, npc, eventId)
        }
    }

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
