package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.invtx.*
import org.rsmod.api.player.events.interact.HeldDropEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseRangedLvl
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.worn.DizanasQuiver
import org.rsmod.api.script.*
import org.rsmod.api.script.advanced.onDestroyHeld
import org.rsmod.api.script.advanced.onWearposChange
import org.rsmod.content.interfaces.bank.BankChargeConfigure
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class DizanasQuiverScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onWearposChange { if (wearpos == Wearpos.Back) syncChargedFlag(player) }
        onPlayerLogin { syncChargedFlag(player) }
        for ((charged, _) in DizanasQuiver.pairs) onProtectedEvent<BankChargeConfigure>(charged.asRSCM()) { configureBank(it) }
        for (symbol in DizanasQuiver.usable) {
            onOpHeld3(symbol) { if (active()) open(inv, it.slot) }
            if (DizanasQuiver.pairs.any { it.first == symbol }) {
                onOpHeld4(symbol) { if (active()) uncharge(inv, it.slot) }
                onOpHeld5(symbol) { if (active()) AmmoContainers.emptyQuiver(player) }
                onOpWorn2(symbol) { if (active()) check(worn, it.slot) }
            } else {
                onOpHeld4(symbol) { if (active()) AmmoContainers.emptyQuiver(player) }
                onOpHeld5(symbol) {
                    if (!active()) return@onOpHeld5
                    val original = inv[it.slot]; val origin = coords
                    val confirmed = confirmDestroy(symbol, 1, it.type.param(params.destroy_note_title), it.type.param(params.destroy_note_desc))
                    if (confirmed && active() && coords == origin && inv[it.slot] === original && player.invDel(inv, it.obj.id, 1, slot = it.slot).success) {
                        publish(HeldDropEvents.Destroy(player, it.slot, it.obj, symbol))
                    }
                }
            }
            onOpHeldU(symbol) {
                if (!active() || inv[it.firstSlot]?.id != symbol.asRSCM() || inv[it.secondSlot]?.id != it.second.id) return@onOpHeldU
                if (MAX_CAPES.any { cape -> it.second.id == cape.asRSCM() }) {
                    if (symbol == "obj.dizanas_quiver_infinite") combineMaxCape(it.firstSlot, it.secondSlot)
                    else mes("Use an unlocked blessed Dizana's quiver to create Dizana's max cape.")
                    return@onOpHeldU
                }
                if (it.second.id == SPLINTER.asRSCM()) charge(it.firstSlot, it.secondSlot) else {
                    val moved = AmmoContainers.fillQuiver(player, inv, it.secondSlot)
                    mes(if (moved > 0) "You add $moved ammunition to your quiver." else "You must empty your quiver before storing a different ammunition type.")
                }
            }
            for (shrine in listOf("loc.ralos_shrine", "loc.ralos_shrine_taur01")) onOpLocU(shrine, symbol) { bless(it.invSlot) }
        }
        for (cape in MAX_CAPES) onOpHeldU(cape, "obj.dizanas_quiver_infinite") { combineMaxCape(it.secondSlot, it.firstSlot) }
        onIfModalButton(AMMO_COMPONENT) {
            if (active() && player.ui.containsModal(MAIN) && it.op == IfButtonOp.Op1 && it.comsub in -1..0 && it.obj?.id?.let { id -> id == DizanasQuiver.stored(player)?.id } != false) AmmoContainers.emptyQuiver(player)
        }
        onIfOpen("interface.wornitems") {
            player.ifSetEvents(EXTRA, -1..0, IfEvent.Op1, IfEvent.Op2, IfEvent.Op10)
            player.ifSetEvents("component.wornitems:extra_ammo_slot", -1..1, IfEvent.Op1)
        }
        onIfOverlayButton(EXTRA) {
            if (!active() || it.comsub !in -1..0 || it.obj?.id?.let { id -> id == DizanasQuiver.stored(player)?.id } == false) return@onIfOverlayButton
            when (it.op) {
                IfButtonOp.Op1 -> AmmoContainers.emptyQuiver(player)
                IfButtonOp.Op2 -> {
                    if (!DizanasQuiver.recognises(worn[Wearpos.Back.slot]) || player.baseRangedLvl < 75) { mes("You must wear Dizana's quiver to use its extra ammo slot."); return@onIfOverlayButton }
                    if (DizanasQuiver.stored(player) == null) {
                        if (AmmoContainers.fillQuiver(player, worn, Wearpos.Quiver.slot) == 0) mes("You have nothing in your worn quiver to fill your Dizana's quiver with.")
                    } else AmmoContainers.swap(player, DizanasQuiver.stored(player)) { old -> DizanasQuiver.store(player, old) }
                }
                IfButtonOp.Op10 -> DizanasQuiver.stored(player)?.let { ammo -> mes(ServerCacheManager.getItem(ammo.id)!!.examine) }
                else -> Unit
            }
        }
        onDestroyHeld { if (DizanasQuiver.usable.any { it.asRSCM() == type.asRSCM() } && !DizanasQuiver.owns(player) && player.invMap["inv.bank"]?.any(DizanasQuiver::recognises) != true) DizanasQuiver.store(player, null) }
        onOpNpc4("npc.anma_assistant") {
            val origin = coords
            if (menu("Ava's devices", "Upgrade my Dizana's quiver", "Cancel") == 0 && coords == origin) upgradeRecovery()
        }
        for (symbol in DizanasQuiver.usable) onOpNpcU(
            ServerCacheManager.getNpc("npc.anma_assistant".asRSCM())!!,
            ServerCacheManager.getItem(symbol.asRSCM())!!,
        ) { upgradeRecovery() }
    }
    private fun syncChargedFlag(player: org.rsmod.game.entity.Player) {
        val cape = player.worn[Wearpos.Back.slot]
        val charged = DizanasQuiver.recognises(cape) && (DizanasQuiver.blessed(cape) || DizanasQuiver.charges(cape).count > 0)
        VarPlayerIntMapSetter.set(player, "varbit.worn_items_dizanas_quiver_charged", if (charged)1 else 0)
    }
    private suspend fun ProtectedAccess.configureBank(event: BankChargeConfigure) {
        fun valid(open: Boolean = true) = AmmoContainers.active(player) && (!open || player.ui.containsModal("interface.bankmain")) && player.vars["varbit.bank_bankops_toggle_on"] == 1 && bank[event.slot] === event.item
        if (!valid()) return
        if (event.item.count != 1) { mes("Withdraw one quiver before configuring its charges."); return }
        val origin = coords; val before = DizanasQuiver.charges(event.item)
        val selected = menu("Configure Dizana's quiver", "Add charges from bank", "Uncharge and reset blessing progress", "Cancel")
        if (selected !in 0..1 || !valid(false) || coords != origin) return
        ifOpenMainSidePair("interface.bankmain", "interface.bankside")
        when (selected) {
            0 -> {
                val sourceSlot = bank.indexOfFirst { it?.id == SPLINTER.asRSCM() && it.vars == 0 }
                val source = if (sourceSlot >= 0) bank[sourceSlot] else null
                val maximum = minOf(source?.count ?: 0, DizanasQuiver.MAX_CHARGES - before.count)
                if (maximum <= 0) { mes("Your quiver is full or you have no sunfire splinters in your bank."); return }
                val requested = countDialog("How many charges? (0 - $maximum)")
                if (!valid() || coords != origin || bank[sourceSlot] !== source) return
                val amount = minOf(requested, maximum); if (amount <= 0) return
                val replacement = DizanasQuiver.withCharges(event.item, before.copy(count = before.count + amount, progress = minOf(DizanasQuiver.BLESSING, before.progress + amount)))
                with(event.script) { if (consumeChargeStack(event.slot, replacement, SPLINTER.asRSCM(), amount)) mes("You add $amount charges to your banked quiver.") }
            }
            1 -> {
                if (!choice2("Uncharge", true, "Cancel", false, title = "Return charges to your bank and reset blessing progress?")) return
                if (!valid(false) || coords != origin) return
                ifOpenMainSidePair("interface.bankmain", "interface.bankside")
                val replacement = DizanasQuiver.withCharges(event.item, DizanasQuiver.Charges(0, 0))
                if (!with(event.script) { refundChargeStack(event.slot, replacement, SPLINTER.asRSCM(), before.count) }) { mes("Your bank has no space for all the returned splinters."); return }
                mes("Your quiver is uncharged; its blessing progress is reset.")
            }
        }
    }
    private suspend fun ProtectedAccess.combineMaxCape(quiverSlot: Int, capeSlot: Int) {
        if (!active()) return
        val quiver = inv[quiverSlot]?.takeIf { it.id == "obj.dizanas_quiver_infinite".asRSCM() } ?: return
        val cape = inv[capeSlot]?.takeIf { item -> MAX_CAPES.any { item.id == it.asRSCM() } } ?: return
        val hoodSlot = inv.indexOfFirst { it?.id == "obj.skillcape_max_hood".asRSCM() }
        if (hoodSlot < 0) { mes("You also need a max hood in your inventory."); return }
        val hood = inv[hoodSlot] ?: return
        val origin = coords
        if (!choice2("Combine", true, "Cancel", false, title = "Create Dizana's max cape and hood?")) return
        if (!active() || coords != origin || inv[quiverSlot] !== quiver || inv[capeSlot] !== cape || inv[hoodSlot] !== hood) return
        val result = player.invTransaction(inv) {
            val i = select(inv)
            delete(i, quiver.id, 1, slot = quiverSlot); delete(i, cape.id, 1, slot = capeSlot); delete(i, hood.id, 1, slot = hoodSlot)
            add(i, "obj.skillcape_max_dizanas".asRSCM(), 1); add(i, "obj.skillcape_max_hood_dizanas".asRSCM(), 1)
        }
        mes(if (result.success) "You create Dizana's max cape and hood." else "The capes could not be combined.")
    }
    private fun ProtectedAccess.active() = AmmoContainers.active(player) && DizanasQuiver.owns(player)
    private fun ProtectedAccess.open(inventory: Inventory, slot: Int) {
        if (!DizanasQuiver.recognises(inventory[slot])) return
        ifOpenMainModal(MAIN); ifSetEvents(AMMO_COMPONENT, -1..0, IfEvent.Op1)
        val item = inventory[slot]; val charges = DizanasQuiver.charges(item)
        ifSetText("component.dizanas_quiver:charges_text", if (DizanasQuiver.blessed(item)) "Blessed: unlimited charges" else "Charges: ${charges.count}<br>Blessing: ${charges.progress / 1500}%")
    }
    private fun ProtectedAccess.check(inventory: Inventory, slot: Int) {
        val item = inventory[slot]; if (!DizanasQuiver.recognises(item)) return
        val charges = DizanasQuiver.charges(item)
        mes(if (DizanasQuiver.blessed(item)) "Your blessed quiver has unlimited charges." else "Your quiver has ${charges.count} charges and ${charges.progress / 1500}% progress towards blessing.")
    }
    private suspend fun ProtectedAccess.charge(quiverSlot: Int, sourceSlot: Int) {
        val item = inv[quiverSlot] ?: return
        if (DizanasQuiver.pairs.none { item.id == it.first.asRSCM() || item.id == it.second.asRSCM() }) return
        val input = inv[sourceSlot]?.takeIf { it.id == SPLINTER.asRSCM() && it.vars == 0 } ?: return
        val before = DizanasQuiver.charges(item)
        val maximum = minOf(input.count, DizanasQuiver.MAX_CHARGES - before.count)
        if (maximum <= 0) { mes("Dizana's quiver is already fully charged."); return }
        val origin = coords; val requested = countDialog("How many charges do you wish to add? (0 - $maximum)")
        if (!active() || coords != origin || inv[quiverSlot] !== item || inv[sourceSlot] !== input) return
        val moved = minOf(requested, maximum)
        if (moved <= 0 || player.invDel(inv, input.id, moved, slot = sourceSlot).failure) return
        inv[quiverSlot] = DizanasQuiver.withCharges(item, before.copy(count = before.count + moved, progress = minOf(DizanasQuiver.BLESSING, before.progress + moved)))
        check(inv, quiverSlot)
    }
    private fun ProtectedAccess.upgradeRecovery() {
        if (!active()) return
        if (!QuestRequirements.hasCompleted(player, "quest_animalmagnetism")) { mes("Complete Animal Magnetism before upgrading your quiver."); return }
        val recovery = (inv.asSequence() + worn.asSequence()).filterNotNull().maxOfOrNull { ServerCacheManager.getItem(it.id)!!.paramOrNull(params.ammo_recovery_rate) ?: 0 } ?: 0
        val level = if (recovery >= 80) 2 else if (recovery >= 72) 1 else 0
        if (level == 0) { mes("Bring an Ava's accumulator, assembler or equivalent cape with your quiver."); return }
        val before = player.vars["varbit.dizanas_quiver_ammo_save"]
        if (level > before) { VarPlayerIntMapSetter.set(player, "varbit.dizanas_quiver_ammo_save", level); mes("Ava upgrades all your quivers without consuming your device.") } else mes("Your quivers already have this ammunition-saving effect.")
    }
    private suspend fun ProtectedAccess.uncharge(inventory: Inventory, slot: Int) {
        val item = inventory[slot] ?: return; val origin = coords
        if (!choice2("Yes", true, "No", false, title = "Uncharge? This resets progress towards blessing.")) return
        if (!active() || inventory[slot] !== item || coords != origin) return
        val count = DizanasQuiver.charges(item).count
        if (count > 0 && player.invAdd(inv, SPLINTER.asRSCM(), count).failure) { mes("You need more inventory space."); return }
        inventory[slot] = DizanasQuiver.withCharges(item, DizanasQuiver.Charges(0, 0))
    }
    private suspend fun ProtectedAccess.bless(slot: Int) {
        val item = inv[slot]?.takeIf { q -> DizanasQuiver.pairs.any { q.id == it.first.asRSCM() || q.id == it.second.asRSCM() } } ?: return
        val origin = coords; val fee = DizanasQuiver.BLESSING - DizanasQuiver.charges(item).progress
        if (!choice2("Bless my quiver", true, "Cancel", false, title = "Bless for $fee sunfire splinters?")) return
        if (!active() || inv[slot] !== item || coords != origin) return
        val locked = item.id == DizanasQuiver.pairs[1].first.asRSCM() || item.id == DizanasQuiver.pairs[1].second.asRSCM()
        val output = if (locked) "obj.dizanas_quiver_infinite_trouver" else "obj.dizanas_quiver_infinite"
        val result = player.invTransaction(inv) { val i = select(inv); delete(i, item.id, 1, slot = slot); if (fee > 0) delete(i, SPLINTER.asRSCM(), fee); add(i, output.asRSCM(), 1, slot = slot) }
        mes(if (result.success) "Ralos blesses your quiver with permanent Sunfire." else "You do not have enough sunfire splinters.")
    }
    companion object {
        const val MAIN = "interface.dizanas_quiver"
        const val AMMO_COMPONENT = "component.dizanas_quiver:ammo_obj"
        const val EXTRA = "component.wornitems:extra_quiver_ammo"
        const val SPLINTER = "obj.sunfiresplinter"
        private val MAX_CAPES = listOf("obj.skillcape_max", "obj.skillcape_max_worn")
    }
}
