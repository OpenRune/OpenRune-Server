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


## Bow string spool

[Bow string spool](https://oldschool.runescape.wiki/w/Bow_string_spool) storage
uses native charges and capacity fields in permanent varp 4761. Charges occupy
bits 0..16, the server capacity tier uses bits 28..31; six tiers cap at 100,000.
Native CS2 5923 reads the charges field for the buff display. No state is cleared
when a spool is dropped or lost. Combining deletes only the spare spool and
preserves all charges. Filling accepts ordinary and noted bow strings; withdrawal
returns only the amount the physical inventory can hold.

Six tests exercise Held/HeldU inputs, every ordinary shortbow/longbow, 27-bow
inventories, dry-run material transactions, interrupted consumption and native
var restoration. Actual Crafting `craftOnce` spinning output invokes the storage
consumer once; a full spool leaves the produced string in the inventory. Incoming
pickups and bank withdrawals are not redirected by this consumer. Stringing XP
uses the detailed [Fletching table](https://oldschool.runescape.wiki/w/Fletching),
native sequences and the existing XP modifier service. Client acceptance remains
a separate check.


## Additional tunnel and ring options

The 508 candidate route links pass native placement and landing checks. Ten
shortcut tests include both Stronghold cave tunnel pieces from either shore and
the one-way Waterbirth crevice. The dense essence mine west shortcut cannot be
climbed in reverse, following its detailed Wiki page. Iorwerth gaps use 1 and
1.5 XP, Barrows wall uses 25 and Waterbirth uses 10. Karuulm uses five ticks.
Brimhaven's vine returns to its actual plane-two platform.

Native Spiritual Fairy Trees (regular and League variant) have Ring-Zanaris,
Ring-configure and Ring-last-destination on options 2, 3 and 4. Thirteen fairy
ring tests pass, including those native loc inputs; their Tree/Remove options
retain their respective systems' ownership. Quest keys for the hideout and
Guthix scroll now match the quest registry convention. Nineteen standalone
spell-teleport tests pass. House DIQ still requires a Construction-owned built
ring; no false house coordinate is substituted.

### Gnomish firelighter native consumer

The existing revision-240 normal-log Firemaking queue now accepts the charged
gnomish firelighter directly. Count (maximum 1,000) and one of the five native
colours are packed into this individual item's inventory vars; separate items
retain separate contents. Charging and Uncharge use inventory transactions. The
queue revalidates the exact item, ground log, coordinates and lifecycle before
consuming one charge, creating the matching native coloured fire and awarding
40 + 10 XP. Ordinary tinderbox and coloured-log behaviour remains covered.
Five real Held/HeldU/queue tests pass, including all colours, refund rollback,
item loss and lifecycle interruption. Source: [Gnomish firelighter](https://oldschool.runescape.wiki/w/Gnomish_firelighter).

### Native equipment kit interfaces and persistent storage

The old Fishing tackle-box handler has been consolidated into the Containers
module to prevent duplicate Held handlers and separate delete/add operations
that could lose contents on full-inventory withdrawal. The existing inventory
651 remains the source of truth, preserving prior contents. Native inventory
855 is used for the huntsman's kit. Both receive server-only pack overrides:
Always stack, Perm scope, unprotected storage, original native sizes 35 and 32.
Their native CS2 scripts (4049/4061 and 4988/5000), quantity varbit 11795 and
actual main/side IF3 item events are connected. The native account inventory
loader restores counts and item vars. Different charge states for an already
stored type are rejected rather than merged; deposits and withdrawals retain
vars. Contents cannot substitute for carried Fishing/Hunter tools. PvP
previews preserve item vars, and real final-item Destroy clears storage only
after confirmation (remaining kits retain shared contents).

Eight native input tests and all 37 Containers module tests pass. Scoped cache
validation compares all 338,195 accepted SERVER configuration files: only native
inventories 651 and 855 differ. The accepted config reference table must be
preserved when packing: CacheLibrary updates otherwise alter the native signed
-1 varp sentinel and break unrelated varbit bases. That test-stage failure was
repaired from the accepted package and the complete regression suite rerun.
No live cache or player data was changed. Final packaging must use this same
reference preservation, with the two inventory overrides explicitly allowed.
Sources: [Tackle box](https://oldschool.runescape.wiki/w/Tackle_box),
[Huntsman's kit](https://oldschool.runescape.wiki/w/Huntsman%27s_kit).

### Native fish crates

All six revision-240 deep-sea fish types use their native full and partial crate
forms. Full crates contain ten fish; partial counts belong to that item's vars
and survive the normal account inventory loader. The actual bankside IF3 Op9
producer invokes Empty and returns a camphor crate after the final fish.
Ordinary Empty discards the crate. Partial withdrawals, the last crate refund
and fish output are one transaction, including full-inventory rollback. The
Wiki-described marlin partial-crate refund bug is not reproduced: the same
bank rule applies consistently to full and partial forms.

Camphor logs require boosted Crafting 62, a chisel, a carried/worn valid hammer
and four adamantite nails; successful two-tick output grants 50 XP. An ice
cooler converts the camphor crate into an empty cooled crate. HeldU packing
uses ten unnoted fish and revalidates the exact inputs after its one-tick delay.
Seven native interaction tests and all 44 Containers tests pass. The Sailing
net/cargo systems are not present in the fork, so their producer integration
remains unavailable rather than being represented by a fake trawling event.
Sources: [Fish crate (giant krill)](https://oldschool.runescape.wiki/w/Fish_crate_(giant_krill)),
[Fish crate (marlin)](https://oldschool.runescape.wiki/w/Fish_crate_(marlin)),
[Camphor crate](https://oldschool.runescape.wiki/w/Camphor_crate).

### Vampyrium shortcut links and native quest options

Ten native links bring the candidate catalogue to 518. Native map collision
checks pass for every final landing. Two-direction rocks/rockslide requirements
are 78 descending and 61 climbing; their successful traversals share a
persistent UTC timestamp so ordinary relogging does not reset the 7.5-minute
550-XP bonus. Climbing awards no XP. The two bushes require Agility 64.
Pillars require Agility 73 and permanent native rubble mining; approach events
allow safe jumps to the blocked pillar tiles, with three staged jumps in each
direction and 1.9 XP per pillar. Mining uses the existing pickaxe animations
and revalidates the native quest policy, gear and lifecycle before unlock.

The two native quest-hidden variants receive only their Climb/Jump-to options
in a scoped pack. Server requirement checks still respect real native quest
progress when requested; virtual completion never sets `varbit.myq6` to 200.
The isolated SERVER stage changes only object definitions 60923 and 61118 in
config group 55, preserving the other 62,523 objects and reference metadata.
The final pack must also update their LIVE/native client options. Fourteen
shortcut tests pass, including real native op/approach producers, quest denial,
rubble gates, both directions and lifecycle cancellation. Distant-click slide
bumps and the precise bush repeat cooldown remain verification points; no
claim of full gameplay acceptance is made for those details. Sources:
[Rocks (Vampyrium)](https://oldschool.runescape.wiki/w/Rocks_(Vampyrium)),
[Rockslide (Vampyrium)](https://oldschool.runescape.wiki/w/Rockslide_(Vampyrium)),
[Bush (Vampyrium)](https://oldschool.runescape.wiki/w/Bush_(Vampyrium)),
[Pillar (Vampyrium)](https://oldschool.runescape.wiki/w/Pillar_(Vampyrium)).

### Steel key ring native interface and storage

All thirteen Wiki-listed reusable keys are accepted exactly once through real
HeldU inputs. The account's permanent 13-bit storage is exposed through the
existing virtual storage API only while a ring is carried. Banked rings retain
contents but cannot satisfy carried-key checks. Explicit material transactions
can still consume a stored key when the related miniquest calls for removal.
Check-keys opens native interface 127, whose actual legacy IF1 Next/Previous/
Remove inputs update models, text and selection. Removal is transactional and
full-inventory failures retain the key. Real Drop calls the standard ground-item
producer after a warning and stale/lifecycle rechecks. Losing the last ring
clears keys unless a duplicate remains carried or banked. A pure native death
hook always retains rings and does not change stored keys during previews.
Seven input tests and all 51 Containers module tests pass. Door-specific key consumers and visual interface
acceptance remain open. Source: [Steel key ring](https://oldschool.runescape.wiki/w/Steel_key_ring).

### Fairy-ring permission and destination requirements

Native `varbit.fairyring_permission` permits partial Fairytale II access; no
guessed quest-stage threshold is used. The existing assume-completed/virtual/
respect-progress policy controls completed quest requirements. Ten destination
gates cover Children of the Sun, Death to the Dorgeshuun, Holy Grail, Regicide,
Land of the Goblins, Fremennik Trials, Monkey Madness I and In Search of the
Myreque. Holy Grail and Land of the Goblins also accept real in-progress quests.
Varlamore first-visit uses its native bit outside assume-completed mode. Native
quest progress remains untouched. Permission, destination requirements and
hideout certificate are checked again after the outgoing animation. Sixteen
fairy-ring tests pass, including native loc/IF3 denial, selective virtual
unlocks, first-visit and late permission loss. Great Conch/Grimstone first-visit
producer integration is still open with Sailing. Source:
[Fairy ring requirements and codes](https://oldschool.runescape.wiki/w/Fairy_ring).

### Rune-pouch automatic pickup and Runecrafting

Native Settings row 306 (`varbit.take_runes_toggle`, 5698) explicitly describes
pickup and Runecrafting only. A source on incoming inventory transactions lets
these actual producers offer complete matching stacks to the pouch; ordinary
awards, withdrawals and bank deposits keep their existing rules. Missing stacks
and insufficient pouch capacity take the ordinary inventory path. Storage
previews are pure and stale toggle/capacity changes reject commits. No additional
virtual rune counts are exposed to combat, avoiding duplicate accounting with
the existing MagicRuneManager. Native altar actions also cancel on movement,
death and logout before consuming essence or awarding XP. Six producer tests
and all 21 pouch, 51 container and six atomic transaction tests pass. Rune
loadouts and full death/UI acceptance remain open. Source:
[Rune pouch interface](https://oldschool.runescape.wiki/w/Rune_pouch#Interface).

### Bank-side pouch Configure

The accepted LIVE and SERVER interface-15 bytes agree and contain components
0–96. They predate the current gameval dictionary's new rune-pouch loadout
buttons (128–135). Existing bank controls are preserved. Native bank-side item
Configure now dispatches a protected item-specific event and opens the existing
rune pouch interface for each of its four forms. Late logout refuses the action.
The native IF3 producer test passes; all 22 pouch tests pass and Bank compiles
(its own test task has no sources). Ten-loadout controls still require a
compatible interface implementation, rather than assigning nonexistent widgets.

### Pouch death and actual destruction

The real PlayerDeath coroutine, native drop selection/application and native
HeldDestroy confirmation now pass six additional tests (28 pouch tests total).
The native `emptyondeath_runepouch` toggle adds all stored runes to the death
outputs and clears them only at completion. Ordinary PvM death otherwise retains
their native vars for reclamation. PvP always spills runes. Looting bag contents
include food/potions on PvM death, excluding them from the PvP killer's output.

Actual Destroy revealed that `invClear` could refuse the protected looting
inventory while its own confirmation dialogue still held the player's lock.
The confirmed item event and death-completion callback now own their cleanup,
without calling the user-action permission guard again. Wilderness destruction
spills non-supplies publicly with no reveal delay; outside it contents disappear.
A surviving carried/banked duplicate retains the account's shared bag storage.

The current Wiki Trouver rules include changes newer than this cache's item
variants. Protection/repair behaviour is still a separate compatibility review,
not assumed complete from generic PvP rune-spill tests. Primary sources:
[Rune pouch](https://oldschool.runescape.wiki/w/Rune_pouch),
[Looting bag](https://oldschool.runescape.wiki/w/Looting_bag), and
[Trouver parchment](https://oldschool.runescape.wiki/w/Trouver_parchment).

### Container crafting: gems and silklined herbs

The five gem recipes (pouch, satchel, upgraded/direct tote and sack) use the
existing Crafting item-use registry. Regular needles consume the recipe's whole
thread reels; costume needles consume no thread. Open gem inputs are accepted.
All linked physical ingredients and output use one inventory transaction.
The sack combines the low-tier and ordinary gem-bag storage into its own state,
refusing an overflowing combination without consuming materials. Other upgrades
retain shared family storage. Silklined herb creation has a one-tick ownership
check and preserves the original open/closed form.

Six native item-use tests pass alongside the existing Crafting registrations;
all 57 Containers tests pass. They cover every recipe/form/tool, XP, valid native
unstackable material slots, full inventories, missing requirements, overflow and
interrupted silk upgrades. Source pages: [Gem pouch](https://oldschool.runescape.wiki/w/Gem_pouch),
[Gem satchel](https://oldschool.runescape.wiki/w/Gem_satchel),
[Gem tote](https://oldschool.runescape.wiki/w/Gem_tote),
[Gem sack](https://oldschool.runescape.wiki/w/Gem_sack), and
[Silklined herb sack](https://oldschool.runescape.wiki/w/Silklined_herb_sack).


### Hunter pouch creation and native Crafting choices

Small/medium/large fur pouches and small/large meat pouches have their native
level, materials, XP and three-tick leather animation. Upgrades retain shared
storage; regular needles use the native `crafting_thread_uses` counter (one reel
per five crafts), while costume needles consume no thread. Fur pouches refuse
Empty outside the bank. Late tool/material loss, movement, death and logout
cancel without output, XP or thread charges.

Initial independent handlers collided with the native mixedhide clothing
recipes. A set-injected `CraftingRecipeProvider` now extends the existing held
Crafting registry. When shared materials offer different sections, the native
choice menu exposes clothing and containers separately; cancel consumes nothing.
Ten container crafting input tests, all 61 container module tests and all 16
existing Crafting tests pass. The added bank-only fur Empty test also passes
after that full-suite run. Primary sources: [Small fur pouch](https://oldschool.runescape.wiki/w/Small_fur_pouch),
[Medium fur pouch](https://oldschool.runescape.wiki/w/Medium_fur_pouch),
[Large fur pouch](https://oldschool.runescape.wiki/w/Large_fur_pouch), and
[Large meat pouch](https://oldschool.runescape.wiki/w/Large_meat_pouch).


### Native bank capacity and container Empty

Generic bank-side storage previously inserted items into the bank backing
inventory directly. This bypassed the player's configured capacity, selected
tab and placeholder handling. A protected bank-side Operate event now reaches
the container consumer before wearable fallback. Stored stacks move through
`BankInvScript.invDeposit`; the account storage decreases only by the committed
amount. Native equipment-kit inventories retain their object vars. Stale IF3
item data and pending logout/death are rejected. Four actual IF3 input tests
cover filling, banking, configured capacity despite free backing slots, partial
maximum stacks, placeholders, selected tab sizes and stale/lifecycle inputs.
All 65 Containers and 28 Pouches tests pass. Fish barrel now uses the same native deposit helper through a protected
bank-side consumer. Its seven real-input Fishing tests and the four generic
container-bank regressions pass. The full-bank fixture now tests configured
capacity rather than filling the entire oversized native backing inventory.


### Shade coffins and worn container controls

The five lock tiers attach to a broken coffin through native Dampe NPC inputs.
The resolved cache places Locks at op three despite the compact Wiki options
list; registrations use that native option. Dampe refuses a duplicate coffin
including banked and worn ownership. Players cannot attach locks themselves.
Held and worn Fill/Configure/Open/Close/Empty keep their native option numbers.
Empty withdraws highest-tier remains first. Confirmed Remove lock atomically
returns the broken coffin and lock, then clears remains; full inventory, stale
object replacement, death, logout and cancel preserve the original state.
Generic container worn operations now bind from native wear-op parameters and
preserve the default Remove handler. Changing the worn form rebuilds appearance.
Five producer/consumer tests pass, and all 70 Containers tests pass. Sources:
[Dampe](https://oldschool.runescape.wiki/w/Dampe),
[Dampe transcript](https://oldschool.runescape.wiki/w/Transcript:Dampe),
[Broken coffin](https://oldschool.runescape.wiki/w/Broken_coffin),
[Bronze coffin](https://oldschool.runescape.wiki/w/Bronze_coffin),
[Gold coffin](https://oldschool.runescape.wiki/w/Gold_coffin), and
[Gold locks](https://oldschool.runescape.wiki/w/Gold_locks).
No direct pyre consumption is added: the documented mechanic requires removing
remains before constructing a pyre. The unstarted Shades activity is preserved.


### Rune bank loadouts and native transfers

The bank Configure consumer offers Open pouch or ten A–J loadouts through the
native dialogue menu. This keeps the accepted revision-240 bank cache: its
loadout components are absent, although newer gameval dictionaries name them.
Saving, loading, quantity edits, rename and clear use permanent native loadout
fields. Split fields are read/written using their inspected widths, preserving
other bits in the same varps; names use permanent account attributes.

Native bank withdrawal/deposit services maintain configured capacity, tab sizes
and placeholders. Excess runes stay in the pouch when the bank is full; a second
return pass handles bank slots freed by withdrawals. Regular pouches refuse four
rune types. Dialogue ownership, movement, death and logout are checked again
before mutation. The native menu closes the bank modal, so completion restores
the bank only for a still-valid session. All 35 Pouches, 70 Containers and seven
Fishing tests pass, including actual quantity/name inputs, all ten loadouts,
save/restore, full banks, missing supply and partial maximum stacks. Final client
acceptance remains open.
