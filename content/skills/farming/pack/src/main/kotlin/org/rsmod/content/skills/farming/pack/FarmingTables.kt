package org.rsmod.content.skills.farming.pack

import dev.openrune.OsrsCacheProvider
import dev.openrune.definition.codec.ObjectCodec
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.filesystem.Cache
import dev.openrune.getCacheLocation
import dev.openrune.pack.columnCoord
import dev.openrune.revision
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.tables.production.productionTable
import java.nio.file.Path
import org.rsmod.map.CoordGrid

/**
 * Crops and the patches they grow in.
 *
 * XP is stored multiplied by ten: two thirds of these values are fractional - a potato plants for
 * 8.5 - and the production xp column is an int. Content divides by ten once, in FarmingCrops.
 *
 * Artwork bases are resolved from named locs at pack time. Regular crops use stage arithmetic
 * checked against the patch's transform table; snape grass supplies explicit state banks.
 */
object FarmingTables {
    const val COL_NAME = 7
    const val COL_PLANT_XP = 8
    const val COL_GROW_BASE = 9
    const val COL_STAGES = 10
    const val COL_STAGE_MINUTES = 11
    const val COL_DISEASED_BASE = 12
    const val COL_PROTECTION = 13
    const val COL_TRANSMIT = 14
    const val COL_SAVE_LOW = 15
    const val COL_SAVE_HIGH = 16
    const val COL_HARVEST_AMOUNT = 17
    const val COL_HARVEST_LEVEL_DIVISOR = 18
    const val COL_WATERED_BASE = 19
    const val COL_DEAD_BASE = 20

    const val COMPOST_KIND = 0
    const val COMPOST_OBJ = 1
    const val COMPOST_XP = 2
    const val COMPOST_HARVEST_LIVES = 3
    const val COMPOST_DISEASE_CHANCE = 4

    const val PATCH_LOC = 0
    const val PATCH_KIND = 1
    const val PATCH_VARP = 2
    const val PATCH_AREA = 3
    const val PATCH_CENTRE = 4

    const val ALLOTMENT = "Allotment"
    const val FLOWER = "Flower"
    const val HERB = "Herb"

    private const val ALLOTMENT_SEEDS = 3
    private const val ALLOTMENT_MINUTES = 10
    private const val FLOWER_MINUTES = 5
    private const val HERB_MINUTES = 20

    private fun patchTransforms(): Map<String, List<Int>> {
        val decoder = OsrsCacheProvider.ObjectDecoder(revision.first)
        val cache = Cache.load(Path.of(getCacheLocation()))
        return try {
            mapOf(
                ALLOTMENT to "loc.farming_veg_patch_1",
                FLOWER to "loc.farming_flower_patch_1",
                HERB to "loc.farming_herb_patch_1",
            ).mapValues { (_, patch) ->
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

    private fun snapeGrassTransmit(transforms: List<Int>): List<Int> {
        // Healthy, watered, diseased, dead; edge stages share artwork.
        return listOf(
            "seedling", "1", "2", "3", "4", "5", "6", "fullygrown",
            "seed_watered", "1_watered", "2_watered", "3_watered",
            "4_watered", "5_watered", "6_watered", "fullygrown",
            "1_diseased", "1_diseased", "2_diseased", "3_diseased",
            "4_diseased", "5_diseased", "6_diseased", "6_diseased",
            "1_dead", "1_dead", "2_dead", "3_dead", "4_dead", "5_dead", "6_dead", "6_dead",
        ).map { stage ->
            val symbol = "loc.snapegrass_$stage"
            // Mature artwork has multiple selectors; use the first.
            transforms.indexOf(symbol.asRSCM()).also {
                check(it >= 0) { "Missing snape-grass transform: $symbol" }
            }
        }
    }

    /**
     * Row order is load-bearing: a planted patch persists its crop as a one-based index into this
     * table, packed into seven bits of the patch varp. Append new crops, never insert.
     */
    fun crops() =
        productionTable(
            "dbtable.farming_crop",
            serverOnly = true,
            extraColumns = {
                column("name", COL_NAME, VarType.STRING)
                column("plant_xp", COL_PLANT_XP, VarType.INT)
                column("grow_base", COL_GROW_BASE, VarType.INT)
                column("stages", COL_STAGES, VarType.INT)
                column("stage_minutes", COL_STAGE_MINUTES, VarType.INT)
                column("diseased_base", COL_DISEASED_BASE, VarType.INT)
                column("protection", COL_PROTECTION, VarType.DBROW)
                column("transmit", COL_TRANSMIT, VarType.INT)
                column("save_low", COL_SAVE_LOW, VarType.INT)
                column("save_high", COL_SAVE_HIGH, VarType.INT)
                column("harvest_amount", COL_HARVEST_AMOUNT, VarType.INT)
                column("harvest_level_divisor", COL_HARVEST_LEVEL_DIVISOR, VarType.INT)
                column("watered_base", COL_WATERED_BASE, VarType.INT)
                column("dead_base", COL_DEAD_BASE, VarType.INT)
            },
        ) {
            val transforms = patchTransforms()

            fun selector(kind: String, symbol: String): Int =
                transforms.getValue(kind).indexOf(symbol.asRSCM()).also {
                    check(it >= 0) { "$symbol is not a transform of $kind patches" }
                }

            fun validateStage(kind: String, base: Int, stage: Int, symbol: String) {
                check(transforms.getValue(kind).getOrNull(base + stage) == symbol.asRSCM()) {
                    "$kind selector ${base + stage} does not resolve to $symbol"
                }
            }

            fun crop(
                name: String,
                kind: String,
                seed: String,
                produce: String,
                level: Int,
                plantXp: Double,
                harvestXp: Double,
                growLoc: String,
                stages: Int,
                stageMinutes: Int,
                seeds: Int = 1,
                diseasedLoc: String = "${growLoc.substringBeforeLast('_')}_1_diseased",
                protection: String? = null,
                transmit: List<Int> = emptyList(),
                saveLow: Int = 0,
                saveHigh: Int = 0,
                harvestAmount: Int = 1,
                harvestLevelDivisor: Int = 0,
            ) =
                row("dbrow.farming_${name.replace(' ', '_')}") {
                    val prefix = growLoc.substringBeforeLast('_')
                    val herb = kind == HERB
                    val growBase = selector(kind, growLoc)
                    val wateredBase =
                        if (herb) growBase else selector(kind, "${prefix}_seed_watered")
                    val diseasedBase = selector(kind, diseasedLoc) - if (herb) 0 else 1
                    val deadBase =
                        selector(kind, if (herb) "loc.herb_1_dead" else "${prefix}_1_dead") -
                            if (herb) 0 else 1

                    if (transmit.isEmpty()) {
                        for (stage in 0..stages) {
                            val symbol =
                                when (stage) {
                                    0 -> growLoc
                                    stages -> "${prefix}_fullygrown"
                                    else -> "${prefix}_$stage"
                                }
                            validateStage(kind, growBase, stage, symbol)
                        }
                        if (herb) {
                            val diseasedPrefix = diseasedLoc.substringBeforeLast('_')
                            for (stage in 1 until stages) {
                                validateStage(kind, diseasedBase, stage - 1, "${diseasedPrefix}_$stage")
                                validateStage(kind, deadBase, stage - 1, "loc.herb_${stage}_dead")
                            }
                        } else {
                            for (stage in 0 until stages) {
                                val watered =
                                    if (stage == 0) "${prefix}_seed_watered"
                                    else "${prefix}_${stage}_watered"
                                validateStage(kind, wateredBase, stage, watered)
                                if (stage > 0) {
                                    validateStage(kind, diseasedBase, stage, "${prefix}_${stage}_diseased")
                                    validateStage(kind, deadBase, stage, "${prefix}_${stage}_dead")
                                }
                            }
                        }
                    }

                    production {
                        input(seed, seeds)
                        statReq("stat.farming", level)
                        xp((harvestXp * 10).toInt())
                        output(produce)
                        category(kind)
                    }
                    column(COL_NAME, name)
                    column(COL_PLANT_XP, (plantXp * 10).toInt())
                    column(COL_GROW_BASE, growBase)
                    column(COL_STAGES, stages)
                    column(COL_STAGE_MINUTES, stageMinutes)
                    column(COL_DISEASED_BASE, diseasedBase)
                    protection?.let { columnRSCM(COL_PROTECTION, it) }
                    if (transmit.isNotEmpty()) {
                        column(COL_TRANSMIT, *transmit.toTypedArray())
                    }
                    column(COL_SAVE_LOW, saveLow)
                    column(COL_SAVE_HIGH, saveHigh)
                    column(COL_HARVEST_AMOUNT, harvestAmount)
                    column(COL_HARVEST_LEVEL_DIVISOR, harvestLevelDivisor)
                    column(COL_WATERED_BASE, wateredBase)
                    column(COL_DEAD_BASE, deadBase)
                }

            fun allotment(
                name: String,
                seed: String,
                produce: String,
                level: Int,
                plantXp: Double,
                harvestXp: Double,
                growLoc: String,
                stages: Int,
                saveLow: Int,
                saveHigh: Int = 180,
                protection: String? = null,
                transmit: List<Int> = emptyList(),
            ) =
                crop(
                    name = name,
                    kind = ALLOTMENT,
                    seed = seed,
                    produce = produce,
                    level = level,
                    plantXp = plantXp,
                    harvestXp = harvestXp,
                    growLoc = growLoc,
                    stages = stages,
                    stageMinutes = ALLOTMENT_MINUTES,
                    seeds = ALLOTMENT_SEEDS,
                    protection = protection,
                    transmit = transmit,
                    saveLow = saveLow,
                    saveHigh = saveHigh,
                )

            fun flower(
                name: String,
                seed: String,
                produce: String,
                level: Int,
                plantXp: Double,
                harvestXp: Double,
                growLoc: String,
                harvestAmount: Int = 1,
                harvestLevelDivisor: Int = 0,
            ) =
                crop(
                    name = name,
                    kind = FLOWER,
                    seed = seed,
                    produce = produce,
                    level = level,
                    plantXp = plantXp,
                    harvestXp = harvestXp,
                    growLoc = growLoc,
                    stages = 4,
                    stageMinutes = FLOWER_MINUTES,
                    harvestAmount = harvestAmount,
                    harvestLevelDivisor = harvestLevelDivisor,
                )

            fun herb(
                name: String,
                seed: String,
                produce: String,
                level: Int,
                plantXp: Double,
                harvestXp: Double,
                growLoc: String,
                saveLow: Int,
                diseasedLoc: String = "${growLoc.removeSuffix("_seed")}_diseased_1",
            ) =
                crop(
                    name = name,
                    kind = HERB,
                    seed = seed,
                    produce = produce,
                    level = level,
                    plantXp = plantXp,
                    harvestXp = harvestXp,
                    growLoc = growLoc,
                    stages = 4,
                    stageMinutes = HERB_MINUTES,
                    diseasedLoc = diseasedLoc,
                    saveLow = saveLow,
                    saveHigh = 80,
                )

            allotment("potato", "obj.potato_seed", "obj.potato", 1, 8.0, 9.0, "loc.potato_seed", 4, 101, protection = "dbrow.farming_marigold")
            allotment("onion", "obj.onion_seed", "obj.onion", 5, 9.5, 10.5, "loc.onion_seed", 4, 105, protection = "dbrow.farming_marigold")
            allotment("cabbage", "obj.cabbage_seed", "obj.cabbage", 7, 10.0, 11.5, "loc.cabbage_seed", 4, 107, protection = "dbrow.farming_rosemary")
            allotment("tomato", "obj.tomato_seed", "obj.tomato", 12, 12.5, 14.0, "loc.tomato_seed", 4, 112, protection = "dbrow.farming_marigold")
            allotment("sweetcorn", "obj.sweetcorn_seed", "obj.sweetcorn", 20, 17.0, 19.0, "loc.sweetcorn_seed", 6, 88)
            allotment("strawberry", "obj.strawberry_seed", "obj.strawberry", 31, 26.0, 29.0, "loc.strawberry_seed", 6, 103)
            allotment(
                name = "watermelon",
                seed = "obj.watermelon_seed",
                produce = "obj.watermelon",
                level = 47,
                plantXp = 48.5,
                harvestXp = 54.5,
                growLoc = "loc.watermelon_seed",
                stages = 8,
                saveLow = 126,
                protection = "dbrow.farming_nasturtium",
            )
            allotment(
                name = "snape grass",
                seed = "obj.snape_grass_seed",
                produce = "obj.snape_grass",
                level = 61,
                plantXp = 82.0,
                harvestXp = 82.0,
                growLoc = "loc.snapegrass_seedling",
                stages = 7,
                saveLow = 148,
                saveHigh = 195,
                transmit = snapeGrassTransmit(transforms.getValue(ALLOTMENT)),
            )

            flower("marigold", "obj.marigold_seed", "obj.marigold", 2, 8.5, 47.0, "loc.marigold_seed")
            flower("rosemary", "obj.rosemary_seed", "obj.rosemary", 11, 12.0, 66.5, "loc.rosemary_seed")
            flower("nasturtium", "obj.nasturtium_seed", "obj.nasturtium", 24, 19.5, 111.0, "loc.nasturtium_seed")
            flower("woad", "obj.woad_seed", "obj.woadleaf", 25, 20.5, 115.5, "loc.woad_seed", harvestAmount = 3)
            flower("limpwurt", "obj.limpwurt_seed", "obj.limpwurt_root", 26, 21.5, 120.0, "loc.limpwurt_seed", harvestAmount = 3, harvestLevelDivisor = 10)

            herb("guam", "obj.guam_seed", "obj.unidentified_guam", 9, 11.0, 12.5, "loc.herb_guam_leaf_seed", 25)
            herb("marrentill", "obj.marrentill_seed", "obj.unidentified_marentill", 14, 13.5, 15.0, "loc.herb_marrentill_seed", 28, diseasedLoc = "loc.herb_marentill_diseased_1")
            herb("tarromin", "obj.tarromin_seed", "obj.unidentified_tarromin", 19, 16.0, 18.0, "loc.herb_tarromin_seed", 31)
            herb("harralander", "obj.harralander_seed", "obj.unidentified_harralander", 26, 21.5, 24.0, "loc.herb_harralander_seed", 36)
            herb("ranarr", "obj.ranarr_seed", "obj.unidentified_ranarr", 32, 27.0, 30.5, "loc.herb_ranarr_weed_seed", 39)
            herb("toadflax", "obj.toadflax_seed", "obj.unidentified_toadflax", 38, 34.0, 38.5, "loc.herb_toadflax_seed", 43)
            herb("irit", "obj.irit_seed", "obj.unidentified_irit", 44, 43.0, 48.5, "loc.herb_irit_leaf_seed", 46)
            herb("avantoe", "obj.avantoe_seed", "obj.unidentified_avantoe", 50, 54.5, 61.5, "loc.herb_avantoe_seed", 50)
            herb("kwuarm", "obj.kwuarm_seed", "obj.unidentified_kwuarm", 56, 69.0, 78.0, "loc.herb_kwuarm_seed", 54)
            herb("snapdragon", "obj.snapdragon_seed", "obj.unidentified_snapdragon", 62, 87.5, 98.5, "loc.herb_snapdragon_seed", 57)
            herb("cadantine", "obj.cadantine_seed", "obj.unidentified_cadantine", 67, 106.5, 120.0, "loc.herb_cadantine_seed", 60)
            herb("lantadyme", "obj.lantadyme_seed", "obj.unidentified_lantadyme", 73, 134.5, 151.5, "loc.herb_lantadyme_seed", 64)
            herb("dwarf weed", "obj.dwarf_weed_seed", "obj.unidentified_dwarf_weed", 79, 170.5, 192.0, "loc.herb_dwarf_weed_seed", 67)
            herb("torstol", "obj.torstol_seed", "obj.unidentified_torstol", 85, 199.5, 224.5, "loc.herb_torstol_seed", 71)
        }

    fun composts() =
        dbTable("dbtable.farming_compost", serverOnly = true) {
            column("kind", COMPOST_KIND, VarType.STRING)
            column("obj", COMPOST_OBJ, VarType.OBJ)
            column("xp", COMPOST_XP, VarType.INT)
            column("harvest_lives", COMPOST_HARVEST_LIVES, VarType.INT)
            column("disease_chance", COMPOST_DISEASE_CHANCE, VarType.INT)

            fun compost(
                kind: String,
                obj: String?,
                xp: Int,
                harvestLives: Int,
                diseaseChance: Int,
            ) =
                row("dbrow.farming_compost_${kind.lowercase()}") {
                    column(COMPOST_KIND, kind)
                    obj?.let { columnRSCM(COMPOST_OBJ, it) }
                    column(COMPOST_XP, xp)
                    column(COMPOST_HARVEST_LIVES, harvestLives)
                    column(COMPOST_DISEASE_CHANCE, diseaseChance)
                }

            // shortcut: approximate base disease rates; replace when exact crop rates are known.
            compost("NONE", null, 0, 3, 350)
            compost("NORMAL", "obj.bucket_compost", 180, 4, 175)
            compost("SUPER", "obj.bucket_supercompost", 260, 5, 70)
            compost("ULTRA", "obj.bucket_ultracompost", 360, 6, 35)
        }

    /**
     * All five farms share the same handful of `farming_transmit_*` varbits, so only one area's
     * patches can be described to the client at a time; `centre` is how the nearest one is picked.
     */
    fun patches() =
        dbTable("dbtable.farming_patch", serverOnly = true) {
            column("loc", PATCH_LOC, VarType.LOC)
            column("kind", PATCH_KIND, VarType.STRING)
            column("varp", PATCH_VARP, VarType.VARP)
            column("area", PATCH_AREA, VarType.STRING)
            column("centre", PATCH_CENTRE, VarType.COORDGRID)

            fun area(
                area: String,
                centre: CoordGrid,
                first: Int,
                veg1: String,
                veg2: String,
                flower: String,
                herb: String,
            ) {
                val slots =
                    listOf(veg1 to ALLOTMENT, veg2 to ALLOTMENT, flower to FLOWER, herb to HERB)
                slots.forEachIndexed { offset, (loc, kind) ->
                    row("dbrow.${loc.removePrefix("loc.")}") {
                        columnRSCM(PATCH_LOC, loc)
                        column(PATCH_KIND, kind)
                        columnRSCM(PATCH_VARP, "varp.farming_patch_%02d".format(first + offset))
                        column(PATCH_AREA, area)
                        columnCoord(PATCH_CENTRE, centre)
                    }
                }
            }

            area(
                area = "Falador",
                centre = CoordGrid(3056, 3309, 0),
                first = 0,
                veg1 = "loc.farming_veg_patch_1",
                veg2 = "loc.farming_veg_patch_2",
                flower = "loc.farming_flower_patch_1",
                herb = "loc.farming_herb_patch_1",
            )
            area(
                area = "Catherby",
                centre = CoordGrid(2811, 3464, 0),
                first = 4,
                veg1 = "loc.farming_veg_patch_3",
                veg2 = "loc.farming_veg_patch_4",
                flower = "loc.farming_flower_patch_2",
                herb = "loc.farming_herb_patch_2",
            )
            area(
                area = "Ardougne",
                centre = CoordGrid(2667, 3375, 0),
                first = 8,
                veg1 = "loc.farming_veg_patch_5",
                veg2 = "loc.farming_veg_patch_6",
                flower = "loc.farming_flower_patch_3",
                herb = "loc.farming_herb_patch_3",
            )
            area(
                area = "Canifis",
                centre = CoordGrid(3605, 3528, 0),
                first = 12,
                veg1 = "loc.farming_veg_patch_7",
                veg2 = "loc.farming_veg_patch_8",
                flower = "loc.farming_flower_patch_4",
                herb = "loc.farming_herb_patch_4",
            )
            area(
                area = "Hosidius",
                centre = CoordGrid(1269, 3727, 0),
                first = 16,
                veg1 = "loc.farming_veg_patch_10",
                veg2 = "loc.farming_veg_patch_11",
                flower = "loc.farming_flower_patch_5",
                herb = "loc.farming_herb_patch_6",
            )
        }
}
