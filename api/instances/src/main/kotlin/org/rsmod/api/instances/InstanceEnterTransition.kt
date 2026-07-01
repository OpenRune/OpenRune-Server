package org.rsmod.api.instances

import org.rsmod.api.player.protect.ProtectedAccess

public data class InstanceEnterTransition(
    val message: String? = null,
    val fadeInSeconds: Int = 4,
    val minimapHideAfterSeconds: Int = 1,
    val fadeOutSeconds: Int = 2,
    val closeOverlayCycles: Int = 3,
)

/**
 * Runs a minimal enter cinematic: optional mesbox, fade to black, hide minimap, then [block]
 * (typically telejump + [InstanceManager.finalizeEntry]).
 */
public suspend fun ProtectedAccess.withInstanceEnterTransition(
    transition: InstanceEnterTransition = InstanceEnterTransition(),
    block: suspend ProtectedAccess.() -> Unit,
) {
    transition.message?.let { mesboxNp(it) }

    val fadeClientDuration = 80

    fadeOverlay(
        startColour = 0,
        startTransparency = 0,
        endColour = 0,
        endTransparency = 255,
        clientDuration = fadeClientDuration,
    )

    block()

    minimapHideMap()
    delay(clientDurationToCycles(fadeClientDuration - 30))
    minimapReset()
}

/** Same fade/minimap flow as [withInstanceEnterTransition]; [block] is typically [InstanceScript.defaultLeaveFlow]. */
public suspend fun ProtectedAccess.withInstanceLeaveTransition(
    transition: InstanceEnterTransition = InstanceEnterTransition(),
    block: suspend ProtectedAccess.() -> Unit,
) {
    transition.message?.let { mesboxNp(it) }

    val fadeClientDuration = 80

    fadeOverlay(
        startColour = 0,
        startTransparency = 0,
        endColour = 0,
        endTransparency = 255,
        clientDuration = fadeClientDuration,
    )

    block()

    minimapHideMap()
    minimapReset()
}

private const val MS_PER_CYCLE: Int = 600

private const val CLIENT_DURATION_PER_SECOND: Int = 100

private fun clientDurationToCycles(clientDuration: Int): Int {
    val durationMs = clientDuration * 1000 / CLIENT_DURATION_PER_SECOND
    return maxOf(1, (durationMs + MS_PER_CYCLE - 1) / MS_PER_CYCLE)
}
