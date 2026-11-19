# Tutorial Island / Learning the Ropes — Implementation Plan

Status: planned, decisions confirmed, implementation not started. Branch:
`feat/tutorial-island`, based on upstream `openrune/main` (`15009ceaa`). This supersedes the
empty "boss" wording in the request: the scope here is Tutorial Island, not a boss.

Reference capture: `C:\Public\rsprox logs\tutorial island\20260821T013635-3be6815`
(rev 240.1, 21 Aug 2026, full new-account run from character creation to Lumbridge arrival).
The local cache is revision 240.2. The current OSRS wiki and its
[`Transcript:Learning the Ropes`](https://oldschool.runescape.wiki/w/Transcript:Learning_the_Ropes)
are the source of truth for text; the capture is the source of truth for packet order,
stage values, interface ids, hint arrows and reward loadout. Where the capture covers only one
dialogue branch, the wiki transcript supplies the remaining branches.

## Goal

A fresh account logs in on Tutorial Island at `(3094, 3107, 0)`, is taken through character
creation and every tutorial chapter, completes **Learning the Ropes** for 1 quest point, and
arrives in Lumbridge with the correct state, quest journal, items and bank. A player who logs out
mid-tutorial resumes at the same stage with the correct control guide and next-action hint.

## Scope

**In scope**

- New-account entry on Tutorial Island, including the native display-name, player-design and
  past-experience modals from the capture.
- The complete stage machine for `varp.tutorial` (281), `varp.tutorial_progress_overlay` (406)
  and the control-guide hints, with the exact stage values observed in the capture.
- All Tutorial Island NPC dialogue and menus: Gielinor Guide, Survival Expert, Master Chef,
  Quest Guide, Mining Instructor, Combat Instructor, Account Guide, Brother Brace, Magic
  Instructor, Ironman tutor, bankers, and the optional/repeat branches from the wiki transcript.
- Tutorial variants of Fishing, Woodcutting, Firemaking, Cooking, Mining, Smithing, melee and
  ranged combat, Prayer, Magic and banking, using the existing skill systems with Tutorial
  Island rules layered on top.
- Tutorial restrictions: skill cap at level 3, no Hitpoints experience, player cannot die in the
  rat pit, no teleports/random events/death handling on the island, and the exit item wipe.
- The `Learning the Ropes` quest: `dbrow.quest_learningtheropes` (9643), quest list entry,
  quest journal text per stage, 1 quest point, completion jingle and quest scroll.
- Leaving the island: Home Teleport, arrival at `(3232, 3232, 0)`, final loadout, 25 coins in
  the bank, run-energy restore, chat unlock, player-op restore, music and the `tutorial_end`
  clientscript.
- Handoff to the existing Lumbridge new-player content, including Adventurer Jon/Adventure Paths
  if enabled by the experience-modal choice.
- Unit/integration tests for stage transitions, item movement, bank coins, XP cap, damage floor,
  quest completion and resume-on-login.
- Live verification with the Developer Tools MCP and a fresh rsprox capture of our own server.

**Out of scope**

- Hardcore worlds, Death's Delegate and Group Ironman/Hardcore Group Ironman creation. The
  Ironman tutor is included; the other mode conversations are deferred until those systems
  exist.
- Permanent Tutorial Island "stayer" support beyond what naturally falls out of the stage
  machine.
- Full Adventure Paths task/reward implementation; the existing Lumbridge content is reused and
  only the tutorial handoff is wired.
- Rag and Bone Man III's return visit to Tutorial Island.

## Current state

**Already available**

- Cache data for the whole island: map squares, locs, NPC spawns, objects, animations, sounds,
  music rows and tutorial-specific items (`obj.shrimp`, `obj.bread`, `obj.net`, tutorial rocks,
  trees, fishing spots, etc.). The capture confirms the dungeon is ordinary map space around
  `y = 9500`, not a private instance.
- Tutorial NPCs: `npc.newbie_basics_instructor` (3308),
  `npc.newbie_survival_instructor` (8503), `npc.newbie_cook_instructor` (3305),
  `npc.newbie_quest_instructor` (3312), `npc.newbie_mining_instructor` (3311),
  `npc.newbie_combat_instructor` (3307), `npc.newbie_account_instructor` (3310),
  `npc.brother_noob` (3319), `npc.newbie_magic_instructor` (3309),
  `npc.ironman_tutor_2` (7942, with `npc.ironman_tutor_2_visible` 7941),
  `npc.noobbanker` (3318), `npc.newbiegiantrat` (3313), `npc.newbiechicken` (3316) and
  `npc.0_48_48_newbiefishing` (3317).
- The quest framework: `QuestScript`, `QuestJournalRegistry`, the `questJournal` DSL, quest
  scroll rewards, quest-point accounting and login stage resync. The cache already exposes
  `dbrow.quest_learningtheropes` (9643); the capture shows the journal title
  **Learning the Ropes** and a 1-QP completion at `varp.tutorial = 671`.
- Login/new-account plumbing: `varbit.new_player_account`, initial stats, display-name
  transmitter, `clientscript.tutorial_default_settings`, per-world-type saves and
  `SessionStateEvent.Initialize`/`Login` hooks.
- Reusable gameplay: dialogue/menu DSL, interface open/close helpers, `mesoverlay` and
  `runClientScript`, hint arrows (`MiscOutput.hintArrowNpc/Tile/Reset`), UI/inventory
  highlighting, music player, bank, ironman setup, `IronmanTutorScript`, and the existing
  Lumbridge `Adventure Paths`/Adventurer Jon content.
- The exact reference state values: the capture's varp sequence, hint values, quest journal
  lines, final inventory and bank contents are transcribed in this plan's appendix.

**Missing**

- Any Tutorial Island script: entry, stage machine, hints, creation flow, NPC dialogue, skill
  overrides, exit sequence and quest registration.
- Handlers for the native `tutorial_displayname`, `player_design` and
  `tutorial_player_experience` interfaces. The client scripts and layouts exist in the cache, but
  the server currently does not open or answer them.
- A Tutorial Island XP cap, Hitpoints lock and "cannot die" rule.
- Tutorial-specific skilling behaviour: guaranteed first shrimp, faster woodcutting/fishing,
  tutorial logs/shrimp, bronze dagger anvil flow and the rat-pit combat rules.
- The island's control-guide hint schedule and idle re-hints.
- The exit loadout, bank coins, quest completion at stage 671 and the later `1000` state.

## Data model

No new persistent state should be needed. The canonical progress is `varp.tutorial` (281), which
is also the quest stage. Use named gameval constants throughout:

| Symbol | Value | Use |
|---|---:|---|
| `varp.tutorial` | 281 | Canonical tutorial/quest stage |
| `varp.tutorial_progress_overlay` | 406 | Client progress bar (2..20 in capture) |
| `varp.tutorial_2` | 2686 | Legacy/completed accounts only; untouched by this run |
| `varbit.current_hint_arrow` | 14192 | Mirrors hint-arrow state |
| `varbit.new_player_account` | — | New-account initialisation |
| `varbit.has_displayname_transmitter` | 8119 | Chat unlock/display-name state |
| `varbit.displayname_status` | 5605 | Native display-name modal state |
| `varbit.tutorial_freestuff_check` | 9323 | Set on arrival in Lumbridge |
| `varbit.hc_first_time_leaving_tutorial_island` | 16686 | Hardcore handoff, deferred |
| `dbrow.quest_learningtheropes` | 9643 | Quest list/journal row |
| `clientscript.tutorial_overlay_hint` | 2584 | Control-guide hint selector |
| `clientscript.tutorial_progressbar_set` | 867 / proc 2263 | Progress-bar update |
| `clientscript.tutorial_end` | 2645 | Close tutorial overlay on arrival |
| `clientscript.tutorial_default_settings` | 2644 | New-account defaults, already sent |

All raw ids above are for planning only. Implementation must use the symbols, and any new symbol
goes through a content `gamevals.toml` plus `checkGamevals`.

The observed chapter/stage map (transcribed from the capture, exact sub-stages preserved):

| Chapter | `varp.tutorial` values | Completes on |
|---|---|---|
| Entry/creation | 1, 2, 3, 7 | name, appearance, experience choice, first guide talk |
| Gielinor Guide | 7, 10 | open Settings, then leave the house |
| Survival Expert | 20, 30, 40, 50, 60, 70, 80, 90, 120 | net, inventory, shrimp, skills, axe/tinderbox, logs, fire, cooked shrimp, gate |
| Master Chef | 130, 140, 150, 160, 170, 183, 200 | enter kitchen, talk, dough, bread, leave |
| Quest Guide | 200, 220, 230, 240, 250, 260 | guide talk, quest list, journal, recap, ladder |
| Mining Instructor | 260, 270, 300, 310, 320, 330, 340, 350 | tin, copper, bronze bar, hammer, dagger |
| Combat Instructor | 350, 360, 370, 390, 400, 405, 410, 420, 430, 440 | equip, sword/shield, melee rat, ranged rat |
| Bank/Account Guide | 450, 460, 470, 480, 490, 500 | bank, poll booth, account menu, guide recap |
| Brother Brace | 500, 510, 520, 525, 530, 531, 532, 540 | prayer book, prayers, bones, chapel exit |
| Magic Instructor | 540, 550, 560, 570, 580, 610, 620, 640, 650, 660, 670, 671 | magic book, runes, Wind Strike, ironman talk, completion |
| Leave island | 671, 680, 1000 | final talk, Home Teleport, arrival |

`varp.tutorial` reaching **671** is quest completion (1 QP and the quest scroll); **680** is the
final "cast Home Teleport" state; **1000** is set on arrival in Lumbridge.

## Architecture

New dedicated module:

```text
content/areas/misc/tutorial-island/
  build.gradle.kts
  src/main/kotlin/org/rsmod/content/areas/misc/tutorialisland/
    TutorialIsland.kt              (startup bindings)
    TutorialStages.kt              (stage constants, varp/varbit accessors)
    TutorialEntry.kt               (new-account spawn, creation sequence, login resume)
    TutorialCreation.kt            (display name, player design, experience modal)
    TutorialHints.kt               (control guide: overlay text, arrows, idle repeats)
    TutorialQuest.kt               (Learning the Ropes QuestScript + journal)
    TutorialSkilling.kt            (fishing, woodcutting, firemaking, cooking, mining, smithing)
    TutorialCombat.kt              (rat-pit rules, XP cap, damage floor)
    TutorialPrayerMagic.kt         (Brother Brace, Terrova, Wind Strike)
    TutorialBank.kt                (bank/poll/account guide)
    TutorialExit.kt                (completion, Home Teleport, loadout, Lumbridge arrival)
    npcs/                          (one file per instructor/dialogue tree)
  src/main/resources/gamevals.toml (only if a server-only symbol is added)
  pack/                            (only if new music/dbrow/enum data is needed)
```

The module depends on `plugin-commons`, `content.quest`, `content.other.ironman`,
`content.areas.city.lumbridge` and the skill modules it reuses. `settings.gradle.kts` discovers
content modules automatically.

If you prefer the existing monolith, the same files can live under
`content/quest/src/main/kotlin/org/rsmod/content/quest/area/tutorialisland/`; the plan keeps them
separate because this is a full area plus quest, not another single-NPC quest.

## Reuse map

- **Quest/journal:** extend `QuestScript("quest_learningtheropes", "varp.tutorial", ...)`. Verify
  the cache `endstate` is `671`; if it is `1000`, use a dedicated quest varbit and mirror it from
  the stage machine instead of writing `1000` at completion. The journal text is taken from the
  capture and the wiki transcript.
- **Dialogue:** `startDialogue`, `Dialogue.chatNpc/chatPlayer/menu/mesbox`, `ProtectedAccess`
  interface helpers. Preserve the capture's wording and `<br>` breaks where they affect layout.
- **Hints:** `runClientScript("clientscript.tutorial_overlay_hint", hint)`, `mesoverlay` strings,
  `MiscOutput.hintArrowNpc/Tile/Reset`, `varbit.current_hint_arrow`, plus the capture's
  `ui_highlight_update`/`inv_highlight_item_on` calls where they are server-driven.
- **Skills:** hook the existing Fishing/Woodcutting/Firemaking/Cooking/Mining/Smithing/Combat/
  Magic/Prayer scripts by the tutorial NPC/loc/obj names, adding tutorial-specific catch,
  success, XP and item rules rather than duplicating the skill cycles.
- **Bank:** existing bank API; seed 25 coins on first tutorial bank access.
- **Ironman:** reuse the `IronmanTutorScript` mode-setup interface and mode checks; add the
  Tutorial Island tutor's distinct dialogue and NPC binding.
- **Lumbridge:** reuse `LumbridgeAdvisors`/Adventurer Jon; only add the tutorial-arrival call,
  visibility varbit and experience-choice handoff.
- **Music:** reuse the music player/area system; play Newbie Melody on the island and Scape Cave
  in the dungeon, matching the capture's tracks.

## Implementation phases

Each phase ends in a compiling commit and the listed verification.

1. **Plan and reference extraction** — this document; extract the stage table, hint texts,
   dialogue transcript and final loadout into test fixtures/checklists.
2. **Stage model + entry** — `TutorialStages`, new-account spawn at `(3094, 3107, 0)`, initial
   `varp.tutorial = 1`, login resume, `new_player_account` handling, tests.
3. **Creation flow** — open/answer the native display-name, player-design and past-experience
   modals; set appearance, name and Adventure Paths choice; transition 2→3→7.
4. **Gielinor Guide** — settings hint, guide dialogue, door, hint arrow and 7→10→20.
5. **Survival Expert** — net, inventory view, fishing, skills view, axe/tinderbox, tree, fire,
   cooking, gate; 20→120.
6. **Master Chef** — kitchen entry, flour/water, dough, range, bread, music/emote/run hints;
   120→200.
7. **Quest Guide** — quest list, `Learning the Ropes` journal, minimap icon explanation, ladder;
   200→260.
8. **Mining Instructor** — pickaxe, tin/copper rocks, furnace, bar, hammer, anvil, bronze dagger;
   260→350.
9. **Combat Instructor** — worn/equipment stats, dagger, sword/shield, combat options, melee rat,
   shortbow/arrows, ranged rat, no-death rule; 350→440.
10. **Bank + Account Guide** — bank booth (25 coins), poll booth, account management, guide
    dialogue; 450→500.
11. **Brother Brace** — prayer book, prayer points, bones, organ, chapel exit; 500→540.
12. **Magic Instructor + completion** — magic book, runes, Wind Strike chicken, Ironman tutor,
    final dialogue, 1 QP/quest scroll at 671; 540→671.
13. **Exit + Lumbridge arrival** — stage 680, Home Teleport, inventory/worn wipe, final loadout,
    25 bank coins, `tutorial_freestuff_check`, `tutorial_end`, run energy, chat unlock, music,
    Adventurer Jon call; 671→1000.
14. **Dialogue completeness audit** — walk every branch in the wiki transcript, including repeat
    and "recap" paths, and add any missing menus.
15. **Verification and hardening** — integration tests, `checkGamevals`, a full live MCP run on a
    fresh account, and an rsprox capture diff against the reference.

## Verification

**Automated**

- Stage tests: every transition fires only from the expected predecessor; the exact capture
  sequence is reproducible; login at each stage restores the right hint/overlay.
- Item-movement tests: every grant/removal uses an inventory transaction; a full inventory
  rolls back the whole action; no item duplication; the exit wipe plus loadout is exact; bank
  seed is idempotent.
- Rules tests: XP is capped at level 3, Hitpoints gain is zero, the player cannot drop below 1
  HP in the rat pit, and the tutorial rules stop applying at stage ≥ 1000.
- Quest tests: 671 grants exactly 1 QP and one journal completion; `1000` does not grant it
  twice; journal text matches stage.
- `gradlew :or-cache:checkGamevals` for every new/changed symbol.

**Live**

- Create a fresh account by logging in with an unused username; run every chapter through the
  Developer Tools MCP (`interact_npc`, `interact_object`, `item_action`, `walk_to`, `wait_for`,
  `get_var_history`, `get_skills`, `get_inventory`, `screenshot`, `dump_interface`).
- Record the run with rsprox and diff against the reference capture: `varp.tutorial` sequence,
  `varp.tutorial_progress_overlay`, hint text/arrow targets, quest journal lines, final
  inventory/bank, arrival tile and quest completion message.
- Re-run the creation flow and the exit to confirm idempotence and no duplicate items.

## Risks / points to verify during implementation

- The native creation modals may require server handling we do not have yet (name availability,
  appearance commit, experience button). This is the largest unknown and is called out as a
  decision below.
- Quest `endstate` may be 671 or 1000; the completion path must match the cache row, not an
  assumption.
- The tutorial dungeon is ordinary map space at `y≈9500`; verify collision, locs and music load
  there.
- Tutorial-specific skilling handlers may not exist for every loc/obj id; the capture is the
  checklist for which ids actually appear.
- Idle re-hint timing is client-driven in OSRS; the server-side schedule must be tuned against
  the capture without becoming a second source of truth.
- Existing bank tutorial, random events, death office, gravestones and teleports must be
  suppressed or redirected while `varp.tutorial < 1000`.

## Decisions (confirmed)

1. **Creation flow:** implement the native display-name, player-design and past-experience modals
   now, including name availability and appearance commit.
2. **Post-tutorial:** implement the full Lumbridge arrival (loadout, bank coins, run-energy/chat
   restore) and hand off to the existing Adventurer Jon/Adventure Paths content. Do not implement
   the remaining Adventure Paths task/reward system in this change.
3. **Game modes:** implement the Ironman tutor and mode switching. Defer Hardcore/Death's Delegate
   and Group Ironman conversations until those systems exist.
4. **Hints:** match the capture's hint sequence, overlay text, arrow targets and idle re-hint
   behaviour, tuned to the server's tick rate.

Base branch is upstream `openrune/main` rather than the fork's `origin/main`, because upstream is
ahead and already contains the newer quest/travel content this branch should build on.

## Appendix — reference facts from the capture

- Login: `version 240.1`, player at `(3094, 3107, 0)`, `varp.tutorial = 1`.
- Tutorial overlay interface: `interface.tutorial_overlay` (614), progress varp 406.
- Display-name interface: `interface.tutorial_displayname` (558); player design:
  `interface.player_design` (679); experience: `interface.tutorial_player_experience` (929).
- Quest completion at tick 973: `varp.tutorial 670→671`, `varp.qp 0→1`,
  `varbit.quests_completed_count 0→1`, game message
  `Congratulations, you've completed a quest: Learning the Ropes`.
- Home Teleport at tick 1034: `varp.tutorial 680→1000`, arrival `(3232, 3232, 0)`,
  `varbit.tutorial_freestuff_check 0→1`, inventory/worn cleared, then loadout:
  `bronze_axe`, `bronze_pickaxe`, `tinderbox`, `net`, 1 `shrimp`, `bronze_dagger`,
  `bronze_sword`, `wooden_shield`, `shortbow`, 25 `bronze_arrow`, 25 `airrune`, 15 `mindrune`,
  `bucket_empty`, `pot_empty`, 1 `bread`, 6 `waterrune`, 4 `earthrune`, 2 `bodyrune`.
- Bank on first tutorial bank access: 25 `coins`.
- Adventurer Jon calls the player immediately on arrival and the existing Adventure Paths content
  takes over.
- Quest journal title: `<col=7f0000>Learning the Ropes</col>`; the journal text observed at the
  Quest Guide is reproduced verbatim in the implementation.
