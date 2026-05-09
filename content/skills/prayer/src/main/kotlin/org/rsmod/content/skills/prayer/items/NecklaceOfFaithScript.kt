package org.rsmod.content.skills.prayer.items

import dev.openrune.util.Wearpos
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.basePrayerLvl
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.script.onPlayerHitpointsChanged
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class NecklaceOfFaithScript : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerHitpointsChanged {
            if (newHitpoints <= 0) {
                return@onPlayerHitpointsChanged
            }

            val wasAboveTwentyPercent = oldHitpoints * 5 > maxHitpoints
            val isBelowTwentyPercent = newHitpoints * 5 < maxHitpoints
            if (!wasAboveTwentyPercent || !isBelowTwentyPercent) {
                return@onPlayerHitpointsChanged
            }

            val necklaceSlot = Wearpos.Front.slot
            val necklace = player.worn[necklaceSlot]
            if (necklace?.isType("obj.necklace_of_faith") != true) {
                return@onPlayerHitpointsChanged
            }

            val restore = player.basePrayerLvl / 4
            if (restore > 0) {
                player.statHeal("stat.prayer", constant = restore, percent = 0)
            }

            player.invDel(player.worn, "obj.necklace_of_faith", count = 1, slot = necklaceSlot)
            player.mes("Your necklace of faith shatters and restores your prayer points.")
        }
    }
}
