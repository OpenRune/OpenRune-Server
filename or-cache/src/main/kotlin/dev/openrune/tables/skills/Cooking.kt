package dev.openrune.tables.skills

import dev.openrune.definition.util.VarType
import dev.openrune.tables.production.productionTable

object Cooking {

    const val COL_BURNT = 7
    const val COL_STOP_BURN_FIRE = 8
    const val COL_STOP_BURN_RANGE = 9
    const val COL_LOW = 10
    const val COL_HIGH = 11
    const val COL_GAUNTLET = 12

    const val COL_VAT_OFFSET = 7
    const val COL_BARREL_OFFSET = 8

    fun ales() = productionTable(
        "dbtable.cooking_ales",
        serverOnly = true,
        defaultCategory = "Brew",
        extraColumns = {
            column("vat_offset", COL_VAT_OFFSET, VarType.INT)
            column("barrel_offset", COL_BARREL_OFFSET, VarType.INT)
        },
    ) {
        row("dbrow.ale_dwarven_stout") {
            production {
                input("obj.hammerstone_hops", 4)
                statReq("stat.cooking", 19)
                xp(215)
                output("obj.dwarven_stout")
                output("obj.mature_dwarven_stout")
            }
            column(COL_VAT_OFFSET, 4)
            column(COL_BARREL_OFFSET, 4)
        }
        row("dbrow.ale_asgarnian_ale") {
            production {
                input("obj.hammerstone_hops", 4)
                statReq("stat.cooking", 24)
                xp(248)
                output("obj.asgarnian_ale")
                output("obj.mature_asgarnian_ale")
            }
            column(COL_VAT_OFFSET, 10)
            column(COL_BARREL_OFFSET, 6)
        }
        row("dbrow.ale_greenmans_ale") {
            production {
                input("obj.hammerstone_hops", 4)
                statReq("stat.cooking", 29)
                xp(281)
                output("obj.greenmans_ale")
                output("obj.mature_greenmans_ale")
            }
            column(COL_VAT_OFFSET, 16)
            column(COL_BARREL_OFFSET, 8)
        }
        row("dbrow.ale_wizards_mind_bomb") {
            production {
                input("obj.yanillian_hops", 4)
                statReq("stat.cooking", 34)
                xp(314)
                output("obj.wizards_mind_bomb")
                output("obj.mature_wizards_mind_bomb")
            }
            column(COL_VAT_OFFSET, 22)
            column(COL_BARREL_OFFSET, 10)
        }
        row("dbrow.ale_dragon_bitter") {
            production {
                input("obj.krandorian_hops", 4)
                statReq("stat.cooking", 39)
                xp(347)
                output("obj.dragon_bitter")
                output("obj.mature_dragon_bitter")
            }
            column(COL_VAT_OFFSET, 28)
            column(COL_BARREL_OFFSET, 12)
        }
        row("dbrow.ale_moonlight_mead") {
            production {
                input("obj.bittercap_mushroom", 4)
                statReq("stat.cooking", 44)
                xp(380)
                output("obj.moonlight_mead")
                output("obj.mature_moonlight_mead")
            }
            column(COL_VAT_OFFSET, 34)
            column(COL_BARREL_OFFSET, 14)
        }
        row("dbrow.ale_axemans_folly") {
            production {
                input("obj.oak_roots", 1)
                statReq("stat.cooking", 49)
                xp(413)
                output("obj.axemans_folly")
                output("obj.mature_axemans_folly")
            }
            column(COL_VAT_OFFSET, 40)
            column(COL_BARREL_OFFSET, 16)
        }
        row("dbrow.ale_chefs_delight") {
            production {
                input("obj.chocolate_dust", 2)
                statReq("stat.cooking", 54)
                xp(446)
                output("obj.chefs_delight")
                output("obj.mature_chefs_delight")
            }
            column(COL_VAT_OFFSET, 46)
            column(COL_BARREL_OFFSET, 18)
        }
        row("dbrow.ale_slayers_respite") {
            production {
                input("obj.wildblood_hops", 4)
                statReq("stat.cooking", 59)
                xp(479)
                output("obj.slayers_respite")
                output("obj.mature_slayers_respite")
            }
            column(COL_VAT_OFFSET, 52)
            column(COL_BARREL_OFFSET, 20)
        }
        row("dbrow.ale_cider") {
            production {
                input("obj.apple_mush", 4)
                statReq("stat.cooking", 14)
                xp(182)
                output("obj.cider")
                output("obj.mature_cider")
            }
            column(COL_VAT_OFFSET, 58)
            column(COL_BARREL_OFFSET, 22)
        }
    }

    fun foods() = productionTable(
        "dbtable.cooking_foods",
        serverOnly = true,
        extraColumns = {
            column("burnt", COL_BURNT, VarType.OBJ)
            column("stop_burn_fire", COL_STOP_BURN_FIRE, VarType.INT)
            column("stop_burn_range", COL_STOP_BURN_RANGE, VarType.INT)
            column("low", COL_LOW, VarType.INT)
            column("high", COL_HIGH, VarType.INT)
            column("supports_gauntlet", COL_GAUNTLET, VarType.BOOLEAN)
        },
    ) {
        row("dbrow.cooking_shrimp") {
            production {
                input("obj.raw_shrimp")
                statReq("stat.cooking", 1)
                xp(30)
                output("obj.shrimp")
                category("Fish")
            }
            columnRSCM(COL_BURNT, "obj.burnt_shrimp")
            column(COL_STOP_BURN_FIRE, 34)
            column(COL_STOP_BURN_RANGE, 34)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_anchovies") {
            production {
                input("obj.raw_anchovies")
                statReq("stat.cooking", 1)
                xp(30)
                output("obj.anchovies")
                category("Fish")
            }
            columnRSCM(COL_BURNT, "obj.burntfish1")
            column(COL_STOP_BURN_FIRE, 34)
            column(COL_STOP_BURN_RANGE, 34)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_sardine") {
            production {
                input("obj.raw_sardine")
                statReq("stat.cooking", 1)
                xp(40)
                output("obj.sardine")
                category("Fish")
            }
            columnRSCM(COL_BURNT, "obj.burntfish1")
            column(COL_STOP_BURN_FIRE, 38)
            column(COL_STOP_BURN_RANGE, 38)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_herring") {
            production {
                input("obj.raw_herring")
                statReq("stat.cooking", 5)
                xp(50)
                output("obj.herring")
                category("Fish")
            }
            columnRSCM(COL_BURNT, "obj.burntfish2")
            column(COL_STOP_BURN_FIRE, 41)
            column(COL_STOP_BURN_RANGE, 41)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_mackerel") {
            production {
                input("obj.raw_mackerel")
                statReq("stat.cooking", 10)
                xp(60)
                output("obj.mackerel")
                category("Fish")
            }
            columnRSCM(COL_BURNT, "obj.burntfish3")
            column(COL_STOP_BURN_FIRE, 45)
            column(COL_STOP_BURN_RANGE, 45)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_trout") {
            production {
                input("obj.raw_trout")
                statReq("stat.cooking", 15)
                xp(70)
                output("obj.trout")
                category("Fish")
            }
            columnRSCM(COL_BURNT, "obj.burntfish3")
            column(COL_STOP_BURN_FIRE, 49)
            column(COL_STOP_BURN_RANGE, 49)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_cod") {
            production {
                input("obj.raw_cod")
                statReq("stat.cooking", 18)
                xp(75)
                output("obj.cod")
                category("Fish")
            }
            columnRSCM(COL_BURNT, "obj.burntfish3")
            column(COL_STOP_BURN_FIRE, 52)
            column(COL_STOP_BURN_RANGE, 52)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_pike") {
            production {
                input("obj.raw_pike")
                statReq("stat.cooking", 20)
                xp(80)
                output("obj.pike")
                category("Fish")
            }
            columnRSCM(COL_BURNT, "obj.burntfish3")
            column(COL_STOP_BURN_FIRE, 64)
            column(COL_STOP_BURN_RANGE, 64)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_salmon") {
            production {
                input("obj.raw_salmon")
                statReq("stat.cooking", 25)
                xp(90)
                output("obj.salmon")
                category("Fish")
            }
            columnRSCM(COL_BURNT, "obj.burntfish3")
            column(COL_STOP_BURN_FIRE, 58)
            column(COL_STOP_BURN_RANGE, 58)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_tuna") {
            production {
                input("obj.raw_tuna")
                statReq("stat.cooking", 30)
                xp(100)
                output("obj.tuna")
                category("Fish")
            }
            columnRSCM(COL_BURNT, "obj.burntfish4")
            column(COL_STOP_BURN_FIRE, 63)
            column(COL_STOP_BURN_RANGE, 63)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_lobster") {
            production {
                input("obj.raw_lobster")
                statReq("stat.cooking", 40)
                xp(120)
                output("obj.lobster")
                category("Fish")
            }
            columnRSCM(COL_BURNT, "obj.burnt_lobster")
            column(COL_STOP_BURN_FIRE, 74)
            column(COL_STOP_BURN_RANGE, 68)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
            column(COL_GAUNTLET, true)
        }
        row("dbrow.cooking_bass") {
            production {
                input("obj.raw_bass")
                statReq("stat.cooking", 43)
                xp(130)
                output("obj.bass")
                category("Fish")
            }
            columnRSCM(COL_BURNT, "obj.burntfish5")
            column(COL_STOP_BURN_FIRE, 80)
            column(COL_STOP_BURN_RANGE, 80)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_swordfish") {
            production {
                input("obj.raw_swordfish")
                statReq("stat.cooking", 45)
                xp(140)
                output("obj.swordfish")
                category("Fish")
            }
            columnRSCM(COL_BURNT, "obj.burnt_swordfish")
            column(COL_STOP_BURN_FIRE, 86)
            column(COL_STOP_BURN_RANGE, 81)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
            column(COL_GAUNTLET, true)
        }
        row("dbrow.cooking_monkfish") {
            production {
                input("obj.raw_monkfish")
                statReq("stat.cooking", 62)
                xp(150)
                output("obj.monkfish")
                category("Fish")
            }
            columnRSCM(COL_BURNT, "obj.burnt_monkfish")
            column(COL_STOP_BURN_FIRE, 92)
            column(COL_STOP_BURN_RANGE, 90)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
            column(COL_GAUNTLET, true)
        }
        row("dbrow.cooking_shark") {
            production {
                input("obj.raw_shark")
                statReq("stat.cooking", 80)
                xp(210)
                output("obj.shark")
                category("Fish")
            }
            columnRSCM(COL_BURNT, "obj.burnt_shark")
            column(COL_STOP_BURN_FIRE, 99)
            column(COL_STOP_BURN_RANGE, 94)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
            column(COL_GAUNTLET, true)
        }
        row("dbrow.cooking_manta_ray") {
            production {
                input("obj.raw_mantaray")
                statReq("stat.cooking", 91)
                xp(216)
                output("obj.mantaray")
                category("Fish")
            }
            columnRSCM(COL_BURNT, "obj.burnt_mantaray")
            column(COL_STOP_BURN_FIRE, 99)
            column(COL_STOP_BURN_RANGE, 99)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
            column(COL_GAUNTLET, true)
        }
        row("dbrow.cooking_sea_turtle") {
            production {
                input("obj.raw_seaturtle")
                statReq("stat.cooking", 82)
                xp(211)
                output("obj.seaturtle")
                category("Fish")
            }
            columnRSCM(COL_BURNT, "obj.burnt_seaturtle")
            column(COL_STOP_BURN_FIRE, 99)
            column(COL_STOP_BURN_RANGE, 99)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
            column(COL_GAUNTLET, true)
        }
        row("dbrow.cooking_dark_crab") {
            production {
                input("obj.raw_dark_crab")
                statReq("stat.cooking", 90)
                xp(215)
                output("obj.dark_crab")
                category("Fish")
            }
            columnRSCM(COL_BURNT, "obj.burnt_dark_crab")
            column(COL_STOP_BURN_FIRE, 99)
            column(COL_STOP_BURN_RANGE, 99)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
            column(COL_GAUNTLET, true)
        }
        row("dbrow.cooking_anglerfish") {
            production {
                input("obj.raw_anglerfish")
                statReq("stat.cooking", 84)
                xp(230)
                output("obj.anglerfish")
                category("Fish")
            }
            columnRSCM(COL_BURNT, "obj.burnt_anglerfish")
            column(COL_STOP_BURN_FIRE, 99)
            column(COL_STOP_BURN_RANGE, 99)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
            column(COL_GAUNTLET, true)
        }
        row("dbrow.cooking_chicken") {
            production {
                input("obj.raw_chicken")
                statReq("stat.cooking", 1)
                xp(30)
                output("obj.cooked_chicken")
                category("Meat")
            }
            columnRSCM(COL_BURNT, "obj.burnt_chicken")
            column(COL_STOP_BURN_FIRE, 34)
            column(COL_STOP_BURN_RANGE, 34)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_meat") {
            production {
                input("obj.raw_beef")
                statReq("stat.cooking", 1)
                xp(30)
                output("obj.cooked_meat")
                category("Meat")
            }
            columnRSCM(COL_BURNT, "obj.burnt_meat")
            column(COL_STOP_BURN_FIRE, 34)
            column(COL_STOP_BURN_RANGE, 34)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_bear_meat") {
            production {
                input("obj.raw_bear_meat")
                statReq("stat.cooking", 1)
                xp(30)
                output("obj.cooked_meat")
                category("Meat")
            }
            columnRSCM(COL_BURNT, "obj.burnt_meat")
            column(COL_STOP_BURN_FIRE, 34)
            column(COL_STOP_BURN_RANGE, 34)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_rat_meat") {
            production {
                input("obj.raw_rat_meat")
                statReq("stat.cooking", 1)
                xp(30)
                output("obj.cooked_meat")
                category("Meat")
            }
            columnRSCM(COL_BURNT, "obj.burnt_meat")
            column(COL_STOP_BURN_FIRE, 34)
            column(COL_STOP_BURN_RANGE, 34)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_rabbit") {
            production {
                input("obj.raw_rabbit")
                statReq("stat.cooking", 1)
                xp(30)
                output("obj.cooked_rabbit")
                category("Meat")
            }
            columnRSCM(COL_BURNT, "obj.burnt_meat")
            column(COL_STOP_BURN_FIRE, 34)
            column(COL_STOP_BURN_RANGE, 34)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_cave_eel") {
            production {
                input("obj.raw_cave_eel")
                statReq("stat.cooking", 38)
                xp(115)
                output("obj.cave_eel")
                category("Fish")
            }
            columnRSCM(COL_BURNT, "obj.burnt_cave_eel")
            column(COL_STOP_BURN_FIRE, 74)
            column(COL_STOP_BURN_RANGE, 74)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_lava_eel") {
            production {
                input("obj.raw_lava_eel")
                statReq("stat.cooking", 53)
                xp(30)
                output("obj.lava_eel")
                category("Fish")
            }
            columnRSCM(COL_BURNT, "obj.burnt_eel")
            column(COL_STOP_BURN_FIRE, 53)
            column(COL_STOP_BURN_RANGE, 53)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_karambwan") {
            production {
                input("obj.tbwt_raw_karambwan")
                statReq("stat.cooking", 30)
                xp(190)
                output("obj.tbwt_cooked_karambwan")
                category("Fish")
            }
            columnRSCM(COL_BURNT, "obj.tbwt_burnt_karambwan")
            column(COL_STOP_BURN_FIRE, 99)
            column(COL_STOP_BURN_RANGE, 99)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_bread") {
            production {
                input("obj.bread_dough")
                statReq("stat.cooking", 1)
                xp(40)
                output("obj.bread")
                category("Bread")
            }
            columnRSCM(COL_BURNT, "obj.burnt_bread")
            column(COL_STOP_BURN_FIRE, 34)
            column(COL_STOP_BURN_RANGE, 34)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_redberry_pie") {
            production {
                input("obj.uncooked_redberry_pie")
                statReq("stat.cooking", 10)
                xp(78)
                output("obj.redberry_pie")
                category("Pie")
            }
            columnRSCM(COL_BURNT, "obj.burnt_pie")
            column(COL_STOP_BURN_FIRE, 50)
            column(COL_STOP_BURN_RANGE, 50)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_meat_pie") {
            production {
                input("obj.uncooked_meat_pie")
                statReq("stat.cooking", 20)
                xp(110)
                output("obj.meat_pie")
                category("Pie")
            }
            columnRSCM(COL_BURNT, "obj.burnt_pie")
            column(COL_STOP_BURN_FIRE, 50)
            column(COL_STOP_BURN_RANGE, 50)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_apple_pie") {
            production {
                input("obj.uncooked_apple_pie")
                statReq("stat.cooking", 30)
                xp(130)
                output("obj.apple_pie")
                category("Pie")
            }
            columnRSCM(COL_BURNT, "obj.burnt_pie")
            column(COL_STOP_BURN_FIRE, 50)
            column(COL_STOP_BURN_RANGE, 50)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_pizza") {
            production {
                input("obj.uncooked_pizza")
                statReq("stat.cooking", 35)
                xp(143)
                output("obj.plain_pizza")
                category("Pizza")
            }
            columnRSCM(COL_BURNT, "obj.burnt_pizza")
            column(COL_STOP_BURN_FIRE, 68)
            column(COL_STOP_BURN_RANGE, 68)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_mud_pie") {
            production {
                input("obj.uncooked_mud_pie")
                statReq("stat.cooking", 29)
                xp(128)
                output("obj.mud_pie")
                category("Pie")
            }
            columnRSCM(COL_BURNT, "obj.burnt_pie")
            column(COL_STOP_BURN_FIRE, 50)
            column(COL_STOP_BURN_RANGE, 50)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_garden_pie") {
            production {
                input("obj.uncooked_garden_pie")
                statReq("stat.cooking", 34)
                xp(138)
                output("obj.garden_pie")
                category("Pie")
            }
            columnRSCM(COL_BURNT, "obj.burnt_pie")
            column(COL_STOP_BURN_FIRE, 68)
            column(COL_STOP_BURN_RANGE, 68)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_fish_pie") {
            production {
                input("obj.uncooked_fish_pie")
                statReq("stat.cooking", 47)
                xp(164)
                output("obj.fish_pie")
                category("Pie")
            }
            columnRSCM(COL_BURNT, "obj.burnt_pie")
            column(COL_STOP_BURN_FIRE, 80)
            column(COL_STOP_BURN_RANGE, 80)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_botanical_pie") {
            production {
                input("obj.uncooked_botanical_pie")
                statReq("stat.cooking", 52)
                xp(180)
                output("obj.botanical_pie")
                category("Pie")
            }
            columnRSCM(COL_BURNT, "obj.burnt_pie")
            column(COL_STOP_BURN_FIRE, 85)
            column(COL_STOP_BURN_RANGE, 85)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_mushroom_pie") {
            production {
                input("obj.uncooked_mushroom_pie")
                statReq("stat.cooking", 60)
                xp(200)
                output("obj.mushroom_pie")
                category("Pie")
            }
            columnRSCM(COL_BURNT, "obj.burnt_pie")
            column(COL_STOP_BURN_FIRE, 90)
            column(COL_STOP_BURN_RANGE, 90)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_admiral_pie") {
            production {
                input("obj.uncooked_admiral_pie")
                statReq("stat.cooking", 70)
                xp(210)
                output("obj.admiral_pie")
                category("Pie")
            }
            columnRSCM(COL_BURNT, "obj.burnt_pie")
            column(COL_STOP_BURN_FIRE, 94)
            column(COL_STOP_BURN_RANGE, 94)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_dragonfruit_pie") {
            production {
                input("obj.uncooked_dragonfruit_pie")
                statReq("stat.cooking", 73)
                xp(210)
                output("obj.dragonfruit_pie")
                category("Pie")
            }
            columnRSCM(COL_BURNT, "obj.burnt_pie")
            column(COL_STOP_BURN_FIRE, 97)
            column(COL_STOP_BURN_RANGE, 97)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_wild_pie") {
            production {
                input("obj.uncooked_wild_pie")
                statReq("stat.cooking", 85)
                xp(240)
                output("obj.wild_pie")
                category("Pie")
            }
            columnRSCM(COL_BURNT, "obj.burnt_pie")
            column(COL_STOP_BURN_FIRE, 99)
            column(COL_STOP_BURN_RANGE, 99)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_summer_pie") {
            production {
                input("obj.uncooked_summer_pie")
                statReq("stat.cooking", 95)
                xp(260)
                output("obj.summer_pie")
                category("Pie")
            }
            columnRSCM(COL_BURNT, "obj.burnt_pie")
            column(COL_STOP_BURN_FIRE, 99)
            column(COL_STOP_BURN_RANGE, 99)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_stew") {
            production {
                input("obj.uncooked_stew")
                statReq("stat.cooking", 25)
                xp(117)
                output("obj.stew")
                category("Stew")
            }
            columnRSCM(COL_BURNT, "obj.burnt_stew")
            column(COL_STOP_BURN_FIRE, 58)
            column(COL_STOP_BURN_RANGE, 58)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }
        row("dbrow.cooking_cake") {
            production {
                input("obj.uncooked_cake")
                statReq("stat.cooking", 40)
                xp(180)
                output("obj.cake")
                category("Cake")
            }
            columnRSCM(COL_BURNT, "obj.burnt_cake")
            column(COL_STOP_BURN_FIRE, 74)
            column(COL_STOP_BURN_RANGE, 74)
            column(COL_LOW, 38)
            column(COL_HIGH, 332)
        }
    }
}
