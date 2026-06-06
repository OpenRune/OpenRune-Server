package dtx.example

import dtx.core.*
import dtx.impl.matrix.matrixTable

/**
 * Quirky way to do uniform table.
 */
val chestTable = matrixTable<Player, Item> {

    name("Unique Treasures")

    onRollCompleted { player, args, treasure ->
        if (treasure == RollResult.Nothing<Item>()) {
            player.sendMessage("The only unique thing here is your bad luck! Ha!")
            return@onRollCompleted
        }
        treasure as RollResult.Single
        if (treasure.result.itemId == "Small Shiv") {
            player.sendMessage("You already have a small shiv in your... oh...")
            return@onRollCompleted
        }
        player.sendMessage("Oh wow, a ${treasure.result}! What!")
    }

    addItemAt(0, 0, Item("Sunblade"))
    addItemAt(0, 1, Item("Amulet of Greater Healing"))
    addItemAt(0, 2, Item("Ring of Protection +1"))
    addItemAt(1, 0, Item("Bag of 500 Gold Pieces"))
    addItemAt(1, 1, Item("Scroll of Arcane Knowledge"))
    addItemAt(1, 2, Item("Potion of Healing"))
    addItemAt(2, 0, Item("Small Shiv"))
    addItemAt(2, 1, Item("Stick for Chaps"))
    // Implicit because it's a 3x3 matrix and any un-filled slots will be Empty, but I put it here
    addItemAt(2, 2, Rollable.Empty())
}
