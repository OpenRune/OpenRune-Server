package org.rsmod.content.other.containers

import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class ProduceContainerScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (spec in ProduceContainers.all) {
            onOpHeld1(spec.symbol) { fill(spec, it.slot) }
            if (spec.quantity > 1) onOpHeld2(spec.symbol) { empty(spec, it.slot, 1) }
            if (spec.quantity > 0) onOpHeld3(spec.symbol) { empty(spec, it.slot, spec.quantity) }
            onOpHeldU(spec.symbol) {
                if (inv[it.firstSlot]?.id != spec.id || inv[it.secondSlot]?.id != it.second.id) return@onOpHeldU
                val matched = ProduceContainers.filled(spec.empty, it.second.id)
                if (matched == null || spec.item != null && spec.item != matched.item) { mes("That doesn't belong in this container."); return@onOpHeldU }
                fill(spec, it.firstSlot, matched)
            }
        }
    }
    private fun ProtectedAccess.fill(spec: ProduceContainer, slot: Int, chosen: ProduceContainer? = null) {
        if (inv[slot]?.id != spec.id) return
        val target = chosen ?: if (spec.item != null) spec else ProduceContainers.firstFill(spec.empty).firstOrNull { inv.physicalCount(it.item!!) > 0 }
        if (target == null) { mes("You have no suitable produce to fill it."); return }
        val n = minOf(spec.capacity - spec.quantity, inv.physicalCount(target.item!!))
        if (n <= 0) { mes("The container is full."); return }
        val result = player.invTransaction(inv) {
            val i = select(inv)
            delete(i, spec.id, 1, slot = slot)
            delete(i, target.item.asRSCM(), n)
            add(i, target.form(spec.quantity + n).asRSCM(), 1, slot = slot)
        }
        if (result.success) mes("You add $n items to the container.")
    }
    private fun ProtectedAccess.empty(spec: ProduceContainer, slot: Int, requested: Int) {
        if (inv[slot]?.id != spec.id || spec.item == null) return
        for (n in minOf(requested, spec.quantity) downTo 1) {
            val result = player.invTransaction(inv, autoCommit = false) {
                val i = select(inv)
                delete(i, spec.id, 1, slot = slot)
                add(i, spec.form(spec.quantity - n).asRSCM(), 1, slot = slot)
                add(i, spec.item.asRSCM(), n)
            }
            if (result.success) { result.commitAll(); return }
        }
        mes("Your inventory is too full to remove any produce.")
    }
}
