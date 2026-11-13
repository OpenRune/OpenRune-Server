package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpWorn2
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingEquipmentScript @Inject constructor(private val equipment: FarmingEquipment) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld3(FarmingEquipment.BOUNTY) {
            mes("Your amulet of bounty has ${equipment.bountyCharges(player)} charges remaining.")
        }
        onOpWorn2(FarmingEquipment.BOUNTY) {
            mes("Your amulet of bounty has ${equipment.bountyCharges(player)} charges remaining.")
        }
        onOpHeld4(FarmingEquipment.BOUNTY) {
            if (invDel(inv, FarmingEquipment.BOUNTY, 1, slot = it.slot, strict = true).success) {
                VarPlayerIntMapSetter.set(player, FarmingEquipment.BOUNTY_USES, 0)
                mes("You break the amulet of bounty.")
            }
        }
    }
}
