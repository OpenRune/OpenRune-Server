package org.rsmod.content.other.special.weapons.scripts.charge

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.utils.format.formatAmount
import org.rsmod.content.other.special.weapons.magic.BoneStaffWeapons
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BoneStaffCharging
@Inject
constructor(private val charges: ObjChargeManager, private val objRepo: ObjRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU(STAFF, CHAOS_RUNE) { charge(inv, it.firstSlot) }
        onOpHeld3(STAFF) { checkCharges(it.inventory[it.slot]) }
        onOpHeld4(STAFF) { uncharge(it.inventory, it.slot) }
        onOpWorn2(STAFF) { checkCharges(player.righthand) }
    }

    private fun ProtectedAccess.checkCharges(obj: InvObj?) {
        val count = charges.getCharges(obj, CHARGES)
        mes("Your Bone staff has ${count.formatAmount} charges remaining.")
    }

    private suspend fun ProtectedAccess.charge(inventory: Inventory, invSlot: Int) {
        val staff = inventory[invSlot]
        if (staff == null || !staff.isType(STAFF)) {
            return
        }
        val room = MAX_CHARGES - charges.getCharges(staff, CHARGES)
        if (room <= 0) {
            mes("Your Bone staff is already fully charged.")
            return
        }
        val max = min(room, invTotal(inv, CHAOS_RUNE))
        if (max <= 0) {
            return
        }
        val requested =
            min(countDialog("How many chaos runes do you wish to add? (0 - ${max.formatAmount})"), max)
        if (requested <= 0) {
            return
        }
        applyCharge(inventory, invSlot, requested)
    }

    fun ProtectedAccess.applyCharge(inventory: Inventory, invSlot: Int, requested: Int) {
        val staff = inventory[invSlot]
        if (staff == null || !staff.isType(STAFF)) {
            return
        }
        val current = charges.getCharges(staff, CHARGES)
        if (requested <= 0 || requested > min(MAX_CHARGES - current, invTotal(inv, CHAOS_RUNE))) {
            return
        }
        if (invDel(inv, CHAOS_RUNE, requested).failure) {
            return
        }
        charges.setValue(inventory, invSlot, CHARGES, current + requested)
    }

    fun ProtectedAccess.applyUncharge(inventory: Inventory, invSlot: Int) {
        val staff = inventory[invSlot]
        if (staff == null || !staff.isType(STAFF)) {
            return
        }
        val current = charges.getCharges(staff, CHARGES)
        if (current <= 0) {
            mes("Your Bone staff has no charges to remove.")
            return
        }
        charges.setValue(inventory, invSlot, CHARGES, 0)
        invAddOrDrop(objRepo, CHAOS_RUNE, current)
        mes("You uncharge your Bone staff.")
    }

    private suspend fun ProtectedAccess.uncharge(inventory: Inventory, invSlot: Int) {
        if (inventory[invSlot] == null) {
            return
        }
        val confirmed = choice2("Yes.", true, "No.", false, title = "Fully uncharge your Bone staff?")
        if (!confirmed) {
            return
        }
        applyUncharge(inventory, invSlot)
    }

    private companion object {
        const val STAFF = BoneStaffWeapons.BONE_STAFF
        const val CHAOS_RUNE = "obj.chaosrune"
        const val CHARGES = BoneStaffWeapons.CHARGES_VAROBJ
        const val MAX_CHARGES = BoneStaffWeapons.MAX_CHARGES
    }
}
