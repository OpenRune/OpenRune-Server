package dtx.example

import dtx.core.RollResult
import dtx.impl.chance.*
import dtx.impl.misc.percent
import dtx.impl.weighted.weightedTable

/**
 * Simple chicken example with a twist
 */
val multiChanceTableExample = multiChanceTable<Player, Item> {

    name("Chicken")

    100.percent chance Item("Bones")
    100.percent chance Item("Raw Chicken")
    75.percent chance weightedTable {

        300 weight Item("Feather", 5)
        100 weight Item("Feather", 15)

        1 weight {

            onRollCompleted { player, args, result ->

                result as RollResult.Single<Item>
                player.sendMessage("what the cluck, ${result.result.itemAmount} feathers?")
            }

            result(Item("Feather", 1_000_000 randTo 100_000_000))
        }
    }
}

fun main() {
    repeat(1_000_000) {
        multiChanceTableExample.roll(examplePlayer)
    }
}