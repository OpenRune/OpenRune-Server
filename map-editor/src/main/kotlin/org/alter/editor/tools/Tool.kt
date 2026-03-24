package org.alter.editor.tools

import org.alter.editor.commands.Command
import org.alter.editor.model.EditorState

interface Tool {
    val name: String
    fun onTileClick(x: Int, z: Int, state: EditorState): Command?
    fun onTileDrag(x: Int, z: Int, state: EditorState): Command?
    fun onTileRelease(state: EditorState) {}
}
