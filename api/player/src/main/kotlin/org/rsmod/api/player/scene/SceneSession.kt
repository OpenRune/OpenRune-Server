package org.rsmod.api.player.scene

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

/**
 * Owns the active [SceneWindow] for one cutscene region. The first window installs a reloading
 * build area; later windows only move the window centre, matching OSRS's reload flag.
 */
public class SceneSession(
    private val access: ProtectedAccess,
    public val region: Region,
    public val montage: SceneMontage,
) {
    private val coordsByWindow = montage.windows.values.associateWith { it.coords(region) }
    private var regionLoaded: Boolean = false

    public var window: SceneWindow = montage.windows.values.first()
        private set

    public fun coords(window: SceneWindow): SceneWindowCoords = coordsByWindow.getValue(window)

    public operator fun get(source: CoordGrid): CoordGrid = window.toWorld(region, source)

    public fun useWindow(
        window: SceneWindow,
        centerX: Int = window.buildCenterX,
        centerZ: Int = window.buildCenterZ,
    ) {
        val area = window.buildArea(region, centerX, centerZ, reload = !regionLoaded)
        access.player.sceneBuildArea = area
        access.player.buildArea = area.base
        access.player.regionRebuildPending = true
        this.window = window
        regionLoaded = true
    }

    public fun close() {
        access.player.sceneBuildArea = null
    }
}
