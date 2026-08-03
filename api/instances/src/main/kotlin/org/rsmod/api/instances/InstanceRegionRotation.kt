package org.rsmod.api.instances

public enum class InstanceRegionRotation(public val steps: Int, public val degrees: Int) {
    NONE(steps = 0, degrees = 0),
    CLOCKWISE_90(steps = 1, degrees = 90),
    CLOCKWISE_180(steps = 2, degrees = 180),
    CLOCKWISE_270(steps = 3, degrees = 270),
    ;

    init {
        require(steps in 0..3) { "steps must be within [0..3]. (steps=$steps)" }
    }

    public companion object {
        public fun fromDegrees(degrees: Int): InstanceRegionRotation =
            when (degrees) {
                0 -> NONE
                90 -> CLOCKWISE_90
                180 -> CLOCKWISE_180
                270 -> CLOCKWISE_270
                else ->
                    error(
                        "Instance rotation must be 0°, 90°, 180°, or 270° clockwise. (degrees=$degrees)",
                    )
            }

        public fun fromSteps(steps: Int): InstanceRegionRotation =
            entries.firstOrNull { it.steps == steps }
                ?: error("Instance rotation steps must be within [0..3]. (steps=$steps)")
    }
}
