package org.rsmod.content.drops

import dtx.core.ArgMap
import dtx.core.with
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.death.NpcDeathDropHook
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.droptable.KillRollContext
import org.rsmod.api.droptable.NpcKillExtraDrops
import org.rsmod.api.droptable.toDropList
import org.rsmod.api.player.events.LootSource

@Singleton
public class NpcDropTableKillHook
@Inject
constructor(
    private val registry: DropTableRegistry,
    private val areaChecker: AreaChecker,
    private val deathDropHooks: Set<NpcDeathDropHook>,
    private val extraDrops: Set<NpcKillExtraDrops>,
    private val loot: LootDelivery,
) : NpcDeathKillHook {

    override fun onKill(context: NpcDeathKillContext) {
        if (deathDropHooks.any { it.suppressesDropTable(context) }) {
            return
        }
        val player = context.hero
        val npc = context.npc
        val args =
            ArgMap(KillRollContext.npc with npc, KillRollContext.areaChecker with areaChecker)
        val rolled = registry.forNpc(npc, areaChecker)?.roll(player, args)?.toDropList().orEmpty()
        loot.give(
            player = player,
            source = LootSource.Npc(npc),
            drops = rolled + extraDrops.flatMap { it.drops(player, npc) },
            destination = null,
            coords = context.dropCoords,
            lootTrackerEventId = context.lootTrackerEventId,
        )
    }
}
