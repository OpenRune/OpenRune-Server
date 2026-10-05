# Construction

How the player-owned house is assembled, where its data comes from, and what is still stubbed.

Everything lives in [`content/skills/construction`](../content/skills/construction). Rooms,
hotspots, furniture, costs and level requirements are read out of the cache at startup; the two
things the vanilla tables do not carry - which loc a build places, and what it pays in xp - are
packed into a cache table of our own by
[`construction/pack`](../content/skills/construction/pack).

## Where the data comes from

| Source | Gives us |
|---|---|
| `dbtable.poh_room` (111) | room name, coin cost, level requirement, `source_offset`, `door_locations`, ordered hotspot list, `room_obj`, add-room `button` component |
| `dbtable.poh_hotspot` (112) | each hotspot's `builddata` — the furniture that can go in it |
| `dbtable.furniture` | `model_obj` (build-menu icon), display name, `material_cost`, level requirement, `hidden_in_build_menu`, upgrade links |
| `dbtable.construction_furniture_build` (ours) | per `model_obj`: the locs a build places, the loc each hotspot part gets, and the xp it awards |
| `dbtable.construction_dispenser` (ours) | furniture that hands out items: what searching a larder, kitchen shelf or tool rack offers, and what a barrel or sink turns a used item into |
| the static map | the actual hotspot and doorway positions inside each room's 8x8 source chunk |

The generated row classes (`PohRoomRow`, `PohHotspotRow`, `FurnitureRow` in `api/generated`) are the
only accessors needed. `ConstructionCatalogue` wraps them and does the one thing the tables cannot:
work out **where** each hotspot physically sits.

### Hotspot and furniture discovery

`source_offset` is a **tile offset**, not a zone key. Every value is a multiple of 8 (0, 8, 16 … 56
across; 0 … 64 up), forming an 8x9 grid of one-zone room chunks based at tile **(1856, 7040)** in
unreachable map space. `ConstructionCatalogue.TEMPLATE_BASE` holds that base; if it ever stops
matching, `locateTemplates()` sweeps the map and reports where the chunks actually are.

Two things the tables do not carry are resolved by name instead:

**Where a hotspot sits.** Room chunks use two conventions, both handled in `HotspotNaming`:

- *Numbered* (the classic rooms): `loc.poh_<room>_<n>`, where `n` is the 1-based slot in the room's
  `hotspot` column. A hotspot spanning several tiles repeats the number with a part suffix
  (`poh_parlour_4_middle`, `_side`, `_corner`), and those group into one `HotspotDef` with several
  `HotspotPart`s.
- *Named* (costume room, menagerie, league hall, superior garden, achievement gallery): named after
  what they build instead, as in `poh_cos_room_cape_rack_hotspot`. These are matched to a slot by
  comparing name fragments against the furniture each slot can build, longest fragment first, and a
  fragment that fits more than one slot is rejected rather than guessed. Any trailing digit here is
  a part number, not a slot: `poh_leaguehall_pedestal_hotspot_1` is one of three pedestals.

**What built furniture looks like.** `dbtable.furniture` has no loc column, and the obj and loc
names are different vocabularies rather than spelling variants, so only about 200 of 500 rows can be
matched by name at all. `dbtable.construction_furniture_build` names the locs outright, keyed by
`model_obj`, and pairs each hotspot part with the loc that goes on it so a rug lays corners on
corners. Name matching - `obj.poh_armchair_1` pairs with `loc.poh_armchair_1` - stays as the
fallback for furniture added to the cache since that table was written.

Doorway hotspots are the `loc.poh_hotspot_door*` family (`_doorl_`/`_doorr_` per house style, plus
the dungeon variants); their position on a zone edge gives the wall direction.

All of this is pure string handling, so `HotspotNamingTest` checks it directly against real cache
names — no cache load or house entry needed to catch a regression.

### What discovery finds

Boot logs a summary, and `::pohrooms [filter]` dumps it per room. The last measured run, before the
named convention was handled, found:

| | Found |
|---|---|
| Rooms | 30 / 30 |
| Doorways | 172 |
| Hotspots | 141 |
| Furniture locs resolved | 156 / 404 |

Known remaining gaps:

- **A scattering of single slots in classic rooms.** Chapel finds 1, 2, 3, 5, 6, 7 but not 4; portal
  room finds only 1, 2, 3, 7; combat room only 4, 5, 6. Those locs use names neither pattern matches.
- **Roughly 60% of furniture has no loc by name**, which is what
  `dbtable.construction_furniture_build` exists to answer; the figures above were measured before it
  was added.

## How a house is built

A house is a grid of rooms: 8x8 per level, 4 levels, with level 1 the ground floor and level 0 the
dungeon. `HouseLayout` holds it as two flat maps — slot to `PlacedRoom(room, rotation)`, and
(slot, hotspot index) to furniture row — and encodes to a single string.

`HouseRegions.allocate` turns that into a dynamic region: one `RegionTemplate` zone copy per placed
room, at the room's grid slot, rotated by the placement's rotation. Everything else in the region
stays empty. Rotation is applied to hotspot coordinates with `RegionRotations.translateCoords`, so a
rotated room's furniture lands where the rotated chunk actually put the hotspot.

Built furniture is spawned as a normal loc at the hotspot's primary part. Hotspot locs carry no
varbit transforms at all (`multiVarBit` and `transforms` are empty on every one of them), so the
built loc has to be resolved by name rather than morphed. Spawning per region means two parlours can
hold different chairs, which per-player varbits could not express.

## Persistence

One persisted attribute, `poh_layout`, holding `HouseLayout.encode()`:

```
1|<slot>:<roomRow>:<rotation>,...|<slot>:<hotspotIndex>:<furnitureRow>,...
```

Version-prefixed, so an older or malformed value decodes to an empty house rather than throwing.
Attributes are stored as JSON in `character_attrs`, so no schema work was needed.
`HouseLayoutTest` covers the round trip, malformed input, and the rule that furniture pointing at a
room that is no longer placed gets dropped.

Entering a house also sets `instance_exit_coord`, the attribute the login path already honours, so a
player who logs out inside their house comes back outside it rather than in dead region space.

## Entry points

- Any of the nine town portals (`loc.poh_rimmington_portal` and friends): op-1 enters, op-2 enters in
  building mode.
- `loc.poh_exit_portal` in the garden leaves.
- `::house` and `::buildmode` do the same thing without walking to a portal.
- `::pohrooms` dumps the catalogue.

In building mode, clicking a furniture hotspot opens `interface.poh_furniture_creation` (458). The
client builds each row itself: the server opens the interface, calls
`[clientscript,poh_furniture_creation_entry]` once per slot (1-31, mapping to components 458:4-458:34
through `enum_1461`) with the slot index, furniture dbrow, level requirement, materials text and
whether the player can afford it, then suspends on `pauseButton()`. Clicking **Build** runs
`[clientscript,poh_furniture_creation_op]`, which resumes that pause button with the slot index.

Op-5 on a built hotspot removes the furniture after a confirmation.

Op-5 on a doorway builds a room through it, or, when a room is already on the other side, offers to
remove that room along with everything built in it. A room cannot be removed while the player
stands in it, while a room sits directly above it, or when it is the last room on the ground floor.
Like a new room, the change shows on re-entry.

Built furniture that does something beyond standing there:

- Portals and the portal nexus teleport (`PohPortals`, `PohNexus`), and the superior garden pools
  restore (`PohPools`).
- Larders, kitchen shelves and workshop tool racks: op-1 Search offers their items one at a time
  through a menu. Barrels and sinks take a used item: a beer glass becomes that barrel's drink, and a
  bucket, jug, bowl, vial or kettle is filled with water (`PohDispensers`, over
  `dbtable.construction_dispenser`). Barrel drinks are the house-only `poh_*` objs.

## Known gaps

- **Hotspot coverage is incomplete** — see above. Both naming conventions are handled, but some
  individual slots still match neither.
- **Room creation uses a chat menu, not `interface.poh_add_room` (212).** That window's room grid is
  driven by `varcs.poh_roompos_01`-`_38`, which the server does not set; the room buttons in
  `dbcol.poh_room:button` all point into it. Until that contract is worked out, clicking a doorway
  hotspot in building mode offers the buildable rooms as a dialogue menu instead.
- **New rooms appear on re-entry.** Placing a room writes the layout but does not patch the live
  region; leaving and re-entering rebuilds it. `RegionRegistry.registerZone` can add a single zone to
  a live region, which is the upgrade path.
- **Construction xp is derived from materials.** `dbtable.furniture` carries no xp column, so
  `MATERIAL_XP` maps the standard per-plank values (29 / 60 / 90 / 140) plus gold leaf, marble,
  magic stones, cloth and limestone. Furniture built from anything else awards no xp yet.
- Not implemented at all: the estate agent and buying/moving a house, house styles, servants,
  the house options window, dungeons as a separate build flow, the menagerie, and guests.
