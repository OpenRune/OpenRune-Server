package org.rsmod.api.player.output

import net.rsprot.protocol.game.outgoing.camera.CamLookAtCycles
import net.rsprot.protocol.game.outgoing.camera.CamLookAtV3
import net.rsprot.protocol.game.outgoing.camera.CamMoveToArcV3
import net.rsprot.protocol.game.outgoing.camera.CamMoveToCyclesV3
import net.rsprot.protocol.game.outgoing.camera.CamMoveToV3
import net.rsprot.protocol.game.outgoing.camera.CamReset
import net.rsprot.protocol.game.outgoing.camera.CamRotateToCoordinateV3
import net.rsprot.protocol.game.outgoing.camera.CamShake
import net.rsprot.protocol.game.outgoing.camera.CamSkybox
import net.rsprot.protocol.game.outgoing.camera.CamTargetV4
import net.rsprot.protocol.game.outgoing.camera.CamUnlock
import net.rsprot.protocol.game.outgoing.camera.util.CameraEaseFunction
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

/**
 * The shake axes the client understands, as documented on [CamShake].
 *
 * ```
 * | Id |  Type   |    Observed Movement   |
 * |----|:-------:|:----------------------:|
 * | 0  | X-axis  |     Left and right     |
 * | 1  | Y-axis  |       Up and down      |
 * | 2  | Z-axis  | Forwards and backwards |
 * | 3  | Y-angle | Panning left and right |
 * | 4  | X-angle |   Panning up and down  |
 * ```
 */
public enum class CamShakeAxis(public val id: Int) {
    LEFT_RIGHT(0),
    UP_DOWN(1),
    FORWARDS_BACKWARDS(2),
    PAN_LEFT_RIGHT(3),
    PAN_UP_DOWN(4),
}

public object Camera {
    public fun camReset(player: Player) {
        player.client.write(CamReset)
    }

    public fun camLookAt(
        player: Player,
        dest: CoordGrid,
        height: Int,
        rate: Int,
        rate2: Int,
        heightRelative: Boolean = false,
    ): Unit = camLookAtV3(player, dest, height, rate, rate2, heightRelative)

    public fun camMoveTo(
        player: Player,
        dest: CoordGrid,
        height: Int,
        rate: Int,
        rate2: Int,
        heightRelative: Boolean = false,
    ): Unit = camMoveToV3(player, dest, height, rate, rate2, heightRelative)

    /**
     * [heightRelative] applies [height] relative to the previous look-at height instead of treating
     * it as absolute.
     */
    public fun camLookAtV3(
        player: Player,
        dest: CoordGrid,
        height: Int,
        rate: Int,
        rate2: Int,
        heightRelative: Boolean = false,
    ) {
        require(height in -32768..32767) {
            "`height` must be within range [-32768..32767]. (height=$height)"
        }
        require(rate in 0..255) { "`rate` must be within range [0..255]. (rate=$rate)" }
        require(rate2 in 0..255) { "`rate2` must be within range [0..255]. (rate2=$rate2)" }
        player.client.write(CamLookAtV3(dest.x, dest.z, height, rate, rate2, heightRelative))
    }

    /**
     * [heightRelative] applies [height] relative to the previous move-to height instead of treating
     * it as absolute.
     */
    public fun camMoveToV3(
        player: Player,
        dest: CoordGrid,
        height: Int,
        rate: Int,
        rate2: Int,
        heightRelative: Boolean = false,
    ) {
        require(height in -32768..32767) {
            "`height` must be within range [-32768..32767]. (height=$height)"
        }
        require(rate in 0..255) { "`rate` must be within range [0..255]. (rate=$rate)" }
        require(rate2 in 0..255) { "`rate2` must be within range [0..255]. (rate2=$rate2)" }
        player.client.write(CamMoveToV3(dest.x, dest.z, height, rate, rate2, heightRelative))
    }

    /**
     * Moves the camera to the absolute tile [dest] along a straight path.
     *
     * @param cycles Duration in 20 ms client cycles.
     * @param easing Curve that controls progress along the path.
     * @param ignoreTerrain Whether to ignore terrain changes during the move.
     * @param heightRelative Whether [height] is relative to the previous move-to height.
     */
    public fun camMoveToCycles(
        player: Player,
        dest: CoordGrid,
        height: Int,
        cycles: Int,
        easing: CameraEaseFunction,
        ignoreTerrain: Boolean = false,
        heightRelative: Boolean = false,
    ) {
        validateTween(height, cycles)
        player.client.write(
            CamMoveToCyclesV3(dest.x, dest.z, height, cycles, ignoreTerrain, easing.id, heightRelative),
        )
    }

    /**
     * Moves the camera along an arc through [center] before it reaches [dest].
     *
     * Both positions use absolute tile coordinates. [easing] controls progress along the arc
     * over [cycles] client cycles of 20 ms each.
     *
     * @param ignoreTerrain Whether to ignore terrain changes during the move.
     * @param heightRelative Whether [height] is relative to the previous move-to height.
     */
    public fun camMoveToArc(
        player: Player,
        center: CoordGrid,
        dest: CoordGrid,
        height: Int,
        cycles: Int,
        easing: CameraEaseFunction,
        ignoreTerrain: Boolean = false,
        heightRelative: Boolean = false,
    ) {
        validateTween(height, cycles)
        player.client.write(
            CamMoveToArcV3(
                center.x, center.z, dest.x, dest.z, height, cycles, ignoreTerrain, easing.id,
                heightRelative,
            ),
        )
    }

    /**
     * Moves the camera's look-at point toward the absolute tile [dest] while leaving the camera
     * position in place. [easing] controls progress over [cycles] client cycles of 20 ms each.
     *
     * @param heightRelative Whether [height] is relative to the previous look-at height.
     */
    public fun camLookAtCycles(
        player: Player,
        dest: CoordGrid,
        height: Int,
        cycles: Int,
        easing: CameraEaseFunction,
        heightRelative: Boolean = false,
    ) {
        validateTween(height, cycles)
        player.client.write(CamLookAtCycles(dest.x, dest.z, height, cycles, easing.id, heightRelative))
    }

    /**
     * Rotates the camera toward the absolute tile [dest] using [easing] over [cycles] client
     * cycles of 20 ms each.
     *
     * @param heightRelative Whether [height] is relative to the previous look-at height.
     * @param trackTarget Whether the client keeps tracking [dest] as its position on screen changes.
     */
    public fun camRotateToCoordinate(
        player: Player,
        dest: CoordGrid,
        height: Int,
        cycles: Int,
        easing: CameraEaseFunction,
        heightRelative: Boolean = false,
        trackTarget: Boolean = false,
    ) {
        validateTween(height, cycles)
        player.client.write(
            CamRotateToCoordinateV3(dest.x, dest.z, height, cycles, easing.id, heightRelative, trackTarget),
        )
    }

    /**
     * Focuses the camera on a tile, player, NPC, or world entity using the client's active world.
     *
     * When the client cannot find the entity identified by [target], it focuses on the local
     * player instead.
     */
    public fun camTarget(player: Player, target: CamTargetV4.CamTargetType) {
        player.client.write(CamTargetV4(target))
    }

    /**
     * Sends [model] as the model to render for the scene's skybox.
     *
     * Rendering depends on client support. The native client implements the skybox behaviour.
     */
    public fun camSkybox(player: Player, model: Int) {
        player.client.write(CamSkybox(model))
    }

    private fun validateTween(height: Int, cycles: Int) {
        require(height in Short.MIN_VALUE..Short.MAX_VALUE) { "Invalid camera height: $height" }
        require(cycles in 0..65535) { "Invalid camera cycle count: $cycles" }
    }

    /**
     * Starts or replaces the shake on [axis]. Other axes continue independently until cleared
     * with [camShakeReset] or [camShakeResetAll].
     *
     * @param random Maximum random displacement.
     * @param amplitude Amplitude of the periodic shake.
     * @param rate Rate of the periodic shake.
     */
    public fun camShake(
        player: Player,
        axis: CamShakeAxis,
        random: Int,
        amplitude: Int,
        rate: Int,
    ) {
        require(random in 0..255) { "`random` must be within range [0..255]. (random=$random)" }
        require(amplitude in 0..255) {
            "`amplitude` must be within range [0..255]. (amplitude=$amplitude)"
        }
        require(rate in 0..255) { "`rate` must be within range [0..255]. (rate=$rate)" }
        player.client.write(CamShake(axis.id, random, amplitude, rate))
    }

    public fun camShakeReset(player: Player, axis: CamShakeAxis) {
        camShake(player, axis, random = 0, amplitude = 0, rate = 0)
    }

    public fun camShakeResetAll(player: Player) {
        for (axis in CamShakeAxis.entries) {
            camShakeReset(player, axis)
        }
    }

    /**
     * Unlocks (`true`) or restores (`false`) the camera's min and max pitch angle. While unlocked
     * the camera can be pitched fully vertical, or into the ground.
     *
     * The live server sends `unlock = false` on cutscene exit, immediately before its two
     * [camReset] calls.
     *
     * @see [CamUnlock]
     */
    public fun camUnlock(player: Player, unlock: Boolean) {
        player.client.write(CamUnlock(unlock))
    }
}
