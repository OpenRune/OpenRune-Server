package org.rsmod.content.skills.smithing.coalbag

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.events.interact.HeldDropEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.script.advanced.onDestroyHeld
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeld5
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CoalBagScript : PluginScript() {

    override fun ScriptContext.startup() {
        listOf("obj.coal_bag", "obj.coal_bag_open").forEach { bag ->
            onOpHeld1(bag) { if (canOperate()) fillBag() }
            onOpHeld3(bag) { if (canOperate()) checkBag() }
            onOpHeld4(bag) { if (canOperate()) emptyBag() }
            onOpHeld5(bag) {
                if (!canOperate()) return@onOpHeld5
                if (CoalBag.storedAmount(player) > 0) { mes("You must empty the coal bag before destroying it."); return@onOpHeld5 }
                val original = inv[it.slot]; val origin = coords
                val confirmed = confirmDestroy(bag, 1, it.type.param(params.destroy_note_title), it.type.param(params.destroy_note_desc))
                if (confirmed && canOperate() && coords == origin && inv[it.slot] === original && CoalBag.storedAmount(player) == 0 && invDel(inv, bag, 1, slot = it.slot).success) {
                    publish(HeldDropEvents.Destroy(player, it.slot, it.obj, bag))
                }
            }
        }

        onOpHeld2("obj.coal_bag") { if (canOperate()) invReplaceSlot(inv, it.slot, 1, ServerCacheManager.getItem("obj.coal_bag_open".asRSCM())!!) }
        onOpHeld2("obj.coal_bag_open") { if (canOperate()) invReplaceSlot(inv, it.slot, 1, ServerCacheManager.getItem("obj.coal_bag".asRSCM())!!) }
        onDestroyHeld { if (type in setOf("obj.coal_bag", "obj.coal_bag_open") && !CoalBag.owns(player) && player.invMap["inv.bank"]?.any { it?.id in listOf("obj.coal_bag".asRSCM(), "obj.coal_bag_open".asRSCM()) } != true) CoalBag.clear(player) }
    }
    private fun ProtectedAccess.canOperate() = CoalBag.owns(player) && player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut

    private fun ProtectedAccess.fillBag() {
        val freeSpace = CoalBag.freeSpace(player)
        if (freeSpace <= 0) {
            mes("Your coal bag is full.")
            return
        }

        val coalInInv = inv.physicalCount("obj.coal")
        if (coalInInv <= 0) {
            mes("You don't have any coal to fill your coal bag with.")
            return
        }

        val toDeposit = minOf(coalInInv, freeSpace)
        if (invDel(inv, "obj.coal", toDeposit, ignoreVirtualStorage = true).success) {
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
}
