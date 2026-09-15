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
                protection: String? = null,
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
                )

            fun flower(
                name: String,
                seed: String,
                produce: String,
                level: Int,
                plantXp: Double,
                harvestXp: Double,
                growBase: Int,
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
                )

            allotment("potato", "obj.potato_seed", "obj.potato", 1, 8.0, 9.0, 6, 4, "dbrow.farming_marigold")
            allotment("onion", "obj.onion_seed", "obj.onion", 5, 9.5, 10.5, 13, 4, "dbrow.farming_marigold")
            allotment("cabbage", "obj.cabbage_seed", "obj.cabbage", 7, 10.0, 11.5, 20, 4, "dbrow.farming_rosemary")
            allotment("tomato", "obj.tomato_seed", "obj.tomato", 12, 12.5, 14.0, 27, 4, "dbrow.farming_marigold")
            allotment("sweetcorn", "obj.sweetcorn_seed", "obj.sweetcorn", 20, 17.0, 19.0, 34, 6)
            allotment("strawberry", "obj.strawberry_seed", "obj.strawberry", 31, 26.0, 29.0, 43, 6)
            allotment(
                name = "watermelon",
                seed = "obj.watermelon_seed",
                produce = "obj.watermelon",
                level = 47,
                plantXp = 48.5,
                harvestXp = 54.5,
                growBase = 52,
                stages = 8,
                protection = "dbrow.farming_nasturtium",
            )

            flower("marigold", "obj.marigold_seed", "obj.marigold", 2, 8.5, 47.0, 8)
            flower("rosemary", "obj.rosemary_seed", "obj.rosemary", 11, 12.0, 66.5, 13)
            flower("nasturtium", "obj.nasturtium_seed", "obj.nasturtium", 24, 19.5, 111.0, 18)
            flower("woad", "obj.woad_seed", "obj.woadleaf", 25, 20.5, 115.5, 23)
            flower("limpwurt", "obj.limpwurt_seed", "obj.limpwurt_root", 26, 21.5, 120.0, 28)

            herb("guam", "obj.guam_seed", "obj.unidentified_guam", 9, 11.0, 12.5, 4, 128)
            herb("marrentill", "obj.marrentill_seed", "obj.unidentified_marentill", 14, 13.5, 15.0, 11, 131)
            herb("tarromin", "obj.tarromin_seed", "obj.unidentified_tarromin", 19, 16.0, 18.0, 18, 134)
            herb("harralander", "obj.harralander_seed", "obj.unidentified_harralander", 26, 21.5, 24.0, 25, 137)
            herb("ranarr", "obj.ranarr_seed", "obj.unidentified_ranarr", 32, 27.0, 30.5, 32, 140)
            herb("toadflax", "obj.toadflax_seed", "obj.unidentified_toadflax", 38, 34.0, 38.5, 39, 143)
            herb("irit", "obj.irit_seed", "obj.unidentified_irit", 44, 43.0, 48.5, 46, 146)
            herb("avantoe", "obj.avantoe_seed", "obj.unidentified_avantoe", 50, 54.5, 61.5, 53, 149)
            herb("kwuarm", "obj.kwuarm_seed", "obj.unidentified_kwuarm", 56, 69.0, 78.0, 68, 152)
            herb("snapdragon", "obj.snapdragon_seed", "obj.unidentified_snapdragon", 62, 87.5, 98.5, 75, 155)
            herb("cadantine", "obj.cadantine_seed", "obj.unidentified_cadantine", 67, 106.5, 120.0, 82, 158)
            herb("lantadyme", "obj.lantadyme_seed", "obj.unidentified_lantadyme", 73, 134.5, 151.5, 89, 161)
            herb("dwarf weed", "obj.dwarf_weed_seed", "obj.unidentified_dwarf_weed", 79, 170.5, 192.0, 96, 164)
            herb("torstol", "obj.torstol_seed", "obj.unidentified_torstol", 85, 199.5, 224.5, 103, 167)
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
