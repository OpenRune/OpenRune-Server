package org.rsmod.content.bosses.gemstonecrab

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.loc.BoundLocInfo

@Singleton
public class GemstoneCrabCaveScript @Inject constructor(private val crab: GemstoneCrabManager) {

    public suspend fun ProtectedAccess.crawl(cave: BoundLocInfo) {
        val exit = crab.caveExitFor(cave.coords)
        if (exit == null) {
            mes("The gemstone crab is already nearby.")
            return
        }

        faceLoc(cave)
        anim("seq.human_longcrawl_priority")
        soundSynth("synth.raft_climb", loops = 3, delay = 4)
        fadeOverlay(
            startColour = 0,
            startTransparency = 255,
            endColour = 0,
            endTransparency = 0,
            clientDuration = FADE_CLIENT_DURATION,
        )
        clearHealthHud()
        crab.releaseBar(player)

        delay(1)
        minimapHideMap()
        delay(1)
        telejump(exit)

        delay(2)
        minimapReset()
        fadeOverlay(
            startColour = 0,
            startTransparency = 0,
            endColour = 0,
            endTransparency = 255,
            clientDuration = FADE_CLIENT_DURATION,
        )
        clearHealthHud()
        closeFadeOverlay(cycles = 2)
    }

    private companion object {
        private const val FADE_CLIENT_DURATION = 50
    }
}
