# Bank Interface Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a full-featured OSRS bank interface with withdraw/deposit, 9 tabs, placeholders, noted withdrawal, search, and bulk deposit.

**Architecture:** Split by concern — `BankState.kt` (varps/attributes), `BankService.kt` (stateless logic), `BankPlugin.kt` (event wiring), `Extensions.kt` (orchestration). Uses existing `invDel`/`invAdd` from `InvTransactionExtensions.kt` for item operations with built-in placeholder/cert flags.

**Tech Stack:** Kotlin 2.0, OpenRune plugin system (`PluginEvent`), RSCM for component IDs, `InvTransactions` for item operations.

**Spec:** `docs/superpowers/specs/2026-03-25-bank-interface-design.md`

**Testing note:** No unit tests in this codebase. Verification is compile-check + in-game functional testing.

**Key API patterns (reference for all tasks):**

```kotlin
// Inventory access
val bankInv = player.invMap.getValue("inv.bank")

// Item operations (InvTransactionExtensions.kt)
player.invDel(inv, obj, count, slot?, strict?, placehold?, autoCommit?)
player.invAdd(inv, obj, count, vars?, slot?, strict?, cert?, uncert?, autoCommit?)

// Transient attributes
val MY_ATTR = AttributeKey<Int>()   // from org.alter.game.model.attr.AttributeKey
player.attr[MY_ATTR]               // get
player.attr[MY_ATTR] = value       // set

// Varps/varbits (persisted)
var Player.myVarp by intVarp("varp.name")
var Player.myVarbit by intVarBit("varbits.name")
var Player.myBool by boolVarBit("varbits.name")

// Events
onInterfaceOpen("interfaces.x") { ... }
onInterfaceClose("interfaces.x") { ... }
onButton("components.x:y") { /* op, slot, item, player available */ }
onIfModalDrag("components.x:y") { /* selectedSlot, targetSlot, player */ }

// Interface management
player.ifOpenMainSidePair("interfaces.bankmain", "interfaces.bankside")
player.ifSetEvents("components.x:y", 0..N, IfEvent.Op1, IfEvent.Op2, ...)

// Amount input
player.queue { val amount = inputInt(player, "How many?") }
```

---

## File Structure

| File | Responsibility |
|------|----------------|
| `content/.../interfaces/bank/BankState.kt` | Create — varp/varbit delegates, AttributeKey instances, tab helper functions |
| `content/.../interfaces/bank/BankService.kt` | Create — stateless withdraw/deposit/tab/placeholder functions |
| `content/.../interfaces/bank/BankPlugin.kt` | Create — PluginEvent with all button/interface event handlers |
| `content/.../interfaces/bank/Extensions.kt` | Modify — implement `openBank()` / `closeBank()` (currently empty stub) |
| `game-server/.../model/inv/map/InvMapInit.kt` | Modify — add `inv.bank` to `defaultInvs` |

---

## Phase 1: Foundation

### Task 1: Add Bank to Default Inventories

**Files:**
- Modify: `game-server/src/main/kotlin/org/alter/game/model/inv/map/InvMapInit.kt`

- [ ] **Step 1: Add `inv.bank` to `defaultInvs`**

```kotlin
public val defaultInvs: MutableSet<String> = hashSetOf(
    "inv.inv",
    "inv.worn",
    "inv.bank",
)
```

- [ ] **Step 2: Compile check**

Run: `gradle :game-server:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add game-server/src/main/kotlin/org/alter/game/model/inv/map/InvMapInit.kt
git commit -m "feat(bank): add inv.bank to default inventories"
```

---

### Task 2: Bank State — Varps, Varbits, Attributes

**Files:**
- Create: `content/src/main/kotlin/org/alter/interfaces/bank/BankState.kt`

- [ ] **Step 1: Create BankState.kt**

```kotlin
package org.alter.interfaces.bank

import org.alter.api.ext.boolVarBit
import org.alter.api.ext.intVarBit
import org.alter.api.ext.intVarp
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Player

/**
 * Bank interface state — varps/varbits (persisted) and transient attributes.
 *
 * Gamevals RSCM references:
 *   varp.bank_tab_display=4170, varp.bank_tab_1..9=4171..4179
 *   varbits.bank_withdrawnotes=3958, varbits.bank_insertmode=3959
 *   varbits.bank_quantity_type=6590
 */
object BankState {

    // --- Persisted state (varps/varbits) ---

    var Player.bankWithdrawAsNote by boolVarBit("varbits.bank_withdrawnotes")
    var Player.bankInsertMode by boolVarBit("varbits.bank_insertmode")
    var Player.bankQuantityType by intVarBit("varbits.bank_quantity_type")

    var Player.bankTab0Size by intVarp("varp.bank_tab_display")
    var Player.bankTab1Size by intVarp("varp.bank_tab_1")
    var Player.bankTab2Size by intVarp("varp.bank_tab_2")
    var Player.bankTab3Size by intVarp("varp.bank_tab_3")
    var Player.bankTab4Size by intVarp("varp.bank_tab_4")
    var Player.bankTab5Size by intVarp("varp.bank_tab_5")
    var Player.bankTab6Size by intVarp("varp.bank_tab_6")
    var Player.bankTab7Size by intVarp("varp.bank_tab_7")
    var Player.bankTab8Size by intVarp("varp.bank_tab_8")
    var Player.bankTab9Size by intVarp("varp.bank_tab_9")

    // --- Transient state (lost on logout) ---

    val BANK_ACTIVE_TAB = AttributeKey<Int>()
    val BANK_SEARCH_MODE = AttributeKey<Boolean>()
    val BANK_PRE_SEARCH_TAB = AttributeKey<Int>()
    val BANK_LAST_X = AttributeKey<Int>()
    val BANK_PLACEHOLDER_MODE = AttributeKey<Boolean>()

    var Player.bankActiveTab: Int
        get() = attr[BANK_ACTIVE_TAB] ?: 0
        set(value) { attr[BANK_ACTIVE_TAB] = value }

    var Player.bankSearchMode: Boolean
        get() = attr[BANK_SEARCH_MODE] ?: false
        set(value) { attr[BANK_SEARCH_MODE] = value }

    var Player.bankPreSearchTab: Int
        get() = attr[BANK_PRE_SEARCH_TAB] ?: 0
        set(value) { attr[BANK_PRE_SEARCH_TAB] = value }

    var Player.bankLastXAmount: Int
        get() = attr[BANK_LAST_X] ?: 0
        set(value) { attr[BANK_LAST_X] = value }

    var Player.bankPlaceholderMode: Boolean
        get() = attr[BANK_PLACEHOLDER_MODE] ?: false
        set(value) { attr[BANK_PLACEHOLDER_MODE] = value }

    // --- Tab size helpers ---

    /** Get tab sizes as IntArray(10). Index 0 = main, 1-9 = custom. */
    fun Player.getTabSizes(): IntArray = intArrayOf(
        bankTab0Size,
        bankTab1Size, bankTab2Size, bankTab3Size,
        bankTab4Size, bankTab5Size, bankTab6Size,
        bankTab7Size, bankTab8Size, bankTab9Size,
    )

    /** Set tab sizes from IntArray(10). */
    fun Player.setTabSizes(sizes: IntArray) {
        bankTab0Size = sizes[0]
        bankTab1Size = sizes[1]
        bankTab2Size = sizes[2]
        bankTab3Size = sizes[3]
        bankTab4Size = sizes[4]
        bankTab5Size = sizes[5]
        bankTab6Size = sizes[6]
        bankTab7Size = sizes[7]
        bankTab8Size = sizes[8]
        bankTab9Size = sizes[9]
    }

    /** Start slot index for a given tab (0-9). */
    fun tabStartSlot(sizes: IntArray, tab: Int): Int = sizes.take(tab).sum()

    /** End slot index (exclusive) for a given tab (0-9). */
    fun tabEndSlot(sizes: IntArray, tab: Int): Int = sizes.take(tab + 1).sum()

    /** Which tab a slot belongs to, or -1 if out of bounds. */
    fun tabForSlot(sizes: IntArray, slot: Int): Int {
        var cumulative = 0
        for (i in sizes.indices) {
            cumulative += sizes[i]
            if (slot < cumulative) return i
        }
        return -1
    }
}
```

- [ ] **Step 2: Compile check**

Run: `gradle :content:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add content/src/main/kotlin/org/alter/interfaces/bank/BankState.kt
git commit -m "feat(bank): add BankState with varps, varbits, and transient attributes"
```

---

## Phase 2: Open / Close

### Task 3: Implement openBank() and closeBank()

**Files:**
- Modify: `content/src/main/kotlin/org/alter/interfaces/bank/Extensions.kt`

**Reference:** `content/src/main/kotlin/org/alter/mechanics/shops/ShopsPlugin.kt` for interface open/transmit pattern.

- [ ] **Step 1: Implement openBank() and closeBank()**

```kotlin
package org.alter.interfaces.bank

import dev.openrune.definition.type.widget.IfEvent
import org.alter.game.model.entity.Player
import org.alter.interfaces.bank.BankState.bankActiveTab
import org.alter.interfaces.bank.BankState.bankSearchMode
import org.alter.interfaces.ifOpenMainSidePair
import org.alter.interfaces.ifSetEvents

fun Player.openBank() {
    val bankInv = invMap.getValue("inv.bank")

    // Transmit bank inventory to client
    startInvTransmit(bankInv)

    // Open the bank main + side panel
    ifOpenMainSidePair("interfaces.bankmain", "interfaces.bankside")

    // Bank items: withdraw options (Op1-Op8), examine (Op10), drag
    ifSetEvents(
        "components.bankmain:items",
        0..799,
        IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4,
        IfEvent.Op5, IfEvent.Op6, IfEvent.Op7, IfEvent.Op8,
        IfEvent.Op10,
        IfEvent.DragTarget,
    )

    // Side panel inventory: deposit options (Op1-Op5), examine (Op10)
    ifSetEvents(
        "components.bankside:items",
        0..27,
        IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4,
        IfEvent.Op5,
        IfEvent.Op10,
    )

    // Tab headers: click to switch
    ifSetEvents(
        "components.bankmain:tabs",
        0..9,
        IfEvent.Op1,
    )

    // Reset transient state
    bankActiveTab = 0
    bankSearchMode = false
}

fun Player.closeBank() {
    val bankInv = invMap.getValue("inv.bank")
    stopInvTransmit(bankInv)
    bankSearchMode = false
}
```

- [ ] **Step 2: Compile check**

Run: `gradle :content:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: In-game test — bank opens and closes**

Click a bank booth → bank interface should open. Press Escape or X → should close without errors.

- [ ] **Step 4: Commit**

```bash
git add content/src/main/kotlin/org/alter/interfaces/bank/Extensions.kt
git commit -m "feat(bank): implement openBank() and closeBank()"
```

---

## Phase 3: Withdraw & Deposit Core

### Task 4: BankService — Withdraw Logic

**Files:**
- Create: `content/src/main/kotlin/org/alter/interfaces/bank/BankService.kt`

- [ ] **Step 1: Create BankService with withdraw and helper functions**

```kotlin
package org.alter.interfaces.bank

import org.alter.api.ext.message
import org.alter.game.model.ExamineEntityType
import org.alter.game.model.entity.Player
import org.alter.game.model.inv.Inventory
import org.alter.game.model.inv.invtx.invAdd
import org.alter.game.model.inv.invtx.invDel
import org.alter.game.model.item.Item
import org.alter.interfaces.bank.BankState.bankActiveTab
import org.alter.interfaces.bank.BankState.bankPlaceholderMode
import org.alter.interfaces.bank.BankState.bankSearchMode
import org.alter.interfaces.bank.BankState.bankWithdrawAsNote
import org.alter.interfaces.bank.BankState.getTabSizes
import org.alter.interfaces.bank.BankState.setTabSizes
import org.alter.interfaces.bank.BankState.tabEndSlot
import org.alter.interfaces.bank.BankState.tabForSlot
import org.alter.interfaces.bank.BankState.tabStartSlot

object BankService {

    // --- Helpers ---

    /** Get the bank inventory. */
    fun Player.getBankInv(): Inventory = invMap.getValue("inv.bank")

    /** Find slot of an item in bank by ID (amount > 0). Returns -1 if not found. */
    fun findItemSlot(bankInv: Inventory, itemId: Int): Int {
        for (i in bankInv.indices) {
            val item = bankInv[i] ?: continue
            if (item.id == itemId && item.amount > 0) return i
        }
        return -1
    }

    /**
     * Find a placeholder slot for the given real item ID.
     * Placeholder items have placeholderTemplate > 0 and placeholderLink == realItemId.
     */
    fun findPlaceholderSlot(bankInv: Inventory, realItemId: Int): Int {
        for (i in bankInv.indices) {
            val item = bankInv[i] ?: continue
            if (item.amount != 0) continue
            val def = item.getDef()
            if (def.placeholderTemplate > 0 && def.placeholderLink == realItemId) return i
        }
        return -1
    }

    /**
     * After removing an item from [slot], shift subsequent items in the same
     * tab down by one and decrement the tab size.
     */
    fun shiftSlotInTab(player: Player, bankInv: Inventory, slot: Int) {
        val sizes = player.getTabSizes()
        val tab = tabForSlot(sizes, slot)
        if (tab < 0) return

        val tabEnd = tabEndSlot(sizes, tab)

        for (i in slot until tabEnd - 1) {
            bankInv[i] = bankInv[i + 1]
        }
        bankInv[tabEnd - 1] = null

        sizes[tab]--
        player.setTabSizes(sizes)
    }

    // --- Withdraw ---

    /**
     * Withdraw items from bank at [slot] with given [amount].
     * Uses invDel with placehold flag, then invAdd with cert flag for noted mode.
     */
    fun withdraw(player: Player, slot: Int, amount: Int) {
        val bankInv = player.getBankInv()
        val bankItem = bankInv[slot] ?: return

        // Don't withdraw placeholders
        val def = bankItem.getDef()
        if (def.placeholderTemplate > 0 && def.placeholderLink > 0) return

        val actual = minOf(amount, bankItem.amount)
        if (actual <= 0) return

        // Check if noted withdrawal — determine output item
        val outputItemId = if (player.bankWithdrawAsNote) {
            val noted = Item(bankItem.id).toNoted()
            noted.id // returns same id if no noted form exists
        } else {
            bankItem.id
        }

        // Check inventory space first
        val canAdd = player.invAdd(
            player.inventory, outputItemId, actual,
            strict = false, autoCommit = false
        )
        if (!canAdd.success) {
            player.message("You don't have enough inventory space.")
            return
        }

        // Remove from bank with placeholder support
        val removed = player.invDel(
            bankInv, bankItem.id, actual,
            slot = slot,
            placehold = player.bankPlaceholderMode,
        )

        if (!removed.success) return

        // Add to inventory (with cert flag for noted conversion)
        player.invAdd(
            player.inventory, bankItem.id, actual,
            cert = player.bankWithdrawAsNote,
        )

        // If slot is now empty and no placeholder was left, shift within tab
        if (bankInv[slot] == null) {
            shiftSlotInTab(player, bankInv, slot)
        }
    }

    fun examine(player: Player, itemId: Int) {
        player.world.sendExamine(player, itemId, ExamineEntityType.ITEM)
    }
}
```

**Note:** The `invAdd` with `autoCommit = false` is used as a space check. If this doesn't work as a dry-run, use `player.inventory.freeSpace()` or `player.inventory.getItemCount(outputItemId)` to check space manually instead. Adapt based on what `autoCommit = false` actually does.

- [ ] **Step 2: Compile check**

Run: `gradle :content:compileKotlin`

- [ ] **Step 3: Commit**

```bash
git add content/src/main/kotlin/org/alter/interfaces/bank/BankService.kt
git commit -m "feat(bank): add BankService with withdraw logic"
```

---

### Task 5: BankService — Deposit Logic

**Files:**
- Modify: `content/src/main/kotlin/org/alter/interfaces/bank/BankService.kt`

- [ ] **Step 1: Add deposit function**

Add to `BankService`:

```kotlin
    /**
     * Deposit items from inventory at [invSlot] with given [amount].
     */
    fun deposit(player: Player, invSlot: Int, amount: Int) {
        val bankInv = player.getBankInv()
        val invItem = player.inventory[invSlot] ?: return

        // Get unnoted form for bank storage
        val unnotedItem = invItem.toUnnoted()
        val actual = minOf(amount, player.inventory.getItemCount(invItem.id))
        if (actual <= 0) return

        // Check if item already exists in bank or has a placeholder
        val existingSlot = findItemSlot(bankInv, unnotedItem.id)
        val placeholderSlot = if (existingSlot == -1) findPlaceholderSlot(bankInv, unnotedItem.id) else -1

        if (existingSlot == -1 && placeholderSlot == -1) {
            // Need a new slot — check capacity
            val totalItems = player.getTabSizes().sum()
            if (totalItems >= 800) {
                player.message("Your bank is too full.")
                return
            }
        }

        // If placeholder exists, replace it with real item at amount 0
        // so the invAdd will stack onto it at the correct position
        if (placeholderSlot != -1) {
            bankInv[placeholderSlot] = Item(unnotedItem.id, 0)
        }

        // Remove from inventory
        val removed = player.invDel(player.inventory, invItem.id, actual)
        if (!removed.success) return

        // Add to bank (uncert converts noted → unnoted automatically)
        val added = player.invAdd(bankInv, invItem.id, actual, uncert = true)

        if (!added.success) {
            // Rollback: give items back
            player.invAdd(player.inventory, invItem.id, actual)
            player.message("Your bank is too full.")
            return
        }

        // If this was a new item (no existing stack, no placeholder), update tab size
        if (existingSlot == -1 && placeholderSlot == -1) {
            val sizes = player.getTabSizes()
            val targetTab = if (player.bankSearchMode) 0 else player.bankActiveTab
            sizes[targetTab]++
            player.setTabSizes(sizes)
        }
    }

    /**
     * Deposit all items from player inventory.
     */
    fun depositInventory(player: Player) {
        for (slot in 0 until 28) {
            if (player.inventory[slot] != null) {
                deposit(player, slot, Int.MAX_VALUE)
            }
        }
    }

    /**
     * Deposit all equipped items and refresh appearance.
     */
    fun depositEquipment(player: Player) {
        val bankInv = player.getBankInv()
        val equipInv = player.equipment

        for (slot in equipInv.indices) {
            val item = equipInv[slot] ?: continue
            val unnotedId = item.toUnnoted().id

            val existingSlot = findItemSlot(bankInv, unnotedId)
            val placeholderSlot = if (existingSlot == -1) findPlaceholderSlot(bankInv, unnotedId) else -1

            if (existingSlot == -1 && placeholderSlot == -1) {
                val totalItems = player.getTabSizes().sum()
                if (totalItems >= 800) {
                    player.message("Your bank is too full.")
                    return
                }
            }

            if (placeholderSlot != -1) {
                bankInv[placeholderSlot] = Item(unnotedId, 0)
            }

            val removed = player.invDel(equipInv, item.id, item.amount, slot = slot)
            if (!removed.success) continue

            val added = player.invAdd(bankInv, item.id, item.amount, uncert = true)
            if (added.success && existingSlot == -1 && placeholderSlot == -1) {
                val sizes = player.getTabSizes()
                sizes[0]++ // Equipment always deposits to main tab
                player.setTabSizes(sizes)
            }
        }

        // Refresh player appearance — equipment changed
        // Use the same pattern as EquipAction: PlayerInfo(player).syncAppearance()
        org.alter.game.info.PlayerInfo(player).syncAppearance()
    }
```

- [ ] **Step 2: Compile check**

Run: `gradle :content:compileKotlin`

- [ ] **Step 3: Commit**

```bash
git add content/src/main/kotlin/org/alter/interfaces/bank/BankService.kt
git commit -m "feat(bank): add deposit, depositInventory, and depositEquipment"
```

---

## Phase 4: Event Wiring

### Task 6: BankPlugin — All Button Handlers

**Files:**
- Create: `content/src/main/kotlin/org/alter/interfaces/bank/BankPlugin.kt`

**Reference:** `content/src/main/kotlin/org/alter/interfaces/inventory/InventoryEvents.kt` for event pattern.

- [ ] **Step 1: Create BankPlugin with all handlers**

```kotlin
package org.alter.interfaces.bank

import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onButton
import org.alter.game.pluginnew.event.impl.onIfModalDrag
import org.alter.game.pluginnew.event.impl.onInterfaceClose
import org.alter.game.pluginnew.event.impl.onInterfaceOpen
import org.alter.interfaces.bank.BankState.bankActiveTab
import org.alter.interfaces.bank.BankState.bankInsertMode
import org.alter.interfaces.bank.BankState.bankLastXAmount
import org.alter.interfaces.bank.BankState.bankPlaceholderMode
import org.alter.interfaces.bank.BankState.bankPreSearchTab
import org.alter.interfaces.bank.BankState.bankQuantityType
import org.alter.interfaces.bank.BankState.bankSearchMode
import org.alter.interfaces.bank.BankState.bankWithdrawAsNote

class BankPlugin : PluginEvent() {

    override fun init() {

        // --- Interface lifecycle ---

        onInterfaceOpen("interfaces.bankmain") {
            // Bank just opened — additional setup if needed
        }

        onInterfaceClose("interfaces.bankmain") {
            player.closeBank()
        }

        // --- Withdraw (bank main items) ---

        onButton("components.bankmain:items") {
            when (op) {
                MenuOption.OP1 -> BankService.withdraw(player, slot, 1)
                MenuOption.OP2 -> BankService.withdraw(player, slot, 5)
                MenuOption.OP3 -> BankService.withdraw(player, slot, 10)
                MenuOption.OP4 -> {
                    // Withdraw-X: prompt for amount
                    player.queue {
                        val amount = inputInt(player, "How many do you wish to withdraw?")
                        if (amount > 0) {
                            player.bankLastXAmount = amount
                            BankService.withdraw(player, slot, amount)
                        }
                    }
                }
                MenuOption.OP5 -> BankService.withdraw(player, slot, Int.MAX_VALUE)
                MenuOption.OP6 -> {
                    // Withdraw All-but-1
                    val bankInv = player.invMap.getValue("inv.bank")
                    val bankItem = bankInv[slot] ?: return@onButton
                    val withdrawAmount = bankItem.amount - 1
                    if (withdrawAmount > 0) BankService.withdraw(player, slot, withdrawAmount)
                }
                MenuOption.OP7 -> {
                    // Withdraw last-X (or prompt if never set)
                    val lastX = player.bankLastXAmount
                    if (lastX <= 0) {
                        player.queue {
                            val amount = inputInt(player, "How many do you wish to withdraw?")
                            if (amount > 0) {
                                player.bankLastXAmount = amount
                                BankService.withdraw(player, slot, amount)
                            }
                        }
                    } else {
                        BankService.withdraw(player, slot, lastX)
                    }
                }
                MenuOption.OP8 -> BankService.releasePlaceholder(player, slot)
                MenuOption.OP10 -> BankService.examine(player, item)
                else -> {}
            }
        }

        // --- Deposit (bank side panel items) ---

        onButton("components.bankside:items") {
            when (op) {
                MenuOption.OP1 -> BankService.deposit(player, slot, 1)
                MenuOption.OP2 -> BankService.deposit(player, slot, 5)
                MenuOption.OP3 -> BankService.deposit(player, slot, 10)
                MenuOption.OP4 -> {
                    player.queue {
                        val amount = inputInt(player, "How many do you wish to deposit?")
                        if (amount > 0) {
                            player.bankLastXAmount = amount
                            BankService.deposit(player, slot, amount)
                        }
                    }
                }
                MenuOption.OP5 -> BankService.deposit(player, slot, Int.MAX_VALUE)
                MenuOption.OP10 -> BankService.examine(player, item)
                else -> {}
            }
        }

        // --- Bulk deposit ---

        onButton("components.bankmain:depositinv") {
            BankService.depositInventory(player)
        }

        onButton("components.bankmain:depositworn") {
            BankService.depositEquipment(player)
        }

        // --- Tab switching ---

        onButton("components.bankmain:tabs") {
            if (slot in 0..9) {
                player.bankActiveTab = slot
            }
        }

        // --- Toggles ---

        onButton("components.bankmain:note_graphic") {
            player.bankWithdrawAsNote = !player.bankWithdrawAsNote
        }

        onButton("components.bankmain:swap_insert_graphic") {
            player.bankInsertMode = !player.bankInsertMode
        }

        onButton("components.bankmain:placeholder_graphic") {
            player.bankPlaceholderMode = !player.bankPlaceholderMode
        }

        // --- Quantity presets ---
        // These set the default withdraw quantity mode.
        // Component names: bankmain:quantity1, quantity5, quantity10, quantityx, quantityall
        // Verify exact names from gamevals during implementation.

        // --- Search ---

        onButton("components.bankmain:search") {
            player.bankPreSearchTab = player.bankActiveTab
            player.bankSearchMode = true
            // TODO: Send client script to open search input box
        }

        // --- Item drag within bank ---

        onIfModalDrag("components.bankmain:items") {
            val from = selectedSlot ?: return@onIfModalDrag
            val to = targetSlot ?: return@onIfModalDrag
            BankService.moveItem(player, from, to)
        }
    }
}
```

**Note:** The `op` field in `ButtonClickEvent` is typed as `MenuOption`. Verify this by checking `ButtonClickEvent.kt`. If it's an `Int` instead, use `when(option)` with integer constants. Also verify `slot` and `item` are directly accessible fields on the event.

- [ ] **Step 2: Compile check**

Run: `gradle :content:compileKotlin`
Fix any API mismatches (field names, imports, method signatures).

- [ ] **Step 3: In-game test — basic deposit and withdraw**

Use `::item` command to get items, open bank:
- Deposit items via right-click on side panel
- Deposit-all inventory and equipment buttons
- Withdraw items from bank with all options
- Withdraw-X prompt works
- Toggle noted withdrawal

- [ ] **Step 4: Commit**

```bash
git add content/src/main/kotlin/org/alter/interfaces/bank/BankPlugin.kt
git commit -m "feat(bank): add BankPlugin with all button handlers"
```

---

## Phase 5: Tab Operations

### Task 7: BankService — Tab Create, Collapse, Move

**Files:**
- Modify: `content/src/main/kotlin/org/alter/interfaces/bank/BankService.kt`

- [ ] **Step 1: Add tab create function**

Add to `BankService`:

```kotlin
    /**
     * Create a new tab by moving item at [sourceSlot] to the first empty tab.
     */
    fun createTab(player: Player, sourceSlot: Int) {
        val bankInv = player.getBankInv()
        if (bankInv[sourceSlot] == null) return

        val sizes = player.getTabSizes()

        // Find first empty tab (1-9, tab 0 is always main)
        val newTab = (1..9).firstOrNull { sizes[it] == 0 } ?: run {
            player.message("You can't create any more tabs.")
            return
        }

        val sourceTab = tabForSlot(sizes, sourceSlot)
        if (sourceTab < 0) return

        // Save item and remove from source
        val savedItem = bankInv[sourceSlot]!!
        val sourceEnd = tabEndSlot(sizes, sourceTab)
        for (i in sourceSlot until sourceEnd - 1) {
            bankInv[i] = bankInv[i + 1]
        }
        bankInv[sourceEnd - 1] = null
        sizes[sourceTab]--

        // Calculate insert position for new tab
        val insertPos = tabStartSlot(sizes, newTab)

        // Shift everything from insertPos onward right by 1
        val totalItems = sizes.sum()
        for (i in totalItems downTo insertPos + 1) {
            bankInv[i] = bankInv[i - 1]
        }
        bankInv[insertPos] = savedItem
        sizes[newTab] = 1

        player.setTabSizes(sizes)
    }

    /**
     * Collapse a tab, moving all its items to end of tab 0 (main).
     */
    fun collapseTab(player: Player, tab: Int) {
        if (tab <= 0 || tab > 9) return
        val bankInv = player.getBankInv()
        val sizes = player.getTabSizes()
        val tabSize = sizes[tab]
        if (tabSize == 0) return

        val tabStart = tabStartSlot(sizes, tab)

        // 1. Copy the tab's items
        val tabItems = Array(tabSize) { bankInv[tabStart + it] }

        // 2. Remove the tab by shifting everything after it down
        val totalItems = sizes.sum()
        for (i in tabStart until totalItems - tabSize) {
            bankInv[i] = bankInv[i + tabSize]
        }
        for (i in totalItems - tabSize until totalItems) {
            bankInv[i] = null
        }
        sizes[tab] = 0

        // 3. Append copied items to end of tab 0
        // After removal, tab 0 still starts at 0, ends at sizes[0]
        val mainEnd = sizes[0]
        // Shift all non-main items right by tabSize to make room
        val nonMainTotal = sizes.drop(1).sum()
        for (i in mainEnd + nonMainTotal - 1 + tabSize downTo mainEnd + tabSize) {
            bankInv[i] = bankInv[i - tabSize]
        }
        for (i in tabItems.indices) {
            bankInv[mainEnd + i] = tabItems[i]
        }
        sizes[0] += tabSize

        // 4. Shift higher tabs down to stay contiguous
        for (i in tab until 9) {
            sizes[i] = sizes[i + 1]
        }
        sizes[9] = 0

        player.setTabSizes(sizes)
    }
```

- [ ] **Step 2: Add moveItem function (swap and insert mode)**

```kotlin
    /**
     * Move/swap an item within the bank. Respects insert vs swap mode.
     */
    fun moveItem(player: Player, fromSlot: Int, toSlot: Int) {
        if (fromSlot == toSlot) return
        val bankInv = player.getBankInv()

        if (player.bankInsertMode) {
            insertMove(player, bankInv, fromSlot, toSlot)
        } else {
            // Swap mode
            val temp = bankInv[fromSlot]
            bankInv[fromSlot] = bankInv[toSlot]
            bankInv[toSlot] = temp
        }
    }

    private fun insertMove(player: Player, bankInv: Inventory, fromSlot: Int, toSlot: Int) {
        val item = bankInv[fromSlot] ?: return
        val sizes = player.getTabSizes()

        val fromTab = tabForSlot(sizes, fromSlot)
        if (fromTab < 0) return

        // Remove from source position
        val fromEnd = tabEndSlot(sizes, fromTab)
        for (i in fromSlot until fromEnd - 1) {
            bankInv[i] = bankInv[i + 1]
        }
        bankInv[fromEnd - 1] = null
        sizes[fromTab]--

        // Adjust toSlot if it was shifted by the removal
        val adjustedTo = if (toSlot > fromSlot) toSlot - 1 else toSlot

        // Recalculate target tab with updated sizes
        val toTab = tabForSlot(sizes, adjustedTo)
        if (toTab < 0) return

        // Insert at target position
        val toEnd = tabEndSlot(sizes, toTab)
        for (i in toEnd downTo adjustedTo + 1) {
            bankInv[i] = bankInv[i - 1]
        }
        bankInv[adjustedTo] = item
        sizes[toTab]++

        player.setTabSizes(sizes)
    }
```

- [ ] **Step 3: Compile check**

Run: `gradle :content:compileKotlin`

- [ ] **Step 4: In-game test — tabs**

- Deposit several items
- Drag item to tab 2 header → tab created
- Switch between tabs
- Drag items within a tab (swap and insert modes)
- Collapse a tab → items return to main

- [ ] **Step 5: Commit**

```bash
git add content/src/main/kotlin/org/alter/interfaces/bank/BankService.kt
git commit -m "feat(bank): add tab create, collapse, and item rearrangement"
```

---

## Phase 6: Placeholders

### Task 8: Placeholder Release Functions

**Files:**
- Modify: `content/src/main/kotlin/org/alter/interfaces/bank/BankService.kt`

- [ ] **Step 1: Add releasePlaceholder and releaseAllPlaceholders**

Add to `BankService`:

```kotlin
    /**
     * Release a single placeholder at [slot].
     */
    fun releasePlaceholder(player: Player, slot: Int) {
        val bankInv = player.getBankInv()
        val item = bankInv[slot] ?: return
        val def = item.getDef()

        if (!(def.placeholderTemplate > 0 && def.placeholderLink > 0)) return

        bankInv[slot] = null
        shiftSlotInTab(player, bankInv, slot)
    }

    /**
     * Release all placeholders. Tab-aware compaction:
     * compact within each tab, then close inter-tab gaps.
     */
    fun releaseAllPlaceholders(player: Player) {
        val bankInv = player.getBankInv()
        val sizes = player.getTabSizes()

        // Pass 1: compact each tab in-place, removing placeholders
        var readOffset = 0
        var writeOffset = 0
        for (tab in 0..9) {
            val tabSize = sizes[tab]
            if (tabSize == 0) {
                continue
            }

            val tabReadStart = readOffset
            val tabWriteStart = writeOffset
            var kept = 0

            for (i in 0 until tabSize) {
                val item = bankInv[tabReadStart + i] ?: continue
                val def = item.getDef()
                if (def.placeholderTemplate > 0 && def.placeholderLink > 0 && item.amount == 0) {
                    continue // Skip placeholder
                }
                if (tabWriteStart + kept != tabReadStart + i) {
                    bankInv[tabWriteStart + kept] = item
                }
                kept++
            }

            // Null out remaining slots in this tab's old range
            for (i in tabWriteStart + kept until tabReadStart + tabSize) {
                bankInv[i] = null
            }

            sizes[tab] = kept
            readOffset += tabSize
            writeOffset += kept
        }

        // Null out any trailing slots
        for (i in writeOffset until 800) {
            if (bankInv[i] != null) bankInv[i] = null
        }

        player.setTabSizes(sizes)
    }
```

- [ ] **Step 2: Compile check**

Run: `gradle :content:compileKotlin`

- [ ] **Step 3: In-game test — placeholders**

- Enable placeholder mode toggle
- Deposit items, withdraw all → placeholder appears
- Deposit item back → fills placeholder slot
- Release single placeholder
- Release all placeholders

- [ ] **Step 4: Commit**

```bash
git add content/src/main/kotlin/org/alter/interfaces/bank/BankService.kt
git commit -m "feat(bank): add placeholder release single and release all"
```

---

## Phase 7: Polish & Verification

### Task 9: Edge Cases & Final Testing

**Files:**
- Possibly modify any of the bank files for fixes found during testing.

- [ ] **Step 1: Verify close handler fires correctly**

Open bank, close it. Check console for errors. If `onInterfaceClose` doesn't fire for the bank modal, investigate the close event pattern — it may require `onIfClose` instead, or the bank may close via a different mechanism (button click on X).

- [ ] **Step 2: Verify search restores previous tab**

```
open bank → switch to tab 3 → click search → close search → should be back on tab 3
```

The search close event should set `bankActiveTab = bankPreSearchTab`. Wire this to the appropriate close/cancel button if not already handled.

- [ ] **Step 3: Handle edge cases**

- Withdrawing from a slot that was modified between button click and handler execution
- Depositing items that would exceed `Int.MAX_VALUE` stack size
- Tab sizes going out of sync (add a debug `::bankfix` command if needed)

- [ ] **Step 4: Full compile**

Run: `gradle :content:compileKotlin`

- [ ] **Step 5: Comprehensive in-game test**

1. Open bank via booth and banker NPC
2. Deposit items (1, 5, 10, X, All)
3. Deposit inventory / equipment buttons
4. Withdraw items (1, 5, 10, X, All, All-but-1)
5. Noted withdrawal toggle
6. Create tabs, switch tabs, collapse tabs
7. Swap vs insert rearrange mode
8. Placeholder toggle → withdraw all → verify placeholder
9. Deposit over placeholder → fills slot
10. Release single and all placeholders
11. Search toggle
12. Close and reopen → verify persistent state

- [ ] **Step 6: Commit**

```bash
git add -A content/src/main/kotlin/org/alter/interfaces/bank/
git commit -m "feat(bank): polish and edge case fixes"
```

---

### Task 10: Final Build Verification

- [ ] **Step 1: Full project build**

Run: `gradle build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Final commit if any remaining changes**

```bash
git add -A
git commit -m "feat(bank): complete bank interface implementation"
```
