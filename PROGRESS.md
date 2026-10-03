# OpenRune fork progress

Updated **2026-10-03**. This is the human-reviewed progress entry point.
The automatic scanner writes [CONTENT_INVENTORY.md](CONTENT_INVENTORY.md), never this file.
A symbol, pet or drop table does not establish a playable or verified boss.

**Accepted runtime:** revision 240, server `444ead71b`, Nero Studio `0efcb039`.
It is preserved unchanged during repository organization.
[Exact recovery baseline](docs/custom/baseline.md).

| Area | Actual status |
|---|---|
| Current playable build | User-accepted; 121 selected server tests + 86 Nero tests; isolated boot and server GitHub CI passed |
| Pets, commands, max cape, timers | Implemented; covered regression cases verified |
| Monster/pet/skill/quest interfaces | Implemented; remaining visual edge cases listed in the detailed progress |
| All weapon specials | IN PROGRESS on `feature/weapon-completeness`; 140/285 registered; 145 still missing; registration does not certify full mechanics |
| Charged weapons | Tested charge/attack slices for tridents, scythes, blowpipe, Eye of Ayak and Sanguinesti; atomic Shadow/Venator loading/refunds; 83 selected weapon/special/impact tests pass; live effect qualification pending |
| Weapon checkpoint validation | 103 tests including NPC/pet/Zulrah regressions pass; server JAR build and isolated Nero-bridge boot pass; accepted installation not replaced |
| Zulrah | Active custom encounter; old alternative recovery code is review material, not installed |
| Araxxor | Planned; capture researched; no encounter implementation yet |
| Doom | Research only; no active encounter/delve system |
| Revision 241 | Upstream review pending; no automatic upgrade |
| Repository organization | Complete: 15 stale branches archived and removed; main and active integration branch retained |

**[Detailed status, origins, NOW / NEXT / LATER / BACKLOG](CUSTOM_PROGRESS.md)**

[Custom inventory](CUSTOM_CONTENT.md) ? [Operating manual](OpenRune_Fork_Development_Workflow.md)
? [Branch audit](docs/custom/branch-audit-20261003.md) ? [Custom documentation](docs/custom/README.md)

Future work: one focused topic per commit, relevant tests, update status and dependency
notes, then merge only a verified state. Compare overlapping upstream features before adoption.
