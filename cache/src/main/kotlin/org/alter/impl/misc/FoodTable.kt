package org.alter.impl.misc

import dev.openrune.cache.VARBIT
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
    // Basic fish
    SHRIMPS("items.shrimp", heal = 3, dbRowId = "dbrows.shrimps_food"),
    ANCHOVIES("items.anchovies", heal = 1, dbRowId = "dbrows.anchovies_food"),
    SARDINE("items.sardine", heal = 4, dbRowId = "dbrows.sardine_food"),
    COOKED_CHICKEN("items.cooked_chicken", heal = 3, dbRowId = "dbrows.cooked_chicken_food"),
    COOKED_MEAT("items.cooked_meat", heal = 3, dbRowId = "dbrows.cooked_meat_food"),
    BREAD("items.bread", heal = 5, dbRowId = "dbrows.bread_food"),
    HERRING("items.herring", heal = 5, dbRowId = "dbrows.herring_food"),
    MACKEREL("items.mackerel", heal = 6, dbRowId = "dbrows.mackerel_food"),
    COD("items.cod", heal = 7, dbRowId = "dbrows.cod_food"),
    TROUT("items.trout", heal = 7, dbRowId = "dbrows.trout_food"),
    PIKE("items.pike", heal = 8, dbRowId = "dbrows.pike_food"),
    PEACH("items.peach", heal = 8, dbRowId = "dbrows.peach_food"),
    SALMON("items.salmon", heal = 9, dbRowId = "dbrows.salmon_food"),
    TUNA("items.tuna", heal = 10, dbRowId = "dbrows.tuna_food"),
    CAVE_EEL("items.cave_eel", heal = 10, dbRowId = "dbrows.cave_eel_food"),
    LAVA_EEL("items.lava_eel", heal = 11, dbRowId = "dbrows.lava_eel_food"),
    JUG_OF_WINE("items.jug_wine", heal = 11, hasEffect = true, dbRowId = "dbrows.jug_of_wine_food"),
    LOBSTER("items.lobster", heal = 12, dbRowId = "dbrows.lobster_food"),
    BASS("items.bass", heal = 13, dbRowId = "dbrows.bass_food"),
    SWORDFISH("items.swordfish", heal = 14, dbRowId = "dbrows.swordfish_food"),
    SWORDTIP_SQUID("items.swordtip_squid", heal = 15, dbRowId = "dbrows.swordtip_squid_food"),
    IXCOZTIC_WHITE("items.ixcoztic_white", heal = 16, hasEffect = true, dbRowId = "dbrows.ixcoztic_white_food"),
    POTATO_WITH_CHEESE("items.potato_cheese", heal = 16, dbRowId = "dbrows.potato_with_cheese_food"),
    MONKFISH("items.monkfish", heal = 16, dbRowId = "dbrows.monkfish_food"),
    JUMBO_SQUID("items.jumbo_squid", heal = 17, dbRowId = "dbrows.jumbo_squid_food"),
    GIANT_KRILL("items.giant_krill", heal = 17, dbRowId = "dbrows.giant_krill_food"),
    HADDOCK("items.haddock", heal = 18, dbRowId = "dbrows.haddock_food"),
    CURRY("items.curry", "items.bowl_empty", heal = 19, dbRowId = "dbrows.curry_food"),
    COOKED_PYRE_FOX("items.curry", heal = 11, dbRowId = "dbrows.cooked_pyre_fox_food"),
    UGTHANKI_KEBAB("items.ugthanki_kebab", heal = 19, dbRowId = "dbrows.ugthanki_kebab_food"),
    YELLOWFIN("items.yellowfin", heal = 19, dbRowId = "dbrows.yellowfin_food"),
    SHARK("items.shark", heal = 20, dbRowId = "dbrows.shark_food"),
    HALIBUT("items.halibut", heal = 20, dbRowId = "dbrows.halibut_food"),
    SEA_TURTLE("items.seaturtle", heal = 21, dbRowId = "dbrows.sea_turtle_food"),
    MANTA_RAY("items.mantaray", heal = 22, dbRowId = "dbrows.manta_ray_food"),
    TUNA_POTATO("items.potato_tuna+sweetcorn", heal = 22, dbRowId = "dbrows.tuna_potato_food"),
    DARK_CRAB("items.dark_crab", heal = 22, dbRowId = "dbrows.dark_crab_food"),
    BLUEFIN("items.bluefin", heal = 22, dbRowId = "dbrows.bluefin_food"),
    MARLIN("items.marlin", heal = 24, dbRowId = "dbrows.marlin_food"),
    CAVEFISH("items.cavefish", heal = 20, dbRowId = "dbrows.cavefish_food"),
    TETRA("items.tetra", heal = 22, dbRowId = "dbrows.tetra_food"),
    ANGLERFISH("items.anglerfish", heal = -1, overheal = true, dbRowId = "dbrows.anglerfish_food"),

    // Basic foods & produce
    ONION("items.onion", heal = 1, dbRowId = "dbrows.onion_food"),
    POTATO("items.potato", heal = 1, dbRowId = "dbrows.potato_food"),
    CABBAGE("items.cabbage", heal = 1, dbRowId = "dbrows.cabbage_food"),
    BANANA("items.banana", heal = 2, dbRowId = "dbrows.banana_food"),
    TOMATO("items.tomato", heal = 2, dbRowId = "dbrows.tomato_food"),
    CHEESE("items.cheese", heal = 2, dbRowId = "dbrows.cheese_food"),
    LEMON("items.lemon", heal = 2, dbRowId = "dbrows.lemon_food"),
    ORANGE("items.orange", heal = 2, dbRowId = "dbrows.orange_food"),
    LIME("items.lime", heal = 2, dbRowId = "dbrows.lime_food"),
    PINEAPPLE("items.pineapple", heal = 2, dbRowId = "dbrows.pineapple_food"),
    DWELLBERRIES("items.dwellberries", heal = 2, dbRowId = "dbrows.dwellberries_food"),
    JANGERBERRIES("items.jangerberries", heal = 2, dbRowId = "dbrows.jangerberries_food"),
    CAERULA_BERRIES("items.caerula_berries", heal = 2, dbRowId = "dbrows.caerula_berries_food"),
    STRAWBERRY("items.strawberry", heal = 5, dbRowId = "dbrows.strawberry_food"),
    WATERMELON_SLICE("items.watermelon_slice", heal = 2, dbRowId = "dbrows.watermelon_slice_food"),
    PAPAYA_FRUIT("items.papaya", heal = 8, dbRowId = "dbrows.papaya_fruit_food"),
    DRAGONFRUIT("items.dragonfruit", heal = 10, dbRowId = "dbrows.dragonfruit_food"),
    CHOCOLATE_BAR("items.chocolate_bar", heal = 3, dbRowId = "dbrows.chocolate_bar_food"),
    EDIBLE_SEAWEED("items.edible_seaweed", heal = 4, dbRowId = "dbrows.edible_seaweed_food"),

    // Cooked meats & misc
    KEBAB("items.kebab", heal = 3, hasEffect = true, dbRowId = "dbrows.kebab_food"),
    LOCUST_MEAT("items.locust_meat", heal = 3, dbRowId = "dbrows.locust_meat_food"),
    ROE("items.brut_roe", heal = 3, dbRowId = "dbrows.roe_food"),
    STEW("items.stew", "items.bowl_empty", heal = 11, dbRowId = "dbrows.stew_food"),
    COOKED_RABBIT("items.cooked_rabbit", heal = 5, dbRowId = "dbrows.cooked_rabbit_food"),
    COOKED_MYSTERY_MEAT("items.cooked_mystery_meat", heal = 5, dbRowId = "dbrows.cooked_mystery_meat_food"),
    SCRAMBLED_EGG("items.scrambled_egg", heal = 5, dbRowId = "dbrows.scrambled_egg_food"),
    CAVIAR("items.brut_caviar", heal = 5, dbRowId = "dbrows.caviar_food"),
    BAGUETTE("items.baguette", heal = 6, dbRowId = "dbrows.baguette_food"),
    SEASONED_SARDINE("items.seasoned_sardine", heal = 4, dbRowId = "dbrows.seasoned_sardine_food"),
    GIANT_FROG_LEGS("items.giant_frog_legs", heal = 6, dbRowId = "dbrows.giant_frog_legs_food"),
    COOKED_CHOMPY("items.cooked_chompy", heal = 10, dbRowId = "dbrows.cooked_chompy_food"),
    CHOC_ICE("items.elid_choc_ice", heal = 7, dbRowId = "dbrows.choc_ice_food"),
    PUMPKIN("items.pumpkin", heal = 14, dbRowId = "dbrows.pumpkin_food"),
    EASTER_EGG("items.easter_egg", heal = 14, dbRowId = "dbrows.easter_egg_food"),
    WRAPPED_OOMLIE("items.wrapped_oomlie", heal = 14, dbRowId = "dbrows.wrapped_oomlie_food"),
    COOKED_SWEETCORN("items.sweetcorn_cooked", heal = 10, dbRowId = "dbrows.cooked_sweetcorn_food"),

    // Roast meats (spit-roasted)
    ROAST_BIRD_MEAT("items.spit_roasted_bird_meat", heal = 6, dbRowId = "dbrows.roast_bird_meat_food"),
    ROAST_RABBIT("items.spit_roasted_rabbit_meat", heal = 7, dbRowId = "dbrows.roast_rabbit_food"),
    ROAST_BEAST_MEAT("items.spit_roasted_beast_meat", heal = 8, dbRowId = "dbrows.roast_beast_meat_food"),

    // Snails
    THIN_SNAIL_MEAT("items.snail_corpse_cooked1", heal = 5, dbRowId = "dbrows.thin_snail_meat_food"),
    LEAN_SNAIL_MEAT("items.snail_corpse_cooked2", heal = 6, dbRowId = "dbrows.lean_snail_meat_food"),
    FAT_SNAIL_MEAT("items.snail_corpse_cooked3", heal = 8, dbRowId = "dbrows.fat_snail_meat_food"),

    // Spider on stick/shaft
    SPIDER_ON_STICK("items.tbw_spider_on_stick_cooked", heal = 7, dbRowId = "dbrows.spider_on_stick_food"),
    SPIDER_ON_SHAFT("items.tbw_spider_on_shaft_cooked", heal = 7, dbRowId = "dbrows.spider_on_shaft_food"),

    // Cooked jubbly
    COOKED_JUBBLY("items.100_jubbly_meat_cooked", heal = 15, dbRowId = "dbrows.cooked_jubbly_food"),

    // Hunter meats
    COOKED_WILD_KEBBIT("items.wildkebbit_cooked", heal = 4, dbRowId = "dbrows.cooked_wild_kebbit_food"),
    COOKED_LARUPIA("items.larupia_cooked", heal = 6, dbRowId = "dbrows.cooked_larupia_food"),
    COOKED_BARB_TAILED_KEBBIT("items.barbkebbit_cooked", heal = 7, dbRowId = "dbrows.cooked_barb_tailed_kebbit_food"),
    COOKED_GRAAHK("items.graahk_cooked", heal = 8, dbRowId = "dbrows.cooked_graahk_food"),
    COOKED_KYATT("items.kyatt_cooked", heal = 9, dbRowId = "dbrows.cooked_kyatt_food"),
    COOKED_DASHING_KEBBIT("items.dashingkebbit_cooked", heal = 13, dbRowId = "dbrows.cooked_dashing_kebbit_food"),

    // Potatoes
    BAKED_POTATO("items.potato_baked", heal = 4, dbRowId = "dbrows.baked_potato_food"),
    POTATO_WITH_BUTTER("items.potato_butter", heal = 14, dbRowId = "dbrows.potato_with_butter_food"),
    CHILLI_POTATO("items.potato_chilli+carne", heal = 14, dbRowId = "dbrows.chilli_potato_food"),
    EGG_POTATO("items.potato_egg+tomato", heal = 16, dbRowId = "dbrows.egg_potato_food"),
    MUSHROOM_POTATO("items.potato_mushroom+onion", heal = 20, dbRowId = "dbrows.mushroom_potato_food"),

    // Bowls
    BOWL_OF_CHILLI_CON_CARNE("items.bowl_chilli+carne", "items.bowl_empty", heal = 5, dbRowId = "dbrows.bowl_of_chilli_con_carne_food"),
    EGG_AND_TOMATO("items.bowl_egg+tomato", "items.bowl_empty", heal = 8, dbRowId = "dbrows.egg_and_tomato_food"),
    MUSHROOM_AND_ONION("items.bowl_mushroom+onion", "items.bowl_empty", heal = 11, dbRowId = "dbrows.mushroom_and_onion_food"),
    TUNA_AND_CORN("items.bowl_tuna+sweetcorn", "items.bowl_empty", heal = 13, dbRowId = "dbrows.tuna_and_corn_food"),

    // Dorgesh-Kaan foods
    BAT_SHISH("items.dorgesh_bat_shish", heal = 2, dbRowId = "dbrows.bat_shish_food"),
    GREEN_GLOOP_SOUP("items.dorgesh_green_gloop_soup", heal = 2, dbRowId = "dbrows.green_gloop_soup_food"),
    FROG_SPAWN_GUMBO("items.dorgesh_frog_spawn_gumbo", heal = 3, dbRowId = "dbrows.frog_spawn_gumbo_food"),
    FRIED_MUSHROOMS("items.bowl_mushroom_fried", "items.bowl_empty", heal = 5, dbRowId = "dbrows.fried_mushrooms_food"),
    SAUTEED_MUSHROOMS("items.dorgesh_sauteed_mushrooms", heal = 6, dbRowId = "dbrows.sauteed_mushrooms_food"),
    CAVE_EEL_SUSHI("items.dorgesh_cave_eel_sushi", heal = 7, dbRowId = "dbrows.cave_eel_sushi_food"),
    FROG_BURGER("items.dorgesh_frog_burger", heal = 6, dbRowId = "dbrows.frog_burger_food"),

    // Crab meats
    RED_CRAB_MEAT("items.red_crab_meat", heal = 8, dbRowId = "dbrows.red_crab_meat_food"),
    BLUE_CRAB_MEAT("items.blue_crab_meat", heal = 14, dbRowId = "dbrows.blue_crab_meat_food"),
    RAINBOW_CRAB_MEAT("items.rainbow_crab_meat", heal = 19, dbRowId = "dbrows.rainbow_crab_meat_food"),

    // Drinks & ales (most have stat effects)
    BEER("items.beer", "items.beer_glass", heal = 1, hasEffect = true, dbRowId = "dbrows.beer_food"),
    ASGARNIAN_ALE("items.asgarnian_ale", "items.beer_glass", heal = 1, hasEffect = true, dbRowId = "dbrows.asgarnian_ale_food"),
    WIZARDS_MIND_BOMB("items.wizards_mind_bomb", "items.beer_glass", heal = 1, hasEffect = true, dbRowId = "dbrows.wizards_mind_bomb_food"),
    GREENMANS_ALE("items.greenmans_ale", "items.beer_glass", heal = 1, hasEffect = true, dbRowId = "dbrows.greenmans_ale_food"),
    DRAGON_BITTER("items.dragon_bitter", "items.beer_glass", heal = 1, hasEffect = true, dbRowId = "dbrows.dragon_bitter_food"),
    DWARVEN_STOUT("items.dwarven_stout", "items.beer_glass", heal = 1, hasEffect = true, dbRowId = "dbrows.dwarven_stout_food"),
    CIDER("items.cider", "items.beer_glass", heal = 1, hasEffect = true, dbRowId = "dbrows.cider_food"),
    AXEMANS_FOLLY("items.axemans_folly", "items.beer_glass", heal = 1, hasEffect = true, dbRowId = "dbrows.axemans_folly_food"),
    CHEFS_DELIGHT("items.chefs_delight", "items.beer_glass", heal = 1, hasEffect = true, dbRowId = "dbrows.chefs_delight_food"),
    SLAYERS_RESPITE("items.slayers_respite", "items.beer_glass", heal = 1, hasEffect = true, dbRowId = "dbrows.slayers_respite_food"),
    GROG("items.grog", heal = 3, hasEffect = true, dbRowId = "dbrows.grog_food"),
    VODKA("items.vodka", heal = 5, hasEffect = true, dbRowId = "dbrows.vodka_food"),
    WHISKY("items.whisky", heal = 5, hasEffect = true, dbRowId = "dbrows.whisky_food"),
    GIN("items.gin", heal = 5, hasEffect = true, dbRowId = "dbrows.gin_food"),
    BRANDY("items.brandy", heal = 5, hasEffect = true, dbRowId = "dbrows.brandy_food"),
    KEG_OF_BEER("items.keg_of_beer", heal = 1, hasEffect = true, dbRowId = "dbrows.keg_of_beer_food"),
    KOVACS_GROG("items.kovacs_grog", heal = 3, hasEffect = true, dbRowId = "dbrows.kovacs_grog_food"),

    // Gnome cocktails (combo foods)
    FRUIT_BLAST("items.fruit_blast", heal = 9, comboFood = true, eatDelay = listOf(2), combatDelay = listOf(3), dbRowId = "dbrows.fruit_blast_food"),
    PREMADE_FRUIT_BLAST("items.premade_fruit_blast", heal = 9, comboFood = true, eatDelay = listOf(2), combatDelay = listOf(3), dbRowId = "dbrows.premade_fruit_blast_food"),
    PINEAPPLE_PUNCH("items.pineapple_punch", heal = 9, comboFood = true, eatDelay = listOf(2), combatDelay = listOf(3), dbRowId = "dbrows.pineapple_punch_food"),
    PREMADE_PINEAPPLE_PUNCH("items.premade_pineapple_punch", heal = 9, comboFood = true, eatDelay = listOf(2), combatDelay = listOf(3), dbRowId = "dbrows.premade_pineapple_punch_food"),
    SHORT_GREEN_GUY("items.sgg", heal = 5, comboFood = true, eatDelay = listOf(2), combatDelay = listOf(3), dbRowId = "dbrows.short_green_guy_food"),
    PREMADE_SGG("items.premade_sgg", heal = 5, comboFood = true, eatDelay = listOf(2), combatDelay = listOf(3), dbRowId = "dbrows.premade_sgg_food"),
    BLURBERRY_SPECIAL("items.blurberry_special", heal = 7, comboFood = true, eatDelay = listOf(2), combatDelay = listOf(3), dbRowId = "dbrows.blurberry_special_food"),
    PREMADE_BLURBERRY_SPECIAL("items.premade_blurberry_special", heal = 7, comboFood = true, eatDelay = listOf(2), combatDelay = listOf(3), dbRowId = "dbrows.premade_blurberry_special_food"),
    DRUNK_DRAGON("items.drunk_dragon", heal = 5, comboFood = true, eatDelay = listOf(2), combatDelay = listOf(3), dbRowId = "dbrows.drunk_dragon_food"),
    PREMADE_DRUNK_DRAGON("items.premade_drunk_dragon", heal = 5, comboFood = true, eatDelay = listOf(2), combatDelay = listOf(3), dbRowId = "dbrows.premade_drunk_dragon_food"),
    WIZARD_BLIZZARD("items.wizard_blizzard", heal = 5, comboFood = true, eatDelay = listOf(2), combatDelay = listOf(3), dbRowId = "dbrows.wizard_blizzard_food"),
    PREMADE_WIZARD_BLIZZARD("items.premade_wizard_blizzard", heal = 5, comboFood = true, eatDelay = listOf(2), combatDelay = listOf(3), dbRowId = "dbrows.premade_wizard_blizzard_food"),
    CHOCOLATE_SATURDAY("items.aluft_choc_saturday", heal = 5, comboFood = true, eatDelay = listOf(2), combatDelay = listOf(3), dbRowId = "dbrows.chocolate_saturday_food"),
    PREMADE_CHOC_SATURDAY("items.premade_choc_saturday", heal = 5, comboFood = true, eatDelay = listOf(2), combatDelay = listOf(3), dbRowId = "dbrows.premade_choc_saturday_food"),

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

    fun consumableFood() = dbTable("tables.consumable_food", serverOnly = true) {

        column("items", ITEMS, VarType.OBJ.count(Food.entries.maxOf { it.items.size }))
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