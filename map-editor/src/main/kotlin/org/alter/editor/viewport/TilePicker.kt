package org.alter.editor.viewport

import javafx.scene.input.PickResult
import org.alter.editor.model.RegionData

/**
 * Determines which tile the mouse is over using JavaFX 3D pick results.
 *
 * The terrain mesh maps tile (x, z) directly to world coordinates,
 * so the intersection point's X and Z can be floored to obtain tile indices.
 */
class TilePicker {

    /**
     * Given a [PickResult] from a mouse event on the terrain mesh,
     * extract the tile (x, z) coordinates.
     *
     * @return a pair of (x, z) tile indices, or `null` if the pick did not hit the terrain.
     */
    fun pickTile(pickResult: PickResult): Pair<Int, Int>? {
        val point = pickResult.intersectedPoint ?: return null
        val x = point.x.toInt().coerceIn(0, RegionData.SIZE - 1)
        val z = point.z.toInt().coerceIn(0, RegionData.SIZE - 1)
        return x to z
    }
}
