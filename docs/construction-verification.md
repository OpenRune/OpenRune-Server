# Construction: in-client verification

This checklist targets the paths that are **new or changed** rather than re-testing the whole skill.

**Read "Known gaps found by running this checklist" at the bottom first.** It was driven in a client
on 2026-09-13: three dispatch bugs were found and fixed, and one is still open that stops furniture
being built at all, which blocks sections A to D.

## Setup

1. Start the server: `gradlew run`. Wait for `Server ready in`.
   Confirm the boot log says:

   ```
   Loaded 30 house rooms, 184 hotspots, 172 doorways, 483/500 furniture locs resolved.
   ```

2. Launch `C:\Users\Christian\AppData\Local\RSProx\RSProx.exe`, click **Launch Session** in the
   launcher window (it does nothing until you do, and it can be sitting minimised).
3. Log in as `getest123` / `getest`.
4. Kit out:

   ```
   ::master
   ::item 8794 1     saw
   ::item 2347 1     hammer
   ::item 1539 500   nails
   ::item 960 500    plank
   ::item 8778 500   oak plank
   ::item 8782 500   mahogany plank
   ::item 8790 100   bolt of cloth
   ```

5. `::buildmode` to enter the house with hotspots showing.

When you finish, **log out through the game's own logout tab** before stopping the server. Killing
the server with a player logged in loses that character's save.

---

## A. Furniture appears at all

This is the 203 -> 483 change. Before it, most furniture built successfully and then showed nothing.

| Step | Expected |
|---|---|
| Build a **Crude wooden chair** in a parlour hotspot | A chair model appears on the hotspot tile |
| Build an **Oak larder** in a kitchen hotspot | A larder appears, not an empty tile |
| Build a **Tool store 1** in the workshop | A tool store appears |
| Build an **Oak altar** in the chapel | An altar appears |

A hotspot that clears but stays empty is the failure to look for. The boot log warns
`Furniture 'X' has no matching loc to place.` for anything it could not resolve, so check the
console before assuming the loc is wrong.

## B. Multi-tile furniture covers its whole hotspot (new code path, highest risk)

`spawnFurniture` used to place one loc at the hotspot's anchor tile. It now places one loc per
hotspot part, paired by the reference table's own hotspot-loc to built-loc mapping. **Nothing has
exercised this.**

| Step | Expected |
|---|---|
| Build a **Brown rug** (parlour, the rug hotspot) | The rug covers the full rug area: corners, sides and middle all rendered, no gaps and no single lonely tile |
| Look at the corners specifically | Corner tiles use the corner model, not a repeated middle tile |
| Build a **Hedge** in the formal garden | Same: ends, middles and corners all present |

Failure modes worth distinguishing:
- **One tile only** -> the multi-part path did not trigger; the piece is being treated as single-loc.
- **All tiles look identical** -> the hotspot's loc ids are missing from that piece's `parts` column
  in `furniture-locs.tsv`, so every part fell back to the first loc. The boot log lists these:
  13 of 109 pieces are in that state today.
- **Tiles in the wrong places** (corner art on a middle tile) -> the pairing is mismatched.

## C. Removing furniture removes every part

`despawnFurniture` was made symmetric with the above. If it is wrong, removal leaves orphan tiles
that persist until relog.

| Step | Expected |
|---|---|
| Remove the Brown rug from B | Every tile disappears, not just the anchor |
| Rebuild it, then remove again | Same, repeatably |
| Leave the house and re-enter (`::house`) | No leftover rug tiles |

## D. XP is the real value, not the material total

Watch the Construction xp drop on each build. Base values (before any xp rate multiplier the realm
applies, so compare ratios if your rate is not 1x):

| Build | Expected base xp |
|---|---|
| Crude wooden chair | 58 |
| Oak chair | 120 |
| Mahogany armchair | 280 |
| Brown rug | 30 |
| Tool store 1 | 120 |

Then build something **not** made of planks (a steel-bar or soft-clay piece, e.g. a **Steel range**
in the kitchen). It must award non-zero xp. Awarding 0 there is the old material-derived behaviour
and means the table lookup is not being consulted.

## E. Hotspots that only exist because of the override table

These rooms found zero or few hotspots before. Build one piece in each to confirm the slot is real
and placed in a sensible spot, not floating or inside a wall.

| Room | Build |
|---|---|
| Costume room | Oak costume box |
| Chapel | Shuttered window (the window slot) |
| Menagerie | Oak house, then the pet feeder |
| Superior garden | A bench, then a theme |
| League hall | A rug |
| Combat room | Boxing ring |

Superior garden's theme is the widest multi-part piece in the game (eight parts), so it is the
strongest test of section B as well.

## F. Hotspots found by the fused-digit naming rule

These were invisible until the `poh_dungeon_4l` / `poh_workshop_3a` style names were handled.

| Room | Build |
|---|---|
| Workshop | Tool store (slot 3) |
| Dungeon corridor / cross / stairs | An Oak door on slots 4 and 5 |
| Treasure room | The door slot |
| Formal garden | The slot 5 piece |

---

## Known gaps: do not report these as bugs

Three hotspot slots have no loc anywhere in their room's chunk, so they cannot be built:

- Portal room slots 4, 5, 6
- Throne room slot 2
- Achievement gallery slot 5

Seventeen furniture rows are leagues and seasonal cosmetics with no loc mapping: the Deadman and
Raging Echoes rugs, Forestry beehives, the halloween pumpkin, Alchemical Hydra head, Gnome Child
icon, two bingo scroll pieces, the league 5 scrying pool and spirit trees, and five league hall
pieces. The boot log names all seventeen.

`::pohrooms` dumps every room with its hotspots, and `::pohrooms <name>` filters to one, which is
the quickest way to see what a room is supposed to offer before you build in it.

`::pohwhere` now also prints the room's doorways and, for the tile under you, which neighbouring
slot that doorway leads to and whether anything is already in it. A room can only be added through
a doorway whose neighbour is empty.

**The build menus do not render in the chatbox.** Both the room list and the furniture list are
centred scroll interfaces (group 187 and 458), so screenshot the middle of the viewport, not the
bottom. Cropping the chatbox makes a menu that opened correctly look like a silent failure.

---

## Known gaps found by running this checklist

Driven in a client on 2026-09-13. Seven bugs were found and fixed; Construction now builds, and the
sections above have all been walked at least once.

**Fixed and confirmed in a client:**

- Every house option is at **op5**, not op1. Hotspots are `ops=[4=Build]`, built pieces are
  `ops=[0=Sit-on, 4=Remove]`. Building, room building and removal were bound to the wrong op, so
  nothing in a house responded to a click at all.
- The build menu's entries are `hide=yes` in the cache, and carry no position, and nothing arranges
  them, so the server shows and places each one.
- The entry script clears its component and rebuilds the children, so click events have to be set
  **after** the entries are filled, not before.
- Each entry is its own component, so the chosen slot comes from the component, not a subcomponent.
- `dbtable.furniture` asks for `obj.any_nails`, a build menu placeholder nobody can hold, so every
  plank-and-nails build failed its material check. A build now spends whichever nail tier is held.
- Resuming from the menu's button costs the script its protected access, so the `delay` for the
  build animation threw and nothing after it ran. Build and removal finish on a player queue.
- Hotspot slots come from the reference pairs rather than from loc names, which had the formal
  garden's 22-tile fence and hedge filed under the flower beds as two-tile stubs.

**Measured results:**

| Test | Result |
|---|---|
| A. Furniture appears | Pass. Wooden bookcase places loc 6768. |
| B. Multi-tile coverage | Pass. Brown rug lays all 16 tiles: corners on the four corners, middles on the inner 2x2, sides on every edge. |
| C. Removal | Pass. Removing the rug clears all 16 tiles and restores all 16 hotspots. |
| D. XP | Pass. Bookcase 115, rug 30, curtains 132, each x150 realm rate. |
| Room building | Pass. "You build a Parlour." |
| Pools | Pass. Restoration pool 706 xp, special attack 250 to 1000. Ornate pool restored 93/99 hitpoints and cured venom and disease; still clear a full venom cycle later. |
| Fence | Pass. Redwood fence laid 20 tiles across three locs. |
| Portals | Pass. Portal room builds, Teak portal 270 xp, destination menu offers eight, finished portal reads "Varrock Portal" and teleports to 3213,3424, and survives a relog. |
| Portal nexus | Pass. Portal nexus room builds for 200000, Marble portal nexus builds as loc 33408 with Teleport / Teleport Menu / Configuration / Upgrade / Remove, and Teleport lists the eight destinations and lands on Varrock at 3213,3424. |

**Still open, all minor:**

- **11 of 114 multi-tile pieces** put the same loc on every part: the three chapel windows, five
  throne room pieces and three portal nexus rugs. Their hotspot loc ids are absent from that piece's
  `parts` column, so every part falls back to the first loc. The boot log names them.
- **Portal room slots 4, 5, 6** have no loc in their chunk.
- **Formal garden slots 4 and 5** are a second pair of flower beds sharing furniture with slots 6
  and 7, so the pairs cannot tell them apart.
- **17 furniture rows** are leagues and seasonal cosmetics with no loc mapping.

## Results

| Test | Pass / fail | Notes |
|---|---|---|
| A. Furniture appears | | |
| B. Multi-tile coverage | | |
| C. Removal | | |
| D. XP values | | |
| E. Override hotspots | | |
| F. Fused-digit hotspots | | |
