# Araxxor

Origin: CUSTOM on the accepted revision-240 APIs. Branch: `feature/araxxor`.
Status: IN PROGRESS; private encounter runtime added, acceptance pending.
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
  boots now have runtime handlers. Some patterns remain provisional; see below.
- Actor and graphic sequences are separate. Explicit death/harvest animations
  replace fallback handling in the private encounter. The smaller corpse retains
  the boss's centre. Destroy is correctly registered on native menu option 3.
- Harvest invokes native reward hooks once and schedules the 34-tick countdown.
  The hidden original NPC retains contribution context until the reward is claimed.
- Ownership, phase epochs, logout and departure invalidate queued work. Cleanup
  removes the boss, eggs, hatching actors, spiders, acid and countdown.

The pre-existing public Araxxor has basic attacks only. Use the private arena for
the encounter cycle. The installed accepted server has not been replaced.

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
The model uses its egg-cycle, hatch-health and enrage rules. The remaining runtime
behaviour still needs implementation and verification against independent evidence.

## Open acceptance work

1. Confirm the two inferred western egg positions. The native map NPC archive
   confirms the boss origin, but contains no eggs; the capture confirms seven.
2. Validate actor skeletons, projectile heights/timing, hatching, death and harvest
   in the client. The stretched blue effect reported by the user is not yet
   reproduced or visually verified as fixed. Symbol checks are not visual proof.
3. Replace provisional square acid spray and acid-ball rendering with verified
   patterns; qualify Ruptura distance falloff and Mirrorback reflection timing
   (currently at attack launch). Minion guaranteed-max-hit rules remain open.
4. Implement Destroy-for-pet rewards and Slayer entry rules. Qualify native drops:
   the upstream pet-morph condition is still an unconditional placeholder and must
   be corrected before reward acceptance. Destroy currently explains its status.
5. Produce a test installer after remaining mechanics/reward checks; obtain live
   visual acceptance before merge. This is not a completed-boss or parity claim.

Use native symbol mappings. Do not adopt raw IDs or foreign assets from the capture.
Do not change accepted Zulrah or resume deferred special attacks as a side effect.

Validation: 18 passing tests cover cycle/assets, native collision, prayer snapshots,
mitigation, cleave/ray geometry, namespace registration and lifecycle. Full server
JAR and isolated server/Nero bridge startup and clean shutdown pass. No live-client
visual acceptance or installer validation is claimed. The initial boot caught an
NPC/content queue-namespace error; it is fixed and covered by a registration test.
