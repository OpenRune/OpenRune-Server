package org.rsmod.content.skills.herblore

import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.herbloreLvl
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.table.herblore.HerbloreHerbsRow
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CleanHerbEvents : PluginScript() {

    override fun ScriptContext.startup() {
        HerbloreHerbsRow.all().forEach { herb ->
            onOpHeld1(herb.grimy) { startClean(herb) }
        }
        onPlayerQueueWithArgs("queue.herblore_clean") { continueClean(it.args) }
    }

    private fun ProtectedAccess.startClean(herb: HerbloreHerbsRow) {
        if (!cleanOne(herb)) return
        if (inv.contains(herb.grimy.internalName)) {
            weakQueue("queue.herblore_clean", 6, herb)
        }
    }

    private fun ProtectedAccess.continueClean(herb: HerbloreHerbsRow) {
        if (!cleanOne(herb)) return
        if (inv.contains(herb.grimy.internalName)) {
            weakQueue("queue.herblore_clean", 6, herb)
        }
    }

    private fun ProtectedAccess.cleanOne(herb: HerbloreHerbsRow): Boolean {
        if (player.herbloreLvl < herb.level) {
            player.mes("You need a Herblore level of ${herb.level} to clean this herb.")
            return false
        }
        if (!invReplace(inv, herb.grimy.internalName, 1, herb.clean.internalName).success) {
            return false
        }
        statAdvance("stat.herblore", herb.xp / 10.0)
        player.mes("You clean the ${herb.clean.name}.")
        return true
    }
}
