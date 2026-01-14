package org.alter.interfaces.bank.scripts

import dev.openrune.ServerCacheManager
import dev.openrune.cache.CacheManager
import dev.openrune.types.ItemServerType
import org.alter.api.Wearpos
import org.alter.api.ext.getVarbit
import org.alter.api.ext.inputInt
import org.alter.api.ext.message
import org.alter.api.ext.setVarbit
import org.alter.constants
import org.alter.game.model.entity.Player
import org.alter.game.model.entity.UpdateInventory.resendSlot
import org.alter.game.model.inv.Inventory
import org.alter.game.model.inv.invtx.invCompress
import org.alter.game.model.inv.invtx.invMoveFromSlot
import org.alter.game.model.inv.invtx.invMoveInv
import org.alter.game.model.inv.invtx.invTransaction
import org.alter.game.model.inv.invtx.select
import org.alter.game.model.inv.isType
import org.alter.game.model.inv.objtx.TransactionResult
import org.alter.game.model.inv.objtx.isOk
import org.alter.game.model.item.Item
import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.IfModalDrag
import org.alter.game.pluginnew.event.impl.onIfModalButton
import org.alter.game.pluginnew.event.impl.onIfModalDrag
import org.alter.game.pluginnew.event.impl.onLogin
import org.alter.interfaces.bank.configs.bank_components
import org.alter.interfaces.bank.*
import org.alter.interfaces.bank.configs.bank_comsubs
import org.alter.interfaces.bank.configs.bank_constants
import org.alter.interfaces.bank.util.BankSlots
import org.alter.interfaces.bank.util.bulkShift
import org.alter.interfaces.bank.util.leftShift
import org.alter.interfaces.bank.util.offset
import org.alter.interfaces.bank.util.rightShift
import org.alter.interfaces.bank.util.shiftInsert
import org.alter.invMoveToSlot
import org.alter.mesLayerClose
import org.alter.objExamine
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.alter.rscm.RSCMType
import kotlin.collections.get
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.text.get


class BankInvScript : PluginEvent() {

    override fun init() {
        onLogin { player.setDefaultCapacity() }

        onIfModalButton(bank_components.side_inventory) { player.sideInvOp(slot, op) }
        onIfModalButton(bank_components.main_inventory) { player.mainInvOp(slot, op) }
        onIfModalButton(bank_components.worn_inventory) { player.wornInvOp(slot, op) }
        onIfModalButton(bank_components.deposit_inventory) { player.depositInv() }
        onIfModalButton(bank_components.deposit_worn) { player.depositWorn() }
        onIfModalButton(bank_components.tabs) { player.selectTab(slot, op) }
        onIfModalButton(bank_components.incinerator_confirm) { player.incinerate(slot, item) }
        onIfModalDrag(bank_components.tabs) { player.dragTab(this) }
        onIfModalDrag(bank_components.side_inventory) { player.dragSideInv(this) }
        onIfModalDrag(bank_components.main_inventory) { player.dragMainInv(this) }
        onIfModalDrag(bank_components.worn_inventory) { player.dragSideInv(this) }
        onIfModalDrag(bank_components.main_inventory, bank_components.tabs) { player.dragIntoTab(this) }

        //val wornComponents = enumResolver[bank_enums.worn_component_map].filterValuesNotNull()
        //for ((slot, component) in wornComponents) {
            //onIfModalButton(component) { wornOp(slot, it.op) }
       // }

        //onPlayerQueue(bank_queues.bank_compress) { compressBank() }
    }

    private suspend fun Player.mainInvOp(slot: Int, op: MenuOption) {
        if (op == MenuOption.OP10) {
            error("TODO")
            //objExamine(bank, slot)
            return
        }
        mainInvWithdraw(slot, op)
    }

    private suspend fun Player.mainInvWithdraw(slot: Int, op: MenuOption) {
        val obj = bank[slot] ?: return resendSlot(bank, 0)
        val objType = ServerCacheManager.getItem(obj.id)?: return
        if (objType.isPlaceholder) {
            bank[slot] = null
            notifySlotUpdate(slot)
            return
        } else if (objType.isType("items.bank_filler")) {
            if (op == MenuOption.OP7) {
                bank[slot] = null
                soundSynth(1413)
                notifySlotUpdate(slot)
            } else if (op == MenuOption.OP8) {
                removeFillers()
            }
            return
        }
        val count =
            when (op) {
                MenuOption.OP1 -> resolveLeftClickQty()
                MenuOption.OP2 -> 1
                MenuOption.OP3 -> 5
                MenuOption.OP4 -> 10
                MenuOption.OP5 -> resolveLastDepositQty()
                MenuOption.OP6 -> {
                    var input = 0
                    queue { input = inputInt(this@mainInvWithdraw) }
                    lastQtyInput = input
                    input
                }
                MenuOption.OP7 -> obj.amount
                MenuOption.OP8 -> obj.amount - 1
                MenuOption.OP9 -> obj.amount
                else -> throw IllegalStateException("Invalid main inv op: $op")
            }
        val transaction =
            invMoveFromSlot(
                from = bank,
                into = inventory,
                fromSlot = slot,
                count = count,
                strict = false,
                cert = withdrawCert,
                placehold = alwaysPlacehold || op == MenuOption.OP9,
            )
        val result = transaction[0]

        if (result == TransactionResult.NotEnoughSpace) {
            message("You don't have enough inventory space.")
            return
        }

        if (!result.isOk()) {
            return
        }

        if (withdrawCert && !objType.canCert) {
            message("This item cannot be withdrawn as a note.")
        }

        if (!result.fullSuccess) {
            message("You don't have enough inventory space to withdraw that many.")
        }

        if (bank[slot] == null) {
            notifySlotUpdate(slot)
        }

        setBanksideExtraOps()
    }

    private suspend fun Player.sideInvOp(slot: Int, op: MenuOption) {
        if (op == MenuOption.OP10) {
            objExamine(this,inventory, slot)
            return
        }
        val count =
            when (op) {
                MenuOption.OP2 -> resolveLeftClickQty()
                MenuOption.OP3 -> 1
                MenuOption.OP4 -> 5
                MenuOption.OP5 -> 10
                MenuOption.OP6 -> resolveLastDepositQty()
                MenuOption.OP7 -> {
                    var input = 0
                    queue { input = inputInt(this@sideInvOp) }
                    lastQtyInput = input
                    input
                }
                MenuOption.OP8 -> Int.MAX_VALUE
                MenuOption.OP9 -> {
                    clickBanksideExtraOp(slot)
                    return
                }
                else -> throw IllegalStateException("Invalid main inv op: $op")
            }
        invDeposit(slot, count, inventory)
    }

    private fun Player.invDeposit(slot: Int, count: Int, inventory: Inventory): Boolean {
        val obj = inventory[slot]
        if (obj == null) {
            resendSlot(inventory, 0)
            return false
        }

        val objType = ServerCacheManager.getItem(obj.id)?: return false

        if (objType.param("param.no_bank") != 0) {
            message("A magical force prevents you from banking this item!")
            return false
        }

        val tab = selectedTab

        val placeholder = ItemServerType.placeholder(objType)
        val containedObjSlot = bank.indexOfFirst { it?.id == obj.id || it?.id == placeholder.id }
        val prioritySlot =
            if (containedObjSlot != -1) {
                containedObjSlot
            } else {
                tab.slotRange(this).firstOrNull { bank[it] == null }
            }

        // Mainly done for emulation purposes - compress all other bank tabs. This is not done
        // for main bank tab.
        if (prioritySlot == null && !tab.isMainTab) {
            val others = BankTab.entries - tab
            for (other in others) {
                val slots = other.slotRange(this)
                compressTabObjs(other)
                trimGapsAndReturnTrailingGaps(slots)
            }
        }

        val tabSlots = tab.slotRange(this)
        val insertQuery =
            invTransaction(inventory, bank) {
                val fromInv = select(inventory)
                val bankInv = select(bank)
                if (prioritySlot == null && !tab.isMainTab) {
                    rightShift {
                        this.from = bankInv
                        this.startSlot = tabSlots.last + 1
                        this.shiftCount = 1
                    }
                }
                transfer {
                    this.from = fromInv
                    this.into = bankInv
                    this.fromSlot = slot
                    this.intoSlot = prioritySlot ?: tabSlots.first
                    this.intoCapacity = 800
                    this.count = count
                    this.uncert = true
                    this.strict = false
                }
            }
        val result = insertQuery.results.last()

        // TODO(content): This message may be incorrect.
        if (result == TransactionResult.NotEnoughSpace && bank.occupiedSpace() >= 800) {
            message("You don't have enough space in your bank account.")
            return false
        }

        if (result == TransactionResult.NotEnoughSpace) {
            message("You already have a full stack of that item in the bank.")
            return false
        }

        if (!result.isOk()) {
            return false
        }

        // Cheap way of checking if obj has taken a new slot in bank.
        val expectedSlot = tabSlots.last + 1
        val expectedObj = uncert(obj)
        if (bank[expectedSlot]?.id == expectedObj.id) {
            tab.increaseSize(this)
        }

        if (!result.fullSuccess) {
            message("You already have a full stack of that item in the bank.")
        }

        setBanksideExtraOps()
        return true
    }

    private suspend fun Player.wornInvOp(slot: Int, op: MenuOption) {
        error("THIS")
    }

    private fun Player.depositInv() {
        if (inventory.isEmpty()) {
            message("You have nothing to deposit.")
            return
        }
        depositInventory(inventory)
    }

    private fun Player.depositWorn() {
        if (equipment.isEmpty()) {
            message("You have nothing to deposit.")
            return
        }
        val startWearposObjs = Wearpos.entries.associateWith { equipment[it.slot] }
        depositInventory(equipment)

        for ((wearpos, oldObj) in startWearposObjs) {
            if (oldObj == null || oldObj == equipment[wearpos.slot]) {
                continue
            }
            error("THIS")
            //WornUnequipOp.notifyWornUnequip(player, wearpos, objTypes[oldObj], eventBus)
        }
    }

    private fun Player.depositInventory(from: Inventory) {
        val unbankableSlots =
            from.indices
                .filter {
                    val obj = from[it]
                    obj != null && ServerCacheManager.getItem(obj.id)?.param("param.no_bank") != 0
                }
                .toHashSet()

        val tab = selectedTab
        if (tab.isMainTab) {
            val startLastOccupiedSpace = bank.lastOccupiedSlot()
            val transaction =
                invMoveInv(
                    from = from,
                    into = bank,
                    untransform = true,
                    intoStartSlot = tab.firstSlot(this),
                    intoCapacity = 800,
                    keepSlots = unbankableSlots,
                )
            val result = transaction[0]

            if (result == TransactionResult.NotEnoughSpace) {
                message("Your bank cannot hold your items.")
                return
            }

            if (transaction.completed() == 0 && unbankableSlots.isNotEmpty()) {
                message("Your items cannot be stored in the bank.")
            } else if (unbankableSlots.isNotEmpty()) {
                message("Some of your items cannot be stored in the bank.")
            }

            val lastOccupiedSlotDiff = bank.lastOccupiedSlot() - startLastOccupiedSpace
            if (lastOccupiedSlotDiff < 0) {
                throw IllegalStateException(
                    "`lastOccupiedSlotDiff` should not be negative: " +
                            "start=$startLastOccupiedSpace, curr=${bank.lastOccupiedSlot()}"
                )
            }

            if (lastOccupiedSlotDiff > 0) {
                tab.increaseSize(this, lastOccupiedSlotDiff)
            }

            setBanksideExtraOps()
            return
        }

        val containedBankObjs = bank.mapNotNull { it?.id }.toHashSet()

        val uniqueInvObjs = mutableSetOf<Int>()
        for (invObj in from) {
            val type = ServerCacheManager.getItem(invObj?.id ?: -1) ?: continue
            val uncert = uncert(type)
            if (uncert.param("param.no_bank") != 0) {
                continue
            }
            uniqueInvObjs += uncert.id
        }

        val containedObjMatches =
            uniqueInvObjs.count { obj ->
                val objItem = ServerCacheManager.getItem(obj)?: return
                val type = ItemServerType.untransform(objItem)
                val containsType = type.id in containedBankObjs
                val containsPlaceholder =
                    type.hasPlaceholder && ItemServerType.placeholder(type).id in containedBankObjs
                containsType || containsPlaceholder
            }
        val requiredSlots = uniqueInvObjs.count()
        val emptySlotCount = tab.slotRange(this).count { bank[it] == null }
        val newInsertSlots = requiredSlots - containedObjMatches - emptySlotCount

        check(newInsertSlots >= 0) {
            "`newInsertSlots` should not be negative: $newInsertSlots " +
                    "(required=$requiredSlots, contained=$containedObjMatches, empty=$emptySlotCount)"
        }

        // Make space for new obj insertions by moving all objs that come _after_ the target bank
        // tab by the corresponding slot amount.
        if (newInsertSlots > 0) {

            // Compress all tabs aside from the target tab to make space.
            val others = BankTab.entries - tab
            for (other in others) {
                val slots = other.slotRange(this)
                compressTabObjs(other)
                trimGapsAndReturnTrailingGaps(slots)
            }

            val tabSlots = tab.slotRange(this)
            val shiftQuery = invTransaction(bank) {
                    val bankInv = select(bank)
                    rightShift {
                        this.from = bankInv
                        this.startSlot = tabSlots.last + 1
                        this.shiftCount = newInsertSlots
                    }
                }

            if (shiftQuery.failure) {
                message("Your bank cannot hold your items.")
                return
            }
        }

        val fromSlots = from.indices.filter { from[it] != null }.distinctBy { from[it]?.id }
        val tabSlots = tab.slotRange(this)

        val bankCapacity = 800
        val filteredFromSlots = fromSlots - unbankableSlots
        val transferQuery =
            invTransaction(from, bank) {
                val fromInv = select(from)
                val bankInv = select(bank)
                for (slot in filteredFromSlots) {
                    transfer {
                        this.from = fromInv
                        this.into = bankInv
                        this.fromSlot = slot
                        this.intoSlot = tabSlots.first
                        this.intoCapacity = bankCapacity
                        this.count = Int.MAX_VALUE
                        this.uncert = true
                        this.untransform = true
                        this.strict = false
                    }
                }
            }
        val noneCompleted = transferQuery.noneCompleted()

        if (noneCompleted && filteredFromSlots.isEmpty()) {
            message("Your items cannot be stored in the bank.")
            return
        }

        if (noneCompleted) {
            message("Your bank cannot hold your items.")
            return
        }

        if (unbankableSlots.isNotEmpty()) {
            message("Some of your items cannot be stored in the bank.")
        }

        if (newInsertSlots > 0) {
            tab.increaseSize(this, newInsertSlots)
        }
    }

    private fun Player.dragTab(drag: IfModalDrag) {
        val fromTabIndex = drag.selectedSlot ?: return resendSlot(bank, 0)
        val intoTabIndex = drag.targetSlot ?: return resendSlot(bank, 0)
        val fromTab = BankTab.forIndex(fromTabIndex - bank_comsubs.other_tabs.first)
        val intoTab = BankTab.forIndex(intoTabIndex - bank_comsubs.other_tabs.first)

        if (fromTab == null || fromTab.isMainTab || fromTab.isEmpty(this)) {
            resendSlot(bank, 0)
            return
        }

        if (intoTab == null || intoTab.isMainTab || intoTab.isEmpty(this)) {
            resendSlot(bank, 0)
            return
        }

        val fromSlots = fromTab.slotRange(this)
        val intoSlots = intoTab.slotRange(this)
        val intoSlot = if (fromTab.index > intoTab.index) intoSlots.first else intoSlots.last

        val transaction =
            invTransaction(bank) {
                val bankInv = select(bank)
                bulkShift {
                    this.from = bankInv
                    this.fromSlots = fromSlots
                    this.intoSlot = intoSlot
                }
            }

        if (transaction.failure) {
            return
        }

        val fromTabSize = fromTab.occupiedSpace(this)
        if (fromTab.index > intoTab.index) {
            val tabShiftRange = fromTab.index - 1 downTo intoTab.index
            for (index in tabShiftRange) {
                val curr = BankTab.forIndex(index)
                checkNotNull(curr) { "`curr` tab should not be null: $index" }

                val next = BankTab.forIndex(index + 1)
                checkNotNull(next) { "`next` tab should not be null: ${index + 1}" }

                setVarbit(next.sizeVarBit,getVarbit(curr.sizeVarBit))
            }
        } else {
            val tabShiftRange = fromTab.index until intoTab.index
            for (index in tabShiftRange) {
                val curr = BankTab.forIndex(index)
                checkNotNull(curr) { "`curr` tab should not be null: $index" }

                val next = BankTab.forIndex(index + 1)
                checkNotNull(next) { "`next` tab should not be null: ${index + 1}" }

                setVarbit(curr.sizeVarBit,getVarbit(next.sizeVarBit))
            }
        }
        setVarbit(intoTab.sizeVarBit,fromTabSize)
    }

    private fun Player.dragSideInv(drag: IfModalDrag) {
        val fromSlot = drag.selectedSlot ?: return resendSlot(inventory, 0)
        val intoSlot = drag.targetSlot ?: return resendSlot(inventory, 0)
        invMoveToSlot(inventory, inventory, fromSlot, intoSlot)
        setBanksideExtraOps()
    }

    private fun Player.dragMainInv(drag: IfModalDrag) {
        val fromSlot = drag.selectedSlot ?: return resendSlot(bank, 0)
        val intoSlot = drag.targetSlot ?: return resendSlot(bank, 0)
        val fromTab = BankTab.forSlot(this, fromSlot) ?: return resendSlot(bank, fromSlot)

        // Intercept drag buttons that target tab "extended" slots. These are special subcomponents
        // created by the client to represent tab slots beyond their current capacity. (e.g., the
        // slot right next to the last obj in a tab)
        val extendedTabSlots = bank_comsubs.tab_extended_slots_offset.offset(bank.size - 1)
        if (intoSlot in extendedTabSlots) {
            val tabIndex = intoSlot - extendedTabSlots.first
            val intoTab = BankTab.forIndex(tabIndex - 1) ?: BankTab.Main

            // Any attempt to insert an obj into the "extended" slot of the tab it already belongs
            // to will be rejected and have its slot resynced.
            if (fromTab == intoTab) {
                resendSlot(bank, fromSlot)
                return
            }

            dragMainInvExtendedSlot(fromTab, fromSlot, intoTab)
            return
        }

        val intoTab = BankTab.forSlot(this, intoSlot) ?: BankTab.Main
        dragMainInv(fromTab, fromSlot, intoTab, intoSlot)
    }

    private fun Player.dragMainInvExtendedSlot(
        fromTab: BankTab,
        fromSlot: Int,
        intoTab: BankTab,
    ) {
        val intoSlots = intoTab.slotRange(this)
        val emptySlot = intoSlots.firstOrNull { bank[it] == null }
        val intoSlot = emptySlot ?: intoSlots.last
        val intoSlotOffset = if (emptySlot == null && fromSlot > intoSlot) 1 else 0
        val targetSlot = intoSlot + intoSlotOffset
        val targetSlotEmpty = bank[targetSlot] == null

        val transaction =
            invTransaction(bank) {
                val bankInv = select(bank)
                if (!targetSlotEmpty) {
                    shiftInsert {
                        this.from = bankInv
                        this.fromSlot = fromSlot
                        this.intoSlot = targetSlot
                    }
                } else {
                    swap {
                        this.from = bankInv
                        this.into = bankInv
                        this.fromSlot = fromSlot
                        this.intoSlot = targetSlot
                    }
                    leftShift {
                        this.from = bankInv
                        this.startSlot = fromSlot + 1
                        this.toSlot = fromSlot
                    }
                }
            }

        if (transaction.failure) {
            resendSlot(bank, fromSlot)
            return
        }

        fromTab.decreaseSize(this)
        if (emptySlot == null) {
            intoTab.increaseSize(this)
        }

        notifySlotUpdate(fromTab)
        if (fromTab.isEmpty(this)) {
            compactEmptyTabs(fromTab.index)
        }
    }

    private fun Player.dragMainInv(
        fromTab: BankTab,
        fromSlot: Int,
        intoTab: BankTab,
        intoSlot: Int,
    ) {
        if (!insertMode) {
            dragMainInvSwap(fromTab, fromSlot, intoTab, intoSlot)
            return
        }

        if (fromTab == intoTab && abs(fromSlot - intoSlot) == 1) {
            dragMainInvSwap(fromTab, fromSlot, intoTab, intoSlot)
            return
        }

        dragMainInvShift(fromTab, fromSlot, intoTab, intoSlot)
    }

    private fun Player.dragMainInvSwap(
        fromTab: BankTab,
        fromSlot: Int,
        intoTab: BankTab,
        intoSlot: Int,
    ) {
        val intoSlots = intoTab.slotRange(this)

        val transaction =
            invTransaction(bank) {
                val bankInv = select(bank)
                swap {
                    this.from = bankInv
                    this.into = bankInv
                    this.fromSlot = fromSlot
                    this.intoSlot = intoSlot
                }
            }

        if (transaction.failure) {
            resendSlot(bank, fromSlot)
            return
        }

        // Allows for size expansion if an obj is dragged outside the tab's current capacity.
        // This is technically only possible for the main bank tab.
        val sizeExpansion = intoSlot - intoSlots.last
        if (sizeExpansion > 0) {
            intoTab.increaseSize(this, sizeExpansion)
        }

        notifySlotUpdate(fromSlot)
        if (fromTab.isEmpty(this)) {
            compactEmptyTabs(fromTab.index)
        }
    }

    private fun Player.dragMainInvShift(
        fromTab: BankTab,
        fromSlot: Int,
        intoTab: BankTab,
        intoSlot: Int,
    ) {
        val intoSameTab = fromTab == intoTab
        val intoEmptySlot = bank[intoSlot] == null
        val intoSlotOffset = if (!intoEmptySlot && fromSlot < intoSlot && !intoSameTab) 1 else 0
        val intoMainTabEmptySlot = fromTab.isMainTab && intoTab.isMainTab && intoEmptySlot
        val targetSlot = intoSlot - intoSlotOffset

        val transaction =
            invTransaction(bank) {
                val bankInv = select(bank)
                if (!intoEmptySlot) {
                    shiftInsert {
                        this.from = bankInv
                        this.fromSlot = fromSlot
                        this.intoSlot = targetSlot
                    }
                } else {
                    swap {
                        this.from = bankInv
                        this.into = bankInv
                        this.fromSlot = fromSlot
                        this.intoSlot = targetSlot
                    }
                    if (!intoMainTabEmptySlot) {
                        leftShift {
                            this.from = bankInv
                            this.startSlot = fromSlot + 1
                            this.toSlot = fromSlot
                        }
                    }
                }
            }

        if (transaction.failure) {
            resendSlot(bank, fromSlot)
            return
        }

        if (!intoMainTabEmptySlot) {
            fromTab.decreaseSize(this)
        }

        if (!intoEmptySlot) {
            intoTab.increaseSize(this)
        } else {
            // Allows for size expansion if an obj is dragged outside the tab's current capacity.
            // This is technically only possible for the main bank tab.
            val sizeExpansion = intoSlot - intoTab.slotRange(this).last
            if (sizeExpansion > 0) {
                intoTab.increaseSize(this, sizeExpansion)
            }
        }

        notifySlotUpdate(fromTab)
        if (fromTab.isEmpty(this)) {
            compactEmptyTabs(fromTab.index)
        }
    }

    private fun Player.dragIntoTab(drag: IfModalDrag) {
        val fromSlot = drag.selectedSlot ?: return resendSlot(bank, 0)
        val comsub = drag.targetSlot ?: return resendSlot(bank, 0)

        if (comsub != bank_comsubs.main_tab && comsub !in bank_comsubs.other_tabs) {
            resendSlot(bank, fromSlot)
            return
        }

        val obj = bank[fromSlot]
        if (obj == null || obj.id != drag.selectedObj) {
            resendSlot(bank, fromSlot)
            return
        }

        val fromTab = BankTab.forSlot(this, fromSlot) ?: return resendSlot(bank, 0)
        if (comsub == bank_comsubs.main_tab) {
            dragIntoTab(fromTab, fromSlot, BankTab.Main)
            return
        }

        val tabIndex = comsub - bank_comsubs.other_tabs.first
        val intoTab = BankTab.forIndex(tabIndex) ?: return resendSlot(bank, fromSlot)
        dragIntoTab(fromTab, fromSlot, intoTab)
    }

    private fun Player.dragIntoTab(fromTab: BankTab, fromSlot: Int, intoTab: BankTab) {
        if (intoTab == fromTab) {
            resendSlot(bank, fromSlot)
            return
        }
        dragMainInvExtendedSlot(fromTab, fromSlot, intoTab)
    }

    private fun Player.selectTab(comsub: Int, op: MenuOption) {
        val tabIndex = comsub - bank_comsubs.other_tabs.first
        val tab = BankTab.forIndex(tabIndex) ?: BankTab.Main
        if (tab == BankTab.Main) {
            if (op == MenuOption.OP1) {
                selectedTab = tab
            } else if (op == MenuOption.OP7) {
                removePlaceholders(tab)
            }
            return
        }

        if (tab.isEmpty(this)) {
            message("To create a new tab, drag items from your bank onto this tab.")
            return
        }

        mesLayerClose(this,constants.meslayer_mode_objsearch)
        when (op) {
            MenuOption.OP1 -> selectedTab = tab
            MenuOption.OP6 -> collapseTab(tab)
            MenuOption.OP7 -> removePlaceholders(tab)
            else -> throw NotImplementedError("Bank tab op not implemented: op=$op")
        }
    }

    private fun Player.removePlaceholders(tab: BankTab) {
        val slots = tab.slotRange(this)
        if (slots.isEmpty()) {
            message("You don't have any placeholders to release.")
            return
        }
        var removed = 0
        for (slot in slots) {
            val obj = bank[slot] ?: continue
            if (obj.getDef().isPlaceholder) {
                bank[slot] = null
                removed++
            }
        }
        if (removed == 0) {
            message("You don't have any placeholders to release.")
            return
        }
        notifySlotUpdate(slots.first)
    }

    private fun Player.removePlaceholders() {
        val tabUpdates = mutableSetOf<BankTab>()
        for (slot in bank.indices) {
            val obj = bank[slot] ?: continue
            if (obj.getDef().isPlaceholder) {
                bank[slot] = null

                val tab = BankTab.forSlot(this, slot)
                tab?.let(tabUpdates::add)
            }
        }
        if (tabUpdates.isEmpty()) {
            return
        }
        soundSynth(1413)
        for (tab in tabUpdates) {
            notifySlotUpdate(tab)
        }
    }

    private suspend fun Player.removeFillers() {
        error("TODO REMOVE FILTERS")
        return
    }

    private fun Player.collapseTab(tab: BankTab) {
        require(!tab.isMainTab) { "Main bank tab cannot be collapsed." }

        for (tab in BankTab.tabs) {
            compressTabObjs(tab)
        }

        val collapseObjCount = tab.occupiedSpace(this)
        val collapseTabSlots = tab.slotRange(this)

        val targetTab = BankTab.Main
        val targetTabSlots = targetTab.slotRange(this)
        val targetEmptySlots =
            targetTabSlots.asSequence().filter { bank[it] == null }.take(collapseObjCount).toList()

        val newSlotsCount = collapseObjCount - targetEmptySlots.size
        val swapQuery =
            invTransaction(bank) {
                val bankInv = select(bank)
                for (i in targetEmptySlots.indices) {
                    val fromSlot = collapseTabSlots.first + i
                    if (fromSlot > collapseTabSlots.last) {
                        break
                    }
                    swap {
                        this.from = bankInv
                        this.into = bankInv
                        this.fromSlot = fromSlot
                        this.intoSlot = targetEmptySlots[i]
                        this.strict = true
                    }
                }
                if (newSlotsCount > 0) {
                    val shiftStartSlot = collapseTabSlots.first + targetEmptySlots.size
                    val shiftSlots = shiftStartSlot..collapseTabSlots.last
                    bulkShift {
                        this.from = bankInv
                        this.fromSlots = shiftSlots
                        this.intoSlot = targetTabSlots.last
                    }
                }
            }
        check(swapQuery.success) { "Could not collapse tab: tab=$tab, err=${swapQuery.err}" }

        targetTab.increaseSize(this, newSlotsCount)

        val trailingNullCount = trimGapsAndReturnTrailingGaps(tab)
        tab.decreaseSize(this, trailingNullCount)
        if (collapseObjCount > trailingNullCount) {
            tab.decreaseSize(this, collapseObjCount - trailingNullCount)
        }

        compactEmptyTabs(tab.index)
        if (selectedTab == tab) {
            selectedTab = BankTab.Main
        }
    }

    private fun Player.notifySlotUpdate(slot: Int) {
        val tab = BankTab.forSlot(this, slot)
        checkNotNull(tab) { "`slot` was not associated with a valid bank tab: $slot" }
        notifySlotUpdate(tab)
    }

    private fun Player.notifySlotUpdate(tab: BankTab) {
        val trailingNullCount = trimGapsAndReturnTrailingGaps(tab)
        if (trailingNullCount == 0) {
            return
        }

        tab.decreaseSize(this, trailingNullCount)

        if (tab.isEmpty(this) && !tab.isMainTab) {
            compactEmptyTabs(tab.index)

            if (selectedTab == tab) {
                selectedTab = BankTab.Main
            }
        }
    }

    private fun Player.trimGapsAndReturnTrailingGaps(slotRange: IntRange): Int {
        // Note: `shiftLeadingGapsToTail` directly modifies `bank` obj array.
        val result = BankSlots.shiftLeadingGapsToTail(bank, slotRange)

        val trailingNullCount = result.trailingGaps
        if (trailingNullCount == 0) {
            return 0
        }

        val shiftStartSlot = min(bank.size - 1, slotRange.last + 1)
        val shiftToSlot = min(bank.size - 1, slotRange.last + 1 - trailingNullCount)

        // Set a "max shift slot" to avoid unnecessary shifting of slots beyond the last occupied
        // slot in bank inventory.
        val maxShiftSlot = min(bank.size, bank.lastOccupiedSlot() + trailingNullCount)

        val shiftQuery =
            invTransaction(bank) {
                val bankInv = select(bank)
                leftShift {
                    this.from = bankInv
                    this.startSlot = shiftStartSlot
                    this.toSlot = shiftToSlot
                    this.maxSlot = maxShiftSlot
                }
            }

        check(shiftQuery.success) {
            "Could not shift bank inventory: " +
                    "startSlot=$shiftStartSlot, toSlot=$shiftToSlot, err=${shiftQuery.err}"
        }

        return trailingNullCount
    }

    private fun Player.trimGapsAndReturnTrailingGaps(tab: BankTab): Int {
        val slots = tab.slotRange(this)
        return trimGapsAndReturnTrailingGaps(slots)
    }

    private fun Player.compressTabObjs(tab: BankTab) {
        if (tab.isEmpty(this)) {
            return
        }
        val tabSlots = tab.slotRange(this)
        if (tabSlots.first != tabSlots.last) {
            val compactQuery = invCompress(bank, tabSlots)
            check(compactQuery.success) {
                "Could not compress tab: tab=$tab, err=${compactQuery.err}"
            }
        }
        val trailingNullCount = tabSlots.count { bank[it] == null }
        if (trailingNullCount > 0) {
            tab.decreaseSize(this, trailingNullCount)
        }
    }

    private fun Player.compactEmptyTabs(startTabIndex: Int) {
        for (index in startTabIndex until BankTab.tabs.size) {
            val curr = BankTab.tabs[index]
            val next = BankTab.tabs.getOrNull(index + 1)
            val nextSize = next?.occupiedSpace(this) ?: 0
            setVarbit(curr.sizeVarBit,nextSize)
        }
    }

    private fun Player.compressBank() {
        for (tab in BankTab.entries) {
            val slots = tab.slotRange(this)
            compressTabObjs(tab)
            trimGapsAndReturnTrailingGaps(slots)
        }
    }

    private fun Player.resolveLeftClickQty(): Int =
        when (leftClickQtyMode) {
            QuantityMode.One -> 1
            QuantityMode.Five -> 5
            QuantityMode.Ten -> 10
            QuantityMode.X -> resolveLastDepositQty()
            QuantityMode.All -> Int.MAX_VALUE
        }

    private fun Player.resolveLastDepositQty(): Int = max(1, lastQtyInput)

    private fun Player.incinerate(comsub: Int, objType: Int?) {
        if (objType == null || ServerCacheManager.getItem(objType)!!.isPlaceholder) {
            return
        }
        val slot = comsub - 1
        val obj = bank[slot] ?: return resendSlot(bank, 0)

        if (!obj.getDef().isType(RSCM.getReverseMapping(RSCMType.OBJTYPES,objType))) {
            resendSlot(bank, slot)
            return
        }

        bank[slot] = null
        soundSynth(159)
        notifySlotUpdate(slot)

        error("TODO")
    }

    private fun Player.setBanksideExtraOps() {

    }

    private suspend fun Player.clickBanksideExtraOp(slot: Int) {
        error("TODO")
    }

    private fun Player.wornOp(wornSlot: Int, op: MenuOption) {
        error("TODO")
    }

    suspend fun releasePlaceholders(player: Player) {
        error("RELEASe placeholder")
    }

     fun addBankFillers(access: Player, requestedCount: Int?) {
        val bank = access.bank
        val bankCapacity = 800

        val freeSpace = bankCapacity - bank.occupiedSpace()
        if (freeSpace <= 0) {
            access.message("Your bank is already full, so there is no reason to add any bank fillers.")
            return
        }

        var count = requestedCount ?: -1
        if (count == -1) {
            access.queue { count = inputInt(access) }
        }
        val cappedCount = min(freeSpace, count)
        if (cappedCount == 0) {
            return
        }

        val targetTab = BankTab.Main
        val startSlot = targetTab.slotRange(access).first
        val emptySlots = (startSlot until bankCapacity).filter { bank[it] == null }

        var completed = 0
        for (slot in emptySlots) {
            if (completed >= count) {
                break
            }
            if (bank[slot] == null) {
                // Give the filler vars to avoid any sort of merging through later transactions.
                val filler = Item("items.bank_filler".asRSCM(), vars = 1)
                bank[slot] = filler
                completed++
            }
        }

        if (completed == 0) {
            access.message("Your bank is already full, so there is no reason to add any bank fillers.")
        } else {
            val formatCount = if (completed == 1) "bank filler" else "bank fillers"
            access.message("You add $completed $formatCount to your bank.")
            targetTab.increaseSize(access, completed)
        }
    }

    private fun Player.setDefaultCapacity() {

    }

    companion object {
        public fun uncert(type: ItemServerType): ItemServerType {
            println("ITEM: ${type.id}")
            if (!type.isCert) {
                return type
            }
            val link = type.noteLinkId
            return ServerCacheManager.getItem(link) ?: throw NoSuchElementException("Type is missing in the map: $link.")
        }

        public fun uncert(obj: Item): Item {
            if (obj.vars != 0) {
                return obj
            }

            val type = ServerCacheManager.getItem(obj.id)?: return obj
            if (!type.isCert) {
                return obj
            }

            val link = type.noteLinkId
            val uncertType =
                ServerCacheManager.getItem(link) ?: throw NoSuchElementException("Type is missing in the map: $link.")
            return Item(uncertType.id, obj.amount)
        }
    }

}