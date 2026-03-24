package org.alter.editor.model

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty

enum class ToolType { SELECT, PAINT_OVERLAY, HEIGHT_BRUSH, COLLISION_TOGGLE, FILL }

/**
 * Observable state for the editor. UI components bind to these properties.
 */
class EditorState {
    val currentTool = SimpleObjectProperty(ToolType.SELECT)
    val selectedOverlay = SimpleIntegerProperty(0)
    val brushSize = SimpleIntegerProperty(1)
    val currentHeightLevel = SimpleIntegerProperty(0)  // which of 4 height levels we're editing

    var selectedTileX: Int = -1
    var selectedTileZ: Int = -1
    var hoveredTileX: Int = -1
    var hoveredTileZ: Int = -1

    var regionData: RegionData? = null
    var dirty: Boolean = false  // true if unsaved changes exist
}
