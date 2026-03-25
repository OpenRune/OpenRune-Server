# Bank Interface Design Spec

**Goal:** Implement a full-featured OSRS bank interface with withdraw/deposit (all quantity options), 9 tabs, placeholders, noted withdrawal, search, and bulk deposit buttons. No bank PIN.

**Architecture:** Split by concern into 4 content files. Leverages the existing `InvTransactions` DSL for item transfers with built-in placeholder/cert handling.

---

## 1. Data Model & State

### Bank Inventory

Already defined: `ContainerKey("bank", "inv.bank", capacity = 800, stackType = STACK)`. All items stack in the bank regardless of normal behavior. Must be added to `InvMapInit.defaultInvs` so it initializes on login.

### Player State (`BankState.kt`)

Tab sizes and toggles are stored as **varps/varbits** (persisted across logout), not transient attributes.

| State | Storage | Default | Purpose |
|-------|---------|---------|---------|
| Tab sizes | Varps (9 values) | all zeros | Item count per tab. Tab 0 = main, tabs 1-8 = custom. Sum = total items. |
| Withdraw-as-note | Varbit | false | Toggle for noted withdrawal |
| Placeholder mode | Varbit | false | Toggle for placeholder mode |
| Rearrange mode | Varbit | 0 (swap) | 0 = swap, 1 = insert mode for drag-and-drop |
| Last X amount | Transient attr | 0 | Last entered Withdraw-X / Deposit-X amount (not persisted) |
| Search mode | Transient attr | false | Whether search is active |
| Active tab | Transient attr | 0 | Currently viewed tab (for deposit targeting) |
| Pre-search tab | Transient attr | 0 | Tab to restore when search closes |

### Tab Layout

Items in the 800-slot container are arranged contiguously by tab. Tab boundaries derived from cumulative sizes:

- Tab 0 (main): `0 ..< sizes[0]`
- Tab 1: `sizes[0] ..< sizes[0]+sizes[1]`
- Tab N: `sum(sizes[0..N-1]) ..< sum(sizes[0..N])`

No per-item tab tracking — just the sizes array.

### Placeholders

Cache items have `placeholderTemplate` and `placeholderLink` fields on `ItemServerType` (which already provides `isPlaceholder: Boolean` checking both fields). When placeholder mode is on and all of an item is withdrawn, replace slot with the placeholder variant at amount 0. Client renders these as greyed-out automatically.

Depositing an item that has an existing placeholder in the bank replaces the placeholder with the real item, preserving position.

---

## 2. Transaction System Integration

The codebase has an `InvTransactions.transaction {}` DSL that natively handles placeholder creation, noted/unnoted conversion, and slot management. Bank operations **must** use this system rather than hand-rolling remove/add/placeholder logic.

Key transaction flags:

- `transfer { placehold = true }` — automatically replaces emptied source slots with placeholder variants
- `transfer { uncert = true }` — converts noted items to unnoted form during transfer (deposit)
- `transfer { cert = true }` — converts items to noted form during transfer (noted withdrawal)
- `insert {}` — placeholder-aware slot resolution (replaces existing placeholders)
- `delete { placehold = true }` — replaces deleted slots with placeholders

Tab-aware slot shifting is a **post-transaction fixup**: after the transaction completes, adjust the flat container's slot positions and update tab sizes. The transaction system operates on flat inventories and does not understand tab boundaries.

**No new Item.kt helpers needed.** `ItemServerType` already has `isPlaceholder`, `placeholderLink`, and `placeholderTemplate`. The Transaction system handles placeholder resolution internally.

---

## 3. Opening & Closing

### `Player.openBank()` (`Extensions.kt`)

1. Get or create bank inventory from `player.invMap` using `inv.bank`
2. Transmit bank inventory to client: `player.startInvTransmit(bankInv)`
3. Transmit `inv.inv` for the side panel
4. Open interface pair: `interfaces.bankmain` + `interfaces.bankside`
5. Send client scripts to configure right-click options on both interfaces (withdraw/deposit amount menus)
6. Send tab sizes to client via varps
7. Send toggle states (noted mode, placeholder mode, rearrange mode) via varbits
8. Set `ACTIVE_TAB = 0`

### Close (via `onInterfaceClose`)

1. Stop transmitting bank inventory: `player.stopInvTransmit(bankInv)`
2. Clear search mode
3. No explicit save — container persists on player model, varps serialized on logout

### Side Panel

`bankside` shows the player's normal inventory with deposit options. Server transmits `inv.inv` and sets correct button event access masks. Client handles the visual.

---

## 4. Withdraw

**Trigger:** Button clicks on `components.bankmain:items`, options 1-10.

| Option | Action |
|--------|--------|
| 1 | Withdraw-1 |
| 2 | Withdraw-5 |
| 3 | Withdraw-10 |
| 4 | Withdraw-X (prompt for amount, store in `LAST_X_AMOUNT`) |
| 5 | Withdraw-All |
| 6 | Withdraw-All-but-1 |
| 7 | Withdraw-X (reuse `LAST_X_AMOUNT`; if 0, show prompt instead) |
| 8 | Release placeholder |
| 9 | (reserved for drag/move) |
| 10 | Examine |

### `BankService.withdraw(player, slot, amount)`

1. Validate slot in bounds, contains real item (not placeholder via `ItemServerType.isPlaceholder`)
2. Clamp: `actual = min(requested, bank[slot].amount)`
3. Use `InvTransactions.transaction`:
   - `transfer` from bank to inventory
   - If `WITHDRAW_AS_NOTE`: set `cert = true` on the transfer. Items with no noted form transfer as-is
   - If `PLACEHOLDER_MODE`: set `placehold = true` on the transfer
4. If transaction fails (no inventory space): "You don't have enough inventory space." — return
5. Post-transaction fixup: if bank slot was emptied and no placeholder was left, shift items within the tab and decrement tab size
6. Refresh both inventories to client

**All-but-1:** `amount = bank[slot].amount - 1`. If only 1 exists, do nothing.

---

## 5. Deposit

**Trigger:** Button clicks on `components.bankside:items`, options 1-10.

Mirrors withdraw: Deposit-1, Deposit-5, Deposit-10, Deposit-X (prompt; if `LAST_X_AMOUNT == 0` show prompt), Deposit-All, Examine.

### `BankService.deposit(player, invSlot, amount)`

1. Get item from player inventory
2. Use `InvTransactions.transaction`:
   - `transfer` from inventory to bank with `uncert = true` (auto-converts noted to unnoted)
   - Transaction system auto-matches placeholders in the bank for the unnoted item ID
3. If item is new to bank (no existing stack, no placeholder): insert at end of active tab's slot range, increment tab size
4. If bank full: "Your bank is too full." — return
5. Refresh both inventories

### Bulk Deposit Buttons

- **Deposit inventory** (`components.bankside:deposit_inventory`): Loop all 28 inventory slots, deposit each non-null item
- **Deposit equipment** (`components.bankside:deposit_equipment`): Loop all 14 equipment slots, transfer each from `inv.worn` to bank. **Refresh player appearance** after all items deposited (equipment changes affect player model)

---

## 6. Tab System

### Creating a Tab

Player drags an item to an empty tab header:
1. Remove item from source position (shift items in source tab, decrement source tab size)
2. Find first empty tab slot (first tab with size 0)
3. Insert item at start of new tab's range
4. Set new tab size to 1

### Collapsing a Tab

1. Move all items from target tab to end of tab 0 (main)
2. Set target tab size to 0
3. Shift higher-numbered tabs down to fill gap (tabs stay contiguous — no empty tabs between populated ones)

### Switching Tabs

Client-side display only. Server sends tab sizes via varps, client shows the correct slice. On tab click, server updates `ACTIVE_TAB` so deposits target the right tab.

### Moving Items

Drag events reorder within the container:
- **Swap mode** (default): swap the two slots. If between tabs, no tab size change needed
- **Insert mode**: remove from source, shift items in source tab, insert at target position, shift items in target tab. Adjust both tab sizes if cross-tab

### Tab Icons

Client automatically uses first item in each tab as icon. No server work needed.

---

## 7. Placeholders

### Toggle

`PLACEHOLDER_MODE` toggled via button on bank interface. Sent to client via varbit.

### On Withdraw (all)

Handled by `transfer { placehold = true }` in the Transaction system. When the source slot is emptied, it is automatically replaced with the placeholder variant.

### On Deposit

Transaction system auto-matches placeholders when inserting into the bank. If a placeholder for the deposited item (unnoted form) exists, it replaces the placeholder with the real item, preserving position and tab.

### Release Single

Right-click option on placeholder item: remove it, shift items in tab, decrement tab size.

### Release All

Button on bank interface. **Tab-aware compaction algorithm:**

1. For each tab (0-8), in order:
   - Walk the tab's slot range, identify placeholder items (amount 0, `isPlaceholder`)
   - Remove placeholders and compact remaining items within the tab's range
   - Update that tab's size to reflect removed placeholders
2. After all tabs processed, shift all tab ranges to be contiguous (close gaps left by reduced tab sizes)
3. Send updated tab sizes via varps
4. Refresh bank inventory to client

---

## 8. Search

1. Player clicks search button on bank interface
2. Server stores `PRE_SEARCH_TAB = ACTIVE_TAB`, sets `BANK_SEARCH_MODE = true`
3. Send client script to open search input box
4. Client filters items locally by name — no server-side filtering
5. While searching, deposits go to tab 0 (main)
6. On close (button or Escape): server sets `BANK_SEARCH_MODE = false`, restores `ACTIVE_TAB = PRE_SEARCH_TAB`

---

## 9. File Structure

| File | Purpose |
|------|---------|
| `content/.../interfaces/bank/BankState.kt` | Varp/varbit property delegates for toggles and tab sizes, transient attribute keys, constants |
| `content/.../interfaces/bank/BankService.kt` | Stateless functions: `withdraw()`, `deposit()`, `depositInventory()`, `depositEquipment()`, `createTab()`, `collapseTab()`, `moveItem()`, `releasePlaceholder()`, `releaseAllPlaceholders()`, tab-aware shift/compact helpers |
| `content/.../interfaces/bank/BankPlugin.kt` | PluginEvent registering all `onButton` handlers for `bankmain`/`bankside`, `onInterfaceOpen`/`onInterfaceClose` |
| `content/.../interfaces/bank/Extensions.kt` | `Player.openBank()` / `Player.closeBank()` orchestration (existing file, currently empty) |

No changes to `Item.kt` — existing `ItemServerType` and Transaction system provide all placeholder/noted handling.

No changes needed to `BankerPlugin.kt` or `BankBoothsPlugin.kt` — they already call `player.openBank()`.

Bank inventory added to `InvMapInit.defaultInvs` for auto-init on login.

RSCM component names (`bankmain:*`, `bankside:*`) will be looked up from gamevals during implementation.

---

## 10. Out of Scope

- Bank PIN system (separate future feature)
- Grand Exchange collect box
- Bank fillers
- Looting bag / seed vault integration
- Group ironman shared bank
- Bank capacity display (X/800 counter — may be client-driven, investigate during implementation)
