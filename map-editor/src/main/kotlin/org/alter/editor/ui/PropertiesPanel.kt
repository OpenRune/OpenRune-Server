package org.alter.editor.ui

import javafx.geometry.Insets
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.Separator
import javafx.scene.control.Spinner
import javafx.scene.control.SpinnerValueFactory
import javafx.scene.layout.VBox
import org.alter.editor.commands.CollisionCommand
import org.alter.editor.commands.CommandStack
import org.alter.editor.commands.HeightCommand
import org.alter.editor.commands.PaintOverlayCommand
import org.alter.editor.model.EditorState

/**
 * Right panel showing and editing properties of the selected tile.
 */
class PropertiesPanel(
    private val state: EditorState,
    private val commandStack: CommandStack,
    private val onMeshDirty: () -> Unit,
) : VBox(8.0) {

    private val titleLabel = Label("No tile selected").apply {
        style = "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13;"
    }
    private val overlaySpinner = Spinner<Int>(SpinnerValueFactory.IntegerSpinnerValueFactory(0, 5, 0))
    private val heightSpinner = Spinner<Int>(SpinnerValueFactory.IntegerSpinnerValueFactory(0, 255, 0))
    private val collisionCheckBox = CheckBox("Blocked")

    private var updatingFromTile = false

    init {
        padding = Insets(10.0)
        prefWidth = 220.0
        style = "-fx-background-color: #2b2b2b;"

        val overlayLabel = Label("Overlay ID").apply { style = "-fx-text-fill: white;" }
        overlaySpinner.prefWidth = 100.0
        val heightLabel = Label("Height").apply { style = "-fx-text-fill: white;" }
        heightSpinner.prefWidth = 100.0
        collisionCheckBox.style = "-fx-text-fill: white;"

        children.addAll(
            titleLabel,
            Separator(),
            overlayLabel, overlaySpinner,
            heightLabel, heightSpinner,
            collisionCheckBox,
        )

        // --- Overlay edit ---
        overlaySpinner.valueProperty().addListener { _, oldVal, newVal ->
            if (updatingFromTile || oldVal == null || newVal == null) return@addListener
            applyOverlayEdit(newVal.toShort())
        }

        // --- Height edit ---
        heightSpinner.valueProperty().addListener { _, oldVal, newVal ->
            if (updatingFromTile || oldVal == null || newVal == null) return@addListener
            applyHeightEdit(oldVal, newVal)
        }

        // --- Collision edit ---
        collisionCheckBox.selectedProperty().addListener { _, oldVal, newVal ->
            if (updatingFromTile || oldVal == null || newVal == null) return@addListener
            applyCollisionEdit(oldVal)
        }
    }

    /** Call this whenever the selected tile changes to refresh the panel. */
    fun refreshFromSelection() {
        val region = state.regionData ?: return
        val x = state.selectedTileX
        val z = state.selectedTileZ
        if (x < 0 || z < 0) {
            titleLabel.text = "No tile selected"
            return
        }

        val hl = state.currentHeightLevel.get()
        val tile = region.getTile(hl, x, z)

        updatingFromTile = true
        titleLabel.text = "Tile ($x, $z)"
        (overlaySpinner.valueFactory as SpinnerValueFactory.IntegerSpinnerValueFactory).value = tile.overlayId.toInt()
        (heightSpinner.valueFactory as SpinnerValueFactory.IntegerSpinnerValueFactory).value = tile.height
        collisionCheckBox.isSelected = tile.isBlocked
        updatingFromTile = false
    }

    private fun applyOverlayEdit(newOverlay: Short) {
        val region = state.regionData ?: return
        val x = state.selectedTileX
        val z = state.selectedTileZ
        if (x < 0 || z < 0) return
        val hl = state.currentHeightLevel.get()
        val tile = region.getTile(hl, x, z)
        val cmd = PaintOverlayCommand(region, hl, listOf(Triple(x, z, tile.overlayId)), newOverlay)
        commandStack.execute(cmd)
        state.dirty = true
        onMeshDirty()
    }

    private fun applyHeightEdit(oldVal: Int, newVal: Int) {
        val region = state.regionData ?: return
        val x = state.selectedTileX
        val z = state.selectedTileZ
        if (x < 0 || z < 0) return
        val hl = state.currentHeightLevel.get()
        val delta = newVal - oldVal
        val cmd = HeightCommand(region, hl, listOf(Triple(x, z, oldVal)), delta)
        commandStack.execute(cmd)
        state.dirty = true
        onMeshDirty()
    }

    private fun applyCollisionEdit(wasBlocked: Boolean) {
        val region = state.regionData ?: return
        val x = state.selectedTileX
        val z = state.selectedTileZ
        if (x < 0 || z < 0) return
        val hl = state.currentHeightLevel.get()
        val cmd = CollisionCommand(region, hl, listOf(Triple(x, z, wasBlocked)))
        commandStack.execute(cmd)
        state.dirty = true
        onMeshDirty()
    }
}
