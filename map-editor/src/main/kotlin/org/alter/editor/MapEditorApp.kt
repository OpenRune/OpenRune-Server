package org.alter.editor

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.SceneAntialiasing
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import org.alter.editor.model.EditorState
import org.alter.editor.model.RegionData
import org.alter.editor.viewport.ViewportPane

class MapEditorApp : Application() {
    override fun start(stage: Stage) {
        val state = EditorState()
        state.regionData = RegionData.blank(12850) // test region

        val viewport = ViewportPane(state)
        viewport.loadRegion(state.regionData!!)

        val root = BorderPane()
        root.center = viewport

        stage.title = "OpenRune Map Editor"
        stage.scene = Scene(root, 1280.0, 800.0, true, SceneAntialiasing.BALANCED)
        stage.show()
    }
}

fun main() {
    Application.launch(MapEditorApp::class.java)
}
