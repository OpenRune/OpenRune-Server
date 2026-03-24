package org.alter.editor.viewport

import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.CullFace
import javafx.scene.shape.DrawMode
import javafx.scene.shape.MeshView
import javafx.scene.shape.TriangleMesh
import javafx.scene.shape.VertexFormat
import org.alter.editor.model.RegionData

/**
 * Builds a JavaFX [TriangleMesh] + [MeshView] from a [RegionData] for a given height level.
 *
 * The vertex grid is 65x65 (shared edges between tiles). Each tile produces 2 triangles
 * colored according to its overlay ID via a small texture atlas.
 */
class TerrainMesh(
    private val regionData: RegionData,
    private val heightLevel: Int = 0,
) {
    val meshView: MeshView = buildMesh()

    /** Rebuild the mesh in-place from current regionData. */
    fun rebuild() {
        val mesh = buildTriangleMesh()
        meshView.mesh = mesh
        meshView.material = buildMaterial()
    }

    // ----- internals -------------------------------------------------------

    private fun buildMesh(): MeshView {
        val view = MeshView(buildTriangleMesh())
        view.material = buildMaterial()
        view.cullFace = CullFace.NONE  // Disable culling to ensure visibility regardless of winding
        view.drawMode = DrawMode.FILL
        return view
    }

    /**
     * Construct the triangle mesh.
     *
     * Vertices: 65x65 grid (indices row-major: index = z * 65 + x).
     * Texture coords: one UV per color slot in the atlas (ATLAS_SIZE entries).
     * Faces: 64x64 tiles x 2 triangles = 8192 faces.
     */
    private fun buildTriangleMesh(): TriangleMesh {
        val mesh = TriangleMesh(VertexFormat.POINT_TEXCOORD)
        val size = RegionData.SIZE // 64

        // --- Points (65 * 65 * 3 floats) ---
        val points = FloatArray(65 * 65 * 3)
        for (z in 0..size) {
            for (x in 0..size) {
                // Use the height of the nearest valid tile (clamp to 0..63)
                val tx = x.coerceAtMost(size - 1)
                val tz = z.coerceAtMost(size - 1)
                val tile = regionData.getTile(heightLevel, tx, tz)
                val y = -(tile.height / 255.0) * MAX_HEIGHT_VISUAL

                val idx = (z * 65 + x) * 3
                points[idx] = x.toFloat()
                points[idx + 1] = y.toFloat()
                points[idx + 2] = z.toFloat()
            }
        }
        mesh.points.setAll(*points)

        // --- Texture coordinates (one UV per atlas slot, centered in each slot's pixel range) ---
        val texCoords = FloatArray(ATLAS_SIZE * 2)
        for (i in 0 until ATLAS_SIZE) {
            // Center of this slot's pixel range in the 256-wide atlas
            val centerPixel = ((i * 256) / ATLAS_SIZE + ((i + 1) * 256) / ATLAS_SIZE) / 2.0f
            texCoords[i * 2] = centerPixel / 256.0f
            texCoords[i * 2 + 1] = 0.5f
        }
        mesh.texCoords.setAll(*texCoords)

        // --- Faces (64 * 64 * 2 triangles, 6 ints each) ---
        val faces = IntArray(size * size * 2 * 6)
        var fi = 0
        for (z in 0 until size) {
            for (x in 0 until size) {
                val tile = regionData.getTile(heightLevel, x, z)
                val colorIdx = overlayColorIndex(tile.overlayId, tile.isBlocked)

                // Vertex indices (row-major in 65-wide grid)
                val v00 = z * 65 + x         // (x,   z)
                val v10 = z * 65 + x + 1     // (x+1, z)
                val v01 = (z + 1) * 65 + x   // (x,   z+1)
                val v11 = (z + 1) * 65 + x + 1 // (x+1, z+1)

                // Triangle 1: v00, v10, v01
                faces[fi++] = v00; faces[fi++] = colorIdx
                faces[fi++] = v10; faces[fi++] = colorIdx
                faces[fi++] = v01; faces[fi++] = colorIdx

                // Triangle 2: v10, v11, v01
                faces[fi++] = v10; faces[fi++] = colorIdx
                faces[fi++] = v11; faces[fi++] = colorIdx
                faces[fi++] = v01; faces[fi++] = colorIdx
            }
        }
        mesh.faces.setAll(*faces)

        return mesh
    }

    /** Build a [PhongMaterial] with a power-of-2 texture atlas for per-tile coloring. */
    private fun buildMaterial(): PhongMaterial {
        // Use a 256x1 texture (power-of-2 for GPU compatibility)
        val atlasWidth = 256
        val image = WritableImage(atlasWidth, 1)
        val pw = image.pixelWriter
        for (i in 0 until ATLAS_SIZE) {
            // Spread each color across multiple pixels for sampling stability
            val startPixel = (i * atlasWidth) / ATLAS_SIZE
            val endPixel = ((i + 1) * atlasWidth) / ATLAS_SIZE
            for (p in startPixel until endPixel) {
                pw.setColor(p, 0, ATLAS_COLORS[i])
            }
        }
        val mat = PhongMaterial()
        mat.diffuseMap = image
        // Also set self-illumination so colors show even without strong lighting
        mat.selfIlluminationMap = image
        return mat
    }

    // ----- color mapping ---------------------------------------------------

    fun getOverlayColor(overlayId: Short): Color {
        return OVERLAY_COLOR_MAP.getOrDefault(overlayId.toInt(), DEFAULT_COLOR)
    }

    /**
     * Returns the atlas index for a given overlay, optionally tinted for blocked tiles.
     */
    private fun overlayColorIndex(overlayId: Short, blocked: Boolean): Int {
        if (blocked) return BLOCKED_INDEX
        return OVERLAY_INDEX_MAP.getOrDefault(overlayId.toInt(), DEFAULT_INDEX)
    }

    companion object {
        /** Maximum visual height in world units. */
        const val MAX_HEIGHT_VISUAL = 10.0

        // Atlas layout: 16 color slots
        const val ATLAS_SIZE = 16

        // Named indices
        private const val DEFAULT_INDEX = 0   // dark green (no overlay / default grass)
        private const val GRASS_INDEX = 1
        private const val DIRT_INDEX = 2
        private const val STONE_INDEX = 3
        private const val WATER_INDEX = 4
        private const val SAND_INDEX = 5
        private const val BLOCKED_INDEX = 6

        private val DEFAULT_COLOR = Color.web("#2d5a1e")

        /** Colors stored in the atlas, indexed by slot. */
        val ATLAS_COLORS: Array<Color> = arrayOf(
            Color.web("#2d5a1e"),  // 0  default / no overlay — dark green
            Color.web("#3a7a28"),  // 1  grass — green
            Color.web("#8b6b3e"),  // 2  dirt — brown
            Color.web("#808080"),  // 3  stone — gray
            Color.web("#2255aa"),  // 4  water — blue
            Color.web("#d4b84f"),  // 5  sand — yellow
            Color.web("#7a2020"),  // 6  blocked — red-tinted
            Color.web("#2d5a1e"),  // 7+ reserved — default green
            Color.web("#2d5a1e"),
            Color.web("#2d5a1e"),
            Color.web("#2d5a1e"),
            Color.web("#2d5a1e"),
            Color.web("#2d5a1e"),
            Color.web("#2d5a1e"),
            Color.web("#2d5a1e"),
            Color.web("#2d5a1e"),
        )

        /**
         * Maps overlay IDs to atlas indices. Unknown overlays fall back to [DEFAULT_INDEX].
         * These are representative groupings; real cache overlay IDs would be mapped more
         * precisely once cache loading is implemented.
         */
        private val OVERLAY_INDEX_MAP: Map<Int, Int> = mapOf(
            0 to DEFAULT_INDEX,
            1 to GRASS_INDEX,
            2 to DIRT_INDEX,
            3 to STONE_INDEX,
            4 to WATER_INDEX,
            5 to SAND_INDEX,
        )

        private val OVERLAY_COLOR_MAP: Map<Int, Color> = mapOf(
            0 to Color.web("#2d5a1e"),
            1 to Color.web("#3a7a28"),
            2 to Color.web("#8b6b3e"),
            3 to Color.web("#808080"),
            4 to Color.web("#2255aa"),
            5 to Color.web("#d4b84f"),
        )
    }
}
