package org.rsmod.content.quest.area.misthalin

import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

internal val MisthalinIslandTemplate: RegionTemplate =
    RegionTemplate.create {
        copy(200, 600, 0) {
            regionZoneX = 4
            regionZoneZ = 4
            zoneWidth = 8
            zoneLength = 8
        }
    }

internal class SceneRegion(private val region: Region?) {
    val instanced: Boolean
        get() = region != null

    operator fun get(coords: CoordGrid): CoordGrid =
        if (region == null) coords else region.normal[coords]

    fun map(camera: SceneCamera): SceneCamera =
        if (region == null) {
            camera
        } else {
            SceneCamera(
                eye = this[camera.eye],
                eyeHeight = camera.eyeHeight,
                lookAt = this[camera.lookAt],
                lookAtHeight = camera.lookAtHeight,
            )
        }
}

internal fun assertMisthalinSceneCoords(coords: Iterable<Pair<String, CoordGrid>>) {
    for ((name, coord) in coords) {
        check(coord.level == 0 && coord.x in 1600..1663 && coord.z in 4800..4863) {
            "Misthalin scene coord is outside the cutscene region block, so `region.normal[...]` " +
                "would throw mid-scene: $name=$coord (allowed: level 0, x 1600..1663, z 4800..4863)"
        }
    }
}
