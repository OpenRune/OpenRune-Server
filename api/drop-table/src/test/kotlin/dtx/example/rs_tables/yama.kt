package dtx.example.rs_tables

import dtx.rs.*
import dtx.core.*
import dtx.example.*
import dtx.table.uniformTable
import kotlin.random.Random

data class RandomItem(
    val item1: Item,
    val item2: Item,
    val item1Chance: Int = 50,
): Rollable<Player, Item>, RollableHooks<Player, Item> by RollableHooks.Default() {

    override fun selectResult(
        target: Player,
        otherArgs: ArgMap,
    ): RollResult<Item> {
        val rollResult = Random.nextInt(100)
        val roll = rollResult < item1Chance
        return RollResult.Single(if (roll) item1 else item2)
    }

}

val yamaUnique = rsWeightedTable<Player, Item> {
    3 weight uniformTable {
        add(Item("oathplate_helm"))
        add(Item("oathplate_chest"))
        add(Item("oathplate_legs"))
    }

    2 weight Item("soulflame_horn")
}

val yamaStandardConsumable = rsPrerollTable<Player, Item> {
     79 outOf 500 chance rsGuaranteedTable {
        val food = RandomItem(Item("wild_pie", 3 randTo 4), Item("pineapple_pizza", 3 randTo 4))
        val restore = RandomItem(Item("prayer_potion_3", 2), Item("super_restore_mix_2", 2))
        val boost = RandomItem(Item("super_combat_1", 1), Item("zamorak_mix_2", 1))
        add(food)
        add(restore)
        add(boost)

        transform { player, result ->

            val flattened = result.flatten() as RollResult.ListOf<Item>

            val concreted = flattened.results.map {
                if (it is RandAmtItem) {
                    it.concrete()
                } else {
                    it
                }
            }

            val transformedResult = RollResult.ListOf(concreted)

            transformedResult
        }
    }
}

/**
 * Numbers stolen from the wiki, adjusted to fit the integer vibe
 */
val yamaStandardOther = rsWeightedTable<Player, Item> {

    81 weight Item("noted_rune_chainbody", 8)            // 1/19.02
    65 weight Item("noted_battlestaff", 40)              // 1/23.78
    49 weight Item("noted_rune_platebody", 8)            // 1/31.7
    32 weight Item("dragon_plateskirt", 1)               // 1/47.56
    32 weight Item("dragon_platelegs", 1)                // 1/47.56

    49 weight Item("blood_rune", 400)                    // 1/31.7
    49 weight Item("law_rune", 150)                      // 1/31.7
    32 weight Item("smoke_rune", 350)                    // 1/47.56
    32 weight Item("soul_rune", 500)                     // 1/47.56
    32 weight Item("soul_rune", 1_000)                   // 1/47.56
    16 weight Item("fire_rune", 40_000)                  // 1/95.11
    16 weight Item("wrath_rune", 800)                    // 1/95.11

    114 weight Item("aether_catalyst", 850)              // 1/13.59
    114 weight Item("diabolic_worms", 90)                // 1/13.59
    81 weight Item("barrel_of_demonic_tallow_full", 1)   // 1/19.02
    65 weight Item("chasm_teleport_scroll", 6)           // 1/23.78
    49 weight Item("noted_emerald", 40)                  // 1/31.7
    49 weight Item("noted_ruby", 40)                     // 1/31.7
    49 weight Item("noted_diamond", 40)                  // 1/31.7
    16 weight Item("onyx_bolt_tips", 150)                // 1/95.11
}

val yamaStandard = RSDropTable(
    tableIdentifier = "Yama standard drops",
    preRoll = yamaStandardConsumable,
    mainTable = yamaStandardOther
)

val yamaRollChain = rsChainedTable<Player, Item> {
    1 outOf 120 rolls yamaUnique
    1 outOf 12 rolls Item("dossier")
    1 outOf 30 rolls Item("forgotten lockbox")
    1 outOf 15 rolls Item("oathplate_shard", 12)
    1 outOf 1 rolls yamaStandard
}

val yamaTertiaries = rsTertiaryTable<Player, Item> {
    1 outOf 2500 chance Item("yami")
    1 outOf 30 chance ClueTier.Elite.scrollBox
}

val yamaDropTable = RSDropTable(
    tableIdentifier = "Yama drop table",
    mainTable = yamaRollChain,
    tertiaries = yamaTertiaries
)

fun main() {
    while(true) {
        val roll = yamaDropTable.roll(examplePlayer)
        val flattened = roll.flatten()
        println(flattened)
        readlnOrNull() ?: break
    }
}

