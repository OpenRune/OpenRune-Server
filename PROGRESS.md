# OpenRune fork progress

Updated **2026-10-04**. This is the human-reviewed progress entry point.
The automatic scanner writes [CONTENT_INVENTORY.md](CONTENT_INVENTORY.md), never this file.
A symbol, pet or drop table does not establish a playable or verified boss.

**Accepted runtime:** revision 240, server `6168204ee`, Nero Studio `0efcb039`.
It is preserved unchanged during repository organization.
[Exact recovery baseline](docs/custom/baseline.md).

| Area | Actual status |
|---|---|
| Current playable build | User-accepted; 121 selected server tests + 86 Nero tests; isolated boot and server GitHub CI passed |
| Pets, commands, max cape, timers | Implemented; covered regression cases verified |
| Monster/pet/skill/quest interfaces | Implemented; remaining visual edge cases listed in the detailed progress |
| All weapon specials | USER-ACCEPTED CHECKPOINT / REMAINDER PAUSED; 191/285 registered; 94 still missing; registration does not certify full mechanics |
| Charged weapons | Tested charge/attack slices for tridents, scythes, blowpipe, Eye of Ayak and Sanguinesti; atomic Shadow/Venator loading/refunds; 83 selected weapon/special/impact tests pass; live effect qualification pending |
| Weapon checkpoint validation | 103 tests including NPC/pet/Zulrah regressions pass; server JAR build and isolated Nero-bridge boot pass; accepted installation not replaced |
| Installable weapon checkpoint | `weapons-update-20261003`, server `880c6a9fa`; 100 payload targets checked against installed guides/cape baseline; installer/rollback tests pass; user installation pending |
| Subsequent melee special effects | Native impact callbacks replace independent timers for drains/healing/freeze/run-energy effects; 42 special tests pass; not included in the frozen installer above |
| Further melee families | Dragon claws, Dragon scimitar, Darklight/Arclight/Emberlight, Dragon sword and Ancient mace: 17 additional item variants; 58 special tests and 122 selected tests total pass; full server build passes; client animation verification pending |
| Latest special validation | 99 special tests + 12 command/interface tests pass; build and isolated boot pass; user gave merge approval on 2026-10-04 |
| Zulrah | Active custom encounter; old alternative recovery code is review material, not installed |
| Araxxor | IN PROGRESS: cycle model, native asset checks and capture evidence; no playable encounter yet |
| Doom | Research only; no active encounter/delve system |
| Revision 241 | Upstream review pending; no automatic upgrade |
| Repository organization | Complete: 15 stale branches archived and removed; main and active integration branch retained |

**[Detailed status, origins, NOW / NEXT / LATER / BACKLOG](CUSTOM_PROGRESS.md)**

[Custom inventory](CUSTOM_CONTENT.md) ? [Operating manual](OpenRune_Fork_Development_Workflow.md)
? [Branch audit](docs/custom/branch-audit-20261003.md) ? [Custom documentation](docs/custom/README.md)

Future work: one focused topic per commit, relevant tests, update status and dependency
notes, then merge only a verified state. Compare overlapping upstream features before adoption.

Demonbane follow-up: parameter-only cache overlays add 96 verified demon flags and
two Duke resistance flags; all 16,577 NPC definitions retain other fields. Claws
reductions now apply per split hit. 122 selected tests, the full server JAR and isolated Nero-bridge boot pass;
these changes are outside the frozen `880c6a9fa` installer.

Current acceptance milestone: `special-fx-update-20261004` / `6168204ee`. User approved the final checkpoint for merge; prior pending notes describe older packages. Remaining specials are parked. Araxxor is next.
