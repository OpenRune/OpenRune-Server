# Araxxor

Origin: CUSTOM on the accepted revision-240 APIs. Branch: `feature/araxxor`.
Status: VERIFIED for covered regressions / USER ACCEPTED on 2026-10-04.
The user accepted the initial private runtime (`b87051471`) on 2026-10-04.
The user also accepted completion build `f7c349c0e` and approved PR #16 for merge.
The initial checkpoint and its rollback remain preserved.
Accepted baseline: `6168204ee` / merged main `8b974210c`; specials remain parked.

## Implemented first chunk

- Separate `content/bosses/araxxor` module using native instance/combat/death APIs.
- Deterministic nine-egg cycle and all three starting colours; hatch after attack 3
  and every six subsequent standard attacks; separate six-attack special clock.
- Destroyed-egg skipping, damage carryover to hatchlings, exhausted hatch handling.
- Quarter-health enrage transition, invalidation of old-phase callbacks, guarded
  corpse/claim/disposal transitions to support one reward per kill.
- Native asset bindings and cache checks for boss/egg/spider HP, size and graphics.
- Capture inspection script reads named JSON entries without extracting/importing
  foreign models, scripts or cache files. Evidence records input hashes.

## Runtime integration - 2026-10-04

- The boss-side web tunnel at (3655, 9814) enters a private one-player arena; the
  inside tunnel exits. Administrator `::araxxor` / `::araxxor leave` is a test route.
  Entry retains the exact return tile and rolls back on failure.
- Native map collision tests check arrival, boss footprint, exit and egg attack
  reach. No replacement map or foreign assets are imported.
- Native melee/ranged/magic attacks, launch-time protection, player mitigation,
  Defence/Prayer drains, egg hatching and all three araxyte handlers are connected.
- Acid hazards, special cycles, enrage, dodgeable cleave, self-damage and Aranea
  boots have runtime handlers. Completion changes and visual limits are below.
- Actor and graphic sequences are separate. Explicit death/harvest animations
  replace fallback handling in the private encounter. The smaller corpse retains
  the boss's centre. Destroy is correctly registered on native menu option 3.
- Harvest invokes native reward hooks once and schedules the 34-tick countdown.
  The hidden original NPC retains contribution context until the reward is claimed.
- Ownership, phase epochs, logout and departure invalidate queued work. Cleanup
  removes the boss, eggs, hatching actors, spiders, acid and countdown.

The pre-existing public Araxxor has basic attacks only. Use the private arena for
the encounter cycle. The installed initial Araxxor test build is the rollback baseline.

## Evidence

The existing revision-240 cache confirms Araxxor 1020 HP/size 7, eggs 65 HP and
hatchlings 58 HP. The cache already provides the NPCs, arena, sequences and effects.
Upstream main was inspected: it has Araxxor drops but no encounter implementation.

[Capture evidence](araxxor-capture-evidence.json) is generated from the supplied
`.npack` and Nero bridge by `tools/araxxor/inspect_capture.py`. It contains 1816
observations. The seven snapshot eggs do not establish all nine spawn positions.
Five initial-state entries are timestamped later in the recording; they must not
be mixed into the tick-zero snapshot. The initial seven-egg snapshot remains
incomplete. Coordinates must be interpreted using native template space and actor
footprints, not one global runtime offset. The recording is from Alora and is
supporting evidence, not an authoritative OSRS ruleset.

Mechanics reference: [OSRS Wiki strategy](https://oldschool.runescape.wiki/w/Araxxor_guide).
The model uses its egg-cycle, hatch-health and enrage rules. The strategy reference guides encounter behaviour; cache symbols establish asset identity,
not animation quality or exhaustive OSRS parity.

## Completion candidate - 2026-10-04

- Mirrorback interception resolves when the incoming hit lands, not when launched.
  Simultaneous hits reserve available spider HP, so damage cannot be redirected
  twice into the same remaining HP. Recoil uses actual applied spider damage;
  native NPC impacts have the required minimum one-tick queue. Close melee recoil
  uses actual damage, and an active Vengeance flag redirects that recoil.
- Content-owned max-hit rules give guaranteed native damage/accuracy rolls for
  crush or heavy ranged when that style has the highest equipped accuracy bonus.
  Noxious halberd bypasses the melee style requirement. Unregistered NPCs and PvP
  retain their previous path. Instance actors inherit the assigned Slayer category
  without mutating cached NPC definitions or decrementing tasks for hatchlings.
- Acid spray uses a forward fan centred on the player's sampled position. Acid
  cannon uses its native rolling NPC and explosion sequence, a 3x3 damage footprint,
  collision-limited ray and wall splashes. Venom trail and acidic-spider death use
  their native graphics. Phase/death/exit cleanup removes rolling actors as well
  as pools and invalidates their delayed callbacks.
- Ruptura distance uses both actor footprints. Egg damage is capped at 64, leaving
  an undamaged egg at 1 HP; a depleted hatch advances after three standard attacks.
  Self-detonation retains its explosion sequence instead of playing another death
  animation over it. The accepted player falloff (80 close, 40 at distance 2, 7 at
  distance 3) is retained; conflicting published distance/max-hit descriptions
  mean these values are not represented as independently verified OSRS parity.
- Normal tunnel access requires current/boosted 92 Slayer and an active araxyte or
  spider assignment, resolved from native task rows. Exhausted tasks prevent the
  next spawn. Administrator `::araxxor` deliberately bypasses those requirements
  for testing, including repeat kills; `::araxxor leave` returns to the entry tile.
- Harvest and Destroy resolve once, share kill credit and respawn timing, and do
  not create fallback bones. Harvest retains the existing table. Destroy rolls
  only Nid (1/1500 instead of 1/3000) and the existing elite clue roll (1/47, with
  the fork's normal clue modifiers). It cannot grant a normal unique/material drop.
- Araxyte morph is conditional on a measured kill under 75 seconds (1..124 ticks),
  no unlocked morph and no item in the player's stored inventories. Untimed public
  kills cannot receive it. The condition vetoes the actual roll, not just preview
  eligibility. Native pet, collection-log, Slayer and clue hooks remain in use.

## Validation and acceptance boundaries

186 selected tests pass (27 encounter, 2 reward, 157 existing/shared regressions).
Automated checks exercise native hit processing, actual-damage callbacks, delayed
Mirrorback appearance/death/departure, simultaneous interception, Slayer task rows,
boosted levels, max-hit isolation, footprint/egg damage, the 75-second boundary,
owned/unlocked morphs and Destroy's exact reward domain. Existing tests cover all
starting colours, egg cycles, cache assets, collision, protection snapshots,
mitigation, corpse claim, 34-tick respawn and cleanup.

The full server JAR build and isolated server/Nero bridge startup and clean shutdown
pass. The build uses a fresh Gradle JVM (`--no-daemon`, 4 GB heap, 2 GB metaspace,
`--no-parallel --max-workers=2`) and in-process Kotlin compilation.

The completion package includes the exact passing XML reports, full-build log,
isolated server/Nero bridge boot result and file hashes. The installer checks the
accepted Araxxor test package before replacement and provides rollback. Player
saves and the accepted Zulrah/special-attack content are not part of this update.

The user accepted the completion build and explicitly approved merging PR #16.
This records user acceptance, not independently measured exhaustive OSRS parity. Two western egg positions retain the
accepted layout; the supplied capture independently establishes only seven.
General Vengeance spell casting and exhaustive Combat Achievements are not added
by this encounter; the combat handler honours an already active Vengeance flag.

No foreign assets or hardcoded raw asset IDs are imported. Specials remain parked.

## Araxyte fang to amulet of rancour

Use an Araxyte fang on an amulet of torture (either item order), with both
unnoted items in inventory and current Crafting level 86 or higher. The native
Crafting combine flow requests confirmation, plays the start/end animations and
graphics, consumes one of each ingredient and creates one amulet of rancour.
It grants 500 base Crafting XP. Cancellation does not consume ingredients;
missing ingredients prevent completion. This uses the existing packed
`crafting_amulet_of_rancour` recipe and `HeldCraftingScript`, with no duplicate
item handler, cache upgrade or change to the accepted installer.

`RancourCraftingTest` loads the actual cache and verifies the recipe requirements,
quantities, XP conversion, confirmation, animation stages and live registration.
The native `HeldUInteractions` dispatcher tries both item orders.

## Fang etching follow-up

The earlier Rancour checks covered only the unetched fang recipe. They did not
cover the cache's Etch option or an etched fang; user testing exposed both missing
handlers. `FangCraftingScript` now routes Etch and chisel use to the same native
Crafting transaction. Araxyte fang requires current Crafting 86; elder venator fang
requires 84. Etching consumes one fang, preserves the chisel and awards no XP.
An etched araxyte fang can use the existing Rancour confirmation/animation recipe
with torture. The original raw-fang recipe and the working rupture assembly are
preserved. The fix requires user acceptance in the client.

Interaction tests dispatch the real item handlers, including both selected-item
orders, insufficient levels, missing chisel, full inventories, confirmation
cancellation, completed Rancour and the existing rupture assembly.
