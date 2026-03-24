package org.alter.editor.viewport

import javafx.scene.Group
import javafx.scene.PerspectiveCamera
import javafx.scene.SubScene
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.transform.Rotate
import kotlin.math.cos
import kotlin.math.sin

/**
 * Orbit camera controller with mouse controls.
 *
 * - **Right-click drag**: orbit (pitch / yaw)
 * - **Scroll wheel**: zoom in / out
 * - **Middle-click drag**: pan
 */
class CameraController(
    private val camera: PerspectiveCamera,
    private val pivot: Group,
) {
    // Orbit state
    private var angleX = 45.0   // pitch (degrees)
    private var angleY = 0.0    // yaw   (degrees)
    private var distance = 80.0 // zoom distance

    // Pan state — default to center of a 64-tile region
    private var panX = 32.0
    private var panZ = 32.0

    // Transforms applied to the pivot
    private val rotateX = Rotate(angleX, Rotate.X_AXIS)
    private val rotateY = Rotate(angleY, Rotate.Y_AXIS)

    // Drag tracking
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0

    init {
        pivot.transforms.setAll(rotateY, rotateX)
        camera.translateZ = -distance
        updateCamera()
    }

    /** Register mouse / scroll handlers on the given [SubScene]. */
    fun attachTo(scene: SubScene) {
        scene.addEventHandler(MouseEvent.MOUSE_PRESSED) { onMousePressed(it) }
        scene.addEventHandler(MouseEvent.MOUSE_DRAGGED) { onMouseDragged(it) }
        scene.addEventHandler(ScrollEvent.SCROLL) { onScroll(it) }
    }

    // ----- event handlers --------------------------------------------------

    private fun onMousePressed(e: MouseEvent) {
        lastMouseX = e.sceneX
        lastMouseY = e.sceneY
    }

    private fun onMouseDragged(e: MouseEvent) {
        val dx = e.sceneX - lastMouseX
        val dy = e.sceneY - lastMouseY
        lastMouseX = e.sceneX
        lastMouseY = e.sceneY

        when (e.button) {
            MouseButton.SECONDARY -> {
                // Orbit
                angleY += dx * ORBIT_SPEED
                angleX = (angleX - dy * ORBIT_SPEED).coerceIn(5.0, 89.0)
                updateCamera()
            }
            MouseButton.MIDDLE -> {
                // Pan — move along the ground plane relative to current yaw
                val radY = Math.toRadians(angleY)
                panX += (dx * cos(radY) + dy * sin(radY)) * PAN_SPEED
                panZ += (-dx * sin(radY) + dy * cos(radY)) * PAN_SPEED
                updateCamera()
            }
            else -> { /* ignore left-click drag here; handled by tile picking */ }
        }
    }

    private fun onScroll(e: ScrollEvent) {
        distance = (distance - e.deltaY * ZOOM_SPEED).coerceIn(MIN_ZOOM, MAX_ZOOM)
        updateCamera()
    }

    // ----- camera update ---------------------------------------------------

    private fun updateCamera() {
        pivot.translateX = panX
        pivot.translateZ = panZ

        rotateX.angle = angleX
        rotateY.angle = angleY

        camera.translateZ = -distance
    }

    companion object {
        private const val ORBIT_SPEED = 0.35
        private const val PAN_SPEED = 0.25
        private const val ZOOM_SPEED = 0.5
        private const val MIN_ZOOM = 20.0
        private const val MAX_ZOOM = 300.0
    }
}
