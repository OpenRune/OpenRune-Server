package org.rsmod.game.scene

import org.rsmod.game.region.zone.RegionZoneCopy
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

/**
 * A client build-area window owned by a cutscene. [center] is the absolute build-area centre, the
 * same value sent in a rebuild packet. [zones] contains only the cells of this window; it must not
 * fall back to the surrounding scene region, or neighbouring windows can leak into the view.
 */
public data class SceneBuildArea(
    public val base: CoordGrid,
    public val center: ZoneKey,
    public val reload: Boolean,
    public val zones: Map<ZoneKey, RegionZoneCopy>,
)
