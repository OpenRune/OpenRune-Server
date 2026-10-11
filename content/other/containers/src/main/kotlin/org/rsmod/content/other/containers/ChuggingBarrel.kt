package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

internal class ChuggingBarrel @Inject constructor() {
    data class Slot(val id: Int, val count: Int)
    fun owns(player: Player) = player.inv.any { it?.id == ITEM.asRSCM() }
    fun contents(player: Player): List<Slot?> {
        val raw = player.attr[CONTENTS].orEmpty(); val used = HashSet<Int>()
        return (0..4).map { i ->
            val id = raw.getOrNull(i * 2); val count = raw.getOrNull(i * 2 + 1)
            if (id != null && count != null && count in 1..100 && PotionFamilies.byId(id) != null && used.add(id)) Slot(id, count) else null
        }
    }
    fun write(player: Player, slots: List<Slot?>) {
        require(slots.size == 5 && slots.filterNotNull().all { it.count in 1..100 && PotionFamilies.byId(it.id) != null })
        require(slots.filterNotNull().map { it.id }.distinct().size == slots.filterNotNull().size)
        if (slots.all { it == null }) player.attr.remove(CONTENTS) else player.attr[CONTENTS] = slots.flatMap { listOf(it?.id ?: -1, it?.count ?: 0) }
    }
    fun clear(player: Player) = player.attr.remove(CONTENTS)
    fun ProtectedAccess.store(inputSlot: Int, bottles: Int, familyWide: Boolean = false, maximumDoses: Int = Int.MAX_VALUE): Int {
        if (!owns(player) || bottles <= 0 || maximumDoses <= 0) return 0
        val original = inv[inputSlot] ?: return 0
        val family = PotionFamilies.byItem(original.id) ?: return 0
        if (original.vars != 0) return 0
        var remaining = bottles; var moved = 0
        for (sourceSlot in inv.indices) {
            if (remaining <= 0 || moved >= maximumDoses) break
            val input = inv[sourceSlot] ?: continue
            if (input.vars != 0 || if (familyWide) family.dose(input.id) == 0 else input.id != original.id) continue
            val contents = contents(player).toMutableList(); val target = contents.indexOfFirst { it?.id == family.id }.takeIf { it >= 0 } ?: contents.indexOfFirst { it == null }
            if (target == -1) break
            val before = contents[target]?.count ?: 0; val amount = minOf(family.dose(input.id), 100 - before, maximumDoses - moved)
            if (amount == 0) break
            val replacement = family.doses[family.dose(input.id) - amount]?.let { ServerCacheManager.getItem(it) }
            val result = if (replacement == null) player.invDel(inv, input.id, 1, slot = sourceSlot) else invReplace(inv, sourceSlot, 1, replacement)
            if (result.failure) continue
            contents[target] = Slot(family.id, before + amount); write(player, contents); moved += amount; remaining--
        }
        return moved
    }
    fun ProtectedAccess.withdraw(slot: Int, requested: Int, target: Inventory = inv): Int {
        if (!owns(player) || requested <= 0 || slot !in 0..4) return 0
        var remaining = requested; var moved = 0
        while (remaining > 0) {
            val contents = contents(player).toMutableList(); val item = contents[slot] ?: break
            val family = PotionFamilies.byId(item.id)!!; val dose = minOf(item.count, family.maximum, remaining)
            if (player.invAdd(target, family.doses.getValue(dose), 1).failure) break
            contents[slot] = if (item.count == dose) null else item.copy(count = item.count - dose)
            write(player, contents); moved += dose; remaining -= dose
        }
        return moved
    }
    fun swap(player: Player, source: Int, target: Int) { if (source !in 0..4 || target !in 0..4) return; val items = contents(player).toMutableList(); val original = items[source]; items[source] = items[target]; items[target] = original; write(player, items) }
    companion object {
        const val ITEM = "obj.mm_prepot_device"
        const val DISASSEMBLED = "obj.mm_prepot_device_disassembled"
        const val DISPLAY = "inv.prepot_device_inv"
        val CONTENTS = AttributeKey<List<Int>>(persistenceKey = "chugging_barrel_contents")
    }
}
