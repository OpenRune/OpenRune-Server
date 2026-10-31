package org.rsmod.content.skills.farming

import dev.openrune.types.ItemServerType
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.output.spam
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeldU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ProduceContainersScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1("obj.sack_empty") { fillEmpty(it.slot) }
        for (form in FarmingProduceContainers.forms) {
            if (form.count > 0) {
                onOpHeld1(form.obj) { fill(it.slot, form) }
                if (form.count > 1) onOpHeld2(form.obj) { withdraw(it.slot, form, 1) }
                onOpHeld3(form.obj) { withdraw(it.slot, form, form.count) }
            }
            if (form.count < FarmingProduceContainers.capacity(form.produce.id)) {
                onOpHeldU(form.obj, form.produce) {
                    transform(it.firstSlot, form, taken = 1, takenSlot = it.secondSlot)
                    // UNVERIFIED: the same text for a partly filled basket (guess)
                    fruit(form)?.let { name -> player.spam("You put ${article(name)} $name in the fruit basket.") }
                }
            }
        }
    }

    private fun ProtectedAccess.fillEmpty(slot: Int) {
        val form = FarmingProduceContainers.sackFillChoices.firstOrNull { count(it.produce) > 0 } ?: return
        fill(slot, form)
    }

    private fun ProtectedAccess.fill(slot: Int, form: ContainerForm) {
        val space = FarmingProduceContainers.capacity(form.produce.id) - form.count
        val amount = minOf(count(form.produce), space)
        if (amount > 0) {
            transform(slot, form, taken = amount)
        } else if (space == 0) {
            fruit(form)?.let { mes("The $it basket is already full.") }
        }
    }

    private fun ProtectedAccess.withdraw(slot: Int, form: ContainerForm, requested: Int) {
        val amount = minOf(requested, inv.freeSpace())
        if (amount <= 0) {
            mes("You don't have enough inventory space.")
            return
        }
        transform(slot, form, given = amount)
        if (requested == 1) {
            fruit(form)?.let { player.spam("You take ${article(it)} $it out of the $it basket.") }
        }
    }

    private fun fruit(form: ContainerForm): String? {
        val full = FarmingProduceContainers.fullContainer(form.produce.internalName) ?: return null
        return full.removePrefix("obj.basket_").takeIf { it != full }?.substringBeforeLast('_')
    }

    private fun article(word: String): String = if (word.first() in "aeiou") "an" else "a"

    private fun ProtectedAccess.count(produce: ItemServerType): Int =
        inv.physicalCount(produce.internalName)

    private fun ProtectedAccess.transform(
        slot: Int,
        form: ContainerForm,
        taken: Int = 0,
        takenSlot: Int? = null,
        given: Int = 0,
    ) {
        val next = FarmingProduceContainers.form(form.produce.id, form.count + taken - given)
        player.invTransaction(inv) {
            val target = select(inv)
            delete(target, form.obj.id, 1, slot)
            add(target, checkNotNull(next).obj.id, 1, slot = slot)
            if (taken > 0) delete(target, form.produce.id, taken, takenSlot)
            if (given > 0) add(target, form.produce.id, given)
        }
    }
}
