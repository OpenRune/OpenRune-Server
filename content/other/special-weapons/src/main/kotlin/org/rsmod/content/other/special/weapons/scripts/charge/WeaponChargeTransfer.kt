package org.rsmod.content.other.special.weapons.scripts.charge

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.invtx.*
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

/** Recipes for the existing native Shadow and Venator item charge layouts. */
internal enum class WeaponChargeTransfer(
    val charged: String,
    val empty: String,
    private val varobj: String,
    val maximum: Int,
    private val materials: List<Pair<String, Int>>,
) {
    SHADOW("obj.tumekens_shadow", "obj.tumekens_shadow_uncharged", "varobj.tumeken_charges", 20_000,
        listOf("obj.chaosrune" to 5, "obj.soulrune" to 2)),
    VENATOR("obj.venator_bow", "obj.venator_bow_uncharged", "varobj.venator_bow_charges", 50_000,
        listOf("obj.ancient_essence" to 1));

    private val bits get() = checkNotNull(ServerCacheManager.getVarObj(varobj.asRSCM())).bits

    fun accepts(item: InvObj): Boolean = item.id == charged.asRSCM() || item.id == empty.asRSCM()

    fun count(item: InvObj): Int {
        require(accepts(item))
        return if (item.id == empty.asRSCM()) 0 else item.vars.getBits(bits).coerceAtMost(maximum)
    }

    fun transfer(player: Player, inventory: Inventory, slot: Int, original: InvObj, amount: Int, refund: Boolean): Boolean {
        // The reference check rejects a replaced item even if its id and charges match.
        if (inventory[slot] !== original || !accepts(original) || amount <= 0) return false
        val before = count(original)
        if (refund && amount != before || !refund && amount > maximum - before) return false
        val after = if (refund) 0 else before + amount
        val replacement = original.copy(id = (if (after == 0) empty else charged).asRSCM(), vars = original.vars.withBits(bits, after))
        return player.invTransaction(inventory) {
            val inv = select(inventory)
            delete(inv, original.id, 1, slot = slot)
            add(inv, replacement.id, 1, vars = replacement.vars, slot = slot)
            for ((material, perCharge) in materials) {
                if (refund) add(inv, material.asRSCM(), amount * perCharge)
                else delete(inv, material.asRSCM(), amount * perCharge)
            }
        }.success
    }
}
