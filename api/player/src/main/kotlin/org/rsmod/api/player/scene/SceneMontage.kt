package org.rsmod.api.player.scene

import org.rsmod.api.repo.region.RegionTemplate

/**
 * A packed set of [SceneWindow]s backed by one large, terrain-only region. The client is only ever
 * shown the active window; the surrounding windows exist for server-side zone registration and for
 * switching without leaving the region.
 */
public class SceneMontage private constructor(
    public val windows: Map<String, SceneWindow>,
    public val template: RegionTemplate,
) {
    public operator fun get(name: String): SceneWindow =
        windows[name] ?: error("Unknown scene window: $name")

    public companion object {
        public fun create(vararg windows: SceneWindow): SceneMontage {
            require(windows.isNotEmpty()) { "A scene montage requires at least one window." }
            val template =
                RegionTemplate.createLarge {
                    terrainOnly = true
                    for (window in windows) {
                        window.copyInto(this)
                    }
                }
            return SceneMontage(windows.associateBy { it.name }, template)
        }
    }
}
