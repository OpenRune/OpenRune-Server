package dtx.example

import dtx.impl.sequential.sequentialTable

/**
 * Good for stuff like dailies, weeklies, something that has some revolving reward.
 */
val sequentialTableExample = sequentialTable<Player, Item> {

    name("Daily Login Rewards")

    addEntry(Item("Day 1 - Spaghetti Box"))
    addEntry(Item("Day 2 - Some coins", 100 randTo 1_000))
    addEntry(Item("Day 3 - Shield", 1))
    addEntry(Item("Day 4 - Epic Healing Potion of Awesomeness", 1))
}
