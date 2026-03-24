package org.alter.editor.tools

import org.alter.editor.commands.Command
import org.alter.editor.commands.HeightCommand
import org.alter.editor.model.EditorState
import org.alter.editor.model.RegionData

/**
 * Height brush tool: click raises tiles by 10, Shift+click lowers by 10.
 * Uses the brush size from EditorState to affect surrounding tiles.
 */
class HeightBrushTool : Tool {
    override val name = "Height Brush"

    var shiftDown: Boolean = false

    override fun onTileClick(x: Int, z: Int, state: EditorState): Command? {
        val region = state.regionData ?: return null
        val hl = state.currentHeightLevel.get()
        val radius = state.brushSize.get() / 2
        val delta = if (shiftDown) -10 else 10

        val affected = mutableListOf<Triple<Int, Int, Int>>()
        for (dz in -radius..radius) {
            for (dx in -radius..radius) {
                val tx = x + dx
                val tz = z + dz
                if (tx < 0 || tx >= RegionData.SIZE || tz < 0 || tz >= RegionData.SIZE) continue
                val tile = region.getTile(hl, tx, tz)
                affected.add(Triple(tx, tz, tile.height))
            }
        }

        state.selectedTileX = x
        state.selectedTileZ = z
        return HeightCommand(region, hl, affected, delta)
    }

    override fun onTileDrag(x: Int, z: Int, state: EditorState): Command? {
        return onTileClick(x, z, state)
    }
}
