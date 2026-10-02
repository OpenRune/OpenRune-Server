package org.rsmod.content.skills.thieving.pack

import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

private const val COINS: String = "obj.coins"
private const val DEFAULT_SHOUT: String = "What do you think you're doing?"

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
 * Prifddinas citizen is an "Elf" target, every named Darkmeyer resident a "Vyre", every named
 * Rellekka citizen a "Fremennik citizen", and the "Bandit"s of Pollnivneach and the Bandit Camp
 * are told apart by symbol.
 *
 * With a [pouch], every coins entry is handed over as one of that pouch instead, holding the
 * entry's amount when opened.
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
    val pouch: String? = null,
    val caughtShout: String = DEFAULT_SHOUT,
    val lowercaseName: Boolean = true,
)

/** Stealing is refused when one of [owners] or [guards] can see the player; a guard attacks. */
data class StallTarget(
    val loc: String,
    val level: Int,
    val xp: Double,
    val loot: List<Loot>,
    val empty: String? = null,
    val respawn: Int = 20,
    val owners: List<String> = emptyList(),
    val guards: List<String> = emptyList(),
    val attemptMessage: String? = null,
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
    const val PP_POUCH = 10
    const val PP_CAUGHT_SHOUT = 11
    const val PP_LOWERCASE_NAME = 12

    const val STALL_LOC = 0
    const val STALL_LEVEL = 1
    const val STALL_XP = 2
    const val STALL_LOOT = 3
    const val STALL_EMPTY = 4
    const val STALL_RESPAWN = 5
    const val STALL_OWNERS = 6
    const val STALL_GUARDS = 7
    const val STALL_ATTEMPT_MESSAGE = 8

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
            column("pouch", PP_POUCH, VarType.OBJ)
            column("caught_shout", PP_CAUGHT_SHOUT, VarType.STRING)
            column("lowercase_name", PP_LOWERCASE_NAME, VarType.BOOLEAN)

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
                    target.pouch?.let { columnRSCM(PP_POUCH, it) }
                    column(PP_CAUGHT_SHOUT, target.caughtShout)
                    column(PP_LOWERCASE_NAME, target.lowercaseName)
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
            column("owners", STALL_OWNERS, VarType.NPC)
            column("guards", STALL_GUARDS, VarType.NPC)
            column("attempt_message", STALL_ATTEMPT_MESSAGE, VarType.STRING)

            for (stall in stallTargets) {
                row("dbrow." + stall.loc.removePrefix("loc.") + "_thieving") {
                    columnRSCM(STALL_LOC, stall.loc)
                    column(STALL_LEVEL, stall.level)
                    column(STALL_XP, (stall.xp * 10).toInt())
                    column(STALL_LOOT, *lootValues(stall.loot))
                    stall.empty?.let { columnRSCM(STALL_EMPTY, it) }
                    column(STALL_RESPAWN, stall.respawn)
                    if (stall.owners.isNotEmpty()) {
                        columnRSCM(STALL_OWNERS, *stall.owners.toTypedArray())
                    }
                    if (stall.guards.isNotEmpty()) {
                        columnRSCM(STALL_GUARDS, *stall.guards.toTypedArray())
                    }
                    stall.attemptMessage?.let { column(STALL_ATTEMPT_MESSAGE, it) }
                }
            }
        }

    internal val pickpocketTargets: List<PickpocketTarget> =
        listOf(
            PickpocketTarget(
                "Man",
                1,
                8.0,
                180,
                240,
                8,
                1,
                guaranteed = coins(3),
                pouch = "obj.pickpocket_coin_pouch_citizen",
                symbolPrefixes =
                    listOf(
                        "falador_doric_area_man",
                        "falador_man",
                        "rimmington_hengel",
                    ),
            ),
            PickpocketTarget(
                "Woman",
                1,
                8.0,
                180,
                240,
                8,
                1,
                guaranteed = coins(3),
                pouch = "obj.pickpocket_coin_pouch_citizen",
                symbolPrefixes =
                    listOf(
                        "rimmington_anja",
                    ),
            ),
            PickpocketTarget(
                "Citizen",
                1,
                8.0,
                180,
                240,
                8,
                1,
                guaranteed = coins(3),
                pouch = "obj.pickpocket_coin_pouch_citizen",
            ),
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
                pouch = "obj.pickpocket_coin_pouch_farmer",
            ),
            hamMember("Male H.A.M. Member"),
            hamMember("Female H.A.M. Member"),
            hamMember("H.A.M. Member"),
            PickpocketTarget(
                "Warrior woman",
                25,
                26.0,
                100,
                240,
                8,
                2,
                guaranteed = coins(18),
                pouch = "obj.pickpocket_coin_pouch_warrior",
            ),
            PickpocketTarget(
                "Warrior",
                25,
                26.0,
                100,
                240,
                8,
                2,
                guaranteed = coins(18),
                pouch = "obj.pickpocket_coin_pouch_warrior",
                symbolPrefixes = listOf("al_kharid_warrior"),
            ),
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
                pouch = "obj.pickpocket_coin_pouch_rogue",
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
                pouch = "obj.pickpocket_coin_pouch_cavegoblin",
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
                lowercaseName = false,
                caughtShout = "Cor blimey mate, what are ye doing in me pockets?",
                symbolPrefixes = listOf("martin_the_master_farmer"),
            ),
            PickpocketTarget(
                "Guard",
                40,
                46.8,
                50,
                240,
                8,
                2,
                guaranteed = coins(30),
                pouch = "obj.pickpocket_coin_pouch_guard",
                symbolPrefixes =
                    listOf(
                        "kourend_guard_",
                    ),
            ),
            PickpocketTarget(
                "Fremennik citizen",
                45,
                65.0,
                50,
                240,
                8,
                2,
                guaranteed = coins(40),
                pouch = "obj.pickpocket_coin_pouch_fremennik",
                symbolPrefixes =
                    listOf(
                        "viking_man",
                        "viking_woman",
                    ),
            ),
            PickpocketTarget(
                "Bearded Pollnivnian Bandit",
                45,
                65.0,
                50,
                240,
                8,
                5,
                guaranteed = coins(40),
                pouch = "obj.pickpocket_coin_pouch_bandit2",
                lowercaseName = false,
                symbolPrefixes =
                    listOf(
                        "feud_arabian_guard2_",
                    ),
            ),
            PickpocketTarget(
                "Wealthy citizen",
                50,
                96.0,
                35,
                200,
                7,
                3,
                guaranteed = coins(85),
                pouch = "obj.pickpocket_coin_pouch_varlamore_wealthy",
            ),
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
                pouch = "obj.pickpocket_coin_pouch_desertbandit",
                lowercaseName = false,
                symbolPrefixes =
                    listOf(
                        "fourdiamonds_sword_bandit",
                    ),
            ),
            PickpocketTarget(
                "Knight of Ardougne",
                55,
                84.3,
                50,
                240,
                8,
                3,
                guaranteed = coins(50),
                pouch = "obj.pickpocket_coin_pouch_knight",
                lowercaseName = false,
            ),
            PickpocketTarget(
                "Knight of Varlamore",
                55,
                84.3,
                50,
                240,
                8,
                3,
                guaranteed = coins(50),
                pouch = "obj.pickpocket_coin_pouch_knight",
                lowercaseName = false,
            ),
            PickpocketTarget(
                "Pollnivnian Bandit",
                55,
                84.3,
                50,
                240,
                8,
                5,
                guaranteed = coins(50),
                pouch = "obj.pickpocket_coin_pouch_bandit",
                lowercaseName = false,
                symbolPrefixes =
                    listOf(
                        "feud_arabian_guard1_",
                    ),
            ),
            PickpocketTarget(
                displayName = "Watchman",
                level = 65,
                xp = 137.5,
                low = 15,
                high = 160,
                stunTicks = 8,
                stunDamage = 3,
                guaranteed = listOf(Loot("obj.bread"), Loot(COINS, 60..60)),
                pouch = "obj.pickpocket_coin_pouch_watchman",
            ),
            PickpocketTarget(
                "Menaphite Thug",
                65,
                137.5,
                50,
                160,
                8,
                5,
                guaranteed = coins(60),
                pouch = "obj.pickpocket_coin_pouch_menaphite",
                lowercaseName = false,
            ),
            PickpocketTarget(
                displayName = "Paladin",
                level = 70,
                xp = 131.8,
                low = 35,
                high = 160,
                stunTicks = 8,
                stunDamage = 3,
                guaranteed = listOf(Loot(COINS, 80..80), Loot("obj.chaosrune", 2..2)),
                pouch = "obj.pickpocket_coin_pouch_paladin",
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
                pouch = "obj.pickpocket_coin_pouch_gnome",
                symbolPrefixes =
                    listOf(
                        "gnomechild",
                        "gnomefemale",
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
                pouch = "obj.pickpocket_coin_pouch_hero",
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
                symbolPrefixes =
                    listOf(
                        "vallessia_",
                        "alek_constantine",
                        "caninelle_draynar",
                        "carnivus_belamorta",
                        "crimsonette_van_marr",
                        "diphylla_bechstein",
                        "draconis_sanguine",
                        "episcula_helsing",
                        "grigor_rasputin",
                        "haemas_lamescus",
                        "lasenna_rasputin",
                        "misdrievus_shadum",
                        "mort_nightshade",
                        "mortina_daubenton",
                        "nakasa_jovkai",
                        "natalidae_shadum",
                        "noctillion_lugosi",
                        "pipistrelle_draynar",
                        "remus_kaninus",
                        "valentin_rasputin",
                        "valentina_diaemus",
                        "vampyressa_van_von",
                        "vampyrus_diaemus",
                        "violetta_sanguine",
                        "vlad_bechstein",
                        "vlad_diaemus",
                        "von_van_von",
                        "vonnetta_varnis",
                        "vormar_vakan",
                    ),
                pouch = "obj.pickpocket_coin_pouch_vyre",
                lowercaseName = false,
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
                pouch = "obj.pickpocket_coin_pouch_elf",
                lowercaseName = false,
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
                lowercaseName = false,
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
                owners = BAKERS,
                guards = ARDOUGNE_MARKET_GUARDS,
            ),
            StallTarget(
                loc = "loc.tea_stall",
                level = 5,
                xp = 16.0,
                loot = listOf(Loot("obj.cup_of_tea")),
                respawn = 4,
                owners = listOf("npc.tea_seller"),
            ),
            StallTarget(
                loc = "loc.silkthiefstall",
                level = 20,
                xp = 24.0,
                loot = listOf(Loot("obj.silk")),
                empty = "loc.market",
                respawn = 8,
                owners = listOf("npc.silk_merchant_ardougne", "npc.silk_merchant"),
                guards = ARDOUGNE_MARKET_GUARDS,
            ),
            StallTarget(
                loc = "loc.rag_market_stall",
                level = 22,
                xp = 27.0,
                loot =
                    listOf(
                        Loot("obj.jug_empty", weight = 39),
                        Loot("obj.jug_water", weight = 20),
                        Loot("obj.grapes", weight = 17),
                        Loot("obj.jug_wine", weight = 13),
                        Loot("obj.rag_bottle_wine", weight = 11),
                    ),
                empty = "loc.rag_market_stall_empty",
                respawn = 8,
                owners = listOf("npc.rag_wine_merchant"),
                guards = DRAYNOR_MARKET_GUARDS,
                attemptMessage = "You attempt to steal something from the wine merchant's stall.",
            ),
            StallTarget(
                loc = "loc.seed_stall",
                level = 27,
                xp = 10.0,
                loot = DRAYNOR_SEED_STALL_LOOT,
                respawn = 5,
                owners = listOf("npc.seed_merchant"),
                guards = DRAYNOR_MARKET_GUARDS,
                attemptMessage = "You attempt to steal some seeds from the seed merchant's stall.",
            ),
            StallTarget(
                loc = "loc.furthiefstall",
                level = 35,
                xp = 45.0,
                loot = listOf(Loot("obj.grey_wolf_fur")),
                empty = "loc.furmarket",
                respawn = 12,
                owners = listOf("npc.fur_merchant_ardougne", "npc.fur_merchant"),
                guards = ARDOUGNE_MARKET_GUARDS,
            ),
            StallTarget(
                loc = "loc.silverthiefstall",
                level = 50,
                xp = 205.0,
                loot = listOf(Loot("obj.silver_ore")),
                empty = "loc.market",
                respawn = 32,
                owners = listOf("npc.silver_merchant_ardougne"),
                guards = ARDOUGNE_MARKET_GUARDS,
            ),
            StallTarget(
                loc = "loc.spicethiefstall",
                level = 65,
                xp = 92.0,
                loot = listOf(Loot("obj.spicespot")),
                empty = "loc.spicemarket",
                respawn = 10,
                owners = listOf("npc.spice_merchant_ardougne", "npc.spice_merchant"),
                guards = ARDOUGNE_MARKET_GUARDS,
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
                owners = listOf("npc.gem_merchant_ardougne", "npc.gem_merchant"),
                guards = ARDOUGNE_MARKET_GUARDS,
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
        pouch = "obj.pickpocket_coin_pouch_ham",
        lowercaseName = false,
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

private val BAKERS =
    listOf("npc.baker_merchant_ardougne", "npc.baker_merchant_ardougne2", "npc.baker_merchant")

private val ARDOUGNE_MARKET_GUARDS =
    listOf(
        "npc.ardougne_guard",
        "npc.ardougne_guard_variant01",
        "npc.ardougne_guard_f",
        "npc.ardougne_guard_f_variant01",
        "npc.knight_of_ardougne",
        "npc.knight_of_ardougne_f",
        "npc.paladin2",
        "npc.paladin_f_variant01",
    )

private val DRAYNOR_MARKET_GUARDS = listOf("npc.farming_market_guard")

private val DRAYNOR_SEED_STALL_LOOT =
    listOf(
        Loot("obj.hammerstone_hop_seed", weight = 120),
        Loot("obj.potato_seed", weight = 119),
        Loot("obj.marigold_seed", weight = 119),
        Loot("obj.barley_seed", weight = 118),
        Loot("obj.onion_seed", weight = 89),
        Loot("obj.asgarnian_hop_seed", weight = 83),
        Loot("obj.cabbage_seed", weight = 71),
        Loot("obj.yanillian_hop_seed", weight = 47),
        Loot("obj.rosemary_seed", weight = 36),
        Loot("obj.nasturtium_seed", weight = 35),
        Loot("obj.tomato_seed", weight = 35),
        Loot("obj.jute_seed", weight = 35),
        Loot("obj.sweetcorn_seed", weight = 30),
        Loot("obj.krandorian_hop_seed", weight = 24),
        Loot("obj.strawberry_seed", weight = 18),
        Loot("obj.wildblood_hop_seed", weight = 12),
        Loot("obj.watermelon_seed", weight = 9),
    )
