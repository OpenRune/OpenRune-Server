package org.rsmod.content.skills.prayer

import org.rsmod.api.player.stat.basePrayerLvl
import org.rsmod.api.player.stat.prayerLvl
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onArea
import org.rsmod.api.script.onAreaExit
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class DemonicRuinsPrayerRegenScript : PluginScript() {
    override fun ScriptContext.startup() {
        onArea("area.demonic_ruins") { enterDemonicRuinsPrayerRegenArea() }
        onAreaExit("area.demonic_ruins") { clearQueue("queue.prayer_demonic_ruins_regen") }
        onPlayerQueue("queue.prayer_demonic_ruins_regen") { processDemonicRuinsPrayerRegen() }
    }

    private fun ProtectedAccess.enterDemonicRuinsPrayerRegenArea() {
        clearQueue("queue.prayer_demonic_ruins_regen")
        player.queue("queue.prayer_demonic_ruins_regen", 5)
        player.mes("A dark and ancient energy is emitted from the ruins granting you faster prayer restoration.",)
    }

    private fun ProtectedAccess.processDemonicRuinsPrayerRegen() {
        if (!inArea("area.demonic_ruins", player.coords)) {
            return
        }
        if (player.prayerLvl < player.basePrayerLvl) {
            statHeal("stat.prayer", constant = 1, percent = 0)
        }
        player.queue("queue.prayer_demonic_ruins_regen", 5)
    }
}
