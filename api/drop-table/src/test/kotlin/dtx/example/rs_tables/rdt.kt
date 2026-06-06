package dtx.example.rs_tables

import dtx.rs.*
import dtx.example.Item
import dtx.example.Player
import dtx.example.examplePlayer

val ringOfWealth = Item("ring_of_wealth")

val megaRareDropTable = rsWeightedTable<Player, Item> {

    113 weight {
        shouldInclude { target, args -> !target.isWearing(ringOfWealth) }
        result(Item("nothing"))
    }

    8 weight Item("rune_spear")
    4 weight Item("shield_left_half")
    3 weight Item("dragon_spear")
}

val gemDropTable = rsWeightedTable<Player, Item> {

    63 weight {
        shouldInclude { target, args -> !target.isWearing(ringOfWealth) }
        result(Item("nothing"))
    }

    32 weight Item("uncut_sapphire")
    16 weight Item("uncut_emerald")
    8 weight Item("uncut_ruby")
    3 weight Item("chaos_talisman")
    3 weight Item("nature_talisman")
    2 weight Item("uncut_diamond")
    1 weight Item("rune_javelin", 5)
    1 weight Item("loop_half_of_key")
    1 weight Item("tooth_half_of_key")
    1 weight megaRareDropTable
}

val rareDropTable = rsWeightedTable<Player, Item> {
    21 weight Item("coins", 3000)
    20 weight gemDropTable
    15 weight megaRareDropTable
    5 weight Item("runite_bar")
    3 weight Item("nature_rune", 67)
    3 weight Item("rune_2h_sword")
    3 weight Item("rune_battleaxe")
    2 weight Item("adamant_javelin", 20)
    2 weight Item("death_rune", 45)
    2 weight Item("law_rune", 45)
    2 weight Item("rune_arrow", 42)
    2 weight Item("steel_arrow", 150)
    2 weight Item("rune_sq_shield")
    2 weight Item("loop_half_of_key")
    2 weight Item("tooth_half_of_key")
    2 weight Item("dragonstone")
    2 weight Item("noted_silver_ore", 100)
    1 weight Item("dragon_med_helm")
    1 weight Item("rune_kiteshield")
}

fun rollRdt(player: Player, rollAmount: Int) {
    val fullResults = rareDropTable.countRoll(rollAmount, player, { it.itemId } )
    fullResults.forEach { (itemId) ->
        print("$itemId - got[")
        val gotDropRate = fullResults[itemId]!!.toDouble() / rollAmount * 100
        print(gotDropRate)
        println("]")
    }
}

fun main() {
    val rolls = 1_000_000
    println("Without ROW")
    rollRdt(examplePlayer, rolls)
    println("\n\nWith ROW")
    val rowPlayer = examplePlayer.copy(equipment = mutableListOf(ringOfWealth))
    rollRdt(rowPlayer, rolls)
}
