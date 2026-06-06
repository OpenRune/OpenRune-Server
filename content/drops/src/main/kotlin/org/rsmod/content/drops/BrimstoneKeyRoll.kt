package org.rsmod.content.drops

import dtx.core.RollResult
import dtx.core.singleRollable
import dtx.rs.RSPrerollTableBuilder
import dtx.rs.brimstoneRarityDenominator
import kotlin.random.Random
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.KillRollContext
import org.rsmod.game.entity.Player

public fun RSPrerollTableBuilder<Player, DropRollItem>.brimstoneKeyRoll(konarTaskBonus: Boolean = false) {
    100 outOf 100 rolls
        singleRollable {
            shouldInclude { player, otherArgs ->
                val npc = otherArgs[KillRollContext.npc] ?: return@shouldInclude false
                val areaChecker = otherArgs[KillRollContext.areaChecker] ?: return@shouldInclude false
                player.shouldDropBrimstoneKey(npc, areaChecker) && npc.type.combatLevel > 0
            }
            selectResult { _, otherArgs ->
                val npc = otherArgs[KillRollContext.npc] ?: return@selectResult RollResult.Nothing()
                val denominator = brimstoneRarityDenominator(npc.type.combatLevel, konarTaskBonus)
                if (Random.nextInt(denominator) != 0) {
                    return@selectResult RollResult.Nothing()
                }
                RollResult.Single(DropRollItem("obj.konar_key", 1))
            }
        }
}
