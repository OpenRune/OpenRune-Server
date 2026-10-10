package org.rsmod.content.skills.thieving.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object ThievingTable {
    const val COL_LOOT_TABLE = 0
    const val COL_NPCS = 1
    const val COL_CATEGORIES = 2
    const val COL_LEVEL = 3
    const val COL_XP = 4
    const val COL_SUCCESS_LOW = 5
    const val COL_SUCCESS_HIGH = 6
    const val COL_STUN_DAMAGE_MIN = 7
    const val COL_STUN_DAMAGE_MAX = 8
    const val COL_STUN_TICKS = 9
    const val COL_CAUGHT_SHOUT = 10
    const val COL_COIN_POUCH = 11
    const val COL_POUCH_COINS_MIN = 12
    const val COL_POUCH_COINS_MAX = 13
    const val COL_ROCKY_CHANCE = 14
    const val COL_HOT_POCKET_DAMAGE = 15
    const val COL_LOWERCASE_NAME = 16
    const val COL_PROPER_NAME = 17

    const val COL_STALL_LOC = 0
    const val COL_STALL_EMPTY_LOC = 1
    const val COL_STALL_LEVEL = 2
    const val COL_STALL_XP = 3
    const val COL_STALL_RESTOCK_TICKS = 4
    const val COL_STALL_GUARDS = 5
    const val COL_STALL_ATTEMPT_MESSAGE = 6
    const val COL_STALL_OWNERS = 7

    private const val DEFAULT_STUN_TICKS = 8
    private const val DEFAULT_ROCKY_CHANCE = 257_211
    private const val DEFAULT_CAUGHT_SHOUT = "What do you think you're doing?"

    fun pickpockets() =
        dbTable("dbtable.thieving_pickpocket", serverOnly = true) {
            column("loot_table", COL_LOOT_TABLE, VarType.STRING)
            column("npcs", COL_NPCS, VarType.NPC)
            column("categories", COL_CATEGORIES, VarType.CATEGORY)
            column("level", COL_LEVEL, VarType.INT)
            column("xp", COL_XP, VarType.INT)
            column("success_low", COL_SUCCESS_LOW, VarType.INT)
            column("success_high", COL_SUCCESS_HIGH, VarType.INT)
            column("stun_damage_min", COL_STUN_DAMAGE_MIN, VarType.INT)
            column("stun_damage_max", COL_STUN_DAMAGE_MAX, VarType.INT)
            column("stun_ticks", COL_STUN_TICKS, VarType.INT)
            column("caught_shout", COL_CAUGHT_SHOUT, VarType.STRING)
            column("coin_pouch", COL_COIN_POUCH, VarType.OBJ)
            column("pouch_coins_min", COL_POUCH_COINS_MIN, VarType.INT)
            column("pouch_coins_max", COL_POUCH_COINS_MAX, VarType.INT)
            column("rocky_chance", COL_ROCKY_CHANCE, VarType.INT)
            column("hot_pocket_damage", COL_HOT_POCKET_DAMAGE, VarType.INT)
            column("lowercase_name", COL_LOWERCASE_NAME, VarType.BOOLEAN)
            column("proper_name", COL_PROPER_NAME, VarType.BOOLEAN)

            fun target(
                row: String,
                lootTable: String,
                level: Int,
                xp: Int,
                low: Int,
                high: Int,
                stunDamage: IntRange,
                npcs: List<String> = emptyList(),
                categories: List<String> = emptyList(),
                stunTicks: Int = DEFAULT_STUN_TICKS,
                caughtShout: String = DEFAULT_CAUGHT_SHOUT,
                pouch: String? = null,
                pouchCoins: IntRange = 0..0,
                rockyChance: Int = DEFAULT_ROCKY_CHANCE,
                hotPocketDamage: Int = 0,
                lowercaseName: Boolean = false,
                properName: Boolean = false,
            ) =
                row(row) {
                    column(COL_LOOT_TABLE, lootTable)
                    if (npcs.isNotEmpty()) columnRSCM(COL_NPCS, *npcs.toTypedArray())
                    if (categories.isNotEmpty()) {
                        columnRSCM(COL_CATEGORIES, *categories.toTypedArray())
                    }
                    column(COL_LEVEL, level)
                    column(COL_XP, xp)
                    column(COL_SUCCESS_LOW, low)
                    column(COL_SUCCESS_HIGH, high)
                    column(COL_STUN_DAMAGE_MIN, stunDamage.first)
                    column(COL_STUN_DAMAGE_MAX, stunDamage.last)
                    column(COL_STUN_TICKS, stunTicks)
                    column(COL_CAUGHT_SHOUT, caughtShout)
                    if (pouch != null) {
                        columnRSCM(COL_COIN_POUCH, pouch)
                        column(COL_POUCH_COINS_MIN, pouchCoins.first)
                        column(COL_POUCH_COINS_MAX, pouchCoins.last)
                    }
                    column(COL_ROCKY_CHANCE, rockyChance)
                    column(COL_HOT_POCKET_DAMAGE, hotPocketDamage)
                    column(COL_LOWERCASE_NAME, lowercaseName)
                    column(COL_PROPER_NAME, properName)
                }

            target(
                "dbrow.thieving_pickpocket_citizen",
                lootTable = "citizen",
                level = 1,
                xp = 80,
                low = 180,
                high = 240,
                stunDamage = 1..1,
                npcs =
                    listOf(
                        "npc.ardougnian_male1",
                        "npc.ardougnian_female1",
                        "npc.death_man_indoors1",
                    ),
                categories =
                    listOf(
                        "category.man",
                        "category.woman",
                        "category.varlamore_citizen",
                        "category.varlamore_citizen_poor",
                        "category.varlamore_citizen_rich",
                        "category.aldarin_citizen_poor",
                        "category.aldarin_citizen",
                        "category.aldarin_citizen_rich",
                        "category.auburnvale_citizen",
                        "category.auburnvale_citizen_rich",
                        "category.kastori_citizen",
                        "category.kastori_citizen_rich",
                        "category.tal_teklan_citizen",
                        "category.tal_teklan_citizen_rich",
                    ),
                pouch = "obj.pickpocket_coin_pouch_citizen",
                pouchCoins = 3..3,
                lowercaseName = true,
            )
            target(
                "dbrow.thieving_pickpocket_farmer",
                lootTable = "farmer",
                level = 10,
                xp = 145,
                low = 150,
                high = 240,
                stunDamage = 1..1,
                npcs = listOf("npc.tal_teklan_farmer"),
                categories =
                    listOf(
                        "category.farmer",
                        "category.varlamore_farmer",
                        "category.kastori_farmer",
                    ),
                pouch = "obj.pickpocket_coin_pouch_farmer",
                pouchCoins = 9..9,
                lowercaseName = true,
            )
            target(
                "dbrow.thieving_pickpocket_ham_member",
                lootTable = "ham_member",
                level = 15,
                xp = 222,
                low = 135,
                high = 239,
                stunDamage = 1..3,
                npcs = listOf("npc.favour_male_ham_civilian"),
                stunTicks = 7,
                pouch = "obj.pickpocket_coin_pouch_ham",
                pouchCoins = 1..21,
            )
            target(
                "dbrow.thieving_pickpocket_ham_member_2",
                lootTable = "ham_member",
                level = 20,
                xp = 222,
                low = 135,
                high = 239,
                stunDamage = 1..3,
                npcs = listOf("npc.favour_female_ham_civilian"),
                stunTicks = 7,
                pouch = "obj.pickpocket_coin_pouch_ham",
                pouchCoins = 1..21,
            )
            target(
                "dbrow.thieving_pickpocket_ham_guard",
                lootTable = "ham_guard",
                level = 20,
                xp = 222,
                low = 135,
                high = 239,
                stunDamage = 1..3,
                categories = listOf("category.ham_storeroom_guard"),
                stunTicks = 7,
                pouch = "obj.pickpocket_coin_pouch_ham",
                pouchCoins = 1..21,
                lowercaseName = true,
            )
            target(
                "dbrow.thieving_pickpocket_tourist",
                lootTable = "tourist",
                level = 1,
                xp = 80,
                low = 180,
                high = 240,
                stunDamage = 1..1,
                categories = listOf("category.varlamore_tourist"),
                lowercaseName = true,
            )
            target(
                "dbrow.thieving_pickpocket_salvager",
                lootTable = "salvager",
                level = 1,
                xp = 80,
                low = 180,
                high = 240,
                stunDamage = 1..1,
                categories = listOf("category.salvager"),
                lowercaseName = true,
            )
            target(
                "dbrow.thieving_pickpocket_warrior",
                lootTable = "warrior",
                level = 25,
                xp = 260,
                low = 100,
                high = 240,
                stunDamage = 2..2,
                npcs = listOf("npc.al_kharid_warrior"),
                categories = listOf("category.warrior"),
                pouch = "obj.pickpocket_coin_pouch_warrior",
                pouchCoins = 18..18,
                lowercaseName = true,
            )
            target(
                "dbrow.thieving_pickpocket_rogue",
                lootTable = "rogue",
                level = 32,
                xp = 365,
                low = 75,
                high = 240,
                stunDamage = 2..2,
                npcs = listOf("npc.rogue"),
                pouch = "obj.pickpocket_coin_pouch_rogue",
                pouchCoins = 25..40,
                lowercaseName = true,
            )
            target(
                "dbrow.thieving_pickpocket_cave_goblin",
                lootTable = "cave_goblin",
                level = 36,
                xp = 400,
                low = 100,
                high = 240,
                stunDamage = 1..1,
                categories = listOf("category.cave_goblin"),
                pouch = "obj.pickpocket_coin_pouch_cavegoblin",
                pouchCoins = 10..50,
                lowercaseName = true,
            )
            target(
                "dbrow.thieving_pickpocket_master_farmer",
                lootTable = "master_farmer",
                level = 38,
                xp = 430,
                low = 90,
                high = 240,
                stunDamage = 3..3,
                categories =
                    listOf(
                        "category.master_farmer",
                        "category.varlamore_master_farmer",
                        "category.kastori_master_farmer",
                    ),
                caughtShout = "Cor blimey mate, what are ye doing in me pockets?",
            )
            target(
                "dbrow.thieving_pickpocket_guard",
                lootTable = "guard",
                level = 40,
                xp = 468,
                low = 50,
                high = 240,
                stunDamage = 2..2,
                npcs =
                    listOf(
                        "npc.jail_guard_1",
                        "npc.jail_guard_2",
                        "npc.jail_guard_3",
                        "npc.jail_guard_4",
                        "npc.jail_guard_5",
                    ),
                categories = listOf("category.guard", "category.ardougne_guard"),
                pouch = "obj.pickpocket_coin_pouch_guard",
                pouchCoins = 30..30,
                lowercaseName = true,
            )
            target(
                "dbrow.thieving_pickpocket_fremennik_citizen",
                lootTable = "fremennik_citizen",
                level = 45,
                xp = 650,
                low = 180,
                high = 240,
                stunDamage = 2..2,
                npcs =
                    listOf(
                        "npc.viking_man",
                        "npc.viking_man2",
                        "npc.viking_man3",
                        "npc.viking_man4",
                        "npc.viking_man5",
                        "npc.viking_woman2",
                        "npc.viking_woman3",
                        "npc.viking_woman4",
                        "npc.viking_woman_indoors",
                    ),
                pouch = "obj.pickpocket_coin_pouch_fremennik",
                pouchCoins = 40..40,
                properName = true,
            )
            target(
                "dbrow.thieving_pickpocket_desert_bandit",
                lootTable = "desert_bandit",
                level = 53,
                xp = 794,
                low = 50,
                high = 240,
                stunDamage = 3..3,
                npcs =
                    listOf(
                        "npc.fourdiamonds_sword_bandit_1",
                        "npc.fourdiamonds_sword_bandit_free",
                    ),
                pouch = "obj.pickpocket_coin_pouch_desertbandit",
                pouchCoins = 30..30,
                lowercaseName = true,
            )
            target(
                "dbrow.thieving_pickpocket_knight",
                lootTable = "knight",
                level = 55,
                xp = 843,
                low = 50,
                high = 240,
                stunDamage = 3..3,
                npcs = listOf("npc.knight_of_ardougne2"),
                categories =
                    listOf(
                        "category.knight_of_ardougne",
                        "category.knight_of_ardougne_west",
                        "category.knight_of_varlamore",
                    ),
                pouch = "obj.pickpocket_coin_pouch_knight",
                pouchCoins = 50..50,
            )
            target(
                "dbrow.thieving_pickpocket_pirate",
                lootTable = "pirate",
                level = 60,
                xp = 720,
                low = 50,
                high = 240,
                stunDamage = 3..3,
                categories = listOf("category.pirate_thieving"),
                stunTicks = 9,
                pouch = "obj.pickpocket_coin_pouch_pirate",
                pouchCoins = 20..20,
                lowercaseName = true,
            )
            target(
                "dbrow.thieving_pickpocket_watchman",
                lootTable = "watchman",
                level = 65,
                xp = 1375,
                low = 15,
                high = 160,
                stunDamage = 3..3,
                npcs = listOf("npc.yanille_watchman"),
                pouch = "obj.pickpocket_coin_pouch_watchman",
                pouchCoins = 60..60,
                rockyChance = 134_625,
                lowercaseName = true,
            )
            target(
                "dbrow.thieving_pickpocket_paladin",
                lootTable = "paladin",
                level = 70,
                xp = 1318,
                low = 35,
                high = 160,
                stunDamage = 3..3,
                categories = listOf("category.paladin", "category.paladin_west"),
                pouch = "obj.pickpocket_coin_pouch_paladin",
                pouchCoins = 80..80,
                rockyChance = 127_056,
                lowercaseName = true,
            )
            target(
                "dbrow.thieving_pickpocket_gnome",
                lootTable = "gnome",
                level = 75,
                xp = 1333,
                low = 33,
                high = 140,
                stunDamage = 1..1,
                categories = listOf("category.gnome"),
                pouch = "obj.pickpocket_coin_pouch_gnome",
                pouchCoins = 300..300,
                rockyChance = 108_718,
                lowercaseName = true,
            )
            target(
                "dbrow.thieving_pickpocket_hero",
                lootTable = "hero",
                level = 80,
                xp = 1633,
                low = 39,
                high = 160,
                stunDamage = 3..3,
                categories = listOf("category.hero"),
                stunTicks = 10,
                pouch = "obj.pickpocket_coin_pouch_hero",
                pouchCoins = 200..300,
                rockyChance = 99_175,
                lowercaseName = true,
            )
            target(
                "dbrow.thieving_pickpocket_vyre",
                lootTable = "vyre",
                level = 82,
                xp = 3069,
                low = 8,
                high = 128,
                stunDamage = 5..5,
                categories = listOf("category.vyre"),
                stunTicks = 10,
                pouch = "obj.pickpocket_coin_pouch_vyre",
                pouchCoins = 230..315,
                rockyChance = 99_175,
                properName = true,
            )
            target(
                "dbrow.thieving_pickpocket_elf",
                lootTable = "elf",
                level = 85,
                xp = 3533,
                low = 6,
                high = 100,
                stunDamage = 5..5,
                categories = listOf("category.elf"),
                stunTicks = 10,
                pouch = "obj.pickpocket_coin_pouch_elf",
                pouchCoins = 280..350,
                rockyChance = 99_175,
                properName = true,
            )
            target(
                "dbrow.thieving_pickpocket_elf_lletya",
                lootTable = "elf_lletya",
                level = 85,
                xp = 3533,
                low = 6,
                high = 100,
                stunDamage = 5..5,
                npcs =
                    listOf(
                        "npc.mourning_town_elf_1",
                        "npc.mourning_town_elf_3",
                        "npc.mourning_town_elf_4",
                        "npc.mourning_town_elf_5_vis",
                    ),
                stunTicks = 10,
                pouch = "obj.pickpocket_coin_pouch_elf",
                pouchCoins = 280..350,
                rockyChance = 99_175,
                properName = true,
            )
            target(
                "dbrow.thieving_pickpocket_tzhaar_hur",
                lootTable = "tzhaar_hur",
                level = 90,
                xp = 1034,
                low = -200,
                high = 200,
                stunDamage = 4..4,
                categories = listOf("category.tzhaar_hur"),
                stunTicks = 10,
                rockyChance = 176_743,
                hotPocketDamage = 4,
            )
            target(
                "dbrow.thieving_pickpocket_workman",
                lootTable = "workman",
                level = 25,
                xp = 104,
                low = 150,
                high = 240,
                stunDamage = 1..1,
                npcs =
                    listOf(
                        "npc.digworkman1",
                        "npc.digworkman2",
                        "npc.qip_digsite_digworkman_03",
                        "npc.qip_digsite_digworkman_04",
                    ),
                stunTicks = 7,
                lowercaseName = true,
            )
            target(
                "dbrow.thieving_pickpocket_bearded_bandit",
                lootTable = "bearded_bandit",
                level = 45,
                xp = 650,
                low = 50,
                high = 240,
                stunDamage = 5..5,
                npcs = listOf("npc.feud_arabian_guard2_1", "npc.feud_arabian_guard2_2"),
                pouch = "obj.pickpocket_coin_pouch_bandit2",
                pouchCoins = 40..40,
                lowercaseName = true,
            )
            target(
                "dbrow.thieving_pickpocket_wealthy_citizen",
                lootTable = "wealthy_citizen",
                level = 50,
                xp = 960,
                low = 35,
                high = 200,
                stunDamage = 3..3,
                npcs =
                    listOf(
                        "npc.varlamore_wealthy_citizen_a",
                        "npc.varlamore_wealthy_citizen_b",
                        "npc.varlamore_wealthy_citizen_c",
                        "npc.varlamore_wealthy_citizen_d",
                    ),
                stunTicks = 7,
                pouch = "obj.pickpocket_coin_pouch_varlamore_wealthy",
                pouchCoins = 85..85,
                lowercaseName = true,
            )
            target(
                "dbrow.thieving_pickpocket_pollnivneach_bandit",
                lootTable = "pollnivneach_bandit",
                level = 55,
                xp = 843,
                low = 50,
                high = 240,
                stunDamage = 5..5,
                npcs = listOf("npc.feud_arabian_guard1_1", "npc.feud_arabian_guard1_2"),
                pouch = "obj.pickpocket_coin_pouch_bandit",
                pouchCoins = 50..50,
                lowercaseName = true,
            )
            target(
                "dbrow.thieving_pickpocket_menaphite_thug",
                lootTable = "menaphite_thug",
                level = 65,
                xp = 1375,
                low = 50,
                high = 160,
                stunDamage = 5..5,
                npcs = listOf("npc.feud_egyptian_doorman_2"),
                pouch = "obj.pickpocket_coin_pouch_menaphite",
                pouchCoins = 60..60,
            )
        }

    fun stalls() =
        dbTable("dbtable.thieving_stall", serverOnly = true) {
            column("loc", COL_STALL_LOC, VarType.LOC)
            column("empty_loc", COL_STALL_EMPTY_LOC, VarType.LOC)
            column("level", COL_STALL_LEVEL, VarType.INT)
            column("xp", COL_STALL_XP, VarType.INT)
            column("restock_ticks", COL_STALL_RESTOCK_TICKS, VarType.INT)
            column("guards", COL_STALL_GUARDS, VarType.NPC)
            column("attempt_message", COL_STALL_ATTEMPT_MESSAGE, VarType.STRING)
            column("owners", COL_STALL_OWNERS, VarType.NPC)

            fun stall(
                row: String,
                loc: String,
                level: Int,
                xp: Int,
                restockTicks: Int,
                emptyLoc: String? = null,
                owners: List<String> = emptyList(),
                guards: List<String> = emptyList(),
                attemptMessage: String? = null,
            ) =
                row(row) {
                    columnRSCM(COL_STALL_LOC, loc)
                    if (emptyLoc != null) columnRSCM(COL_STALL_EMPTY_LOC, emptyLoc)
                    column(COL_STALL_LEVEL, level)
                    column(COL_STALL_XP, xp)
                    column(COL_STALL_RESTOCK_TICKS, restockTicks)
                    if (guards.isNotEmpty()) columnRSCM(COL_STALL_GUARDS, *guards.toTypedArray())
                    if (attemptMessage != null) column(COL_STALL_ATTEMPT_MESSAGE, attemptMessage)
                    if (owners.isNotEmpty()) columnRSCM(COL_STALL_OWNERS, *owners.toTypedArray())
                }

            stall(
                "dbrow.thieving_stall_seed",
                loc = "loc.seed_stall",
                level = 27,
                xp = 100,
                restockTicks = 4,
                owners = listOf("npc.seed_merchant"),
                guards = DRAYNOR_MARKET_GUARDS,
                attemptMessage = "You attempt to steal some seeds from the seed merchant's stall.",
            )
            stall(
                "dbrow.thieving_stall_wine",
                loc = "loc.rag_market_stall",
                level = 22,
                xp = 270,
                restockTicks = 8,
                emptyLoc = "loc.rag_market_stall_empty",
                owners = listOf("npc.rag_wine_merchant"),
                guards = DRAYNOR_MARKET_GUARDS,
                attemptMessage = "You attempt to steal something from the wine merchant's stall.",
            )
            stall(
                "dbrow.thieving_stall_ardougne_baker",
                loc = "loc.cakethiefstall",
                level = 5,
                xp = 160,
                restockTicks = 4,
                emptyLoc = "loc.bakerymarket",
                owners = BAKERS,
                guards = ARDOUGNE_MARKET_GUARDS,
            )
            stall(
                "dbrow.thieving_stall_varrock_tea",
                loc = "loc.tea_stall",
                level = 5,
                xp = 160,
                restockTicks = 4,
                emptyLoc = "loc.market",
                owners = listOf("npc.tea_seller"),
            )
            stall(
                "dbrow.thieving_stall_ardougne_silk",
                loc = "loc.silkthiefstall",
                level = 20,
                xp = 240,
                restockTicks = 8,
                emptyLoc = "loc.market",
                owners = listOf("npc.silk_merchant_ardougne", "npc.silk_merchant"),
                guards = ARDOUGNE_MARKET_GUARDS,
            )
            stall(
                "dbrow.thieving_stall_ardougne_fur",
                loc = "loc.furthiefstall",
                level = 35,
                xp = 450,
                restockTicks = 12,
                emptyLoc = "loc.furmarket",
                owners = listOf("npc.fur_merchant_ardougne", "npc.fur_merchant"),
                guards = ARDOUGNE_MARKET_GUARDS,
            )
            stall(
                "dbrow.thieving_stall_ardougne_silver",
                loc = "loc.silverthiefstall",
                level = 50,
                xp = 2050,
                restockTicks = 32,
                emptyLoc = "loc.market",
                owners = listOf("npc.silver_merchant_ardougne"),
                guards = ARDOUGNE_MARKET_GUARDS,
            )
            stall(
                "dbrow.thieving_stall_ardougne_spice",
                loc = "loc.spicethiefstall",
                level = 65,
                xp = 920,
                restockTicks = 10,
                emptyLoc = "loc.spicemarket",
                owners = listOf("npc.spice_merchant_ardougne", "npc.spice_merchant"),
                guards = ARDOUGNE_MARKET_GUARDS,
            )
            stall(
                "dbrow.thieving_stall_ardougne_gem",
                loc = "loc.gemthiefstall",
                level = 75,
                xp = 4080,
                restockTicks = 100,
                emptyLoc = "loc.gemmarket",
                owners = listOf("npc.gem_merchant_ardougne", "npc.gem_merchant"),
                guards = ARDOUGNE_MARKET_GUARDS,
            )
            stall(
                "dbrow.thieving_stall_prif_silk",
                loc = "loc.prif_marketstall_silk",
                level = 20,
                xp = 240,
                restockTicks = 8,
                emptyLoc = "loc.prif_marketstall_empty",
                owners = listOf("npc.prif_silk"),
                guards = PRIFDDINAS_GUARDS,
            )
            stall(
                "dbrow.thieving_stall_prif_silver",
                loc = "loc.prif_marketstall_silver",
                level = 50,
                xp = 2050,
                restockTicks = 32,
                emptyLoc = "loc.prif_marketstall_empty",
                owners = listOf("npc.prif_silver"),
                guards = PRIFDDINAS_GUARDS,
            )
            stall(
                "dbrow.thieving_stall_prif_spice",
                loc = "loc.prif_marketstall_spice",
                level = 65,
                xp = 920,
                restockTicks = 10,
                emptyLoc = "loc.prif_marketstall_empty",
                owners = listOf("npc.prif_spice"),
                guards = PRIFDDINAS_GUARDS,
            )
            stall(
                "dbrow.thieving_stall_prif_gem",
                loc = "loc.prif_marketstall_gem",
                level = 75,
                xp = 4080,
                restockTicks = 100,
                emptyLoc = "loc.prif_marketstall_empty",
                owners = listOf("npc.prif_gem"),
                guards = PRIFDDINAS_GUARDS,
            )
            stall(
                "dbrow.thieving_stall_rellekka_fish",
                loc = "loc.viking_fish_market",
                level = 42,
                xp = 420,
                restockTicks = 12,
                emptyLoc = "loc.viking_market",
                owners = listOf("npc.viking_fish_monger"),
                guards = listOf("npc.viking_guard"),
            )
            stall(
                "dbrow.thieving_stall_rellekka_fur",
                loc = "loc.viking_fur_market",
                level = 35,
                xp = 450,
                restockTicks = 12,
                emptyLoc = "loc.viking_market",
                owners = listOf("npc.viking_fur_monger"),
                guards = listOf("npc.viking_guard"),
            )
            stall(
                "dbrow.thieving_stall_miscellania_fish",
                loc = "loc.misc_fish_market",
                level = 42,
                xp = 420,
                restockTicks = 12,
                owners = listOf("npc.misc_fish_monger"),
                guards = listOf("npc.royal_misc_guard"),
            )
            stall(
                "dbrow.thieving_stall_miscellania_veg",
                loc = "loc.misc_veg_market",
                level = 2,
                xp = 100,
                restockTicks = 2,
                owners = listOf("npc.misc_veg_monger"),
                guards = listOf("npc.royal_misc_guard"),
            )
            stall(
                "dbrow.thieving_stall_etceteria_fish",
                loc = "loc.etc_fish_market",
                level = 42,
                xp = 420,
                restockTicks = 12,
                owners = listOf("npc.etc_fish_monger"),
                guards = ETCETERIA_GUARDS,
            )
            stall(
                "dbrow.thieving_stall_etceteria_veg",
                loc = "loc.etc_veg_market",
                level = 2,
                xp = 100,
                restockTicks = 2,
                owners = listOf("npc.etc_veg_monger"),
                guards = ETCETERIA_GUARDS,
            )
            stall(
                "dbrow.thieving_stall_keldagrim_bakery",
                loc = "loc.dwarf_market_bakery",
                level = 5,
                xp = 160,
                restockTicks = 16,
                emptyLoc = "loc.dwarf_market_empty_stall",
                owners = listOf("npc.dwarf_city_shop_bakery"),
                guards = KELDAGRIM_GUARDS,
            )
            stall(
                "dbrow.thieving_stall_keldagrim_crafting",
                loc = "loc.dwarf_market_crafting",
                level = 5,
                xp = 200,
                restockTicks = 8,
                emptyLoc = "loc.dwarf_market_empty_stall",
                owners = listOf("npc.dwarf_city_shop_craft"),
                guards = KELDAGRIM_GUARDS,
            )
            stall(
                "dbrow.thieving_stall_keldagrim_crossbow",
                loc = "loc.xbows_dwarf_market",
                level = 49,
                xp = 520,
                restockTicks = 8,
                owners = listOf("npc.xbows_sales_dwarf_keldegrim"),
                guards = KELDAGRIM_GUARDS,
            )
            stall(
                "dbrow.thieving_stall_keldagrim_silver",
                loc = "loc.dwarf_market_silver",
                level = 50,
                xp = 2050,
                restockTicks = 32,
                emptyLoc = "loc.dwarf_market_empty_stall",
                owners = listOf("npc.dwarf_city_shop_silver"),
                guards = KELDAGRIM_GUARDS,
            )
            stall(
                "dbrow.thieving_stall_keldagrim_gem",
                loc = "loc.dwarf_market_gems",
                level = 75,
                xp = 4080,
                restockTicks = 100,
                emptyLoc = "loc.dwarf_market_empty_stall",
                owners = listOf("npc.dwarf_city_shop_gems"),
                guards = KELDAGRIM_GUARDS,
            )
            stall(
                "dbrow.thieving_stall_hosidius_bread",
                loc = "loc.hos_stall_bread",
                level = 5,
                xp = 160,
                restockTicks = 4,
                emptyLoc = "loc.hos_stall_empty",
            )
            stall(
                "dbrow.thieving_stall_hosidius_fruit",
                loc = "loc.hos_fruit_stall_02",
                level = 25,
                xp = 285,
                restockTicks = 4,
                emptyLoc = "loc.hos_fruit_stall",
                guards = listOf("npc.hosidius_guarddog"),
            )
            stall(
                "dbrow.thieving_stall_warrens_fish",
                loc = "loc.fish_stall_warrens",
                level = 42,
                xp = 420,
                restockTicks = 12,
                owners = listOf("npc.warrens_fishmonger"),
                guards = listOf("npc.warrens_thief_stall"),
            )
            stall(
                "dbrow.thieving_stall_fortis_baker",
                loc = "loc.fortis_market_stall_bakers",
                level = 5,
                xp = 160,
                restockTicks = 4,
                emptyLoc = "loc.fortis_market_stall",
                owners = listOf("npc.fortis_shop_baker"),
                guards = FORTIS_GUARDS,
            )
            stall(
                "dbrow.thieving_stall_fortis_silk",
                loc = "loc.fortis_market_stall_silk",
                level = 20,
                xp = 240,
                restockTicks = 8,
                emptyLoc = "loc.fortis_market_stall",
                owners = listOf("npc.fortis_shop_silk"),
                guards = FORTIS_GUARDS,
            )
            stall(
                "dbrow.thieving_stall_fortis_fur",
                loc = "loc.fortis_market_stall_fur",
                level = 35,
                xp = 450,
                restockTicks = 12,
                emptyLoc = "loc.fortis_market_stall",
                owners = listOf("npc.fortis_shop_fur"),
                guards = FORTIS_GUARDS,
            )
            stall(
                "dbrow.thieving_stall_fortis_spice",
                loc = "loc.fortis_market_stall_spice",
                level = 65,
                xp = 920,
                restockTicks = 10,
                emptyLoc = "loc.fortis_market_stall",
                owners = listOf("npc.fortis_shop_spices"),
                guards = FORTIS_GUARDS,
            )
            stall(
                "dbrow.thieving_stall_fortis_gem",
                loc = "loc.fortis_market_stall_gems",
                level = 75,
                xp = 4080,
                restockTicks = 100,
                emptyLoc = "loc.fortis_market_stall",
                owners = listOf("npc.fortis_shop_gems"),
                guards = FORTIS_GUARDS,
            )
        }
}

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

private val KELDAGRIM_GUARDS = (1..4).map { "npc.dwarf_city_black_guard$it" }

private val ETCETERIA_GUARDS = listOf("npc.etc_guard1", "npc.etc_guard2")

private val FORTIS_GUARDS =
    (1..5).flatMap { listOf("npc.varlamore_guard_m_$it", "npc.varlamore_guard_f_$it") }
