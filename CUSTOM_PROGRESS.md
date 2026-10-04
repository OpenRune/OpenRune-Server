# EvolvedMind OpenRune progress

Updated: 2026-10-04. The user has accepted the current runtime and explicitly requires
it to be preserved. The completed organization pass changed documentation and Git references,
not gameplay, cache, plugins or the installed package.

**Current accepted server:** `6168204ee3992a3e00f6906e34200d4cff3e95f4` (revision 240).
**Paired Nero Studio:** `0efcb039c539a469a1dab2c4c654c61ecf41aa51`.
[Baseline / test evidence](docs/custom/baseline.md) ? [Branch audit](docs/custom/branch-audit-20261003.md)

## Status meaning

VERIFIED means the stated checks passed, not exhaustive OSRS correctness.
IMPLEMENTED / NEEDS TESTING means code exists with remaining acceptance checks.
NEEDS REVIEW and UPSTREAM REVIEW forbid automatic adoption.
PLANNED / NOT STARTED describe future work, not functionality in the current build.

## Active work

| Work | State | Scope |
|---|---|---|
| Repository organization | VERIFIED | 15 stale branches archived and removed; 2 branches retained; accepted runtime preserved |
| Accepted gameplay bundle | VERIFIED at baseline | User acceptance + 121 selected server tests, 86 Nero tests, isolated boot; GitHub server CI/format/gameval checks passed |
| Weapon completeness | ACCEPTED CHECKPOINT / REMAINDER PAUSED on `feature/weapon-completeness` | Requested after cleanup: 285/285 special target plus normal attacks, charges and FX; Zulrah encounter is frozen |
| Weapon test installer | IMPLEMENTED / NEEDS USER TESTING | `special-fx-update-20261004` at `6168204ee`; user gave explicit green light on 2026-10-04 |

## Bosses

| Feature | State | Origin | Evidence / limitation |
|---|---|---|---|
| Zulrah | IMPLEMENTED / NEEDS TESTING for exhaustive mechanics | CUSTOM | User played it; halberd regression covered; old death-recovery implementation is archived, not active |
| GWD four bosses | IMPLEMENTED / NEEDS TESTING for full encounters | UPSTREAM + CUSTOM EXTENSIONS | Current respawn policy 100 ticks / 60 s; Nero reads actual deadlines |
| Other existing boss modules | IMPLEMENTED / NEEDS TESTING | UPSTREAM; selected custom timer integration | Amoxliatl, Barrows, Callisto, demonic gorilla, Duke, gemstone crab, KBD, Leviathan, Muspah, Scurrius, Spindel, tormented demon, Vardorvis, Whisperer |
| Araxxor | IN PROGRESS on `feature/araxxor` | CUSTOM | Deterministic cycle and native asset checks; no live encounter yet; see docs/custom/araxxor.md |
| Doom of Mokhaiotl | NOT STARTED encounter | CUSTOM research + upstream drop/pet data | Archived research exists; a drop table does not constitute a boss fight |

## Systems

| System | State | Origin | Verification / remaining work |
|---|---|---|---|
| Follower relog and Lil' Zik morph fixes | VERIFIED for covered regressions | UPSTREAM + CUSTOM EXTENSIONS | Pet synchronization/morph suites and user gameplay feedback |
| Native commands / inventory loadouts / ::spres | VERIFIED for covered regressions | UPSTREAM + CUSTOM EXTENSIONS | Inventory-only grants; no replacement of worn gear |
| Max cape submenu indexing | VERIFIED for covered regressions | CUSTOM | 15 tests, including actual packets and Farming Guild selection |
| Monster drops, pet gallery, skill/quest guides | IMPLEMENTED / NEEDS TESTING for visual edge cases | HYBRID | Search, navigation, log/map and all skill buttons tested; fixed/resized scrolling remains an explicit visual checklist |
| Combat specials and shields | ACCEPTED CHECKPOINT / REMAINDER PAUSED | UPSTREAM + CUSTOM EXTENSIONS | 191/285 special-energy items registered; 94 missing, six shield forms covered separately; see combat audit |
| Respawn countdowns | VERIFIED for covered regressions | HYBRID | Actual deadline snapshots; non-GWD policy is 34 ticks = 20.4 s |
| Nero object library and loot colours | VERIFIED for covered regressions | CUSTOM integration | Paired plugin tests; native Ground Items aggregation/value colour fallback |
| Plugin lifecycle / offline login / installer alternatives | NEEDS REVIEW | Archived custom alternatives | Not imported into the accepted runtime |

## Skills, quests, raids and minigames

- Existing skill modules: cooking, crafting, firemaking, fishing, herblore, magic,
  mining, prayer, runecrafting, slayer, smithing, thieving, woodcutting, plus shared
  combat/skill APIs. Presence does not certify complete skills.
- No sailing gameplay integration in this accepted build; cape boat actions remain disabled.
- Quest tests cover existing implemented quest flows; guide styling does not complete missing quests.
- Raids/minigames have no newly verified implementation in this pass. Pet names,
  drops, collection-log entries or cache symbols must not be counted as playable encounters.
- The automatic presence scanner is retained as supporting inventory in
  [CONTENT_INVENTORY.md](CONTENT_INVENTORY.md), not as the quality/status authority.

## Roadmap

### NOW

- User accepted PR #15 on 2026-10-04. Preserve this checkpoint; remaining specials are parked. Araxxor is the next focused feature.
- Keep the accepted baseline recoverable and main organized.

### NEXT

- Start Araxxor only as a focused feature slice with mechanics and acceptance tests.
- Compare archived improvements only when intentionally selected for new work.

### LATER

- Paired revision 241 upgrade: client/protocol/cache, Boss DSL, custom encounter and HUD review.
- Other weapon families remain in the active completeness audit; never promote registration counts to full mechanic verification.

### BACKLOG

- Doom encounter/delve progression after Araxxor priorities are resolved.
- Reassess old Zulrah item recovery and personal pet helper commands.

## Upstream review queue

`upstream/main` observed at `71c5ec59f427463de34bd90f538e5bd68cb42d93`.
Revision 241 and Boss DSL changes overlap custom dependencies. Outcome:
**DEFERRED REVIEW**; no upstream merge or runtime upgrade in this organization pass.
See [upstream review](docs/custom/upstream-review.md).

Demonbane follow-up: parameter-only cache overlays add 96 verified demon flags and
two Duke resistance flags; all 16,577 NPC definitions retain other fields. Claws
reductions now apply per split hit. 122 selected tests, the full server JAR and isolated Nero-bridge boot pass;
these changes are outside the frozen `880c6a9fa` installer.

Dragon hasta slice: all five variants implemented; 61 special tests pass including
partial/full energy, misses, NPC/PvP and native cost modifiers. Coverage 162/285;
123 registrations remain missing, and live FX qualification is still pending.

Saradomin sword slice: all three identities registered with distinct ordinary and
blessed damage paths, caster/target effects and hybrid accuracy. 65 special tests
and 129 selected tests overall pass; full server JAR builds. Current registry is
165/285, with 120 missing. Blessed sword degradation and live FX qualification
remain open. The frozen installer and accepted installation are unchanged.

Ancient warrior slice: four Vesta longswords and three Statius warhammers registered;
variant-specific 30%/75% Defence drains, post-roll damage reductions and reduced
defence accuracy tested. 68 special tests pass; registry 172/285 with 113 missing.
Their mode restrictions, degradation and visual alignment are not fully qualified.

Abyssal tentacle: two variants added, including miss-triggered freeze and independent
poison with native immunities. Whip graphics/player animation mix-up repaired and
target ownership regression-tested. 73 special tests pass; 174/285 registrations,
111 missing. Tentacle charge degradation and live visual parity remain open.

Staff protection: eight variants added with temporary native state, 100-tick expiry,
prayer stacking and damage-time weapon checks. 79 special / 143 selected tests,
full JAR, cache rebuild and isolated Nero-bridge startup/clean shutdown pass.
Registry at this slice: 182/285; 103 missing. Live visual qualification and remaining weapon
mechanics are still open; the accepted installation and frozen installer are intact.


Latest ranged / BH / Dorgeshuun slices: nine Dark bow identities, corrected minimum
hits and BH distributions, projectile timing and Dragon knife Duality effects.
BH Dragon mace uses its own 60% defence roll. Four Bone daggers and Dorgeshuun
crossbow now guarantee hits according to the last positive damager, respecting the
quest policy; their non-stacking Defence drain waits for actual impact. Native
contribution totals are unchanged. 97 special / 161 selected tests and the full
server JAR pass. Current registry: 191/285; 94 missing. Seeking arrows need missing
double-launch metadata; mode restrictions, boss drain floors and live FX validation
remain open. The accepted installation and frozen installer remain unchanged.

Weapon acceptance command: `::weptest <set>` supplies registered combat weapons/shields in inventory-only atomic batches. New specials are paused; 94 missing registrations remain deferred. See [commands](docs/custom/commands.md).

Final acceptance FX correction: Bludgeon graphic moved below NPC/player targets;
Volatile/Eldritch player animation separated from their graphic-model sequences.
Damage and hit timing unchanged. New specials remain paused; final visual user
check and explicit merge approval are still pending.

## Acceptance milestone — 2026-10-04

User explicitly approved the final FX checkpoint (6168204ee) for merge. PR #15 is merged into the gameplay integration branch; PR #14 carries the combined accepted work to main. 99 special tests, 12 command/interface tests, full build and isolated boot passed; all three GitHub workflows on 6168204ee passed. This acceptance does not declare 285/285 completeness: 191 registered, 94 deferred. Earlier pending notes above describe historical checkpoints.

Araxxor foundation: separate cycle/asset module and reproducible capture evidence; runtime entry/combat/loot still pending. The seven-egg snapshot is incomplete and later-timestamped initial-state rows must not be mixed into the start snapshot. No live installation changes.
