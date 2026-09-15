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

---

## Known gaps found by running this checklist

Driven in a client on 2026-09-13. Four dispatch bugs were found and three are fixed. The sections
above were written before any of them were known.

**Fixed and confirmed in a client:**

- Every house option is at **op5**, not op1. Hotspots are `ops=[4=Build]`, built pieces are
  `ops=[0=Sit-on, 4=Remove]`. Building, room building and removal were all bound to the wrong op,
  so nothing in a house responded to a click at all.
- The build menu's entries are `hide=yes` in the cache, so the server has to show the ones it fills.
- Those entries carry no position and nothing arranges them, so the server places them in a grid.

**Still broken: you cannot actually build a piece of furniture.** The menu opens and lists the right
furniture, but clicking an entry does nothing. The entry's Build op runs
`[clientscript,poh_furniture_creation_op]`, which calls `cc_resume_pausebutton`;
`InterfaceEvents.isEnabled` (api/net) short-circuits when `comsub == -1` and consults the cache's
static component flags instead of what `ifSetEvents` registered, and interface 458's entries declare
no ops of their own. Enabling the whole child range on each entry did not help, which points at
`comsub` arriving as -1. The next step is one diagnostic boot that logs the `component` and `sub` of
the incoming ResumePauseButton, which decides whether the fix belongs in the content module or in
`InterfaceEvents`.

Until that is fixed, sections A to D cannot be completed: they all need a piece of furniture to
exist. Sections E and F can be checked as far as "the build menu opens and lists the right furniture
for that hotspot", which is itself worth confirming across rooms.

## Results

| Test | Pass / fail | Notes |
|---|---|---|
| A. Furniture appears | | |
| B. Multi-tile coverage | | |
| C. Removal | | |
| D. XP values | | |
| E. Override hotspots | | |
| F. Fused-digit hotspots | | |
