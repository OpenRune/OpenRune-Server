package org.rsmod.content.quest.area.misthalin

import com.github.michaelbull.logging.InlineLogger
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.map.CoordGrid

private val logger = InlineLogger()

internal class SceneCamera(
    val eye: CoordGrid,
    val eyeHeight: Int,
    val lookAt: CoordGrid,
    val lookAtHeight: Int,
)

internal const val SceneSnapRate = 232

internal const val SceneDriftRate = 10

internal const val SceneCreepRate = 5

/** Ticks to let a 50-client-cycle (1s) fade finish before closing the overlay. */
internal const val SceneFadeTicks = 2

internal const val SceneFadeDuration = 50

internal suspend fun ProtectedAccess.lockedScene(
    vantage: CoordGrid,
    faceAt: CoordGrid,
    camera: SceneCamera,
    returnTo: CoordGrid? = null,
    underFade: suspend () -> Unit = {},
    teardown: () -> Unit = {},
    fadeOutDuration: Int = SceneFadeDuration,
    fadeInDuration: Int = SceneFadeDuration,
    reveal: (suspend ProtectedAccess.() -> Unit)? = null,
    exitCut: Boolean = false,
    body: suspend ProtectedAccess.() -> Unit,
) {
    try {
        beginScene(vantage, faceAt, camera, underFade, fadeOutDuration, fadeInDuration, reveal)
        body()
    } finally {
        endScene(returnTo, teardown, fadeOutDuration, fadeInDuration, exitCut)
    }
}

internal suspend fun ProtectedAccess.beginScene(
    vantage: CoordGrid,
    faceAt: CoordGrid,
    camera: SceneCamera,
    underFade: suspend () -> Unit = {},
    fadeOutDuration: Int = SceneFadeDuration,
    fadeInDuration: Int = SceneFadeDuration,
    reveal: (suspend ProtectedAccess.() -> Unit)? = null,
) {
    fadeOverlay(
        startColour = 0,
        startTransparency = 255,
        endColour = 0,
        endTransparency = 0,
        clientDuration = fadeOutDuration,
    )
    delay(2)

    hideEntityOps()
    hideTopLevel()
    hideHealthHud()
    clearHealthHud()

    minimapHideMap()
    camModeClose()

    maxDrawDistance(enabled = true)

    tempDisableAcceptAid()
    closeTopLevelTabsLenient()

    // Run viewport-dependent camera scripts after closing the top-level tabs.
    syncDrawDistance()

    camShakeResetAll()

    telejump(vantage)
    rebuildAppearance()

    camMoveToV3(camera.eye, height = camera.eyeHeight, rate = SceneSnapRate, rate2 = SceneSnapRate)
    camLookAtV3(
        camera.lookAt,
        height = camera.lookAtHeight,
        rate = SceneSnapRate,
        rate2 = SceneSnapRate,
    )
    faceSquare(faceAt)

    underFade()

    delay(1)

    if (reveal != null) {
        delay(SceneFadeTicks)
        reveal()
        return
    }

    fadeOverlay(
        startColour = 0,
        startTransparency = 0,
        endColour = 0,
        endTransparency = 255,
        clientDuration = fadeInDuration,
    )

    // Wait for the fade before closing the overlay; closing early cancels it.
    delay(SceneFadeTicks)
    closeFadeOverlayNow()
}

// Cancellation cleanup cannot suspend again; restore camera, HUD and movement synchronously on failure.
internal suspend fun ProtectedAccess.endScene(
    returnTo: CoordGrid?,
    teardown: () -> Unit = {},
    fadeOutDuration: Int = SceneFadeDuration,
    fadeInDuration: Int = SceneFadeDuration,
    exitCut: Boolean = false,
) {
    var teardownRan = false
    fun runTeardown() {
        if (teardownRan) {
            return
        }
        teardownRan = true
        try {
            teardown()
        } catch (t: Throwable) {
            logger.error(t) { "Scene teardown failed; continuing with scene restore." }
        }
    }
    try {
        fadeOverlay(
            startColour = 0,
            startTransparency = if (exitCut) 0 else 255,
            endColour = 0,
            endTransparency = 0,
            clientDuration = if (exitCut) 0 else fadeOutDuration,
        )
        delay(if (exitCut) 1 else SceneFadeTicks)

        runTeardown()

        if (returnTo != null) {
            telejump(returnTo)
        }
        rebuildAppearance()
        restoreSceneState()

        if (exitCut) {
            delay(1)
        }

        fadeOverlay(
            startColour = 0,
            startTransparency = 0,
            endColour = 0,
            endTransparency = 255,
            clientDuration = fadeInDuration,
        )

        delay(SceneFadeTicks)
        closeFadeOverlayNow()
    } catch (cancelled: Throwable) {
        runTeardown()
        if (returnTo != null) {
            telejump(returnTo)
        }
        rebuildAppearance()
        restoreSceneState()
        closeFadeOverlayNow()
        throw cancelled
    }
}

private fun ProtectedAccess.restoreSceneState() {
    ifClose()
    camUnlock(unlock = false)
    camReset()

    showEntityOps()
    restoreLastAcceptAid()
    maxDrawDistance(enabled = false)

    minimapReset()
    camModeReset()
    showTopLevel()
    showHealthHud()
    openTopLevelTabs()

    syncDrawDistance()
}
