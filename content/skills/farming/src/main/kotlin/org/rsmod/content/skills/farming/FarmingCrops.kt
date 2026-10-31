package org.rsmod.content.skills.farming

import org.rsmod.api.table.farming.FarmingCompostRow
import org.rsmod.api.table.farming.FarmingCropRow

internal const val STAT_FARMING: String = "stat.farming"

internal const val RAKE: String = "obj.rake"
internal const val DIBBER: String = "obj.dibber"
internal const val SPADE: String = "obj.spade"
internal const val PLANT_CURE: String = "obj.plant_cure"
internal const val WEEDS: String = "obj.weeds"

internal const val NOTHING: String = "Nothing interesting happens."

internal const val ANIM_RAKE: String = "seq.farming_raking"
internal const val ANIM_PLANT: String = "seq.farming_seed_dibbing"
internal const val ANIM_WATER: String = "seq.farming_watering"
internal const val ANIM_HARVEST: String = "seq.picking_low"
internal const val ANIM_CURE: String = "seq.farming_plant_cure"
internal const val ANIM_POUR_COMPOST: String = "seq.farming_pour_water"
internal const val SOUND_COMPOST: String = "synth.farming_compost"
internal const val SOUND_PLANT: String = "synth.farming_seed_dibbing"
internal const val SOUND_CURE: String = "synth.farming_plant_cure"
internal const val SOUND_DIG: String = "synth.digspade"
internal const val SOUND_PICK: String = "synth.pick"

enum class PatchKind {
    ALLOTMENT,
    FLOWER,
    HERB;

    val label: String
        get() = if (this == ALLOTMENT) "allotment" else "${name.lowercase().replace('_', ' ')} patch"

    /** Only allotment and flower patches can be watered. */
    val waterable: Boolean
        get() = this == ALLOTMENT || this == FLOWER

    companion object {
        fun of(name: String): PatchKind = valueOf(name.uppercase())
    }
}

enum class HarvestModel {
    LIVES,
    BATCH,
}

/** Transmit values, each an index into the patch kind's multiloc transform table. */
data class CropArt(
    val healthy: List<Int>,
    val watered: List<Int> = emptyList(),
    val diseased: List<Int> = emptyList(),
    val dead: List<Int> = emptyList(),
)

data class Crop(
    val index: Int,
    val kind: PatchKind,
    val name: String,
    val seed: String,
    val produce: String?,
    val level: Int,
    val plantXp: Double,
    val harvestXp: Double,
    val stages: Int,
    val cycleMinutes: Int,
    val art: CropArt,
    val harvest: HarvestModel = HarvestModel.LIVES,
    val seedsPerPlant: Int = 1,
    val saveLow: Int? = null,
    val saveHigh: Int? = null,
    val harvestAmount: Int = 1,
    val harvestLevelDivisor: Int = 0,
    val protectionIndex: Int = 0,
    val diseaseMax: Int? = null,
    val diseaseLegacy: Boolean = false,
    val deathMinutes: Int? = null,
) {
    /** Whether this crop's harvest is source-backed: unsourced yields (null save constants) stay unshipped. */
    val harvestShipped: Boolean
        get() =
            when (harvest) {
                HarvestModel.LIVES -> saveLow != null && saveHigh != null && produce != null
                HarvestModel.BATCH -> produce != null
            }

    /** Content availability, not an OSRS planting requirement. */
    val shipped: Boolean
        get() = (diseaseLegacy || diseaseMax != null) && deathMinutes != null && harvestShipped
}

private fun FarmingCropRow.toCrop(indices: Map<Int, Int>): Crop =
    Crop(
        index = cropIndex,
        kind = PatchKind.of(category),
        name = name,
        seed = input.internalName,
        produce = output?.internalName,
        level = statReq.first().t1,
        plantXp = plantXp / 10.0,
        harvestXp = xp / 10.0,
        stages = stages,
        cycleMinutes = cycleMinutes,
        art = CropArt(healthy = artHealthy, watered = artWatered, diseased = artDiseased, dead = artDead),
        harvest = HarvestModel.valueOf(harvest),
        seedsPerPlant = inputAmount,
        saveLow = save.getOrNull(0),
        saveHigh = save.getOrNull(1),
        harvestAmount = harvestAmount,
        harvestLevelDivisor = harvestLevelDivisor,
        protectionIndex = protection?.let { indices.getValue(it.rowId) } ?: 0,
        diseaseMax = diseaseMax,
        diseaseLegacy = diseaseLegacy,
        deathMinutes = deathMinutes,
    )

object FarmingCrops {
    val all: List<Crop> by lazy {
        val rows = FarmingCropRow.all().sortedBy(FarmingCropRow::cropIndex)
        val indices = rows.associate { it.rowId to it.cropIndex }
        rows.map { it.toCrop(indices) }.also { crops ->
            check(crops.map(Crop::index).distinct().size == crops.size) { "Crop indices are not unique" }
        }
    }

    private val bySeed: Map<String, Crop> by lazy { all.associateBy(Crop::seed) }

    private val byIndex: Map<Int, Crop> by lazy { all.associateBy(Crop::index) }

    fun bySeed(seed: String): Crop? = bySeed[seed]

    fun byIndex(index: Int): Crop? = byIndex[index]
}

/**
 * [PatchState] persists the crop as an index so it stays a plain value; resolving that index is a
 * table lookup, which is why it lives here rather than on the state itself.
 */
val PatchState.crop: Crop?
    get() = if (cropIndex == 0) null else FarmingCrops.byIndex(cropIndex)

enum class Compost(val label: String) {
    NONE("none"),
    NORMAL("compost"),
    SUPER("supercompost"),
    ULTRA("ultracompost"),
}

data class CompostDef(
    val kind: Compost,
    val obj: String?,
    val xp: Double,
    val harvestLives: Int,
    val diseaseChance: Double,
    val diseaseDivisor: Int,
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
                    diseaseDivisor = row.diseaseDivisor,
                )
        }
    }

    fun get(kind: Compost): CompostDef = definitions.getValue(kind)

    fun byObj(obj: String): CompostDef? = definitions.values.firstOrNull { it.obj == obj }
}
