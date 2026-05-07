package org.rsmod.content.skills.herblore

import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.herbloreLvl
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.table.herblore.HerbloreHerbsRow
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CleanHerbEvents : PluginScript() {

    override fun ScriptContext.startup() {
        HerbloreHerbsRow.all().forEach { herb ->
            onOpHeld1(herb.grimy) { clean(herb) }
        }
    }

    private fun ProtectedAccess.clean(herb: HerbloreHerbsRow) {
        if (player.herbloreLvl < herb.level) {
            player.mes("You need a Herblore level of ${herb.level} to clean this herb.")
            return
        }

        if (!invReplace(inv, herb.grimy.internalName, 1, herb.clean.internalName).success) {
            return
        }

        statAdvance("stat.herblore", herb.xp / 10.0)
        player.mes("You clean the ${herb.clean.name}.")
    }
}
