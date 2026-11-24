package org.rsmod.game.entity.util

/**
 * Defines how the client interpolates an entity's model transparency over time.
 *
 * @property startTime Start offset in 20 ms client cycles.
 * @property endTime End offset in 20 ms client cycles.
 * @property startTransparency Initial transparency from 0 through 255.
 * @property endTransparency Final transparency from 0 through 255.
 * @property useStartTransparency Whether to use the supplied start value instead of the
 *     model's current transparency. A negative start time always uses the supplied value.
 */
public data class EntityTransparency(
    public val startTime: Int,
    public val endTime: Int,
    public val startTransparency: Int,
    public val endTransparency: Int,
    public val useStartTransparency: Boolean = true,
) {
    init {
        require(startTime in -32768..32767 && endTime in -32768..32767)
        require(startTransparency in 0..255 && endTransparency in 0..255)
    }
}

/**
 * Schedules an animation freeze on the client while server movement and timers run normally.
 *
 * @property delay Delay before the freeze in 20 ms client cycles.
 * @property duration Length of the freeze in 20 ms client cycles.
 * @property cancelSequence Whether to cancel the sequence when applying the freeze.
 */
public data class EntityAnimationFreeze(
    public val delay: Int,
    public val duration: Int,
    public val cancelSequence: Boolean = false,
) {
    init {
        require(delay in 0..65535 && duration in 0..65535)
    }
}
