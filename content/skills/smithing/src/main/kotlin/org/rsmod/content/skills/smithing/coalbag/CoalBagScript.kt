package org.rsmod.content.skills.smithing.coalbag

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CoalBagScript : PluginScript() {

    override fun ScriptContext.startup() {
        listOf("obj.coal_bag", "obj.coal_bag_open").forEach { bag ->
            onOpHeld1(bag) { fillBag() }
            onOpHeld3(bag) { checkBag() }
            onOpHeld4(bag) { emptyBag() }
        }

        onOpHeld2("obj.coal_bag") { openBag() }
        onOpHeld2("obj.coal_bag_open") { closeBag() }

    }

    private fun ProtectedAccess.fillBag() {
        val freeSpace = CoalBag.freeSpace(player)
        if (freeSpace <= 0) {
            mes("Your coal bag is full.")
            return
        }

        val coalInInv = inv.count("obj.coal")
        if (coalInInv <= 0) {
            mes("You don't have any coal to fill your coal bag with.")
            return
        }

        val toDeposit = minOf(coalInInv, freeSpace)
        if (invDel(inv, "obj.coal", toDeposit).success) {
            CoalBag.addStored(player, toDeposit)
            mes("You fill the coal bag with $toDeposit coal.")
        }
    }

    private fun ProtectedAccess.emptyBag() {
        val current = CoalBag.storedAmount(player)
        if (current <= 0) {
            mes("Your coal bag is empty.")
            return
        }

        val added = invAdd(inv, "obj.coal", current, strict = false, ignoreVirtualStorage = true).completed()
        if (added <= 0) {
            mes("You don't have any free space in your inventory.")
            return
        }

        CoalBag.removeStored(player, added)
        mes("You empty $added coal from the coal bag.")
    }

    private fun ProtectedAccess.checkBag() {
        val current = CoalBag.storedAmount(player)
        if (current <= 0) {
            mes("Your coal bag is empty.")
            return
        }

        val capacity = CoalBag.capacity(player)
        mes("Your coal bag contains $current of $capacity coal.")
    }

    private fun ProtectedAccess.openBag() {
        invReplace(inv, "obj.coal_bag", 1, "obj.coal_bag_open")
    }

    private fun ProtectedAccess.closeBag() {
        invReplace(inv, "obj.coal_bag_open", 1, "obj.coal_bag")
    }
}
