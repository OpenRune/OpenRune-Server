package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.startInvTransmit
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.script.*
import org.rsmod.game.inv.InvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class SeedBoxScript @Inject constructor(private val store: ContainerStore, private val box: SeedBox) : PluginScript() {
    override fun ScriptContext.startup() {
        for (form in SeedBox.forms) onOpHeld3(form) { if (inv[it.slot]?.id == form.asRSCM() && valid()) open() }
        onIfOpen(MAIN) { if (box.owns(player)) player.startInvTransmit(player.invMap.getOrPut(DISPLAY)) }
        onIfClose(MAIN) {
            player.invMap[DISPLAY]?.let { player.stopInvTransmit(it); it.fillNulls() }
            val contents = box.seeds(player)
            player.mes(if (contents.isEmpty()) "The seed box is empty." else contents.joinToString("; ", prefix = "Seed box: ") { "${ServerCacheManager.getItem(it)!!.name}: ${box.count(player, it)}" })
        }
        onIfModalButton("component.hosidius_seedbox:seed_layer") { withdraw(it) }
        onIfModalButton("component.hosidius_seedbox:fill_button") { if (valid()) { for (item in inv.filterNotNull().toList()) store.store(player, SeedBox.spec, item.id, item.count); refresh() } }
        onIfModalButton("component.hosidius_seedbox:empty_button") { if (valid()) { for (id in box.seeds(player)) store.withdraw(player, SeedBox.spec, id, Int.MAX_VALUE, inv); refresh() } }
    }
    private fun ProtectedAccess.valid() = box.owns(player) && player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut
    private fun ProtectedAccess.open() { refresh(false); ifOpenMainModal(MAIN); ifSetEvents("component.hosidius_seedbox:seed_layer", 0..17, IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4, IfEvent.Op5, IfEvent.Op6, IfEvent.Op10); for (button in listOf("fill_button", "empty_button")) ifSetEvents("component.hosidius_seedbox:$button", -1..0, IfEvent.Op1); runClientScript(DRAW) }
    private fun ProtectedAccess.refresh(draw: Boolean = true) {
        val display = player.invMap.getOrPut(DISPLAY); display.fillNulls()
        for ((slot, id) in box.seeds(player).withIndex()) display[slot] = InvObj(ServerCacheManager.getItem(id)!!, box.count(player, id))
        invTransmit(display); invTransmit(inv)
        if (draw && player.ui.containsModal(MAIN)) runClientScript(DRAW)
    }
    private suspend fun ProtectedAccess.withdraw(event: IfModalButton) {
        if (!valid() || !player.ui.containsModal(MAIN) || event.comsub !in 0..17 || event.comsub % 3 == 0) return
        val item = player.invMap[DISPLAY]?.get(event.comsub / 3) ?: return
        if (event.obj?.id?.let { it != item.id } == true) return
        if (event.op == IfButtonOp.Op10) { mes(ServerCacheManager.getItem(item.id)!!.examine); return }
        if (event.op == IfButtonOp.Op6) { box.first(player, item.id); refresh(); return }
        val origin = coords; val order = box.seeds(player)
        val amount = when (event.op) { IfButtonOp.Op1 -> 1; IfButtonOp.Op2 -> 5; IfButtonOp.Op3 -> 10; IfButtonOp.Op4 -> countDialog(); IfButtonOp.Op5 -> Int.MAX_VALUE; else -> return }
        ifCloseChat()
        if (amount <= 0 || !valid() || coords != origin || !player.ui.containsModal(MAIN) || box.seeds(player) != order || player.invMap[DISPLAY]?.get(event.comsub / 3) !== item) return
        store.withdraw(player, SeedBox.spec, item.id, amount, inv); refresh()
    }
    companion object {
        const val MAIN = "interface.hosidius_seedbox"
        const val DISPLAY = "inv.seed_box"
        const val DRAW = 45551
    }
}
