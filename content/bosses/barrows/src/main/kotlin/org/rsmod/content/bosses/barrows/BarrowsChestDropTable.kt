package org.rsmod.content.bosses.barrows

import dtx.core.RollResult
import dtx.core.singleRollable
import dtx.rs.RSDropTable
import dtx.rs.locs
import dtx.rs.rsGuaranteedTable
import org.rsmod.api.droptable.DropRateModifiers
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.content.drops.eliteClueDropDenominator
import org.rsmod.game.entity.Player

internal const val BARROWS_CHEST = "loc.barrows_stone_chest"

@field:RegisterDropTable
@JvmField
val barrowsChestDropTable: RSDropTable<Player, DropRollItem> =
    RSDropTable(
        tableIdentifier = "Barrows Chest",
        locs = locs(BARROWS_CHEST),
        guaranteed =
            rsGuaranteedTable {
                add(singleRollable { selectResult { player, _ -> player.rollBarrowsChest() } })
            },
    )

private fun Player.rollBarrowsChest(): RollResult<DropRollItem> {
    val loot =
        BarrowsLoot.roll(
            slain = slainBrothers,
            potential = fullPotential,
            runeBonus = vars["varbit.morytania_diary_hard_complete"] == 1,
            clueDenominator = eliteClueDropDenominator(BarrowsLoot.CLUE_DENOMINATOR),
            equipmentBoost = DropRateModifiers.multiplierFor(this),
        )
    if (loot.isEmpty()) return RollResult.Nothing()
    return RollResult.ListOf(
        loot.map { item ->
            if (item.obj == BarrowsLoot.CLUE_OBJ) {
                DropRollItem(
                    item.obj,
                    item.count,
                    transformObj = { it.clueScrollTransformObj(item.obj) },
                )
            } else {
                DropRollItem(item.obj, item.count)
            }
        }
    )
}
