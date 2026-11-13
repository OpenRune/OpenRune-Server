package org.rsmod.api.player.scene

import org.rsmod.api.repo.region.RegionStaticTemplate
import org.rsmod.game.region.Region
import org.rsmod.game.region.util.RegionRotations
import org.rsmod.game.region.zone.RegionZoneCopy
import org.rsmod.game.region.zone.RegionZoneFlag
import org.rsmod.game.scene.SceneBuildArea
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

/**
 * A rectangular source-zone block placed into the scene region. Blocks reuse the same rotation and
 * placement semantics as [RegionStaticTemplate.copy], including its [RegionZoneFlag] support.
 */
public data class SceneBlock(
    public val sourceX: Int,
    public val sourceZ: Int,
    public val zoneWidth: Int,
    public val zoneLength: Int,
    public val destX: Int,
    public val destZ: Int,
    public val rotation: Int,
)

public data class SceneCell(public val x: Int, public val z: Int)

/**
 * A camera/build-area vantage over a shared [SceneWindow] table. OSRS can reuse the same copied map
 * window with a different build-area centre; the west run and pushups do exactly that.
 */
public class SceneWindowView(
    public val name: String,
    public val buildCenterX: Int,
    public val buildCenterZ: Int,
    public val transformOffsetX: Int,
    public val transformOffsetZ: Int,
    public val sourceBase: CoordGrid,
    public val rotation: Int,
)

/**
 * One OSRS rebuild-region table, expressed as rectangular copied blocks. A window can expose
 * multiple [views]; each view has its own build-area centre and content transform while sharing the
 * same copied table.
 */
public class SceneWindow(
    public val name: String,
    public val flag: RegionZoneFlag,
    public val blocks: List<SceneBlock>,
    public val views: Map<String, SceneWindowView>,
    public val defaultView: String,
) {
    init {
        require(blocks.isNotEmpty()) { "Scene window requires at least one block. (name=$name)" }
        require(views.isNotEmpty()) { "Scene window requires at least one view. (name=$name)" }
        require(defaultView in views) {
            "Scene window default view is not defined. (name=$name, view=$defaultView)"
        }
        for (block in blocks) {
            require(block.zoneWidth > 0 && block.zoneLength > 0) {
                "Scene block dimensions must be positive. (name=$name, block=$block)"
            }
            require(block.rotation in 0..3) {
                "Scene block rotation must be within range [0..3]. (name=$name, block=$block)"
            }
            for (x in 0 until block.zoneWidth) {
                for (z in 0 until block.zoneLength) {
                    val (rx, rz) =
                        RegionRotations.translateZone(
                            regionRot = block.rotation,
                            zoneX = x,
                            zoneZ = z,
                            zoneWidth = block.zoneWidth,
                            zoneLength = block.zoneLength,
                        )
                    val destX = block.destX + rx
                    val destZ = block.destZ + rz
                    require(destX in 0..MAX_REGION_ZONE && destZ in 0..MAX_REGION_ZONE) {
                        "Scene block destination is outside the packed region. " +
                            "(name=$name, x=$destX, z=$destZ)"
                    }
                }
            }
        }
        for (view in views.values) {
            require(
                view.buildCenterX in 6..(MAX_REGION_ZONE - 6) &&
                    view.buildCenterZ in 6..(MAX_REGION_ZONE - 6)
            ) {
                "Scene view build centre is outside the packed region. (name=$name, view=${view.name})"
            }
            require(view.rotation in 0..3) {
                "Scene view rotation must be within range [0..3]. (name=$name, view=${view.name})"
            }
        }
    }

    public fun view(name: String = defaultView): SceneWindowView =
        views[name] ?: error("Unknown scene window view. (window=$this, view=$name)")

    public fun coords(region: Region, viewName: String = defaultView): SceneWindowCoords =
        SceneWindowCoords(region, this, view(viewName))

    public fun zones(): Map<SceneCell, RegionZoneCopy> {
        val result = LinkedHashMap<SceneCell, RegionZoneCopy>()
        for (block in blocks) {
            for (x in 0 until block.zoneWidth) {
                for (z in 0 until block.zoneLength) {
                    val (rx, rz) =
                        RegionRotations.translateZone(
                            regionRot = block.rotation,
                            zoneX = x,
                            zoneZ = z,
                            zoneWidth = block.zoneWidth,
                            zoneLength = block.zoneLength,
                        )
                    val dest = SceneCell(block.destX + rx, block.destZ + rz)
                    val source = ZoneKey(block.sourceX + x, block.sourceZ + z, 0)
                    result[dest] = RegionZoneCopy(source, block.rotation, flag)
                }
            }
        }
        return result
    }

    public fun buildArea(
        region: Region,
        viewName: String = defaultView,
        reload: Boolean = false,
    ): SceneBuildArea {
        val view = view(viewName)
        val regionBase = ZoneKey.from(region.southWest)
        val base =
            region.southWest.translate(
                (view.buildCenterX - 6) * 8,
                (view.buildCenterZ - 6) * 8,
            )
        val center = regionBase.translate(view.buildCenterX, view.buildCenterZ)
        val zones =
            zones().mapKeys { (cell, _) ->
                ZoneKey(regionBase.x + cell.x, regionBase.z + cell.z, 0)
            }
        return SceneBuildArea(base, center, reload, zones)
    }

    internal fun toWorld(region: Region, view: SceneWindowView, source: CoordGrid): CoordGrid {
        val dx = source.x - view.sourceBase.x
        val dz = source.z - view.sourceBase.z
        val translation =
            RegionRotations.translateZone(
                regionRot = (4 - view.rotation) and 3,
                zoneX = dx,
                zoneZ = dz,
                zoneWidth = BUILD_AREA_SIZE,
                zoneLength = BUILD_AREA_SIZE,
            )
        return region.southWest.translate(
            view.transformOffsetX * 8 + translation.x,
            view.transformOffsetZ * 8 + translation.z,
        )
    }

    internal fun copyInto(template: RegionStaticTemplate) {
        for (block in blocks) {
            template.copy(block.sourceX, block.sourceZ, 0) {
                regionZoneX = block.destX
                regionZoneZ = block.destZ
                zoneWidth = block.zoneWidth
                zoneLength = block.zoneLength
                rotation = block.rotation
                flag = this@SceneWindow.flag
            }
        }
    }

    private companion object {
        const val BUILD_AREA_SIZE = 104
        const val MAX_REGION_ZONE = 39
    }
}

public class SceneWindowCoords internal constructor(
    private val region: Region,
    private val window: SceneWindow,
    private val view: SceneWindowView,
) {
    public operator fun get(source: CoordGrid): CoordGrid = window.toWorld(region, view, source)
}
