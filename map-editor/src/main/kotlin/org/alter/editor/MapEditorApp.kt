package org.alter.editor

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.SceneAntialiasing
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import org.alter.editor.commands.CommandStack
import org.alter.editor.model.EditorState
import org.alter.editor.model.RegionData
import org.alter.editor.ui.PropertiesPanel
import org.alter.editor.ui.StatusBar
import org.alter.editor.ui.ToolPanel
import org.alter.editor.viewport.ViewportPane

class MapEditorApp : Application() {
    override fun start(stage: Stage) {
        val state = EditorState()
        state.regionData = RegionData.blank(12850) // test region

        val commandStack = CommandStack()

        // Use lateinit so lambdas can reference components created later
        lateinit var propertiesPanel: PropertiesPanel
        lateinit var statusBar: StatusBar

        val viewport = ViewportPane(state, commandStack) {
            propertiesPanel.refreshFromSelection()
            statusBar.refresh()
        }
        viewport.loadRegion(state.regionData!!)

        val toolPanel = ToolPanel(state)

        propertiesPanel = PropertiesPanel(state, commandStack) {
            viewport.rebuildMesh()
        }

        statusBar = StatusBar(state)

        // MenuBar
        val undoItem = MenuItem("Undo").apply {
            accelerator = KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN)
            setOnAction {
                if (commandStack.undo()) {
                    viewport.rebuildMesh()
                    propertiesPanel.refreshFromSelection()
                }
            }
        }
        val redoItem = MenuItem("Redo").apply {
            accelerator = KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN)
            setOnAction {
                if (commandStack.redo()) {
                    viewport.rebuildMesh()
                    propertiesPanel.refreshFromSelection()
                }
            }
        }
        val exitItem = MenuItem("Exit").apply {
            setOnAction { stage.close() }
        }

        val menuBar = MenuBar(
            Menu("File", null, exitItem),
            Menu("Edit", null, undoItem, SeparatorMenuItem(), redoItem),
        )

        // Layout
        val root = BorderPane()
        root.top = menuBar
        root.left = toolPanel
        root.center = viewport
        root.right = propertiesPanel
        root.bottom = statusBar

        stage.title = "OpenRune Map Editor"
        stage.scene = Scene(root, 1280.0, 800.0, true, SceneAntialiasing.BALANCED)

        // Keyboard shortcuts (Ctrl+Z / Ctrl+Y) also work via menu accelerators,
        // but we add scene-level handling for robustness.
        stage.scene.setOnKeyPressed { e ->
            if (e.isControlDown && e.code == KeyCode.Z) {
                if (commandStack.undo()) {
                    viewport.rebuildMesh()
                    propertiesPanel.refreshFromSelection()
                }
            } else if (e.isControlDown && e.code == KeyCode.Y) {
                if (commandStack.redo()) {
                    viewport.rebuildMesh()
                    propertiesPanel.refreshFromSelection()
                }
            }
        }

        // Periodically refresh status bar for hovered tile updates
        val timer = object : javafx.animation.AnimationTimer() {
            private var lastUpdate = 0L
            override fun handle(now: Long) {
                if (now - lastUpdate > 100_000_000) { // ~10 FPS
                    statusBar.refresh()
                    lastUpdate = now
                }
            }
        }
        timer.start()

        stage.show()
    }
}

fun main() {
    Application.launch(MapEditorApp::class.java)
}
