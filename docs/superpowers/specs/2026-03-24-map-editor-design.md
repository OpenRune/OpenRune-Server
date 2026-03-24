# Map Editor Design Specification

**Date:** 2026-03-24
**Scope:** JavaFX 3D terrain editor for creating custom OSRS map regions
**Purpose:** Enable building a fully custom game world for the RPG server

---

## 1. Overview

A desktop map editor built with Kotlin/JavaFX that lets you create and edit OSRS map regions in 3D. The editor reads/writes directly to the OSRS cache format that OpenRune already loads, so what you save is what the server runs.

### Why
The server is being built as a custom RPG with an original world. Existing RSPS map editors don't support revision 236. This editor fills that gap using the same Kotlin/cache tooling the server already uses.

### Scope — Terrain Only (V1)
- Paint tile overlays (grass, stone, dirt, water, sand, etc.)
- Edit terrain height per tile corner
- Toggle tile collision (walkable/blocked)
- 3D viewport with orbit/pan/zoom camera
- Load/save regions from/to OSRS cache format
- Create new blank regions

### Out of Scope (V1)
- Object placement (trees, walls, buildings) — V2
- NPC spawn point placement — V2
- Multi-region editing / world map overview — V2
- OSRS texture rendering — using solid colors per overlay type
- Underlay editing — overlays only for V1

---

## 2. Application Architecture

### Window Layout

```
┌──────────────────────────────────────────────────┐
│  Menu Bar (File, Edit, Region)                    │
├────────┬─────────────────────────┬───────────────┤
│        │                         │               │
│ Tool   │    3D Viewport          │  Properties   │
│ Panel  │    (JavaFX SubScene)    │  Panel        │
│        │                         │               │
│ - Select    Orbit camera with    │  - Tile x,z   │
│ - Paint     mouse controls       │  - Overlay ID │
│ - Height                         │  - Heights    │
│ - Collision  Click to select/    │    NW,NE,SW,SE│
│ - Fill       edit tiles          │  - Collision   │
│        │                         │               │
├────────┴─────────────────────────┴───────────────┤
│  Status Bar (cursor tile coords, region ID)       │
└──────────────────────────────────────────────────┘
```

### Module Structure

New Gradle module `map-editor` alongside existing modules. Depends on `:cache` for cache read/write.

```
map-editor/
  build.gradle.kts
  src/main/kotlin/org/alter/editor/
    MapEditorApp.kt              — JavaFX Application entry point
    ui/
      EditorLayout.kt            — Main window: toolbar, viewport, panels
      ToolPanel.kt               — Left panel: tool buttons
      PropertiesPanel.kt         — Right panel: tile property editor
      MenuBarSetup.kt            — File/Edit/Region menus
    viewport/
      TerrainMesh.kt             — Builds TriangleMesh from tile data
      CameraController.kt        — Orbit/pan/zoom mouse controls
      TilePicker.kt              — Ray cast mouse → tile coordinate
      TileHighlight.kt           — Wireframe overlay on selected tile
    model/
      RegionData.kt              — In-memory region: 64x64 TileData array
      TileData.kt                — Per tile: overlay, 4 corner heights, collision
      EditorState.kt             — Current tool, selection, undo stack
    tools/
      Tool.kt                    — Interface for all editing tools
      SelectTool.kt              — Click to inspect tile
      PaintOverlayTool.kt        — Paint overlay type on tiles
      HeightBrushTool.kt         — Raise/lower terrain height
      CollisionToggleTool.kt     — Toggle walkable/blocked
      FillTool.kt                — Flood fill overlay type
    io/
      CacheRegionLoader.kt       — Reads region tile data from cache
      CacheRegionSaver.kt        — Writes region tile data to cache
    commands/
      Command.kt                 — Undo/redo interface
      PaintCommand.kt            — Undoable overlay paint
      HeightCommand.kt           — Undoable height change
      CollisionCommand.kt        — Undoable collision toggle
```

---

## 3. 3D Viewport

### Terrain Mesh

- One region = 64x64 tiles
- Each tile = 4 vertices (corners shared with neighbors) + 2 triangles
- Vertex grid: 65x65 vertices
- Total triangles: ~8192 per region
- Height values from cache data raise/lower each vertex on the Y axis

```
Vertex layout per tile (x,z):
  (x, z+1) ---- (x+1, z+1)
      |    \          |
      |     \         |
      |      \        |
  (x, z) ---- (x+1, z)
```

### Materials & Colors

Each overlay type maps to a solid `PhongMaterial` color:

| Overlay | Color |
|---------|-------|
| Grass | Green (#4a7c2c) |
| Dirt/path | Brown (#8b6914) |
| Stone/floor | Gray (#808080) |
| Water | Blue (#2060c0) |
| Sand | Yellow (#c8b464) |
| No overlay | Dark green (#2d5a1e) |

V2 could add actual OSRS textures, but solid colors are sufficient for terrain design.

### Camera

- **Orbit:** Right-click drag rotates around a focal point
- **Zoom:** Scroll wheel moves camera closer/further
- **Pan:** Middle-click drag moves the focal point
- **Default view:** 45-degree angle looking down at region center
- **Constraints:** Min/max zoom distance, can't go below terrain

Implementation: `PerspectiveCamera` attached to a pivot `Group`. Rotation modifies pivot transforms, zoom modifies camera `translateZ`.

### Tile Picking

- On mouse move: cast ray from camera through mouse position into scene
- Intersect ray with terrain mesh to find hit point
- Convert hit world coordinate to tile (x, z) by flooring
- Highlight hovered tile with a semi-transparent colored quad above the mesh
- On click: pass tile coordinate to current tool

---

## 4. Editing Tools

### Tool Interface

```kotlin
interface Tool {
    val name: String
    val icon: String  // icon resource path
    fun onTileClick(tile: TileCoord, state: EditorState): Command?
    fun onTileDrag(tile: TileCoord, state: EditorState): Command?
    fun onTileRelease(state: EditorState)
}
```

Each tool returns a `Command` for undo/redo support. Drag operations batch into a single compound command.

### Select Tool
- Click: select single tile, show in properties panel
- Drag: select rectangular area (for batch operations)
- No command produced (read-only)

### Paint Overlay Tool
- Select overlay type from dropdown in tool panel
- Click: paint single tile
- Drag: paint all tiles under cursor
- Produces `PaintCommand(tiles, oldOverlays, newOverlay)`

### Height Brush Tool
- Click: raise selected vertex/tile center by 1 height unit
- Shift+click: lower by 1
- Brush size selector: 1, 3, 5 tiles (applies to all vertices in radius)
- Height range: 0-3 (matching OSRS height levels, with finer subdivision using cache height values 0-255)
- Produces `HeightCommand(vertices, oldHeights, newHeights)`

### Collision Toggle Tool
- Click: toggle tile between walkable and blocked
- Blocked tiles rendered with red-tinted overlay
- Drag: paint collision across tiles
- Produces `CollisionCommand(tiles, oldFlags, newFlags)`

### Fill Tool
- Click: flood fill from clicked tile outward
- Fills all connected tiles with same overlay type
- Stops at tiles with different overlay or region edge
- Produces `PaintCommand(filledTiles, oldOverlays, newOverlay)`

---

## 5. Properties Panel

Shows and allows direct editing of the selected tile:

```
┌─ Tile Properties ──────────┐
│ Position: (32, 48)         │
│                            │
│ Overlay: [Grass      ▼]   │
│                            │
│ Height:                    │
│   NW: [12 ▲▼]  NE: [12 ▲▼]│
│   SW: [10 ▲▼]  SE: [10 ▲▼]│
│                            │
│ Collision: [✓ Walkable]    │
└────────────────────────────┘
```

Changes in the properties panel apply immediately to the mesh and produce undo commands.

---

## 6. Undo/Redo System

```kotlin
interface Command {
    fun execute()
    fun undo()
    val description: String
}
```

- Command stack with configurable max size (default 100)
- Ctrl+Z: undo, Ctrl+Y: redo
- Drag operations produce a `CompoundCommand` wrapping multiple atomic changes
- Stack cleared on save (optional) or region switch

---

## 7. Cache Integration

### Loading a Region

```
User selects region ID (or "New Region")
  → CacheRegionLoader reads from data/cache/LIVE (or EDITOR working dir)
  → Parses tile overlay IDs, height values, collision flags
  → Builds RegionData (64x64 TileData array)
  → TerrainMesh generates 3D mesh from RegionData
  → Viewport displays mesh
```

Uses the OpenRune/OpenRS2 cache library already in the project (`dev.openrune.filesystem.Cache`). Region data is stored in cache archive index 5 (map data).

### Saving a Region

```
User clicks Save (Ctrl+S)
  → CacheRegionSaver writes RegionData to cache format
  → Saves to data/cache/EDITOR/ (separate from live cache)
  → Status bar shows "Saved region 12850"
```

### Exporting to Server

```
User clicks "Export to Server"
  → Copies modified region from EDITOR to LIVE cache
  → Triggers cache rebuild (buildCache Gradle task) or manual instruction
  → Server loads updated region on next restart
```

### New Region

- Dialog: enter region ID (validates not already in use)
- Creates blank 64x64 region: grass overlay, height 0, all walkable
- Saved to EDITOR cache directory

---

## 8. Implementation Order

1. **Module setup** — Gradle module, JavaFX application skeleton, empty window with layout
2. **Data model** — RegionData, TileData, EditorState
3. **Cache loader** — Read region tile data from OSRS cache
4. **Terrain mesh** — Generate TriangleMesh from RegionData, display in SubScene
5. **Camera** — Orbit/pan/zoom controls
6. **Tile picker** — Ray cast to find tile under cursor, highlight
7. **Select tool + properties panel** — Click tile, show info, edit values
8. **Paint tool** — Paint overlays on tiles
9. **Height tool** — Raise/lower terrain
10. **Collision tool** — Toggle walkable/blocked
11. **Fill tool** — Flood fill overlay
12. **Undo/redo** — Command stack
13. **Cache saver** — Write region back to cache format
14. **New region** — Create blank regions

---

## 9. Key Dependencies

- **JavaFX 3D** — `javafx.scene.shape.TriangleMesh`, `MeshView`, `SubScene`, `PerspectiveCamera`
- **`:cache` module** — `dev.openrune.filesystem.Cache` for cache read/write
- **JDK 17+** — Already the project target
- **No additional external dependencies**

---

## 10. Out of Scope (Future Versions)

- **V2: Object placement** — Browse cache objects, place in region, rotate, delete
- **V2: NPC spawn editor** — Place NPC spawn points with walk radius visualization
- **V2: Multi-region** — Edit multiple adjacent regions, world map grid view
- **V2: Textures** — Load actual OSRS textures for overlays instead of solid colors
- **V2: Underlay editing** — Edit underlay types (base terrain under overlays)
- **V3: Region linking** — Define connections between regions, portals, transitions
- **V3: Content integration** — Place skilling spots, shops, quest markers directly in editor
