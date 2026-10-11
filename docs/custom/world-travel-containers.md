# Native world travel and container decisions

Current status belongs to [PROGRESS.md](../../PROGRESS.md).

## Accepted base

Revenant Caves PR36 merged as `7f0fdc01f8429822778927af944f1dc53b6c64fb`.
Edgeville home PR37 was retargeted to main and merged as
`98822c1ef901783bd9aa6d4062eb2cc1fa81bbde`. The resulting gameplay tree
`483b592943b29e5272bb4c9021cc94ca61006c8e` equals the tested home candidate
`40224c542acb44b79bf49281fb25ad9fe50115af`. New work retains revision 240.

## Reviewed upstream

- [Agility PR227](https://github.com/OpenRune/OpenRune-Server/pull/227), open
  draft at `98fe5d9a0554b202ccd63492c326dbe65793243a`: selective shortcut
  data reuse; rooftop courses are outside this request. Runtime data initially
  yields 353 candidate links and 160 loc/option bindings. Native comparison
  exposes wrong symbols, non-clickable intermediate props, mismatched options
  and blocked landings. The upstream list alone is not sufficient coverage.
- [Content PR216](https://github.com/OpenRune/OpenRune-Server/pull/216), closed
  unmerged at `375581c32314973d7de6a04e5e7f3713ef429f22`: selected fairy-ring
  and rune/looting-pouch modules adapted to existing fork APIs. The wider branch
  is not imported. Death uses the fork's pure preview DropHook with deferred
  completion; the old pre-drop mutation hook is not introduced. Ground pickup
  receives a small optional storage hook after ownership validation and repeats
  validation before deleting the obj after a pickup delay.
- [Varrock PR243](https://github.com/OpenRune/OpenRune-Server/pull/243), merged
  head `aa35c4e6c1cfd4ba66d7ecdb7482d4263e320a98`, and [Draynor PR245](https://github.com/OpenRune/OpenRune-Server/pull/245),
  merged head `20efad8edde22d9c750d67bc5146fb7240149900`: existing handlers
  retain ownership; no duplicate registration.
- Container searches also located [Crafting PR146](https://github.com/OpenRune/OpenRune-Server/pull/146):
  it explicitly defers pouch and silk sack storage/upgrade interactions.
  [Consumables PR140](https://github.com/OpenRune/OpenRune-Server/pull/140)
  provides the potion effect system already present in this fork. Raid PR271
  targets revision 241; it is outside this revision-240 scope.

## Primary data and verification

The [Shortcuts list](https://oldschool.runescape.wiki/w/Shortcuts) supplies
162 rows, including multiple levels sharing one object/page. The
[container list](https://oldschool.runescape.wiki/w/Container_items#List_of_containers)
contains 39 categories. The [fairy-ring table](https://oldschool.runescape.wiki/w/Fairy_ring)
is compared with the native `dbtable.fairyring` (all 64 dial combinations).
Wiki raw/API requests are batched after encountering rate limiting. Local
source manifests record URLs and SHA256 hashes.

Native audit reads the accepted isolated home cache: 34,606 item definitions,
62,525 loc definitions and 8,258 relevant placed objects. One pre-existing
malformed map archive group 25287 is recorded rather than silently counted as
valid; affected routes must be checked separately if the requested content uses it.

Spider Cave evidence and coordinates are documented in [Araxxor](araxxor.md).
The Araxxor suite passes 29 tests. Fairy rings pass 12 tests covering all 64
code combinations, 54 fixed destinations through native loc and IF3 inputs,
favourites/Last-destination and cancellation. Native centres deliberately carry
loc/decor collision; some also have a blocked-floor flag. Their authoritative
row landing is retained and a free adjacent exit is required instead of treating
the ring itself as an ordinary walkable destination. The sound alias is included
in the isolated test package; it must accompany the eventual bounded deployment.
No live installation has been performed.

## Storage transaction safety

The previous virtual-storage adapter mutated bags during dry-run capacity checks.
It could retain a partial storage insert even if the physical inventory portion
failed. Transfers now capture a pure capacity/count snapshot, validate it before
physical commit and apply both portions once. Noted and variable-bearing products
retain normal physical handling; zero/negative counts cannot mutate storage.
Bank inventories are excluded from held coal/essence hooks.

Ground pickup uses the same pure preflight, ownership checks and delayed recheck.
The ground object is deleted only after the destination transaction can succeed.
Alternate inventory storage is optional and receives its persistence callback
only after a successful pickup. Generic container tests exercise the actual
Held/HeldU/Obj interaction producers, storage persistence, full inventories and
late denial/space/death/logout cases. Smithing and Runecrafting compile; their
`test` tasks currently contain no test sources.

## Initial generic container scope

`containers.tsv` declares 51 native forms in 16 storage families. Native inventory
options select their actual handler positions; Wear is left to normal equipment
handling. Item insertion rejects notes, unsupported types and capacity overflow.
Stored quantities persist as account attributes with notification on every
change. Integer arithmetic is widened for seed stacks. These basic operations
and twelve native interaction/pickup tests are a first chunk, not evidence that
all 39 requested categories or their specialised interfaces are complete.


Fairy-ring special travel covers the native AIR → DLR → DJQ → AJS hideout
sequence (ring centre 2328,4426), repeated travel-log sequence, staff-free island
return and all six native rotation inputs. `fairyring_lastloc_opvis` spans bits
0–6 of the same varp as the destination and is derived visibility, not a boolean
to set; only `fairyring_lastloc_set` is written alongside the destination. DIQ
requires an actual built house ring. Construction/house instances do not exist
in the fork yet, so an unavailable house route is explained rather than sent to
a made-up location.


The first container chunk also handles the 57 native fruit-basket and vegetable-
sack states as individual items, without account-shared contents. Actual Held
and HeldU input tests cover separate baskets, incompatible mixing, full inventory,
partial withdrawal, sack priorities and Remove-one. Generic death previews are
pure; PvP herbs spill even if their sack is protected, protected tackle contents
persist, and Flamtaer bags/contents are destroyed on death. Confirmed destruction
clears the last shared container's state, except the Huntsman's kit's persistent
account storage. Bank-side Use fills tackle/Huntsman kits; plank sacks fill if
planks are carried and otherwise empty to the bank. Eighteen tests pass in this
initial chunk. Specialised native UIs/consumers and the remaining categories
are still in progress.


## Native shortcut foundation

[Shortest Path](https://github.com/Skretzo/shortest-path/tree/7e81a40d0312e767fd7833637e57caae80a80253)
provides additional route pairs at pinned commit
`7e81a40d0312e767fd7833637e57caae80a80253`; its BSD 2-Clause license and
attribution accompany the module. The 434 candidate links are not equivalent
to completion of every Wiki row. Native multi-loc children supply actual
click options; effective map levels account for bridge-floor flags. Existing
working area handlers retain ownership. The old Port Sarim Wyvern placeholder
is replaced by native west/east shortcut routes.

Six tests validate registration/animations, collision at every landing and actual
loc inputs for rope setup, bridge repair and interrupted crossings. Mausoleum
bridge repair uses the detailed Wiki page's six teak planks and 28 nails of any
tier per side, native persistent bridge bits and 550 Construction XP only after
an atomic physical/virtual inventory commit. Both sides must be repaired to
cross. Karamja's three stones have independent rolls and XP per successful stone.
The current Wiki Giants' Plateau IDs refer to Catherby in revision 240; they
cannot safely transfer the Giants' level or XP to the Catherby shortcut.

## Fish barrel integration

The original `fish_barrel` save key remains intact. Native inventory options
are read from the item definition, preserving Wear on option two for sack
barrels. Fishing awards enter opened barrels through the pure storage hook;
stored fish are never exposed as cooking/eating inputs. Combination and
splitting are atomic and retain shared contents and open/closed state.

Seven tests use Held/HeldU/Worn producers, the fishing award producer and native
IF3 bank option nine. Bank-side explicit container actions now take precedence
when their Type event is handled, matching the label produced by the bank UI.
A full bank never removes unstored fish. Death previews protect fish barrels in
PvP without mutating persistent contents. Deposit-box/UIM interaction requires
further work; these tests do not establish visual client acceptance.


## Rune pouch and scroll book native routes

Fifteen pouch tests include actual Held/HeldU/IF3 inputs for rune insertion,
withdrawal, Divine creation and Revert. Three-tick crafting checks a needle,
boosted Crafting 75 and both original item references again before an atomic
transaction. Revert returns the thread and pouch together; the warning explains
that slot four is lost, and a failed output does not clear any rune vars. Creation
awards no Crafting XP. Native bankside presets and auto-pickup/Runecrafting still
require integration; their absence is not concealed by the storage unit tests.

The native scroll-book interface 597 handles the twenty accepted scrolls through
its own Activate/Set as default/Remove controls. Five input tests use the actual
IF3 handler and Held/HeldU producers; the standalone scroll tests pass after
extracting a shared travel service. Both payment sources recheck ownership,
permission and count at deferred arrival without creating a temporary scroll.

Native CS2 procedure 2832 reads Watson's count as `high * 256 + low`. Procedure
5157 reads the default destination as `high * 15 + low`; it is not a bitshift.
Those formulas were decoded from the isolated accepted revision-240 scripts and
are asserted through native vars by the input tests. Counts/defaults use the
permanent native varbits, preserving existing save state. Visual confirmation of
the native Revenant dynamic labels remains an explicit client check.


## Additional native shortcut setup

The expanded 487 links pass native option registration, placement matching and
walkable landing checks. Nine tests include actual Loc/LocU/Ap producers for the
Viyeldi long rope, Mining 78 laboratory wall and River Lum grapple; interrupted
setup does not consume materials or grant unlocks. The native persistent varbits
control the Viyeldi, laboratory and Wyrmscraig transforms. Long cave connections
teleport after their action rather than drawing an exact-move line through walls.

Primary pages for the additional Varlamore, Meiyerditch and Sailing routes were
retrieved through the Wiki API on 11 October 2026. Native Nemus stepping stones
differ from the current Wiki ID. The Fremennik spike floor awards no XP, the
Wintertodt gaps award 18 and polar hunting rocks award 1. These values follow
the detailed primary pages rather than guessed defaults. Visual timings and
remaining Wiki coverage still require validation.
