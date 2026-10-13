package org.rsmod.content.other.nerocurator

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.events.interact.HeldDropEvents
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.worn.DizanasQuiver
import org.rsmod.api.script.*
import org.rsmod.api.script.advanced.onWearposChange
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Only the new completionist forms register item handlers; existing quiver content keeps its owner. */
class CompletionistQuiverScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin { sync(player) }
        onWearposChange { if (wearpos == Wearpos.Back) sync(player) }
        for (symbol in ITEMS) {
            onOpHeld3(symbol) { if (active()) open(inv, it.slot) }
            onOpHeld4(symbol) { if (active()) CompletionistAmmo.emptyQuiver(player) }
            onOpHeld5(symbol) {
                if (!active()) return@onOpHeld5
                val item = inv[it.slot] ?: return@onOpHeld5; val origin = coords
                if (confirmDestroy(symbol, 1, "Destroy your Completionist cape?", "The cape and its ingredients will not be returned.") && active() && coords == origin && inv[it.slot] === item && player.invDel(inv, item.id, 1, slot = it.slot).success) publish(HeldDropEvents.Destroy(player, it.slot, item, symbol))
            }
            onOpHeldU(symbol) {
                if (active()) {
                    val amount = CompletionistAmmo.fillQuiver(player, inv, it.secondSlot)
                    mes(if (amount > 0) "You add $amount ammunition to your quiver." else "Empty your quiver before storing a different ammunition type.")
                }
            }
            onOpWorn2(symbol) { if (active()) open(worn, it.slot) }
            onOpNpcU(ServerCacheManager.getNpc("npc.anma_assistant".asRSCM())!!, ServerCacheManager.getItem(symbol.asRSCM())!!) { upgradeRecovery() }
        }
        onIfModalButton(AMMO) {
            if (active() && player.ui.containsModal(MAIN) && it.op == IfButtonOp.Op1 && it.comsub in -1..0 && it.obj?.id?.let { id -> id == DizanasQuiver.stored(player)?.id } != false) CompletionistAmmo.emptyQuiver(player)
        }
        onIfOpen("interface.wornitems") { player.ifSetEvents(EXTRA, -1..0, IfEvent.Op1, IfEvent.Op2, IfEvent.Op10) }
        onIfOverlayButton(EXTRA) {
            if (!active() || it.comsub !in -1..0 || it.obj?.id?.let { id -> id == DizanasQuiver.stored(player)?.id } == false) return@onIfOverlayButton
            when (it.op) {
                IfButtonOp.Op1 -> CompletionistAmmo.emptyQuiver(player)
                IfButtonOp.Op2 -> {
                    if (!ITEMS.any { worn[Wearpos.Back.slot]?.id == it.asRSCM() })return@onIfOverlayButton
                    val stored = DizanasQuiver.stored(player)
                    if (stored == null) CompletionistAmmo.fillQuiver(player, worn, Wearpos.Quiver.slot)
                    else CompletionistAmmo.swap(player, stored) { DizanasQuiver.store(player, it) }
                }
                IfButtonOp.Op10 -> DizanasQuiver.stored(player)?.let { mes(ServerCacheManager.getItem(it.id)!!.examine) }
                else -> Unit
            }
        }
    }
    private fun sync(player: org.rsmod.game.entity.Player) {
        val cape = player.worn[Wearpos.Back.slot]
        val charged = DizanasQuiver.recognises(cape) && (DizanasQuiver.blessed(cape) || DizanasQuiver.charges(cape).count > 0)
        VarPlayerIntMapSetter.set(player, "varbit.worn_items_dizanas_quiver_charged", if (charged)1 else 0)
    }
    private fun ProtectedAccess.active() = CompletionistAmmo.active(player) && (inv.asSequence() + worn.asSequence()).any { item -> ITEMS.any { item?.id == it.asRSCM() } }
    private fun ProtectedAccess.open(inventory: Inventory, slot: Int) {
        if (!ITEMS.any { inventory[slot]?.id == it.asRSCM() })return
        ifOpenMainModal(MAIN); ifSetEvents(AMMO, -1..0, IfEvent.Op1)
        ifSetText("component.dizanas_quiver:charges_text", "Blessed: unlimited charges")
    }
    private fun ProtectedAccess.upgradeRecovery() {
        if (!active())return
        if (!QuestRequirements.hasCompleted(player, "quest_animalmagnetism")) { mes("Complete Animal Magnetism before upgrading your quiver."); return }
        val recovery = (inv.asSequence() + worn.asSequence()).filterNotNull().maxOfOrNull { ServerCacheManager.getItem(it.id)!!.paramOrNull(params.ammo_recovery_rate) ?: 0 } ?: 0
        val level = if (recovery >= 80)2 else if (recovery >= 72)1 else 0
        if (level == 0) { mes("Bring an Ava's accumulator or assembler with your quiver."); return }
        if (level > player.vars["varbit.dizanas_quiver_ammo_save"]) { VarPlayerIntMapSetter.set(player, "varbit.dizanas_quiver_ammo_save", level); mes("Ava upgrades your quivers without consuming your device.") } else mes("Your quivers already have this ammunition-saving effect.")
    }
    companion object {
        val ITEMS = listOf("obj.nero_completionist_dizana", "obj.nero_completionist_dizana_locked")
        const val MAIN = "interface.dizanas_quiver"
        const val AMMO = "component.dizanas_quiver:ammo_obj"
        const val EXTRA = "component.wornitems:extra_quiver_ammo"
    }
}
