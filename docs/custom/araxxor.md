# Araxxor

Origin: CUSTOM on the accepted revision-240 APIs. Branch: `feature/araxxor`.
Status: IN PROGRESS; encounter foundation only, not yet playable.
Accepted baseline: `6168204ee` / merged main `8b974210c`; specials remain parked.

## Implemented first chunk

- Separate `content/bosses/araxxor` module; no active NPC handlers or entry commands yet.
- Deterministic nine-egg cycle and all three starting colours; hatch after attack 3
  and every six subsequent standard attacks; separate six-attack special clock.
- Destroyed-egg skipping, damage carryover to hatchlings, exhausted hatch handling.
- Quarter-health enrage transition, invalidation of old-phase callbacks, guarded
  corpse/claim/disposal transitions to support one reward per kill.
- Native asset bindings and cache checks for boss/egg/spider HP, size and graphics.
- Capture inspection script reads named JSON entries without extracting/importing
  foreign models, scripts or cache files. Evidence records input hashes.

No production instance, hazards, damage handlers, loot grants or respawn task is
registered by this chunk. The cycle class is an isolated model awaiting runtime
integration; its guards are not yet a complete death/logout security boundary.

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

## Next chunks

1. Native instance entry/exit, full nine-egg arena placement, attack registration
   and lifecycle cleanup (death, logout, teleport, abandoned entry and re-entry).
2. Normal melee/ranged/magic selection and launch-time protection; drains; acid
   special patterns and all three araxyte behaviours.
3. Enrage cleave telegraph/geometry, persistent acid, self-damage and Aranea boots.
4. Corpse harvest/destroy choice, native drop table/KC/pet/collection log, one-time
   reward guarantee and requested 34-tick (20.4 s) respawn/HUD integration.
5. Isolated build/boot, live visual acceptance, then a new installer and explicit
   approval before merging. Do not merge this foundation as a completed boss.

Use native symbol mappings. Do not adopt raw IDs or foreign assets from the capture.
Do not change accepted Zulrah or resume deferred special attacks as a side effect.

Validation: 7 tests pass, module formatting passes, full server shadow JAR builds.
Capture inspection successfully processes all 1816 events. No live encounter or
installer validation is claimed for this foundation.
