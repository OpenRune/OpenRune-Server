package org.alter.impl

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

enum class Food(
    vararg val items: String,
    val heal: Int,
    val overheal: Boolean = false,
    val comboFood: Boolean = false,
    val hasEffect: Boolean = false,
    val eatDelay: List<Int> = listOf(),
    val combatDelay: List<Int> = listOf(),
    val dbRowId: String
) {
    SHRIMPS("items.shrimp", heal = 3, dbRowId = "dbrows.shrimps_food"),
    COOKED_CHICKEN("items.cooked_chicken", heal = 3, dbRowId = "dbrows.cooked_chicken_food"),
    COOKED_MEAT("items.cooked_meat", heal = 3, dbRowId = "dbrows.cooked_meat_food"),
    BREAD("items.bread", heal = 5, dbRowId = "dbrows.bread_food"),
    HERRING("items.herring", heal = 5, dbRowId = "dbrows.herring_food"),
    MACKEREL("items.mackerel", heal = 6, dbRowId = "dbrows.mackerel_food"),
    TROUT("items.trout", heal = 7, dbRowId = "dbrows.trout_food"),
    PIKE("items.pike", heal = 8, dbRowId = "dbrows.pike_food"),
    PEACH("items.peach", heal = 8, dbRowId = "dbrows.peach_food"),
    SALMON("items.salmon", heal = 9, dbRowId = "dbrows.salmon_food"),
    TUNA("items.tuna", heal = 10, dbRowId = "dbrows.tuna_food"),
    JUG_OF_WINE("items.jug_wine", heal = 11, hasEffect = true, dbRowId = "dbrows.jug_of_wine_food"),
    LOBSTER("items.lobster", heal = 12, dbRowId = "dbrows.lobster_food"),
    BASS("items.bass", heal = 13, dbRowId = "dbrows.bass_food"),
    SWORDFISH("items.swordfish", heal = 14, dbRowId = "dbrows.swordfish_food"),
    IXCOZTIC_WHITE("items.ixcoztic_white", heal = 16, hasEffect = true, dbRowId = "dbrows.ixcoztic_white_food"),
    POTATO_WITH_CHEESE("items.potato_cheese", heal = 16, dbRowId = "dbrows.potato_with_cheese_food"),
    MONKFISH("items.monkfish", heal = 16, dbRowId = "dbrows.monkfish_food"),
    CURRY("items.curry", "items.bowl_empty", heal = 19, dbRowId = "dbrows.curry_food"),
    COOKED_PYRE_FOX("items.curry", heal = 11, dbRowId = "dbrows.cooked_pyre_fox_food"),
    SHARK("items.shark", heal = 20, dbRowId = "dbrows.shark_food"),
    SEA_TURTLE("items.seaturtle", heal = 21, dbRowId = "dbrows.sea_turtle_food"),
    MANTA_RAY("items.mantaray", heal = 22, dbRowId = "dbrows.manta_ray_food"),
    TUNA_POTATO("items.potato_tuna+sweetcorn", heal = 22, dbRowId = "dbrows.tuna_potato_food"),
    DARK_CRAB("items.dark_crab", heal = 22, dbRowId = "dbrows.dark_crab_food"),
    ANGLERFISH("items.anglerfish", heal = -1, overheal = true, dbRowId = "dbrows.anglerfish_food"),
    ONION("items.onion", heal = 1, dbRowId = "dbrows.onion_food"),

    // Cakes
    CAKE(
        "items.cake",
        "items.partial_cake",
        "items.cake_slice",
        heal = 4,
        eatDelay = listOf(2, 2, 3),
        combatDelay = listOf(2, 2, 3),
        dbRowId = "dbrows.cake_food"
    ),
    CHOCOLATE_CAKE(
        "items.chocolate_cake",
        "items.partial_chocolate_cake",
        "items.chocolate_slice",
        heal = 5,
        eatDelay = listOf(2, 2, 3),
        combatDelay = listOf(2, 2, 3),
        dbRowId = "dbrows.chocolate_cake_food"
    ),

    // Pies
    REDBERRY_PIE(
        "items.redberry_pie",
        "items.half_a_redberry_pie",
        "items.piedish",
        heal = 5,
        eatDelay = listOf(1, 2),
        combatDelay = listOf(1, 2),
        dbRowId = "dbrows.redberry_pie_food"
    ),
    MEAT_PIE(
        "items.meat_pie",
        "items.half_a_meat_pie",
        "items.piedish",
        heal = 6,
        eatDelay = listOf(1, 2),
        combatDelay = listOf(1, 2),
        dbRowId = "dbrows.meat_pie_food"
    ),
    GARDEN_PIE(
        "items.garden_pie",
        "items.half_garden_pie",
        "items.piedish",
        heal = 6,
        eatDelay = listOf(1, 1),
        combatDelay = listOf(1, 1),
        hasEffect = true,
        dbRowId = "dbrows.garden_pie_food"
    ),
    FISH_PIE(
        "items.fish_pie",
        "items.half_fish_pie",
        "items.piedish",
        heal = 6,
        eatDelay = listOf(1, 1),
        combatDelay = listOf(1, 1),
        hasEffect = true,
        dbRowId = "dbrows.fish_pie_food"
    ),
    APPLE_PIE(
        "items.apple_pie",
        "items.half_an_apple_pie",
        "items.piedish",
        heal = 7,
        eatDelay = listOf(1, 2),
        combatDelay = listOf(1, 2),
        dbRowId = "dbrows.apple_pie_food"
    ),
    BOTANICAL_PIE(
        "items.botanical_pie",
        "items.half_botanical_pie",
        "items.piedish",
        heal = 7,
        eatDelay = listOf(1, 1),
        combatDelay = listOf(1, 1),
        hasEffect = true,
        dbRowId = "dbrows.botanical_pie_food"
    ),
    MUSHROOM_PIE(
        "items.mushroom_pie",
        "items.half_mushroom_pie",
        "items.piedish",
        heal = 8,
        eatDelay = listOf(1, 1),
        combatDelay = listOf(1, 1),
        hasEffect = true,
        dbRowId = "dbrows.mushroom_pie_food"
    ),
    ADMIRAL_PIE(
        "items.admiral_pie",
        "items.half_admiral_pie",
        "items.piedish",
        heal = 8,
        eatDelay = listOf(1, 1),
        combatDelay = listOf(1, 1),
        hasEffect = true,
        dbRowId = "dbrows.admiral_pie_food"
    ),

    // Pizzas
    PLAIN_PIZZA(
        "items.plain_pizza",
        "items.half_plain_pizza",
        heal = 3,
        eatDelay = listOf(1, 2),
        combatDelay = listOf(3),
        dbRowId = "dbrows.plain_pizza_food"
    ),
    MEAT_PIZZA(
        "items.meat_pizza",
        "items.half_meat_pizza",
        heal = 3,
        eatDelay = listOf(1, 2),
        combatDelay = listOf(3),
        dbRowId = "dbrows.meat_pizza_food"
    ),
    ANCHOVY_PIZZA(
        "items.anchovie_pizza",
        "items.half_anchovie_pizza",
        heal = 2,
        eatDelay = listOf(1, 2),
        combatDelay = listOf(3),
        dbRowId = "dbrows.anchovy_pizza_food"
    ),
    DRAGONFRUIT_PIE(
        "items.dragonfruit_pie",
        "items.half_dragonfruit_pie",
        heal = 10,
        eatDelay = listOf(1, 1),
        combatDelay = listOf(1, 1),
        hasEffect = true,
        dbRowId = "dbrows.dragonfruit_pie_food"
    ),
    PINEAPPLE_PIZZA(
        "items.pineapple_pizza",
        "items.half_pineapple_pizza",
        heal = 11,
        eatDelay = listOf(1, 2),
        combatDelay = listOf(3),
        dbRowId = "dbrows.pineapple_pizza_food"
    ),
    WILD_PIE(
        "items.wild_pie",
        "items.half_wild_pie",
        heal = 11,
        eatDelay = listOf(1, 1),
        combatDelay = listOf(1, 1),
        hasEffect = true,
        dbRowId = "dbrows.wild_pie_food"
    ),
    SUMMER_PIE(
        "items.summer_pie",
        "items.half_summer_pie",
        heal = 11,
        eatDelay = listOf(1, 1),
        combatDelay = listOf(1, 1),
        hasEffect = true,
        dbRowId = "dbrows.summer_pie_food"
    ),

    // Crunchies (combo foods)
    TOAD_CRUNCHIES(
        "items.toad_crunchies",
        heal = 8,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.toad_crunchies_food"
    ),
    PREMADE_TD_CRUNCH(
        "items.premade_toad_crunchies",
        heal = 8,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.premade_td_crunch_food"
    ),
    SPICY_CRUNCHIES(
        "items.spicy_crunchies",
        heal = 8,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.spicy_crunchies_food"
    ),
    PREMADE_SY_CRUNCH(
        "items.premade_spicy_crunchies",
        heal = 8,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.premade_sy_crunch_food"
    ),
    WORM_CRUNCHIES(
        "items.worm_crunchies",
        heal = 8,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.worm_crunchies_food"
    ),
    PREMADE_WM_CRUN(
        "items.premade_worm_crunchies",
        heal = 8,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.premade_wm_crun_food"
    ),
    CHOCCHIP_CRUNCHIES(
        "items.chocchip_crunchies",
        heal = 7,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.chocchip_crunchies_food"
    ),
    PREMADE_CH_CRUNCH(
        "items.premade_chocchip_crunchies",
        heal = 7,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.premade_ch_crunch_food"
    ),

    // Batta
    FRUIT_BATTA(
        "items.fruit_batta",
        heal = 11,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.fruit_batta_food"
    ),
    PREMADE_FRT_BATTA(
        "items.premade_fruit_batta",
        heal = 11,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.premade_frt_batta_food"
    ),
    TOAD_BATTA(
        "items.toad_batta",
        heal = 11,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.toad_batta_food"
    ),
    PREMADE_TD_BATTA(
        "items.premade_toad_batta",
        heal = 11,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.premade_td_batta_food"
    ),
    WORM_BATTA(
        "items.worm_batta",
        heal = 2,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.worm_batta_food"
    ),
    PREMADE_WM_BATTA(
        "items.premade_worm_batta",
        heal = 11,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.premade_wm_batta_food"
    ),
    VEGETABLE_BATTA(
        "items.vegetable_batta",
        heal = 11,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.vegetable_batta_food"
    ),
    PREMADE_VEG_BATTA(
        "items.premade_vegetable_batta",
        heal = 11,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.premade_veg_batta_food"
    ),
    CHEESE_TOM_BATTA(
        "items.cheese+tom_batta",
        heal = 11,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.cheese_tom_batta_food"
    ),
    PREMADE_CT_BATTA(
        "items.premade_cheese+tom_batta",
        heal = 11,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.premade_ct_batta_food"
    ),

    // Special combo foods
    WORM_HOLE(
        "items.worm_hole",
        heal = 12,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.worm_hole_food"
    ),
    PREMADE_WORM_HOLE(
        "items.premade_worm_hole",
        heal = 12,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.premade_worm_hole_food"
    ),
    VEG_BALL(
        "items.veg_ball",
        heal = 12,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.veg_ball_food"
    ),
    PREMADE_VEG_BALL(
        "items.premade_veg_ball",
        heal = 12,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.premade_veg_ball_food"
    ),
    CHOCOLATE_BOMB(
        "items.chocolate_bomb",
        heal = 15,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.chocolate_bomb_food"
    ),
    PREMADE_CHOC_BOMB(
        "items.premade_chocolate_bomb",
        heal = 15,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.premade_choc_bomb_food"
    ),
    TANGLED_TOADS_LEGS(
        "items.tangled_toads_legs",
        heal = 15,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.tangled_toads_legs_food"
    ),
    PREMADE_TTL(
        "items.premade_tangled_toads_legs",
        heal = 15,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.premade_ttl_food"
    ),
    TOADS_LEGS(
        "items.toads_legs",
        heal = 3,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.toads_legs_food"
    ),
    CRYSTAL_PADDLEFISH(
        "items.gauntlet_combo_food",
        heal = 16,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.crystal_paddlefish_food"
    ),
    CORRUPTED_PADDLEFISH(
        "items.gauntlet_combo_food_hm",
        heal = 16,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.corrupted_paddlefish_food"
    ),
    COOKED_KARAMBWAN(
        "items.tbwt_cooked_karambwan",
        heal = 18,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.cooked_karambwan_food"
    ),
    BLIGHTED_KARAMBWAN(
        "items.blighted_karambwan",
        heal = 18,
        eatDelay = listOf(2),
        combatDelay = listOf(3),
        comboFood = true,
        dbRowId = "dbrows.blighted_karambwan_food"
    ),

    // Blighted and other special foods
    BLIGHTED_MANTA_RAY("items.blighted_mantaray", heal = 22, dbRowId = "dbrows.blighted_manta_ray_food"),
    BLIGHTED_ANGLERFISH(
        "items.blighted_anglerfish",
        heal = -1,
        overheal = true,
        dbRowId = "dbrows.blighted_anglerfish_food"
    ),
    SWEETS("items.trail_sweets", heal = -1, dbRowId = "dbrows.sweets_food"),
    MOONLIGHT_MEAD(
        "items.keg_mature_moonlight_mead_1",
        "items.keg_mature_moonlight_mead_2",
        "items.keg_mature_moonlight_mead_3",
        "items.cert_keg_mature_moonlight_mead_4",
        heal = 6,
        dbRowId = "dbrows.moonlight_mead_food"
    );
}


object FoodTable {


    const val ITEMS = 0
    const val HEAL = 1
    const val COMBO_FOOD = 2
    const val HAS_EFFECT = 3
    const val OVERHEAL = 4
    const val EAT_DELAY = 5
    const val COMBAT_DELAY = 6

    fun consumableFood() = dbTable("tables.consumable_food") {

        column("items", ITEMS, VarType.OBJ)
        column("heal", HEAL, VarType.INT)

        column("combo", COMBO_FOOD, VarType.BOOLEAN)
        column("effect", HAS_EFFECT, VarType.BOOLEAN)
        column("overheal", OVERHEAL, VarType.BOOLEAN)
        column("eatDelay", EAT_DELAY, VarType.INT)
        column("combatDelay", COMBAT_DELAY, VarType.INT)

        Food.entries.forEach { food ->
            row(food.dbRowId) {

                columnRSCM(ITEMS, *food.items)

                column(HEAL, food.heal)
                column(COMBO_FOOD, food.comboFood)
                column(HAS_EFFECT, food.hasEffect)
                column(OVERHEAL, food.overheal)

                if (food.eatDelay.isNotEmpty()) {
                    column(EAT_DELAY, food.eatDelay)
                }
                if (food.combatDelay.isNotEmpty()) {
                    column(COMBAT_DELAY, food.combatDelay)
                }

            }
        }
    }

}