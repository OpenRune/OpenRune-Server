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
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.utils.format.formatAmount
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class EyeOfAyakCharging
@Inject
constructor(private val charges: ObjChargeManager, private val objRepo: ObjRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld3(UNCHARGED) { charge(it.inventory, it.slot, null) }
        onOpHeld4(UNCHARGED) { dismantle(it.inventory, it.slot) }
        onOpHeld3(CHARGED) { checkCharges(it.inventory[it.slot]) }
        onOpHeld4(CHARGED) { uncharge(it.inventory, it.slot) }
        onOpWorn2(CHARGED) { checkCharges(player.righthand) }
        for (staff in listOf(UNCHARGED, CHARGED)) {
            for (material in listOf(DEMON_TEAR, DEATH_RUNE, CHAOS_RUNE)) {
                onOpHeldU(staff, material) { charge(inv, it.firstSlot, material) }
            }
        }
    }

    private fun ProtectedAccess.checkCharges(obj: InvObj?) {
        val count = charges.getCharges(obj, CHARGES)
        mes("Your Eye of Ayak has ${count.formatAmount} charges.")
    }

    private fun ProtectedAccess.runeSets(): Int =
        min(invTotal(inv, DEATH_RUNE) / DEATH_PER_CHARGE, invTotal(inv, CHAOS_RUNE))

    private suspend fun ProtectedAccess.charge(
        inventory: Inventory,
        invSlot: Int,
        material: String?,
    ) {
        val staff = inventory[invSlot]
        if (staff == null || !(staff.isType(UNCHARGED) || staff.isType(CHARGED))) {
            return
        }

        val current = charges.getCharges(staff, CHARGES)
        val tears = invTotal(inv, DEMON_TEAR)
        val sets = runeSets()
        val runeMode = charges.getCharges(staff, RUNE_MODE) == 1
        val useRunes =
            when {
                material != null -> material != DEMON_TEAR
                current > 0 -> runeMode
                else -> tears == 0
            }

        if (current > 0 && runeMode && !useRunes) {
            mes(
                "The Eye of Ayak is charged with runes, and thus can only be charged with more " +
                    "runes. You must fully uncharge it to use demon tears."
            )
            return
        }
        if (current > 0 && !runeMode && useRunes) {
            mes(
                "The Eye of Ayak is charged with demon tears, and thus can only be charged with " +
                    "more demon tears. You must fully uncharge it to use runes."
            )
            return
        }

        val available = if (useRunes) sets else tears
        if (available == 0) {
            mes(
                "You need at least one chaos rune and two death runes or one demon tear in " +
                    "order to charge the Eye of Ayak."
            )
            return
        }

        val room = MAX_CHARGES - current
        if (room <= 0) {
            mes("The Eye of Ayak cannot hold any more charges.")
            return
        }

        val max = min(room, available)
        val requested =
            min(countDialog("How many charges do you wish to add? (0 - ${max.formatAmount})"), max)
        if (requested <= 0) {
            return
        }

        applyCharge(inventory, invSlot, requested, useRunes)
    }

    suspend fun ProtectedAccess.applyCharge(
        inventory: Inventory,
        invSlot: Int,
        requested: Int,
        useRunes: Boolean,
    ) {
        val staff = inventory[invSlot]
        if (staff == null || !(staff.isType(UNCHARGED) || staff.isType(CHARGED))) {
            return
        }
        val current = charges.getCharges(staff, CHARGES)
        if (current > 0 && (charges.getCharges(staff, RUNE_MODE) == 1) != useRunes) {
            return
        }
        val available = if (useRunes) runeSets() else invTotal(inv, DEMON_TEAR)
        if (requested <= 0 || requested > min(MAX_CHARGES - current, available)) {
            return
        }

        if (useRunes) {
            val removed =
                invDel(
                    inv,
                    DEATH_RUNE,
                    requested * DEATH_PER_CHARGE,
                    CHAOS_RUNE,
                    requested,
                )
            if (removed.failure) {
                return
            }
        } else if (invDel(inv, DEMON_TEAR, requested).failure) {
            return
        }

        val total: Int
        if (current == 0 && staff.isType(CHARGED)) {
            total = requested
            charges.setValue(inventory, invSlot, CHARGES, total)
        } else {
            val result = charges.addCharges(inventory, invSlot, requested, CHARGES, MAX_CHARGES)
            if (result.isFailure()) {
                refund(useRunes, requested)
                return
            }
            total = result.total
        }
        charges.setValue(inventory, invSlot, RUNE_MODE, if (useRunes) 1 else 0)

        val kind = if (useRunes) "runes" else "demon tears"
        mesbox(
            "The Eye of Ayak has been charged with $kind. " +
                "It currently has ${total.formatAmount} charges."
        )
    }

    private fun ProtectedAccess.refund(useRunes: Boolean, charges: Int) {
        if (useRunes) {
            invAddOrDrop(objRepo, DEATH_RUNE, charges * DEATH_PER_CHARGE)
            invAddOrDrop(objRepo, CHAOS_RUNE, charges)
        } else {
            invAddOrDrop(objRepo, DEMON_TEAR, charges)
        }
    }

    private suspend fun ProtectedAccess.uncharge(inventory: Inventory, invSlot: Int) {
        if (inventory[invSlot] == null) {
            return
        }
        val confirmed = choice2("Yes.", true, "No.", false, title = "Fully uncharge the Eye of Ayak?")
        if (!confirmed) {
            return
        }
        applyUncharge(inventory, invSlot)
    }

    fun ProtectedAccess.applyUncharge(inventory: Inventory, invSlot: Int) {
        val staff = inventory[invSlot]
        if (staff == null || !staff.isType(CHARGED)) {
            return
        }
        val runes = charges.getCharges(staff, RUNE_MODE) == 1
        val removed = charges.removeAllCharges(inventory, invSlot, CHARGES)
        charges.setValue(inventory, invSlot, RUNE_MODE, 0)
        refund(runes, removed)
        mes("You uncharge the Eye of Ayak.")
    }

    private suspend fun ProtectedAccess.dismantle(inventory: Inventory, invSlot: Int) {
        if (inventory[invSlot] == null) {
            return
        }
        val confirmed = choice2("Yes.", true, "No.", false, title = "Dismantle the Eye of Ayak?")
        if (!confirmed) {
            return
        }
        applyDismantle(inventory, invSlot)
    }

    fun ProtectedAccess.applyDismantle(inventory: Inventory, invSlot: Int) {
        val staff = inventory[invSlot]
        if (staff == null || !staff.isType(UNCHARGED)) {
            return
        }
        if (invDel(inventory, UNCHARGED, 1, slot = invSlot).failure) {
            return
        }
        invAddOrDrop(objRepo, DEMON_TEAR, DISMANTLE_TEARS)
        mes("You dismantle the Eye of Ayak.")
    }

    private companion object {
        const val UNCHARGED = "obj.eye_of_ayak_uncharged"
        const val CHARGED = "obj.eye_of_ayak"
        const val DEMON_TEAR = "obj.demon_tear"
        const val DEATH_RUNE = "obj.deathrune"
        const val CHAOS_RUNE = "obj.chaosrune"
        const val CHARGES = "varobj.charges_65535"
        const val RUNE_MODE = "varobj.ayak_rune_charges"
        const val MAX_CHARGES = 50_000
        const val DEATH_PER_CHARGE = 2
        const val DISMANTLE_TEARS = 10_000
    }
}
