package dtx.example.rs_tables

import dtx.rs.*
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import dtx.core.singleRollable
import dtx.example.Item
import dtx.example.Player
import dtx.example.allowPlayerMessages
import dtx.example.examplePlayer
import dtx.example.randTo

val oborGuaranteed = rsGuaranteedTable<Player, Item> {
    tableIdentifier = "Obor guaranteed drops"
    add(Item("big_bones"))
    add(clueDrop(ClueTier.Beginner))
    add(Item("ensouled_giant_head"))
}

val oborUnique = singleRollable<Player, Item> { result(Item("hill_giant_club")) }

val oborMainTable = rsWeightedTable<Player, Item> {
    name("Obor main drop table")
    6 weight Item("rune_med_helm")
    5 weight Item("rune_full_helm")
    5 weight Item("rune_longsword")
    4 weight Item("rune_battleaxe")
    3 weight Item("rune_kiteshield")
    3 weight Item("rune_chainbody")
    3 weight Item("rune_platelegs")
    3 weight Item("rune_plateskirt")
    3 weight Item("rune_2h_sword")
    12 weight Item("law_rune", 50 randTo 99)
    12 weight Item("cosmic_rune", 60 randTo 119)
    10 weight Item("chaos_rune", 100 randTo 199)
    10 weight Item("death_rune", 40 randTo 79)
    7 weight Item("nature_rune", 40 randTo 79)
    8 weight Item("noted_limpwurt_root", 20)
    8 weight Item("noted_big_bones", 50)
    6 weight Item("coins", 10_000 randTo 15_000)
    5 weight rsGuaranteedTable {
        add(Item("noted_uncut_ruby", 5))
        add(Item("noted_uncut_diamond", 5))
    }
    4 weight Item("coins", 12_000 randTo 20_000)
    1 weight oborUnique
}


val oborTertiaries = rsTertiaryTable<Player, Item> {
    name("Obor tertiaries")
    1 outOf 16 chance Item("giant_key")
    1 outOf 400 chance LongAndCurvedBoneTable
    1 outOf 5_000 chance championScroll(ChampionType.Giant)
}

val fullOborTable = RSDropTable(
    tableIdentifier = "Obor Drops",
    guaranteed = oborGuaranteed,
    mainTable = oborMainTable,
    tertiaries = oborTertiaries,
)

fun <T, R, E: RSTable<T, R>> E.countRoll(rolls: Int, target: T, idSelector: (R) -> String, otherArgs: ArgMap = ArgMap.Empty): Map<String, Int> = buildMap {
    fun R.inc() {
        val itemId = idSelector(this)
        putIfAbsent(itemId, 0)
        put(itemId, get(itemId)!! + 1)
    }

    repeat(rolls) {
        when (val result = roll(target, otherArgs).flatten()) {
            is RollResult.Nothing -> return@repeat
            is RollResult.Single -> result.result.inc()
            is RollResult.ListOf -> (result.flatten() as RollResult.ListOf<R>).results.forEach { it.inc() }
        }
    }
}

fun Double.truncStr(toDigits: Int = 2) = "%.${toDigits}f".format(this)

fun oborRollComparison(player: Player, rolls: Int = 817_368) {
    // OSRS wiki-sourced expected drop rates, as a percentage per kill
    val mainExpected = listOf(
        // Weapons and Armour
        "rune_med_helm" to 5.08,
        "rune_full_helm" to 4.23,
        "rune_longsword" to 4.23,
        "rune_battleaxe" to 3.39,
        "rune_kiteshield" to 2.54,
        "rune_chainbody" to 2.54,
        "rune_platelegs" to 2.54,
        "rune_plateskirt" to 2.54,
        "rune_2h_sword" to 2.54,

        // Runes
        "law_rune" to 10.17,
        "cosmic_rune" to 10.17,
        "chaos_rune" to 8.47,
        "death_rune" to 8.47,
        "nature_rune" to 5.93,

        // Other
        "noted_limpwurt_root" to 6.78,
        "noted_big_bones" to 6.78,
        "noted_uncut_ruby" to 4.24,
        "noted_uncut_diamond" to 4.24,
        // this is the other coin chance
        "coins" to 3.39 + 5.08,
    )

    val expected = listOf(
        // Guaranteed Drops
        "big_bones" to 100.00,
        "clue_scroll_(beginner)" to 100.00,
        "ensouled_giant_head" to 100.00,

        *mainExpected.toTypedArray(),

        //unique
        "hill_giant_club" to 0.85,

        // Tertiary
        "giant_key" to 6.25,
        "long_bone" to 0.25,
        "giant_champion_scroll" to 0.02,
        "curved_bone" to 0.02
    )
    // OSRS wiki-sourced seed drop amount to get the above expected chances
    val rollAmount = rolls
    val fullResults = fullOborTable.countRoll(rollAmount, player, { it.itemId } )
    println("full: $fullResults")
    println(mainExpected.sumOf { it.second })
    println(fullResults.entries.filter { fr -> mainExpected.any { it.first == fr.key }}.sumOf { it.value.toDouble() / rollAmount })
    println(fullResults.entries.sumOf { it.value })
    expected.forEach { (itemId, expectedDropRate) ->
        print("$itemId - expected[${expectedDropRate}] got[")
        val gotDropRate = if (fullResults[itemId] != null) {
            fullResults[itemId]!!.toDouble() / rollAmount * 100
        } else {
            0.0
        }
        print(gotDropRate.truncStr(4))
        val delta = gotDropRate - expectedDropRate
        println("] (delta: ${delta.truncStr(4)})")
    }
}

fun main() {
    allowPlayerMessages = false
    oborRollComparison(examplePlayer)
    examplePlayer.currentWorld = 2
    examplePlayer.questPoints = 33
    oborRollComparison(examplePlayer)
}
