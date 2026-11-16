package org.rsmod.content.other.special.weapons.scripts.charge

import jakarta.inject.Inject
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.utils.format.formatAmount
import org.rsmod.content.other.special.weapons.magic.StarterStaffWeapons
import org.rsmod.game.inv.InvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class StarterStaffCheck @Inject constructor(private val charges: ObjChargeManager) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld3(STAFF) { check(it.inventory[it.slot]) }
        onOpWorn2(STAFF) { check(player.righthand) }
    }

    private fun ProtectedAccess.check(obj: InvObj?) {
        val used = charges.getCharges(obj, StarterStaffWeapons.USED_VAROBJ)
        val left = StarterStaffWeapons.MAX_CASTS - used
        mes("Your starter staff has ${left.formatAmount} casts left.")
    }

    private companion object {
        const val STAFF = StarterStaffWeapons.STARTER_STAFF
    }
}
