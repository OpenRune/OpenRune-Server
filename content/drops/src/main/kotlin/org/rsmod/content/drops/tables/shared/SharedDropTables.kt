package org.rsmod.content.drops.tables.shared

import dtx.rs.RSWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.game.entity.Player

public object SharedDropTables {
    public val herb: RSWeightedTable<Player, DropRollItem> = herbDropTable
    public val usefulHerb: RSWeightedTable<Player, DropRollItem> = usefulHerbDropTable
    public val combatHerb: RSWeightedTable<Player, DropRollItem> = combatHerbDropTable
    public val gem: RSWeightedTable<Player, DropRollItem> = gemDropTable
    public val seed: RSWeightedTable<Player, DropRollItem> = allotmentSeedDropTable
    public val rareSeed: RSWeightedTable<Player, DropRollItem> = rareSeedDropTable
    public val megaRare: RSWeightedTable<Player, DropRollItem> = megaRareDropTable
    public val rareDrop: RSWeightedTable<Player, DropRollItem> = rareDropTable
}
