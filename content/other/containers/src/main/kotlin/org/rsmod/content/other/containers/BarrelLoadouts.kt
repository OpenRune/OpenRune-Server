package org.rsmod.content.other.containers

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.game.entity.Player

/** The native UI encodes a 100-dose limit as zero in twenty permanent eight-bit fields. */
internal object BarrelLoadoutCodec {
    fun read(player: Player, index: Int): List<ChuggingBarrel.Slot?> {
        require(index in 0..3)
        val seen = mutableSetOf<Int>()
        return (0..4).map { slot ->
            val id = player.vars["varp.prepot_device_loadout_${index}_slot_${slot}_potion"]
            val cap = player.vars["varbit.prepot_device_loadout_${index}_slot_${slot}_cap"]
            if (PotionFamilies.byId(id) == null || !seen.add(id)) null else ChuggingBarrel.Slot(id, if (cap == 0) 100 else cap.coerceIn(1, 100))
        }
    }
    fun write(player: Player, index: Int, slots: List<ChuggingBarrel.Slot?>) {
        require(index in 0..3 && slots.size == 5)
        require(slots.filterNotNull().all { PotionFamilies.byId(it.id) != null && it.count in 1..100 })
        require(slots.filterNotNull().map { it.id }.distinct().size == slots.filterNotNull().size)
        for ((slot, item) in slots.withIndex()) {
            VarPlayerIntMapSetter.set(player, "varp.prepot_device_loadout_${index}_slot_${slot}_potion", item?.id ?: -1)
            VarPlayerIntMapSetter.set(player, "varbit.prepot_device_loadout_${index}_slot_${slot}_cap", item?.count?.takeUnless { it == 100 } ?: 0)
        }
    }
}

internal class BarrelLoadouts @Inject constructor(private val barrel: ChuggingBarrel, private val bankScript: BankInvScript) {
    fun ProtectedAccess.deposit(slot: Int, requested: Int): Int {
        if (!barrel.owns(player) || requested <= 0 || slot !in 0..4) return 0
        var remaining = requested; var moved = 0
        while (remaining > 0) {
            val items = barrel.contents(player).toMutableList(); val item = items[slot] ?: break
            val family = PotionFamilies.byId(item.id)!!; val dose = minOf(item.count, family.maximum, remaining)
            val bottles = minOf(item.count, remaining) / dose
            val accepted = with(bankScript) { depositStoredStack(family.doses.getValue(dose), bottles) }
            if (accepted == 0) break
            val amount = accepted * dose
            items[slot] = if (item.count == amount) null else item.copy(count = item.count - amount)
            barrel.write(player, items); remaining -= amount; moved += amount
        }
        return moved
    }
    fun ProtectedAccess.fill(slot: Int, requested: Int): Int {
        if (!barrel.owns(player) || requested <= 0 || slot !in 0..4) return 0
        val original = barrel.contents(player)[slot] ?: return 0
        val family = PotionFamilies.byId(original.id)!!
        val source = inv.indexOfFirst { it != null && family.dose(it.id) > 0 }
        var moved = if (source >= 0) with(barrel) { store(source, Int.MAX_VALUE, true, minOf(requested, 100 - original.count)) } else 0
        var remaining = minOf(requested - moved, 100 - original.count - moved)
        for (dose in family.maximum downTo 1) {
            val id = family.doses.getValue(dose)
            while (remaining > 0) {
                val take = minOf(dose, remaining)
                if (with(bankScript) { withdrawStoredStack(id, 1) } != 1) break
                if (take < dose && with(bankScript) { depositStoredStack(family.doses.getValue(dose - take), 1) } != 1) {
                    // The original stack/placeholder or its freed slot can always accept the rollback.
                    check(with(bankScript) { depositStoredStack(id, 1) } == 1)
                    break
                }
                val items = barrel.contents(player).toMutableList(); val before = items[slot]!!
                items[slot] = before.copy(count = before.count + take); barrel.write(player, items)
                moved += take; remaining -= take
            }
        }
        return moved
    }
    fun ProtectedAccess.load(index: Int) {
        val desired = BarrelLoadoutCodec.read(player, index)
        fun returnExcess() {
            for ((slot, item) in barrel.contents(player).withIndex()) if (item != null) {
                val wanted = desired.firstOrNull { it?.id == item.id }?.count ?: 0
                deposit(slot, (item.count - wanted).coerceAtLeast(0))
            }
        }
        returnExcess()
        for (wanted in desired.filterNotNull()) {
            val carried = inv.indexOfFirst { it != null && PotionFamilies.byId(wanted.id)!!.dose(it.id) > 0 }
            val missing = wanted.count - (barrel.contents(player).firstOrNull { it?.id == wanted.id }?.count ?: 0)
            if (carried >= 0 && missing > 0) with(barrel) { store(carried, Int.MAX_VALUE, true, missing) }
            var items = barrel.contents(player).toMutableList()
            val slot = items.indexOfFirst { it?.id == wanted.id }.takeIf { it >= 0 } ?: items.indexOfFirst { it == null }
            if (slot == -1) continue
            if (items[slot] == null) {
                // Install the first dose only after a real bank withdrawal succeeds.
                val family = PotionFamilies.byId(wanted.id)!!
                for (dose in family.maximum downTo 1) {
                    val take = minOf(dose, wanted.count)
                    if (with(bankScript) { withdrawStoredStack(family.doses.getValue(dose), 1) } == 0) continue
                    if (take < dose && with(bankScript) { depositStoredStack(family.doses.getValue(dose - take), 1) } != 1) {
                        check(with(bankScript) { depositStoredStack(family.doses.getValue(dose), 1) } == 1); continue
                    }
                    items[slot] = ChuggingBarrel.Slot(wanted.id, take); barrel.write(player, items); break
                }
            }
            val count = barrel.contents(player)[slot]?.takeIf { it.id == wanted.id }?.count ?: continue
            fill(slot, (wanted.count - count).coerceAtLeast(0))
        }
        returnExcess()
        val current = barrel.contents(player).filterNotNull()
        val ordered = desired.map { wanted -> current.firstOrNull { wanted?.id == it.id } }.toMutableList()
        for (item in current.filter { item -> desired.none { it?.id == item.id } }) ordered[ordered.indexOfFirst { it == null }] = item
        barrel.write(player, ordered)
        mes(if (barrel.contents(player) == desired) "You load your chugging barrel loadout." else "The bank could not supply or accept every dose in this loadout.")
    }
}
