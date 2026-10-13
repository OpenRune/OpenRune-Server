# Combat Achievements — native framework, first slice and reviewed implementation plan

Reviewed 2026-10-13 against fork main `219995c52be04acfc02fcaf884dbee3b76a52f11`.
The initial delivery was research/planning only. The owner then authorized
implementation, now active on `feature/combat-achievements` in an isolated checkout.
Current status: [PROGRESS.md](../../PROGRESS.md). Keep Java 21, revision 240 and the
accepted runtime/cache. This first slice does not complete the full CA system.

## Sources and evidence limits

Owner references: [Combat Achievements](https://oldschool.runescape.wiki/w/Combat_Achievements)
and [Strategies](https://oldschool.runescape.wiki/w/Combat_Achievements/Strategies),
followed by [All tasks](https://oldschool.runescape.wiki/w/Combat_Achievements/All_tasks)
and [RuneMonk](https://runemonk.com/).
Direct Wiki access was denied by robots.txt. Older indexed excerpts from
[Strategies alias](https://oldschool.runescape.wiki/w/Combat_achievement_guide)
support starting with kill-count tasks, then restrictions/mechanics and speed tasks.
That player strategy informs delivery order; it is not the authoritative task catalogue.
The supplied All_tasks page also remained robots-blocked. Normal browser inspection
was attempted after browser setup/troubleshooting and found no connected browser.
RuneMonk's accessible page exposed entity/model viewing without authoritative CA
catalogue data. Those limits do not imply that task data is absent.

The implementation instead freezes the available isolated revision-240 preview
LIVE/SERVER cache and its gamevals, decoded read-only with the existing OpenRune
codecs. These fingerprints identify the available source snapshot, not the latest
accepted runtime cache. The
[versioned catalogue](combat-achievements-catalogue.json) records every native task,
source SHA256 fingerprints, enum/struct/varbit mappings, complete thresholds and
Group Ironman exclusions. LIVE/SERVER task metadata and selected completion mappings
match; both source cache data-file hashes remain unchanged after research.

[Jagex's shipped points update, 3 May 2023](https://secure.runescape.com/m=news/points-based-combat-achievements?oldschool=1)
confirms six difficulty tiers worth 1–6 points, rewards reachable by combining tasks
across tiers, and a native progress display. Its historical task counts and thresholds
must not be copied as revision-240/current constants. The frozen revision-240
catalogue and threshold table below provide this implementation's source contract.
Do not use Deadman/Leagues points
or the pre-rework requirement to complete every task in a tier.
The [Laidee dialogue excerpt](https://oldschool.runescape.wiki/w/Transcript%3ALaidee_Gnonock)
explains that added tasks can increase rank thresholds; catalogue updates need migrations.

Initial fork files directly checked: AGENTS.md, PROGRESS.md, roadmap.json, documentation index,
SummarySideScript.kt, SlayerSuperiorManager.kt, BraceletOfSlaughter.kt,
ClueScrollDropChecks.kt and BarrowsScript.kt. Code search returned no indexed fork
results even for files fetched directly, so absence of search matches is not proof
of absent implementation. The initial documentation-only review performed no local
checkout/build. Implementation continuation uses a separate checkout and targeted
source/native audits; reward and wider encounter audits remain source gates.

## Initial foundation and reuse decision

| Confirmed source | Meaning and planned integration |
|---|---|
| SummarySideScript opens interface.ca_overview using protected access | Existing entry point, not evidence that task navigation/tracking works. Reuse native widgets/CS2 and verify click routes. |
| SlayerSuperiorManager.hasEliteCombatAchievements returns false | Elite superior-rate consumer exists but cannot unlock. Replace through a shared reward entitlement query. |
| BraceletOfSlaughter uses ELITE_COMBAT_ACHIEVEMENTS_COMPLETE = false | Recharge benefit is inert. Audit the corresponding Expeditious implementation and test real charge depletion. |
| ClueScrollDropChecks.hasUnlockedEasy/Medium/Hard/Elite/MasterCombat all return true | Existing clue-rate benefits are unconditional. Audit generated/static and dynamic tables to avoid double bonuses or accidental accepted-loot regressions. |
| BarrowsScript recognizes medium+ CA offhands and Ghommal defender variants | An item-based perk consumer exists; distinguish owning/equipping an item from account reward entitlement. Preserve accepted Barrows behavior pending explicit source-backed integration. |
| PROGRESS identifies accepted bosses, separate Gauntlet records continuation and future raids | Task coverage follows playable verified mechanics, not module/model presence. Do not infer perfection from generic KC/PB data. |
| Roadmap Combat Tasks represents Combat Achievements; Achievements is another entry | On implementation start, move CA into the active PROGRESS section and remove Combat Tasks from the backlog source. General diaries/achievements remain separate. |

Searches of upstream main and PRs used CombatAchievements, combat_achievements,
combat achievement/task, achievement, ghommal and ca_overview. No dedicated complete
CA framework PR was returned. The indexed upstream snapshot was
`cc6dbc89b6fc77670c721f982668f5810e21faea`; searches are not exhaustive.

[Upstream PR #148](https://github.com/OpenRune/OpenRune-Server/pull/148) is merged,
head `4d3cdde1a838f023097d03249b4d0102285a8faa`. Metadata and relevant collection-log/
summary patches were inspected. They provide native navigation/state examples and
explicitly do not deliver Combat Achievements. Reuse the fork's already present
integration; do not import the whole PR. No upstream test/CI pass is claimed.
Other returned broad Slayer/raid PRs are candidates for later producer-hook audits,
not selected CA implementations. Detailed review/test metadata is required before
any new code port; preserve attribution/licensing.

**Decision:** implement the missing shared CA framework and boss adapters on native
fork APIs. Keep existing controllers; add explicit evidence events where needed.
No upstream merge, revision upgrade, general diary implementation or completionist
eligibility change is included in this plan.

## Target scope and catalogue contract

Complete system: native task/boss/reward browsing, task detectors, persistent progress,
point/rank calculation, notification settings, reward NPC claims/reclaims, item rewards
and all applicable perk consumers. Deliver supported encounters in bounded batches.
Unavailable bosses/activities retain their catalogue identity but cannot award completion.
Full catalogue completion remains dependent on those encounters becoming playable.

Each task records stable native ID, title/source version, difficulty/points, type,
encounter/mode, party limits, exact predicate, timer boundaries, evidence producer,
detector version and support status. Validate native struct/enum/varbit/DB mappings
against revision 240; never identify tasks by list index or name alone.
Maintain implementation coverage in a technical matrix, not a second status log.
No arbitrary lowering of OSRS thresholds to the subset of available bosses.

Task families: KC, speed, restricted actions/equipment, mechanical, perfection and
stamina/streak. Speed timing uses game ticks with explicit strict/inclusive comparison.
Restricted damage checks inspect actual attacks/hits and damage sources, not final
equipment. Zero damage, avoided hazards and missed attacks have task-specific meanings.
Perfection composes documented encounter predicates; absence of telemetry cannot mean success.

## Native source contract and implemented first slice

Task enums `3981..3986` contain **655** unique, contiguous stable IDs `0..654`, with
tier counts **41 / 64 / 89 / 166 / 173 / 122**. Native enum `4846` gives each tier
1–6 points. Cumulative normal-world rank thresholds are therefore **41 / 169 / 436 /
1100 / 1965 / 2697**. All native tasks remain in those calculations even where this
fork has no playable encounter or detector.

Native `ca_get_data` (`7819`) resolves a stable task ID from struct param `1306`.
Completion script `4834` selects word `taskId / 32` and bit `taskId % 32` in
`ca_task_completed_0..20`. The native words are permanent and transmitted by the
existing varp setter. Param `1307` contains chat-action struct `3594`; it is not a
completion varbit. Native completion varbits corroborate the same word/bit layout.
The API checks the pinned ID/tier-count invariants and permanent completion scopes.

Group Ironman modes `4..6` use native group size and task param `1425`. For sizes
2–5, a task is unavailable when `groupSize <= param1425`. That predicate exactly
matches native exclusion enums `4086..4094` and `861`; excluded task bits remain
stored while their points and summaries are omitted for the relevant profile.

| Profile | Cumulative point thresholds |
|---|---|
| Normal | 41 / 169 / 436 / 1100 / 1965 / 2697 |
| Group size 2 | 41 / 169 / 436 / 1096 / 1916 / 2570 |
| Group size 3 | 41 / 169 / 436 / 1096 / 1936 / 2620 |
| Group size 4 | 41 / 169 / 436 / 1096 / 1941 / 2637 |
| Group size 5 | 41 / 169 / 436 / 1100 / 1965 / 2691 |

These are revision-240 catalogue/profile values, not a reduction to available
server content. Exact group encounter histories and catalogue-update
loss/requalification rules remain later work.

| Native task | Stable ID / struct | Detector and boundary |
|---|---|---|
| Barrows Novice | `24` / `3164` | Ten legitimate chest openings. The native description does not require all six brothers. |
| Barrows Champion | `25` / `3165` | Twenty-five legitimate chest openings using the same producer. |
| Kraken Adept | `168` / `3308` | Twenty legitimate credited boss kills. |
| Unnecessary Optimisation | `169` / `3309` | Credited Kraken death after four distinct tentacles from that encounter have died; awakening whirlpools is insufficient. |

`api/combat-achievements` now owns the verified catalogue, completion service,
points/ranks, native summary refresh and narrow Barrows/Kraken evidence consumers.
`content/other/combat-achievements` owns native browsing; boss controllers remain in
their existing modules. API Gradle group `org.rsmod.api` keeps the new API and
content modules with the same project name from colliding on `org.rsmod` coordinates.
Gameplay modules depend on the API and never on reward UI.

New permanent, server-only counters `ca_legitimate_barrows_chests` (`65270`) and
`ca_legitimate_kraken_kills` (`65271`) start at zero. Historical KC/chest totals and
testloot commands do not backfill them. Completion sets a native task bit once,
then derives points/tier/boss summaries from native completion state. The Kraken
hook snapshots eligible death/participant/tentacle evidence before encounter cleanup
and applies its prepared callback once. Unsupported detectors have no award route.

Native overview, tasks, bosses and rewards screens reuse native revision-240 CS2/layouts;
stable boss enum keys govern selection and clickmask bounds. The native boss
browser sends `ResumePauseButton` without a suspended dialogue. The shared input
handler previously closed that modal and tried to resume a missing coroutine, so
the extension adds a bounded stateless `IfModalPauseButton` route for an open modal
while idle. Suspended dialogues retain ownership; existing component/open-interface
checks, pause permission masks, delays and the normal fallback remain enforced.
Reward browsing does not grant items, lamps, claimed packages or perks.

Completion chat uses the existing queue for optional popups. Native settings struct
`319` inverts the popup preference: varbit `ca_task_popup = 0` enables the popup and
`1` disables it. The implementation and notification case use that contract.

The isolated bounded cache tool writes only server varp definitions `65270` and
`65271` with permanent scope and no client transmission. Its reopened snapshot
reports exactly those two changed definitions, **337,567 preserved definitions**
and the original signed varp sentinel. Existing native definitions, task IDs,
varbit layouts and CS2 are preserved. No accepted live cache/JAR/RSA/playerdata is
replaced by this development step.

## Reviewed full-system architecture and remaining integrations

The first slice places native catalogue/progression in the shared API and browsing
in the content module as described above. Future encounter evaluators, entitlement
interfaces, Ghommal/Laidee claims and any needed separate `pack` must preserve that
dependency direction. Claims/perk services below remain intended full-system work.

- Registry resolves stable IDs and dispatches only relevant task evaluators.
- Attempt state is owned by encounter generation and participant ID; it retains mode,
  party history, timing, violations and actor identities. Start/reset/finish are explicit.
  Boss phases/transmogs do not automatically start a fresh attempt.
- Immutable evidence describes attack selection/impact, hazards, prayer/stat changes,
  item consumption, mechanic outcomes, death and activity completion. Define ordering
  for simultaneous hits/deaths; classify damage provenance including poison/venom,
  reflected damage, thralls and environmental damage.
- Completion service validates final evidence and atomically marks a task once.
  Permanent native vars/inventoryvars store completion/claim state; points are derived
  or checked against completed IDs. UI/client vars cannot authorize completion.
- Reward service distinguishes earned rank, claimed packages and currently effective
  perks. Item/lamp delivery and claim markers need an atomic or recoverable transaction.
  Full inventory/cancel/logout cannot lose rewards or duplicate XP.
- Entitlement API replaces scattered true/false placeholders. Item-specific benefits
  still enforce documented equipment/ownership conditions.
- Notifications use the existing queue and native chat/settings. Cleanup on exit,
  death/logout/reset/unload removes transient attempt state and subscriptions.
  Persistent completed tasks survive. No world-wide per-player task scan each tick.

Historical migration: grant only demonstrably supported KC tasks from audited permanent
counters. Existing testloot commands increment some KC, so provenance must be reviewed
before backfilling them as legitimate achievements. Speed/perfection require sufficient
historical evidence; a raw PB or total count is not enough. Record a migration version
and make replay harmless. Admin kits/bypasses need an explicit achievement-eligibility
policy; testloot must never directly award tasks.

## Ordered delivery and acceptance

| Milestone | Work | Exit evidence |
|---|---|---|
| 0. Audit and specification | Full source tree, nested instructions, native IDs/CS2, exact catalogue/thresholds/rewards, upstream candidates, all perk placeholders and KC/PB provenance | Versioned task/reward matrix; resolved mappings; unavailable task list; baseline behavior and deliberate policies recorded |
| 1. Framework/persistence | Registry, attempt lifecycle, completion/points/rank, native vars, migration and entitlement API | Duplicate/reordered events, restart/save-load, boundary IDs/points, catalogue reorder/add/remove, two concurrent encounters and stale NPC-slot reuse |
| 2. First real task slice | Barrows and one accepted single-boss encounter; KC plus a directly verified restriction/mechanic | Real native input → combat/hazard → completion → persisted state; legitimate failure and interruption; no synthetic-only success |
| 3. Native UI and claims | Overview/tasks/bosses/rewards, filters/search, settings; reward NPCs and first verified claim package | Native button producer routes, CS2 arguments/vars, filters and bounds, cancel/full inventory/duplicate claim/relog; fixed/resizable client |
| 4. Accepted boss coverage | Barrows, Zulrah, Araxxor, Kraken, Corp and applicable accepted creatures; only source-confirmed catalogue tasks | Per-task success/failure evidence, team credit rules, speed boundaries, restricted damage sources and perfect/streak reset |
| 5. Perk consumers | Clue rates, Slayer superior/bracelet benefits, hilt/teleport/Barrows perks, cosmetic/reclaim/fusion and other applicable consumers | Claimed/unclaimed/earned/equipped distinction, inventory/death behavior, rate fixtures, no stacked/baked-in double bonus; accepted loot/controller regressions |
| 6. Activity/group tasks | Gauntlet when its integration qualifies; future CoX/other raids and group content as implemented | Real activity completion, mode/party-size/history, all-player failure propagation, joins/leaves/disconnect, wipe/retry and exact timers |
| 7. Full acceptance | Catalogue reconciliation, deferred task/reward dependencies, isolated cache/runtime build, native playtest and preservation package | Executed tests reported, revision240 boot, preserved IDs/varbits/runtime, owner checks of full task → claim → perk → relog cycle |

Reward audit includes hilt tiers and reclaim/teleports, lamps, Slayer/crossbow cosmetics,
defender combinations and lucky-penny effects, clue benefits, Slayer rates/charges,
Barrows drain and relevant activity/instance rewards. This is a consumer inventory to
verify, not a claim that every effect or exact cost is already established.
Benefits for absent activities remain explicit dependencies, never unconditional unlocks.

## Plan review

| Risk found | Correction |
|---|---|
| Requiring all tasks in each tier | Use the shipped point system with pinned catalogue thresholds and update migrations. |
| Awarding perfect tasks from generic NPC death | Require complete encounter evidence; fail closed when telemetry is missing. |
| Counting phase deaths or shared boss damage incorrectly | Encounter completion token and task-specific credit/party rules. |
| One-time item grants tied to earned rank | Separate task completion, rank, claims and perk eligibility; recoverable claims. |
| Clue benefits currently always enabled while Slayer benefits are disabled | Audit each real consumer; baseline and migration policy before replacing placeholders. Never blanket-change accepted loot. |
| Testloot/historical counters satisfy real-game tasks | Audit provenance; allow only justified backfill and exclude administrative reward simulation. |
| Stamina/perfection state survives departure or mixes parties | Encounter-owned reset rules and participant histories; no slot-keyed global state. |
| Unsupported tasks omitted from thresholds | Preserve pinned OSRS catalogue; partial delivery is explicitly partial. |
| General Achievements or completionist roadmap pulled into CA work | Separate systems; expose reviewed integration points without expanding this request. |

Initial review outcome: suitable sequence after milestone 0. Scope XXL because most work is
encounter instrumentation and reward consumers, not the UI. No engineering duration
or overall completion percentage is justified yet.

## Verification and remaining research

Use registered native action producers and consumers, controlled game randomness,
task-specific success/failure traces, and negative tests for unrelated encounters.
Test attack-versus-impact equipment/prayer timing, misses, zero hits, simultaneous
events, death/leave/logout/reset, full inventory, claim replay and restart.
Run scoped tests/format, affected API/boss/Slayer/drop regressions, JAR and bounded
cache/gameval checks for code changes; reopen native vars and verify original mappings.
The initial documentation delivery required no gameplay test/build. Code continuation
now requires the scoped tests, bounded packing, runtime build and native acceptance
appropriate to the first slice.

### First-slice validation record — 2026-10-13

The final scoped command passes **65 tests**, with no failures or skipped cases:
API 9, network 17, native UI 7, Barrows 12 and Kraken 20. The tests cover catalogue
boundaries, legitimate counters, permanent-state reconstruction, signed bit 31/last
word, rank/claim separation, Group Ironman exclusions, inverted popup settings and
full aggregate bounds. Native chest interactions and native tentacle/boss death
queues reach the real completion service; duplicate, partial, bypass, administrative
testloot and interruption cases are included. Repeated chest actions close the
native reward modal through its registered reward-delivery callback before the next
protected interaction.

Exact-file Kotlin formatting checks and generated progress checks pass. The full
Java 21 `:server:app:shadowJar` build passes. JAR SHA256:
`165a9fa2257c5c50a1b2a0815a29b2c4de0f707a20a5567885210f610446860b`
(218,940,874 bytes). A bounded smoke against that exact JAR passes production
`EventModule` injection, CA plugin startup, native boss key 33 selection, real
progress refresh and invalid/closed input rejection. This does not establish full
server boot, connected services or client rendering.

Validation uses isolated copies of the available preview cache. The CA-only cache
adds exactly the two permanent counters and preserves 337,567 existing definitions;
it is retained as `CA-BOUNDED`. The native gameplay fixture additionally needs
existing source definitions absent from that preview: parameters 65557/65559,
Barrows varp 64132 and its original seven varbits 64200..64206. Their bounded
bootstrap changes only eleven config files, including the standard/server records
of the same Barrows varp, and retains the signed sentinel. These are existing
prerequisites, not new CA production definitions. For full-JAR compilation,
current existing pack tables and pet enums are generated in a separate
`CA-COMPILATION` copy. No temporary bootstrap source, generated API code or cache
binary is part of this feature commit. Accepted live cache/JAR/RSA/playerdata and
the original source snapshots are preserved.

Permanent-state reconstruction copies only permanent vars into a new player; it
does not exercise the production database serializer. Owner checks still need the
actual client in fixed/resizable mode, legitimate task → notification → relog,
boss search/selection and interruptions. Production save/load, reward NPC claims,
lamps/items/reclaims, perk consumers and wider catalogue detectors remain open.

Remaining source checks: exact server-side predicates for the remaining native
catalogue tasks, all item/perk costs and claim/equipment requirements, historical
completion policy, timing/group restrictions and catalogue-update
loss/requalification rules. Native revision-240 IDs/counts/text/layouts and normal/
Group Ironman thresholds are now frozen; the four selected detector predicates are
bounded by that verified native metadata and real encounter evidence.
Direct Wiki sources were blocked; acquire permitted current/versioned reference data.
Do not interpret historical proposals or older strategy excerpts as exact current parity.
