# Upstream review ? 2026-10-03

Observed upstream main: `71c5ec59f427463de34bd90f538e5bd68cb42d93`.
Accepted local runtime: revision 240 at `444ead71b`. No upstream content is adopted here.

| Incoming work | Local overlap / dependency | Decision |
|---|---|---|
| `0b16a53e5` cutscene / abandoned instance recovery | Equivalent source exists via `a53731b25`; tests differ by terminal newline | Already represented; no duplicate handlers |
| `625fe96ab` expand Boss DSL; `3c11747d2` native bosses migrate | Custom Zulrah, timer/death integration and EffectInterpreter | DEFERRED REVIEW |
| `59e8281fb`, `e436dda5f` revision 241 | Protocol libraries, cache symbols, CS2, native interfaces and pinned Nero client | DEFERRED REVIEW; upgrade as a paired project |
| `ea1329d47` AGENTS; progress/README updates | Local fork operating manual / inventory | Keep local user workflow; inspect new rules during upgrade |
| `71c5ec59f` upstream deployment workflow | Upstream secrets, hosts and deployment assumptions | Do not import as a fork deployment |

Before a future sync, compare functionality, correctness, native integration,
architecture/core changes, tests, edge cases, performance, maintainability and
custom-only value. Record CUSTOM WINS / UPSTREAM WINS / HYBRID / DEFERRED REVIEW.
A clean Git merge is not semantic validation. Exactly one gameplay handler path remains active.

Revision 241 acceptance: paired client login; cache build; skills/quests/commands/pets
and menu signatures; Zulrah entry/reach/loot/relog; specials/shields; GWD/other timers;
plugin bridge and isolated boot; then documented in-client checks. Do not change a
revision number or overwrite current modules merely to claim newest-upstream parity.
