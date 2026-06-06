package dtx.example

import dtx.impl.chance.multiChanceTable
import dtx.impl.misc.percent
import dtx.impl.weighted.weightedTable

/**
 * Goblin unique drops example
 */
val weightedTableExample = weightedTable<Player, Item> {

    name("Goblin Uniques")

    10 weight Item("Goblin Ear", 1 randTo 2)
    6 weight Item("Goblin Tooth", 1 randTo 13)
    3 weight Item("Tattered rags", 1)

    2 weight multiChanceTable {


        // 37.5% chance to include BOTH items
        75.percent chance Item("Goblin Shaman Staff")
        50.percent chance Item("Shamanic Rune", 1 randTo 12)
    }

    1 weight Item("Copper Ore", 1 randTo 3)
}
