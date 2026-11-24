package org.rsmod.api.player.cinematic

import com.github.michaelbull.logging.InlineLogger
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

private val logger = InlineLogger()

public const val SceneSnapRate: Int = 232

public const val SceneDriftRate: Int = 10

public const val SceneCreepRate: Int = 5

/** Ticks to let a 50-client-cycle (1s) fade finish before closing the overlay. */
public const val SceneFadeTicks: Int = 2

public const val SceneFadeDuration: Int = 50

public class SceneCamera(
    public val eye: CoordGrid,
    public val eyeHeight: Int,
    public val lookAt: CoordGrid,
    public val lookAtHeight: Int,
    public val rate: Int = SceneSnapRate,
    public val rate2: Int = SceneSnapRate,
)

public class SceneRegion(public val region: Region?) {
    public val instanced: Boolean
        get() = region != null

    public operator fun get(coords: CoordGrid): CoordGrid =
        if (region == null) coords else region.normal[coords]

    public fun map(camera: SceneCamera): SceneCamera =
        if (region == null) {
            camera
        } else {
            SceneCamera(
                eye = this[camera.eye],
                eyeHeight = camera.eyeHeight,
                lookAt = this[camera.lookAt],
                lookAtHeight = camera.lookAtHeight,
                rate = camera.rate,
                rate2 = camera.rate2,
            )
        }
}

public suspend fun ProtectedAccess.lockedScene(
    vantage: CoordGrid,
    faceAt: CoordGrid,
    camera: SceneCamera,
    returnTo: CoordGrid? = null,
    underFade: suspend () -> Unit = {},
    teardown: () -> Unit = {},
    fadeOutDuration: Int = SceneFadeDuration,
    fadeInDuration: Int = SceneFadeDuration,
    exitCut: Boolean = false,
    body: suspend ProtectedAccess.() -> Unit,
) {
    try {
        beginScene(vantage, faceAt, camera, underFade, fadeOutDuration, fadeInDuration)
        body()
    } finally {
        endScene(returnTo, teardown, fadeOutDuration, fadeInDuration, exitCut)
    }
}

/**
 * Fades to black, hides the interface and teleports the player to [vantage].
 *
 * Kept separate from the camera setup so scenes can pace their opening around the client loading
 * the copied region, instead of moving the camera on the same tick as the teleport.
 */
public suspend fun ProtectedAccess.enterScene(
    vantage: CoordGrid,
    fadeOutDuration: Int = SceneFadeDuration,
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
}

public fun ProtectedAccess.applySceneCamera(camera: SceneCamera, faceAt: CoordGrid) {
    camMoveToV3(camera.eye, height = camera.eyeHeight, rate = camera.rate, rate2 = camera.rate2)
    camLookAtV3(
        camera.lookAt,
        height = camera.lookAtHeight,
        rate = camera.rate,
        rate2 = camera.rate2,
    )
    faceSquare(faceAt)
}

public suspend fun ProtectedAccess.beginScene(
    vantage: CoordGrid,
    faceAt: CoordGrid,
    camera: SceneCamera,
    underFade: suspend () -> Unit = {},
    fadeOutDuration: Int = SceneFadeDuration,
    fadeInDuration: Int = SceneFadeDuration,
) {
    enterScene(vantage, fadeOutDuration)
    applySceneCamera(camera, faceAt)

    underFade()

    delay(1)

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

/**
 * Plays a scene whose first frame is revealed through [reveal] instead of fading in.
 *
 * The opening is paced so the client has time to load the copied region before the camera moves:
 * the player is teleported on tick 0, [onEntered] runs on tick 1, the camera and [underFade] run on
 * tick 2, and [reveal] opens the overlay on tick 3.
 *
 * The fade overlay is intentionally left open on exit, so the caller can show any follow-up
 * dialogue over it and close it afterwards with [ProtectedAccess.closeFadeOverlayNow].
 */
public suspend fun ProtectedAccess.revealScene(
    vantage: CoordGrid,
    faceAt: CoordGrid,
    camera: SceneCamera,
    returnTo: CoordGrid? = null,
    onEntered: suspend ProtectedAccess.() -> Unit = {},
    underFade: suspend () -> Unit = {},
    teardown: () -> Unit = {},
    reveal: suspend ProtectedAccess.() -> Unit,
    fadeOutDuration: Int = SceneFadeDuration,
    fadeInDuration: Int = SceneFadeDuration,
    body: suspend ProtectedAccess.() -> Unit,
) {
    try {
        enterScene(vantage, fadeOutDuration)
        delay(1)
        onEntered()
        delay(1)
        applySceneCamera(camera, faceAt)
        underFade()
        delay(1)
        reveal()
        body()
    } finally {
        endScene(
            returnTo = returnTo,
            teardown = teardown,
            fadeOutDuration = fadeOutDuration,
            fadeInDuration = fadeInDuration,
            exitCut = true,
            closeFadeOverlay = false,
        )
    }
}

// Cancellation cleanup cannot suspend again; restore camera, HUD and movement synchronously on failure.
public suspend fun ProtectedAccess.endScene(
    returnTo: CoordGrid?,
    teardown: () -> Unit = {},
    fadeOutDuration: Int = SceneFadeDuration,
    fadeInDuration: Int = SceneFadeDuration,
    exitCut: Boolean = false,
    closeFadeOverlay: Boolean = true,
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

        if (exitCut) {
            delay(1)
        }
        restoreSceneState()

        fadeOverlay(
            startColour = 0,
            startTransparency = 0,
            endColour = 0,
            endTransparency = 255,
            clientDuration = fadeInDuration,
        )

        if (!closeFadeOverlay) {
            delay(1)
            return
        }

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

    showEntityOps()
    restoreLastAcceptAid()
    maxDrawDistance(enabled = false)

    minimapReset()
    camModeReset()
    showTopLevel()
    showHealthHud()
    camUnlock(unlock = false)
    camReset()
    openTopLevelTabs()

    syncDrawDistance()
}
