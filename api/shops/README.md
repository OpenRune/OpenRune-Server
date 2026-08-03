# Shops

Shared shop UI and buy/sell logic. Payment media come from `dbtable.shop_currency` — do not copy shop operations per currency.

## Flow

1. Add a key to `.data/gamevals/currency.rscm`
2. Add a row in `or-cache/.../ShopCurrencyTable.kt` (`dbtable.shop_currency`)
3. Pack the server cache (regenerates table wrappers)
4. Open the shop with `currency = "currency.<key>"`

`ShopScript` loads every shop-currency row at startup and registers `StandardCurrencyShopOperations`.

## Dbrow shape

| Column | Type | Meaning |
| --- | --- | --- |
| `key` | STRING | `currency.rscm` key (e.g. `currency.tokkul`) |
| `singular_name` | STRING | "coin", "tokkul", "Slayer point" |
| `plural_name` | STRING | "coins", "tokkul", "Slayer points" |
| `obj` | OBJ | Inventory payment item (omit for varbit currencies) |
| `varbit` | INT | Payment varbit via `columnRSCM` (omit for inv currencies) |

Set **either** `obj` **or** `varbit` — the other stays unset/null.

### Inventory currency

```kotlin
row("dbrow.shop_currency_tokkul") {
    column(KEY, "currency.tokkul")
    column(SINGULAR_NAME, "tokkul")
    column(PLURAL_NAME, "tokkul")
    columnRSCM(OBJ, "obj.tokkul")
}
```

### Varbit currency

```kotlin
row("dbrow.shop_currency_slayer_points") {
    column(KEY, "currency.slayer_points")
    column(SINGULAR_NAME, "Slayer point")
    column(PLURAL_NAME, "Slayer points")
    columnRSCM(VARBIT, "varbit.slayer_points")
}
```

## Kotlin hooks (optional)

Most currencies need only the dbrow. Use hooks when pricing or client sync is custom:

```kotlin
// Fixed / non-GE costs (looked up at purchase time)
shopOps.costOf("currency.stardust") { type ->
    fixedPrices[type] ?: type.cost.coerceAtLeast(1)
}

// Varbit UI sync after deduct/credit
shopOps.sync("currency.slayer_points") { player ->
    // e.g. SlayerRewardsPoints.syncPoints(player)
}
```

## Opening a shop

```kotlin
shops.open(
    player = player,
    title = "Dusuri's Star Shop",
    shopInv = "inv.magictraining_inventory",
    buyPercentage = 80.0,
    sellPercentage = 100.0,
    changePercentage = 0.0,
    currency = "currency.stardust",
)
```

NPC-driven opens default to `currency.standard_gp` unless you pass another key.

## Key types

- `ShopCurrencyTable` — dbtable definition (or-cache)
- `ShopCurrencyRows` — loads rows into `ShopCurrency`
- `ShopOperationMap.registerCurrenciesFromDb` — startup registration
- `ShopOperationMap.costOf` / `sync` — optional Kotlin overrides
- `Shops.open` — open the shop UI with a currency key
