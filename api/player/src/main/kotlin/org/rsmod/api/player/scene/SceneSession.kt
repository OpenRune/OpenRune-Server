package org.rsmod.api.player.scene

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

/**
 * Owns the active [SceneWindow] and [SceneWindowView] for one cutscene region. The first window
 * installs a reloading build area; later views only move the window centre, matching OSRS's reload
 * flag.
 */
public class SceneSession(
    private val access: ProtectedAccess,
    public val region: Region,
    public val montage: SceneMontage,
) {
    private var regionLoaded: Boolean = false

    public var window: SceneWindow = montage.windows.values.first()
        private set
    public var view: SceneWindowView = window.view()
        private set

    public fun coords(
        window: SceneWindow,
        viewName: String = window.defaultView,
    ): SceneWindowCoords = window.coords(region, viewName)

    public operator fun get(source: CoordGrid): CoordGrid = window.toWorld(region, view, source)

    public fun useWindow(
        window: SceneWindow,
        viewName: String = window.defaultView,
    ) {
        val area = window.buildArea(region, viewName, reload = !regionLoaded)
        access.player.sceneBuildArea = area
        access.player.buildArea = area.base
        access.player.regionRebuildPending = true
        this.window = window
        this.view = window.view(viewName)
        regionLoaded = true
    }

    public fun close() {
        access.player.sceneBuildArea = null
    }
}
