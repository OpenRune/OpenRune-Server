package org.rsmod.content.skills.farming.pack

private const val ALLOTMENT_SEEDS = 3
private const val ALLOTMENT_MINUTES = 10
private const val FLOWER_MINUTES = 5
private const val HERB_MINUTES = 20
private const val HERB_STAGES = 4
private const val FLOWER_STAGES = 4
private const val ALLOTMENT_SAVE_HIGH = 180
private const val HERB_SAVE_HIGH = 80
private const val HERB_DISEASE_MAX = 26

/** Diseased herbs die on the next slower (40-minute) tick (Mod Ash, 3 Feb 2023). */
internal const val HERB_DEATH_MINUTES = 40

// shortcut: allotments and flowers keep the inherited "dies one cycle after disease" timing;
// replace once their diseased tick rate is sourced.
private const val INHERITED_ALLOTMENT_DEATH = ALLOTMENT_MINUTES
private const val INHERITED_FLOWER_DEATH = FLOWER_MINUTES
private const val FLOWER_PET = 281_040
private const val HERB_PET = 98_364

/** `seedling`, `1`..`stages-1`, `fullygrown` under one loc prefix. */
internal fun growArt(seed: String, prefix: String, stages: Int): List<String> =
    listOf(seed) + (1 until stages).map { "${prefix}_$it" } + "${prefix}_fullygrown"

/** `prefix_<stage><suffix>` for stages 1..stages-1, with no art at stage 0 or maturity. */
internal fun damagedArt(stages: Int, symbol: (Int) -> String): List<String?> =
    listOf<String?>(null) + (1 until stages).map(symbol) + null

private fun allotment(
    key: String,
    index: Int,
    name: String,
    produce: String,
    level: Int,
    plantXp: Double,
    harvestXp: Double,
    stages: Int,
    saveLow: Int,
    petChance: Int,
    payment: List<Pair<String, Int>>,
    protection: String? = null,
    prefix: String = "loc.$key",
    saveHigh: Int = ALLOTMENT_SAVE_HIGH,
) = CropSpec(
    key = key,
    index = index,
    kind = "ALLOTMENT",
    name = name,
    seed = "obj.${key}_seed",
    seeds = ALLOTMENT_SEEDS,
    produce = produce,
    level = level,
    plantXp = plantXp,
    harvestXp = harvestXp,
    stages = stages,
    cycleMinutes = ALLOTMENT_MINUTES,
    healthy = growArt("${prefix}_seed", prefix, stages),
    watered =
        listOf("${prefix}_seed_watered") + (1 until stages).map { "${prefix}_${it}_watered" } +
            null,
    diseased = damagedArt(stages) { "${prefix}_${it}_diseased" },
    dead = damagedArt(stages) { "${prefix}_${it}_dead" },
    saveLow = saveLow,
    saveHigh = saveHigh,
    protection = protection,
    diseaseLegacy = true,
    deathMinutes = INHERITED_ALLOTMENT_DEATH,
    petChance = petChance,
    payment = payment,
    magicSecateurs = true,
)

private fun flower(
    key: String,
    index: Int,
    name: String,
    seed: String,
    produce: String,
    level: Int,
    plantXp: Double,
    harvestXp: Double,
    prefix: String,
    harvestAmount: Int = 1,
    harvestLevelDivisor: Int = 0,
    magicSecateurs: Boolean = false,
) = CropSpec(
    key = key,
    index = index,
    kind = "FLOWER",
    name = name,
    seed = seed,
    produce = produce,
    level = level,
    plantXp = plantXp,
    harvestXp = harvestXp,
    stages = FLOWER_STAGES,
    cycleMinutes = FLOWER_MINUTES,
    healthy = growArt("${prefix}_seed", prefix, FLOWER_STAGES),
    watered =
        listOf("${prefix}_seed_watered") +
            (1 until FLOWER_STAGES).map { "${prefix}_${it}_watered" } + null,
    diseased = damagedArt(FLOWER_STAGES) { "${prefix}_${it}_diseased" },
    dead = damagedArt(FLOWER_STAGES) { "${prefix}_${it}_dead" },
    harvestAmount = harvestAmount,
    harvestLevelDivisor = harvestLevelDivisor,
    harvest = "BATCH",
    diseaseLegacy = true,
    deathMinutes = INHERITED_FLOWER_DEATH,
    petChance = FLOWER_PET,
    magicSecateurs = magicSecateurs,
)

private fun herb(
    key: String,
    index: Int,
    name: String,
    produce: String,
    level: Int,
    plantXp: Double,
    harvestXp: Double,
    prefix: String,
    saveLow: Int,
    diseasedPrefix: String = "${prefix}_diseased",
    seed: String = "obj.${key}_seed",
) = CropSpec(
    key = key,
    index = index,
    kind = "HERB",
    name = name,
    seed = seed,
    produce = produce,
    level = level,
    plantXp = plantXp,
    harvestXp = harvestXp,
    stages = HERB_STAGES,
    cycleMinutes = HERB_MINUTES,
    healthy = growArt("${prefix}_seed", prefix, HERB_STAGES),
    diseased = damagedArt(HERB_STAGES) { "${diseasedPrefix}_$it" },
    dead = damagedArt(HERB_STAGES) { "loc.herb_${it}_dead" },
    saveLow = saveLow,
    saveHigh = HERB_SAVE_HIGH,
    diseaseMax = HERB_DISEASE_MAX,
    deathMinutes = HERB_DEATH_MINUTES,
    petChance = HERB_PET,
    magicSecateurs = true,
)

private val SNAPE_GRASS_STAGES = listOf("seedling", "1", "2", "3", "4", "5", "6", "fullygrown")

/** Crop indices 1..29 shipped before the full-skill cutover; their order is persisted. */
internal val LEGACY_CROPS: List<CropSpec> =
    listOf(
        allotment("potato", 1, "potato", "obj.potato", 1, 8.0, 9.0, 4, 101, 281_040, listOf("obj.bucket_compost" to 2), "dbrow.farming_marigold"),
        allotment("onion", 2, "onion", "obj.onion", 5, 9.5, 10.5, 4, 105, 281_040, listOf("obj.sack_potato_10" to 1), "dbrow.farming_marigold"),
        allotment("cabbage", 3, "cabbage", "obj.cabbage", 7, 10.0, 11.5, 4, 107, 281_040, listOf("obj.sack_onion_10" to 1), "dbrow.farming_rosemary"),
        allotment("tomato", 4, "tomato", "obj.tomato", 12, 12.5, 14.0, 4, 112, 281_040, listOf("obj.sack_cabbage_10" to 2), "dbrow.farming_marigold"),
        allotment("sweetcorn", 5, "sweetcorn", "obj.sweetcorn", 20, 17.0, 19.0, 6, 88, 224_832, listOf("obj.jute_fibre" to 10)),
        allotment("strawberry", 6, "strawberry", "obj.strawberry", 31, 26.0, 29.0, 6, 103, 187_360, listOf("obj.basket_apple_5" to 1)),
        allotment("watermelon", 7, "watermelon", "obj.watermelon", 47, 48.5, 54.5, 8, 126, 160_594, listOf("obj.curry_leaf" to 10), "dbrow.farming_nasturtium"),
        flower("marigold", 8, "marigold", "obj.marigold_seed", "obj.marigold", 2, 8.5, 47.0, "loc.marigold"),
        flower("rosemary", 9, "rosemary", "obj.rosemary_seed", "obj.rosemary", 11, 12.0, 66.5, "loc.rosemary"),
        flower("nasturtium", 10, "nasturtium", "obj.nasturtium_seed", "obj.nasturtium", 24, 19.5, 111.0, "loc.nasturtium"),
        flower("woad", 11, "woad", "obj.woad_seed", "obj.woadleaf", 25, 20.5, 115.5, "loc.woad", harvestAmount = 3),
        flower("limpwurt", 12, "limpwurt", "obj.limpwurt_seed", "obj.limpwurt_root", 26, 21.5, 120.0, "loc.limpwurt", harvestAmount = 3, harvestLevelDivisor = 10, magicSecateurs = true),
        herb("guam", 13, "guam", "obj.unidentified_guam", 9, 11.0, 12.5, "loc.herb_guam_leaf", 25),
        herb("marrentill", 14, "marrentill", "obj.unidentified_marentill", 14, 13.5, 15.0, "loc.herb_marrentill", 28, diseasedPrefix = "loc.herb_marentill_diseased"),
        herb("tarromin", 15, "tarromin", "obj.unidentified_tarromin", 19, 16.0, 18.0, "loc.herb_tarromin", 31),
        herb("harralander", 16, "harralander", "obj.unidentified_harralander", 26, 21.5, 24.0, "loc.herb_harralander", 36),
        herb("ranarr", 17, "ranarr", "obj.unidentified_ranarr", 32, 26.5, 30.5, "loc.herb_ranarr_weed", 39),
        herb("toadflax", 18, "toadflax", "obj.unidentified_toadflax", 38, 34.0, 38.5, "loc.herb_toadflax", 43),
        herb("irit", 19, "irit", "obj.unidentified_irit", 44, 43.0, 48.5, "loc.herb_irit_leaf", 46),
        herb("avantoe", 20, "avantoe", "obj.unidentified_avantoe", 50, 54.5, 61.5, "loc.herb_avantoe", 50),
        herb("kwuarm", 21, "kwuarm", "obj.unidentified_kwuarm", 56, 69.0, 78.0, "loc.herb_kwuarm", 54),
        herb("snapdragon", 22, "snapdragon", "obj.unidentified_snapdragon", 62, 87.5, 98.5, "loc.herb_snapdragon", 57),
        herb("cadantine", 23, "cadantine", "obj.unidentified_cadantine", 67, 106.5, 120.0, "loc.herb_cadantine", 60),
        herb("lantadyme", 24, "lantadyme", "obj.unidentified_lantadyme", 73, 134.5, 151.5, "loc.herb_lantadyme", 64),
        herb("dwarf_weed", 25, "dwarf weed", "obj.unidentified_dwarf_weed", 79, 170.5, 192.0, "loc.herb_dwarf_weed", 67),
        herb("torstol", 26, "torstol", "obj.unidentified_torstol", 85, 199.5, 224.5, "loc.herb_torstol", 71),
        CropSpec(
            key = "snape_grass",
            index = 27,
            kind = "ALLOTMENT",
            name = "snape grass",
            seed = "obj.snape_grass_seed",
            seeds = ALLOTMENT_SEEDS,
            produce = "obj.snape_grass",
            level = 61,
            plantXp = 82.0,
            harvestXp = 82.0,
            stages = 7,
            cycleMinutes = ALLOTMENT_MINUTES,
            healthy = SNAPE_GRASS_STAGES.map { "loc.snapegrass_$it" },
            watered =
                listOf("seed_watered", "1_watered", "2_watered", "3_watered", "4_watered", "5_watered", "6_watered", null)
                    .map { it?.let { stage -> "loc.snapegrass_$stage" } },
            diseased = damagedArt(7) { "loc.snapegrass_${it}_diseased" },
            dead = damagedArt(7) { "loc.snapegrass_${it}_dead" },
            saveLow = 148,
            saveHigh = 195,
            diseaseLegacy = true,
            deathMinutes = INHERITED_ALLOTMENT_DEATH,
            petChance = 173_977,
            payment = listOf("obj.jangerberries" to 5),
            magicSecateurs = true,
        ),
        // shortcut: inherited Huasca CTS 58 (HFC) disagrees with TF's fitted 59; re-pin when sourced.
        herb("huasca", 28, "huasca", "obj.unidentified_huasca", 65, 86.5, 110.0, "loc.herb_huasca", 58),
        flower("white_lily", 29, "white lily", "obj.white_lily_seed", "obj.whitelilly", 58, 42.0, 250.0, "loc.lily"),
    )
