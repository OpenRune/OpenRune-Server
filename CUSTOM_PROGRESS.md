# EvolvedMind OpenRune progress

Updated: 2026-10-03. The user has accepted the current runtime and explicitly requires
it to be preserved. The completed organization pass changed documentation and Git references,
not gameplay, cache, plugins or the installed package.

**Current accepted server:** `444ead71bec435e3eba0c378aa6718f7a7ac57d4` (revision 240).
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
| Weapon completeness | IN PROGRESS on `feature/weapon-completeness` | Requested after cleanup: 285/285 special target plus normal attacks, charges and FX; Zulrah encounter is frozen |
| Weapon test installer | IMPLEMENTED / NEEDS USER TESTING | `weapons-update-20261003`, checkpoint `880c6a9fa`; preflight and scratch install/rollback pass; later melee-impact work is separate |

## Bosses

| Feature | State | Origin | Evidence / limitation |
|---|---|---|---|
| Zulrah | IMPLEMENTED / NEEDS TESTING for exhaustive mechanics | CUSTOM | User played it; halberd regression covered; old death-recovery implementation is archived, not active |
| GWD four bosses | IMPLEMENTED / NEEDS TESTING for full encounters | UPSTREAM + CUSTOM EXTENSIONS | Current respawn policy 100 ticks / 60 s; Nero reads actual deadlines |
| Other existing boss modules | IMPLEMENTED / NEEDS TESTING | UPSTREAM; selected custom timer integration | Amoxliatl, Barrows, Callisto, demonic gorilla, Duke, gemstone crab, KBD, Leviathan, Muspah, Scurrius, Spindel, tormented demon, Vardorvis, Whisperer |
| Araxxor | PLANNED | CUSTOM research | Capture inventory exists; encounter not implemented |
| Doom of Mokhaiotl | NOT STARTED encounter | CUSTOM research + upstream drop/pet data | Archived research exists; a drop table does not constitute a boss fight |

## Systems

| System | State | Origin | Verification / remaining work |
|---|---|---|---|
| Follower relog and Lil' Zik morph fixes | VERIFIED for covered regressions | UPSTREAM + CUSTOM EXTENSIONS | Pet synchronization/morph suites and user gameplay feedback |
| Native commands / inventory loadouts / ::spres | VERIFIED for covered regressions | UPSTREAM + CUSTOM EXTENSIONS | Inventory-only grants; no replacement of worn gear |
| Max cape submenu indexing | VERIFIED for covered regressions | CUSTOM | 15 tests, including actual packets and Farming Guild selection |
| Monster drops, pet gallery, skill/quest guides | IMPLEMENTED / NEEDS TESTING for visual edge cases | HYBRID | Search, navigation, log/map and all skill buttons tested; fixed/resized scrolling remains an explicit visual checklist |
| Combat specials and shields | IN PROGRESS | UPSTREAM + CUSTOM EXTENSIONS | 162/285 special-energy items registered; 123 missing, six shield forms covered separately; see combat audit |
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

- Complete weapon families in tested chunks: normal attacks, charges, special mechanics and effect placement.
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
