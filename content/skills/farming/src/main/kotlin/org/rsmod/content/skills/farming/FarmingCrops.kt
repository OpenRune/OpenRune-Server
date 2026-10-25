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
internal const val ANIM_COMPOST: String = "seq.farming_trowel_digging"

enum class PatchKind {
    ALLOTMENT,
    FLOWER,
    HERB,
    HOPS,
    BUSH,
    TREE,
    FRUIT_TREE,
    SPIRIT_TREE,
    SEAWEED,
    CORAL,
    GRAPE_VINE,
    MUSHROOM,
    BELLADONNA,
    HESPORI,
    ANIMA,
    HARDWOOD_TREE,
    CALQUAT,
    CRYSTAL_TREE,
    CELASTRUS,
    REDWOOD,
    CACTUS;

    val label: String = "${name.lowercase().replace('_', ' ')} patch"

    /** Only allotment, flower and hops patches can be watered (Farming; Watering can). */
    val waterable: Boolean
        get() = this == ALLOTMENT || this == FLOWER || this == HOPS

    /** Crops cured by pruning with secateurs rather than plant cure. */
    val prunes: Boolean
        get() =
            this == BUSH || this == TREE || this == FRUIT_TREE || this == SPIRIT_TREE ||
                this == HARDWOOD_TREE || this == CALQUAT || this == CELASTRUS || this == REDWOOD

    companion object {
        fun of(name: String): PatchKind = valueOf(name.uppercase())
    }
}

enum class HarvestModel {
    LIVES,
    BATCH,
    PICK,
    CHOP,
    NONE,
}

/** Transmit values, each an index into the patch kind's multiloc transform table. */
data class CropArt(
    val healthy: List<Int>,
    val watered: List<Int> = emptyList(),
    val diseased: List<Int> = emptyList(),
    val dead: List<Int> = emptyList(),
    val produce: List<Int> = emptyList(),
    val stump: Int? = null,
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
    val checkXp: Double = 0.0,
    val firstStageMinutes: Int = 0,
    val seedsPerPlant: Int = 1,
    val saveLow: Int? = null,
    val saveHigh: Int? = null,
    val harvestLives: Int? = null,
    val harvestAmount: Int = 1,
    val harvestLevelDivisor: Int = 0,
    val protectionIndex: Int = 0,
    val diseaseMax: Int? = null,
    val diseaseLegacy: Boolean = false,
    val regrowMinutes: Int = 0,
    val petChance: Int = 0,
    val roots: String? = null,
    val woodLoc: String? = null,
    val payment: List<Pair<String, Int>> = emptyList(),
    val magicSecateurs: Boolean = false,
    val immune: Boolean = false,
    val deathMinutes: Int? = null,
    val held: String? = null,
    val hesporiSeedChance: Int? = null,
    val growthSkipPercent: Int = 0,
) {
    val checksHealth: Boolean
        get() = checkXp > 0.0

    val maxProduce: Int
        get() = (art.produce.size - 1).coerceAtLeast(0)

    /**
     * Whether this crop's harvest is source-backed. Unsourced yields (null save constants on a
     * lives crop) and Woodcutting-driven chopping stay unshipped.
     */
    val harvestShipped: Boolean
        get() =
            when (harvest) {
                HarvestModel.LIVES -> saveLow != null && saveHigh != null && produce != null
                HarvestModel.BATCH, HarvestModel.PICK -> produce != null
                HarvestModel.CHOP, HarvestModel.NONE -> false
            }

    /**
     * Content availability, not an OSRS planting requirement. Held metadata remains addressable
     * for saved crops and consumers; unavailable mechanics are not exposed as playable content.
     */
    val shipped: Boolean
        get() {
            val disease = immune || (diseaseLegacy || diseaseMax != null) && deathMinutes != null
            return held == null && disease && (harvestShipped || kind == PatchKind.ANIMA)
        }
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
        art =
            CropArt(
                healthy = artHealthy,
                watered = artWatered,
                diseased = artDiseased,
                dead = artDead,
                produce = artProduce,
                stump = artStump,
            ),
        harvest = HarvestModel.valueOf(harvest),
        checkXp = checkXp / 10.0,
        firstStageMinutes = firstStageMinutes,
        seedsPerPlant = inputAmount,
        saveLow = save.getOrNull(0),
        saveHigh = save.getOrNull(1),
        harvestLives = harvestLives,
        harvestAmount = harvestAmount,
        harvestLevelDivisor = harvestLevelDivisor,
        protectionIndex = protection?.let { indices.getValue(it.rowId) } ?: 0,
        diseaseMax = diseaseMax,
        diseaseLegacy = diseaseLegacy,
        regrowMinutes = regrowMinutes,
        petChance = petChance,
        hesporiSeedChance = hesporiSeedChance,
        roots = roots?.internalName,
        woodLoc = woodLoc?.internalName,
        payment = payment.map { it.t0.internalName to it.t1 },
        magicSecateurs = magicSecateurs,
        immune = immune,
        deathMinutes = deathMinutes,
        held = held,
        growthSkipPercent = growthSkipPercent,
    )

object FarmingCrops {
    val all: List<Crop> by lazy {
        val rows = FarmingCropRow.all().sortedBy(FarmingCropRow::cropIndex)
        val indices = rows.associate { it.rowId to it.cropIndex }
        rows.map { it.toCrop(indices) }.also { crops ->
            check(crops.map(Crop::index) == (1..crops.size).toList()) { "Crop indices have gaps" }
        }
    }

    private val bySeed: Map<String, Crop> by lazy { all.associateBy(Crop::seed) }

    fun bySeed(seed: String): Crop? = bySeed[seed]

    fun byIndex(index: Int): Crop? = all.getOrNull(index - 1)
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
