# Map Editor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a JavaFX 3D terrain editor that reads/writes OSRS map regions directly from the game cache.

**Architecture:** New `map-editor` Gradle module depending on `:cache`. JavaFX 3D SubScene renders terrain as TriangleMesh. Tools modify in-memory RegionData which is serialized back to cache format. Orbit camera, ray-cast tile picking, undo/redo command stack.

**Tech Stack:** Kotlin, JavaFX 3D (TriangleMesh, SubScene, PerspectiveCamera), OpenRune cache library (`dev.openrune.filesystem.Cache`)

**Spec:** `docs/superpowers/specs/2026-03-24-map-editor-design.md`

---

## Phase 1: Project Setup & Data Model

### Task 1: Gradle Module Setup

**Files:**
- Create: `map-editor/build.gradle.kts`
- Modify: `settings.gradle.kts` — add `include(":map-editor")`
- Create: `map-editor/src/main/kotlin/org/alter/editor/MapEditorApp.kt`

- [ ] **Step 1: Create build.gradle.kts**

```kotlin
plugins {
    id("org.openjfx.javafxplugin") version "0.1.0"
}

javafx {
    version = "17"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.graphics")
}

dependencies {
    implementation(project(":cache"))
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("ch.qos.logback:logback-classic:1.4.14")
}
```

Note: Check if the root `build.gradle.kts` already applies the JavaFX plugin or if we need to add the plugin repo. Also check if `org.openjfx.javafxplugin` is available — if not, add JavaFX dependencies manually:
```kotlin
val javafxVersion = "17.0.2"
implementation("org.openjfx:javafx-controls:$javafxVersion")
implementation("org.openjfx:javafx-graphics:$javafxVersion")
```

- [ ] **Step 2: Add to settings.gradle.kts**

Add `include(":map-editor")` after the existing module includes.

- [ ] **Step 3: Create minimal MapEditorApp.kt**

```kotlin
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
```

- [ ] **Step 4: Verify it builds and launches**

Run the `map-editor` module's main class from IDE. Should show a window with "OpenRune Map Editor" label.

- [ ] **Step 5: Commit**

```bash
git add map-editor/ settings.gradle.kts
git commit -m "feat(map-editor): add Gradle module with minimal JavaFX app"
```

---

### Task 2: Data Model

**Files:**
- Create: `map-editor/src/main/kotlin/org/alter/editor/model/TileData.kt`
- Create: `map-editor/src/main/kotlin/org/alter/editor/model/RegionData.kt`
- Create: `map-editor/src/main/kotlin/org/alter/editor/model/EditorState.kt`

- [ ] **Step 1: Create TileData**

```kotlin
package org.alter.editor.model

/**
 * Data for a single map tile. Matches the cache format from RegionLoader.TileData.
 */
data class TileData(
    var overlayId: Short = 0,
    var overlayPath: Byte = 0,
    var overlayRotation: Byte = 0,
    var underlayId: Short = 0,
    var height: Int = 0,
    var settings: Byte = 0,  // BLOCKED_TILE=0x1, BRIDGE_TILE=0x2
) {
    val isBlocked: Boolean get() = (settings.toInt() and 0x1) != 0
    val isBridge: Boolean get() = (settings.toInt() and 0x2) != 0

    fun setBlocked(blocked: Boolean) {
        settings = if (blocked) {
            (settings.toInt() or 0x1).toByte()
        } else {
            (settings.toInt() and 0x1.inv()).toByte()
        }
    }
}
```

- [ ] **Step 2: Create RegionData**

```kotlin
package org.alter.editor.model

/**
 * In-memory representation of a 64x64 map region with 4 height levels.
 */
class RegionData(val regionId: Int) {
    // tiles[height][x][z] — matches cache format
    val tiles: Array<Array<Array<TileData>>> = Array(4) {
        Array(64) { Array(64) { TileData() } }
    }

    val regionX: Int get() = regionId shr 8
    val regionZ: Int get() = regionId and 0xFF
    val baseX: Int get() = regionX shl 6
    val baseZ: Int get() = regionZ shl 6

    fun getTile(height: Int, x: Int, z: Int): TileData = tiles[height][x][z]

    companion object {
        const val SIZE = 64
        const val HEIGHT_LEVELS = 4

        fun blank(regionId: Int): RegionData {
            // Flat grass region, all walkable
            val data = RegionData(regionId)
            for (h in 0 until HEIGHT_LEVELS) {
                for (x in 0 until SIZE) {
                    for (z in 0 until SIZE) {
                        data.tiles[h][x][z] = TileData(overlayId = 0, height = 0)
                    }
                }
            }
            return data
        }
    }
}
```

- [ ] **Step 3: Create EditorState**

```kotlin
package org.alter.editor.model

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleIntegerProperty

enum class ToolType { SELECT, PAINT_OVERLAY, HEIGHT_BRUSH, COLLISION_TOGGLE, FILL }

class EditorState {
    val currentTool: ObjectProperty<ToolType> = SimpleObjectProperty(ToolType.SELECT)
    val selectedOverlay: SimpleIntegerProperty = SimpleIntegerProperty(0)
    val brushSize: SimpleIntegerProperty = SimpleIntegerProperty(1)
    val currentHeight: SimpleIntegerProperty = SimpleIntegerProperty(0) // 0-3 height level being edited

    var selectedTileX: Int = -1
    var selectedTileZ: Int = -1

    var regionData: RegionData? = null
}
```

- [ ] **Step 4: Commit**

```bash
git add map-editor/src/main/kotlin/org/alter/editor/model/
git commit -m "feat(map-editor): add TileData, RegionData, EditorState data models"
```

---

## Phase 2: Cache I/O

### Task 3: Cache Region Loader

**Files:**
- Create: `map-editor/src/main/kotlin/org/alter/editor/io/CacheRegionLoader.kt`

- [ ] **Step 1: Implement CacheRegionLoader**

This reads terrain data from the OSRS cache. The parsing logic matches `game-server/.../fs/RegionLoader.kt` `loadTerrain()` method exactly.

```kotlin
package org.alter.editor.io

import dev.openrune.filesystem.Cache
import org.alter.editor.model.RegionData
import org.alter.editor.model.TileData
import java.io.File
import java.nio.ByteBuffer

object CacheRegionLoader {

    /**
     * Load a region's terrain data from the cache.
     * @param cachePath Path to the cache directory (e.g., "data/cache/LIVE")
     * @param regionId The region ID to load
     * @return RegionData or null if region doesn't exist
     */
    fun load(cachePath: String, regionId: Int): RegionData? {
        val cache = Cache.load(File(cachePath).toPath())
        val regionX = regionId shr 8
        val regionZ = regionId and 0xFF

        // Map archive is index 5, terrain files named "m{x}_{z}"
        val mapData = cache.data(5, "m${regionX}_$regionZ") ?: return null

        val region = RegionData(regionId)
        parseTerrain(mapData, region)
        return region
    }

    /**
     * Parse terrain data from raw bytes into RegionData.
     * Format matches RegionLoader.loadTerrain() in game-server.
     */
    private fun parseTerrain(data: ByteArray, region: RegionData) {
        val buf = ByteBuffer.wrap(data)

        for (height in 0 until 4) {
            for (x in 0 until 64) {
                for (z in 0 until 64) {
                    val tile = region.tiles[height][x][z]
                    decodeTile(buf, tile)
                }
            }
        }
    }

    private fun decodeTile(buf: ByteBuffer, tile: TileData) {
        while (true) {
            val attribute = buf.get().toInt() and 0xFF
            when {
                attribute == 0 -> return  // End of tile
                attribute == 1 -> {
                    tile.height = buf.get().toInt() and 0xFF
                }
                attribute in 2..49 -> {
                    // Overlay data (rev208+ uses 2-byte overlay ID)
                    tile.overlayId = buf.short
                    tile.overlayPath = ((attribute - 2) / 4).toByte()
                    tile.overlayRotation = ((attribute - 2) and 3).toByte()
                }
                attribute in 50..81 -> {
                    tile.settings = (attribute - 49).toByte()
                }
                attribute >= 82 -> {
                    tile.underlayId = (attribute - 81).toShort()
                }
            }
        }
    }
}
```

Read `game-server/src/main/kotlin/org/alter/game/fs/RegionLoader.kt` to verify the parsing logic matches exactly — especially the rev208 overlay ID handling and attribute ranges.

- [ ] **Step 2: Commit**

```bash
git add map-editor/src/main/kotlin/org/alter/editor/io/CacheRegionLoader.kt
git commit -m "feat(map-editor): add CacheRegionLoader for reading terrain from cache"
```

---

### Task 4: Cache Region Saver

**Files:**
- Create: `map-editor/src/main/kotlin/org/alter/editor/io/CacheRegionSaver.kt`

- [ ] **Step 1: Implement CacheRegionSaver**

Encodes RegionData back to the cache terrain format — the reverse of the loader.

```kotlin
package org.alter.editor.io

import dev.openrune.filesystem.Cache
import org.alter.editor.model.RegionData
import org.alter.editor.model.TileData
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File

object CacheRegionSaver {

    /**
     * Save a region's terrain data to the cache.
     * @param cachePath Path to the cache directory
     * @param region The RegionData to save
     */
    fun save(cachePath: String, region: RegionData) {
        val cache = Cache.load(File(cachePath).toPath())
        val regionX = region.regionX
        val regionZ = region.regionZ

        val encoded = encodeTerrain(region)
        cache.write(5, "m${regionX}_$regionZ", encoded)
    }

    private fun encodeTerrain(region: RegionData): ByteArray {
        val baos = ByteArrayOutputStream()
        val out = DataOutputStream(baos)

        for (height in 0 until 4) {
            for (x in 0 until 64) {
                for (z in 0 until 64) {
                    encodeTile(out, region.tiles[height][x][z])
                }
            }
        }

        return baos.toByteArray()
    }

    private fun encodeTile(out: DataOutputStream, tile: TileData) {
        // Overlay
        if (tile.overlayId.toInt() != 0) {
            val attribute = 2 + (tile.overlayPath.toInt() * 4) + tile.overlayRotation.toInt()
            out.writeByte(attribute)
            out.writeShort(tile.overlayId.toInt()) // rev208+ 2-byte overlay
        }
        // Settings/collision
        if (tile.settings.toInt() != 0) {
            out.writeByte(tile.settings.toInt() + 49)
        }
        // Underlay
        if (tile.underlayId.toInt() != 0) {
            out.writeByte(tile.underlayId.toInt() + 81)
        }
        // Height
        if (tile.height != 0) {
            out.writeByte(1)
            out.writeByte(tile.height)
        }
        // End marker
        out.writeByte(0)
    }
}
```

NOTE: The exact `cache.write()` API for map archives needs to be verified. Read how `PackServerConfig` writes data and check if the Cache class supports writing by name string ("m{x}_{z}") or needs a numeric group/file ID. The implementer must read the `dev.openrune.filesystem.Cache` API to determine the correct write method signature.

- [ ] **Step 2: Commit**

```bash
git add map-editor/src/main/kotlin/org/alter/editor/io/CacheRegionSaver.kt
git commit -m "feat(map-editor): add CacheRegionSaver for writing terrain to cache"
```

---

## Phase 3: 3D Viewport

### Task 5: Terrain Mesh Builder

**Files:**
- Create: `map-editor/src/main/kotlin/org/alter/editor/viewport/TerrainMesh.kt`

- [ ] **Step 1: Implement TerrainMesh**

Builds a JavaFX `TriangleMesh` from RegionData. 65x65 vertex grid (shared edges), 8192 triangles. Height values map to Y axis. Overlay types map to vertex colors via a color lookup.

Key JavaFX 3D classes:
- `TriangleMesh` — vertex positions (points), texture coords, face indices
- `MeshView` — renders the mesh with a material
- `PhongMaterial` — diffuse color per face (or use vertex colors via texture map)

For per-tile coloring without textures, create a small 1-pixel-per-overlay texture atlas and map each tile's UV coordinates to its overlay color.

The mesh must be rebuildable when tiles change (height/overlay edits).

- [ ] **Step 2: Commit**

---

### Task 6: Camera Controller

**Files:**
- Create: `map-editor/src/main/kotlin/org/alter/editor/viewport/CameraController.kt`

- [ ] **Step 1: Implement orbit/pan/zoom camera**

- `PerspectiveCamera` attached to a pivot `Group` at the region center
- Right-click drag: orbit (rotate pivot X/Y)
- Scroll: zoom (camera translateZ)
- Middle-click drag: pan (translate pivot X/Z)
- Initial position: 45-degree angle, looking at center of 64x64 region
- Zoom constraints: min 50, max 2000 units

- [ ] **Step 2: Commit**

---

### Task 7: Viewport SubScene Integration

**Files:**
- Create: `map-editor/src/main/kotlin/org/alter/editor/viewport/ViewportPane.kt`
- Modify: `map-editor/src/main/kotlin/org/alter/editor/MapEditorApp.kt`

- [ ] **Step 1: Create ViewportPane**

Wraps a `SubScene` containing the terrain mesh, camera, and ambient light. Handles mouse events and delegates to CameraController and TilePicker.

- [ ] **Step 2: Update MapEditorApp to show viewport**

Replace the placeholder label with the 3-panel layout: ToolPanel | ViewportPane | PropertiesPanel. Load a hardcoded region (or blank region) and display the terrain.

- [ ] **Step 3: Verify** — Launch the app. Should see a colored 3D terrain grid with orbit camera.

- [ ] **Step 4: Commit**

---

### Task 8: Tile Picker

**Files:**
- Create: `map-editor/src/main/kotlin/org/alter/editor/viewport/TilePicker.kt`

- [ ] **Step 1: Implement ray-cast tile picking**

On mouse move over the SubScene, determine which tile (x, z) the cursor is over:
- Use `SubScene.getPickResult()` or manual ray-cast from camera through mouse position
- JavaFX provides `PickResult` on mouse events which includes the intersection point on the mesh
- Convert the 3D intersection point to tile coordinates: `tileX = floor(worldX)`, `tileZ = floor(worldZ)`

- [ ] **Step 2: Add tile highlight**

Render a semi-transparent colored quad slightly above the hovered tile. Update position on mouse move.

- [ ] **Step 3: Commit**

---

## Phase 4: UI & Tools

### Task 9: Editor Layout (Tool Panel + Properties Panel)

**Files:**
- Create: `map-editor/src/main/kotlin/org/alter/editor/ui/ToolPanel.kt`
- Create: `map-editor/src/main/kotlin/org/alter/editor/ui/PropertiesPanel.kt`
- Create: `map-editor/src/main/kotlin/org/alter/editor/ui/MenuBarSetup.kt`
- Modify: `map-editor/src/main/kotlin/org/alter/editor/MapEditorApp.kt`

- [ ] **Step 1: Create ToolPanel**

Left panel with toggle buttons for each tool (Select, Paint, Height, Collision, Fill). Paint tool shows overlay type dropdown. Height tool shows brush size selector. Binds to EditorState properties.

- [ ] **Step 2: Create PropertiesPanel**

Right panel showing selected tile info: coordinates, overlay ID (editable dropdown), height per corner (editable spinners), collision checkbox. Updates when tile selection changes. Edits apply to RegionData and rebuild mesh.

- [ ] **Step 3: Create MenuBarSetup**

File menu: New Region, Open Region (by ID), Save (Ctrl+S), Export to Server, Exit.
Edit menu: Undo (Ctrl+Z), Redo (Ctrl+Y).
Region menu: Region Info (shows ID, coordinates).

- [ ] **Step 4: Wire into MapEditorApp**

BorderPane layout: MenuBar top, ToolPanel left, ViewportPane center, PropertiesPanel right, StatusBar bottom.

- [ ] **Step 5: Commit**

---

### Task 10: Select Tool

**Files:**
- Create: `map-editor/src/main/kotlin/org/alter/editor/tools/Tool.kt`
- Create: `map-editor/src/main/kotlin/org/alter/editor/tools/SelectTool.kt`

- [ ] **Step 1: Create Tool interface and SelectTool**

```kotlin
interface Tool {
    val name: String
    fun onTileClick(x: Int, z: Int, state: EditorState): Command?
    fun onTileDrag(x: Int, z: Int, state: EditorState): Command?
    fun onTileRelease(state: EditorState)
}
```

SelectTool: updates `selectedTileX/Z` in EditorState, triggers PropertiesPanel refresh. Returns null command (read-only).

- [ ] **Step 2: Commit**

---

### Task 11: Paint Overlay Tool + Undo System

**Files:**
- Create: `map-editor/src/main/kotlin/org/alter/editor/commands/Command.kt`
- Create: `map-editor/src/main/kotlin/org/alter/editor/commands/PaintCommand.kt`
- Create: `map-editor/src/main/kotlin/org/alter/editor/commands/CommandStack.kt`
- Create: `map-editor/src/main/kotlin/org/alter/editor/tools/PaintOverlayTool.kt`

- [ ] **Step 1: Create Command interface and CommandStack**

```kotlin
interface Command {
    fun execute()
    fun undo()
    val description: String
}

class CommandStack(private val maxSize: Int = 100) {
    private val undoStack = ArrayDeque<Command>()
    private val redoStack = ArrayDeque<Command>()

    fun execute(cmd: Command) {
        cmd.execute()
        undoStack.addLast(cmd)
        redoStack.clear()
        if (undoStack.size > maxSize) undoStack.removeFirst()
    }

    fun undo(): Boolean { ... }
    fun redo(): Boolean { ... }
}
```

- [ ] **Step 2: Create PaintCommand and PaintOverlayTool**

PaintCommand stores: list of (x, z, oldOverlay, newOverlay). Execute sets new, undo restores old.

PaintOverlayTool: on click/drag, creates PaintCommand for each tile. Drag batches into CompoundCommand.

- [ ] **Step 3: Verify** — Select paint tool, pick an overlay, click tiles. They should change color. Ctrl+Z undoes.

- [ ] **Step 4: Commit**

---

### Task 12: Height Brush Tool

**Files:**
- Create: `map-editor/src/main/kotlin/org/alter/editor/commands/HeightCommand.kt`
- Create: `map-editor/src/main/kotlin/org/alter/editor/tools/HeightBrushTool.kt`

- [ ] **Step 1: Implement HeightBrushTool**

Click raises tile height by 1 (Shift+click lowers). Brush size applies to surrounding tiles. HeightCommand stores old/new heights. Mesh rebuilds after each edit to show updated terrain.

- [ ] **Step 2: Commit**

---

### Task 13: Collision Toggle Tool

**Files:**
- Create: `map-editor/src/main/kotlin/org/alter/editor/commands/CollisionCommand.kt`
- Create: `map-editor/src/main/kotlin/org/alter/editor/tools/CollisionToggleTool.kt`

- [ ] **Step 1: Implement CollisionToggleTool**

Click toggles tile blocked flag. Blocked tiles show red tint on the mesh (modify the overlay color lookup to blend red for blocked tiles).

- [ ] **Step 2: Commit**

---

### Task 14: Fill Tool

**Files:**
- Create: `map-editor/src/main/kotlin/org/alter/editor/tools/FillTool.kt`

- [ ] **Step 1: Implement FillTool**

Flood fill from clicked tile. BFS/DFS expanding to adjacent tiles with same overlay. Stops at different overlay or region edge. Produces PaintCommand for all filled tiles.

- [ ] **Step 2: Commit**

---

## Phase 5: File Operations

### Task 15: Open / Save / New Region

**Files:**
- Modify: `map-editor/src/main/kotlin/org/alter/editor/ui/MenuBarSetup.kt`
- Modify: `map-editor/src/main/kotlin/org/alter/editor/MapEditorApp.kt`

- [ ] **Step 1: Implement Open Region dialog**

Dialog asks for region ID (integer input). Loads from `data/cache/LIVE` via CacheRegionLoader. Rebuilds mesh and resets EditorState.

- [ ] **Step 2: Implement New Region dialog**

Dialog asks for region ID. Creates blank RegionData. Displays flat grass terrain.

- [ ] **Step 3: Implement Save**

Ctrl+S saves current RegionData to `data/cache/EDITOR/` via CacheRegionSaver. Creates EDITOR directory if needed.

- [ ] **Step 4: Implement Export to Server**

Copies the saved region from EDITOR to LIVE cache. Shows confirmation dialog.

- [ ] **Step 5: Commit**

---

### Task 16: Status Bar + Polish

**Files:**
- Create: `map-editor/src/main/kotlin/org/alter/editor/ui/StatusBar.kt`

- [ ] **Step 1: Add status bar**

Shows: current region ID, hovered tile coordinates (x, z), current tool name. Updates on mouse move and tool change.

- [ ] **Step 2: Add keyboard shortcuts**

Ctrl+Z: undo, Ctrl+Y: redo, Ctrl+S: save, 1-5: tool selection hotkeys.

- [ ] **Step 3: Final verification**

Launch editor, open an existing OSRS region (e.g., Lumbridge region 12850), verify terrain renders correctly. Paint some tiles, change heights, toggle collision. Save and verify file is written.

- [ ] **Step 4: Commit**

---

## Summary

| Phase | Tasks | Description |
|-------|-------|-------------|
| 1 | 1-2 | Module setup, data model |
| 2 | 3-4 | Cache loading and saving |
| 3 | 5-8 | 3D viewport, camera, tile picking |
| 4 | 9-14 | UI layout, all 5 editing tools, undo/redo |
| 5 | 15-16 | File operations, status bar, polish |
