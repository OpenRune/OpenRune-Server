package org.rsmod.content.quest.area.lumbridge

import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.map.CoordGrid

internal object TheRestlessGhostRegions {
    val Graveyard: RegionTemplate =
        RegionTemplate.create {
            copy(GraveyardZoneX, GraveyardZoneZ, 0) {
                regionZoneX = BlockOffsetZones
                regionZoneZ = BlockOffsetZones
                zoneWidth = BlockZones
                zoneLength = BlockZones
            }
        }

    const val GraveyardZoneX: Int = 405
    const val GraveyardZoneZ: Int = 397

    private const val BlockZones = 3
    private const val BlockOffsetZones = 6
}

internal fun assertRestlessGhostSceneCoords() {
    val originX = TheRestlessGhostRegions.GraveyardZoneX * ZoneSize
    val originZ = TheRestlessGhostRegions.GraveyardZoneZ * ZoneSize

    for ((name, coord) in ReleaseSceneCoords) {
        check(
            coord.level == 0 &&
                coord.x in originX until originX + BlockSize &&
                coord.z in originZ until originZ + BlockSize
        ) {
            "Restless Ghost release-scene coord is outside the copied block, so " +
                "`region.normal[...]` would throw mid-scene: $name=$coord " +
                "(allowed: level 0, x ${originX}..${originX + BlockSize - 1}, " +
                "z ${originZ}..${originZ + BlockSize - 1})"
        }
    }
}

private const val ZoneSize = 8
private const val BlockSize = 3 * ZoneSize

private val ReleaseSceneCoords: List<Pair<String, CoordGrid>>
    get() =
        listOf(
            "vantage" to ReleaseScene.Vantage,
            "ghost" to ReleaseScene.GhostTile,
            "gate" to ReleaseScene.GateTile,
            "gateOpen" to ReleaseScene.GateOpenTile,
            "returnTo" to ReleaseScene.ReturnTile,
            "soulLeg1" to ReleaseScene.SoulWest,
            "soulLeg2" to ReleaseScene.SoulSouth,
            "soulLeg3" to ReleaseScene.SoulRiver,
            "openingEye" to ReleaseScene.OpeningCamera.eye,
            "openingLookAt" to ReleaseScene.OpeningCamera.lookAt,
            "followEye" to ReleaseScene.FollowCamera.eye,
            "followLookAt" to ReleaseScene.FollowCamera.lookAt,
            "riverEye" to ReleaseScene.RiverCamera.eye,
            "riverLookAt" to ReleaseScene.RiverCamera.lookAt,
        )
