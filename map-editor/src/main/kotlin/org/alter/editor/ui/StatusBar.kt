package org.alter.editor.ui

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import org.alter.editor.model.EditorState

/**
 * Bottom status bar showing region ID, hovered tile coordinates, and current tool name.
 */
class StatusBar(private val state: EditorState) : HBox(16.0) {

    private val regionLabel = Label()
    private val hoverLabel = Label()
    private val toolLabel = Label()

    init {
        padding = Insets(4.0, 10.0, 4.0, 10.0)
        alignment = Pos.CENTER_LEFT
        style = "-fx-background-color: #1e1e1e;"

        val labelStyle = "-fx-text-fill: #cccccc; -fx-font-size: 12;"
        regionLabel.style = labelStyle
        hoverLabel.style = labelStyle
        toolLabel.style = labelStyle

        val spacer = Region()
        HBox.setHgrow(spacer, Priority.ALWAYS)

        children.addAll(regionLabel, hoverLabel, spacer, toolLabel)

        // Bind tool label
        state.currentTool.addListener { _, _, newVal ->
            toolLabel.text = "Tool: $newVal"
        }
        toolLabel.text = "Tool: ${state.currentTool.get()}"

        refresh()
    }

    /** Call periodically or on state change to update labels. */
    fun refresh() {
        val region = state.regionData
        regionLabel.text = if (region != null) "Region: ${region.regionId}" else "No region"
        hoverLabel.text = if (state.hoveredTileX >= 0) "Tile: (${state.hoveredTileX}, ${state.hoveredTileZ})" else "Tile: ---"
    }
}
