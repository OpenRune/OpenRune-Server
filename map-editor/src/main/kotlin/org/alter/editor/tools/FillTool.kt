package org.alter.editor.tools

import org.alter.editor.commands.Command
import org.alter.editor.commands.PaintOverlayCommand
import org.alter.editor.model.EditorState
import org.alter.editor.model.RegionData
import java.util.ArrayDeque

/**
 * Fill tool: flood fill from the clicked tile using BFS.
 * Fills all connected tiles that share the same overlay ID with the selected overlay.
 */
class FillTool : Tool {
    override val name = "Fill"

    override fun onTileClick(x: Int, z: Int, state: EditorState): Command? {
        val region = state.regionData ?: return null
        val hl = state.currentHeightLevel.get()
        val targetOverlay = region.getTile(hl, x, z).overlayId
        val newOverlay = state.selectedOverlay.get().toShort()
        if (targetOverlay == newOverlay) return null

        val visited = mutableSetOf<Pair<Int, Int>>()
        val affected = mutableListOf<Triple<Int, Int, Short>>()
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(Pair(x, z))
        visited.add(Pair(x, z))

        while (queue.isNotEmpty()) {
            val (cx, cz) = queue.poll()
            val tile = region.getTile(hl, cx, cz)
            if (tile.overlayId != targetOverlay) continue

            affected.add(Triple(cx, cz, tile.overlayId))

            for ((dx, dz) in listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)) {
                val nx = cx + dx
                val nz = cz + dz
                if (nx < 0 || nx >= RegionData.SIZE || nz < 0 || nz >= RegionData.SIZE) continue
                val neighbor = Pair(nx, nz)
                if (neighbor !in visited) {
                    visited.add(neighbor)
                    queue.add(neighbor)
                }
            }
        }

        state.selectedTileX = x
        state.selectedTileZ = z
        return PaintOverlayCommand(region, hl, affected, newOverlay)
    }

    override fun onTileDrag(x: Int, z: Int, state: EditorState): Command? {
        return null  // Fill does not support drag
    }
}
