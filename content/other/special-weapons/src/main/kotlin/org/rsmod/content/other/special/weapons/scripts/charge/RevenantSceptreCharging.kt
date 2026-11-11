package org.rsmod.content.other.special.weapons.scripts.charge

import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.script.onOpWorn3
import org.rsmod.api.utils.format.formatAmount
import org.rsmod.content.other.special.weapons.magic.RevenantSceptre
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class RevenantSceptreCharging
@Inject
constructor(private val charges: ObjChargeManager, private val objRepo: ObjRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (sceptre in RevenantSceptre.ALL) {
            bind(sceptre)
        }
    }

    private fun ScriptContext.bind(sceptre: RevenantSceptre) {
        for (obj in sceptre.all) {
            onOpHeldU(obj, ETHER) { charge(sceptre, inv, it.firstSlot) }
        }
        for (obj in sceptre.unchargedForms) {
            onOpHeld3(obj) { dismantle(sceptre, it.inventory, it.slot) }
            onOpHeld4(obj) { swap(sceptre, it.inventory, it.slot) }
        }
        for (obj in sceptre.chargedForms) {
            onOpHeld3(obj) { checkCharges(it.inventory[it.slot]) }
            onOpHeld4(obj) { swap(sceptre, it.inventory, it.slot) }
            onOpHeld5(obj) { uncharge(sceptre, it.inventory, it.slot) }
            onOpWorn2(obj) { checkCharges(player.righthand) }
            onOpWorn3(obj) { swap(sceptre, player.worn, Wearpos.RightHand.slot) }
        }
    }

    private fun ProtectedAccess.checkCharges(obj: InvObj?) {
        val usable = max(0, charges.getCharges(obj, ETHER_VAROBJ) - RevenantSceptre.ACTIVATION_ETHER)
        mes("Your sceptre has ${usable.formatAmount} charges left powering it.")
    }

    private fun isSceptre(sceptre: RevenantSceptre, obj: InvObj?): Boolean =
        obj != null && sceptre.all.any { obj.isType(it) }

    private suspend fun ProtectedAccess.charge(
        sceptre: RevenantSceptre,
        inventory: Inventory,
        invSlot: Int,
    ) {
        val staff = inventory[invSlot]
        if (!isSceptre(sceptre, staff)) {
            return
        }

        val current = charges.getCharges(staff, ETHER_VAROBJ)
        val room = RevenantSceptre.MAX_ETHER - current
        if (room <= 0) {
            mes("Your sceptre cannot hold any more revenant ether.")
            return
        }

        val held = invTotal(inv, ETHER)
        val needed = max(0, RevenantSceptre.ACTIVATION_ETHER - current)
        if (held < needed || held == 0) {
            mes("You need at least ${needed.formatAmount} revenant ether to activate this weapon.")
            return
        }

        val max = min(room, held)
        val requested =
            min(countDialog("How many ether would you like to add? (0 - ${max.formatAmount})"), max)
        if (requested <= 0) {
            return
        }

        applyCharge(sceptre, inventory, invSlot, requested)
    }

    fun ProtectedAccess.applyCharge(
        sceptre: RevenantSceptre,
        inventory: Inventory,
        invSlot: Int,
        requested: Int,
    ) {
        val staff = inventory[invSlot]
        if (!isSceptre(sceptre, staff)) {
            return
        }
        val current = charges.getCharges(staff, ETHER_VAROBJ)
        val total = current + requested
        val needed = max(0, RevenantSceptre.ACTIVATION_ETHER - current)
        val withinLimits =
            requested in max(1, needed)..min(RevenantSceptre.MAX_ETHER - current, invTotal(inv, ETHER))
        if (!withinLimits) {
            return
        }
        if (invDel(inv, ETHER, requested).failure) {
            return
        }

        val activating = current < RevenantSceptre.ACTIVATION_ETHER
        charges.setValue(inventory, invSlot, ETHER_VAROBJ, total)
        if (activating) {
            val inactive = inventory[invSlot]!!
            val obj = sceptre.all.first { inactive.isType(it) }
            inventory[invSlot] = InvObj(sceptre.chargedFor(obj), vars = inactive.vars)
            mes("You use ${RevenantSceptre.ACTIVATION_ETHER.formatAmount} ether to activate the weapon.")
            val extra = total - RevenantSceptre.ACTIVATION_ETHER
            mes("You add a further ${extra.formatAmount} revenant ether to your weapon, giving it a total of ${extra.formatAmount} charges.")
        } else {
            val usable = total - RevenantSceptre.ACTIVATION_ETHER
            mes("You add ${requested.formatAmount} revenant ether to your weapon, giving it a total of ${usable.formatAmount} charges.")
        }
    }

    private suspend fun ProtectedAccess.uncharge(
        sceptre: RevenantSceptre,
        inventory: Inventory,
        invSlot: Int,
    ) {
        if (inventory[invSlot] == null) {
            return
        }
        val confirmed =
            choice2("Yes.", true, "No.", false, title = "Remove all ether from the sceptre?")
        if (!confirmed) {
            return
        }
        applyUncharge(sceptre, inventory, invSlot)
    }

    fun ProtectedAccess.applyUncharge(
        sceptre: RevenantSceptre,
        inventory: Inventory,
        invSlot: Int,
    ) {
        val staff = inventory[invSlot]
        if (staff == null || sceptre.chargedForms.none { staff.isType(it) }) {
            return
        }
        val ether = charges.getCharges(staff, ETHER_VAROBJ)
        val obj = sceptre.chargedForms.first { staff.isType(it) }
        inventory[invSlot] = InvObj(sceptre.unchargedFor(obj), vars = staff.vars)
        charges.setValue(inventory, invSlot, ETHER_VAROBJ, 0)
        invAddOrDrop(objRepo, ETHER, ether)
        mes("You uncharge your sceptre and receive ${ether.formatAmount} revenant ether.")
    }

    private fun ProtectedAccess.swap(
        sceptre: RevenantSceptre,
        inventory: Inventory,
        invSlot: Int,
    ) {
        val staff = inventory[invSlot]
        if (!isSceptre(sceptre, staff)) {
            return
        }
        val obj = sceptre.all.first { staff!!.isType(it) }
        inventory[invSlot] = InvObj(sceptre.swapTarget(obj), vars = staff!!.vars)
    }

    private suspend fun ProtectedAccess.dismantle(
        sceptre: RevenantSceptre,
        inventory: Inventory,
        invSlot: Int,
    ) {
        if (inventory[invSlot] == null) {
            return
        }
        val confirmed = choice2("Yes.", true, "No.", false, title = "Dismantle the sceptre?")
        if (!confirmed) {
            return
        }
        applyDismantle(sceptre, inventory, invSlot)
    }

    fun ProtectedAccess.applyDismantle(
        sceptre: RevenantSceptre,
        inventory: Inventory,
        invSlot: Int,
    ) {
        val staff = inventory[invSlot]
        if (staff == null || sceptre.unchargedForms.none { staff.isType(it) }) {
            return
        }
        if (invDel(inventory, sceptre.unchargedForms.first { staff.isType(it) }, 1, slot = invSlot).failure) {
            return
        }
        if (sceptre === RevenantSceptre.ACCURSED) {
            invAddOrDrop(objRepo, RevenantSceptre.THAMMARON.uncharged)
            invAddOrDrop(objRepo, VETION_SKULL)
            mes("You separate the skull of Vet'ion from the sceptre.")
        } else {
            invAddOrDrop(objRepo, ETHER, THAMMARON_DISMANTLE_ETHER)
            mes("You dismantle the sceptre and receive ${THAMMARON_DISMANTLE_ETHER.formatAmount} revenant ether.")
        }
    }

    private companion object {
        const val ETHER = RevenantSceptre.ETHER
        const val ETHER_VAROBJ = RevenantSceptre.ETHER_VAROBJ
        const val VETION_SKULL = "obj.wbr_vetion_skull"
        const val THAMMARON_DISMANTLE_ETHER = 7_500
    }
}
