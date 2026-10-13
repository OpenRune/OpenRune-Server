package org.rsmod.content.other.gravestones

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.player.input.PlayerInput
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.ui.IfOverlayButton
import org.rsmod.api.player.worn.HeldEquipResult
import org.rsmod.api.script.onApNpc1
import org.rsmod.api.script.onApNpc3
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.other.gravestones.Gravestones.Companion.INTERFACE
import org.rsmod.content.other.gravestones.Gravestones.Companion.TIMER
import org.rsmod.content.other.gravestones.Gravestones.Companion.TIMER_INTERVAL
import org.rsmod.game.entity.Npc
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GravestoneScript @Inject constructor(private val graves: Gravestones) : PluginScript() {
    override fun ScriptContext.startup() {
        // A grave opens from up to 7 tiles away with line of sight
        // (https://oldschool.runescape.wiki/w/Grave).
        for (style in GRAVE_STYLES) {
            onOpNpc1(style) { if (owns(it.npc)) openGrave() }
            onApNpc1(style) { approach(it.npc) { openGrave() } }
            // UNVERIFIED: Loot takes what Take-All in the free section would.
            onOpNpc3(style) { if (owns(it.npc)) take(freeSlots()) }
            onApNpc3(style) { approach(it.npc) { take(freeSlots()) } }
        }
        onPlayerLogin {
            if (player.graveCoords != null) {
                // live capture, rev 241.
                // UNVERIFIED: whether items in Death's Office alone also warn, and which setting
                // turns it off.
                // TODO: Live sends it after the welcome and "Did you know?" messages, which the
                // login module sends later, on `EngineLogin`.
                player.mes(LOGIN_WARNING)
                graves.spawn(player)
                if (player.graveDuration > 0) {
                    player.softTimer(TIMER, TIMER_INTERVAL)
                }
            }
        }
        onPlayerLogout { graves.despawn(player) }
        onEvent<PlayerInput> { player.lastInput = player.currentMapClock }
        onPlayerSoftTimer(TIMER) { graves.tick(player) }
        onIfClose(INTERFACE) {
            player.stopInvTransmit(player.grave)
            // live capture, rev 241.
            player.runClientScript(TRANSMIT_DATA.asRSCM(RSCMType.CLIENTSCRIPT), -1, -1)
        }
        for (button in TIMER_BUTTONS) {
            onIfOverlayButton(button) { timerInfo() }
        }
        onIfOverlayButton(FREE_ITEMS) { takeOrExamine(it) }
        onIfOverlayButton(PAY_ITEMS) { takeOrExamine(it) }
        onIfOverlayButton(FREE_BUTTON) { take(freeSlots()) }
        onIfOverlayButton(PAY_BUTTON) { if (graves.fee(player) > 0) pay() else take(paySlots()) }
        onIfOverlayButton(CLOSE_BUTTON) { graves.close(player) }
    }

    private suspend fun ProtectedAccess.approach(npc: Npc, action: suspend ProtectedAccess.() -> Unit) {
        if (!owns(npc)) {
            return
        }
        if (!isWithinDistance(npc, ACCESS_RANGE)) {
            apRange(ACCESS_RANGE)
            return
        }
        action()
    }

    private fun ProtectedAccess.owns(npc: Npc): Boolean =
        player.graveNpc == npc.uid

    /** Live capture, rev 241. */
    private fun ProtectedAccess.openGrave() {
        graves.sendFeeThresholds(player)
        invTransmit(player.grave)
        ifOpenOverlay(INTERFACE)
        ifSetEvents(FREE_ITEMS, ITEM_SLOTS, IfEvent.Op1, IfEvent.Op10)
        ifSetEvents(PAY_ITEMS, ITEM_SLOTS, IfEvent.Op1, IfEvent.Op10, IfEvent.Depth2)
        sendFee()
    }

    // TODO: Items can be discarded from the grave into the coffer to lower the fee
    // (https://oldschool.runescape.wiki/w/Items_Kept_on_Death).
    private fun ProtectedAccess.sendFee() {
        val coffer = player.deathCoffer
        runClientScript(TRANSMIT_DATA.asRSCM(RSCMType.CLIENTSCRIPT), graves.fee(player), coffer)
    }

    private fun ProtectedAccess.takeOrExamine(button: IfOverlayButton) {
        when (button.op) {
            IfButtonOp.Op1 -> takeSlot(button.comsub)
            IfButtonOp.Op10 -> player.grave[button.comsub]?.let { mes(getInvObj(it).examine) }
            else -> Unit
        }
    }

    private fun ProtectedAccess.takeSlot(slot: Int) {
        if (slot !in ITEM_SLOTS || player.grave[slot] == null) {
            return
        }
        if (graves.isPaySlot(player, slot) && graves.fee(player) > 0) {
            // UNVERIFIED: the message for taking an item before paying.
            mes("You need to pay the fee before you can take that item.")
            return
        }
        take(listOf(slot))
    }

    private fun ProtectedAccess.freeSlots(): List<Int> =
        ITEM_SLOTS.filter { player.grave[it] != null && !graves.isPaySlot(player, it) }

    private fun ProtectedAccess.paySlots(): List<Int> =
        ITEM_SLOTS.filter { graves.isPaySlot(player, it) }

    private fun ProtectedAccess.pay() {
        val fee = graves.fee(player)
        if (fee <= 0) {
            return
        }
        val source = payDeathFee(fee)
        if (source == null) {
            // UNVERIFIED: the message when nothing can cover the fee.
            mes("You don't have enough coins with you or in your bank to pay the fee.")
            return
        }
        mes(deathFeeMessage(source, fee))
        player.feePaid = true
        sendFee()
    }

    /**
     * Moves the objs in [slots] out of the grave, equipable ones first ("equipable items are now
     * prioritised", https://oldschool.runescape.wiki/w/Grave, 14 February 2019), each back in the
     * inventory slot it held before the death when that slot is free. With "Auto-equip worn items
     * from gravestone" on, whatever was worn goes back on (live capture, rev 241, which also
     * re-equipped the kept items), first so the kept items free the inventory slots the grave's
     * objs came from.
     */
    private fun ProtectedAccess.take(slots: List<Int>) {
        val grave = player.grave
        val ordered = slots.sortedByDescending { slot -> grave[slot]?.let { getInvObj(it).isEquipable } }
        if (player.autoEquip) {
            reequipWorn()
        }
        var noSpace = false
        for (slot in ordered) {
            if (!moveToInv(slot)) {
                noSpace = true
            }
        }
        if (player.autoEquip) {
            reequipWorn()
        }
        if (noSpace) {
            // UNVERIFIED: the message when the inventory fills up.
            mes("You don't have enough inventory space.")
        }
        if (grave.isEmpty()) {
            // live capture, rev 241.
            mes("You successfully retrieved everything from your gravestone.")
            graves.close(player)
            graves.remove(player)
            return
        }
        sendFee()
    }

    private fun ProtectedAccess.moveToInv(slot: Int): Boolean {
        val obj = player.grave[slot] ?: return true
        val type = getInvObj(obj)
        val single = obj.count == 1 || (type.isStackable && obj.id !in player.inv.map { it?.id })
        val intoSlot = if (single) GraveLayout.invSlot(player, obj.id) else null
        val result =
            invMoveFromSlot(
                from = player.grave,
                into = player.inv,
                fromSlot = slot,
                count = obj.count,
                intoSlot = intoSlot,
            )
        if (result.failure) {
            return false
        }
        if (intoSlot != null) {
            GraveLayout.consumeInv(player, intoSlot)
        }
        return true
    }

    private fun ProtectedAccess.reequipWorn() {
        for (wornSlot in 0 until GraveLayout.WORN_SLOT_COUNT) {
            val obj = GraveLayout.wornObj(player, wornSlot) ?: continue
            if (player.worn[wornSlot] != null) {
                continue
            }
            val invSlot = player.inv.indexOfFirst { it?.id == obj }
            if (invSlot == -1) {
                continue
            }
            if (invEquip(invSlot) is HeldEquipResult.Success) {
                GraveLayout.consumeWorn(player, wornSlot)
            }
        }
    }

    /**
     * The grave timer on the HUD (live capture, rev 241): the time
     * left is the ticks left in whole seconds.
     */
    private fun ProtectedAccess.timerInfo() {
        if (player.graveCoords == null) {
            return
        }
        mes(
            "A <col=ef1020>gravestone</col> contains some of your recently dropped items; you can " +
                "retrieve them by visiting it. The <col=ef1020>World Map</col> will show its " +
                "location if the area has a map."
        )
        val seconds = player.graveDuration * 6 / 10
        // UNVERIFIED: the singular forms, and the wording under a minute.
        val left = "${seconds / 60} minutes, ${seconds % 60} seconds"
        mes(
            "In <col=ef1020>$left</col> it will expire, after which your items can be retrieved " +
                "from <col=ef1020>Death's Office</col> instead."
        )
    }

    private companion object {
        const val ACCESS_RANGE = 7
        val TIMER_BUTTONS =
            listOf(
                "component.toplevel:gravestone",
                "component.toplevel_osm:gravestone",
                "component.toplevel_osrs_stretch:gravestone",
                "component.toplevel_pre_eoc:gravestone",
            )
        const val LOGIN_WARNING =
            "@mes_hl_red@You have items stored in an item retrieval service. Please visit the " +
                "last location you died. If you die again before retrieving them, they will be " +
                "lost.</col>"

        val GRAVE_STYLES = listOf("npc.gravestone_default", "npc.gravestone_angel")
        val ITEM_SLOTS = 0 until 120

        const val FREE_ITEMS = "component.gravestone_generic:freeitems"
        const val PAY_ITEMS = "component.gravestone_generic:payitems"
        const val FREE_BUTTON = "component.gravestone_generic:freebutton"
        const val PAY_BUTTON = "component.gravestone_generic:paybutton"
        const val CLOSE_BUTTON = "component.gravestone_generic:close"
        const val TRANSMIT_DATA = "clientscript.[clientscript,gravestone_transmit_data]"
    }
}
