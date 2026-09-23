package org.rsmod.content.other.special.weapons.scripts.charge

import jakarta.inject.Inject
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
import org.rsmod.api.utils.format.formatAmount
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class VenatorBowCharging
@Inject
constructor(private val charges: ObjChargeManager, private val objRepo: ObjRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld3("obj.venator_bow_uncharged") { charge(it.inventory, it.slot) }
        onOpHeld4("obj.venator_bow") { charge(it.inventory, it.slot) }
        onOpHeld5("obj.venator_bow") { uncharge(it.inventory, it.slot) }
        onOpHeld3("obj.venator_bow") { checkCharges(it.inventory[it.slot]) }
        onOpWorn2("obj.venator_bow") { checkCharges(player.righthand) }

        onOpHeldU("obj.venator_bow", "obj.ancient_essence") { charge(inv, it.firstSlot) }
        onOpHeldU("obj.venator_bow_uncharged", "obj.ancient_essence") {
            charge(inv, it.firstSlot)
        }
    }

    private suspend fun ProtectedAccess.charge(inventory: Inventory, invSlot: Int) {
        if ("obj.ancient_essence" !in inv) {
            mes("You don't appear to have any ancient essence to charge the Venator bow with.")
            return
        }

        val currCharges = charges.getCharges(inventory[invSlot], "varobj.venator_bow_charges")
        if (currCharges >= MAX_CHARGES) {
            mes("Your Venator bow is fully charged.")
            return
        }

        val maxCharges = min(MAX_CHARGES - currCharges, invTotal(inv, "obj.ancient_essence"))
        val question = "How many charges do you want to apply? (Up to $maxCharges)"
        val requested = min(countDialog(question), maxCharges)
        if (requested == 0) {
            return
        }

        val removeEssence = invDel(inv, "obj.ancient_essence", requested)
        if (removeEssence.failure) {
            return
        }

        charges.addCharges(inventory, invSlot, requested, "varobj.venator_bow_charges", MAX_CHARGES)
        objbox(
            "obj.venator_bow",
            400,
            "You apply $requested charges to your Venator bow.",
        )
    }

    private fun ProtectedAccess.checkCharges(obj: InvObj?) {
        val charges = charges.getCharges(obj, "varobj.venator_bow_charges")
        mes("Your Venator bow has $charges charges remaining.")
    }

    private suspend fun ProtectedAccess.uncharge(inventory: Inventory, invSlot: Int) {
        val currCharges = charges.getCharges(inventory[invSlot], "varobj.venator_bow_charges")
        if (currCharges == 0) {
            charges.removeAllCharges(inventory, invSlot, "varobj.venator_bow_charges")
            return
        }

        if (inv.freeSpace() < 1 && "obj.ancient_essence" !in inv) {
            mes("You don't have enough inventory space for the essence gained from uncharging.")
            return
        }

        val confirmation =
            choice2(
                "Proceed.",
                true,
                "Cancel.",
                false,
                title = "Uncharge all the charges from your bow?",
            )

        if (!confirmation) {
            return
        }

        val chargesRemoved = charges.removeAllCharges(inv, invSlot, "varobj.venator_bow_charges")
        check(chargesRemoved > 0)

        invAddOrDrop(objRepo, "obj.ancient_essence", chargesRemoved)

        val message =
            "You uncharge your Venator bow, regaining ${chargesRemoved.formatAmount} " +
                "ancient essence in the process."
        objbox("obj.venator_bow_uncharged", 400, message)
    }

    private companion object {
        const val MAX_CHARGES = 50_000
    }
}
