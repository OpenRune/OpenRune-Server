package org.rsmod.api.player.output

import net.rsprot.protocol.game.outgoing.specific.AnimSpecific
import net.rsprot.protocol.game.outgoing.specific.MapAnimSpecific
import net.rsprot.protocol.game.outgoing.specific.ProjAnimSpecificV4
import org.rsmod.game.entity.Player
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid

public fun Player.spotanimMapPrivate(spotanim: Int, coords: CoordGrid, height: Int, delay: Int = 0) {
    val x = coords.x - buildArea.x
    val z = coords.z - buildArea.z
    if (x !in 0 until BUILD_AREA_SIZE || z !in 0 until BUILD_AREA_SIZE) {
        return
    }
    client.write(MapAnimSpecific(spotanim, delay, height, x, z))
}

private const val BUILD_AREA_SIZE = 104

/**
 * Plays an animation on the local player for this client. Other players see the normal
 * player-info animation state.
 *
 * @param sequence Sequence ID, or `-1` to clear the current animation.
 * @param delay Delay before playback in 20 ms client cycles.
 */
public fun Player.animSpecific(sequence: Int, delay: Int = 0) {
    require(sequence in -1..65534 && delay in 0..255)
    client.write(AnimSpecific(sequence, delay))
}

/**
 * Sends a visual projectile to this client using the absolute start and end coordinates
 * in [projectile], including the plane at each end.
 *
 * Use the shared projectile path when nearby players should see the effect.
 */
public fun Player.projAnimSpecific(projectile: ProjAnim) {
    client.write(
        ProjAnimSpecificV4(
            id = projectile.spotanim,
            startHeight = projectile.startHeight,
            endHeight = projectile.endHeight,
            startTime = projectile.startTime,
            endTime = projectile.endTime,
            angle = projectile.angle,
            progress = projectile.progress,
            startX = projectile.startCoord.x,
            startZ = projectile.startCoord.z,
            startLevel = projectile.startCoord.level,
            sourceIndex = projectile.sourceIndex,
            endX = projectile.endCoord.x,
            endZ = projectile.endCoord.z,
            endLevel = projectile.endCoord.level,
            targetIndex = projectile.targetIndex,
        ),
    )
}
