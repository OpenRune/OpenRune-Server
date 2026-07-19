package org.rsmod.content.other.ironman

import jakarta.inject.Inject
import org.rsmod.api.player.ironman.IronmanRestrictions
import org.rsmod.api.player.ironman.syncGamemodeFromVarbit
import org.rsmod.api.script.advanced.onOpPlayer4
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class IronmanScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<SessionStateEvent.Login> { player.syncGamemodeFromVarbit() }
        onOpPlayer4 { IronmanRestrictions.blockTrade(player, it.target) }
    }
}
