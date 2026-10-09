package org.rsmod.content.skills.thieving.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

private const val DEFAULT_SHOUT: String = "What do you think you're doing?"

/**
 * [low] and [high] are the level-1 and level-99 odds out of 256 fed to the shared skilling success
 * formula, taken from the wiki's own pickpocket charts.
 *
 * [symbolPrefixes] binds npcs whose cache name is their own rather than the table's - every
 * Prifddinas citizen is an "Elf" target, every named Darkmeyer resident a "Vyre", every named
 * Rellekka citizen a "Fremennik citizen", and the "Bandit"s of Pollnivneach and the Bandit Camp
 * are told apart by symbol.
 */
data class PickpocketTarget(
    val displayName: String,
    val level: Int,
    val xp: Double,
    val low: Int,
    val high: Int,
    val stunTicks: Int,
    val stunDamage: Int,
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
    val empty: String? = null,
    val respawn: Int = 20,
    val owners: List<String> = emptyList(),
    val guards: List<String> = emptyList(),
    val attemptMessage: String? = null,
)

data class CoinPouch(val obj: String, val coins: IntRange)

/**
 * Pickpocket targets, market stalls and the coin pouches pickpocketing hands out. Loot lives in
 * the thieving module's drop tables, keyed by these rows.
 *
 * Xp is stored multiplied by ten: the wiki quotes fractional values - a Workman is 10.4 - and the
 * xp column is an int. ThievingScript divides by ten when awarding it.
 */
object ThievingTables {
    const val PP_NAME = 0
    const val PP_LEVEL = 1
    const val PP_XP = 2
    const val PP_LOW = 3
    const val PP_HIGH = 4
    const val PP_STUN_TICKS = 5
    const val PP_STUN_DAMAGE = 6
    const val PP_SYMBOL_PREFIXES = 7
    const val PP_POUCH = 8
    const val PP_CAUGHT_SHOUT = 9
    const val PP_LOWERCASE_NAME = 10

    const val STALL_LOC = 0
    const val STALL_LEVEL = 1
    const val STALL_XP = 2
    const val STALL_EMPTY = 3
    const val STALL_RESPAWN = 4
    const val STALL_OWNERS = 5
    const val STALL_GUARDS = 6
    const val STALL_ATTEMPT_MESSAGE = 7

    const val POUCH_OBJ = 0
    const val POUCH_COINS_MIN = 1
    const val POUCH_COINS_MAX = 2

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

    fun coinPouches() =
        dbTable("dbtable.thieving_coin_pouch", serverOnly = true) {
            column("obj", POUCH_OBJ, VarType.OBJ)
            column("coins_min", POUCH_COINS_MIN, VarType.INT)
            column("coins_max", POUCH_COINS_MAX, VarType.INT)

            for (pouch in coinPouches) {
                row("dbrow.thieving_" + pouch.obj.removePrefix("obj.pickpocket_")) {
                    columnRSCM(POUCH_OBJ, pouch.obj)
                    column(POUCH_COINS_MIN, pouch.coins.first)
                    column(POUCH_COINS_MAX, pouch.coins.last)
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
            ),
            PickpocketTarget("Villager", 30, 8.0, 100, 240, 8, 2),
            PickpocketTarget(
                displayName = "Rogue",
                level = 32,
                xp = 36.5,
                low = 75,
                high = 240,
                stunTicks = 8,
                stunDamage = 2,
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
                lowercaseName = false,
            ),
        )

    internal val stallTargets: List<StallTarget> =
        listOf(
            StallTarget(
                loc = "loc.cakethiefstall",
                level = 5,
                xp = 16.0,
                empty = "loc.bakerymarket",
                respawn = 4,
                owners = BAKERS,
                guards = ARDOUGNE_MARKET_GUARDS,
            ),
            StallTarget(
                loc = "loc.tea_stall",
                level = 5,
                xp = 16.0,
                respawn = 4,
                owners = listOf("npc.tea_seller"),
            ),
            StallTarget(
                loc = "loc.silkthiefstall",
                level = 20,
                xp = 24.0,
                empty = "loc.market",
                respawn = 8,
                owners = listOf("npc.silk_merchant_ardougne", "npc.silk_merchant"),
                guards = ARDOUGNE_MARKET_GUARDS,
            ),
            StallTarget(
                loc = "loc.rag_market_stall",
                level = 22,
                xp = 27.0,
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
                respawn = 5,
                owners = listOf("npc.seed_merchant"),
                guards = DRAYNOR_MARKET_GUARDS,
                attemptMessage = "You attempt to steal some seeds from the seed merchant's stall.",
            ),
            StallTarget(
                loc = "loc.furthiefstall",
                level = 35,
                xp = 45.0,
                empty = "loc.furmarket",
                respawn = 12,
                owners = listOf("npc.fur_merchant_ardougne", "npc.fur_merchant"),
                guards = ARDOUGNE_MARKET_GUARDS,
            ),
            StallTarget(
                loc = "loc.silverthiefstall",
                level = 50,
                xp = 205.0,
                empty = "loc.market",
                respawn = 32,
                owners = listOf("npc.silver_merchant_ardougne"),
                guards = ARDOUGNE_MARKET_GUARDS,
            ),
            StallTarget(
                loc = "loc.spicethiefstall",
                level = 65,
                xp = 92.0,
                empty = "loc.spicemarket",
                respawn = 10,
                owners = listOf("npc.spice_merchant_ardougne", "npc.spice_merchant"),
                guards = ARDOUGNE_MARKET_GUARDS,
            ),
            StallTarget(
                loc = "loc.gemthiefstall",
                level = 75,
                xp = 408.0,
                empty = "loc.gemmarket",
                respawn = 100,
                owners = listOf("npc.gem_merchant_ardougne", "npc.gem_merchant"),
                guards = ARDOUGNE_MARKET_GUARDS,
            ),
            StallTarget(
                loc = "loc.prif_marketstall_silk",
                level = 20,
                xp = 24.0,
                empty = "loc.prif_marketstall_empty",
                respawn = 8,
                owners = listOf("npc.prif_silk"),
                guards = PRIFDDINAS_GUARDS,
            ),
            StallTarget(
                loc = "loc.prif_marketstall_silver",
                level = 50,
                xp = 205.0,
                empty = "loc.prif_marketstall_empty",
                respawn = 32,
                owners = listOf("npc.prif_silver"),
                guards = PRIFDDINAS_GUARDS,
            ),
            StallTarget(
                loc = "loc.prif_marketstall_spice",
                level = 65,
                xp = 92.0,
                empty = "loc.prif_marketstall_empty",
                respawn = 10,
                owners = listOf("npc.prif_spice"),
                guards = PRIFDDINAS_GUARDS,
            ),
            StallTarget(
                loc = "loc.prif_marketstall_gem",
                level = 75,
                xp = 408.0,
                empty = "loc.prif_marketstall_empty",
                respawn = 100,
                owners = listOf("npc.prif_gem"),
                guards = PRIFDDINAS_GUARDS,
            ),
            StallTarget(
                loc = "loc.viking_fish_market",
                level = 42,
                xp = 42.0,
                empty = "loc.viking_market",
                respawn = 12,
                owners = listOf("npc.viking_fish_monger"),
                guards = listOf("npc.viking_guard"),
            ),
            StallTarget(
                loc = "loc.viking_fur_market",
                level = 35,
                xp = 45.0,
                empty = "loc.viking_market",
                respawn = 12,
                owners = listOf("npc.viking_fur_monger"),
                guards = listOf("npc.viking_guard"),
            ),
            StallTarget(
                loc = "loc.misc_fish_market",
                level = 42,
                xp = 42.0,
                respawn = 12,
                owners = listOf("npc.misc_fish_monger"),
                guards = listOf("npc.royal_misc_guard"),
            ),
            StallTarget(
                loc = "loc.misc_veg_market",
                level = 2,
                xp = 10.0,
                respawn = 2,
                owners = listOf("npc.misc_veg_monger"),
                guards = listOf("npc.royal_misc_guard"),
            ),
            StallTarget(
                loc = "loc.etc_fish_market",
                level = 42,
                xp = 42.0,
                respawn = 12,
                owners = listOf("npc.etc_fish_monger"),
                guards = ETCETERIA_GUARDS,
            ),
            StallTarget(
                loc = "loc.etc_veg_market",
                level = 2,
                xp = 10.0,
                respawn = 2,
                owners = listOf("npc.etc_veg_monger"),
                guards = ETCETERIA_GUARDS,
            ),
            StallTarget(
                loc = "loc.dwarf_market_bakery",
                level = 5,
                xp = 16.0,
                empty = "loc.dwarf_market_empty_stall",
                respawn = 16,
            ),
            StallTarget(
                loc = "loc.dwarf_market_crafting",
                level = 5,
                xp = 20.0,
                empty = "loc.dwarf_market_empty_stall",
                respawn = 8,
            ),
            StallTarget(
                loc = "loc.xbows_dwarf_market",
                level = 49,
                xp = 52.0,
                respawn = 8,
            ),
            StallTarget(
                loc = "loc.dwarf_market_silver",
                level = 50,
                xp = 205.0,
                empty = "loc.dwarf_market_empty_stall",
                respawn = 32,
            ),
            StallTarget(
                loc = "loc.dwarf_market_gems",
                level = 75,
                xp = 408.0,
                empty = "loc.dwarf_market_empty_stall",
                respawn = 100,
            ),
            StallTarget(
                loc = "loc.hos_stall_bread",
                level = 5,
                xp = 16.0,
                empty = "loc.hos_stall_empty",
                respawn = 4,
            ),
            StallTarget(
                loc = "loc.hos_fruit_stall_02",
                level = 25,
                xp = 28.5,
                empty = "loc.hos_fruit_stall",
                respawn = 4,
                guards = listOf("npc.hosidius_guarddog"),
            ),
            StallTarget(
                loc = "loc.fish_stall_warrens",
                level = 42,
                xp = 42.0,
                respawn = 12,
                owners = listOf("npc.warrens_fishmonger"),
                guards = listOf("npc.warrens_thief_stall"),
            ),
            StallTarget(
                loc = "loc.fortis_market_stall_bakers",
                level = 5,
                xp = 16.0,
                empty = "loc.fortis_market_stall",
                respawn = 4,
                owners = listOf("npc.fortis_shop_baker"),
                guards = FORTIS_GUARDS,
            ),
            StallTarget(
                loc = "loc.fortis_market_stall_silk",
                level = 20,
                xp = 24.0,
                empty = "loc.fortis_market_stall",
                respawn = 8,
                owners = listOf("npc.fortis_shop_silk"),
                guards = FORTIS_GUARDS,
            ),
            StallTarget(
                loc = "loc.fortis_market_stall_fur",
                level = 35,
                xp = 45.0,
                empty = "loc.fortis_market_stall",
                respawn = 12,
                owners = listOf("npc.fortis_shop_fur"),
                guards = FORTIS_GUARDS,
            ),
            StallTarget(
                loc = "loc.fortis_market_stall_spice",
                level = 65,
                xp = 92.0,
                empty = "loc.fortis_market_stall",
                respawn = 10,
                owners = listOf("npc.fortis_shop_spices"),
                guards = FORTIS_GUARDS,
            ),
            StallTarget(
                loc = "loc.fortis_market_stall_gems",
                level = 75,
                xp = 408.0,
                empty = "loc.fortis_market_stall",
                respawn = 100,
                owners = listOf("npc.fortis_shop_gems"),
                guards = FORTIS_GUARDS,
            ),
        )

    internal val coinPouches: List<CoinPouch> =
        listOf(
            CoinPouch("obj.pickpocket_coin_pouch_citizen", 3..3),
            CoinPouch("obj.pickpocket_coin_pouch_farmer", 9..9),
            CoinPouch("obj.pickpocket_coin_pouch_ham", 1..21),
            CoinPouch("obj.pickpocket_coin_pouch_warrior", 18..18),
            CoinPouch("obj.pickpocket_coin_pouch_rogue", 25..40),
            CoinPouch("obj.pickpocket_coin_pouch_cavegoblin", 10..50),
            CoinPouch("obj.pickpocket_coin_pouch_guard", 30..30),
            CoinPouch("obj.pickpocket_coin_pouch_fremennik", 40..40),
            CoinPouch("obj.pickpocket_coin_pouch_bandit2", 40..40),
            CoinPouch("obj.pickpocket_coin_pouch_varlamore_wealthy", 85..85),
            CoinPouch("obj.pickpocket_coin_pouch_desertbandit", 30..30),
            CoinPouch("obj.pickpocket_coin_pouch_knight", 50..50),
            CoinPouch("obj.pickpocket_coin_pouch_bandit", 50..50),
            CoinPouch("obj.pickpocket_coin_pouch_watchman", 60..60),
            CoinPouch("obj.pickpocket_coin_pouch_menaphite", 60..60),
            CoinPouch("obj.pickpocket_coin_pouch_paladin", 80..80),
            CoinPouch("obj.pickpocket_coin_pouch_gnome", 300..300),
            CoinPouch("obj.pickpocket_coin_pouch_hero", 200..300),
            CoinPouch("obj.pickpocket_coin_pouch_vyre", 230..315),
            CoinPouch("obj.pickpocket_coin_pouch_elf", 280..350),
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
        pouch = "obj.pickpocket_coin_pouch_ham",
        lowercaseName = false,
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

private val PRIFDDINAS_GUARDS = (0..7).map { "npc.prif_guard$it" } + "npc.prif_city_guard"

private val ETCETERIA_GUARDS = listOf("npc.etc_guard1", "npc.etc_guard2")

private val FORTIS_GUARDS =
    (1..5).flatMap { listOf("npc.varlamore_guard_m_$it", "npc.varlamore_guard_f_$it") }
