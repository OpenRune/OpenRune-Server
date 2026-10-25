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
 * index of that loc in the transform table of the crop kind's representative patch. `#N` is a
 * literal transmit value, for multi-part patches whose parts repeat one child across states.
 *
 * [healthy] has `stages + 1` entries, the last being the mature art (the Check-health art for
 * crops with [checkXp]). [watered], [diseased] and [dead] are indexed the same way; `null`
 * entries reuse the nearest earlier art (stage 0 reuses stage 1). [produceArt] is the checked or
 * renewable art by remaining produce count; [stump] the chopped-down art.
 *
 * [harvest] is one of `LIVES` (save chance from [saveLow]/[saveHigh], starting lives from compost
 * or [harvestLives]), `BATCH` ([harvestAmount] plus a level roll over [harvestLevelDivisor]),
 * `PICK` (one produce per pick from [produceArt] counts, regrowing every [regrowMinutes]),
 * `CHOP` (logs through Woodcutting against [woodLoc]) or `NONE`. Null save constants on a `LIVES`
 * crop mean its yield is not sourced, so its harvest stays unshipped.
 *
 * Disease needs a sourced [diseaseMax] (or the inherited [diseaseLegacy] roll) plus the diseased
 * crop's [deathMinutes] tick; [immune] marks crops sourced as disease-free. Missing mechanics or
 * a [held] reason mark unsupported content, not an additional OSRS planting requirement.
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
    val petChance: Int,
    val seeds: Int = 1,
    val checkXp: Double = 0.0,
    val harvestXp: Double = 0.0,
    val firstStageMinutes: Int = 0,
    val watered: List<String?> = emptyList(),
    val diseased: List<String?> = emptyList(),
    val dead: List<String?> = emptyList(),
    val produceArt: List<String> = emptyList(),
    val harvest: String = "LIVES",
    val harvestLives: Int? = null,
    val stump: String? = null,
    val saveLow: Int? = null,
    val saveHigh: Int? = null,
    val harvestAmount: Int = 1,
    val harvestLevelDivisor: Int = 0,
    val protection: String? = null,
    val diseaseMax: Int? = null,
    val diseaseLegacy: Boolean = false,
    val regrowMinutes: Int = 0,
    val roots: String? = null,
    val woodLoc: String? = null,
    val payment: List<Pair<String, Int>> = emptyList(),
    val magicSecateurs: Boolean = false,
    val immune: Boolean = false,
    val deathMinutes: Int? = null,
    val held: String? = null,
    val growthSkipPercent: Int = 0,
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
    const val COL_CHECK_XP = 9
    const val COL_STAGES = 10
    const val COL_CYCLE_MINUTES = 11
    const val COL_FIRST_STAGE_MINUTES = 12
    const val COL_PROTECTION = 13
    const val COL_ART_HEALTHY = 14
    const val COL_ART_WATERED = 15
    const val COL_ART_DISEASED = 16
    const val COL_ART_DEAD = 17
    const val COL_ART_PRODUCE = 18
    const val COL_HARVEST = 19
    const val COL_ART_STUMP = 20
    const val COL_SAVE = 21
    const val COL_HARVEST_AMOUNT = 22
    const val COL_HARVEST_LEVEL_DIVISOR = 23
    const val COL_CROP_INDEX = 24
    const val COL_DISEASE_MAX = 25
    const val COL_DISEASE_LEGACY = 26
    const val COL_REGROW_MINUTES = 27
    const val COL_PET_CHANCE = 28
    const val COL_ROOTS = 29
    const val COL_WOOD_LOC = 30
    const val COL_PAYMENT = 31
    const val COL_MAGIC_SECATEURS = 32
    const val COL_HARVEST_LIVES = 33
    const val COL_IMMUNE = 34
    const val COL_DEATH_MINUTES = 35
    const val COL_HELD = 36
    const val COL_HESPORI_SEED_CHANCE = 37
    const val COL_GROWTH_SKIP_PERCENT = 38

    const val COMPOST_KIND = 0
    const val COMPOST_OBJ = 1
    const val COMPOST_XP = 2
    const val COMPOST_HARVEST_LIVES = 3
    const val COMPOST_DISEASE_CHANCE = 4
    const val COMPOST_DISEASE_DIVISOR = 5
    const val COMPOST_BUCKETS = 6
    const val COMPOST_UPGRADES = 7
    const val COMPOST_CAPACITY = 8
    const val COMPOST_CHARGES_PER_BUCKET = 9

    const val PATCH_KIND = 0
    const val PATCH_PATCHES = 1
    const val PATCH_DISEASE_FREE = 2
    const val PATCH_XP_BONUS = 3
    const val PATCH_YIELD_BONUS = 4
    const val PATCH_ACCESS = 5

    // Hespori seed oldid15304593; barley/hammerstone's 244 contradicts the 224 formula.
    private val HESPORI_SEED_CHANCES =
        mapOf(
            281_040 to 562,
            224_832 to 449,
            187_360 to 374,
            160_594 to 321,
            173_977 to 347,
            112_416 to null,
            89_933 to 179,
            74_944 to 149,
            64_238 to 128,
            56_208 to 112,
            98_364 to 196,
            44_966 to 89,
            37_472 to 74,
            32_119 to 64,
            28_104 to 56,
            22_483 to 44,
            16_059 to 32,
            14_052 to 28,
            11_242 to 22,
            9_368 to 18,
            9_000 to 18,
            7_500 to 15,
            7_000 to 14,
            8_000 to 16,
            6_000 to 12,
            5_000 to 10,
            385_426 to 770,
        )

    /** Variant patches of a kind share transmit values; only their child models differ. */
    private val REPRESENTATIVE_PATCH =
        mapOf(
            "ALLOTMENT" to "loc.farming_veg_patch_1",
            "FLOWER" to "loc.farming_flower_patch_1",
            "HERB" to "loc.farming_herb_patch_1",
            "HOPS" to "loc.farming_hops_patch_1",
            "BUSH" to "loc.farming_bush_patch_1",
            "TREE" to "loc.farming_tree_patch_1",
            "FRUIT_TREE" to "loc.farming_fruit_tree_patch_1",
            "SPIRIT_TREE" to "loc.farming_spirit_tree_patch_1",
            "SEAWEED" to "loc.farming_seaweed_patch_1",
            "CORAL" to "loc.farming_coral_patch_1",
            "GRAPE_VINE" to "loc.grapevine_patch_01",
            "MUSHROOM" to "loc.farming_mushroom_patch",
            "BELLADONNA" to "loc.farming_belladonna_patch",
            "HESPORI" to "loc.hespori_patch",
            "ANIMA" to "loc.farming_anima_patch_1",
            "HARDWOOD_TREE" to "loc.farming_hardwood_tree_patch_1",
            "CALQUAT" to "loc.farming_calquat_tree_patch",
            "CRYSTAL_TREE" to "loc.farming_crystal_tree_patch_1",
            "CELASTRUS" to "loc.farming_celastrus_patch_1",
            "REDWOOD" to "loc.farming_redwood_tree_patch_0_1",
            "CACTUS" to "loc.farming_cactus_patch",
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
        get() =
            LEGACY_CROPS + HOPS_CROPS + BUSH_CROPS + TREE_CROPS + FRUIT_TREE_CROPS +
                SPECIAL_CROPS

    /** `crop_index` is persisted in seven bits of the patch varp: never renumber a crop. */
    fun crops() =
        productionTable(
            "dbtable.farming_crop",
            serverOnly = true,
            requireOutput = false,
            extraColumns = {
                column("name", COL_NAME, VarType.STRING)
                column("plant_xp", COL_PLANT_XP, VarType.INT)
                column("check_xp", COL_CHECK_XP, VarType.INT)
                column("stages", COL_STAGES, VarType.INT)
                column("cycle_minutes", COL_CYCLE_MINUTES, VarType.INT)
                column("first_stage_minutes", COL_FIRST_STAGE_MINUTES, VarType.INT)
                column("protection", COL_PROTECTION, VarType.DBROW)
                column("art_healthy", COL_ART_HEALTHY, VarType.INT)
                column("art_watered", COL_ART_WATERED, VarType.INT)
                column("art_diseased", COL_ART_DISEASED, VarType.INT)
                column("art_dead", COL_ART_DEAD, VarType.INT)
                column("art_produce", COL_ART_PRODUCE, VarType.INT)
                column("harvest", COL_HARVEST, VarType.STRING)
                column("art_stump", COL_ART_STUMP, VarType.INT)
                column("save", COL_SAVE, VarType.INT, VarType.INT)
                column("harvest_amount", COL_HARVEST_AMOUNT, VarType.INT)
                column("harvest_level_divisor", COL_HARVEST_LEVEL_DIVISOR, VarType.INT)
                column("crop_index", COL_CROP_INDEX, VarType.INT)
                column("disease_max", COL_DISEASE_MAX, VarType.INT)
                column("disease_legacy", COL_DISEASE_LEGACY, VarType.BOOLEAN)
                column("regrow_minutes", COL_REGROW_MINUTES, VarType.INT)
                column("pet_chance", COL_PET_CHANCE, VarType.INT)
                column("roots", COL_ROOTS, VarType.OBJ)
                column("wood_loc", COL_WOOD_LOC, VarType.LOC)
                column("payment", COL_PAYMENT, VarType.OBJ, VarType.INT)
                column("magic_secateurs", COL_MAGIC_SECATEURS, VarType.BOOLEAN)
                column("harvest_lives", COL_HARVEST_LIVES, VarType.INT)
                column("immune", COL_IMMUNE, VarType.BOOLEAN)
                column("death_minutes", COL_DEATH_MINUTES, VarType.INT)
                column("held", COL_HELD, VarType.STRING)
                column("hespori_seed_chance", COL_HESPORI_SEED_CHANCE, VarType.INT)
                column("growth_skip_percent", COL_GROWTH_SKIP_PERCENT, VarType.INT)
            },
        ) {
            val transforms = patchTransforms()
            val specs = CROPS.sortedBy(CropSpec::index)
            check(specs.map(CropSpec::index) == (1..specs.size).toList()) {
                "Crop indices must run 1..${specs.size} without gaps"
            }

            fun resolve(spec: CropSpec, symbol: String): Int {
                val transforms = transforms.getValue(spec.kind)
                if (symbol.startsWith('#')) {
                    return symbol.drop(1).toInt().also {
                        check(it in transforms.indices) { "$symbol is outside ${spec.kind} transforms" }
                    }
                }
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
                    column(COL_CHECK_XP, (spec.checkXp * 10).roundToInt())
                    column(COL_STAGES, spec.stages)
                    column(COL_CYCLE_MINUTES, spec.cycleMinutes)
                    column(COL_FIRST_STAGE_MINUTES, spec.firstStageMinutes)
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
                    if (spec.produceArt.isNotEmpty()) {
                        val art = spec.produceArt.map { resolve(spec, it) }
                        column(COL_ART_PRODUCE, *art.toTypedArray())
                    }
                    column(COL_HARVEST, spec.harvest)
                    spec.harvestLives?.let { column(COL_HARVEST_LIVES, it) }
                    spec.stump?.let { column(COL_ART_STUMP, resolve(spec, it)) }
                    if (spec.saveLow != null && spec.saveHigh != null) {
                        column(COL_SAVE, spec.saveLow, spec.saveHigh)
                    }
                    column(COL_HARVEST_AMOUNT, spec.harvestAmount)
                    column(COL_HARVEST_LEVEL_DIVISOR, spec.harvestLevelDivisor)
                    column(COL_CROP_INDEX, spec.index)
                    spec.diseaseMax?.let { column(COL_DISEASE_MAX, it) }
                    column(COL_DISEASE_LEGACY, spec.diseaseLegacy)
                    column(COL_REGROW_MINUTES, spec.regrowMinutes)
                    column(COL_PET_CHANCE, spec.petChance)
                    if (spec.kind != "ANIMA" && spec.kind != "HESPORI") {
                        HESPORI_SEED_CHANCES.getValue(spec.petChance)?.let {
                            column(COL_HESPORI_SEED_CHANCE, it)
                        }
                    }
                    spec.roots?.let { columnRSCM(COL_ROOTS, it) }
                    spec.woodLoc?.let { columnRSCM(COL_WOOD_LOC, it) }
                    if (spec.payment.isNotEmpty()) {
                        val values = spec.payment.flatMap { (obj, count) -> listOf(obj.asRSCM(), count) }
                        column(COL_PAYMENT, *values.toTypedArray())
                    }
                    column(COL_MAGIC_SECATEURS, spec.magicSecateurs)
                    column(COL_IMMUNE, spec.immune)
                    spec.deathMinutes?.let { column(COL_DEATH_MINUTES, it) }
                    spec.held?.let { column(COL_HELD, it) }
                    column(COL_GROWTH_SKIP_PERCENT, spec.growthSkipPercent)
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
            column("buckets", COMPOST_BUCKETS, VarType.OBJ, VarType.STRING, VarType.OBJ)
            column(
                "upgrades", COMPOST_UPGRADES,
                VarType.OBJ, VarType.OBJ, VarType.INT, VarType.OBJ, VarType.OBJ,
                VarType.INT, VarType.INT, VarType.INT, VarType.STRING,
            )
            column("capacity", COMPOST_CAPACITY, VarType.INT)
            column("charges_per_bucket", COMPOST_CHARGES_PER_BUCKET, VarType.INT)

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
                    FarmingCompostTable.write(this, kind)
                }

            // shortcut: disease_chance is the inherited approximation for disease_legacy crops;
            // drop it once their max rolls are sourced.
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
}
