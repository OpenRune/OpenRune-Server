package org.rsmod.content.skills.farming

import org.rsmod.api.table.farming.FarmingCropRow

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
    HERB;

    companion object {
        fun of(category: String): PatchKind = valueOf(category.uppercase())
    }
}

/**
 * [growBase] is the index into the patch loc's own transform table: the client renders
 * `transforms[growBase + stage]`. Allotments and flowers offset that by +64 watered, +128 diseased
 * and +192 dead; herb patches pack their diseased art separately, which is what [diseasedBase] is
 * for.
 *
 * [transmit] is the escape hatch for a crop whose art the cache scattered instead of laying out in
 * one run. When it is set it replaces all of the above: healthy, watered, diseased and dead banks
 * back to back, each [stages] + 1 long and indexed by stage.
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
    val transmit: List<Int> = emptyList(),
)

private fun FarmingCropRow.toCrop(): Crop =
    Crop(
        kind = PatchKind.of(category),
        name = name,
        seed = input.internalName,
        produce = output.internalName,
        level = statReq.first().t1,
        plantXp = plantXp / 10.0,
        harvestXp = xp / 10.0,
        growBase = growBase,
        stages = stages,
        stageMinutes = stageMinutes,
        diseasedBase = diseasedBase,
        seedsPerPlant = inputAmount,
        transmit = transmit,
    )

object FarmingCrops {
    val all: List<Crop> by lazy { FarmingCropRow.all().map(FarmingCropRow::toCrop) }

    private val bySeed: Map<String, Crop> by lazy { all.associateBy(Crop::seed) }

    fun bySeed(seed: String): Crop? = bySeed[seed]

    fun index(crop: Crop): Int = all.indexOf(crop) + 1

    fun byIndex(index: Int): Crop? = all.getOrNull(index - 1)
}

/**
 * [PatchState] persists the crop as an index so it stays a plain value; resolving that index is a
 * table lookup, which is why it lives here rather than on the state itself.
 */
val PatchState.crop: Crop?
    get() = if (cropIndex == 0) null else FarmingCrops.byIndex(cropIndex)

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
