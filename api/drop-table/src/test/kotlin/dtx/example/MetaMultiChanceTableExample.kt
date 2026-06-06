package dtx.example

import dtx.core.ArgMap
import dtx.impl.chance.MetaChanceRollable
import dtx.impl.chance.metaMultiChanceTable
import dtx.impl.meta.MetaEntryFilter
import dtx.impl.meta.MetaRollable
import dtx.impl.misc.percent

enum class BossModAmount(val modifyBy: Double) {
    Miniscule(0.025), Slight(0.05), Small(0.1), Moderate(1.0), Medium(2.0), Large(5.0);
}

data class BossFilter(
    val idEquals: String,
    val direction: MetaModDirection,
    val type: BossModAmount,
): MetaEntryFilter<Player, Item> {

    override fun filterEntry(modifier: String): Boolean {
        return modifier == idEquals
    }

    override fun modifyEntry(entry: MetaRollable<Player, Item>) {
        entry as MetaChanceRollable<Player, Item>
        val old = entry.chance
        val modBy = direction.operation(0.0, type.modifyBy)
        val new = direction.operation(0.0, direction.operation(old, modBy))
        println("Modifying ${entry.identifier} by ${type.modifyBy} ($old -> $new (range[${entry.minChance} - ${entry.maxChance}])")
        entry.increaseCurrentChanceBy(modBy)
    }
}

/**
 * Mild example of an NPC-based pity drop system rather than player-based.
 * This example never guarantees a drop but just raises the chance of a drop happening, and then lowers it after it is picked.
 */
val ExampleBossDrops = metaMultiChanceTable<Player, Item> {

    name("Example boss drops")

    100.0.percent chance {

        id("bone")

        rollable(Item("Bone"))

        minChance(100.0)
        maxChance(100.0)

        addFilter(BossFilter("scrap", MetaModDirection.Increase, BossModAmount.Small))
        addFilter(BossFilter("helmet", MetaModDirection.Increase, BossModAmount.Slight))
        addFilter(BossFilter("sword", MetaModDirection.Increase, BossModAmount.Miniscule))
    }

    33.percent chance {

        id("scrap")

        rollable(Item("Armour scrap", 1 randTo 3))

        minChance(25.0)
        maxChance(40.0)


        addFilter(BossFilter("scrap", MetaModDirection.Decrease, BossModAmount.Moderate))
        addFilter(BossFilter("helmet", MetaModDirection.Increase, BossModAmount.Small))
        addFilter(BossFilter("sword", MetaModDirection.Increase, BossModAmount.Slight))
    }

    10.percent chance {

        id("helmet")

        rollable(Item("Helmet"))

        minChance(6.66)
        maxChance(16.66)

        addFilter(BossFilter("helmet", MetaModDirection.Decrease, BossModAmount.Moderate))
        addFilter(BossFilter("scrap", MetaModDirection.Increase, BossModAmount.Slight))
        addFilter(BossFilter("sword", MetaModDirection.Increase, BossModAmount.Miniscule))
    }

    2.percent chance {

        id("sword")
        rollable(Item("Sword"))

        minChance(1.00)
        maxChance(5.00)

        addFilter(BossFilter("sword", MetaModDirection.Decrease, BossModAmount.Large))
        addFilter(BossFilter("helmet", MetaModDirection.Increase, BossModAmount.Moderate))
    }

}

fun main() {
    repeat(1000) {
        println("-------------------------")
        println("Chances at $it rolls")
        ExampleBossDrops.tableEntries.forEach { item ->
            println("${item.identifier}: ${item.chance}")
        }
        println("-------------------------")
        readlnOrNull()

        val result = ExampleBossDrops.roll(examplePlayer, ArgMap())
        println("Result: $result")
    }
}
