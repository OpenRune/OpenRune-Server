package org.rsmod.content.skills.farming

import org.rsmod.api.table.farming.FarmingCompostRow
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
 * Artwork bases are resolved from named locs during cache packing. Allotments and flowers add
 * their stage to the selected base; diseased/dead herbs use three shared growth stages.
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
    val wateredBase: Int,
    val deadBase: Int,
    val diseasedBase: Int = -1,
    val seedsPerPlant: Int = 1,
    val transmit: List<Int> = emptyList(),
    val saveLow: Int = 0,
    val saveHigh: Int = 0,
    val harvestAmount: Int = 1,
    val harvestLevelDivisor: Int = 0,
    val protectionIndex: Int = 0,
)

private fun FarmingCropRow.toCrop(indices: Map<Int, Int>): Crop =
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
        wateredBase = wateredBase,
        deadBase = deadBase,
        diseasedBase = diseasedBase,
        seedsPerPlant = inputAmount,
        transmit = transmit,
        saveLow = saveLow,
        saveHigh = saveHigh,
        harvestAmount = harvestAmount,
        harvestLevelDivisor = harvestLevelDivisor,
        protectionIndex = protection?.let { indices.getValue(it.rowId) } ?: 0,
    )

object FarmingCrops {
    val all: List<Crop> by lazy {
        val rows = FarmingCropRow.all()
        val indices = rows.mapIndexed { index, row -> row.rowId to index + 1 }.toMap()
        rows.map { it.toCrop(indices) }
    }

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

enum class Compost {
    NONE,
    NORMAL,
    SUPER,
    ULTRA,
}

data class CompostDef(
    val kind: Compost,
    val obj: String?,
    val xp: Double,
    val harvestLives: Int,
    val diseaseChance: Double,
)

object FarmingComposts {
    private val definitions: Map<Compost, CompostDef> by lazy {
        FarmingCompostRow.all().associate { row ->
            val kind = Compost.valueOf(row.kind)
            kind to
                CompostDef(
                    kind = kind,
                    obj = row.obj?.internalName,
                    xp = row.xp / 10.0,
                    harvestLives = row.harvestLives,
                    diseaseChance = row.diseaseChance / 1000.0,
                )
        }
    }

    fun get(kind: Compost): CompostDef = definitions.getValue(kind)

    fun byObj(obj: String): CompostDef? = definitions.values.firstOrNull { it.obj == obj }
}
