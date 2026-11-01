package org.rsmod.content.interfaces.gameframe.script

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.script.onIfOpen
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class OrbsScript @Inject constructor(private val clock: MapClock) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOpen("interface.orbs") { player.syncOrbs() }
    }

    private fun Player.syncOrbs() {
        ifSetEvents("component.orbs:triggers", 0..0, IfEvent.ScriptTrigger)
        runClientScript(HEALTH_REGEN_ORBS_SYNCH.asRSCM(RSCMType.CLIENTSCRIPT), clock.cycle)
        runClientScript(SPEC_REGEN_ORBS_SYNCH.asRSCM(RSCMType.CLIENTSCRIPT), clock.cycle)
    }

    private companion object {
        const val HEALTH_REGEN_ORBS_SYNCH = "clientscript.[clientscript,script4717]"
        const val SPEC_REGEN_ORBS_SYNCH = "clientscript.[clientscript,script4722]"
    }
}
