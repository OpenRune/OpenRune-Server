package org.rsmod.content.skills.farming.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import dev.openrune.tables.production.productionTable
import org.rsmod.map.CoordGrid

/**
 * Crops and the patches they grow in.
 *
 * XP is stored multiplied by ten: two thirds of these values are fractional - a potato plants for
 * 8.5 - and the production xp column is an int. Content divides by ten once, in FarmingCrops.
 *
 * `grow_base` is an index into the patch loc's own transform table: the client renders
 * `transforms[grow_base + stage]`. Allotments and flowers offset that by +64 watered, +128 diseased
 * and +192 dead; herb patches pack their diseased art separately, which is what `diseased_base` is
 * for, and why it is set on herbs alone.
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

    /**
     * Row order is load-bearing: a planted patch persists its crop as a one-based index into this
     * table, packed into seven bits of the patch varp. Append new crops, never insert.
     */
    /**
     * Snape grass is the one crop the base-plus-stage arithmetic cannot describe. It was added to
     * the allotment patch long after the other seven, by which point the loc's four 64-slot banks
     * were full, so its art sits in whatever slots were spare: the leftovers of potato's diseased
     * and dead blocks. Healthy runs 128-134 but its grown form is at 138; watered runs 63-69;
     * diseased and dead each break in half between stage three and four. The values repeat at the
     * ends the way the cache's own table does, so every state a patch can reach resolves.
     *
     * Ordered healthy, watered, diseased, dead, each bank `stages + 1` long and indexed by stage.
     */
    private val SNAPE_GRASS_TRANSMIT =
        listOf(
            128, 129, 130, 131, 132, 133, 134, 138,
            63, 64, 65, 66, 67, 68, 69, 138,
            196, 196, 197, 198, 202, 203, 204, 204,
            193, 193, 194, 195, 209, 210, 211, 211,
        )

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
            },
        ) {
            fun crop(
                name: String,
                kind: String,
                seed: String,
                produce: String,
                level: Int,
                plantXp: Double,
                harvestXp: Double,
                growBase: Int,
                stages: Int,
                stageMinutes: Int,
                seeds: Int = 1,
                diseasedBase: Int = -1,
                protection: String? = null,
                transmit: List<Int> = emptyList(),
                saveLow: Int = 0,
                saveHigh: Int = 0,
                harvestAmount: Int = 1,
                harvestLevelDivisor: Int = 0,
            ) =
                row("dbrow.farming_${name.replace(' ', '_')}") {
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
                }

            fun allotment(
                name: String,
                seed: String,
                produce: String,
                level: Int,
                plantXp: Double,
                harvestXp: Double,
                growBase: Int,
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
                    growBase = growBase,
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
                growBase: Int,
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
                    growBase = growBase,
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
                growBase: Int,
                diseasedBase: Int,
                saveLow: Int,
            ) =
                crop(
                    name = name,
                    kind = HERB,
                    seed = seed,
                    produce = produce,
                    level = level,
                    plantXp = plantXp,
                    harvestXp = harvestXp,
                    growBase = growBase,
                    stages = 4,
                    stageMinutes = HERB_MINUTES,
                    diseasedBase = diseasedBase,
                    saveLow = saveLow,
                    saveHigh = 80,
                )

            allotment("potato", "obj.potato_seed", "obj.potato", 1, 8.0, 9.0, 6, 4, 101, protection = "dbrow.farming_marigold")
            allotment("onion", "obj.onion_seed", "obj.onion", 5, 9.5, 10.5, 13, 4, 105, protection = "dbrow.farming_marigold")
            allotment("cabbage", "obj.cabbage_seed", "obj.cabbage", 7, 10.0, 11.5, 20, 4, 107, protection = "dbrow.farming_rosemary")
            allotment("tomato", "obj.tomato_seed", "obj.tomato", 12, 12.5, 14.0, 27, 4, 112, protection = "dbrow.farming_marigold")
            allotment("sweetcorn", "obj.sweetcorn_seed", "obj.sweetcorn", 20, 17.0, 19.0, 34, 6, 88)
            allotment("strawberry", "obj.strawberry_seed", "obj.strawberry", 31, 26.0, 29.0, 43, 6, 103)
            allotment(
                name = "watermelon",
                seed = "obj.watermelon_seed",
                produce = "obj.watermelon",
                level = 47,
                plantXp = 48.5,
                harvestXp = 54.5,
                growBase = 52,
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
                growBase = 128,
                stages = 7,
                saveLow = 148,
                saveHigh = 195,
                transmit = SNAPE_GRASS_TRANSMIT,
            )

            flower("marigold", "obj.marigold_seed", "obj.marigold", 2, 8.5, 47.0, 8)
            flower("rosemary", "obj.rosemary_seed", "obj.rosemary", 11, 12.0, 66.5, 13)
            flower("nasturtium", "obj.nasturtium_seed", "obj.nasturtium", 24, 19.5, 111.0, 18)
            flower("woad", "obj.woad_seed", "obj.woadleaf", 25, 20.5, 115.5, 23, harvestAmount = 3)
            flower("limpwurt", "obj.limpwurt_seed", "obj.limpwurt_root", 26, 21.5, 120.0, 28, harvestAmount = 3, harvestLevelDivisor = 10)

            herb("guam", "obj.guam_seed", "obj.unidentified_guam", 9, 11.0, 12.5, 4, 128, 25)
            herb("marrentill", "obj.marrentill_seed", "obj.unidentified_marentill", 14, 13.5, 15.0, 11, 131, 28)
            herb("tarromin", "obj.tarromin_seed", "obj.unidentified_tarromin", 19, 16.0, 18.0, 18, 134, 31)
            herb("harralander", "obj.harralander_seed", "obj.unidentified_harralander", 26, 21.5, 24.0, 25, 137, 36)
            herb("ranarr", "obj.ranarr_seed", "obj.unidentified_ranarr", 32, 27.0, 30.5, 32, 140, 39)
            herb("toadflax", "obj.toadflax_seed", "obj.unidentified_toadflax", 38, 34.0, 38.5, 39, 143, 43)
            herb("irit", "obj.irit_seed", "obj.unidentified_irit", 44, 43.0, 48.5, 46, 146, 46)
            herb("avantoe", "obj.avantoe_seed", "obj.unidentified_avantoe", 50, 54.5, 61.5, 53, 149, 50)
            herb("kwuarm", "obj.kwuarm_seed", "obj.unidentified_kwuarm", 56, 69.0, 78.0, 68, 152, 54)
            herb("snapdragon", "obj.snapdragon_seed", "obj.unidentified_snapdragon", 62, 87.5, 98.5, 75, 155, 57)
            herb("cadantine", "obj.cadantine_seed", "obj.unidentified_cadantine", 67, 106.5, 120.0, 82, 158, 60)
            herb("lantadyme", "obj.lantadyme_seed", "obj.unidentified_lantadyme", 73, 134.5, 151.5, 89, 161, 64)
            herb("dwarf weed", "obj.dwarf_weed_seed", "obj.unidentified_dwarf_weed", 79, 170.5, 192.0, 96, 164, 67)
            herb("torstol", "obj.torstol_seed", "obj.unidentified_torstol", 85, 199.5, 224.5, 103, 167, 71)
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
