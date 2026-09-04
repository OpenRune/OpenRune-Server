package org.rsmod.content.other.special.weapons.scripts.charge

import jakarta.inject.Inject
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.utils.format.formatAmount
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Wiki: "the bow must first be activated with 1,000 revenant ether... The 1,000 ether used to
 * activate the bow does not count towards the ammo usage of the bow, thus additional ether must
 * be added (up to 16,000) to fire it." The real item is `obj.wild_cave_shard` in this cache, not
 * a "revenant_ether"-named alias.
 *
 * Real op layout confirmed via a live cache dump: `iop3=Check, iop5=Uncharge` on the charged
 * variant - no separate partial "Unload" exists on this item at all, only an all-or-nothing
 * Uncharge (matching the activated-with-ObjChargeManager charge model - there's one pooled charge
 * count, not a separate dart/scale-style breakdown). Check is on `onOpWorn2` rather than `onOpWorn1`
 * despite the real client showing it at `wear_op1` - `onOpWorn1` replaces this engine's own
 * "Remove" handling regardless of what the real client's op1 label says (confirmed the hard way on
 * the toxic blowpipe's own Check option), so `onOpWorn2` is the safe slot instead.
 */
class WebweaverBowCharging
@Inject
constructor(
    private val charges: ObjChargeManager,
    private val objRepo: ObjRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU("obj.wild_cave_webweaver_uncharged", "obj.wild_cave_shard") {
            activate(inv, it.firstSlot)
        }
        onOpHeldU("obj.wild_cave_webweaver_charged", "obj.wild_cave_shard") {
            topUp(inv, it.firstSlot)
        }

        onOpHeld3(CHARGED) { checkCharges(inv[it.slot]) }
        onOpWorn2(CHARGED) { checkCharges(player.righthand) }
        onOpHeld5(CHARGED) { uncharge(inv, it.slot) }
    }

    private suspend fun ProtectedAccess.activate(inventory: Inventory, invSlot: Int) {
        val available = invTotal(inv, ETHER_ITEM)
        if (available < ACTIVATION_COST) {
            mes(
                "You need at least ${ACTIVATION_COST.formatAmount} revenant ether to " +
                    "activate this bow."
            )
            return
        }

        val question =
            "How much revenant ether do you want to use? (minimum " +
                "${ACTIVATION_COST.formatAmount}, up to ${available.formatAmount})"
        val requested = countDialog(question).coerceIn(ACTIVATION_COST, available)

        val removed = invDel(inv, ETHER_ITEM, requested)
        if (removed.failure) {
            return
        }

        // Paranoid check: Should always be the case.
        check(inventory[invSlot].isType("obj.wild_cave_webweaver_uncharged"))

        val startingCharges = requested - ACTIVATION_COST
        charges.addCharges(inventory, invSlot, startingCharges, ETHER_VAROBJ, MAX_CHARGES)

        val suffix = if (startingCharges > 0) " The rest is loaded as ammo." else ""
        mes(
            "You activate your Webweaver bow, using ${ACTIVATION_COST.formatAmount} " +
                "revenant ether.$suffix"
        )
    }

    private suspend fun ProtectedAccess.topUp(inventory: Inventory, invSlot: Int) {
        val available = invTotal(inv, ETHER_ITEM)
        if (available <= 0) {
            mes("You don't have any revenant ether.")
            return
        }

        val current = charges.getCharges(inventory[invSlot], ETHER_VAROBJ)
        if (current >= MAX_CHARGES) {
            mes("Your Webweaver bow is fully charged.")
            return
        }

        val maxAdd = MAX_CHARGES - current
        val cap = minOf(available, maxAdd)
        val question = "How much revenant ether do you want to add? (up to ${cap.formatAmount})"
        val requested = countDialog(question).coerceIn(1, cap)

        val removed = invDel(inv, ETHER_ITEM, requested)
        if (removed.failure) {
            return
        }

        charges.addCharges(inventory, invSlot, requested, ETHER_VAROBJ, MAX_CHARGES)
        mes("You add ${requested.formatAmount} revenant ether to your Webweaver bow.")
    }

    private fun ProtectedAccess.checkCharges(obj: InvObj?) {
        val remaining = charges.getCharges(obj, ETHER_VAROBJ)
        mes("Your Webweaver bow has ${remaining.formatAmount} charges of revenant ether left.")
    }

    private suspend fun ProtectedAccess.uncharge(inventory: Inventory, invSlot: Int) {
        val current = charges.getCharges(inventory[invSlot], ETHER_VAROBJ)
        if (current == 0) {
            mes("Your Webweaver bow has no charges to uncharge.")
            return
        }
        if (inv.freeSpace() < 1) {
            mes("You don't have enough inventory space to uncharge your Webweaver bow.")
            return
        }

        val confirmed =
            choice2(
                "Proceed.",
                true,
                "Cancel.",
                false,
                title = "Uncharge all revenant ether from your Webweaver bow?",
            )
        if (!confirmed) {
            return
        }

        val removed = charges.removeAllCharges(inventory, invSlot, ETHER_VAROBJ)
        check(removed > 0)
        invAddOrDrop(objRepo, ETHER_ITEM, removed)
        mes("You uncharge your Webweaver bow, regaining ${removed.formatAmount} revenant ether.")
    }

    private companion object {
        const val CHARGED: String = "obj.wild_cave_webweaver_charged"
        const val ETHER_ITEM: String = "obj.wild_cave_shard"
        const val ETHER_VAROBJ: String = "varobj.charges_16383"
        const val ACTIVATION_COST: Int = 1000
        const val MAX_CHARGES: Int = 16000
    }
}
