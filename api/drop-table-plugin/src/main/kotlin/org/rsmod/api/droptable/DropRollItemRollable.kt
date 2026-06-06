package org.rsmod.api.droptable

import dtx.core.Rollable
import dtx.core.Single
import dtx.core.rollableHooks
import dtx.core.singleRollable
import org.rsmod.game.entity.Player

public fun dropRollable(drop: DropRollItem): Rollable<Player, DropRollItem> {
    if (drop.isNothing) {
        return Single(
            drop,
            rollableHooks {
                shouldInclude { player, _ -> drop.condition(player) }
            },
        )
    }
    if (drop.killCondition == null && drop.bonusDrops.isEmpty()) {
        return Single(drop)
    }
    return conditionalDropRollable(drop)
}

public fun dropRollableWithPlayerCondition(drop: DropRollItem): Rollable<Player, DropRollItem> {
    if (drop.isNothing) {
        return dropRollable(drop)
    }
    return singleRollable {
        shouldInclude { player, otherArgs ->
            if (!drop.condition(player)) {
                return@shouldInclude false
            }
            val killCondition = drop.killCondition ?: return@shouldInclude true
            val npc = otherArgs[KillRollContext.npc] ?: return@shouldInclude false
            val areaChecker = otherArgs[KillRollContext.areaChecker] ?: return@shouldInclude false
            killCondition(player, npc, areaChecker)
        }
        result(drop)
    }
}

private fun conditionalDropRollable(drop: DropRollItem): Rollable<Player, DropRollItem> =
    singleRollable {
        shouldInclude { player, otherArgs ->
            if (!drop.condition(player)) {
                return@shouldInclude false
            }
            val killCondition = drop.killCondition ?: return@shouldInclude true
            val npc = otherArgs[KillRollContext.npc] ?: return@shouldInclude false
            val areaChecker = otherArgs[KillRollContext.areaChecker] ?: return@shouldInclude false
            killCondition(player, npc, areaChecker)
        }
        result(drop)
    }

public fun nothing(): Rollable<Player, DropRollItem> = dropRollable(nothingDrop(includeWhen = { true }))

public fun ringNothing(): Rollable<Player, DropRollItem> = dropRollable(nothingDrop())
