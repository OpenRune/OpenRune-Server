package org.rsmod.content.skills.thieving

import dtx.rs.RSDropTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

internal typealias ThievingDropTable = RSDropTable<Player, DropRollItem>

internal object ThievingDropTables {
    val pickpockets: Map<String, ThievingDropTable> =
        mapOf(
            "dbrow.thieving_man" to pouchOnly("Man", "obj.pickpocket_coin_pouch_citizen"),
            "dbrow.thieving_woman" to pouchOnly("Woman", "obj.pickpocket_coin_pouch_citizen"),
            "dbrow.thieving_citizen" to pouchOnly("Citizen", "obj.pickpocket_coin_pouch_citizen"),
            "dbrow.thieving_farmer" to
                RSDropTable(
                    tableIdentifier = "Farmer",
                    mainTable =
                        rsPlayerWeightedTable {
                            123 weight "obj.pickpocket_coin_pouch_farmer" count 1
                            5 weight "obj.potato_seed" count 1
                        },
                ),
            "dbrow.thieving_male_h_a_m_member" to hamMember("Male H.A.M. Member"),
            "dbrow.thieving_female_h_a_m_member" to hamMember("Female H.A.M. Member"),
            "dbrow.thieving_h_a_m_member" to hamMember("H.A.M. Member"),
            "dbrow.thieving_warrior_woman" to
                pouchOnly("Warrior woman", "obj.pickpocket_coin_pouch_warrior"),
            "dbrow.thieving_warrior" to pouchOnly("Warrior", "obj.pickpocket_coin_pouch_warrior"),
            "dbrow.thieving_workman" to
                RSDropTable(
                    tableIdentifier = "Workman",
                    mainTable =
                        rsPlayerWeightedTable {
                            3 weight "obj.specimen_brush" count 1
                            3 weight "obj.skull" count 1
                            1 weight "obj.coins" count 10
                            1 weight "obj.rope" count 1
                            1 weight "obj.bucket_empty" count 1
                            1 weight "obj.leather_gloves" count 1
                            1 weight "obj.spade" count 1
                        },
                ),
            "dbrow.thieving_villager" to
                RSDropTable(
                    tableIdentifier = "Villager",
                    guaranteed = rsPlayerGuaranteedTable { "obj.coins" count 5 },
                ),
            "dbrow.thieving_rogue" to
                RSDropTable(
                    tableIdentifier = "Rogue",
                    mainTable =
                        rsPlayerWeightedTable {
                            123 weight "obj.pickpocket_coin_pouch_rogue" count 1
                            9 weight "obj.airrune" count 8
                            6 weight "obj.jug_wine" count 1
                            5 weight "obj.lockpick" count 1
                            1 weight "obj.iron_dagger_p" count 1
                        },
                ),
            "dbrow.thieving_cave_goblin" to
                pouchOnly("Cave goblin", "obj.pickpocket_coin_pouch_cavegoblin"),
            "dbrow.thieving_master_farmer" to
                RSDropTable(tableIdentifier = "Master Farmer", mainTable = masterFarmerSeeds()),
            "dbrow.thieving_guard" to pouchOnly("Guard", "obj.pickpocket_coin_pouch_guard"),
            "dbrow.thieving_fremennik_citizen" to
                pouchOnly("Fremennik citizen", "obj.pickpocket_coin_pouch_fremennik"),
            "dbrow.thieving_bearded_pollnivnian_bandit" to
                pouchOnly("Bearded Pollnivnian Bandit", "obj.pickpocket_coin_pouch_bandit2"),
            "dbrow.thieving_wealthy_citizen" to
                pouchOnly("Wealthy citizen", "obj.pickpocket_coin_pouch_varlamore_wealthy"),
            "dbrow.thieving_desert_bandit" to
                RSDropTable(
                    tableIdentifier = "Desert Bandit",
                    mainTable =
                        rsPlayerWeightedTable {
                            5 weight "obj.pickpocket_coin_pouch_desertbandit" count 1
                            1 weight "obj.1doseantipoison" count 1
                            1 weight "obj.lockpick" count 1
                        },
                ),
            "dbrow.thieving_knight_of_ardougne" to
                pouchOnly("Knight of Ardougne", "obj.pickpocket_coin_pouch_knight"),
            "dbrow.thieving_knight_of_varlamore" to
                pouchOnly("Knight of Varlamore", "obj.pickpocket_coin_pouch_knight"),
            "dbrow.thieving_pollnivnian_bandit" to
                pouchOnly("Pollnivnian Bandit", "obj.pickpocket_coin_pouch_bandit"),
            "dbrow.thieving_watchman" to
                RSDropTable(
                    tableIdentifier = "Watchman",
                    guaranteed =
                        rsPlayerGuaranteedTable {
                            "obj.bread" count 1
                            "obj.pickpocket_coin_pouch_watchman" count 1
                        },
                ),
            "dbrow.thieving_menaphite_thug" to
                pouchOnly("Menaphite Thug", "obj.pickpocket_coin_pouch_menaphite"),
            "dbrow.thieving_paladin" to
                RSDropTable(
                    tableIdentifier = "Paladin",
                    guaranteed =
                        rsPlayerGuaranteedTable {
                            "obj.pickpocket_coin_pouch_paladin" count 1
                            "obj.chaosrune" count 2
                        },
                ),
            "dbrow.thieving_gnome" to
                RSDropTable(
                    tableIdentifier = "Gnome",
                    mainTable =
                        rsPlayerWeightedTable {
                            56 weight "obj.arrow_shaft" count 2..4
                            30 weight "obj.pickpocket_coin_pouch_gnome" count 1
                            24 weight "obj.swamp_toad" count 1
                            8 weight "obj.gold_ore" count 1
                            5 weight "obj.earthrune" count 1
                            3 weight "obj.king_worm" count 1
                            2 weight "obj.fire_orb" count 1
                        },
                ),
            "dbrow.thieving_hero" to
                RSDropTable(
                    tableIdentifier = "Hero",
                    mainTable =
                        rsPlayerWeightedTable {
                            105 weight "obj.pickpocket_coin_pouch_hero" count 1
                            8 weight "obj.deathrune" count 2
                            6 weight "obj.jug_wine" count 1
                            5 weight "obj.bloodrune" count 1
                            2 weight "obj.fire_orb" count 1
                            1 weight "obj.diamond" count 1
                            1 weight "obj.gold_ore" count 1
                        },
                ),
            "dbrow.thieving_vyre" to
                RSDropTable(
                    tableIdentifier = "Vyre",
                    mainTable =
                        rsPlayerWeightedTable {
                            109 weight "obj.pickpocket_coin_pouch_vyre" count 1
                            8 weight "obj.deathrune" count 2
                            6 weight "obj.blood_pint" count 1
                            5 weight "obj.uncut_ruby" count 1
                            2 weight "obj.bloodrune" count 4
                            1 weight "obj.diamond" count 1
                            1 weight "obj.cooked_mystery_meat" count 1
                        },
                ),
            "dbrow.thieving_elf" to
                RSDropTable(
                    tableIdentifier = "Elf",
                    mainTable =
                        rsPlayerWeightedTable {
                            105 weight "obj.pickpocket_coin_pouch_elf" count 1
                            8 weight "obj.deathrune" count 2
                            6 weight "obj.jug_wine" count 1
                            5 weight "obj.naturerune" count 3
                            2 weight "obj.fire_orb" count 1
                            1 weight "obj.diamond" count 1
                            1 weight "obj.gold_ore" count 1
                        },
                ),
            "dbrow.thieving_tzhaar_hur" to
                RSDropTable(
                    tableIdentifier = "TzHaar-Hur",
                    mainTable =
                        rsPlayerWeightedTable {
                            182 weight "obj.tzhaar_token" count 3..7
                            5 weight "obj.uncut_sapphire" count 1
                            4 weight "obj.uncut_emerald" count 1
                            3 weight "obj.uncut_ruby" count 1
                            1 weight "obj.uncut_diamond" count 1
                        },
                ),
        )

    val stalls: Map<String, ThievingDropTable> =
        mapOf(
            "dbrow.cakethiefstall_thieving" to
                RSDropTable(
                    tableIdentifier = "Baker's stall",
                    mainTable =
                        rsPlayerWeightedTable {
                            6 weight "obj.cake" count 1
                            3 weight "obj.bread" count 1
                            1 weight "obj.chocolate_slice" count 1
                        },
                ),
            "dbrow.tea_stall_thieving" to single("Tea stall", "obj.cup_of_tea"),
            "dbrow.silkthiefstall_thieving" to single("Silk stall", "obj.silk"),
            "dbrow.rag_market_stall_thieving" to
                RSDropTable(
                    tableIdentifier = "Wine stall",
                    mainTable =
                        rsPlayerWeightedTable {
                            39 weight "obj.jug_empty" count 1
                            20 weight "obj.jug_water" count 1
                            17 weight "obj.grapes" count 1
                            13 weight "obj.jug_wine" count 1
                            11 weight "obj.rag_bottle_wine" count 1
                        },
                ),
            "dbrow.seed_stall_thieving" to
                RSDropTable(
                    tableIdentifier = "Seed stall",
                    mainTable =
                        rsPlayerWeightedTable {
                            120 weight "obj.hammerstone_hop_seed" count 1
                            119 weight "obj.potato_seed" count 1
                            119 weight "obj.marigold_seed" count 1
                            118 weight "obj.barley_seed" count 1
                            89 weight "obj.onion_seed" count 1
                            83 weight "obj.asgarnian_hop_seed" count 1
                            71 weight "obj.cabbage_seed" count 1
                            47 weight "obj.yanillian_hop_seed" count 1
                            36 weight "obj.rosemary_seed" count 1
                            35 weight "obj.nasturtium_seed" count 1
                            35 weight "obj.tomato_seed" count 1
                            35 weight "obj.jute_seed" count 1
                            30 weight "obj.sweetcorn_seed" count 1
                            24 weight "obj.krandorian_hop_seed" count 1
                            18 weight "obj.strawberry_seed" count 1
                            12 weight "obj.wildblood_hop_seed" count 1
                            9 weight "obj.watermelon_seed" count 1
                        },
                ),
            "dbrow.furthiefstall_thieving" to single("Fur stall", "obj.grey_wolf_fur"),
            "dbrow.silverthiefstall_thieving" to single("Silver stall", "obj.silver_ore"),
            "dbrow.spicethiefstall_thieving" to single("Spice stall", "obj.spicespot"),
            "dbrow.gemthiefstall_thieving" to
                RSDropTable(
                    tableIdentifier = "Gem stall",
                    mainTable =
                        rsPlayerWeightedTable {
                            100 weight "obj.uncut_sapphire" count 1
                            25 weight "obj.uncut_emerald" count 1
                            12 weight "obj.uncut_ruby" count 1
                            3 weight "obj.uncut_diamond" count 1
                        },
                ),
        )

    private fun pouchOnly(name: String, pouch: String): ThievingDropTable =
        RSDropTable(tableIdentifier = name, guaranteed = rsPlayerGuaranteedTable { pouch count 1 })

    private fun single(name: String, obj: String): ThievingDropTable =
        RSDropTable(tableIdentifier = name, guaranteed = rsPlayerGuaranteedTable { obj count 1 })

    private fun hamMember(name: String): ThievingDropTable =
        RSDropTable(
            tableIdentifier = name,
            mainTable =
                rsPlayerWeightedTable {
                    187 weight "obj.pickpocket_coin_pouch_ham" count 1
                    44 weight "obj.digsitebuttons" count 1
                    44 weight "obj.digsitearmour1" count 1
                    44 weight "obj.digsitesword" count 1
                    33 weight "obj.bronze_arrow" count 1..13
                    33 weight "obj.bronze_axe" count 1
                    33 weight "obj.bronze_dagger" count 1
                    33 weight "obj.bronze_pickaxe" count 1
                    33 weight "obj.iron_axe" count 1
                    33 weight "obj.iron_dagger" count 1
                    33 weight "obj.iron_pickaxe" count 1
                    33 weight "obj.leather_armour" count 1
                    33 weight "obj.feather" count 1..7
                    33 weight "obj.logs" count 1
                    33 weight "obj.thread" count 1..10
                    33 weight "obj.cow_hide" count 1
                    22 weight "obj.steel_arrow" count 1..13
                    22 weight "obj.steel_axe" count 1
                    22 weight "obj.steel_dagger" count 1
                    22 weight "obj.steel_pickaxe" count 1
                    22 weight "obj.knife" count 1
                    22 weight "obj.needle" count 1
                    22 weight "obj.raw_anchovies" count 1
                    22 weight "obj.raw_chicken" count 1
                    22 weight "obj.tinderbox" count 1
                    22 weight "obj.uncut_opal" count 1
                    22 weight "obj.uncut_jade" count 1
                    22 weight "obj.coal" count 1
                    22 weight "obj.iron_ore" count 1
                    11 weight "obj.ham_boots" count 1
                    11 weight "obj.ham_cloak" count 1
                    11 weight "obj.ham_gloves" count 1
                    11 weight "obj.ham_hood" count 1
                    11 weight "obj.ham_badge" count 1
                    11 weight "obj.ham_robe" count 1
                    11 weight "obj.ham_shirt" count 1
                    12 weight "obj.unidentified_guam" count 1
                    6 weight "obj.unidentified_marentill" count 1
                    4 weight "obj.unidentified_tarromin" count 1
                },
        )

    private fun masterFarmerSeeds() =
        rsPlayerWeightedTable {
            17699 weight "obj.potato_seed" count 1..4
            13280 weight "obj.onion_seed" count 1..3
            6944 weight "obj.cabbage_seed" count 1..3
            6369 weight "obj.tomato_seed" count 1..2
            2212 weight "obj.sweetcorn_seed" count 1..2
            1106 weight "obj.strawberry_seed" count 1
            529 weight "obj.watermelon_seed" count 1
            385 weight "obj.snape_grass_seed" count 1
            5556 weight "obj.barley_seed" count 1..12
            5556 weight "obj.hammerstone_hop_seed" count 1..9
            4184 weight "obj.asgarnian_hop_seed" count 1..6
            4149 weight "obj.jute_seed" count 1..9
            2770 weight "obj.yanillian_hop_seed" count 1..6
            1385 weight "obj.krandorian_hop_seed" count 1..6
            704 weight "obj.wildblood_hop_seed" count 1..3
            4587 weight "obj.marigold_seed" count 1
            3040 weight "obj.nasturtium_seed" count 1
            1965 weight "obj.rosemary_seed" count 1
            1451 weight "obj.woad_seed" count 1
            1159 weight "obj.limpwurt_seed" count 1
            3876 weight "obj.redberry_bush_seed" count 1
            2717 weight "obj.cadavaberry_bush_seed" count 1
            1942 weight "obj.dwellberry_bush_seed" count 1
            775 weight "obj.jangerberry_bush_seed" count 1
            282 weight "obj.whiteberry_bush_seed" count 1
            107 weight "obj.poisonivy_bush_seed" count 1
            203 weight "obj.mushroom_seed" count 1
            122 weight "obj.belladonna_seed" count 1
            81 weight "obj.cactus_seed" count 1
            53 weight "obj.seaweed_seed" count 1
            41 weight "obj.potato_cactus_seed" count 1
            1714 weight "obj.guam_seed" count 1
            1046 weight "obj.marrentill_seed" count 1
            714 weight "obj.tarromin_seed" count 1
            485 weight "obj.harralander_seed" count 1
            372 weight "obj.ranarr_seed" count 1
            226 weight "obj.toadflax_seed" count 1
            154 weight "obj.irit_seed" count 1
            106 weight "obj.avantoe_seed" count 1
            72 weight "obj.kwuarm_seed" count 1
            54 weight "obj.snapdragon_seed" count 1
            34 weight "obj.cadantine_seed" count 1
            24 weight "obj.lantadyme_seed" count 1
            14 weight "obj.dwarf_weed_seed" count 1
            11 weight "obj.torstol_seed" count 1
        }
}
