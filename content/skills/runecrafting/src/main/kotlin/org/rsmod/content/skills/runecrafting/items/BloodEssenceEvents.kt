package org.rsmod.content.skills.runecrafting.items

import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.skills.runecrafting.bloodEssenceCharges
import org.rsmod.api.script.onOpHeld1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BloodEssenceEvents : PluginScript() {

    override fun ScriptContext.startup() {
        onOpHeld1(BloodEssence.INACTIVE) {
            activateBloodEssence()
        }

        onOpHeld1(BloodEssence.ACTIVE) {
            checkBloodEssence()
        }
    }

    private fun ProtectedAccess.activateBloodEssence() {
        if (BloodEssence.hasActive(player)) {
            mes("You can only have one active blood essence at a time.")
            return
        }

        if (invDel(inv, BloodEssence.INACTIVE, 1).failure) {
            return
        }

        if (invAdd(inv, BloodEssence.ACTIVE, 1).failure) {
            invAdd(inv, BloodEssence.INACTIVE, 1)
            return
        }

        player.bloodEssenceCharges = BloodEssence.MAX_CHARGES
        soundSynth(BloodEssence.ACTIVATE_SOUND)
        mes("You activate the blood essence.")
    }

    private fun ProtectedAccess.checkBloodEssence() {
        val charges = player.bloodEssenceCharges
        mes("Your blood essence has $charges charges remaining.")
    }
}
