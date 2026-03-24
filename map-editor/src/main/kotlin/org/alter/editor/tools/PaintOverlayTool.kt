package org.alter.editor.tools

import org.alter.editor.commands.Command
import org.alter.editor.commands.CompoundCommand
import org.alter.editor.commands.PaintOverlayCommand
import org.alter.editor.model.EditorState

/**
 * Paint overlay tool: paints the selected overlay onto tiles.
 * During a drag, accumulates individual commands and batches them on release.
 */
class PaintOverlayTool : Tool {
    override val name = "Paint Overlay"

    private val dragCommands = mutableListOf<Command>()
    private var dragging = false

    override fun onTileClick(x: Int, z: Int, state: EditorState): Command? {
        val region = state.regionData ?: return null
        val hl = state.currentHeightLevel.get()
        val tile = region.getTile(hl, x, z)
        val oldOverlay = tile.overlayId
        val newOverlay = state.selectedOverlay.get().toShort()
        if (oldOverlay == newOverlay) return null

        state.selectedTileX = x
        state.selectedTileZ = z
        return PaintOverlayCommand(region, hl, listOf(Triple(x, z, oldOverlay)), newOverlay)
    }

    override fun onTileDrag(x: Int, z: Int, state: EditorState): Command? {
        dragging = true
        val cmd = onTileClick(x, z, state) ?: return null
        dragCommands.add(cmd)
        return cmd
    }

    override fun onTileRelease(state: EditorState) {
        // Drag commands were already executed individually; clear for next drag.
        dragCommands.clear()
        dragging = false
    }
}
