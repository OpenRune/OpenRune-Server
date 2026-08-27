package org.rsmod.content.skills.sailing

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Singleton
class BoatKinematics @Inject constructor(private val boats: BoatManager) {
    fun tick() {
        for (boat in boats.all) {
            step(boat)
        }
    }

    private fun step(boat: Boat) {
        val accel = boat.type.acceleration
        if (boat.speed < boat.targetSpeed) {
            boat.speed = min(boat.speed + accel, boat.targetSpeed)
        } else if (boat.speed > boat.targetSpeed) {
            boat.speed = max(boat.speed - accel, boat.targetSpeed)
        }

        val entity = boat.entity
        val diff = ((boat.targetAngle - entity.angle + HALF_ANGLE) and ANGLE_MASK) - HALF_ANGLE
        if (diff != 0) {
            val turn = diff.coerceIn(-TURN_RATE_PER_TICK, TURN_RATE_PER_TICK)
            entity.updateAngle((entity.angle + turn) and ANGLE_MASK)
        }

        if (boat.speed != 0) {
            val theta = entity.angle * TWO_PI / ANGLE_FULL
            val dx = quantize(boat.speed * -sin(theta))
            val dz = quantize(boat.speed * -cos(theta))
            if (dx != 0 || dz != 0) {
                entity.updateCoord(
                    level = entity.projectedLevel,
                    fineX = entity.fineX + dx,
                    fineZ = entity.fineZ + dz,
                    teleport = false,
                )
            }
        }
    }

    private fun quantize(v: Double): Int =
        if (v >= 0) {
            (v / VELOCITY_QUANTUM + 0.5).toInt() * VELOCITY_QUANTUM
        } else {
            -((-v / VELOCITY_QUANTUM + 0.5).toInt() * VELOCITY_QUANTUM)
        }

    private companion object {
        private const val ANGLE_FULL = 2048
        private const val ANGLE_MASK = ANGLE_FULL - 1
        private const val HALF_ANGLE = ANGLE_FULL / 2
        private const val TURN_RATE_PER_TICK = 128
        private const val VELOCITY_QUANTUM = 32
        private const val TWO_PI = 2.0 * PI
    }
}
