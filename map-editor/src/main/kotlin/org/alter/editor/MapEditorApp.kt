package org.alter.editor

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import javafx.stage.Stage

class MapEditorApp : Application() {
    override fun start(stage: Stage) {
        val root = StackPane(Label("OpenRune Map Editor"))
        stage.title = "OpenRune Map Editor"
        stage.scene = Scene(root, 1280.0, 800.0)
        stage.show()
    }
}

fun main() {
    Application.launch(MapEditorApp::class.java)
}
