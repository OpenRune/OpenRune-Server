# Ironman

Modes sync via `Player.gamemode` + `varbit.ironman`. Admin: `::gamemode normal|ironman|uim|hcim`.

## UIM bank

Gate with `IronmanRestrictions.blockUimBank(player)` or `ProtectedAccess.tryOpenBank()`.
`inv.bank` is tagged `uimBlocked = true` for the same rule via `blockUimInventory`.

Unnote still works (note on banker / bank booth).

For other storage: set `uimBlocked = true` on the inv in `inv.toml`, re-pack, then `blockUimInventory`.

## HCIM safe deaths

Unsafe death demotes to Ironman. Before a scripted safe death:

```kotlin
player.markNextDeathSafe()
```

## Enforced

- Trade, foreign loot, Accept Aid
- UIM: no bank / deposit box; death keeps 0 items
- HCIM: unsafe death demotes
- Ironman: no combat XP (and ironman-blocked hitmark) on NPCs already damaged by another player
