# Project overview

Reviewed **2026-10-11**. This is the source for current status and priorities.
Implementation details: [custom documentation](docs/custom/README.md).
Detected modules and references: [technical inventory](CONTENT_INVENTORY.md).

**Status:** 🟢 Complete · 🟡 Started · 🔴 Not started

## 🟡 Started: world shortcuts, containers and fairy rings

Owner-requested scope: every revision-240 Agility shortcut, all container items
in the Wiki list, the full fairy-ring network, and the missing surface Spider
Cave entrance/exit for araxytes and Araxxor. Work uses existing native mechanics
and separate reviewable chunks. Upstream #227/#216 and the current Wiki lists
are being compared with the fork; content presence is not completion evidence.
Existing accepted Araxxor combat and travel remain intact. Full route, storage,
persistence, rollback and native interaction validation is still in progress.

- Surface Spider Cave: native Enter/Exit handlers and free landing tiles added;
  real loc interaction/routing and death/logout/movement cancellation pass.
  All **29 Araxxor module tests** pass; client acceptance is still pending.
- Fairy rings: **64 native code rows**, **54 fixed destinations** and native
  loc-to-IF3 travel, favourites/Last-destination and cancellation checks pass
  (**16 tests**). Native centres have walkable exits. The AIR/DLR/DJQ/AJS
  hideout sequence and all six rotation buttons pass; Last-destination uses the
  separate set bit, never the overlapping op-visibility mask. POH DIQ requires
  a built house ring (Construction is not implemented); remaining prerequisite
  and first-visit checks for the Sailing islands, plus client acceptance, remain open.
  Native Fairy Godfather permission and ten destination quest gates are enforced
  under the existing quest policy, including late permission/certificate loss.
  Virtual completion keeps the fork's accepted free-travel behaviour.
- Agility: **518 candidate links** compared with native definitions, map placements
  and the current Wiki/Shortest Path sources. Native option/animation registration
  and walkable landings pass. **Fourteen tests** also exercise bridge repair, rope setup
  and interrupted travel through real loc inputs. Remaining Wiki routes, grapples,
  detailed animations/failures and full route acceptance are still in progress.
  Native Stronghold tunnel pieces work from both shores. Waterbirth and the dense
  essence mine retain their one-way rules; spiritual fairy-tree Ring options
  use their distinct native numbering. Vampyrium includes rubble mining, staged
  pillars and the shared 7.5-minute slide XP cooldown without altering native
  quest progress. Distant-click slide bumps and the bush repeat cooldown remain
  to be verified.
- Containers: all **39 Wiki categories** inventoried against native items.
  Rune/looting pouch baselines adapted. **35 pouch tests** cover codecs, storage
  rules, native rune insertion/withdrawal, interrupted Divine crafting and atomic
  revert with thread return and fourth-slot warning. The real ground-pickup and native altar producers now honour the native
  Rune-picking setting: complete matching stacks only, with ordinary inventory
  fallback and late cancellation. Six new tests cover these producers and pure
  capacity previews. Native bank-side Configure now opens all four pouch forms without an invalid-op exception; bank regression compilation and 35 pouch tests pass. Native death sequence and Destroy confirmation now honour the rune-empty-on-death toggle and clear bag contents even while the death/dialogue lock is active. Wilderness bag destruction immediately exposes non-supplies; duplicates retain shared storage. All ten native loadouts support save/load, quantity edits, names and clearing through the bank Configure menu. Native bank withdrawals preserve tabs/placeholders, partial deposits retain excess runes, and loading validates the carried pouch after interrupted menus. Trouver protection review, remaining looting-bag input validation and final UI
  acceptance remain open. Initial storage operations for **51
  native forms in 16 families**, plus **57 native fruit basket/vegetable sack
  forms**, pass **18 interaction/pickup/death tests**.
  Whitelists reject quest herbs, unsupported kit items, notes and over-capacity
  inserts; save/load and full-inventory withdrawals are covered. Real bank-side input now uses the native bank deposit service for stored contents, enforcing configured capacity, selected tabs, placeholders and partial stack limits. Four new native-input tests pass; all 65 Containers and 35 Pouches tests pass. Family-specific interfaces, remaining crafting and specialised containers are in progress.
  Basket/sack contents stay in their individual native item states. Generic
  storage has pure death previews, deferred PvP/Flamtaer cleanup, last-container
  destroy cleanup and family-specific bank-side filling/emptying.
- Tackle box / huntsman's kit: native main/side screens, Fill/Empty, quantity
  controls, partial transfers and 35/32-type limits pass **eight tests**. Existing
  tackle contents and charged-tool vars are preserved. Native account loading and
  confirmed last-kit destruction are covered; all **70 container module tests**
  pass. Visual UI acceptance and final bank/death integration remain open.
- Bow string spool: six native capacity tiers (250 to 100,000), notes, account
  storage and all twelve ordinary bow-stringing routes pass **six tests**,
  including 27 bows per inventory and actual spinning output. Interrupted actions
  preserve charges and XP; visual native input acceptance remains pending.
- Gnomish firelighter: per-item storage for all five colours, 1,000-charge limit,
  native Check/Uncharge and actual Firemaking queues pass **five tests**. A fire
  consumes one charge and grants the normal-log XP plus 10; late item loss,
  movement, death/logout and full-inventory refunds preserve charges and XP.
  Visual and final death-path acceptance remain open.
- Master scroll book: all **20 native scroll types** store up to 1,000 each, use
  the native UI and support default selection/withdrawal. **Five tests** cover
  native item/IF3 producers, every teleport, Revenant entrance/warning selection,
  full inventory and deferred arrival/late cancellation. The existing standalone
  scroll suite also passes after sharing its travel service. Client labels and
  visual acceptance remain open.
- Steel key ring: all thirteen reusable keys, the native Check-keys/IF1
  interface, removal, drop warning and banked-duplicate protection pass **seven
  tests**. Keys restore from permanent account attributes and stay kept on PvM/
  PvP death. Door-specific consumer validation and client acceptance remain open.
- Container crafting: native gem pouch/satchel/tote/sack recipes, direct tote
  creation, all five Hunter pouch recipes and regular/costume needles pass **ten input tests**, including all
  open source forms, correct XP, full inventories and overflowing shared storage.
  Silklined herb-sack creation preserves herbs and the open state; late movement,
  death, logout and stale silk cancel without consumption. Existing Crafting
  item-use registrations remain compatible through one shared recipe provider. Native clothing and pouch choices are both reachable for shared furs. Hunter upgrades preserve stored fur/meat, use three ticks and the native five-crafts-per-thread charge; fur Empty is bank-only. All 61 Containers and 16 Crafting tests pass; the additional fur Empty interaction test also passes. Coffins now have native Dampe Talk-to/Locks/item-use, five lock tiers, held/worn controls, highest-first Empty and transactional Remove lock; five new input tests and all 70 Containers tests pass. Further specialised container creation remains in progress.
- Fish crates: all six deep-sea species, cooled-crate creation and camphor
  crafting pass **seven native input tests**. Real bank IF3 Empty returns the
  camphor crate; ordinary Empty discards it. Partial contents survive native
  account loading, and full-inventory/lifecycle failures preserve materials.
  All **70 Containers module tests** pass. Sailing net/cargo producers are not
  available in this fork; visual acceptance remains open.
- Fish barrels: native Fill/Open/Close, worn sack controls, combination/dismantling,
  direct fishing storage and bank-side Empty pass **seven native input tests**.
  Configured bank capacity is enforced through the native deposit service; the seven Fishing tests and four container-bank regressions pass. Full-bank failures retain contents; PvP previews protect the barrel without
  changing storage. Deposit-box/UIM handling and final client checks remain open.
- Forestry kit/basket: native main/side interface, all twenty display rows,
  inventory/equipment submenus, separate shared kit/log storage, dismantling and
  direct Woodcutting collection pass **15 native input tests**. Sturdy-harness
  and clothes-pouch crafting enforce both skills; permanent clothes/cape
  attachments preserve their storage. Stored/worn Lumberjack pieces grant the
  correct combined 2.5% without stacking; rations consume only after a successful
  cut. Full inventory, bank capacity, stale inputs, movement, death/logout and
  saved attributes are covered. All **85 Containers**, **16 Crafting**, **seven
  Fishing**, **15 max-cape** and **12 network** tests pass after item-specific
  equipment-submenu dispatch. Native visual acceptance and the remaining
  Forestry consumable/bird-nest effects are open; Forestry events are not added.
- Shared storage safety: dry runs no longer mutate virtual contents; physical
  inventory and storage commit together. Bank inventories cannot consume held
  coal/essence. Pickup rechecks capacity and permissions before ground deletion,
  supports alternate looting storage and cancels on death/logout. Atomic API
  tests pass; Smithing/Runecrafting compile (their test tasks have no sources).

## 🟢 Complete: Edgeville home teleporter and Emblem trader

Owner-selected home: **Edgeville**. Public `::home` arrives near the bank;
Talk-to the Teleport wizard or use `::teleports` to open the native catalogue.
Nine categories, all-category search, paging, destination details and a separate
Teleport button show **145 destinations: 84 enabled routes and 61 future/review
entries**. Unfinished encounters stay visible as Coming soon without teleport
output. Boss routes preserve the existing entrance/instance mechanics.

Standard travel respects teleport blocks, Wilderness limits and Revenant delay;
Wilderness destinations require confirmation. Native outgoing effects are
cleared on arrival/cancellation. All enabled landing tiles use native collision
checks; existing Varrock content remains intact.

Edgeville's Emblem trader now redeems all six Revenant artefacts and their notes
through the existing atomic exchange, including snapshot revalidation, cancel,
death/logout/movement and coin-overflow handling. Existing skull logic remains.

Validated: **110 selected tests**, including **15 new teleporter/trader tests**;
formatter, runtime JAR, preservation of existing cache archives/file IDs and
**220,164 mappings**, all **19,438 varbit bases**, native new/existing-account
initialisation and isolated rev240/Nero boot. The final runtime verifies **85
walkable tiles** and all **45 menu components**. Guarded package:
`outputs/home-teleporter-20261011/INSTALLEREN.cmd` with checkpoint and rollback.
Owner accepted the result on **2026-10-11**. [PR #37](https://github.com/EvolvedMind/OpenRune-Server/pull/37)
is merged as `98822c1ef`; its tree matches tested head `40224c542`. [Implementation, sources and manual tests](docs/custom/home-teleporter.md).

## 🟢 Complete: Revenant Caves

Owner accepted the implementation on **2026-10-11**: three entrances, two exits, five
boostable Agility shortcuts, 100,000-coin admission/death risk, singles-plus,
30 ordinary spawns across all eleven species, healing/prayer adaptation,
ethereum/weapon charges, ancient artefacts and Maledictus with its specials,
MVP/participant rewards and Surge. Native loot/KC/Collection Log routes and
administrator test commands are connected. Existing rev240 content is retained.

**46 new feature tests; 206 selected tests in total**, scoped format checks,
the runtime JAR and bounded cache/mapping/login checks pass. All 117,585 LIVE
archives, unrelated SERVER files and 220,158 existing mappings are preserved;
six new server vars are added. The accepted test result is retained; no new multiplayer test is claimed. [Sources, configured differences and tests](docs/custom/revenant-caves.md).

[PR #36](https://github.com/EvolvedMind/OpenRune-Server/pull/36) merged as `7f0fdc01f`;
the tested content is unchanged in the final main tree.
Guarded test package: `outputs/revenants-entrance-fix-20261011/INSTALLEREN.cmd`.

Entrance fix: remove the silent account-membership gate on this members world.
Regression tests cover the default account flag and real map routing/object
clicks for all three entrances. The 100,000-coin fee and safe cancellation remain.


## 🟢 Complete: teleport animation correction and all scroll routes

[PR #35](https://github.com/EvolvedMind/OpenRune-Server/pull/35) is merged
(`4b7a1ad84`); its tree matches tested head `a113bdc44`.
Arceuus/Lunar outgoing animations and graphics now clear at arrival or late
cancellation. All **20 real rev240 teleport scrolls** have native inventory
handlers, exact single-scroll consumption and safe cancellation. Guthix keeps
its accepted TD-cave destination and uses the shared scroll animation cleanup;
the duplicate Temple handler is removed. Revenant uses its real third option
and all three entrances with a Wilderness confirmation.

**69 selected tests**, scoped formatter checks and the runtime JAR build pass.
Isolated rev240/Nero boot and all **22 arrival tiles** pass.
The guarded test package is `outputs/teleport-fixes-20261010/INSTALLEREN.cmd`.
The owner accepted the test result on **2026-10-10** and authorized closeout.
[Routes, upstream decision and validation](docs/custom/teleport-scrolls.md).

## 🟢 Complete: Lizardman Shamans — accepted; PR #34 merged

[PR #34](https://github.com/EvolvedMind/OpenRune-Server/pull/34) merged on
2026-10-10 as `d5f5cc3d0f619ff278b88859a29e89419f92956a`; its complete tree
matches tested head `02361e283f8f7f83e131e890cc77e0ba2f1668d7`. GitHub reports
no commit statuses or workflow runs, so no online CI pass is claimed.
The owner accepted the installed test package on **2026-10-10**. All **29 installed
target hashes and 985 prerequisite hashes** match the guarded package; its cache
and runtime match the isolated validation. The accepted installation is retained.

The five ordinary canyon/cave/Temple variants now have a Boss DSL controller:
melee, normal ranged spit, dodgeable green acid spit, jump/landing and three
finite purple spawns. Native attack/block/death assets remain intact. Tier-5
Shayzien pieces reduce acid damage by 20% each; the full worn set blocks acid
damage while poison and antipoison keep their native rules. Pending specials
and minions are cancelled on death, logout, departure, respawn and unload.

Dragon warhammer is in the ordinary loot table at **1/3000**. `::test shamans`
arrives at the owner's **1451, 3696, 0**; `::testloot shamans [1–1000]` uses the
native death/loot hooks and increments the shared permanent Shaman killcount.
Successful warhammer drops also reach the Collection Log and loot tracker.
Chambers of Xeric variants retain their separate existing content.

**137 selected Kotlin tests and 14 reference-metadata fixtures pass**, including
native approach → combat → hit-consumer routes, prayer/armour/dodge, minion
cleanup, teleport, killcount and forced successful warhammer reward delivery.
The candidate preserves every LIVE archive and all **220,157 existing gameval
mappings**, changing only five server NPCs and adding one server-only varp.
Both account types and all **19,438 varbit bases** pass the real login check.
Isolated rev240/Nero boot, native Shaman bindings, exact teleport tile, bridge,
pet tabs/gorilla access and clean shutdown pass. Test package:
`outputs/shamans-20261010/INSTALLEREN.cmd`. The owner confirmed the gameplay
result and authorized closeout. Complete covers the agreed ordinary Shaman
extension; raids, diary unlocks and exact OSRS probability reconstruction are
outside this scope. See [implementation and sources](docs/custom/lizardman-shamans.md).

## 🟢 Complete: companions, categorized `::pet` gallery and Items fix — PR #32 merged

The owner's screenshot roster is available in the existing administrator gallery:
Archibald (seven paint patterns), Broav, Cats, Dogs, Hellcats, Humphrey Dumphrey,
Mayor of Catherby, Pet fish (three bowls), Pet rock, Spooky chair and Toy cat.
**194 native item choices** are separated into **All, Boss, Skilling, Minigames,
Cats, Dogs and Other** tabs in the existing native interface. Search stays within
the selected tab, switching tabs resets the page, and two-line pet names retain
their space. New companions have
native item actions and ownership-checked actor pickup where applicable;
Cats/Dogs retain their existing care/follower scripts. Paint and fish-food
conversion use inventory transactions. The temporary chair safely waits for
inventory room and retains its saved follower when a full inventory prevents
logout return. No originating quest/event or POH menagerie is implemented by
this bounded extension. See [pet actions and source decision](docs/custom/pets.md).

The reported screenshot is the Monster Guide (`::drops`). Its **Items** button
failed while indexing an old, unavailable clue-item symbol. The preview now
ignores unavailable item references in its item index and retains an unavailable
drop row without sending an invalid item icon. Actual loot tables are preserved.
The full production catalogue loads **818 monsters and 1,425 items** successfully;
native `::spawn` remains available. See [interface fix](docs/custom/interfaces.md).

**108 selected tests pass**: 30 Pets, 13 Monster Info, 8 Collection Log, 43 Doom
and 14 gorilla. Full formatter, gameval check, runtime JAR and isolated
rev240/Nero startup pass. Only the existing pet interface, its initialization
script and pet-interface metadata are patched into the installed companion
cache. Checks preserve every other archive and all existing mapping IDs.
Runtime checks confirm all 194 choices, seven native tab buttons, native actor
definitions and unchanged Doom/gorilla access.
Guarded test package: `outputs/pets-tabs-items-fix-20261009/INSTALLEREN.cmd`, with
an independent rollback to the verified installed companion package. The owner
accepted the package and authorized merge on 2026-10-09. All **23 live target
hashes** and **958 prerequisite hashes** match the approved installer; its
candidate cache matches the isolated runtime. [PR #32](https://github.com/EvolvedMind/OpenRune-Server/pull/32)
merged as `c6afd938f`; its complete tree matches reviewed head `26cbf8b7c` exactly,
whose runtime code matches tested commit `96634b327`. GitHub reports no commit
statuses or PR workflow runs; the repository's previously disabled Actions
remain a separate limitation, with no online CI pass claimed. The merge changes
no live files; the installed test package is retained without rebuilding it.
Treasure Trails remains parked with its remaining-work review recorded below.

## 🟢 Complete: Mokhaiotl waystone and Confliction maxmage — PR #31 merged

Native **Channel** now teleports to Doom's pre-lair and consumes one waystone
only on arrival. Normal teleport restrictions and native instance lifecycle
remain in place. `::maxmage` supplies Confliction gauntlets; other gear and Shadow
charges are preserved. **101 selected tests**, full formatter, gameval checks and
runtime JAR pass. Isolated rev240/Nero startup confirms the native Channel option,
wearable gauntlets, walkable Doom lobby and preserved gorilla objects. Server and
private database stop cleanly. [PR #31](https://github.com/EvolvedMind/OpenRune-Server/pull/31)
merged as `59a71addd`; its tree matches tested head `b152effda` exactly.
Guarded installer: `outputs/doom-waystone-maxmage-20261009/INSTALLEREN.cmd`,
with rollback to the verified installed PR #30 baseline. Cache, client plugins,
world edits, RSA and playerdata are preserved. Its changes are included in the
installed companion package, whose live hashes match the accepted installer;
visual confirmation of the standard teleport effect is a manual game check.
See [Doom mechanics](docs/custom/doom-of-mokhaiotl.md).

## 🟢 Complete: Doom delve loot-rate correction — PR #30 merged

The owner authorized update, implementation and merge of
[PR #30](https://github.com/EvolvedMind/OpenRune-Server/pull/30) on 2026-10-09.
Merged as `4d9d974d1`; its tree exactly matches tested head `a3bfe1c74`.
Mokhaiotl cloth, Eye of Ayak, Avernic treads, Dom and elite clues now use
delve-specific odds, capped at delve 9. The main uniques share an exclusive roll;
Dom and clues are independent. Existing reward/claim/logging paths are preserved.
All **234 selected project tests** pass, including five rare-rate tests, the
production DoomRewards pipeline, Collection Log/command coverage and 132 clue
regressions. Scoped formatting, JAR and isolated revision-240/Nero boot pass
against the exact installed cache/mappings/overlay. The gorilla hole/sign/rope
and absent old arch are preserved. Full local `spotlessCheck` and gameval conflict
checks pass. GitHub Actions is disabled for this repository (workflow dispatch
returned HTTP 422); no online CI is claimed. Installer:
`outputs/doom-delve-rates-20261009/INSTALLEREN.cmd`, with rollback to the matched
installed gorilla package. On 2026-10-09, all nine live target hashes match this
package's after-install manifest, confirming the installed PR #30 baseline.
See [Doom mechanics](docs/custom/doom-of-mokhaiotl.md).

## 🟢 Complete: gorilla hole/rope and underlying map arch repair — installed

The owner reported the old cave entrance and inactive hole/rope after PR #29.
The replacement installer preserves the current Nero edits and replaces the
four task-specific preview objects with the three functional access objects.
An isolated map scan additionally found the native Ruptured cavern
(`loc.mm2_cave_boss_waterfall_small`, 28719) at `(2106,5652,0)` underneath the old
custom entrance. The gorilla module now removes only this native object at that
tile, creates the outside hole/sign and inside rope, and restores its own map
change on unload without overwriting later editor changes. **124 selected tests**,
scoped formatting and the full JAR pass. Isolated startup confirms exactly the
three requested objects, both walkable arrival tiles and absence of the native
arch after applying the persisted overlay. The owner installed
`outputs/gorilla-rope-complete-20261008/INSTALLEREN.cmd`. All 21 live target hashes
match its after-install manifest on 2026-10-09. Its source commit `0d2079013` is
retained in PR #30 so the next Doom runtime cannot regress this installed repair.

## 🟢 Complete: prayers, spellbook travel and gorilla access — approved scope

The owner authorized merge after the screenshot placement correction on
2026-10-08. [PR #29](https://github.com/EvolvedMind/OpenRune-Server/pull/29) merged
as `b9baac5a8`; its tree matches tested head `66ca7608c`. Prayer, teleport and
gorilla changes remain in separate commits. Revision 240, accepted bosses,
specials, gear and the 60/60 clue-task slice are preserved. Merge performs no
automatic installation.

- Chivalry/Piety are quest-free with their normal Prayer/Defence levels. The five
  prayer scrolls permanently unlock Rigour, Augury, Preserve, Deadeye and Mystic
  Vigour after confirmed Read; upgraded native quick-prayer slots are handled.
- Fixed travel, homes, group and Teleother routes on all four spellbooks are
  unlocked and rune-free, with no travel XP. Existing teleport safety and
  recipient consent remain. Native spell metadata still supplies the accepted
  Barrows tablet's crafting level/runes/essence/XP.
- Screenshot correction: outside Hole at `(2428,3522,0)` and Danger sign at
  `(2429,3521,0)`; inside Climbing rope at `(2108,5651,0)`. Climb-down lands at
  `(2108,5654,0)`; Climb-up returns outside to `(2428,3521,0)`.
  Tortured gorillas share Demonic attack animations and switch after four
  consecutive zero-damage impacts; Demonic gorillas keep three.
- `::testprayers` supplies five scrolls; `::testgorillas` reaches the screenshot
  outside entrance. Future CoX/Royal Titans loot and dynamic POH/boat/Bounty Hunter/minigame
  destinations remain separate systems.
- **574 selected tests pass**, including 132 clue regressions; scoped formatter,
  revision-240 cache and full JAR pass. Isolated revision-240/Nero startup and
  real map-collision checks pass; private PostgreSQL stops and process exits 0.
  Client rendering is not certified by server tests; manual checks are documented
  for the owner when installing the requested placement correction.
- First test package `prayer-teleports-gorillas-20261008` remains unchanged.
  The entrance correction passes **121 selected tests**, formatter/cache/JAR and
  isolated startup with actual runtime placements and walkable landing tiles.
  New package: `outputs/gorilla-entrance-fix-20261008/INSTALLEREN.cmd`, with an
  independent rollback to the verified installed first package, including the
  four task-specific Nero preview replacements. Other world edits are preserved;
  no playerdata or RSA replacement or automatic installation.
- CI on the exact reviewed head `66ca7608c`: [build/tests/boot](https://github.com/EvolvedMind/OpenRune-Server/actions/runs/37844865414),
  [Formatting](https://github.com/EvolvedMind/OpenRune-Server/actions/runs/37844865443)
  and [Gameval Conflict Check](https://github.com/EvolvedMind/OpenRune-Server/actions/runs/37844865555)
  all passed. Original installer source `f94663e17` and corrected installer source
  `66ca7608c` remain immutable checkpoints; this merge record changes docs only.
- [Policy, sources, native interactions and manual tests](docs/custom/prayers-teleports-gorillas.md).

## 🟢 Complete: Tormented Demons — accepted encounter

The user tested the encounter and approved merge after the two menu/arrival
corrections on 2026-10-08. [PR #28](https://github.com/EvolvedMind/OpenRune-Server/pull/28)
merged into `main` as `8eac919a8`. Revision 240 and previously accepted
content are preserved.

- Impact-based XP, protection/prayer switches, defenceless appearance, slow-hit
  punish, bombs, single/dual/triple roles and lifecycle cleanup are implemented.
- Temple access/exit, 26 spawns, chamber limits/indicator, ordered rewards, ashes,
  private smouldering effects/items and full synapse/claw/Arclight crafting.
- Native Wield equips all affected weapons; Arclight Check uses `Op3` and synapse
  Revert uses `Op4`. Scroll and default `::testtd` arrive at `(4061,4464,0)`.
- The full delivery passed 412 selected regressions. The acceptance correction
  passes **51 TD tests**, scoped formatting, JAR and isolated revision-240/Nero
  startup. GitHub CI, Formatting and Gameval checks pass on the merged PR head.
- New installer: `outputs/tormented-demons-options-fix-20261008/INSTALLEREN.cmd`.
  Its independent rollback returns to the verified installed full TD package;
  previous installers/checkpoints remain preserved. No automatic live installation.
- Exact parity of unpublished lower-level consumable formulas is a reference
  verification limit, documented in the feature note. While Guthix Sleeps and
  global Combat Achievements remain separate systems.
- [Mechanics, sources, commands and evidence](docs/custom/tormented-demons.md).

## 🟡 Parked: Treasure Trails — remaining-work review recorded

The owner requested the remaining-work review on 2026-10-09, then parked further
implementation until later. The accepted 60/60 task slice is preserved; Mimic
remains last. The current production-catalog review confirms
37 normally selectable map clues, of which eight have native map interfaces and
29 fall back to empty clue text. Complete those maps first, then world routes,
puzzle variants and reported guardian animations; expand torn-master parts,
wire permanent tier completion counters and the native Bloodhound obtain route,
and validate complete six-tier trails before adding master-casket Mimic.
The review changes no gameplay. The 132 clue tests remain recorded regression
evidence, not a fresh test run or proof of all 997 catalog records being playable.
See [review and ordered chunks](docs/custom/treasure-trails.md#remaining-work-review--2026-10-09).

- **60/60 skill-task rows have completion handling.** The user approved the current scope on 2026-10-07; [PR #27](https://github.com/EvolvedMind/OpenRune-Server/pull/27) merged into `main` as `c535c713a`. This is task coverage, not overall clue completion.
- Finish the remaining clue routes, maps and puzzles; then validate complete trails and their lifecycle.
- The guardian progression fix is accepted and merged. User-approved [PR #26](https://github.com/EvolvedMind/OpenRune-Server/pull/26) merged into `main` on 2026-10-07 (`e6aa9e06a`): Uri recovery, native elf pickpocketing, corrected altar consumers and watermelon planting/Sherlock completion. **Mimic comes last and is not implemented.**
- Previous approved checkpoint: 180 selected tests, scoped Kotlin checks, full server JAR and isolated revision-240/Nero startup pass. Package `clue-farming-20261007` and its independent rollback remain unchanged; merge does not install it.
- CI for the approved PR #26 merge: [build/tests/boot](https://github.com/EvolvedMind/OpenRune-Server/actions/runs/37623645527), Formatting and Gameval checks all passed. The boot-port correction changes CI only. Documentation generator tests: 16/16 pass; generated output check passes.
- New eight-task slice: dragonstone enchantment, castle chest, Dorgesh lamp, Shayzien platebody, Barrows tablet, spiritual mage, Fiyr cremation and catacomb kill. Validation: 201 selected tests (107 clues), scoped formatting, full JAR and isolated revision-240/Nero startup pass; exit 0, PostgreSQL PID absent, no cleanup errors. Fresh package: `outputs/clue-tasks-50-20261007/INSTALLEREN.cmd`, with its own rollback. It is not installed automatically.
- Final ten added: aerial eel, Skullball, Ape Atoll/Rellekka laps, black warlock, red chinchompa, own spirit tree, reanimated abyssal, torn parts and Tecu salamander. Native producer/consumer and regression tests pass (226 total, 132 clues). Full JAR/cache, ten scoped formatting checks and isolated revision-240/Nero startup pass; exit 0, private PostgreSQL PID absent, no cleanup errors. The approved delivery `outputs/clue-tasks-60-20261007/INSTALLEREN.cmd` remains unchanged, with independent rollback to the verified installed 50-task baseline. Merge performs no installation. [Sources and activity limits](docs/custom/clue-final-native-tasks.md).
- CI for PR #27's exact reviewed head `84616d805`: [build/tests/boot](https://github.com/EvolvedMind/OpenRune-Server/actions/runs/37669314684), [Formatting](https://github.com/EvolvedMind/OpenRune-Server/actions/runs/37669314268) and [Gameval Conflict Check](https://github.com/EvolvedMind/OpenRune-Server/actions/runs/37669314243) all passed. The merged tree matches that tested head.
- [Implementation](docs/custom/treasure-trails.md) · [coverage and gaps](docs/custom/clue-task-coverage.md) · [test commands](docs/custom/clue-testing.md) · [native action evidence](docs/custom/clue-completion.md) · [Farming scope](docs/custom/clue-farming.md) · [eight new tasks and activity limits](docs/custom/clue-native-tasks.md).

## Accepted baseline

**Protected runtime recovery point:** revision **240**, Doom/HUD: server `f40f4d951`, Nero `b29d93c`.
Exact commits, package and recovery checkpoints: [baseline](docs/custom/baseline.md).

Acceptance below applies to the recorded scope. A merged change does not establish that it was installed in the live runtime.

| Status | Area | Accepted scope / reference |
|:---:|---|---|
| 🟢 | [Zulrah](docs/custom/zulrah.md) | Existing custom encounter; owner-confirmed acceptance. |
| 🟢 | [Araxxor](docs/custom/araxxor.md) | Encounter accepted; merged in PR #16. |
| 🟢 | [Kraken](docs/custom/kraken.md) | Public-cave encounter accepted; private instances are outside this scope. |
| 🟢 | [Corporeal Beast](docs/custom/corporeal-beast.md) | Encounter accepted; clan instances and individual Combat Achievement conditions remain separate work. |
| 🟢 | Barrows | Accepted native encounter/rewards; `::testloot barrows [count]` uses native chest rewards. |
| 🟢 | [Doom of Mokhaiotl and NPC combat-stat HUD](docs/custom/doom-of-mokhaiotl.md) | Accepted package; merged through server PR #23 and Nero PR #11. |
| 🟢 | [Small extensions](docs/custom/small-extensions.md) | OSRS GE Prices, Improved Item Examine, Notification Queue and Collection Log Reward Broadcasts approved and merged into `474296f2b`. Advisory formatting findings remain open. |
| 🟢 | [Clue skill-task handling](docs/custom/clue-task-coverage.md) | Current 60/60 scope approved and merged in PR #27; supporting activity limits and remaining full trails are tracked separately above. |
| 🟢 | [Prayer unlocks, free spellbook travel and gorilla access](docs/custom/prayers-teleports-gorillas.md) | Owner-authorized scope merged in PR #29, including the requested outside hole/sign and inside rope correction; future encounter loot/dynamic destinations remain separate. |

## Existing content and open follow-ups

| Status | Area | Remaining work / reference |
|:---:|---|---|
| 🟢 | [Pets](docs/custom/pets.md), [commands](docs/custom/commands.md), [max cape](docs/max-cape.md) and [timers](docs/custom/architecture.md) | Implemented; preserve covered behavior when shared systems change. |
| 🟡 | [Interfaces](docs/custom/interfaces.md) | Implemented; remaining fixed/resizable scrolling and visual edge cases need in-game verification. |
| 🟡 | [Boss-item crafting](docs/custom/boss-item-crafting.md) | Native atomic recipes implemented; user authorized merge. New recipes still need in-game acceptance. |
| 🟡 | [Slayer menus and helmet crafting](docs/custom/slayer.md) | Source audit: existing task/unlock/extension handlers and ordinary helmet recipes. Fix Trade routing, task state, purchases, block slots, toggles and Suqah mapping; complete the requested menu and conversion paths. |
| 🟡 | [NPC combat animation import](docs/custom/combat.md#ordinary-npc-combat-animations) | Work round closed on 2026-10-10 at the owner's request; implementation remains Started. 97.15% technical attack/block/death coverage across 3,993 Attack-option types; 3,844 types have all three roles accounted for. Pack: 3,749 definitions / 10,671 missing parameters. Runtime: 56 spawn forms, 11 combat transitions, 29 multipart deaths (24 Maiden corpse forms) and 40 non-retaliating props. Verified: 370 Kotlin tests, 8 importer checks, 225 lifecycle bindings, build, cache preservation and isolated boot. Tested code: `925f17d4e` on local `feature/npc-combat-animations`. Owner installed lifecycle package; login regression reproduced from a 32,768 shift in varp reference IDs. Recovery: `outputs/npc-animations-login-fix-20261010/INSTALLEREN.cmd`, one servercache file with rollback. All NPC data/JAR retained; original file IDs, 19,438 varbit bases, real new/existing-account flag writes, eight metadata fixtures and isolated boot pass. Owner accepted the installed recovery on 2026-10-10; [PR #33](https://github.com/EvolvedMind/OpenRune-Server/pull/33) merged as `64ab394119d10df87b3a87e4458ef4462cb3ae78` from reviewed head `29286cc0a`; the bounded animation import is accepted, while the listed remaining scope stays Started. Remaining: 341 action slots across 149 types, secondary attack styles, specials/phase integration and native-client visual acceptance. Accepted controllers and unrelated files are preserved. |
| 🔴 | [Persistence warning follow-up](docs/custom/upstream-review-20261006.md) | Recorded concurrent database-close/save-drain warning; investigate separately from accepted encounter scope. |
| 🟢 | Upstream research (review only) | [Test harness PR #282](docs/custom/upstream-pr-282-test-harness.md) and [level-up PR #286](docs/custom/upstream-level-up-779b81b.md) reviewed; not imported. Implementation has not started. Any adoption requires a scoped revision-240 port. |

## Backlog

Planning only; list order does not authorize starting work. Check existing code and upstream reuse before estimating remaining effort.

<!-- roadmap:start -->

Requirements and details: [roadmap.json](tools/progress/roadmap.json).

Scope: XS = tiny, S = small, M = medium, L = large, XL = very large, XXL = multiple systems. These describe the full target; inspect existing work and upstream reuse to estimate the remaining work.

### Bosses

| Status | Feature | Scope |
|:---:|---|---|
| 🔴 | Thermonuclear Smoke Devil | S |
| 🔴 | Deranged Archaeologist | S |
| 🔴 | Giant Mole | S–M |
| 🔴 | Chaos Fanatic | S–M |
| 🔴 | Obor | M |
| 🔴 | Brutus | M |
| 🔴 | Chaos Elemental | M |
| 🔴 | Kalphite Queen | M |
| 🔴 | Dagannoth Kings | M |
| 🔴 | Sarachnis | M–L |
| 🔴 | Shellbane Gryphon | M–L |
| 🔴 | Royal Titans | L |
| 🔴 | Vet'ion & Calvar'ion | L |
| 🔴 | Cerberus | L |
| 🔴 | Vorkath | L |
| 🔴 | Abyssal Sire | L |
| 🔴 | Alchemical Hydra | L |
| 🔴 | Grotesque Guardians | L–XL |
| 🔴 | The Hueycoatl | L–XL |
| 🔴 | Nex | XL |
| 🔴 | The Nightmare | XL |
| 🔴 | Yama | XL–XXL |

### Minigames and activities

| Status | Feature | Scope |
|:---:|---|---|
| 🔴 | Aerial Fishing | M |
| 🔴 | Tithe Farm | M–L |
| 🔴 | Vale Totems | M–L |
| 🔴 | Puro-Puro | M–L |
| 🔴 | Mahogany Homes | L |
| 🔴 | Mastering Mixology | L |
| 🔴 | Giants' Foundry | L |
| 🔴 | Fight Caves | L |
| 🔴 | Zalcano | L |
| 🔴 | Wintertodt | L–XL |
| 🔴 | Mage Training Arena | L–XL |
| 🔴 | Tempoross | XL |
| 🔴 | Pest Control | XL |
| 🔴 | Moons of Peril | XL |
| 🔴 | Guardians of the Rift | XL |
| 🔴 | The Inferno | XL–XXL |
| 🔴 | Fortis Colosseum | XL–XXL |
| 🔴 | The Gauntlet | XL–XXL |

### Other systems

| Status | Feature | Scope |
|:---:|---|---|
| 🔴 | Warriors' Guild Cyclopes & Defenders | M |
| 🔴 | Hunter Guild | L |
| 🔴 | Random Events | XL |
| 🔴 | Forestry | XL |
| 🔴 | Own Grand Exchange | XL–XXL |
| 🔴 | Achievements | XL–XXL |
| 🔴 | Combat Tasks | XXL |
| 🔴 | Leagues | XXL |
| 🔴 | Sailing Skill | XXL |
| 🔴 | Completionist Cape + Particles | TBD |
| 🔴 | Hooded Slayer Helmets + Variants | TBD |

### Parked

| Status | Feature | Note |
|:---:|---|---|
| 🟡 | Treasure Trails — remaining completion work | Owner parked further implementation on 2026-10-09. Preserve the accepted 60/60 Charlie/Sherlock task checkpoint; full Treasure Trails and Mimic remain unfinished. |
| 🟡 | Weapon Special Attacks | Accepted checkpoint: 191/285 item registrations; 94 deferred. Registration is not full mechanics validation. Resume only when selected. |
| 🔴 | RSPS AgentCraft — In-Game Developer Observatory | Parked concept: real Claude/Codex agents through an in-game developer scene, with an external runner and isolated worktrees. Full-system scope XXL. |

### Runtime migration

| Status | Feature | Note |
|:---:|---|---|
| 🟡 | Revision 241 Upgrade | Deferred compatibility review. Keep revision 240; any client, protocol or cache upgrade needs a separate explicit task. |

<!-- roadmap:end -->
