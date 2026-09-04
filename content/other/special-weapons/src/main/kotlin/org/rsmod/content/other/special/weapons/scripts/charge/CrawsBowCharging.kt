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
 * be added (up to 16,000)." Same activation cost, max ammo, and revenant ether item as the
 * Webweaver bow it can be upgraded into - mirrors [WebweaverBowCharging] exactly.
 *
 * Real op layout confirmed cache-side: `iop3=Check, iop5=Uncharge` on the charged variant, no
 * separate partial "Unload" (same all-or-nothing Uncharge as Webweaver bow). Check is on
 * `onOpWorn2` rather than `onOpWorn1` for the same reason as the other charge weapons in this
 * package: `onOpWorn1` while worn is hardcoded "Remove" in this engine regardless of what the
 * real client's `wear_op1` claims.
 */
class CrawsBowCharging
@Inject
constructor(
    private val charges: ObjChargeManager,
    private val objRepo: ObjRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU("obj.wild_cave_bow_uncharged", "obj.wild_cave_shard") {
            activate(inv, it.firstSlot)
        }
        onOpHeldU("obj.wild_cave_bow_charged", "obj.wild_cave_shard") { topUp(inv, it.firstSlot) }

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
        check(inventory[invSlot].isType("obj.wild_cave_bow_uncharged"))

        val startingCharges = requested - ACTIVATION_COST
        charges.addCharges(inventory, invSlot, startingCharges, ETHER_VAROBJ, MAX_CHARGES)

        val suffix = if (startingCharges > 0) " The rest is loaded as ammo." else ""
        mes("You activate your Craw's bow, using ${ACTIVATION_COST.formatAmount} revenant ether.$suffix")
    }

    private suspend fun ProtectedAccess.topUp(inventory: Inventory, invSlot: Int) {
        val available = invTotal(inv, ETHER_ITEM)
        if (available <= 0) {
            mes("You don't have any revenant ether.")
            return
        }

        val current = charges.getCharges(inventory[invSlot], ETHER_VAROBJ)
        if (current >= MAX_CHARGES) {
            mes("Your Craw's bow is fully charged.")
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
        mes("You add ${requested.formatAmount} revenant ether to your Craw's bow.")
    }

    private fun ProtectedAccess.checkCharges(obj: InvObj?) {
        val remaining = charges.getCharges(obj, ETHER_VAROBJ)
        mes("Your Craw's bow has ${remaining.formatAmount} charges of revenant ether left.")
    }

    private suspend fun ProtectedAccess.uncharge(inventory: Inventory, invSlot: Int) {
        val current = charges.getCharges(inventory[invSlot], ETHER_VAROBJ)
        if (current == 0) {
            mes("Your Craw's bow has no charges to uncharge.")
            return
        }
        if (inv.freeSpace() < 1) {
            mes("You don't have enough inventory space to uncharge your Craw's bow.")
            return
        }

        val confirmed =
            choice2(
                "Proceed.",
                true,
                "Cancel.",
                false,
                title = "Uncharge all revenant ether from your Craw's bow?",
            )
        if (!confirmed) {
            return
        }

        val removed = charges.removeAllCharges(inventory, invSlot, ETHER_VAROBJ)
        check(removed > 0)
        invAddOrDrop(objRepo, ETHER_ITEM, removed)
        mes("You uncharge your Craw's bow, regaining ${removed.formatAmount} revenant ether.")
    }

    private companion object {
        const val CHARGED: String = "obj.wild_cave_bow_charged"
        const val ETHER_ITEM: String = "obj.wild_cave_shard"
        const val ETHER_VAROBJ: String = "varobj.charges_16383"
        const val ACTIVATION_COST: Int = 1000
        const val MAX_CHARGES: Int = 16000
    }
}
