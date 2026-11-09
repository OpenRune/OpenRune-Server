# Grand Exchange

`content/other/grand-exchange` runs the Grand Exchange through the official interfaces
(`ge_offers` 465, `ge_offers_side` 467, `ge_history` 383) and their own clientscripts. Clerks
(Talk-to, Exchange, History) and the Exchange booths open it.

## Offers

- Eight slots per character. A buy takes `price x quantity` coins from the inventory; a sell takes
  the items, plain ones first and then notes.
- Players' offers match each other first: the best price wins, and the oldest offer wins a tie. A
  trade runs at the price of the offer that was placed first, so a buyer who bid above a
  resting sell is refunded the difference into their collection box.
- Anything still open fills against the simulated market (below).
- Collect from the offer view (items as notes, as items, or to the bank) or with "Collect all" on
  the index. Coins stop at the inventory's 2,147,483,647 cap and the rest stays in the box.
- Abort moves whatever is left into the collection box. Modify offer is not implemented; abort and
  place a new one.
- History lists the last 20 completed trades.

Not implemented: the 2% sell tax, 4-hour buy limits, item sets and the collection box interface
(`ge_collect` 402).

## Prices and the simulated market

`GePrices` gives each item a guide price:

1. **Live**: with `live-prices: true`, a daemon thread fetches `/latest` and `/1h` from
   `prices.runescape.wiki` every `refresh-minutes`, sending `user-agent`. An item's price is its
   1h average where it traded in the last hour, otherwise its latest trades. The game thread only
   ever reads the last good result.
2. **Stored**: `enum.ge_guide_prices` in the cache, built from the wiki's 24h averages. It covers
   items the live data doesn't, and everything when live prices are off or the wiki is
   unreachable.

An offer within `wiggle-room-percent` of the guide price fills against the market:

- A buy at or above `guide - band` fills. It pays the lower of its own price and the guide price.
- A sell at or below `guide + band` fills. It gets the higher of its own price and the guide price.
- Anything further out waits for a player, and is checked again after each live price refresh.
- `fill-mode: instant` fills the whole offer at once. `gradual` fills `gradual-fill-percent` of it
  every `gradual-fill-seconds`.
- Items with no guide price only trade between players.

Refresh the stored prices with `node tools/ge-prices/snapshot.mjs`, then `gradlew buildCache`.

## Config (`game.yml`)

```yaml
gameplay:
  grand-exchange:
    enabled: true
    live-prices: true
    refresh-minutes: 10
    user-agent: "OpenRune-Server Grand Exchange (github.com/osrs-solo/OpenRune-Server)"
    wiggle-room-percent: 5.0
    fill-mode: "instant"
    gradual-fill-percent: 10.0
    gradual-fill-seconds: 30
```

## Storage and dupe safety

- Every offer lives in `GeBook`, in memory on the game thread, with offline players' offers
  included. It loads from `public.ge_offers` and `public.ge_history` at startup; those tables are
  keyed by world type and character id.
- A player's rows are written inside their character save (`GeOfferPipeline`), in the same
  transaction as their inventory. Placing an offer or collecting from one asks for that save at
  once, and the offer is held out of matching until it lands. Because of that, a crash can
  never keep both the inventory change and the offer change, or lose both.
- A fill writes every offer it touches in one transaction. Each row carries a version, and an
  older write never overwrites a newer one.
- `GeBookTest` covers matching, prices and conservation. `GrandExchangeItemFlowTest` covers every
  inventory move: no coins, a full inventory, the coin cap, double confirms, vanished items and
  collecting twice.
