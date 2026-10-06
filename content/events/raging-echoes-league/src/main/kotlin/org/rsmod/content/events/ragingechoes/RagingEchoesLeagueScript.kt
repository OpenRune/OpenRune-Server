package org.rsmod.content.events.ragingechoes

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class RagingEchoesLeagueScript @Inject constructor() : PluginScript() {
    override val worldTypes = listOf(WorldType.RAGING_ECHOES_LEAGUE)

    override fun ScriptContext.startup() {
        onPlayerLogin { player.mes("Welcome to Leagues Echo") }
    }
}
