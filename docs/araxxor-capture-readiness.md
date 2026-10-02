# Araxxor capture readiness

The supplied `recording-20261001-014324.npack` and matching Nero bridge JSON provide
reference observations, not an executable boss implementation. No Araxxor encounter
has been added by the Max cape change.

## Observed data

- Bridge source: `alora-runelite-java11`; captured 2026-09-30 23:41:06 UTC.
- 1,816 events across ticks 0 through 231, including 139 hitsplats, 153 animation
  changes, 45 projectile spawns, five NPC spawns and two actor deaths.
- Initial Araxxor: NPC 13668, template position (3633, 9816, 0).
- Initial eggs: NPC 13670 at (3626, 9809), (3630, 9828), (3644, 9808);
  NPC 13674 at (3632, 9804), (3644, 9824); NPC 13672 at (3638, 9804),
  (3638, 9828), all on plane 0.
- Later NPCs: 13671 twice, 13673, 13675 and 13669. Araxxor's actor-death event is
  at tick 201; later observations include the death and corpse/harvest sequence.
- Observed projectile IDs: 2924 (29 events), 1560 (15), 1622 (one). These include
  player attacks: ownership and effects must be resolved before assigning mechanics.
- Bridge geometry includes 999 terrain entities, 458 ground objects, 386 game
  objects and 68 walls. The archive's missing-dependency report is empty.

The instance uses runtime coordinates around (10847..10873, 5265..5301). Use each
entity's template coordinates: instance chunks do not share one global translation.
An empty missing-dependency report only establishes the capture's asset coverage;
it does not prove that every boss phase or rule was observed.

## Implementation starting point

The server already contains `AraxxorDropTable` and pet definitions, but no Araxxor
boss controller was found. Reuse native revision-240 assets and symbolic gamevals;
verify every captured NPC, animation and projectile against this cache instead of
blindly copying IDs or foreign cache assets.

Next implementation work should cover instance entry/exit and cleanup, attack
selection and timing, eggs and spider behaviours, acid/arena hazards, enrage,
death/harvest rewards and the existing drop table, kill count, pet and respawn HUD.
Validate these against authoritative mechanics and additional observations where
this single kill is ambiguous. Test death, logout, re-entry and instance teardown
before treating the encounter as ready to play.
