package org.rsmod.api.death

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Singleton
import org.rsmod.api.mechanics.toxins.Toxin.cureAllToxins
import org.rsmod.api.player.deathResetTimers
import org.rsmod.api.player.disablePrayers
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

@Singleton
public class PlayerDeath() {
    private var Player.specialAttackType by intVarp("varp.sa_attack")

    public suspend fun death(access: ProtectedAccess) {
        access.deathSequence()
    }

    private suspend fun ProtectedAccess.deathSequence() {
        val respawn = CoordGrid(0, 50, 50, 21, 18)
        val randomRespawn = mapFindSquareLineOfWalk(respawn, minRadius = 0, maxRadius = 2)
        stopAction()
        delay(2)
        anim("seq.human_death")
        delay(4)
        combatClearQueue()
        clearQueue("queue.death")
        midiSong("midi.stop_music")
        midiJingle("jingle.air_guitar_jingle")
        mes("Oh dear, you are dead!")
        telejump(randomRespawn ?: respawn)
        resetAnim()
        // TODO: Drop death invs, etc.
        resetPlayerState()
        restoreToplevelTabs(
            "component.toplevel_osrs_stretch:pvp_icons",
            "component.toplevel_osrs_stretch:side1",
            "component.toplevel_osrs_stretch:side2",
            "component.toplevel_osrs_stretch:side4",
            "component.toplevel_osrs_stretch:side5",
            "component.toplevel_osrs_stretch:side6",
            "component.toplevel_osrs_stretch:side9",
            "component.toplevel_osrs_stretch:side8",
            "component.toplevel_osrs_stretch:side7",
            "component.toplevel_osrs_stretch:side10",
            "component.toplevel_osrs_stretch:side11",
            "component.toplevel_osrs_stretch:side12",
            "component.toplevel_osrs_stretch:side13",
        )
    }

    private fun ProtectedAccess.resetPlayerState() {
        player.disablePrayers()
        player.cureAllToxins()
        player.deathResetTimers()

        player.specialAttackType = 0
        player.skullIcon = null

        rebuildAppearance()

        camReset()
        statRestoreAll(ServerCacheManager.getStats().values.map { RSCM.getReverseMapping(RSCMType.STAT, it.id) })
        minimapReset()
    }
}
