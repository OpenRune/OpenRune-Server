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

class TridentOfTheSeasCharging
@Inject
constructor(private val charges: ObjChargeManager, private val objRepo: ObjRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (trident in TRIDENTS) {
            onOpHeld3(trident) { checkCharges(it.inventory[it.slot]) }
            for (material in MATERIALS) {
                onOpHeldU(trident, material) { charge(inv, it.firstSlot) }
            }
        }
        onOpHeld4(FULL) { uncharge(it.inventory, it.slot) }
        onOpHeld4(CHARGED) { uncharge(it.inventory, it.slot) }
        onOpWorn2(CHARGED) { checkCharges(player.righthand) }
        onOpWorn2(FULL) { checkCharges(player.righthand) }
    }

    private fun currentCharges(obj: InvObj?): Int =
        when {
            obj == null -> 0
            obj.isType(FULL) -> MAX_CHARGES
            else -> charges.getCharges(obj, CHARGE_VAROBJ)
        }

    private fun ProtectedAccess.checkCharges(obj: InvObj?) {
        val name = if (obj.isType(UNCHARGED)) "Uncharged trident" else "Trident of the Seas"
        when (val count = currentCharges(obj)) {
            0 -> mes("Your $name has no charges.")
            1 -> mes("Your $name has one charge.")
            else -> mes("Your $name has ${count.formatAmount} charges.")
        }
    }

    private suspend fun ProtectedAccess.charge(inventory: Inventory, invSlot: Int) {
        val trident = inventory[invSlot]
        val maxByMaterials = maxChargesFromInventory()
        if (maxByMaterials == 0) {
            val name = if (trident.isType(UNCHARGED)) "Uncharged trident" else "trident"
            mes(
                "You need one death rune, one chaos rune, five fire runes and ten coins to " +
                    "charge the $name."
            )
            return
        }

        val room = MAX_CHARGES - currentCharges(trident)
        if (room <= 0) {
            mes("Your Trident of the Seas is already fully charged.")
            return
        }

        val max = min(room, maxByMaterials)
        val requested = min(countDialog("How many charges would you like to add? (0 - $max)"), max)
        if (requested == 0) {
            return
        }

        val currentSlot = inventory[invSlot]
        if (currentSlot == null || !currentSlot.isType(UNCHARGED) && !currentSlot.isType(CHARGED)) {
            return
        }

        val removeRunes =
            invDel(
                inv = inv,
                type1 = DEATH_RUNE,
                count1 = requested * DEATH_PER_CHARGE,
                type2 = CHAOS_RUNE,
                count2 = requested * CHAOS_PER_CHARGE,
            )
        if (removeRunes.failure) {
            return
        }
        val removeRest =
            invDel(
                inv = inv,
                type1 = FIRE_RUNE,
                count1 = requested * FIRE_PER_CHARGE,
                type2 = COINS,
                count2 = requested * COINS_PER_CHARGE,
            )
        if (removeRest.failure) {
            invAdd(inv, DEATH_RUNE, requested * DEATH_PER_CHARGE)
            invAdd(inv, CHAOS_RUNE, requested * CHAOS_PER_CHARGE)
            return
        }

        val result = charges.addCharges(inventory, invSlot, requested, CHARGE_VAROBJ, MAX_CHARGES)
        if (result.isFailure()) {
            invAdd(inv, DEATH_RUNE, requested * DEATH_PER_CHARGE)
            invAdd(inv, CHAOS_RUNE, requested * CHAOS_PER_CHARGE)
            invAdd(inv, FIRE_RUNE, requested * FIRE_PER_CHARGE)
            invAdd(inv, COINS, requested * COINS_PER_CHARGE)
            return
        }

        val added = if (requested == 1) "a charge" else "${requested.formatAmount} charges"
        mes("You add $added to the Trident of the Seas. New total: ${result.total.formatAmount}")
    }

    private fun ProtectedAccess.maxChargesFromInventory(): Int {
        val death = invTotal(inv, DEATH_RUNE) / DEATH_PER_CHARGE
        val chaos = invTotal(inv, CHAOS_RUNE) / CHAOS_PER_CHARGE
        val fire = invTotal(inv, FIRE_RUNE) / FIRE_PER_CHARGE
        val coins = invTotal(inv, COINS) / COINS_PER_CHARGE
        return minOf(death, chaos, fire, coins)
    }

    private suspend fun ProtectedAccess.uncharge(inventory: Inventory, invSlot: Int) {
        val trident = inventory[invSlot] ?: return
        val count = currentCharges(trident)

        val confirmation =
            choice2(
                "Okay, uncharge it.",
                true,
                "No, don't uncharge it.",
                false,
                title = "You will NOT get the coins back.",
            )
        if (!confirmation) {
            return
        }

        val refunded =
            if (trident.isType(FULL)) {
                inventory[invSlot] = InvObj(UNCHARGED)
                count
            } else {
                charges.removeAllCharges(inventory, invSlot, CHARGE_VAROBJ)
            }

        invAddOrDrop(objRepo, DEATH_RUNE, refunded * DEATH_PER_CHARGE)
        invAddOrDrop(objRepo, CHAOS_RUNE, refunded * CHAOS_PER_CHARGE)
        invAddOrDrop(objRepo, FIRE_RUNE, refunded * FIRE_PER_CHARGE)
        mes("You uncharge your Trident of the Seas.")
    }

    private companion object {
        const val FULL = "obj.tots"
        const val CHARGED = "obj.tots_charged"
        const val UNCHARGED = "obj.tots_uncharged"
        val TRIDENTS = listOf(FULL, CHARGED, UNCHARGED)

        const val DEATH_RUNE = "obj.deathrune"
        const val CHAOS_RUNE = "obj.chaosrune"
        const val FIRE_RUNE = "obj.firerune"
        const val COINS = "obj.coins"
        val MATERIALS = listOf(DEATH_RUNE, CHAOS_RUNE, FIRE_RUNE, COINS)

        const val CHARGE_VAROBJ = "varobj.charges_16383"
        const val MAX_CHARGES = 2_500

        const val DEATH_PER_CHARGE = 1
        const val CHAOS_PER_CHARGE = 1
        const val FIRE_PER_CHARGE = 5
        const val COINS_PER_CHARGE = 10
    }
}
