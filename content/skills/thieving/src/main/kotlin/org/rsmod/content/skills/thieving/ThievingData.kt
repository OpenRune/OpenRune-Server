package org.rsmod.content.skills.thieving

import org.rsmod.api.random.GameRandom

internal const val STAT_THIEVING: String = "stat.thieving"
internal const val ANIM_PICKPOCKET: String = "seq.human_pickpocket"
internal const val ANIM_STEAL_STALL: String = "seq.human_pickuptable"
internal const val ANIM_STUNNED: String = "seq.stunned_thieving"
internal const val SPOTANIM_STUNNED: String = "spotanim.stunned_thieving"
internal const val COINS: String = "obj.coins"

data class Loot(val obj: String, val amount: IntRange = 1..1, val weight: Int = 1)

/**
 * [low] and [high] are the level-1 and level-99 odds out of 256 fed to the shared skilling success
 * formula. They are approximations of the live rates and are the knob to turn if a target feels
 * too generous or too punishing; everything else here is wiki data.
 */
data class PickpocketTarget(
    val displayName: String,
    val level: Int,
    val xp: Double,
    val low: Int,
    val high: Int,
    val stunTicks: Int,
    val stunDamage: Int,
    val loot: List<Loot>,
)

data class StallTarget(
    val loc: String,
    val level: Int,
    val xp: Double,
    val loot: List<Loot>,
    val empty: String? = null,
    val respawn: Int = 20,
)

fun List<Loot>.roll(random: GameRandom): Pair<String, Int> {
    val total = sumOf(Loot::weight)
    var roll = random.of(1, total)
    for (entry in this) {
        roll -= entry.weight
        if (roll <= 0) {
            return entry.obj to random.of(entry.amount.first, entry.amount.last)
        }
    }
    val fallback = last()
    return fallback.obj to random.of(fallback.amount.first, fallback.amount.last)
}

private fun coins(amount: Int) = listOf(Loot(COINS, amount..amount))

private fun target(
    displayName: String,
    level: Int,
    xp: Double,
    low: Int,
    high: Int,
    stunTicks: Int = 4,
    stunDamage: Int = 1,
    loot: List<Loot>,
) = PickpocketTarget(displayName, level, xp, low, high, stunTicks, stunDamage, loot)

object ThievingData {
    // TODO: unique loot tables (Master Farmer seeds, Hero gems/runes, Ardougne knight caskets).
    // Coins are the shared bulk drop and keep every target usable until those land.
    val pickpocketTargets: List<PickpocketTarget> =
        listOf(
            target("Man", 1, 8.0, 240, 255, loot = coins(3)),
            target("Woman", 1, 8.0, 240, 255, loot = coins(3)),
            target("Farmer", 10, 14.5, 190, 255, loot = coins(9)),
            target("Female H.A.M. Member", 15, 18.5, 175, 255, loot = coins(5)),
            target("Male H.A.M. Member", 20, 22.5, 170, 255, loot = coins(5)),
            target("H.A.M. Member", 20, 22.5, 170, 255, loot = coins(5)),
            target("Warrior woman", 25, 26.0, 155, 255, stunDamage = 2, loot = coins(18)),
            target("Al-Kharid warrior", 25, 26.0, 155, 255, stunDamage = 2, loot = coins(18)),
            target(
                displayName = "Rogue",
                level = 32,
                xp = 35.5,
                low = 160,
                high = 255,
                stunDamage = 2,
                loot =
                    listOf(
                        Loot(COINS, 25..40, weight = 6),
                        Loot("obj.lockpick", weight = 2),
                        Loot("obj.jug_wine", weight = 1),
                        Loot("obj.iron_dagger", weight = 1),
                    ),
            ),
            target("Cave goblin", 36, 40.0, 150, 255, stunDamage = 2, loot = coins(20)),
            target("Master Farmer", 38, 43.0, 110, 255, stunDamage = 3, loot = coins(20)),
            target("Guard", 40, 46.8, 147, 255, stunDamage = 2, loot = coins(30)),
            target("Fremennik citizen", 45, 65.0, 145, 255, stunDamage = 2, loot = coins(40)),
            target(
                "Bearded Pollnivnian Bandit",
                45,
                65.0,
                145,
                255,
                stunDamage = 3,
                loot = coins(40),
            ),
            target("Desert Bandit", 53, 79.5, 140, 255, stunDamage = 3, loot = coins(30)),
            target("Knight of Ardougne", 55, 84.3, 135, 255, stunDamage = 3, loot = coins(50)),
            target("Pollnivnian Bandit", 55, 84.3, 135, 255, stunDamage = 3, loot = coins(50)),
            target("Yanille Watchman", 65, 137.5, 130, 255, stunDamage = 3, loot = coins(60)),
            target("Menaphite Thug", 65, 137.5, 130, 255, stunDamage = 3, loot = coins(60)),
            target("Paladin", 70, 151.75, 120, 255, stunDamage = 4, loot = coins(80)),
            target("Gnome", 75, 198.5, 110, 255, stunDamage = 3, loot = coins(300)),
            target("Hero", 80, 273.3, 100, 255, stunDamage = 4, loot = coins(200)),
            target("Vyre", 82, 306.9, 95, 255, stunTicks = 5, stunDamage = 4, loot = coins(200)),
            target("Elf", 85, 353.0, 90, 255, stunTicks = 5, stunDamage = 5, loot = coins(280)),
            target(
                displayName = "TzHaar-Hur",
                level = 90,
                xp = 103.4,
                low = 80,
                high = 255,
                stunTicks = 5,
                stunDamage = 6,
                loot = listOf(Loot("obj.tzhaar_token", 8..20)),
            ),
        )

    val stalls: List<StallTarget> =
        listOf(
            StallTarget(
                loc = "loc.cakethiefstall",
                level = 5,
                xp = 16.0,
                loot =
                    listOf(
                        Loot("obj.cake", weight = 6),
                        Loot("obj.bread", weight = 3),
                        Loot("obj.chocolate_slice", weight = 1),
                    ),
                empty = "loc.bakerymarket",
                respawn = 18,
            ),
            StallTarget(
                loc = "loc.tea_stall",
                level = 5,
                xp = 16.0,
                loot = listOf(Loot("obj.cup_of_tea")),
                respawn = 18,
            ),
            StallTarget(
                loc = "loc.silkthiefstall",
                level = 20,
                xp = 24.0,
                loot = listOf(Loot("obj.silk")),
                empty = "loc.market",
                respawn = 31,
            ),
            StallTarget(
                loc = "loc.furthiefstall",
                level = 35,
                xp = 36.0,
                loot = listOf(Loot("obj.grey_wolf_fur")),
                empty = "loc.furmarket",
                respawn = 31,
            ),
            StallTarget(
                loc = "loc.silverthiefstall",
                level = 50,
                xp = 54.0,
                loot = listOf(Loot("obj.silver_ore")),
                empty = "loc.market",
                respawn = 62,
            ),
            StallTarget(
                loc = "loc.spicethiefstall",
                level = 65,
                xp = 81.0,
                loot = listOf(Loot("obj.spicespot")),
                empty = "loc.spicemarket",
                respawn = 132,
            ),
            StallTarget(
                loc = "loc.gemthiefstall",
                level = 75,
                xp = 160.0,
                loot =
                    listOf(
                        Loot("obj.uncut_sapphire", weight = 100),
                        Loot("obj.uncut_emerald", weight = 25),
                        Loot("obj.uncut_ruby", weight = 12),
                        Loot("obj.uncut_diamond", weight = 3),
                    ),
                empty = "loc.gemmarket",
                respawn = 167,
            ),
        )
}
