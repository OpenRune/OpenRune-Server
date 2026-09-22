package org.rsmod.api.player.output

import net.rsprot.protocol.game.outgoing.camera.CamLookAtV2
import net.rsprot.protocol.game.outgoing.camera.CamLookAtV3
import net.rsprot.protocol.game.outgoing.camera.CamMoveToV2
import net.rsprot.protocol.game.outgoing.camera.CamMoveToV3
import net.rsprot.protocol.game.outgoing.camera.CamReset
import net.rsprot.protocol.game.outgoing.camera.CamShake
import net.rsprot.protocol.game.outgoing.camera.CamUnlock
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

public object Camera {
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
    public val shakeAxes: IntRange = 0..4

    public fun camReset(player: Player) {
        player.client.write(CamReset)
    }

    public fun camShake(player: Player, axis: Int, random: Int, amplitude: Int, rate: Int) {
        player.client.write(CamShake(axis, random, amplitude, rate))
    }

    public fun camLookAt(player: Player, dest: CoordGrid, height: Int, rate: Int, rate2: Int) {
        player.client.write(CamLookAtV2(dest.x, dest.z, height, rate, rate2))
    }

    public fun camMoveTo(player: Player, dest: CoordGrid, height: Int, rate: Int, rate2: Int) {
        player.client.write(CamMoveToV2(dest.x, dest.z, height, rate, rate2))
    }

    /**
     * V3 encodes signed height and can apply it relative to the previous look-at height; V2 is
     * unsigned and absolute.
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
     * V3 encodes signed height and can apply it relative to the previous move-to height; V2 is
     * unsigned and absolute.
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

    /** Each axis stacks independently and keeps shaking until explicitly reset. */
    public fun camShake(player: Player, axis: Int, random: Int, amplitude: Int, rate: Int) {
        require(axis in shakeAxes) { "`axis` must be within range [0..4]. (axis=$axis)" }
        require(random in 0..255) { "`random` must be within range [0..255]. (random=$random)" }
        require(amplitude in 0..255) {
            "`amplitude` must be within range [0..255]. (amplitude=$amplitude)"
        }
        require(rate in 0..255) { "`rate` must be within range [0..255]. (rate=$rate)" }
        player.client.write(CamShake(axis, random, amplitude, rate))
    }

    public fun camShakeReset(player: Player, axis: Int) {
        camShake(player, axis, random = 0, amplitude = 0, rate = 0)
    }

    public fun camShakeResetAll(player: Player) {
        for (axis in shakeAxes) {
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
