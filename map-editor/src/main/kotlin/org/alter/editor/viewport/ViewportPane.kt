package org.alter.editor.viewport

import javafx.scene.AmbientLight
import javafx.scene.Group
import javafx.scene.PerspectiveCamera
import javafx.scene.PointLight
import javafx.scene.SceneAntialiasing
import javafx.scene.SubScene
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import org.alter.editor.model.EditorState
import org.alter.editor.model.RegionData

/**
 * Wraps the 3D viewport (terrain mesh, camera, lights) into a JavaFX node
 * that can be placed in the editor layout.
 */
class ViewportPane(private val state: EditorState) : StackPane() {

    private val worldRoot = Group()
    private val pivot = Group()
    private val camera = PerspectiveCamera(true).apply {
        nearClip = 0.1
        farClip = 1000.0
    }
    private val cameraController: CameraController
    private val tilePicker = TilePicker()
    private var terrainMesh: TerrainMesh? = null

    private val subScene: SubScene

    init {
        // Camera lives inside the pivot so orbit transforms move it
        pivot.children.add(camera)
        worldRoot.children.add(pivot)

        // Lighting
        val ambient = AmbientLight(Color.color(0.6, 0.6, 0.6))
        val pointLight = PointLight(Color.WHITE).apply {
            translateX = 32.0
            translateY = -30.0  // above the terrain (negative Y = up)
            translateZ = 32.0
        }
        worldRoot.children.addAll(ambient, pointLight)

        // SubScene — 3D content embedded in the 2D layout
        subScene = SubScene(worldRoot, 800.0, 600.0, true, SceneAntialiasing.BALANCED)
        subScene.fill = Color.web("#1a1a2e")
        subScene.camera = camera

        // Bind SubScene size to this pane so it resizes with the window
        subScene.widthProperty().bind(widthProperty())
        subScene.heightProperty().bind(heightProperty())

        children.add(subScene)

        // Camera controller
        cameraController = CameraController(camera, pivot)
        cameraController.attachTo(subScene)

        // Tile picking on mouse move / click
        subScene.addEventHandler(MouseEvent.MOUSE_MOVED) { onMouseMoved(it) }
        subScene.addEventHandler(MouseEvent.MOUSE_CLICKED) { onMouseClicked(it) }
    }

    /** Build the terrain mesh for [regionData] and add it to the scene. */
    fun loadRegion(regionData: RegionData) {
        // Remove old mesh if present
        terrainMesh?.let { worldRoot.children.remove(it.meshView) }

        val mesh = TerrainMesh(regionData, state.currentHeightLevel.get())
        terrainMesh = mesh
        worldRoot.children.add(mesh.meshView)
    }

    /** Rebuild the mesh after tile edits. */
    fun rebuildMesh() {
        terrainMesh?.rebuild()
    }

    // ----- event handlers --------------------------------------------------

    private fun onMouseMoved(e: MouseEvent) {
        val pick = e.pickResult ?: return
        val tile = tilePicker.pickTile(pick)
        if (tile != null) {
            state.hoveredTileX = tile.first
            state.hoveredTileZ = tile.second
        }
    }

    private fun onMouseClicked(e: MouseEvent) {
        if (e.button == javafx.scene.input.MouseButton.PRIMARY) {
            val pick = e.pickResult ?: return
            val tile = tilePicker.pickTile(pick)
            if (tile != null) {
                state.selectedTileX = tile.first
                state.selectedTileZ = tile.second
            }
        }
    }
}
