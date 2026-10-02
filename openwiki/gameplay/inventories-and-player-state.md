---
type: subsystem
title: Inventories, vars and player state
description: How content mutates inventories through atomic objtx transactions (invAdd/invDel/invTransfer), how virtual item storage hooks redirect items, how player state should be stored in varps/varbits via property delegates (attrs as last resort), and how stats xp modifiers and shops build on these.
tags: [inventory, transactions, varbits, varps, attrs, shops, stats]
verified:
  - by: openwiki/0.7.0
    at: 2026-10-02T20:46:41.242Z
sources:
  - id: openwiki-source-093e5b76d36998c2c8d9083f
    resource: repo://api/attr/src/main/kotlin/org/rsmod/api/attr/AttributeKey.kt
  - id: openwiki-source-3e78ee266d047c7cd621c713
    resource: repo://api/inv-storage/src/main/kotlin/org/rsmod/api/inv/storage/PlayerItemStorageHook.kt
  - id: openwiki-source-82ee320f8e7294e80cbd1626
    resource: repo://api/invtx/src/main/kotlin/org/rsmod/api/invtx/InvTransactionExtensions.kt
  - id: openwiki-source-e9242d566bb93463c416d700
    resource: repo://api/invtx/src/main/kotlin/org/rsmod/api/invtx/InvTransactions.kt
  - id: openwiki-source-9f04a46e2b0d4fa98cfffc37
    resource: repo://api/player/src/main/kotlin/org/rsmod/api/player/vars/VarPlayerTypeDelegates.kt
  - id: openwiki-source-0e843ac6c9187884bbf61576
    resource: repo://api/shops/src/main/kotlin/org/rsmod/api/shops/Shops.kt
  - id: openwiki-source-2d4debbc6e2fefb23ab45b17
    resource: repo://content/areas/city/draynor/src/main/kotlin/org/rsmod/content/areas/city/draynor/ErnestLeverPuzzle.kt
  - id: openwiki-source-cbd80468a2644e75ddb1d3e5
    resource: repo://engine/objtx/src/main/kotlin/org/rsmod/objtx/Transaction.kt
generated: { by: "claude-code", at: "2026-10-02T20:46:41.242Z" }
---

# Inventories, vars and player state

## Inventory transactions

All inventory mutation goes through a transaction engine so multi-step changes (take coins *and*
add an item, move between bank and inventory) are atomic.

### objtx (engine)

`Transaction<T>` (`engine/objtx`) is a small query DSL over registered
`TransactionInventory`s: `insert`, `delete`, `transfer`, `swap`, `dump`, `compact`. Each query
appends a `TransactionResult`; an `Err` result throws `TransactionCancellation`, aborting the
rest of the block. Cert/uncert (noted items), placeholders, transforms, stackability and
dummy items are resolved through lookup tables supplied to the transaction.

### InvTransactions (api)

`InvTransactions` (`api/invtx`) binds the engine to game objs:

- `InvTransactions.from()` builds the cert/transform/placeholder/stackable/dummyitem lookups once
  from all item types in `ServerCacheManager`.
- `transaction(autoCommit) { ... }` runs the block, swallows `TransactionCancellation`, and
  commits **only if every query succeeded and `autoCommit` is true**. With `autoCommit = false`
  the caller inspects the result list and commits explicitly.

Content uses the `Player` extensions in `InvTransactionExtensions.kt`:

| Function | Purpose |
|---|---|
| `invAdd(inv, obj, count, ...)` | add, optionally strict/slot/cert/uncert; may be redirected into virtual storage |
| `invAddOrDrop` | add, or drop on the floor if full |
| `invDel`, `invDelAll` | delete by obj/slot |
| `invTransfer`, `invMoveAll`, `invSwap`, `invCompress` | move between/within inventories |
| `invTakeFee(fee)` | take coins, returning success |

Inside a combined transaction use the `Transaction<InvObj>.add/delete/transfer/...` forms so the
whole set succeeds or fails together. Each returns a `TransactionResultList` with `success` /
`failure` and per-query details.

### Virtual item storage

`api/inv-storage` defines `PlayerItemStorageHook` (`shouldProcess`, `contains`, `remove`, `add`)
for containers that are not real inventories (e.g. an open coal bag). `invAdd` can redirect into
these hooks unless `ignoreVirtualStorage = true`; consume policies decide whether stored items
count toward requirements.

## Vars: the default place for player state

Per the project guidelines, simple state (ints, booleans, enums, coords, counters, flags,
timers) is stored in **varps/varbits**, not in attributes:

- persistent across logout → a permanent varp/varbit;
- session-only → a temporary varp/varbit.

`api/player/.../vars/VarPlayerTypeDelegates.kt` provides Kotlin property delegates keyed by
gameval symbol:

```kotlin
private var ProtectedAccess.leverA by boolVarBit("varbit.ernestlever_a")
val Player.ardougneEasyDiary: Boolean by boolVarBit("varbit.ardougne_diary_easy_complete")
```

Available delegates: `intVarp`, `strVarp`, `boolVarp`, `typeCoordVarp`, `typeNpcUidVarp`,
`typePlayerUidVarp`, `typeIntVarp`, `enumVarp`/`enumVarpOrNull`, and the varbit equivalents
(`intVarBit`, `boolVarBit`, `typeCoordVarBit`, `typeIntVarBit`, `enumVarBit`...). Custom varp /
varbit ids are declared in a module's `gamevals.toml` (see
[Cache build and gamevals](../cache/cache-build-and-gamevals.md)). Varps marked transmit are sent
to the client automatically; `ServerCacheManager` precomputes the transmitted list.

### Attributes (last resort)

`AttributeKey<T>(persistenceKey, resetOnDeath, temp)` and `AttributeMap` (`api/attr`) hold
arbitrary typed values. A non-null `persistenceKey` makes the value saved with the character
(avoid `Double`/`Float` for persisted keys). Use attrs only for data a var cannot represent —
complex objects or runtime-only references — never as a shortcut to registering a var.

Persistence of vars, inventories and stats is described in
[Accounts and database persistence](../persistence/accounts-and-database.md).

## Stats and xp modifiers

`api/stats` contains `levelmod` (invisible level boosts) and `xpmod` (`XpModifiers`,
`WornXpModifiers`, `StatXpMod`) used when awarding xp — e.g. outfit bonuses — without each skill
re-implementing the maths. Stat updates are flushed to the client in player post-tick (see
[Game cycle and tick processing](../architecture/game-cycle.md)).

## Shops

`Shops.open(player, title, shopInv, ...)` (`api/shops`):

- resolves the shop inventory by `inv.*` symbol — **shared** scope invs are one global inventory
  for all players, non-shared ones live in the player's `invMap`;
- records `player.openedShop` (currency, buy/sell/change percentages);
- starts transmitting both inventories, opens `interface.shopmain` + `interface.shopside`, and
  sets op events on the item components.

Buying/selling is implemented by `ShopScript` and `ShopOperations` (standard gp and alternative
currencies — obj currencies or varbit currencies from the shop currency db table).
`ShopRestockProcess` restores modified shops toward their initial stock over time.

A typical NPC shopkeeper: `onOpNpc3("npc.bob") { player.openShop(it.npc) }` (see
[Writing content plugins](../plugins/plugin-scripts.md)).
