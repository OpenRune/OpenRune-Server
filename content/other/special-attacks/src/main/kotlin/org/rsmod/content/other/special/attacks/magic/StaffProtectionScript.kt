package org.rsmod.content.other.special.attacks.magic

import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerInit
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class StaffProtectionScript : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerInit { StaffProtectionSpecialAttacks.clear(player) }
        onPlayerLogout { StaffProtectionSpecialAttacks.clear(player) }
        onEvent<PlayerHitEvents.BeforeImpact> { hit = StaffProtectionSpecialAttacks.modify(player, hit) }
    }
}
