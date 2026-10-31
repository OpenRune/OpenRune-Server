package org.rsmod.content.skills.farming.pack

private const val HOPS_SAVE_HIGH = 180
private const val BUSH_MINUTES = 20
private const val BUSH_REGROW_MINUTES = 20

private fun hops(
    key: String,
    index: Int,
    name: String,
    seed: String,
    seeds: Int,
    produce: String,
    level: Int,
    plantXp: Double,
    harvestXp: Double,
    stages: Int,
    cycleMinutes: Int,
    prefix: String,
    saveLow: Int?,
    petChance: Int,
    payment: List<Pair<String, Int>>,
) = CropSpec(
    key = key,
    index = index,
    kind = "HOPS",
    name = name,
    seed = seed,
    seeds = seeds,
    produce = produce,
    level = level,
    plantXp = plantXp,
    harvestXp = harvestXp,
    stages = stages,
    cycleMinutes = cycleMinutes,
    healthy = growArt("${prefix}_seed", prefix, stages),
    watered =
        listOf("${prefix}_seed_watered") +
            (1 until stages).map { "${prefix}_${it}_watered" } + null,
    diseased = damagedArt(stages) { "${prefix}_${it}_diseased" },
    dead = damagedArt(stages) { "${prefix}_${it}_dead" },
    harvest = "LIVES",
    saveLow = saveLow,
    saveHigh = saveLow?.let { HOPS_SAVE_HIGH },
    petChance = petChance,
    payment = payment,
    magicSecateurs = true,
)

private fun bush(
    key: String,
    index: Int,
    name: String,
    seed: String,
    produce: String,
    level: Int,
    plantXp: Double,
    checkXp: Double,
    harvestXp: Double,
    stages: Int,
    prefix: String,
    petChance: Int,
    payment: List<Pair<String, Int>>,
    immune: Boolean = false,
) = CropSpec(
    key = key,
    index = index,
    kind = "BUSH",
    name = name,
    seed = seed,
    produce = produce,
    level = level,
    plantXp = plantXp,
    checkXp = checkXp,
    harvestXp = harvestXp,
    stages = stages,
    cycleMinutes = BUSH_MINUTES,
    healthy =
        listOf("${prefix}_seedling") + (1 until stages).map { "${prefix}_$it" } +
            "${prefix}_claim_xp",
    diseased =
        listOf<String?>(null) + (1 until stages).map { "${prefix}_${it}_diseased" } +
            "${prefix}_fullygrown_diseased",
    dead =
        listOf<String?>(null) + (1 until stages).map { "${prefix}_${it}_dead" } +
            "${prefix}_fullygrown_dead",
    produceArt = listOf("${prefix}_fullygrown") + (1..4).map { "${prefix}_berry_$it" },
    harvest = "PICK",
    regrowMinutes = BUSH_REGROW_MINUTES,
    petChance = petChance,
    payment = payment,
    magicSecateurs = true,
    immune = immune,
    held = "Level-scaled extra berry yield beyond the four visible berries is unsourced",
)

internal val HOPS_CROPS: List<CropSpec> =
    listOf(
        hops("barley", 31, "Barley", "obj.barley_seed", 4, "obj.barley", 3, 8.5, 9.5, 4, 10, "loc.barley", 103, 112_416, listOf("obj.bucket_compost" to 3)),
        hops("hammerstone", 32, "Hammerstone hops", "obj.hammerstone_hop_seed", 4, "obj.hammerstone_hops", 4, 9.0, 10.0, 4, 10, "loc.hammer_hops", 104, 112_416, listOf("obj.marigold" to 1)),
        hops("asgarnian", 33, "Asgarnian hops", "obj.asgarnian_hop_seed", 4, "obj.asgarnian_hops", 8, 10.9, 12.0, 5, 10, "loc.asgarnian_hops", 108, 89_933, listOf("obj.sack_onion_10" to 1)),
        hops("jute", 34, "Jute", "obj.jute_seed", 3, "obj.jute_fibre", 13, 13.0, 14.5, 5, 10, "loc.jute", 113, 89_933, listOf("obj.barley_malt" to 6)),
        hops("yanillian", 35, "Yanillian hops", "obj.yanillian_hop_seed", 4, "obj.yanillian_hops", 16, 14.5, 16.0, 6, 10, "loc.yanillian_hops", 116, 74_944, listOf("obj.basket_tomato_5" to 1)),
        hops("flax", 36, "Flax", "obj.flax_seed", 3, "obj.flax", 18, 16.0, 17.5, 3, 20, "loc.flax", null, 89_933, listOf("obj.grain" to 6)),
        hops("krandorian", 37, "Krandorian hops", "obj.krandorian_hop_seed", 4, "obj.krandorian_hops", 21, 17.5, 19.5, 7, 10, "loc.krandorian_hops", 121, 64_238, listOf("obj.sack_cabbage_10" to 3)),
        hops("wildblood", 38, "Wildblood hops", "obj.wildblood_hop_seed", 4, "obj.wildblood_hops", 28, 23.0, 26.0, 8, 10, "loc.wildblood_hops", 128, 56_208, listOf("obj.nasturtium" to 1)),
        hops("hemp", 39, "Hemp", "obj.hemp_seed", 3, "obj.hemp", 37, 33.0, 37.0, 4, 20, "loc.hemp", null, 56_208, listOf("obj.flax" to 6)),
        hops("cotton", 40, "Cotton", "obj.cotton_seed", 3, "obj.cotton_boll", 71, 72.0, 82.0, 5, 20, "loc.cotton", null, 56_208, listOf("obj.hemp" to 6)),
    )

// shortcut: Only the four visible berries are sourced; extend yield when extra-berry rules are verified.
internal val BUSH_CROPS: List<CropSpec> =
    listOf(
        bush("redberry", 41, "Redberry bush", "obj.redberry_bush_seed", "obj.redberries", 10, 11.5, 64.0, 4.5, 5, "loc.redberry_bush", 44_966, listOf("obj.sack_cabbage_10" to 4)),
        bush("cadavaberry", 42, "Cadavaberry bush", "obj.cadavaberry_bush_seed", "obj.cadavaberries", 22, 18.0, 102.5, 7.0, 6, "loc.cadavaberry_bush", 37_472, listOf("obj.basket_tomato_5" to 3)),
        bush("dwellberry", 43, "Dwellberry bush", "obj.dwellberry_bush_seed", "obj.dwellberries", 36, 31.5, 177.5, 12.0, 7, "loc.dwellberry_bush", 32_119, listOf("obj.basket_strawberry_5" to 3)),
        bush("jangerberry", 44, "Jangerberry bush", "obj.jangerberry_bush_seed", "obj.jangerberries", 48, 50.5, 284.5, 19.0, 8, "loc.jangerberry_bush", 28_104, listOf("obj.watermelon" to 6)),
        bush("whiteberry", 45, "Whiteberry bush", "obj.whiteberry_bush_seed", "obj.white_berries", 59, 78.0, 437.5, 29.0, 8, "loc.whiteberry_bush", 28_104, listOf("obj.bittercap_mushroom" to 8)),
        bush("poison_ivy", 46, "Poison Ivy bush", "obj.poisonivy_bush_seed", "obj.poisonivy_berries", 70, 120.0, 675.0, 45.0, 8, "loc.poisonivy_bush", 28_104, emptyList(), immune = true),
    )
