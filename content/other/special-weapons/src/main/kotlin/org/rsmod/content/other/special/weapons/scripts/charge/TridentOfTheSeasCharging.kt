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
        val name = if (obj.isType(UNCHARGED)) "Uncharged trident" else NAME
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
            mesbox(
                "You need one death rune, one chaos rune, five fire runes and ten coins to " +
                    "charge the $name."
            )
            return
        }

        val room = MAX_CHARGES - currentCharges(trident)
        if (room <= 0) {
            mes("Your $NAME is already fully charged.")
            return
        }

        val max = min(room, maxByMaterials)
        val requested =
            min(countDialog("How many charges would you like to add? (0 - ${max.formatAmount})"), max)
        if (requested <= 0) {
            return
        }

        applyCharge(inventory, invSlot, requested)
    }

    suspend fun ProtectedAccess.applyCharge(
        inventory: Inventory,
        invSlot: Int,
        requested: Int,
    ) {
        val currentSlot = inventory[invSlot]
        if (currentSlot == null || !currentSlot.isType(UNCHARGED) && !currentSlot.isType(CHARGED)) {
            return
        }
        if (requested > min(MAX_CHARGES - currentCharges(currentSlot), maxChargesFromInventory())) {
            return
        }

        val death = requested * DEATH_PER_CHARGE
        val chaos = requested * CHAOS_PER_CHARGE
        val fire = requested * FIRE_PER_CHARGE
        val coins = requested * COINS_PER_CHARGE

        val removeRunes = invDel(inv, DEATH_RUNE, death, CHAOS_RUNE, chaos)
        if (removeRunes.failure) {
            return
        }
        val removeRest = invDel(inv, FIRE_RUNE, fire, COINS, coins)
        if (removeRest.failure) {
            invAddOrDrop(objRepo, DEATH_RUNE, death)
            invAddOrDrop(objRepo, CHAOS_RUNE, chaos)
            return
        }

        val result = charges.addCharges(inventory, invSlot, requested, CHARGE_VAROBJ, MAX_CHARGES)
        if (result.isFailure()) {
            invAddOrDrop(objRepo, DEATH_RUNE, death)
            invAddOrDrop(objRepo, CHAOS_RUNE, chaos)
            invAddOrDrop(objRepo, FIRE_RUNE, fire)
            invAddOrDrop(objRepo, COINS, coins)
            return
        }

        val added = if (requested == 1) "a charge" else "${requested.formatAmount} charges"
        mesbox("You add $added to the $NAME. New total: ${result.total.formatAmount}")
    }

    private fun ProtectedAccess.maxChargesFromInventory(): Int {
        val death = invTotal(inv, DEATH_RUNE) / DEATH_PER_CHARGE
        val chaos = invTotal(inv, CHAOS_RUNE) / CHAOS_PER_CHARGE
        val fire = invTotal(inv, FIRE_RUNE) / FIRE_PER_CHARGE
        val coins = invTotal(inv, COINS) / COINS_PER_CHARGE
        return minOf(death, chaos, fire, coins)
    }

    private suspend fun ProtectedAccess.uncharge(inventory: Inventory, invSlot: Int) {
        if (inventory[invSlot] == null) {
            return
        }

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

        applyUncharge(inventory, invSlot)
    }

    fun ProtectedAccess.applyUncharge(inventory: Inventory, invSlot: Int) {
        val trident = inventory[invSlot]
        if (trident == null || !trident.isType(FULL) && !trident.isType(CHARGED)) {
            return
        }

        val refunded =
            if (trident.isType(FULL)) {
                inventory[invSlot] = InvObj(UNCHARGED)
                MAX_CHARGES
            } else {
                charges.removeAllCharges(inventory, invSlot, CHARGE_VAROBJ)
            }

        invAddOrDrop(objRepo, DEATH_RUNE, refunded * DEATH_PER_CHARGE)
        invAddOrDrop(objRepo, CHAOS_RUNE, refunded * CHAOS_PER_CHARGE)
        invAddOrDrop(objRepo, FIRE_RUNE, refunded * FIRE_PER_CHARGE)
        mes("You uncharge your $NAME.")
    }

    private companion object {
        const val NAME = "Trident of the seas"
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
