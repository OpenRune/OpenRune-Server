package org.alter.editor.tools

import org.alter.editor.commands.Command
import org.alter.editor.model.EditorState

/**
 * Select tool: updates the selected tile in state, does not produce an undoable command.
 */
class SelectTool : Tool {
    override val name = "Select"

    override fun onTileClick(x: Int, z: Int, state: EditorState): Command? {
        state.selectedTileX = x
        state.selectedTileZ = z
        return null
    }

    override fun onTileDrag(x: Int, z: Int, state: EditorState): Command? {
        state.selectedTileX = x
        state.selectedTileZ = z
        return null
    }
}
