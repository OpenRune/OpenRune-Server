package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.events.interact.HeldBanksideEvents
import org.rsmod.api.player.events.interact.HeldDropEvents
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.startInvTransmit
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.player.ui.IfModalDrag
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfModalDrag
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.content.interfaces.bank.BanksideExtraOpsEvent
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingBagScript @Inject constructor(private val banking: BankInvScript) : PluginScript() {
    private val herbSack = FarmingBags.herbSack
    private val seedBox = FarmingBags.seedBox
    private val reagentPouch = FarmingBags.reagentPouch

    override fun ScriptContext.startup() {
        for (sack in herbSack.variants) {
            onOpHeld1(sack) { fillHerbSack(sack) }
            onOpHeld2(sack) { toggle(herbSack, sack, it.slot, it.obj) }
            onOpHeld3(sack) { checkHerbSack(sack) }
            onOpHeld4(sack) { emptyHerbSack(sack) }
            onOpHeldU(sack) { useOnHerbSack(sack, it.firstSlot, it.second, it.secondSlot) }
            onProtectedEvent<HeldBanksideEvents.Type>(sack.asRSCM(RSCMType.OBJ)) {
                depositToBank(herbSack)
            }
        }

        for (box in seedBox.variants) {
            onOpHeld1(box) { fillSeedBox() }
            onOpHeld2(box) { emptySeedBox() }
            onOpHeld3(box) { openSeedBox() }
            onOpHeld4(box) { toggle(seedBox, box, it.slot, it.obj) }
            onOpHeldU(box) { useOnSeedBox(box, it.second, it.secondSlot) }
            onProtectedEvent<HeldBanksideEvents.Type>(box.asRSCM(RSCMType.OBJ)) { emptySeedBoxToBank() }
        }
        onIfModalButton(SEED_LAYER) { withdrawSeeds(it) }
        onIfModalDrag(SEED_LAYER) { reorderSeeds(it) }
        onIfModalButton(SEED_FILL) { fillSeedBox() }
        onIfModalButton(SEED_EMPTY) { emptySeedBox() }
        onIfModalButton(SEED_CLOSE) { ifCloseSub(SEED_INTERFACE) }
        onIfClose(SEED_INTERFACE) { closeSeedBox(player) }

        for (pouch in reagentPouch.variants) {
            onOpHeld1(pouch) { fill(player, reagentPouch, pouch) }
            onOpHeld2(pouch) { toggle(reagentPouch, pouch, it.slot, it.obj) }
            onOpHeld4(pouch) { checkBag(reagentPouch, "reagent pouch") }
            onProtectedEvent<HeldBanksideEvents.Type>(pouch.asRSCM(RSCMType.OBJ)) {
                if (hasStorable(player, reagentPouch)) {
                    fill(player, reagentPouch, pouch)
                } else {
                    depositToBank(reagentPouch)
                }
            }
        }

        onEvent<BanksideExtraOpsEvent> {
            if (!hasStorable(player, reagentPouch)) return@onEvent
            for (slot in player.inv.indices) {
                val obj = player.inv[slot] ?: continue
                if (getInvObj(obj).internalName in reagentPouch.variants) {
                    fillBits = fillBits or (1 shl slot)
                }
            }
        }

        onEvent<HeldDropEvents.Destroy> {
            BAGS.firstOrNull { type in it.variants }?.clearIfGone(player)
        }
    }

    private fun ProtectedAccess.toggle(bag: FarmingBag, variant: String, slot: Int, obj: InvObj) {
        if (inv[slot]?.id != obj.id) return
        val closedIndex = bag.closed.indexOf(variant)
        val replacement =
            if (closedIndex >= 0) bag.open[closedIndex] else bag.closed[bag.open.indexOf(variant)]
        invReplaceSlot(inv, slot, 1, item(replacement))
    }

    /** Moves every storable obj in the inventory into [bag] while it fits; returns what moved. */
    private fun fill(player: Player, bag: FarmingBag, variant: String): List<Pair<ItemServerType, Int>> {
        val inv = player.inv
        val moved = mutableListOf<Pair<ItemServerType, Int>>()
        for (slot in inv.indices) {
            val obj = inv[slot] ?: continue
            if (!bag.isStorable(obj.id) || moved.any { it.first.id == obj.id }) continue
            val count = minOf(inv.countOf(obj.id), bag.room(player, variant, obj.id))
            if (count > 0 && bag.put(player, obj.id, count, from = inv).success) {
                moved += getInvObj(obj) to count
            }
        }
        return moved
    }

    private fun hasStorable(player: Player, bag: FarmingBag): Boolean =
        player.inv.any { it != null && bag.isStorable(it.id) }

    private fun ProtectedAccess.fillHerbSack(sack: String) {
        if (!hasStorable(player, herbSack)) {
            mes("You have no grimy herbs in your inventory that can be stored in the sack.")
            return
        }
        mes("You search your inventory for herbs appropriate to put in the sack...", ChatType.Spam)
        if (fill(player, herbSack, sack).isNotEmpty()) {
            mes("You add the herbs to your sack.", ChatType.Spam)
        } else {
            mes("You don't have enough space for the herbs.")
        }
    }

    private fun ProtectedAccess.checkHerbSack(sack: String) {
        checkBag(herbSack, FarmingBags.herbSackName(sack))
    }

    private fun ProtectedAccess.checkBag(bag: FarmingBag, name: String) {
        val store = bag.store(player)
        mes("You look in your $name and see:")
        if (store.isEmpty()) {
            mes("The $name is empty.")
            return
        }
        for (type in bag.storable) {
            val count = store.countOf(type.id)
            if (count > 0) mes("$count x ${type.name}")
        }
    }

    private fun ProtectedAccess.emptyHerbSack(sack: String) {
        val store = herbSack.store(player)
        if (store.isEmpty()) {
            mes("The ${FarmingBags.herbSackName(sack)} is empty.")
            return
        }
        for (type in herbSack.storable) {
            val stored = store.countOf(type.id)
            if (stored == 0) continue
            val count = inv.fits(type, stored)
            if (count == 0 || !herbSack.take(player, type.id, count, inv)) return
        }
    }

    private suspend fun ProtectedAccess.useOnHerbSack(
        sack: String,
        sackSlot: Int,
        used: ItemServerType,
        usedSlot: Int,
    ) {
        if (used.id == SPIDER_SILK.asRSCM(RSCMType.OBJ) && !FarmingBags.isSilklined(sack)) {
            lineSack(sack, sackSlot, usedSlot)
            return
        }
        if (!herbSack.isStorable(used.id)) {
            mes("You can't possibly put that in there, it would spoil your sack.")
            return
        }
        if (inv[usedSlot]?.id != used.id) return
        if (herbSack.room(player, sack, used.id) < 1) {
            // UNVERIFIED: Fill's no-space message reused for a herb used on a full sack.
            mes("You don't have enough space for the herbs.")
            return
        }
        if (!herbSack.put(player, used.id, 1, from = inv).success) return
        // UNVERIFIED: the pickup message reused for a herb used on the sack.
        val name = FarmingBags.herbSackName(sack)
        mes("You put the ${used.name} herb into your $name.", ChatType.Spam)
    }

    private suspend fun ProtectedAccess.lineSack(sack: String, sackSlot: Int, silkSlot: Int) {
        val lined =
            if (sack == FarmingBags.HERB_SACK) {
                FarmingBags.SILKLINED_HERB_SACK
            } else {
                FarmingBags.SILKLINED_HERB_SACK_OPEN
            }
        val sackType = item(sack)
        val linedType = item(lined)
        // UNVERIFIED: dialogue box types; the text is from the transcript.
        doubleobjbox(
            sack,
            SPIDER_SILK,
            "Are you sure you wish to combine the ${sackType.name} and Pristine spider silk to " +
                "create ${linedType.name}? This cannot be reversed.",
        )
        if (!choice2("Proceed with the combination.", true, "Cancel.", false)) return
        val combined =
            player.invTransaction(inv) {
                val held = select(inv)
                delete(held, SPIDER_SILK.asRSCM(RSCMType.OBJ), 1, slot = silkSlot)
                delete(held, sackType.id, 1, slot = sackSlot)
                add(held, linedType.id, 1, slot = sackSlot)
            }
        if (combined.failure) return
        objbox(
            lined,
            "You successfully combine the ${sackType.name} and Pristine spider silk to create " +
                "${linedType.name}.",
        )
    }

    private fun ProtectedAccess.fillSeedBox() {
        if (!hasStorable(player, seedBox)) {
            mes("You have no seeds that will fit in the seed box.")
            return
        }
        val box = seedBox.held(player) ?: return
        for ((type, count) in fill(player, seedBox, box)) {
            mes("Stored $count x ${type.name} in your seed box.", ChatType.Spam)
        }
        if (hasStorable(player, seedBox)) {
            mes("You don't have space in your seed box for that.")
        }
    }

    private fun ProtectedAccess.useOnSeedBox(box: String, used: ItemServerType, usedSlot: Int) {
        val obj = inv[usedSlot]
        if (!seedBox.isStorable(used.id) || obj?.id != used.id) {
            mes(Constants.dm_default)
            return
        }
        val stored =
            seedBox.room(player, box, used.id) >= obj.count &&
                seedBox.put(player, used.id, obj.count, from = inv).success
        if (!stored) {
            mes("You don't have space in your seed box for that.")
            return
        }
        mes("Stored ${obj.count} x ${used.name} in your seed box.", ChatType.Spam)
    }

    private fun ProtectedAccess.emptySeedBox() {
        val store = seedBox.store(player)
        if (store.isEmpty()) {
            // UNVERIFIED: the bank Empty message reused for the inventory Empty.
            mes("Your seed box is already empty.")
            return
        }
        for (slot in store.indices) {
            val obj = store[slot] ?: continue
            if (withdrawSeed(obj, obj.count) == 0) return
        }
    }

    /** Moves up to [requested] of a stored seed into the inventory; returns how many moved. */
    private fun ProtectedAccess.withdrawSeed(obj: InvObj, requested: Int): Int {
        val type = getInvObj(obj)
        val count = inv.fits(type, minOf(requested, obj.count))
        if (count == 0 || !seedBox.take(player, obj.id, count, inv)) {
            mes(Constants.dm_invspace)
            return 0
        }
        mes("Emptied $count x ${type.name} to your inventory.", ChatType.Spam)
        return count
    }

    private fun ProtectedAccess.openSeedBox() {
        val store = seedBox.store(player)
        player.startInvTransmit(store)
        ifOpenSide(SEED_INTERFACE)
        ClientScripts.interfaceInvInit(
            player,
            store,
            SEED_LAYER,
            objRowCount = 2,
            objColCount = 3,
            dragType = 1,
            op1 = "Withdraw 1",
            op2 = "Withdraw 3",
            op3 = "Withdraw 4",
            op4 = "Withdraw X",
            op5 = "Withdraw All",
        )
        ifSetEvents(
            SEED_LAYER,
            store.indices,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op5,
            IfEvent.Op10,
            IfEvent.Depth1,
            IfEvent.DragTarget,
        )
    }

    private suspend fun ProtectedAccess.withdrawSeeds(event: IfModalButton) {
        val store = seedBox.store(player)
        if (event.comsub !in store.indices) return
        val obj = store[event.comsub] ?: return
        if (event.obj?.id != obj.id) return
        val requested =
            when (event.op) {
                IfButtonOp.Op1 -> 1
                IfButtonOp.Op2 -> 3
                IfButtonOp.Op3 -> 4
                IfButtonOp.Op4 -> countDialog()
                IfButtonOp.Op5 -> obj.count
                IfButtonOp.Op10 -> return objExamine(store, event.comsub)
                else -> return
            }
        val current = store[event.comsub]
        if (requested <= 0 || current?.id != obj.id) return
        withdrawSeed(current, requested)
    }

    private fun ProtectedAccess.reorderSeeds(drag: IfModalDrag) {
        val from = drag.selectedSlot ?: return
        val into = drag.targetSlot ?: return
        val store = seedBox.store(player)
        if (from in store.indices && into in store.indices && from != into) {
            invMoveToSlot(store, store, from, into)
        }
    }

    private fun closeSeedBox(player: Player) {
        val store = seedBox.store(player)
        player.stopInvTransmit(store)
        if (store.isEmpty()) {
            player.mes("The seed box is empty.", ChatType.Spam)
            return
        }
        player.mes("The seed box contains:", ChatType.Spam)
        for (obj in store) {
            if (obj != null) player.mes("${obj.count} x ${getInvObj(obj).name}.", ChatType.Spam)
        }
    }

    private fun ProtectedAccess.emptySeedBoxToBank() {
        if (seedBox.store(player).isEmpty()) {
            player.mes("Your seed box is already empty.")
            return
        }
        depositToBank(seedBox)
        if (seedBox.store(player).isEmpty()) {
            player.mes("Your seed box is now empty.", ChatType.Spam)
        }
    }

    /** Moves each stored stack into the bank as far as it fits; the rest stays in [bag]. */
    private fun ProtectedAccess.depositToBank(bag: FarmingBag) {
        val store = bag.store(player)
        for (slot in store.indices) {
            val obj = store[slot] ?: continue
            with(banking) { invDeposit(slot, obj.count, store) }
        }
    }

    private fun item(obj: String): ItemServerType =
        checkNotNull(ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ))) { "Missing obj: $obj" }

    private companion object {
        const val SPIDER_SILK = "obj.slayer_spider_silk"
        const val SEED_INTERFACE = "interface.hosidius_seedbox"
        const val SEED_LAYER = "component.hosidius_seedbox:seed_layer"
        const val SEED_FILL = "component.hosidius_seedbox:fill_button"
        const val SEED_EMPTY = "component.hosidius_seedbox:empty_button"
        const val SEED_CLOSE = "component.hosidius_seedbox:close_button"

        val BAGS = listOf(FarmingBags.herbSack, FarmingBags.seedBox, FarmingBags.reagentPouch)
    }
}
