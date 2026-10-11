package org.rsmod.content.areas.revenants

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import org.rsmod.api.invtx.*
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

internal object RevenantCharges {
    const val ETHER = "obj.wild_cave_shard"
    const val BRACELET = "obj.wild_cave_bracelet_charged"
    const val EMPTY_BRACELET = "obj.wild_cave_bracelet_uncharged"
    private val bits get() = checkNotNull(ServerCacheManager.getVarObj("varobj.revenant_ether".asRSCM())).bits
    private val absorptionBits get() = checkNotNull(ServerCacheManager.getVarObj("varobj.revenant_absorption".asRSCM())).bits
    data class Kind(val charged: String, val empty: String, val max: Int = 17_000, val deposit: Int = 1_000, val dismantle: Int = 7_500)
    val bracelet = Kind(BRACELET, EMPTY_BRACELET, 16_000, 0, 250)
    val weapons = listOf("chainmace", "bow", "sceptre", "sceptre_recol", "webweaver", "ursine", "accursed", "accursed_recol").map { name ->
        val base = name.removeSuffix("_recol"); val suffix = if (name.endsWith("_recol")) "_recol" else ""
        Kind("obj.wild_cave_${base}_charged$suffix", "obj.wild_cave_${base}_uncharged$suffix")
    }
    val kinds = listOf(bracelet) + weapons
    fun kind(item: InvObj) = kinds.firstOrNull { item.id == it.charged.asRSCM() || item.id == it.empty.asRSCM() }
    fun count(item: InvObj): Int = kind(item)?.let { item.vars.getBits(bits).coerceIn(0, it.max) } ?: 0
    fun absorbing(item: InvObj): Boolean = item.vars.getBits(absorptionBits) == 1
    fun withCount(item: InvObj, count: Int): InvObj {
        val kind = requireNotNull(kind(item)); require(count in 0..kind.max)
        return item.copy(id = (if (count == 0) kind.empty else kind.charged).asRSCM(), vars = item.vars.withBits(bits, count))
    }
    fun toggle(item: InvObj) = item.copy(vars = item.vars.withBits(absorptionBits, if (absorbing(item)) 0 else 1))
    fun protected(player: Player): Boolean = player.worn[Wearpos.Hands.slot]?.let { kind(it) == bracelet && count(it) > 0 } == true
    fun absorb(player: Player, amount: Int): Int {
        val slot = Wearpos.Hands.slot; val item = player.worn[slot] ?: return amount
        if (kind(item) != bracelet || !absorbing(item)) return amount
        val added = minOf(amount, bracelet.max - count(item))
        if (added > 0) player.worn[slot] = withCount(item, count(item) + added)
        return amount - added
    }
    fun reduceDamage(player: Player, damage: Int): Int {
        val slot = Wearpos.Hands.slot; val item = player.worn[slot] ?: return damage
        if (kind(item) != bracelet || count(item) == 0) return damage
        player.worn[slot] = withCount(item, count(item) - 1)
        return damage / 4
    }
    fun transfer(player: Player, inventory: Inventory, slot: Int, item: InvObj, amount: Int, refund: Boolean): Boolean {
        val kind = kind(item) ?: return false
        val count = count(item)
        if (inventory[slot] !== item || amount <= 0 || (!refund && amount > kind.max - count) || (refund && amount > count)) return false
        val result = player.invTransaction(inventory, player.inv) {
            val target = select(inventory); val bag = if (inventory === player.inv) target else select(player.inv)
            val replacement = withCount(item, if (refund) count - amount else count + amount)
            delete(target, item.id, item.count, slot = slot)
            add(target, replacement.id, replacement.count, vars = replacement.vars, slot = slot)
            if (refund) add(bag, ETHER.asRSCM(), amount) else delete(bag, ETHER.asRSCM(), amount)
        }
        return result.success
    }
}
