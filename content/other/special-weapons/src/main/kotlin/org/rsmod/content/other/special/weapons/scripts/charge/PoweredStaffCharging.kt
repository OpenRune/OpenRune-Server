package org.rsmod.content.other.special.weapons.scripts.charge

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.obj.charges.ObjChargeManager.Companion.isFailure
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.utils.format.formatAmount
import org.rsmod.content.other.special.weapons.magic.PoweredStaffType
import org.rsmod.content.other.special.weapons.magic.PoweredStaffTypes
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PoweredStaffCharging
@Inject
constructor(private val charges: ObjChargeManager, private val objRepo: ObjRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (type in PoweredStaffTypes.ALL.filter { it.chargeable && !it.customCharging }) {
            bindMaterials(type)
            if (type.chargeOp) {
                bindChargeOp(type)
            } else {
                bindUseOnOnly(type)
            }
        }
    }

    private fun ScriptContext.bindMaterials(type: PoweredStaffType) {
        for (staff in type.allObjs) {
            for (cost in type.costs) {
                onOpHeldU(staff, cost.obj) { charge(type, inv, it.firstSlot) }
            }
        }
    }

    private fun ScriptContext.bindChargeOp(type: PoweredStaffType) {
        for (staff in type.uncharged) {
            if (type.unchargedChargeOp == UNCHARGED_OP_4) {
                onOpHeld4(staff) { charge(type, it.inventory, it.slot) }
            } else {
                onOpHeld3(staff) { charge(type, it.inventory, it.slot) }
            }
        }
        for (staff in type.charged) {
            onOpHeld3(staff) { checkCharges(type, it.inventory[it.slot]) }
            onOpHeld4(staff) { charge(type, it.inventory, it.slot) }
            onOpHeld5(staff) { uncharge(type, it.inventory, it.slot) }
            onOpWorn2(staff) { checkCharges(type, player.righthand) }
        }
    }

    private fun ScriptContext.bindUseOnOnly(type: PoweredStaffType) {
        for (staff in type.allObjs) {
            onOpHeld3(staff) { checkCharges(type, it.inventory[it.slot]) }
        }
        for (staff in type.charged + type.alwaysFull.keys) {
            onOpHeld4(staff) { uncharge(type, it.inventory, it.slot) }
            onOpWorn2(staff) { checkCharges(type, player.righthand) }
        }
    }

    private companion object {
        const val UNCHARGED_OP_4 = 4
    }

    private fun currentCharges(type: PoweredStaffType, obj: InvObj?): Int {
        val varobj = type.varobj ?: return 0
        return when {
            obj == null -> 0
            type.alwaysFull.keys.any { obj.isType(it) } -> type.maxCharges
            else -> charges.getCharges(obj, varobj)
        }
    }

    private fun ProtectedAccess.checkCharges(type: PoweredStaffType, obj: InvObj?) {
        val name = type.name
        when (val count = currentCharges(type, obj)) {
            0 -> mes("Your $name has no charges.")
            1 -> mes("Your $name has one charge.")
            else -> mes("Your $name has ${count.formatAmount} charges${type.checkSuffix}.")
        }
    }

    private fun ProtectedAccess.maxChargesFromInventory(type: PoweredStaffType): Int =
        type.costs.minOf { invTotal(inv, it.obj) / it.perCharge }

    private fun isStaff(type: PoweredStaffType, obj: InvObj?): Boolean =
        obj != null && type.allObjs.any { obj.isType(it) }

    private suspend fun ProtectedAccess.charge(
        type: PoweredStaffType,
        inventory: Inventory,
        invSlot: Int,
    ) {
        val staff = inventory[invSlot]
        if (!isStaff(type, staff)) {
            return
        }

        val maxByMaterials = maxChargesFromInventory(type)
        if (maxByMaterials == 0) {
            mesbox(type.noChargesMessage)
            return
        }

        val room = type.maxCharges - currentCharges(type, staff)
        if (room <= 0) {
            mes("Your ${type.name} is already fully charged.")
            return
        }

        val max = min(room, maxByMaterials)
        val requested =
            min(countDialog("How many charges would you like to add? (0 - ${max.formatAmount})"), max)
        if (requested <= 0) {
            return
        }

        applyCharge(type, inventory, invSlot, requested)
    }

    suspend fun ProtectedAccess.applyCharge(
        type: PoweredStaffType,
        inventory: Inventory,
        invSlot: Int,
        requested: Int,
    ) {
        val varobj = type.varobj ?: return
        val staff = inventory[invSlot]
        if (!isStaff(type, staff)) {
            return
        }
        val room = type.maxCharges - currentCharges(type, staff)
        if (requested <= 0 || requested > min(room, maxChargesFromInventory(type))) {
            return
        }

        val taken = mutableListOf<Pair<String, Int>>()
        for (cost in type.costs) {
            val amount = requested * cost.perCharge
            if (invDel(inv, cost.obj, amount).failure) {
                refund(taken)
                return
            }
            taken += cost.obj to amount
        }

        val total: Int
        if (currentCharges(type, staff) == 0 && type.charged.any { staff!!.isType(it) }) {
            total = requested
            charges.setValue(inventory, invSlot, varobj, total)
        } else {
            val result = charges.addCharges(inventory, invSlot, requested, varobj, type.maxCharges)
            if (result.isFailure()) {
                refund(taken)
                return
            }
            total = result.total
        }

        val added = if (requested == 1) "a charge" else "${requested.formatAmount} charges"
        mesbox("You add $added to the ${type.name}. New total: ${total.formatAmount}")
    }

    private fun ProtectedAccess.refund(items: List<Pair<String, Int>>) {
        for ((obj, amount) in items) {
            invAddOrDrop(objRepo, obj, amount)
        }
    }

    private suspend fun ProtectedAccess.uncharge(
        type: PoweredStaffType,
        inventory: Inventory,
        invSlot: Int,
    ) {
        if (inventory[invSlot] == null) {
            return
        }

        val confirmation =
            choice2(
                "Okay, uncharge it.",
                true,
                "No, don't uncharge it.",
                false,
                title = type.unchargeTitle,
            )
        if (!confirmation) {
            return
        }

        applyUncharge(type, inventory, invSlot)
    }

    fun ProtectedAccess.applyUncharge(type: PoweredStaffType, inventory: Inventory, invSlot: Int) {
        val varobj = type.varobj ?: return
        val staff = inventory[invSlot]
        val fullUncharged =
            type.alwaysFull.entries.firstOrNull { staff != null && staff.isType(it.key) }?.value
        val charged = staff != null && type.charged.any { staff.isType(it) }
        if (fullUncharged == null && !charged) {
            return
        }

        val refunded =
            if (fullUncharged != null) {
                inventory[invSlot] = InvObj(fullUncharged)
                type.maxCharges
            } else {
                charges.removeAllCharges(inventory, invSlot, varobj)
            }

        for (refund in type.refunds) {
            invAddOrDrop(objRepo, refund.obj, refunded * refund.perCharge)
        }
        mes("You uncharge your ${type.name}.")
    }
}
