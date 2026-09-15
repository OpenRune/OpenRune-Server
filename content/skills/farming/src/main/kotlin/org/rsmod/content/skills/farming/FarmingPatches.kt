package org.rsmod.content.skills.farming

import org.rsmod.map.CoordGrid

data class PatchDef(val loc: String, val kind: PatchKind, val varp: String)

/**
 * A farm. All five share the same handful of `farming_transmit_*` varbits, so only one area's
 * patches can be described to the client at a time; [centre] is how the nearest one is picked.
 */
data class FarmingArea(val name: String, val centre: CoordGrid, val patches: List<PatchDef>)

private fun patch(loc: String, kind: PatchKind, index: Int) =
    PatchDef(loc, kind, "varp.farming_patch_%02d".format(index))

private fun area(
    name: String,
    x: Int,
    z: Int,
    first: Int,
    veg1: String,
    veg2: String,
    flower: String,
    herb: String,
) =
    FarmingArea(
        name = name,
        centre = CoordGrid(x, z, 0),
        patches =
            listOf(
                patch(veg1, PatchKind.ALLOTMENT, first),
                patch(veg2, PatchKind.ALLOTMENT, first + 1),
                patch(flower, PatchKind.FLOWER, first + 2),
                patch(herb, PatchKind.HERB, first + 3),
            ),
    )

object FarmingPatches {
    // ponytail: area centres are approximate with a generous radius. They only decide which farm's
    // state gets described to the client on approach; interacting with a patch transmits its own
    // area regardless, so a centre being a few tiles out costs nothing.
    const val AREA_RADIUS: Int = 40

    val areas: List<FarmingArea> =
        listOf(
            area(
                name = "Falador",
                x = 3056,
                z = 3309,
                first = 0,
                veg1 = "loc.farming_veg_patch_1",
                veg2 = "loc.farming_veg_patch_2",
                flower = "loc.farming_flower_patch_1",
                herb = "loc.farming_herb_patch_1",
            ),
            area(
                name = "Catherby",
                x = 2811,
                z = 3464,
                first = 4,
                veg1 = "loc.farming_veg_patch_3",
                veg2 = "loc.farming_veg_patch_4",
                flower = "loc.farming_flower_patch_2",
                herb = "loc.farming_herb_patch_2",
            ),
            area(
                name = "Ardougne",
                x = 2667,
                z = 3375,
                first = 8,
                veg1 = "loc.farming_veg_patch_5",
                veg2 = "loc.farming_veg_patch_6",
                flower = "loc.farming_flower_patch_3",
                herb = "loc.farming_herb_patch_3",
            ),
            area(
                name = "Canifis",
                x = 3605,
                z = 3528,
                first = 12,
                veg1 = "loc.farming_veg_patch_7",
                veg2 = "loc.farming_veg_patch_8",
                flower = "loc.farming_flower_patch_4",
                herb = "loc.farming_herb_patch_4",
            ),
            area(
                name = "Hosidius",
                x = 1269,
                z = 3727,
                first = 16,
                veg1 = "loc.farming_veg_patch_10",
                veg2 = "loc.farming_veg_patch_11",
                flower = "loc.farming_flower_patch_5",
                herb = "loc.farming_herb_patch_6",
            ),
        )

    val all: List<PatchDef> = areas.flatMap(FarmingArea::patches)

    fun areaOf(patch: PatchDef): FarmingArea = areas.first { patch in it.patches }

    fun nearest(coords: CoordGrid): FarmingArea? =
        areas.firstOrNull {
            it.centre.level == coords.level &&
                kotlin.math.abs(it.centre.x - coords.x) <= AREA_RADIUS &&
                kotlin.math.abs(it.centre.z - coords.z) <= AREA_RADIUS
        }
}
