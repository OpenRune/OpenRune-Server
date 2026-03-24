package org.alter.editor.tools

import org.alter.editor.commands.CollisionCommand
import org.alter.editor.commands.Command
import org.alter.editor.model.EditorState

/**
 * Collision toggle tool: click toggles the blocked flag on a tile.
 */
class CollisionToggleTool : Tool {
    override val name = "Collision Toggle"

    override fun onTileClick(x: Int, z: Int, state: EditorState): Command? {
        val region = state.regionData ?: return null
        val hl = state.currentHeightLevel.get()
        val tile = region.getTile(hl, x, z)
        val wasBlocked = tile.isBlocked

        state.selectedTileX = x
        state.selectedTileZ = z
        return CollisionCommand(region, hl, listOf(Triple(x, z, wasBlocked)))
    }

    override fun onTileDrag(x: Int, z: Int, state: EditorState): Command? {
        return onTileClick(x, z, state)
    }
}
