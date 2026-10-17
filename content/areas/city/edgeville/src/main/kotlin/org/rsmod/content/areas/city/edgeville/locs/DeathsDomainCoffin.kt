package org.rsmod.content.areas.city.edgeville.locs

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Death's Domain coffin in the Edgeville graveyard and the portal back out of Death's office.
 * Both play the purple portal overlay for the two ticks before the teleport, then fade it out.
 */
class DeathsDomainCoffin : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.death_office_access_coffin") { enterDeathsOffice() }
        onOpLoc1("loc.death_office_exitportal") { leaveDeathsOffice() }
    }

    private suspend fun ProtectedAccess.enterDeathsOffice() {
        arriveDelay()
        anim("seq.human_pickupfloor", delay = 20)
        portalTo(DEATHS_OFFICE)
    }

    private suspend fun ProtectedAccess.leaveDeathsOffice() {
        arriveDelay()
        portalTo(EDGEVILLE_GRAVEYARD)
    }

    private suspend fun ProtectedAccess.portalTo(dest: CoordGrid) {
        val background = "component.overlay_portal:background".asRSCM(RSCMType.COMPONENT)
        val foreground = "component.overlay_portal:foreground".asRSCM(RSCMType.COMPONENT)
        ifOpenFullOverlay(PORTAL_OVERLAY)
        runClientScript(
            PORTAL_SCRIPT.asRSCM(RSCMType.CLIENTSCRIPT),
            background,
            foreground,
            PORTAL_MODEL,
            PORTAL_COLOUR,
            -1,
            -1,
        )
        soundSynth("synth.deaths_domain_portal")
        delay(2)
        telejump(dest)
        runClientScript(PORTAL_REMOVE_SCRIPT.asRSCM(RSCMType.CLIENTSCRIPT), background, foreground, -1, -1)
        delay(1)
        ifCloseSub(PORTAL_OVERLAY)
    }

    private companion object {
        private const val PORTAL_OVERLAY = "interface.overlay_portal"
        private const val PORTAL_SCRIPT = "clientscript.[clientscript,overlay_portal]"
        private const val PORTAL_REMOVE_SCRIPT = "clientscript.[clientscript,overlay_portal_remove]"
        private const val PORTAL_MODEL = 39504
        private const val PORTAL_COLOUR = 0x3F009F

        private val DEATHS_OFFICE = CoordGrid(3171, 5726, 0)
        private val EDGEVILLE_GRAVEYARD = CoordGrid(3097, 3475, 0)
    }
}
