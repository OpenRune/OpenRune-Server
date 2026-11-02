package org.rsmod.content.quest.area.corsaircove

import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.content.quest.area.misthalin.SceneCamera
import org.rsmod.map.CoordGrid

internal object CorsairCurseRegions {
    val CoveHut: RegionTemplate =
        RegionTemplate.create {
            copyAllLevels(HutZoneX, HutZoneZ) {
                regionZoneX = BlockOffsetZones
                regionZoneZ = BlockOffsetZones
                zoneWidth = HutZoneWidth
                zoneLength = HutZoneLength
            }
        }

    val CoveShore: RegionTemplate =
        RegionTemplate.create {
            copyAllLevels(ShoreZoneX, ShoreZoneZ) {
                regionZoneX = BlockOffsetZones
                regionZoneZ = BlockOffsetZones
                zoneWidth = ShoreZoneWidth
                zoneLength = ShoreZoneLength
            }
        }

    const val HutZoneX: Int = 314
    const val HutZoneZ: Int = 351
    const val HutZoneWidth: Int = 4
    const val HutZoneLength: Int = 5

    const val ShoreZoneX: Int = 312
    const val ShoreZoneZ: Int = 353
    const val ShoreZoneWidth: Int = 3
    const val ShoreZoneLength: Int = 3

    private const val BlockOffsetZones = 6
}

internal fun assertCorsairCurseSceneCoords() {
    val hut: RegionTemplate = CorsairCurseRegions.CoveHut
    val shore: RegionTemplate = CorsairCurseRegions.CoveShore
    check(hut === CorsairCurseRegions.CoveHut && shore === CorsairCurseRegions.CoveShore) {
        "The Corsair Curse cutscene region templates failed to build."
    }

    assertInside(
        block = "CoveHut",
        originX = CorsairCurseRegions.HutZoneX * ZoneSize,
        originZ = CorsairCurseRegions.HutZoneZ * ZoneSize,
        width = CorsairCurseRegions.HutZoneWidth * ZoneSize,
        length = CorsairCurseRegions.HutZoneLength * ZoneSize,
        coords = ColinScene.coords() + DriftwoodScene.coords() + TelescopeScene.coords(),
    )

    assertInside(
        block = "CoveShore",
        originX = CorsairCurseRegions.ShoreZoneX * ZoneSize,
        originZ = CorsairCurseRegions.ShoreZoneZ * ZoneSize,
        width = CorsairCurseRegions.ShoreZoneWidth * ZoneSize,
        length = CorsairCurseRegions.ShoreZoneLength * ZoneSize,
        coords = GnocciScene.coords(),
    )
}

private fun assertInside(
    block: String,
    originX: Int,
    originZ: Int,
    width: Int,
    length: Int,
    coords: List<Pair<String, CoordGrid>>,
) {
    for ((name, coord) in coords) {
        check(
            coord.x in originX until originX + width && coord.z in originZ until originZ + length
        ) {
            "Corsair Curse scene coord is outside the copied $block block, so `region.normal[...]` " +
                "would throw mid-scene: $name=$coord (allowed: x ${originX}..${originX + width - 1}, " +
                "z ${originZ}..${originZ + length - 1})"
        }
    }
}

private const val ZoneSize = 8

internal object ColinScene {
    val Anchor: CoordGrid = CoordGrid(2528, 2836, 1)
    val Vantage: CoordGrid = CoordGrid(2527, 2823, 1)

    val Opening: SceneFrame =
        SceneFrame(CoordGrid(2527, 2824, 1), 700, CoordGrid(2527, 2839, 1), 350)

    val OpeningDolly: SceneFrame =
        SceneFrame(CoordGrid(2527, 2828, 1), 700, CoordGrid(2527, 2839, 1), 350)

    val Startled: SceneFrame =
        SceneFrame(CoordGrid(2523, 2831, 1), 650, CoordGrid(2540, 2844, 1), 350)

    val StartledDolly: SceneFrame =
        SceneFrame(CoordGrid(2523, 2830, 1), 700, CoordGrid(2540, 2844, 1), 350)

    val StartledReturn: SceneFrame =
        SceneFrame(CoordGrid(2523, 2831, 1), 650, CoordGrid(2540, 2844, 1), 350)

    val CloseUp: SceneFrame =
        SceneFrame(CoordGrid(2524, 2834, 1), 400, CoordGrid(2540, 2839, 1), 350)

    val Closing: SceneFrame =
        SceneFrame(CoordGrid(2516, 2827, 1), 1300, CoordGrid(2540, 2844, 1), 350)

    fun coords(): List<Pair<String, CoordGrid>> =
        listOf("colin.anchor" to Anchor, "colin.vantage" to Vantage) +
            listOf(Opening, OpeningDolly, Startled, StartledDolly, StartledReturn, CloseUp, Closing)
                .flatMapIndexed { i, frame -> frame.coords("colin.frame$i") }
}

internal object GnocciScene {
    val Anchor: CoordGrid = CoordGrid(2505, 2836, 0)
    val Vantage: CoordGrid = CoordGrid(2498, 2832, 0)

    val BurialMark: CoordGrid = CoordGrid(2505, 2839, 0)

    val PanicFacing: CoordGrid = CoordGrid(2505, 2841, 0)

    val Opening: SceneFrame =
        SceneFrame(CoordGrid(2499, 2833, 0), 800, CoordGrid(2505, 2837, 0), 200)

    val OpeningDolly: SceneFrame =
        SceneFrame(CoordGrid(2503, 2830, 0), 280, CoordGrid(2505, 2837, 0), 200)

    val Reveal: SceneFrame =
        SceneFrame(CoordGrid(2504, 2841, 0), 220, CoordGrid(2505, 2835, 0), 170)

    val RevealDolly: SceneFrame =
        SceneFrame(CoordGrid(2505, 2839, 0), 220, CoordGrid(2505, 2835, 0), 170)

    val Burial: SceneFrame =
        SceneFrame(CoordGrid(2510, 2835, 0), 350, CoordGrid(2504, 2840, 0), 200)

    fun coords(): List<Pair<String, CoordGrid>> =
        listOf(
            "gnocci.anchor" to Anchor,
            "gnocci.vantage" to Vantage,
            "gnocci.burial" to BurialMark,
            "gnocci.panicfacing" to PanicFacing,
        ) +
            listOf(Opening, OpeningDolly, Reveal, RevealDolly, Burial).flatMapIndexed { i, frame ->
                frame.coords("gnocci.frame$i")
            }
}

internal object DriftwoodScene {
    val Anchor: CoordGrid = CoordGrid(2532, 2836, 0)

    val Vantage: CoordGrid = CoordGrid(2530, 2841, 1)

    val BeachVantage: CoordGrid = CoordGrid(2529, 2838, 0)

    val BedMark: CoordGrid = CorsairCurseRefs.IthoiHut

    val HutDoor: CoordGrid = CoordGrid(2529, 2833, 0)

    val RunRoute: List<CoordGrid> =
        listOf(
            CoordGrid(2530, 2833, 0),
            CoordGrid(2532, 2833, 0),
            CoordGrid(2532, 2835, 0),
            CoordGrid(2532, 2836, 0),
        )

    val ExitRoute: List<CoordGrid> =
        listOf(CoordGrid(2532, 2835, 0), CoordGrid(2532, 2833, 0), CoordGrid(2529, 2833, 0))

    val Fire: CoordGrid = CoordGrid(2530, 2837, 0)

    val ProjectileEnd: CoordGrid = CoordGrid(2531, 2837, 0)

    val DouseCoords: CoordGrid = ProjectileEnd

    val Opening: SceneFrame =
        SceneFrame(CoordGrid(2529, 2835, 1), 200, CoordGrid(2529, 2841, 1), 170)

    val OpeningDolly: SceneFrame =
        SceneFrame(CoordGrid(2529, 2837, 1), 200, CoordGrid(2529, 2841, 1), 170)

    val Reverse: SceneFrame =
        SceneFrame(CoordGrid(2531, 2841, 0), 300, CoordGrid(2531, 2835, 0), 240)

    val ReverseDolly: SceneFrame =
        SceneFrame(CoordGrid(2531, 2840, 0), 300, CoordGrid(2531, 2835, 0), 240)

    fun coords(): List<Pair<String, CoordGrid>> =
        listOf(
            "driftwood.anchor" to Anchor,
            "driftwood.vantage" to Vantage,
            "driftwood.beachvantage" to BeachVantage,
            "driftwood.bed" to BedMark,
            "driftwood.hutdoor" to HutDoor,
            "driftwood.fire" to Fire,
            "driftwood.projectileend" to ProjectileEnd,
            "driftwood.douse" to DouseCoords,
        ) +
            (RunRoute + ExitRoute).mapIndexed { i, coord -> "driftwood.route$i" to coord } +
            listOf(Opening, OpeningDolly, Reverse, ReverseDolly).flatMapIndexed { i, frame ->
                frame.coords("driftwood.frame$i")
            }
}

// Scene bounds must contain whole NPC footprints, including the five-tile ogre.
internal object TelescopeScene {
    val Vantage: CoordGrid = CoordGrid(2528, 2845, 0)

    val Ogre: CoordGrid = CoordGrid(2526, 2839, 0)

    val Opening: SceneFrame =
        SceneFrame(CoordGrid(2528, 2811, 0), 400, CoordGrid(2528, 2836, 0), 300)

    val PushIn: SceneFrame =
        SceneFrame(CoordGrid(2528, 2816, 0), 400, CoordGrid(2528, 2836, 0), 300)

    fun coords(): List<Pair<String, CoordGrid>> =
        listOf("telescope.vantage" to Vantage, "telescope.ogre" to Ogre) +
            listOf(Opening, PushIn).flatMapIndexed { i, frame -> frame.coords("telescope.frame$i") }
}

internal object RimmingtonFlyby {
    val Vantage: CoordGrid = CoordGrid(2918, 3231, 0)

    val FaceAt: CoordGrid = CoordGrid(2911, 3231, 0)

    val Opening: SceneFrame =
        SceneFrame(CoordGrid(2943, 3224, 0), 800, CoordGrid(2928, 3224, 0), 100)

    val Dolly: SceneFrame = SceneFrame(CoordGrid(2926, 3224, 0), 800, CoordGrid(2911, 3224, 0), 100)

    val Pan: SceneFrame = SceneFrame(CoordGrid(2926, 3224, 0), 800, CoordGrid(2911, 3227, 0), 100)
}

internal data class SceneFrame(
    val eye: CoordGrid,
    val eyeHeight: Int,
    val lookAt: CoordGrid,
    val lookAtHeight: Int,
) {
    fun coords(name: String): List<Pair<String, CoordGrid>> =
        listOf("$name.eye" to eye, "$name.lookAt" to lookAt)

    fun toCamera(): SceneCamera = SceneCamera(eye, eyeHeight, lookAt, lookAtHeight)
}
