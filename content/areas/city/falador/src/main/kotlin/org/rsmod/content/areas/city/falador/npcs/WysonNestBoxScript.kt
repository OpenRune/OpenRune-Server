package org.rsmod.content.areas.city.falador.npcs

import dev.openrune.ServerCacheManager
import jakarta.inject.Inject
import kotlin.math.max
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.events.interact.HeldBanksideEvents
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.content.interfaces.bank.bankCapacity
import org.rsmod.content.interfaces.bank.selectedTab
import org.rsmod.content.interfaces.bank.setBanksideExtraOps
import org.rsmod.content.interfaces.bank.util.rightShift
import org.rsmod.game.type.placeholder
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class WysonNestBoxScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        for (nest in WysonNest.entries) {
            onOpHeld1(nest.box) { extract(nest, it.slot) }
            onOpHeld4(nest.box) { check(nest, it.slot) }
            onProtectedEvent<HeldBanksideEvents.Deposit>(nest.boxId) {
                extractToBank(nest, it.slot)
            }
        }
    }

    private suspend fun ProtectedAccess.extract(nest: WysonNest, slot: Int) {
        if (!WysonMoleParts.extract(player, slot, nest)) {
            mes("You don't have enough inventory space.")
        }
    }

    private fun ProtectedAccess.extractToBank(nest: WysonNest, slot: Int) {
        val box = inv[slot] ?: return
        if (box.id != nest.boxId) {
            return
        }
        val count = box.wysonNestCount()
        val capacity = bankCapacity
        val nestType = ServerCacheManager.getItem(nest.nestId) ?: return
        val placeholderId = if (nestType.hasPlaceholder) placeholder(nestType).id else -1
        val existingSlot = bank.indexOfFirst { it?.id == nest.nestId || it?.id == placeholderId }
        val tab = selectedTab
        val tabSlots = tab.slotRange(this)
        val gapSlot = tabSlots.firstOrNull { bank[it] == null }
        val needsNewSlot = existingSlot == -1 && gapSlot == null
        val tabEnd = tab.firstSlot(this) + tab.occupiedSpace(this)
        val tail = max(tabEnd, bank.lastOccupiedSlot())
        if (needsNewSlot && tail >= capacity) {
            mes("You don't have enough space in your bank account.")
            return
        }
        val targetSlot = when {
            existingSlot != -1 -> existingSlot
            gapSlot != null -> gapSlot
            else -> tabEnd
        }
        val shift = needsNewSlot
        val transaction =
            player.invTransaction(bank) {
                val bankInv = select(bank)
                if (shift) {
                    rightShift {
                        this.from = bankInv
                        this.startSlot = targetSlot
                        this.shiftCount = 1
                    }
                }
                insert {
                    this.into = bankInv
                    this.obj = nest.nestId
                    this.strictSlot = targetSlot
                    this.strictCount = count
                    this.capacity = capacity
                }
            }

        if (transaction.failure) {
            mes("You don't have enough space in your bank account.")
            return
        }

        if (shift) {
            tab.increaseSize(this)
        }
        inv[slot] = null
        player.setBanksideExtraOps()
    }

    private fun ProtectedAccess.check(nest: WysonNest, slot: Int) {
        val box = inv[slot] ?: return
        if (box.id != nest.boxId) {
            return
        }
        val count = box.wysonNestCount()
        val noun = if (count == 1) "bird nest" else "bird nests"
        mes("The box contains $count $noun.")
    }
}
