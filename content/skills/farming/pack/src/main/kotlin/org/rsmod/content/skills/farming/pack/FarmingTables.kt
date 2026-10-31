package org.rsmod.content.skills.farming.pack

import dev.openrune.OsrsCacheProvider
import dev.openrune.definition.codec.ObjectCodec
import dev.openrune.definition.dbtables.DBRowBuilder
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.filesystem.Cache
import dev.openrune.getCacheLocation
import dev.openrune.revision
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.tables.production.productionTable
import java.nio.file.Path
import kotlin.math.roundToInt
import org.rsmod.map.CoordGrid

/**
 * One crop. Art lists hold loc symbols that are resolved at pack time into transmit values: the
 * index of that loc in the transform table of the crop kind's representative patch.
 *
 * [healthy] has `stages + 1` entries, the last being the mature art. [watered], [diseased] and
 * [dead] are indexed the same way; `null` entries reuse the nearest earlier art (stage 0 reuses
 * stage 1).
 *
 * [harvest] is one of `LIVES` (save chance from [saveLow]/[saveHigh], starting lives from compost)
 * or `BATCH` ([harvestAmount] plus a level roll over [harvestLevelDivisor]). Null save constants
 * on a `LIVES` crop mean its yield is not sourced, so its harvest stays unshipped.
 *
 * Disease needs a sourced [diseaseMax] (or the inherited [diseaseLegacy] roll) plus the diseased
 * crop's [deathMinutes] tick.
 */
data class CropSpec(
    val key: String,
    val index: Int,
    val kind: String,
    val name: String,
    val seed: String,
    val produce: String?,
    val level: Int,
    val plantXp: Double,
    val stages: Int,
    val cycleMinutes: Int,
    val healthy: List<String>,
    val seeds: Int = 1,
    val harvestXp: Double = 0.0,
    val watered: List<String?> = emptyList(),
    val diseased: List<String?> = emptyList(),
    val dead: List<String?> = emptyList(),
    val harvest: String = "LIVES",
    val saveLow: Int? = null,
    val saveHigh: Int? = null,
    val harvestAmount: Int = 1,
    val harvestLevelDivisor: Int = 0,
    val protection: String? = null,
    val diseaseMax: Int? = null,
    val diseaseLegacy: Boolean = false,
    val deathMinutes: Int? = null,
)

data class PatchSpec(val loc: String, val varp: String, val site: String, val coord: CoordGrid)

/**
 * A per-site rule on a `varbit.` or `varp.` gameval [variable] (empty = always). For disease-free
 * rules [value] is the threshold the var must reach; for XP/yield bonuses the var must be at least
 * 1 and [value] is the bonus.
 */
data class SiteRule(val site: String, val variable: String, val value: Int)

data class SiteAccess(val site: String, val quest: String = "", val variable: String = "", val minimum: Int = 0, val message: String)

data class PatchKindSpec(
    val kind: String,
    val row: String,
    val patches: List<PatchSpec>,
    val diseaseFree: List<SiteRule> = emptyList(),
    val xpBonus: List<SiteRule> = emptyList(),
    val yieldBonus: List<SiteRule> = emptyList(),
    val access: List<SiteAccess> = emptyList(),
)

object FarmingTables {
    const val COL_NAME = 7
    const val COL_PLANT_XP = 8
    const val COL_STAGES = 9
    const val COL_CYCLE_MINUTES = 10
    const val COL_PROTECTION = 11
    const val COL_ART_HEALTHY = 12
    const val COL_ART_WATERED = 13
    const val COL_ART_DISEASED = 14
    const val COL_ART_DEAD = 15
    const val COL_HARVEST = 16
    const val COL_SAVE = 17
    const val COL_HARVEST_AMOUNT = 18
    const val COL_HARVEST_LEVEL_DIVISOR = 19
    const val COL_CROP_INDEX = 20
    const val COL_DISEASE_MAX = 21
    const val COL_DISEASE_LEGACY = 22
    const val COL_DEATH_MINUTES = 23

    const val COMPOST_KIND = 0
    const val COMPOST_OBJ = 1
    const val COMPOST_XP = 2
    const val COMPOST_HARVEST_LIVES = 3
    const val COMPOST_DISEASE_CHANCE = 4
    const val COMPOST_DISEASE_DIVISOR = 5

    const val PATCH_KIND = 0
    const val PATCH_PATCHES = 1
    const val PATCH_DISEASE_FREE = 2
    const val PATCH_XP_BONUS = 3
    const val PATCH_YIELD_BONUS = 4
    const val PATCH_ACCESS = 5

    /** Variant patches of a kind share transmit values; only their child models differ. */
    private val REPRESENTATIVE_PATCH =
        mapOf(
            "ALLOTMENT" to "loc.farming_veg_patch_1",
            "FLOWER" to "loc.farming_flower_patch_1",
            "HERB" to "loc.farming_herb_patch_1",
        )

    private fun patchTransforms(): Map<String, List<Int>> {
        val decoder = OsrsCacheProvider.ObjectDecoder(revision.first)
        val cache = Cache.load(Path.of(getCacheLocation()))
        return try {
            REPRESENTATIVE_PATCH.mapValues { (_, patch) ->
                val id = patch.asRSCM()
                val data =
                    checkNotNull(
                        cache.data(decoder.index, decoder.getArchive(id), decoder.getFile(id)),
                    ) { "Missing $patch" }
                checkNotNull(ObjectCodec(revision.first).loadData(id, data).transforms) {
                    "Missing transforms for $patch"
                }
            }
        } finally {
            cache.close()
        }
    }

    internal fun nourishmentThreshold(): Int {
        val decoder = OsrsCacheProvider.ObjectDecoder(revision.first)
        val cache = Cache.load(Path.of(getCacheLocation()))
        return try {
            val id = "loc.my2arm_herbpatch_fire".asRSCM()
            val data = checkNotNull(cache.data(decoder.index, decoder.getArchive(id), decoder.getFile(id)))
            val transforms = checkNotNull(ObjectCodec(revision.first).loadData(id, data).transforms)
            check(transforms.last() == "loc.my2arm_herbpatch_fire_special".asRSCM())
            check(transforms.dropLast(1).all { it == "loc.my2arm_fire_pit_empty".asRSCM() })
            transforms.lastIndex
        } finally {
            cache.close()
        }
    }

    val CROPS: List<CropSpec>
        get() = LEGACY_CROPS

    /** `crop_index` is persisted in seven bits of the patch varp: never renumber a crop. */
    fun crops() =
        productionTable(
            "dbtable.farming_crop",
            serverOnly = true,
            requireOutput = false,
            extraColumns = {
                column("name", COL_NAME, VarType.STRING)
                column("plant_xp", COL_PLANT_XP, VarType.INT)
                column("stages", COL_STAGES, VarType.INT)
                column("cycle_minutes", COL_CYCLE_MINUTES, VarType.INT)
                column("protection", COL_PROTECTION, VarType.DBROW)
                column("art_healthy", COL_ART_HEALTHY, VarType.INT)
                column("art_watered", COL_ART_WATERED, VarType.INT)
                column("art_diseased", COL_ART_DISEASED, VarType.INT)
                column("art_dead", COL_ART_DEAD, VarType.INT)
                column("harvest", COL_HARVEST, VarType.STRING)
                column("save", COL_SAVE, VarType.INT, VarType.INT)
                column("harvest_amount", COL_HARVEST_AMOUNT, VarType.INT)
                column("harvest_level_divisor", COL_HARVEST_LEVEL_DIVISOR, VarType.INT)
                column("crop_index", COL_CROP_INDEX, VarType.INT)
                column("disease_max", COL_DISEASE_MAX, VarType.INT)
                column("disease_legacy", COL_DISEASE_LEGACY, VarType.BOOLEAN)
                column("death_minutes", COL_DEATH_MINUTES, VarType.INT)
            },
        ) {
            val transforms = patchTransforms()
            val specs = CROPS.sortedBy(CropSpec::index)
            val indices = specs.map(CropSpec::index)
            check(indices.distinct().size == indices.size && indices.all { it in 1..MAX_CROP_INDEX }) {
                "Crop indices must be unique and within 1..$MAX_CROP_INDEX"
            }

            fun resolve(spec: CropSpec, symbol: String): Int {
                val transforms = transforms.getValue(spec.kind)
                return transforms.indexOf(symbol.asRSCM()).also {
                    check(it >= 0) { "$symbol is not a transform of ${spec.kind} patches" }
                }
            }

            fun stageArt(spec: CropSpec, art: List<String?>): List<Int> {
                check(art.size == spec.stages + 1) {
                    "${spec.key}: stage art needs ${spec.stages + 1} entries, has ${art.size}"
                }
                val firstKnown = checkNotNull(art.firstOrNull { it != null }) { spec.key }
                var last = firstKnown
                return art.map { symbol -> resolve(spec, (symbol ?: last).also { last = it }) }
            }

            for (spec in specs) {
                row("dbrow.farming_${spec.key}") {
                    production {
                        input(spec.seed, spec.seeds)
                        statReq("stat.farming", spec.level)
                        xp((spec.harvestXp * 10).roundToInt())
                        spec.produce?.let { output(it) }
                        category(spec.kind)
                    }
                    column(COL_NAME, spec.name.lowercase())
                    column(COL_PLANT_XP, (spec.plantXp * 10).roundToInt())
                    column(COL_STAGES, spec.stages)
                    column(COL_CYCLE_MINUTES, spec.cycleMinutes)
                    spec.protection?.let { columnRSCM(COL_PROTECTION, it) }
                    column(COL_ART_HEALTHY, *stageArt(spec, spec.healthy).toTypedArray())
                    if (spec.watered.isNotEmpty()) {
                        column(COL_ART_WATERED, *stageArt(spec, spec.watered).toTypedArray())
                    }
                    if (spec.diseased.isNotEmpty()) {
                        column(COL_ART_DISEASED, *stageArt(spec, spec.diseased).toTypedArray())
                    }
                    if (spec.dead.isNotEmpty()) {
                        column(COL_ART_DEAD, *stageArt(spec, spec.dead).toTypedArray())
                    }
                    column(COL_HARVEST, spec.harvest)
                    if (spec.saveLow != null && spec.saveHigh != null) {
                        column(COL_SAVE, spec.saveLow, spec.saveHigh)
                    }
                    column(COL_HARVEST_AMOUNT, spec.harvestAmount)
                    column(COL_HARVEST_LEVEL_DIVISOR, spec.harvestLevelDivisor)
                    column(COL_CROP_INDEX, spec.index)
                    spec.diseaseMax?.let { column(COL_DISEASE_MAX, it) }
                    column(COL_DISEASE_LEGACY, spec.diseaseLegacy)
                    spec.deathMinutes?.let { column(COL_DEATH_MINUTES, it) }
                }
            }
        }

    fun composts() =
        dbTable("dbtable.farming_compost", serverOnly = true) {
            column("kind", COMPOST_KIND, VarType.STRING)
            column("obj", COMPOST_OBJ, VarType.OBJ)
            column("xp", COMPOST_XP, VarType.INT)
            column("harvest_lives", COMPOST_HARVEST_LIVES, VarType.INT)
            column("disease_chance", COMPOST_DISEASE_CHANCE, VarType.INT)
            column("disease_divisor", COMPOST_DISEASE_DIVISOR, VarType.INT)

            fun compost(
                kind: String,
                obj: String?,
                xp: Int,
                harvestLives: Int,
                diseaseChance: Int,
                diseaseDivisor: Int,
            ) =
                row("dbrow.farming_compost_${kind.lowercase()}") {
                    column(COMPOST_KIND, kind)
                    obj?.let { columnRSCM(COMPOST_OBJ, it) }
                    column(COMPOST_XP, xp)
                    column(COMPOST_HARVEST_LIVES, harvestLives)
                    column(COMPOST_DISEASE_CHANCE, diseaseChance)
                    column(COMPOST_DISEASE_DIVISOR, diseaseDivisor)
                }

            // UNVERIFIED: disease_chance for disease_legacy crops is an inherited approximation (guess)
            compost("NONE", null, 0, 3, 350, 1)
            compost("NORMAL", "obj.bucket_compost", 180, 4, 175, 2)
            compost("SUPER", "obj.bucket_supercompost", 260, 5, 70, 5)
            compost("ULTRA", "obj.bucket_ultracompost", 360, 6, 35, 10)
        }

    /** One row per patch kind; each patch is a (loc, varp, site, coord) record. */
    fun patches() =
        dbTable("dbtable.farming_patch", serverOnly = true) {
            column("kind", PATCH_KIND, VarType.STRING)
            column("patches", PATCH_PATCHES, VarType.LOC, VarType.VARP, VarType.STRING, VarType.COORDGRID)
            column("disease_free", PATCH_DISEASE_FREE, VarType.STRING, VarType.STRING, VarType.INT)
            column("xp_bonus", PATCH_XP_BONUS, VarType.STRING, VarType.STRING, VarType.INT)
            column("yield_bonus", PATCH_YIELD_BONUS, VarType.STRING, VarType.STRING, VarType.INT)
            column("access", PATCH_ACCESS, VarType.STRING, VarType.STRING, VarType.STRING, VarType.INT, VarType.STRING)

            fun DBRowBuilder.rules(col: Int, rules: List<SiteRule>) {
                if (rules.isEmpty()) return
                column(col, *rules.flatMap { listOf<Any>(it.site, it.variable, it.value) }.toTypedArray())
            }

            for (spec in FARMING_PATCH_KINDS) {
                row(spec.row) {
                    column(PATCH_KIND, spec.kind)
                    val values =
                        spec.patches.flatMap {
                            listOf<Any>(it.loc.asRSCM(), it.varp.asRSCM(), it.site, it.coord.packed)
                        }
                    column(PATCH_PATCHES, *values.toTypedArray())
                    rules(PATCH_DISEASE_FREE, spec.diseaseFree)
                    rules(PATCH_XP_BONUS, spec.xpBonus)
                    rules(PATCH_YIELD_BONUS, spec.yieldBonus)
                    if (spec.access.isNotEmpty()) {
                        column(PATCH_ACCESS, *spec.access.flatMap {
                            listOf<Any>(it.site, it.quest, it.variable, it.minimum, it.message)
                        }.toTypedArray())
                    }
                }
            }
        }

    private const val MAX_CROP_INDEX = 127
}
