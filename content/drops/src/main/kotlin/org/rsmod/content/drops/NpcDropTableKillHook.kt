package org.rsmod.content.drops

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import dtx.core.with
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.constants
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.droptable.KillRollContext
import org.rsmod.api.droptable.rollCount
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

@Singleton
public class NpcDropTableKillHook
@Inject
constructor(
    private val registry: DropTableRegistry,
    private val areaChecker: AreaChecker,
    private val objRepo: ObjRepository,
    private val random: GameRandom,
) : NpcDeathKillHook {

    override fun onKill(context: NpcDeathKillContext) {
        val table = registry.forNpc(context.npc, areaChecker) ?: return

        val player = context.hero
        val duration = player.lootDropDuration ?: constants.lootdrop_duration
        val dropCoords = context.npc.coords

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
            is RollResult.Single -> spawnDrop(result.result, dropCoords, duration, player, npc)
            is RollResult.ListOf ->
                result.results.forEach { drop ->
                    spawnDrop(drop, dropCoords, duration, player, npc)
                }
        }
    }

    private fun spawnDrop(
        drop: DropRollItem,
        coords: CoordGrid,
        duration: Int,
        receiver: Player,
        npc: Npc,
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
        objRepo.add(obj, coords, duration, receiver, drop.rollCount(random))
        for (bonus in drop.bonusDrops) {
            spawnDrop(bonus, coords, duration, receiver, npc)
        }
    }
}
