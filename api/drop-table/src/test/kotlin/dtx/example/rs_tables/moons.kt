package dtx.example.rs_tables

import dtx.rs.*
import dtx.core.ArgKey
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.singleRollable
import dtx.example.Item
import dtx.example.Player
import dtx.example.examplePlayer
import dtx.example.randTo
import dtx.table.uniformTable
import kotlin.random.Random

val ROLLED_MOON_UNIQUE = ArgKey("rolled_moon_unique", false)

fun howManyRolls(defeatedMoons: Int): Int {
    return (defeatedMoons * (defeatedMoons + 1)) / 2
}

enum class MoonBoss(vararg val uniques: String) {

    Eclipse("eclipse_atlatl", "eclipse_moon_helm", "eclipse_moon_chestplate", "eclipse_moon_tassets"),
    Blood("dual_macuahitl", "blood_moon_helm", "blood_moon_chestplate", "blood_moon_tassets"),
    Blue("blue_moon_spear", "blue_moon_helm", "blue_moon_chestplate", "blue_moon_tassets");

    val moonRoll = uniformTable<Player, Item> {

        vetoRoll { player, otherArgs ->
            val defeatedAtLeastOne = player.moonsTempProgress.howManyDefeated() > 0
            val defeatedThisMoon = player.moonsTempProgress.defeated[ordinal]
            if (!defeatedAtLeastOne) {
                return@vetoRoll true
            }
            if (!defeatedThisMoon) {
                return@vetoRoll true
            }
            false
        }

        uniques.forEach {

            add {

                shouldInclude { player, otherArgs ->
                    !player.moonsProtection.protectedAgainst(this@MoonBoss, it)
                }

                onRollCompleted { player, usedArgs, result, ->
                    player.moonsProtection.addProtectionAgainst(this@MoonBoss, it)
                }

                selectResult { player, otherArgs ->
                    otherArgs[ROLLED_MOON_UNIQUE] = true
                    RollResult.Single(Item(it))
                }
            }
        }
    }
}

val moonsUniques = rsPrerollTable<Player, Item> {

    vetoRoll { player, otherArgs ->
        player.moonsTempProgress.howManyDefeated() == 0
    }

    MoonBoss.entries.forEach { moonBoss ->
        1 outOf 56 chance moonBoss.moonRoll
    }
}

val moonsStandardTable = rsWeightedTable<Player, Item> {

    name("Moons of Peril — Standard Loot (approx)")

    10 weight Item("atlatl_dart", 72 randTo 120)
    8 weight Item("swamp_tar", 79 randTo 119)
    7 weight Item("sun_kissed_bones_noted", 6 randTo 12)
    7 weight Item("supercompost_noted", 6 randTo 12)
    6 weight Item("soft_clay_noted", 15 randTo 25)
    6 weight Item("grimy_harralander_noted", 12 randTo 18)
    6 weight Item("blessed_bone_shards", 160 randTo 195)
    6 weight Item("water_orb_noted", 14 randTo 20)

    4 weight Item("maple_seed", 1)
    4 weight Item("wyrmling_bones_noted", 42 randTo 54)
    3 weight Item("grimy_irit_leaf_noted", 12 randTo 18)
    3 weight Item("yew_seed", 1)
}

val moonsStandardDrop = singleRollable<Player, Item> {

    vetoRoll {  player, otherArgs ->
        val rolledUnique = otherArgs[ROLLED_MOON_UNIQUE]
        val howManyDefeated = player.moonsTempProgress.howManyDefeated()
        val defeatedNone = howManyDefeated == 0
        val shouldVeto = rolledUnique || defeatedNone
        shouldVeto
    }

    selectResult { player, otherArgs ->
        // Either didn't get a unique, as noted in the veto, or defeated at least 1 moon.

        val rolls = howManyRolls(player.moonsTempProgress.howManyDefeated())

        if (rolls == 1) {
            return@selectResult moonsStandardTable.roll(player, otherArgs)
        }

        RollResult.ListOf(
            buildList {
                repeat(rolls) {

                    add(moonsStandardTable.roll(player, otherArgs))
                }
            }
        )

        val standard = buildList {
            repeat(rolls) {
                when (val standardRoll = moonsStandardTable.roll(player)) {
                    is RollResult.Single -> add(standardRoll.result)
                    is RollResult.ListOf -> addAll(standardRoll.results)
                    is RollResult.Nothing -> {}
                }
            }
        }

        when (standard.size) {
            0 -> RollResult.Nothing()
            1 -> RollResult.Single(standard.first())
            else -> RollResult.ListOf(standard)
        }
    }

    onRollCompleted { player, usedArgs, result ->
        player.moonsTempProgress.reset()
        usedArgs[ROLLED_MOON_UNIQUE] = false
    }
}

// Optional: a variant builder if you want different counts of Moons defeated
val lunarChest = RSDropTable(
    tableIdentifier = "Moons of Peril — Lunar Chest",
    preRoll = moonsUniques,
    mainTable = RsSingleRollable("Moons Standard", moonsStandardDrop)
)

fun Player.randomizeMoons() {
    moonsTempProgress.reset()
    MoonBoss.entries.forEach {
        if (Random.nextBoolean()) {
            moonsTempProgress.defeat(it)
        }
    }
}

fun Player.defeatedMoons(): Set<MoonBoss> = buildSet {
    MoonBoss.entries.forEachIndexed { index, moonBoss ->
        if (moonsTempProgress.defeated[index]) {
            add(moonBoss)
        }
    }
}

fun main() {
    println(moonsUniques.tableEntries)
    while(true) {
        println("uniques rolled: ${examplePlayer.moonsProtection.items.contentDeepToString()}")
        examplePlayer.randomizeMoons()
        println("Rolling moons for ${examplePlayer.username} ${examplePlayer.defeatedMoons()}")
        println(lunarChest.roll(examplePlayer))
        readlnOrNull() ?: break
    }
}
