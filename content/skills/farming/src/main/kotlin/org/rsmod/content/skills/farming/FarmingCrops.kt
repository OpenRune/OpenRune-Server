package org.rsmod.content.skills.farming

internal const val STAT_FARMING: String = "stat.farming"

internal const val RAKE: String = "obj.rake"
internal const val DIBBER: String = "obj.dibber"
internal const val SPADE: String = "obj.spade"
internal const val SECATEURS: String = "obj.secateurs"
internal const val PLANT_CURE: String = "obj.plant_cure"
internal const val WEEDS: String = "obj.weeds"

internal const val ANIM_RAKE: String = "seq.farming_raking"
internal const val ANIM_PLANT: String = "seq.farming_seed_dibbing"
internal const val ANIM_WATER: String = "seq.farming_watering"
internal const val ANIM_HARVEST: String = "seq.picking_low"
internal const val ANIM_CURE: String = "seq.farming_plant_cure"
internal const val ANIM_COMPOST: String = "seq.farming_trowel_digging"

enum class PatchKind {
    ALLOTMENT,
    FLOWER,
    HERB,
}

/**
 * [growBase] is the index into the patch loc's own transform table: the client renders
 * `transforms[growBase + stage]`, so these values come straight from the cache rather than from
 * any server's table. Allotments and flowers offset that by +64 watered, +128 diseased and +192
 * dead; herb patches pack their diseased art separately, which is what [diseasedBase] is for.
 */
data class Crop(
    val kind: PatchKind,
    val name: String,
    val seed: String,
    val produce: String,
    val level: Int,
    val plantXp: Double,
    val harvestXp: Double,
    val growBase: Int,
    val stages: Int,
    val stageMinutes: Int,
    val diseasedBase: Int = -1,
    val seedsPerPlant: Int = 1,
    val protection: String? = null,
)

private fun allotment(
    name: String,
    seed: String,
    produce: String,
    level: Int,
    plantXp: Double,
    harvestXp: Double,
    growBase: Int,
    stages: Int,
    protection: String? = null,
) =
    Crop(
        kind = PatchKind.ALLOTMENT,
        name = name,
        seed = seed,
        produce = produce,
        level = level,
        plantXp = plantXp,
        harvestXp = harvestXp,
        growBase = growBase,
        stages = stages,
        stageMinutes = 10,
        seedsPerPlant = 3,
        protection = protection,
    )

private fun flower(
    name: String,
    seed: String,
    produce: String,
    level: Int,
    plantXp: Double,
    harvestXp: Double,
    growBase: Int,
) =
    Crop(
        kind = PatchKind.FLOWER,
        name = name,
        seed = seed,
        produce = produce,
        level = level,
        plantXp = plantXp,
        harvestXp = harvestXp,
        growBase = growBase,
        stages = 4,
        stageMinutes = 5,
    )

private fun herb(
    name: String,
    seed: String,
    produce: String,
    level: Int,
    plantXp: Double,
    harvestXp: Double,
    growBase: Int,
    diseasedBase: Int,
) =
    Crop(
        kind = PatchKind.HERB,
        name = name,
        seed = seed,
        produce = produce,
        level = level,
        plantXp = plantXp,
        harvestXp = harvestXp,
        growBase = growBase,
        stages = 4,
        stageMinutes = 20,
        diseasedBase = diseasedBase,
    )

object FarmingCrops {
    val all: List<Crop> =
        listOf(
            allotment("potato", "obj.potato_seed", "obj.potato", 1, 8.0, 9.0, 6, 4, "marigold"),
            allotment("onion", "obj.onion_seed", "obj.onion", 5, 9.5, 10.5, 13, 4, "marigold"),
            allotment("cabbage", "obj.cabbage_seed", "obj.cabbage", 7, 10.0, 11.5, 20, 4, "rosemary"),
            allotment("tomato", "obj.tomato_seed", "obj.tomato", 12, 12.5, 14.0, 27, 4, "marigold"),
            allotment("sweetcorn", "obj.sweetcorn_seed", "obj.sweetcorn", 20, 17.0, 19.0, 34, 6),
            allotment("strawberry", "obj.strawberry_seed", "obj.strawberry", 31, 26.0, 29.0, 43, 6),
            allotment(
                name = "watermelon",
                seed = "obj.watermelon_seed",
                produce = "obj.watermelon",
                level = 47,
                plantXp = 48.5,
                harvestXp = 54.5,
                growBase = 52,
                stages = 8,
                protection = "nasturtium",
            ),
            flower("marigold", "obj.marigold_seed", "obj.marigold", 2, 8.5, 47.0, 8),
            flower("rosemary", "obj.rosemary_seed", "obj.rosemary", 11, 12.0, 66.5, 13),
            flower("nasturtium", "obj.nasturtium_seed", "obj.nasturtium", 24, 19.5, 111.0, 18),
            flower("woad", "obj.woad_seed", "obj.woadleaf", 25, 20.5, 115.5, 23),
            flower("limpwurt", "obj.limpwurt_seed", "obj.limpwurt_root", 26, 21.5, 120.0, 28),
            herb("guam", "obj.guam_seed", "obj.unidentified_guam", 9, 11.0, 12.5, 4, 128),
            herb("marrentill", "obj.marrentill_seed", "obj.unidentified_marentill", 14, 13.5, 15.0, 11, 131),
            herb("tarromin", "obj.tarromin_seed", "obj.unidentified_tarromin", 19, 16.0, 18.0, 18, 134),
            herb("harralander", "obj.harralander_seed", "obj.unidentified_harralander", 26, 21.5, 24.0, 25, 137),
            herb("ranarr", "obj.ranarr_seed", "obj.unidentified_ranarr", 32, 27.0, 30.5, 32, 140),
            herb("toadflax", "obj.toadflax_seed", "obj.unidentified_toadflax", 38, 34.0, 38.5, 39, 143),
            herb("irit", "obj.irit_seed", "obj.unidentified_irit", 44, 43.0, 48.5, 46, 146),
            herb("avantoe", "obj.avantoe_seed", "obj.unidentified_avantoe", 50, 54.5, 61.5, 53, 149),
            herb("kwuarm", "obj.kwuarm_seed", "obj.unidentified_kwuarm", 56, 69.0, 78.0, 68, 152),
            herb("snapdragon", "obj.snapdragon_seed", "obj.unidentified_snapdragon", 62, 87.5, 98.5, 75, 155),
            herb("cadantine", "obj.cadantine_seed", "obj.unidentified_cadantine", 67, 106.5, 120.0, 82, 158),
            herb("lantadyme", "obj.lantadyme_seed", "obj.unidentified_lantadyme", 73, 134.5, 151.5, 89, 161),
            herb("dwarf weed", "obj.dwarf_weed_seed", "obj.unidentified_dwarf_weed", 79, 170.5, 192.0, 96, 164),
            herb("torstol", "obj.torstol_seed", "obj.unidentified_torstol", 85, 199.5, 224.5, 103, 167),
        )

    private val bySeed = all.associateBy(Crop::seed)

    fun bySeed(seed: String): Crop? = bySeed[seed]

    fun index(crop: Crop): Int = all.indexOf(crop) + 1

    fun byIndex(index: Int): Crop? = all.getOrNull(index - 1)
}

/** Watering cans, empty first, so the index doubles as the number of charges left. */
val WATERING_CANS: List<String> =
    listOf(
        "obj.watering_can_0",
        "obj.watering_can_1",
        "obj.watering_can_2",
        "obj.watering_can_3",
        "obj.watering_can_4",
        "obj.watering_can_5",
        "obj.watering_can_6",
        "obj.watering_can_7",
        "obj.watering_can_8",
    )

enum class Compost(val obj: String, val diseaseChance: Double) {
    NONE("", 0.35),
    NORMAL("obj.bucket_compost", 0.22),
    SUPER("obj.bucket_supercompost", 0.115),
    ULTRA("obj.bucket_ultracompost", 0.0),
}
