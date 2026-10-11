package org.rsmod.content.other.containers

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.script.*
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object ShadeCoffins {
    const val BROKEN = "obj.shades_coffin_broken"
    const val DAMPE = "npc.shades_coffin_keeper"
    val tiers = listOf("bronze", "steel", "black", "silver", "gold")
    val locks = tiers.map { "obj.shades_lock_$it" }
    fun lockFor(coffin: String): String? = tiers.firstOrNull { coffin.removeSuffix("_open") == "obj.shades_coffin_$it" }?.let { "obj.shades_lock_$it" }
    fun hasCoffin(access: ProtectedAccess): Boolean = listOf(access.inv, access.worn).plus(access.player.invMap["inv.bank"]?.let(::listOf).orEmpty()).any { inventory -> inventory.any { obj -> obj != null && (obj.id == BROKEN.asRSCM() || ContainerCatalog.byId[obj.id]?.family == "shade_coffin") } }
}

internal class ShadeCoffinScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(ShadeCoffins.DAMPE) {
            when (menu("Dampe", hotkeys = true, choices = listOf("Get a broken coffin", "Attach locks", "Cancel"))) {
                0 -> getCoffin()
                1 -> chooseLock()
            }
        }
        onOpNpc3(ShadeCoffins.DAMPE) { chooseLock() }
        onOpNpcU(ShadeCoffins.DAMPE) {
            val lock = ShadeCoffins.locks.firstOrNull { symbol -> symbol.asRSCM() == it.objType.id }
            if (lock != null) attach(lock) else if (it.objType.id == ShadeCoffins.BROKEN.asRSCM()) chooseLock() else mes("Bring Dampe a broken coffin and some locks.")
        }
        for (lock in ShadeCoffins.locks) for (coffin in listOf(ShadeCoffins.BROKEN) + ContainerCatalog.all.filter { it.family == "shade_coffin" }.map { it.symbol }) onOpHeldU(coffin, lock) {
            mes("You don't know how to attach these locks. Dampe can help.")
        }
    }
    private fun ProtectedAccess.getCoffin() {
        if (player.hitpoints <= 0 || player.pendingLogout || player.loggingOut) return
        if (ShadeCoffins.hasCoffin(this)) { mes("You already have a coffin."); return }
        if (invAdd(inv, ShadeCoffins.BROKEN, 1).success) mes("Dampe gives you a broken coffin. Bring him locks from the catacombs.") else mes("You need an empty inventory space.")
    }
    private suspend fun ProtectedAccess.chooseLock() {
        val locks = ShadeCoffins.locks.filter { inv.physicalCount(it) > 0 }
        if (locks.isEmpty()) { mes("Bring Dampe locks from the catacombs."); return }
        if (locks.size == 1) { attach(locks.single()); return }
        val index = menu("Choose locks", hotkeys = true, choices = locks.map { it.removePrefix("obj.shades_lock_").replaceFirstChar(Char::titlecase) + " locks" } + "Cancel")
        if (index in locks.indices) attach(locks[index])
    }
    private fun ProtectedAccess.attach(lock: String) {
        if (player.hitpoints <= 0 || player.pendingLogout || player.loggingOut || inv.physicalCount(ShadeCoffins.BROKEN) < 1 || inv.physicalCount(lock) < 1) { mes("You need a broken coffin and unnoted locks."); return }
        val coffin = "obj.shades_coffin_" + lock.removePrefix("obj.shades_lock_")
        val result = player.invTransaction(inv) { val i = select(inv); delete(i, ShadeCoffins.BROKEN.asRSCM(), 1); delete(i, lock.asRSCM(), 1); add(i, coffin.asRSCM(), 1) }
        if (result.success) mes("Dampe attaches the locks to your coffin.") else mes("You need space in your inventory.")
    }
}
