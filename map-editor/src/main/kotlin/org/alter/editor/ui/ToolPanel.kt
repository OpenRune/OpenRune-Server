package org.alter.editor.ui

import javafx.geometry.Insets
import javafx.scene.control.Label
import javafx.scene.control.RadioButton
import javafx.scene.control.Separator
import javafx.scene.control.Spinner
import javafx.scene.control.SpinnerValueFactory
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.VBox
import org.alter.editor.model.EditorState
import org.alter.editor.model.ToolType

/**
 * Left panel with tool selection, overlay selector, brush size, and height level.
 */
class ToolPanel(private val state: EditorState) : VBox(8.0) {

    init {
        padding = Insets(10.0)
        prefWidth = 200.0
        style = "-fx-background-color: #2b2b2b;"

        // --- Tool selection ---
        val toolLabel = Label("Tool").apply { style = "-fx-text-fill: white; -fx-font-weight: bold;" }
        children.add(toolLabel)

        val toggleGroup = ToggleGroup()

        val toolEntries = listOf(
            "Select" to ToolType.SELECT,
            "Paint Overlay" to ToolType.PAINT_OVERLAY,
            "Height Brush" to ToolType.HEIGHT_BRUSH,
            "Collision" to ToolType.COLLISION_TOGGLE,
            "Fill" to ToolType.FILL,
        )

        for ((label, toolType) in toolEntries) {
            val rb = RadioButton(label).apply {
                this.toggleGroup = toggleGroup
                style = "-fx-text-fill: white;"
                userData = toolType
                isSelected = toolType == state.currentTool.get()
            }
            children.add(rb)
        }

        toggleGroup.selectedToggleProperty().addListener { _, _, newVal ->
            if (newVal != null) {
                state.currentTool.set(newVal.userData as ToolType)
            }
        }

        children.add(Separator())

        // --- Overlay selector ---
        val overlayLabel = Label("Overlay ID").apply { style = "-fx-text-fill: white; -fx-font-weight: bold;" }
        val overlaySpinner = Spinner<Int>(SpinnerValueFactory.IntegerSpinnerValueFactory(0, 5, state.selectedOverlay.get()))
        overlaySpinner.prefWidth = 100.0
        overlaySpinner.valueProperty().addListener { _, _, newVal ->
            state.selectedOverlay.set(newVal)
        }
        children.addAll(overlayLabel, overlaySpinner)

        children.add(Separator())

        // --- Brush size (visible when Height tool selected) ---
        val brushLabel = Label("Brush Size").apply { style = "-fx-text-fill: white; -fx-font-weight: bold;" }
        val brushSpinner = Spinner<Int>(SpinnerValueFactory.ListSpinnerValueFactory(
            javafx.collections.FXCollections.observableArrayList(1, 3, 5)
        ))
        brushSpinner.prefWidth = 100.0
        brushSpinner.valueProperty().addListener { _, _, newVal ->
            state.brushSize.set(newVal)
        }

        // Show/hide brush controls based on tool
        val updateBrushVisibility = {
            val isHeight = state.currentTool.get() == ToolType.HEIGHT_BRUSH
            brushLabel.isVisible = isHeight
            brushLabel.isManaged = isHeight
            brushSpinner.isVisible = isHeight
            brushSpinner.isManaged = isHeight
        }
        state.currentTool.addListener { _, _, _ -> updateBrushVisibility() }
        updateBrushVisibility()

        children.addAll(brushLabel, brushSpinner)

        children.add(Separator())

        // --- Height level selector ---
        val hlLabel = Label("Height Level").apply { style = "-fx-text-fill: white; -fx-font-weight: bold;" }
        val hlSpinner = Spinner<Int>(SpinnerValueFactory.IntegerSpinnerValueFactory(0, 3, state.currentHeightLevel.get()))
        hlSpinner.prefWidth = 100.0
        hlSpinner.valueProperty().addListener { _, _, newVal ->
            state.currentHeightLevel.set(newVal)
        }
        children.addAll(hlLabel, hlSpinner)
    }
}
