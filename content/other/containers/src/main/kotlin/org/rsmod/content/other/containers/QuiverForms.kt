package org.rsmod.content.other.containers

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.hook.PlayerObjTakeStorageHook
import org.rsmod.api.player.worn.DizanasQuiver
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.obj.Obj

internal data class QuiverFamily(val key: String, val unlocked: String, val locked: String, val broken: String, val lockedBroken: String, val mangled: String, val repairFee: Int, val deepCoins: Int) {
    val chargeable get() = key == "basic"
    val symbols get() = listOf(unlocked, locked, broken, lockedBroken, mangled) + if (chargeable) DizanasQuiver.pairs.map { it.first } else emptyList()
    fun locked(item: InvObj) = item.id in listOf(locked, lockedBroken, mangled).map { it.asRSCM() } || chargeable && item.id == DizanasQuiver.pairs[1].first.asRSCM()
    fun broken(item: InvObj) = item.id in listOf(broken, lockedBroken, mangled).map { it.asRSCM() }
    fun charges(item: InvObj): DizanasQuiver.Charges = if (chargeable) DizanasQuiver.charges(item.copy(id = unlocked.asRSCM())) else DizanasQuiver.Charges(0, 0)
    fun usable(item: InvObj, locked: Boolean): InvObj {
        if (!chargeable) return item.copy(id = (if (locked) this.locked else unlocked).asRSCM())
        return DizanasQuiver.withCharges(item.copy(id = (if (locked) this.locked else unlocked).asRSCM()), charges(item))
    }
    fun repairCost(item: InvObj) = if (item.id == mangled.asRSCM()) 500000 else repairFee
    fun lost(item: InvObj, deep: Boolean): InvObj? {
        val locked = locked(item)
        if (deep && !locked) return null
        val output = if (locked) (if (deep) mangled else lockedBroken) else broken
        val charges = charges(item)
        val vars = if (chargeable) DizanasQuiver.withCharges(InvObj(unlocked), DizanasQuiver.Charges(0, charges.progress - charges.count)).vars else item.vars
        return item.copy(id = output.asRSCM(), vars = vars)
    }
}

internal object QuiverForms {
    val all = listOf(
        QuiverFamily("basic", "obj.dizanas_quiver_uncharged", "obj.dizanas_quiver_uncharged_trouver", "obj.dizanas_quiver_broken", "obj.dizanas_quiver_trouver_broken", "obj.dizanas_quiver_trouver_mangled", 270000, 9600),
        QuiverFamily("blessed", "obj.dizanas_quiver_infinite", "obj.dizanas_quiver_infinite_trouver", "obj.dizanas_quiver_infinite_broken", "obj.dizanas_quiver_infinite_trouver_broken", "obj.dizanas_quiver_infinite_trouver_mangled", 400000, 12000),
        QuiverFamily("max", "obj.skillcape_max_dizanas", "obj.skillcape_max_dizanas_trouver", "obj.skillcape_max_dizanas_broken", "obj.skillcape_max_dizanas_trouver_broken", "obj.skillcape_max_dizanas_trouver_mangled", 400000, 569250),
    )
    val symbols get() = all.flatMap { it.symbols }
    fun family(item: InvObj?) = item?.let { obj -> all.firstOrNull { family -> family.symbols.any { it.asRSCM() == obj.id } } }
    fun carried(player: Player) = (player.inv.asSequence() + player.worn.asSequence()).filterNotNull()
    fun stored(player: Player) = carried(player) + player.invMap["inv.bank"]?.asSequence().orEmpty().filterNotNull()
}

/** Paid locks are counted per family; destroying/reobtaining cannot create extra paid copies. */
internal object QuiverLocks {
    val KEY = AttributeKey<Map<String, Int>>(persistenceKey = "dizanas_quiver_trouver_locks")
    fun remembered(player: Player): Map<String, Int> = QuiverForms.all.associate { family ->
        val owned = QuiverForms.stored(player).filter { QuiverForms.family(it) == family && family.locked(it) }.sumOf { it.count }
        family.key to maxOf(player.attr[KEY]?.get(family.key) ?: 0, owned)
    }
    fun change(player: Player, before: Map<String, Int>, family: QuiverFamily, delta: Int) {
        player.attr[KEY] = before + (family.key to ((before[family.key] ?: 0) + delta).coerceAtLeast(0))
    }
    fun rememberRemoved(player: Player, item: InvObj) {
        val family = QuiverForms.family(item)?.takeIf { it.locked(item) } ?: return
        val owned = QuiverForms.stored(player).filter { QuiverForms.family(it) == family && family.locked(it) }.sumOf { it.count }
        val before = remembered(player)
        player.attr[KEY] = before + (family.key to maxOf(before[family.key] ?: 0, owned + item.count))
    }
    fun restore(player: Player) {
        val remembered = remembered(player)
        if (remembered.values.all { it == 0 }) return
        player.attr[KEY] = remembered
        for (family in QuiverForms.all) {
            var missing = (remembered[family.key] ?: 0) - QuiverForms.stored(player).filter { QuiverForms.family(it) == family && family.locked(it) }.sumOf { it.count }
            if (missing <= 0) continue
            for (inventory in listOf(player.inv, player.worn)) for (slot in inventory.indices) {
                val item = inventory[slot]?.takeIf { it.count == 1 && QuiverForms.family(it) == family && !family.locked(it) && !family.broken(it) } ?: continue
                if (missing <= 0) break
                inventory[slot] = family.usable(item, locked = true); missing--
                if (inventory === player.worn) player.rebuildAppearance()
            }
        }
    }
}

internal class QuiverPickupLocks @Inject constructor() : PlayerObjTakeStorageHook {
    override fun inventory(player: Player, obj: Obj, objType: ItemServerType): Inventory? = if (QuiverForms.symbols.any { it.asRSCM() == obj.type }) player.inv else null
    override fun committed(player: Player, obj: Obj, objType: ItemServerType) { QuiverLocks.restore(player) }
}
