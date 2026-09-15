package org.rsmod.content.skills.thieving.pack

import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

private const val COINS: String = "obj.coins"

/**
 * One entry of a weighted table. [weight] is the numerator of the wiki's rarity, so a table's
 * weights sum to its denominator.
 */
data class Loot(val obj: String, val amount: IntRange = 1..1, val weight: Int = 1)

/**
 * [low] and [high] are the level-1 and level-99 odds out of 256 fed to the shared skilling success
 * formula, taken from the wiki's own pickpocket charts. [guaranteed] is handed over on every
 * success and [loot] is one weighted roll on top of it.
 *
 * [symbolPrefixes] binds npcs whose cache name is their own rather than the table's - every
 * Prifddinas citizen is an "Elf" target, every Vallessia a "Vyre" one.
 */
data class PickpocketTarget(
    val displayName: String,
    val level: Int,
    val xp: Double,
    val low: Int,
    val high: Int,
    val stunTicks: Int,
    val stunDamage: Int,
    val guaranteed: List<Loot> = emptyList(),
    val loot: List<Loot> = emptyList(),
    val symbolPrefixes: List<String> = emptyList(),
)

data class StallTarget(
    val loc: String,
    val level: Int,
    val xp: Double,
    val loot: List<Loot>,
    val empty: String? = null,
    val respawn: Int = 20,
)

private fun coins(amount: Int) = listOf(Loot(COINS, amount..amount))

/**
 * Pickpocket targets and market stalls.
 *
 * Xp is stored multiplied by ten: the wiki quotes fractional values - a Workman is 10.4 - and the
 * xp column is an int. Content divides by ten once, in ThievingData.
 *
 * A loot column packs four slots per entry, obj/min/max/weight, so one weighted table lives in one
 * column rather than a second table keyed back to this one. Guaranteed drops need no weight and
 * pack three.
 */
object ThievingTables {
    const val PP_NAME = 0
    const val PP_LEVEL = 1
    const val PP_XP = 2
    const val PP_LOW = 3
    const val PP_HIGH = 4
    const val PP_STUN_TICKS = 5
    const val PP_STUN_DAMAGE = 6
    const val PP_GUARANTEED = 7
    const val PP_LOOT = 8
    const val PP_SYMBOL_PREFIXES = 9

    const val STALL_LOC = 0
    const val STALL_LEVEL = 1
    const val STALL_XP = 2
    const val STALL_LOOT = 3
    const val STALL_EMPTY = 4
    const val STALL_RESPAWN = 5

    private fun lootValues(entries: List<Loot>): Array<Any> =
        entries
            .flatMap {
                listOf(
                    ConstantProvider.getMapping(it.obj),
                    it.amount.first,
                    it.amount.last,
                    it.weight,
                )
            }
            .toTypedArray()

    private fun guaranteedValues(entries: List<Loot>): Array<Any> =
        entries
            .flatMap {
                listOf(ConstantProvider.getMapping(it.obj), it.amount.first, it.amount.last)
            }
            .toTypedArray()

    private fun rowName(displayName: String): String =
        "dbrow.thieving_" +
            displayName
                .lowercase()
                .map { if (it.isLetterOrDigit()) it else '_' }
                .joinToString("")
                .replace(Regex("_+"), "_")
                .trim('_')

    fun pickpockets() =
        dbTable("dbtable.thieving_pickpocket", serverOnly = true) {
            column("name", PP_NAME, VarType.STRING)
            column("level", PP_LEVEL, VarType.INT)
            column("xp", PP_XP, VarType.INT)
            column("low", PP_LOW, VarType.INT)
            column("high", PP_HIGH, VarType.INT)
            column("stun_ticks", PP_STUN_TICKS, VarType.INT)
            column("stun_damage", PP_STUN_DAMAGE, VarType.INT)
            column("guaranteed", PP_GUARANTEED, VarType.OBJ, VarType.INT, VarType.INT)
            column("loot", PP_LOOT, VarType.OBJ, VarType.INT, VarType.INT, VarType.INT)
            column("symbol_prefixes", PP_SYMBOL_PREFIXES, VarType.STRING)

            for (target in pickpocketTargets) {
                row(rowName(target.displayName)) {
                    column(PP_NAME, target.displayName)
                    column(PP_LEVEL, target.level)
                    column(PP_XP, (target.xp * 10).toInt())
                    column(PP_LOW, target.low)
                    column(PP_HIGH, target.high)
                    column(PP_STUN_TICKS, target.stunTicks)
                    column(PP_STUN_DAMAGE, target.stunDamage)
                    if (target.guaranteed.isNotEmpty()) {
                        column(PP_GUARANTEED, *guaranteedValues(target.guaranteed))
                    }
                    if (target.loot.isNotEmpty()) {
                        column(PP_LOOT, *lootValues(target.loot))
                    }
                    if (target.symbolPrefixes.isNotEmpty()) {
                        column(PP_SYMBOL_PREFIXES, *target.symbolPrefixes.toTypedArray())
                    }
                }
            }
        }

    fun stalls() =
        dbTable("dbtable.thieving_stall", serverOnly = true) {
            column("loc", STALL_LOC, VarType.LOC)
            column("level", STALL_LEVEL, VarType.INT)
            column("xp", STALL_XP, VarType.INT)
            column("loot", STALL_LOOT, VarType.OBJ, VarType.INT, VarType.INT, VarType.INT)
            column("empty", STALL_EMPTY, VarType.LOC)
            column("respawn", STALL_RESPAWN, VarType.INT)

            for (stall in stallTargets) {
                row("dbrow." + stall.loc.removePrefix("loc.") + "_thieving") {
                    columnRSCM(STALL_LOC, stall.loc)
                    column(STALL_LEVEL, stall.level)
                    column(STALL_XP, (stall.xp * 10).toInt())
                    column(STALL_LOOT, *lootValues(stall.loot))
                    stall.empty?.let { columnRSCM(STALL_EMPTY, it) }
                    column(STALL_RESPAWN, stall.respawn)
                }
            }
        }

    internal val pickpocketTargets: List<PickpocketTarget> =
        listOf(
            PickpocketTarget("Man", 1, 8.0, 180, 240, 8, 1, guaranteed = coins(3)),
            PickpocketTarget("Woman", 1, 8.0, 180, 240, 8, 1, guaranteed = coins(3)),
            PickpocketTarget("Citizen", 1, 8.0, 180, 240, 8, 1, guaranteed = coins(3)),
            PickpocketTarget(
                displayName = "Farmer",
                level = 10,
                xp = 14.5,
                low = 150,
                high = 240,
                stunTicks = 8,
                stunDamage = 1,
                loot =
                    listOf(
                        Loot(COINS, 9..9, weight = 123),
                        Loot("obj.potato_seed", weight = 5),
                    ),
            ),
            hamMember("Male H.A.M. Member"),
            hamMember("Female H.A.M. Member"),
            hamMember("H.A.M. Member"),
            PickpocketTarget("Warrior woman", 25, 26.0, 100, 240, 8, 2, guaranteed = coins(18)),
            PickpocketTarget("Al-Kharid warrior", 25, 26.0, 100, 240, 8, 2, guaranteed = coins(18)),
            PickpocketTarget(
                displayName = "Workman",
                level = 25,
                xp = 10.4,
                low = 150,
                high = 240,
                stunTicks = 7,
                stunDamage = 1,
                loot =
                    listOf(
                        Loot("obj.specimen_brush", weight = 3),
                        Loot("obj.skull", weight = 3),
                        Loot(COINS, 10..10, weight = 1),
                        Loot("obj.rope", weight = 1),
                        Loot("obj.bucket_empty", weight = 1),
                        Loot("obj.leather_gloves", weight = 1),
                        Loot("obj.spade", weight = 1),
                    ),
            ),
            PickpocketTarget("Villager", 30, 8.0, 100, 240, 8, 2, guaranteed = coins(5)),
            PickpocketTarget(
                displayName = "Rogue",
                level = 32,
                xp = 36.5,
                low = 75,
                high = 240,
                stunTicks = 8,
                stunDamage = 2,
                loot =
                    listOf(
                        Loot(COINS, 25..40, weight = 123),
                        Loot("obj.airrune", 8..8, weight = 9),
                        Loot("obj.jug_wine", weight = 6),
                        Loot("obj.lockpick", weight = 5),
                        Loot("obj.iron_dagger_p", weight = 1),
                    ),
            ),
            PickpocketTarget(
                displayName = "Cave goblin",
                level = 36,
                xp = 40.0,
                low = 150,
                high = 240,
                stunTicks = 7,
                stunDamage = 1,
                guaranteed = listOf(Loot(COINS, 10..50)),
            ),
            PickpocketTarget(
                displayName = "Master Farmer",
                level = 38,
                xp = 43.0,
                low = 90,
                high = 240,
                stunTicks = 8,
                stunDamage = 3,
                loot = MASTER_FARMER_SEEDS,
            ),
            PickpocketTarget("Guard", 40, 46.8, 50, 240, 8, 2, guaranteed = coins(30)),
            PickpocketTarget("Fremennik citizen", 45, 65.0, 50, 240, 8, 2, guaranteed = coins(40)),
            PickpocketTarget(
                "Bearded Pollnivnian Bandit",
                45,
                65.0,
                50,
                240,
                8,
                5,
                guaranteed = coins(40),
            ),
            PickpocketTarget("Wealthy citizen", 50, 96.0, 35, 200, 7, 3, guaranteed = coins(85)),
            PickpocketTarget(
                displayName = "Desert Bandit",
                level = 53,
                xp = 79.4,
                low = 50,
                high = 240,
                stunTicks = 8,
                stunDamage = 3,
                loot =
                    listOf(
                        Loot(COINS, 30..30, weight = 5),
                        Loot("obj.1doseantipoison", weight = 1),
                        Loot("obj.lockpick", weight = 1),
                    ),
            ),
            PickpocketTarget("Knight of Ardougne", 55, 84.3, 50, 240, 8, 3, guaranteed = coins(50)),
            PickpocketTarget("Knight of Varlamore", 55, 84.3, 50, 240, 8, 3, guaranteed = coins(50)),
            PickpocketTarget("Pollnivnian Bandit", 55, 84.3, 50, 240, 8, 5, guaranteed = coins(50)),
            PickpocketTarget(
                displayName = "Watchman",
                level = 65,
                xp = 137.5,
                low = 15,
                high = 160,
                stunTicks = 8,
                stunDamage = 3,
                guaranteed = listOf(Loot("obj.bread"), Loot(COINS, 60..60)),
            ),
            PickpocketTarget("Menaphite Thug", 65, 137.5, 50, 160, 8, 5, guaranteed = coins(60)),
            PickpocketTarget(
                displayName = "Paladin",
                level = 70,
                xp = 131.8,
                low = 35,
                high = 160,
                stunTicks = 8,
                stunDamage = 3,
                guaranteed = listOf(Loot(COINS, 80..80), Loot("obj.chaosrune", 2..2)),
            ),
            PickpocketTarget(
                displayName = "Gnome",
                level = 75,
                xp = 133.3,
                low = 33,
                high = 140,
                stunTicks = 8,
                stunDamage = 1,
                loot =
                    listOf(
                        Loot("obj.arrow_shaft", 2..4, weight = 56),
                        Loot(COINS, 300..300, weight = 30),
                        Loot("obj.swamp_toad", weight = 24),
                        Loot("obj.gold_ore", weight = 8),
                        Loot("obj.earthrune", weight = 5),
                        Loot("obj.king_worm", weight = 3),
                        Loot("obj.fire_orb", weight = 2),
                    ),
            ),
            PickpocketTarget(
                displayName = "Hero",
                level = 80,
                xp = 163.3,
                low = 39,
                high = 160,
                stunTicks = 10,
                stunDamage = 3,
                loot =
                    listOf(
                        Loot(COINS, 200..300, weight = 105),
                        Loot("obj.deathrune", 2..2, weight = 8),
                        Loot("obj.jug_wine", weight = 6),
                        Loot("obj.bloodrune", weight = 5),
                        Loot("obj.fire_orb", weight = 2),
                        Loot("obj.diamond", weight = 1),
                        Loot("obj.gold_ore", weight = 1),
                    ),
            ),
            PickpocketTarget(
                displayName = "Vyre",
                level = 82,
                xp = 306.9,
                low = 8,
                high = 128,
                stunTicks = 10,
                stunDamage = 5,
                loot =
                    listOf(
                        Loot(COINS, 230..315, weight = 109),
                        Loot("obj.deathrune", 2..2, weight = 8),
                        Loot("obj.blood_pint", weight = 6),
                        Loot("obj.uncut_ruby", weight = 5),
                        Loot("obj.bloodrune", 4..4, weight = 2),
                        Loot("obj.diamond", weight = 1),
                        Loot("obj.cooked_mystery_meat", weight = 1),
                    ),
                symbolPrefixes = listOf("vallessia_"),
            ),
            PickpocketTarget(
                displayName = "Elf",
                level = 85,
                xp = 353.3,
                low = 6,
                high = 100,
                stunTicks = 10,
                stunDamage = 5,
                loot =
                    listOf(
                        Loot(COINS, 280..350, weight = 105),
                        Loot("obj.deathrune", 2..2, weight = 8),
                        Loot("obj.jug_wine", weight = 6),
                        Loot("obj.naturerune", 3..3, weight = 5),
                        Loot("obj.fire_orb", weight = 2),
                        Loot("obj.diamond", weight = 1),
                        Loot("obj.gold_ore", weight = 1),
                    ),
                symbolPrefixes = listOf("prif_citizen_"),
            ),
            PickpocketTarget(
                displayName = "TzHaar-Hur",
                level = 90,
                xp = 103.4,
                low = -200,
                high = 200,
                stunTicks = 10,
                stunDamage = 4,
                loot =
                    listOf(
                        Loot("obj.tzhaar_token", 3..7, weight = 182),
                        Loot("obj.uncut_sapphire", weight = 5),
                        Loot("obj.uncut_emerald", weight = 4),
                        Loot("obj.uncut_ruby", weight = 3),
                        Loot("obj.uncut_diamond", weight = 1),
                    ),
            ),
        )

    internal val stallTargets: List<StallTarget> =
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
                respawn = 4,
            ),
            StallTarget(
                loc = "loc.tea_stall",
                level = 5,
                xp = 16.0,
                loot = listOf(Loot("obj.cup_of_tea")),
                respawn = 4,
            ),
            StallTarget(
                loc = "loc.silkthiefstall",
                level = 20,
                xp = 24.0,
                loot = listOf(Loot("obj.silk")),
                empty = "loc.market",
                respawn = 8,
            ),
            StallTarget(
                loc = "loc.furthiefstall",
                level = 35,
                xp = 45.0,
                loot = listOf(Loot("obj.grey_wolf_fur")),
                empty = "loc.furmarket",
                respawn = 12,
            ),
            StallTarget(
                loc = "loc.silverthiefstall",
                level = 50,
                xp = 205.0,
                loot = listOf(Loot("obj.silver_ore")),
                empty = "loc.market",
                respawn = 32,
            ),
            StallTarget(
                loc = "loc.spicethiefstall",
                level = 65,
                xp = 92.0,
                loot = listOf(Loot("obj.spicespot")),
                empty = "loc.spicemarket",
                respawn = 10,
            ),
            StallTarget(
                loc = "loc.gemthiefstall",
                level = 75,
                xp = 408.0,
                loot =
                    listOf(
                        Loot("obj.uncut_sapphire", weight = 100),
                        Loot("obj.uncut_emerald", weight = 25),
                        Loot("obj.uncut_ruby", weight = 12),
                        Loot("obj.uncut_diamond", weight = 3),
                    ),
                empty = "loc.gemmarket",
                respawn = 100,
            ),
        )
}

private fun hamMember(displayName: String) =
    PickpocketTarget(
        displayName = displayName,
        level = 15,
        xp = 22.2,
        low = 135,
        high = 239,
        stunTicks = 7,
        stunDamage = 1,
        loot = HAM_MEMBER_LOOT,
    )

/**
 * The wiki quotes most of this table out of 102 and the three herbs out of 561, so every weight is
 * scaled to 1122nds to hold both. The 22 that do not add up are the clue scroll and the empty roll.
 */
private val HAM_MEMBER_LOOT =
    listOf(
        Loot(COINS, 1..21, weight = 187),
        Loot("obj.digsitebuttons", weight = 44),
        Loot("obj.digsitearmour1", weight = 44),
        Loot("obj.digsitesword", weight = 44),
        Loot("obj.bronze_arrow", 1..13, weight = 33),
        Loot("obj.bronze_axe", weight = 33),
        Loot("obj.bronze_dagger", weight = 33),
        Loot("obj.bronze_pickaxe", weight = 33),
        Loot("obj.iron_axe", weight = 33),
        Loot("obj.iron_dagger", weight = 33),
        Loot("obj.iron_pickaxe", weight = 33),
        Loot("obj.leather_armour", weight = 33),
        Loot("obj.feather", 1..7, weight = 33),
        Loot("obj.logs", weight = 33),
        Loot("obj.thread", 1..10, weight = 33),
        Loot("obj.cow_hide", weight = 33),
        Loot("obj.steel_arrow", 1..13, weight = 22),
        Loot("obj.steel_axe", weight = 22),
        Loot("obj.steel_dagger", weight = 22),
        Loot("obj.steel_pickaxe", weight = 22),
        Loot("obj.knife", weight = 22),
        Loot("obj.needle", weight = 22),
        Loot("obj.raw_anchovies", weight = 22),
        Loot("obj.raw_chicken", weight = 22),
        Loot("obj.tinderbox", weight = 22),
        Loot("obj.uncut_opal", weight = 22),
        Loot("obj.uncut_jade", weight = 22),
        Loot("obj.coal", weight = 22),
        Loot("obj.iron_ore", weight = 22),
        Loot("obj.ham_boots", weight = 11),
        Loot("obj.ham_cloak", weight = 11),
        Loot("obj.ham_gloves", weight = 11),
        Loot("obj.ham_hood", weight = 11),
        Loot("obj.ham_badge", weight = 11),
        Loot("obj.ham_robe", weight = 11),
        Loot("obj.ham_shirt", weight = 11),
        Loot("obj.unidentified_guam", weight = 12),
        Loot("obj.unidentified_marentill", weight = 6),
        Loot("obj.unidentified_tarromin", weight = 4),
    )

/**
 * Weights are the wiki's 1/x seed rarities scaled to 100000ths, which is the resolution the tail
 * needs: torstol is 1/9272 and dwarf weed 1/6944. The rates are the base ones - live scales the
 * three highest herbs with Farming level, which nothing here models.
 */
private val MASTER_FARMER_SEEDS =
    listOf(
        Loot("obj.potato_seed", 1..4, weight = 17699),
        Loot("obj.onion_seed", 1..3, weight = 13280),
        Loot("obj.cabbage_seed", 1..3, weight = 6944),
        Loot("obj.tomato_seed", 1..2, weight = 6369),
        Loot("obj.sweetcorn_seed", 1..2, weight = 2212),
        Loot("obj.strawberry_seed", weight = 1106),
        Loot("obj.watermelon_seed", weight = 529),
        Loot("obj.snape_grass_seed", weight = 385),
        Loot("obj.barley_seed", 1..12, weight = 5556),
        Loot("obj.hammerstone_hop_seed", 1..9, weight = 5556),
        Loot("obj.asgarnian_hop_seed", 1..6, weight = 4184),
        Loot("obj.jute_seed", 1..9, weight = 4149),
        Loot("obj.yanillian_hop_seed", 1..6, weight = 2770),
        Loot("obj.krandorian_hop_seed", 1..6, weight = 1385),
        Loot("obj.wildblood_hop_seed", 1..3, weight = 704),
        Loot("obj.marigold_seed", weight = 4587),
        Loot("obj.nasturtium_seed", weight = 3040),
        Loot("obj.rosemary_seed", weight = 1965),
        Loot("obj.woad_seed", weight = 1451),
        Loot("obj.limpwurt_seed", weight = 1159),
        Loot("obj.redberry_bush_seed", weight = 3876),
        Loot("obj.cadavaberry_bush_seed", weight = 2717),
        Loot("obj.dwellberry_bush_seed", weight = 1942),
        Loot("obj.jangerberry_bush_seed", weight = 775),
        Loot("obj.whiteberry_bush_seed", weight = 282),
        Loot("obj.poisonivy_bush_seed", weight = 107),
        Loot("obj.mushroom_seed", weight = 203),
        Loot("obj.belladonna_seed", weight = 122),
        Loot("obj.cactus_seed", weight = 81),
        Loot("obj.seaweed_seed", weight = 53),
        Loot("obj.potato_cactus_seed", weight = 41),
        Loot("obj.guam_seed", weight = 1714),
        Loot("obj.marrentill_seed", weight = 1046),
        Loot("obj.tarromin_seed", weight = 714),
        Loot("obj.harralander_seed", weight = 485),
        Loot("obj.ranarr_seed", weight = 372),
        Loot("obj.toadflax_seed", weight = 226),
        Loot("obj.irit_seed", weight = 154),
        Loot("obj.avantoe_seed", weight = 106),
        Loot("obj.kwuarm_seed", weight = 72),
        Loot("obj.snapdragon_seed", weight = 54),
        Loot("obj.cadantine_seed", weight = 34),
        Loot("obj.lantadyme_seed", weight = 24),
        Loot("obj.dwarf_weed_seed", weight = 14),
        Loot("obj.torstol_seed", weight = 11),
    )
