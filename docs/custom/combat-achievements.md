# Combat Achievements — research and reviewed implementation plan

Reviewed 2026-10-13 against fork main `219995c52be04acfc02fcaf884dbee3b76a52f11`.
This delivery is research/planning only. Gameplay has not started. Current status:
[PROGRESS.md](../../PROGRESS.md). Keep Java 21, revision 240 and accepted runtime/cache.

## Sources and evidence limits

Owner references: [Combat Achievements](https://oldschool.runescape.wiki/w/Combat_Achievements)
and [Strategies](https://oldschool.runescape.wiki/w/Combat_Achievements/Strategies).
Direct Wiki access was denied by robots.txt. Older indexed excerpts from
[Strategies alias](https://oldschool.runescape.wiki/w/Combat_achievement_guide)
support starting with kill-count tasks, then restrictions/mechanics and speed tasks.
That player strategy informs delivery order; it is not the authoritative task catalogue.

[Jagex's shipped points update, 3 May 2023](https://secure.runescape.com/m=news/points-based-combat-achievements?oldschool=1)
confirms six difficulty tiers worth 1–6 points, rewards reachable by combining tasks
across tiers, and a native progress display. Its historical task counts and thresholds
must not be copied as revision-240/current constants. Freeze a revision-compatible
catalogue and threshold table before implementation. Do not use Deadman/Leagues points
or the pre-rework requirement to complete every task in a tier.
The [Laidee dialogue excerpt](https://oldschool.runescape.wiki/w/Transcript%3ALaidee_Gnonock)
explains that added tasks can increase rank thresholds; catalogue updates need migrations.

Fork files directly checked: AGENTS.md, PROGRESS.md, roadmap.json, documentation index,
SummarySideScript.kt, SlayerSuperiorManager.kt, BraceletOfSlaughter.kt,
ClueScrollDropChecks.kt and BarrowsScript.kt. Code search returned no indexed fork
results even for files fetched directly, so absence of search matches is not proof
of absent implementation. Full source-tree/native asset audit remains milestone 0.
No local checkout/build was performed for this documentation request.

## Current foundation and reuse decision

| Confirmed source | Meaning and planned integration |
|---|---|
| SummarySideScript opens interface.ca_overview using protected access | Existing entry point, not evidence that task navigation/tracking works. Reuse native widgets/CS2 and verify click routes. |
| SlayerSuperiorManager.hasEliteCombatAchievements returns false | Elite superior-rate consumer exists but cannot unlock. Replace through a shared reward entitlement query. |
| BraceletOfSlaughter uses ELITE_COMBAT_ACHIEVEMENTS_COMPLETE = false | Recharge benefit is inert. Audit the corresponding Expeditious implementation and test real charge depletion. |
| ClueScrollDropChecks.hasUnlockedEasy/Medium/Hard/Elite/MasterCombat all return true | Existing clue-rate benefits are unconditional. Audit generated/static and dynamic tables to avoid double bonuses or accidental accepted-loot regressions. |
| BarrowsScript recognizes medium+ CA offhands and Ghommal defender variants | An item-based perk consumer exists; distinguish owning/equipping an item from account reward entitlement. Preserve accepted Barrows behavior pending explicit source-backed integration. |
| PROGRESS identifies accepted bosses, separate Gauntlet records continuation and future raids | Task coverage follows playable verified mechanics, not module/model presence. Do not infer perfection from generic KC/PB data. |
| Roadmap Combat Tasks represents Combat Achievements; Achievements is another entry | Use one CA roadmap identity. General diaries/achievements remain separate. |

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

## Proposed architecture

Proposed shared `api/combat-achievements` exposes narrow evidence and entitlement
interfaces; proposed `content/other/combat-achievements` owns catalogue/evaluators,
Ghommal/Laidee interactions, native UI and separate `pack`. Confirm module placement
and dependency direction in milestone 0 so combat/bosses never depend on reward UI.

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

Review outcome: suitable sequence after milestone 0. Scope XXL because most work is
encounter instrumentation and reward consumers, not the UI. No engineering duration
or overall completion percentage is justified yet.

## Verification and remaining research

Use registered native action producers and consumers, controlled game randomness,
task-specific success/failure traces, and negative tests for unrelated encounters.
Test attack-versus-impact equipment/prayer timing, misses, zero hits, simultaneous
events, death/leave/logout/reset, full inventory, claim replay and restart.
Run scoped tests/format, affected API/boss/Slayer/drop regressions, JAR and bounded
cache/gameval checks for code changes; reopen native vars and verify original mappings.
No gameplay test/build is needed or claimed for this documentation-only delivery.

Remaining source checks: complete revision240 task IDs/counts/text/predicates and
thresholds, all item/perk costs and claim/equipment requirements, historical completion
policy, timing/group restrictions and catalogue-update loss/requalification rules.
Direct Wiki sources were blocked; acquire permitted current/versioned reference data.
Do not interpret historical proposals or older strategy excerpts as exact current parity.
