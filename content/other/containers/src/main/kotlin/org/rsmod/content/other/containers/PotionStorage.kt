package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.ironman.isUltimateIronman
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.interfaces.bank.BankDepositStorage
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

internal object PotionStorageCatalog {
    data class Family(val enum: Int, val group: String, val counter: String, val size: String?, val doses: Map<Int, Int>) {
        val maximum = doses.keys.max()
        val id = doses.getValue(maximum)
        fun dose(id: Int) = doses.entries.firstOrNull { it.value == id && it.key > 0 }?.key ?: 0
    }
    val all: List<Family> by lazy { javaClass.getResourceAsStream("/potion-storage.tsv")!!.bufferedReader().useLines { lines -> lines.drop(1).map { line -> val v = line.split('\t'); val enum = ServerCacheManager.getEnum(v[0].toInt())!!; Family(enum.id, v[1], v[2], v[3].takeIf { it != "-" && it.isNotBlank() }, enum.values.mapValues { (it.value as Number).toInt() }.filter { it.key > 0 && it.value >= 0 }) }.toList() } }
    fun byItem(id: Int) = all.firstOrNull { it.dose(id) > 0 }
}

/** Real permanent native counters are also the data source for the existing bank CS2. */
internal class PotionStorage @Inject constructor() : BankDepositStorage {
    fun unlocked(player: Player) = player.vars[UNLOCKED] != 0 && !player.isUltimateIronman
    fun count(player: Player, family: PotionStorageCatalog.Family) = player.vars[family.counter].coerceAtLeast(0)
    fun vials(player: Player) = player.vars[VIALS].coerceAtLeast(0)
    fun size(player: Player, family: PotionStorageCatalog.Family) = family.size?.let { (player.vars[it] + 1).coerceIn(1, family.maximum) } ?: 1
    fun setSize(player: Player, family: PotionStorageCatalog.Family, size: Int) { require(size in 1..family.maximum); family.size?.let { VarPlayerIntMapSetter.set(player, it, size - 1) } }
    private fun setCount(player: Player, family: PotionStorageCatalog.Family, count: Int) = VarPlayerIntMapSetter.set(player, family.counter, count)
    private fun setVials(player: Player, count: Int) = VarPlayerIntMapSetter.set(player, VIALS, count)
    override fun accepts(access: ProtectedAccess, inventory: Inventory, slot: Int): Boolean {
        val obj = inventory[slot] ?: return false
        if (!unlocked(access.player) || access.player.hitpoints <= 0 || access.player.pendingLogout || access.player.loggingOut || !access.player.ui.containsModal("interface.bankmain") || (access.player.vars["varbit.bank_currenttab"] != 15 && access.player.vars["varbit.bank_depositpotion"] == 0) || obj.vars != 0) return false
        val type = ServerCacheManager.getItem(obj.id)!!; val id = if (type.certtemplate != 0)type.certlink else obj.id
        return id == VIAL.asRSCM() || PotionStorageCatalog.byItem(id) != null
    }
    override fun deposit(access: ProtectedAccess, inventory: Inventory, slot: Int, count: Int): Int = with(access) {
        if (!accepts(this, inventory, slot) || count <= 0) return@with 0
        val input = inventory[slot]!!; val type = ServerCacheManager.getItem(input.id)!!; val id = if (type.certtemplate != 0)type.certlink else input.id
        val family = PotionStorageCatalog.byItem(id); val dose = family?.dose(id) ?: 0
        val before = family?.let { count(player, it) } ?: 0
        val moved = minOf(count, input.count, Int.MAX_VALUE - vials(player), if (dose == 0)Int.MAX_VALUE else (Int.MAX_VALUE - before) / dose)
        if (moved <= 0 || player.invDel(inventory, input.id, moved, slot = slot).failure) return@with 0
        if (family != null) { if (before == 0)setSize(player, family, dose); setCount(player, family, before + moved * dose) }
        setVials(player, vials(player) + moved); moved
    }
    fun ProtectedAccess.withdraw(family: PotionStorageCatalog.Family, requested: Int, notes: Boolean): Int {
        if (!unlocked(player) || requested <= 0) return 0
        var total = count(player, family); var availableVials = vials(player); if (availableVials == 0) { mes("You don't have any vials."); return 0 }
        val dose = size(player, family); val full = minOf(total / dose, requested, availableVials)
        var moved = 0
        if (full > 0) { val result = player.invAdd(inv, family.doses.getValue(dose), full, strict = false, cert = notes); if (result.failure) return 0; moved = result.completed(); total -= moved * dose; availableVials -= moved }
        if (moved == full && moved < requested && total in 1 until dose && availableVials > 0 && player.invAdd(inv, family.doses.getValue(total), 1, cert = notes).success) { moved++; total = 0; availableVials-- }
        if (moved > 0) { setCount(player, family, total); setVials(player, availableVials) }; return moved
    }
    fun ProtectedAccess.withdrawVials(requested: Int, notes: Boolean): Int {
        if (!unlocked(player) || requested <= 0 || vials(player) == 0) return 0
        val result = player.invAdd(inv, VIAL.asRSCM(), minOf(requested, vials(player)), strict = false, cert = notes)
        val moved = if (result.success)result.completed() else 0; if (moved > 0)setVials(player, vials(player) - moved); return moved
    }

    /** Direct container transfers move doses only; they neither create nor consume physical vials. */
    fun takeDoses(player: Player, id: Int, requested: Int): Int {
        if (!unlocked(player) || requested <= 0) return 0
        val family = PotionStorageCatalog.byItem(id) ?: return 0; val before = count(player, family); val moved = minOf(before, requested)
        if (moved > 0)setCount(player, family, before - moved); return moved
    }
    fun putDoses(player: Player, id: Int, requested: Int): Int {
        if (!unlocked(player) || requested <= 0) return 0
        val family = PotionStorageCatalog.byItem(id) ?: return 0; val before = count(player, family); val moved = minOf(requested, Int.MAX_VALUE - before)
        if (moved > 0) { if (before == 0)setSize(player, family, family.maximum); setCount(player, family, before + moved) }; return moved
    }
    fun favourites(player: Player) = (1..8).map { player.vars["varp.potionstore_favourite_$it"] }.filter { id -> PotionStorageCatalog.all.any { it.doses[1] == id } }.distinct()
    fun favourite(player: Player, family: PotionStorageCatalog.Family): Boolean {
        val id = family.doses.getValue(1); val faves = favourites(player).toMutableList()
        if (!faves.remove(id)) { if (faves.size == 8)return false; faves += id }
        for (slot in 1..8)VarPlayerIntMapSetter.set(player, "varp.potionstore_favourite_$slot", faves.getOrElse(slot - 1) { -1 }); return true
    }

    /** Native rows have five children even when hidden. Header vial links follow optional backing rectangles. */
    fun vialSlots(player: Player): Set<Int> {
        val faves = favourites(player); val groups = listOf(faves.size, PotionStorageCatalog.all.count { it.group == "potion" && it.doses[1] !in faves && count(player, it) > 0 }, PotionStorageCatalog.all.count { it.group == "unfinished" && it.doses[1] !in faves && count(player, it) > 0 })
        var child = PotionStorageCatalog.all.size * 5; val result = mutableSetOf<Int>()
        for (count in groups)if (count > 0) { if (count % 4 == 1)child++; result += child + 4; child += 5 }; return result
    }
    companion object { const val UNLOCKED = "varbit.mm_potion_storage_unlocked"; const val VIALS = "varp.potionstore_vials"; const val VIAL = "obj.vial_empty" }
}
