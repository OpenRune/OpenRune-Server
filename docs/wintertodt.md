# Wintertodt — 1:1 OSRS implementation plan

Status: planning
Branch: `wintertodt`
Base: `main` (this repository has no `master`; `main` is the primary branch)
Target: OSRS revision 240.2, post-9 October 2024 rework, including the 16 October
2024 healing-points change and the later warm-clothing additions.

---

## 1. Goal

Implement Wintertodt to match live OSRS behaviour, not just "a skilling boss":

- the Wintertodt Camp, Doors of Dinh, and the prison as a **world-shared**
  activity (not a private instance);
- the full round loop: waiting, 60-second respawn, 3500 energy, four braziers,
  four pyromancers, cold/snow/brazier/pyromancer attacks, warmth, death;
- every player action in the prison: chop roots, fletch kindling, feed, light,
  repair, pick herbs, mix/drink potions, heal pyromancers, take crate supplies;
- exact points, XP multipliers, reward-pool maths, reward cart rolls, unique
  order, collection log, and the eight Wintertodt combat achievements;
- the stock `wint_status` HUD (interface 396) driven by the stock
  `wint_update` clientscript, plus the stock sounds, animations and projectiles
  already present in the cache.

No new custom interface is needed: interface 396 and clientscript 1421 are in the
240.2 cache and are exactly what Jagex uses.

---

## 2. Sources of truth

### Wiki

- <https://oldschool.runescape.wiki/w/Wintertodt>
- <https://oldschool.runescape.wiki/w/Wintertodt/Strategies> (damage formulas,
  energy drain, attack-frequency model)
- <https://oldschool.runescape.wiki/w/Warmth> (warmth, warm clothing, regen)
- <https://oldschool.runescape.wiki/w/Brazier> (XP, tool requirements)
- <https://oldschool.runescape.wiki/w/Pyromancer> (14 HP, help option)
- <https://oldschool.runescape.wiki/w/Transcript:Pyromancer> (dialogue lines)
- <https://oldschool.runescape.wiki/w/Rejuvenation_potion>
- <https://oldschool.runescape.wiki/w/Reward_Cart>
- <https://oldschool.runescape.wiki/w/Crate_(Wintertodt)>
- <https://oldschool.runescape.wiki/w/Bruma_roots>
- <https://oldschool.runescape.wiki/w/Sprouting_roots>
- <https://oldschool.runescape.wiki/w/Doors_of_Dinh>

The reward-cart data comes from the wiki calculator module
`Module:Wintertodt supply crate` (`?action=raw`), which is the authoritative
source for the level-interpolation rolls.

### Packet logs

- `C:\Public\rsprox logs\wintertodt.txt` — mass-world session, 1566 ticks,
  client 233.1, world 311 (Wintertodt).
- `C:\Public\rsprox logs\wintertodt (2).txt` — solo/duo session, multiple
  rounds, 721→724 subdued count.

Key facts extracted from the logs are in Appendix A. All ids were re-checked
against `.data/gamevals-binary/gamevals.dat` (240.2), and the names/ids match.

### Cache / codebase

- `.data/gamevals-binary/gamevals.dat` for NPC, loc, obj, interface, varbit,
  varp, anim, projanim, hitmark and headbar ids.
- `content/minigames/gauntlet` — instance + HUD + boss lifecycle analogue.
- `content/minigames/giants-foundry` — points, rewards, transaction-safety and
  lifecycle tests analogue.
- `content/areas/misc/motherlode-mine` — `onArea` HUD open/close and world-tick
  manager analogue.
- `content/events/shooting-stars` — world singleton, `GameLifecycle.LateCycle`,
  `LocRepository` morphs, NPC spawn/despawn, headbar analogue.
- `api/instances` is **not** used: Wintertodt is one shared prison per world.

---

## 3. Scope

In scope:

- Wintertodt Camp NPCs and the Doors of Dinh entry/peek flow.
- The prison area, invisible walls, pillar jump, crates, roots, sprouting roots,
  bank chest, deposit box, bonfire, anvil, snow storm.
- Warmth (HUD bar, damage, regeneration, food/potion restore, death at 0).
- Round state: energy, braziers, pyromancers, attacks, respawn.
- All skills/actions and their exact XP/point values.
- Reward cart, owed-reward pool, uniques, level-scaled resources, collection log.
- The eight Wintertodt combat achievements.
- Music, sounds, animations, projectiles, warning snowfall, brazier fire NPCs.

Out of scope for this branch (tracked separately if needed):

- League / Deadman / seasonal variants and league task ids.
- The historical pre-2024 supply crate item and its discontinued behaviour.
- Hiscores backend changes (the "new high score" message is in scope; the
  website hiscore is not).
- Any rebalancing that differs from live OSRS.

---

## 4. Current baseline

There is **no** existing Wintertodt code in `content/` or `api/`. The closest
patterns are:

| Pattern | Reference |
|---|---|
| World-tick manager | `ShootingStarManager.tick()` + `onEvent<GameLifecycle.LateCycle>` |
| Area HUD open/close | `MotherlodeMineScript` (`onArea`/`onAreaExit`, `ifOpenOverlay`) |
| Overlay HUD target | `component.toplevel_osrs_stretch:overlay_hud` |
| Points/reward transactions | `FoundryRewardTransactionsTest`, `GauntletRewards` |
| Object morphs | `LocRepository.change(from, "loc....", Int.MAX_VALUE)` |
| NPC transforms | `Npc.transmog(type, Int.MAX_VALUE)` |
| Headbars | `Npc.showHeadbar`, `Player.showHeadbar` |
| Stock interface scripts | `Player.runClientScript("clientscript.wint_update", ...)` |
| DB-table data | `ShootingStarsTable` / `content/events/shooting-stars/pack` |
| Combat achievements | `ca_task_wintertodt_*` varbits 12737–12744, total 12919 |

---

## 5. Architecture

### 5.1 World-shared, not instanced

Wintertodt is a single shared activity per world. One player's feeds drain the
same 3500 energy that everyone sees; other players' attacks and pyromancer
states are visible. Do **not** use `InstanceScript`.

Implement a `@Singleton WintertodtGame` owned by the world, ticked from
`GameLifecycle.LateCycle`. It holds:

- `phase`: `WAITING`, `ACTIVE`, `SUBDUED` (60s respawn), `EMPTY` (optional
  distinction; see §6);
- `energy` (0..3500);
- four `Brazier` records, each with its own `Pyromancer` record;
- `players: MutableMap<Player, WintertodtSession>`;
- `nextAttackCycle`, `respawnEndCycle`, `nextHudCycle`;
- the snow-storm loc reference and warning-effect handles.

`WintertodtSession` holds per-player runtime state that is not worth a varp:
cold-hit cooldown, fletch/feed interruption lock, current action type, next
warmth-regen accumulator, and the current round's points mirror if needed.

Basic player state goes through varps/varbits (see §7.5).

### 5.2 Tick loop

`GameLifecycle.LateCycle` every tick:

1. Sync the phase (`WAITING` → `ACTIVE` when a player enters and the round is
   ready; `ACTIVE` → `SUBDUED` at energy 0; `SUBDUED` → `WAITING` at timer 0).
2. Update the 60s respawn varbit (`100 → 0`, `-2` every 2 ticks).
3. If `ACTIVE`:
   - drain energy from each lit brazier with a healthy pyromancer;
   - regenerate energy if all four braziers are out;
   - update pyromancer resume timers;
   - run the attack scheduler;
   - apply per-player warmth regeneration and cold cooldowns;
   - update `wint_update` every 2 ticks.
4. Handle round end: XP, points→rewards, count, high score, messages, clear
   braziers/pyros, set storm idle, start 60s timer.
5. Clear point state for players who left/logged out.

All timing uses `MapClock.cycle` (1 cycle = 1 game tick = 600 ms).

### 5.3 Module layout

```
content/minigames/wintertodt/
  build.gradle.kts
  src/main/kotlin/org/rsmod/content/minigames/wintertodt/
    Wintertodt.kt                 // ids, coords, ticks, constants
    WintertodtGame.kt             // world singleton + state machine
    WintertodtSession.kt          // per-player runtime state
    WintertodtWarmth.kt           // warmth, warm clothing, food/potion, death
    WintertodtBraziers.kt         // light/fix/feed + loc morphs
    WintertodtPyromancers.kt      // spawn, HP, down/up, heal, speech
    WintertodtAttacks.kt          // scheduler + five attack types + warnings
    WintertodtWoodcutting.kt      // bruma roots
    WintertodtFletching.kt        // knife on root
    WintertodtPotions.kt          // herb+vial, drink, heal, Brew'ma
    WintertodtCrates.kt           // tool/vial crates, bank, deposit box
    WintertodtRewards.kt          // points, XP, count, PB, reward pool
    WintertodtRewardCart.kt       // cart states + rolls
    WintertodtHud.kt              // interface 396, wint_update, music, timer
    WintertodtEntry.kt            // Doors of Dinh, requirement, peek, enter/exit
    WintertodtCamp.kt             // camp NPC dialogue + Ignisia exchanges
    WintertodtCommand.kt          // ::wintertodt debug helpers
  src/main/resources/gamevals.toml
  pack/
    build.gradle.kts
    src/main/kotlin/.../pack/WintertodtPluginPack.kt
    src/main/kotlin/.../pack/WintertodtTables.kt
    src/main/resources/pack/configs/wintertodt.toml   // varps/varbits if custom
  src/test/kotlin/...            // formulas + transactions + lifecycle
```

Dependencies: `api.player`, `api.player-output`, `api.invtx`, `api.repo`,
`api.registry`, `api.random`, `api.config`, `api.death`, `api.attr`,
`api.table`/generated, `content.interfaces.collection-log`,
`content.other.consumables` (for the food event), `content.skills.woodcutting`
(for axe speed/success formula). No `api.instances`.

---

## 6. Round state machine

| State | Energy | Braziers | Storm loc | Timer | Transition |
|---|---|---|---|---|---|
| `WAITING` | 3500 | unlit | `wint_snow_storm_idle` | none | first player enters → `ACTIVE` |
| `ACTIVE` | 3500→0 | player-driven | `wint_snow_storm` | none | energy 0 → `SUBDUED` |
| `SUBDUED` | 0 | all unlit/broken reset | `wint_snow_storm_idle` | `wint_transmit_respawndelay` 100→0 (60s) | timer 0 → `WAITING` |
| `EMPTY` | current | current | current | current | optional: keep last game alive while nobody is inside |

Observed from logs:

- The 60s timer is `wint_transmit_respawndelay = 100`, decremented by **2 every
  two ticks** until 0 (`log 2`, ticks 2580–2680 = exactly 60s).
- The mass-world log starts with the timer already at 16; that is a partial
  countdown, not the normal full value.
- The HUD's own client timer is driven by the varbit (`wint_timer_init`,
  `wint_timer_resynch`, `wint_timer_countdown`); the server only has to keep the
  varbit correct.

Behaviour when the prison is empty is not fully proven by the logs. Start with
"the round keeps its current energy and state; players can re-enter and
continue", which matches the peek message "The Wintertodt has 77% energy left.
There are no players within the prison." Verify with a live client and, if
needed, another log session.

---

## 7. Data and identifiers

All ids below are confirmed against the 240.2 `gamevals.dat`; the packet-log
names match.

### 7.1 Locs

| Symbol | Id | Use |
|---|---:|---|
| `loc.wint_brazier` | 29312 | unlit brazier |
| `loc.wint_brazier_broken` | 29313 | broken brazier |
| `loc.wint_brazier_lit` | 29314 | lit brazier |
| `loc.wint_roots` | 29311 | bruma roots (Chop) |
| `loc.wint_herb_roots` | 29315 | sprouting roots (Pick) |
| `loc.wint_chest_hammer` | 29316 | hammer crate |
| `loc.wint_chest_knife` | 29317 | knife crate |
| `loc.wint_chest_axe` | 29318 | bronze axe crate |
| `loc.wint_chest_tinderbox` | 29319 | tinderbox crate |
| `loc.wint_chest_vial` | 29320 | unfinished potion crate |
| `loc.wint_bankchest` | 29321 | bank chest |
| `loc.wint_door` | 29322 | Doors of Dinh (Enter/Peek) |
| `loc.wint_door_frame` | 29323 | frame |
| `loc.wint_area_attack_icicle` | 29324 | warning centre |
| `loc.wint_area_attack_snow` | 29325 | warning 3x3 snow |
| `loc.wint_pillar_jump` | 29326 | 60 Agility shortcut |
| `loc.wint_deposit_box` | 29327 | deposit box |
| `loc.wint_snow_storm` | 29308 | active Wintertodt |
| `loc.wint_snow_storm_idle` | 29309 | dormant/quiescent Wintertodt |
| `loc.wint_bonfire` | 29300 | camp fire |
| `loc.wint_anvil` | 29310 | camp anvil |
| `loc.wint_reward_pile_empty` | 55411 | reward cart, 0 owed |
| `loc.wint_reward_pile_1` | 55412 | 1–9 owed |
| `loc.wint_reward_pile_2` | 55413 | 10–24 owed |
| `loc.wint_reward_pile_3` | 55414 | 25–49 owed |
| `loc.wint_reward_pile_4` | 55415 | 50–99 owed |
| `loc.wint_reward_pile_full` | 55416 | 100+ owed |
| `loc.wint_helix` | 29722 | magic-helix visual |

Brazier coords observed: `(1620/1638, 3997)` and `(1620/1638, 4015)`. Storm
centre observed: `(1627, 4004)`.

### 7.2 NPCs

| Symbol | Id | Use |
|---|---:|---|
| `npc.wint_wizard` | 7371 | healthy pyromancer |
| `npc.wint_wizard_down` | 7372 | incapacitated pyromancer |
| `npc.wint_fire` | 7373 | brazier flame stream (visual) |
| `npc.wint_master_pyromancer` | 7374 | Ignisia (camp) |
| `npc.wint_wcguild_rep` | 7376 | camp NPC |
| `npc.wint_shay_guard` | 7377 | camp NPC |
| `npc.wint_pisc_navigator` | 7378 | camp NPC |
| `npc.wint_soldier` | 7379 | camp NPC |
| `npc.wint_troublecat` | 7380 | camp cat |
| `npc.wint_toad` | 7381 | camp toad |
| `npc.wint_area_npc` | 7383 | attack-warning marker NPC (visual) |
| `npc.wint_master_smith` | 8626 | camp smith (multinpc 8545) |
| `npc.wint_brewma` | 13793 | Brew'ma |
| `npc.lovakengj_minecart_northern_tundras` | 12706 | camp transport |

Pyromancers are `wint_wizard`/`wint_wizard_down` and transform between the two.
`wint_area_npc` and `wint_fire` are transient visual NPCs; spawn them only if
we want exact visual parity (see §13).

### 7.3 Objects

| Symbol | Id | Use |
|---|---:|---|
| `obj.wint_bruma_root` | 20695 | root |
| `obj.wint_bruma_kindling` | 20696 | kindling |
| `obj.wint_vial` | 20697 | unfinished rejuvenation potion |
| `obj.wint_herb` | 20698 | bruma herb |
| `obj.wint_potion4` | 20699 | 4-dose |
| `obj.wint_potion3` | 20700 | 3-dose |
| `obj.wint_potion2` | 20701 | 2-dose |
| `obj.wint_potion1` | 20702 | 1-dose |
| `obj.wint_reward_box` | 20703 | reward box (old supply crate) |
| `obj.wint_burnt_page` | 20718 | burnt page |
| `obj.wint_torch` | 20720 | bruma torch |
| `obj.wint_torch_offhand` | 29777 | off-hand bruma torch |
| `obj.wint_pyro_set` | 24554 | pyromancer set exchange item |
| `obj.wint_searing_page` | 28931 | searing page |
| `obj.pyromancer_hood` | 20708 | pyromancer outfit piece (warm) |
| `obj.pyromancer_top` | 20704 | pyromancer outfit piece (warm) |
| `obj.pyromancer_bottom` | 20706 | pyromancer outfit piece (warm) |
| `obj.pyromancer_boots` | 20710 | pyromancer outfit piece (warm) |
| `obj.pyromancer_gloves` | 20712 | **warm gloves** (cache name is misleading; not a set piece) |
| `obj.tome_of_fire` | 20714 | charged tome (Ignisia exchange) |
| `obj.tome_of_fire_uncharged` | 20716 | empty tome (reward cart unique) |
| `obj.phoenixpet` | 20693 | phoenix pet |

Standard items (hammer, knife, bronze axe, tinderbox, vial, rake, etc.) use
their ordinary gameval symbols.

### 7.4 Interface, clientscripts, sprites

| Symbol | Id | Use |
|---|---:|---|
| `interface.wint_status` | 396 | HUD overlay |
| `clientscript.wint_update` | 1421 | 10-int HUD update |
| `clientscript.wint_outside_event` | 1432 | leave HUD state |
| `clientscript.wint_inside_event` | 1433 | enter HUD state |
| `clientscript.wint_layout_init` | 2383 | interface onload |
| `clientscript.wint_layout_update` | 2384 | interface onload |
| `clientscript.wint_timer_init` | 2753 | timer onload |
| `clientscript.wint_timer_resynch` | 2754 | timer |
| `clientscript.wint_timer_countdown` | 2756 | timer |
| `sprite.wint_icons:0` | 1397 | brazier broken |
| `sprite.wint_icons:1` | 1398 | brazier unlit |
| `sprite.wint_icons:2` | 1399 | brazier lit |
| `sprite.wint_icons:3` | 1400 | pyromancer down |

The `wint_update` parameter order, decoded from the stock CS2, is:

```
int0 = points
int1 = Wintertodt energy (0..3500)
int2..int5 = pyromancer 1..4 (1 = healthy, anything else = down icon)
int6..int9 = brazier 1..4 (0 = broken, 1 = unlit, 2 = lit)
```

Target overlay component from the live log:
`component.toplevel_pre_eoc:overlay_hud` (164:8). In this repo the equivalent
gameframe target is usually `component.toplevel_osrs_stretch:overlay_hud`
(see Motherlode). Prefer the `osrs_stretch` name if it resolves; otherwise use
the `pre_eoc` name. This is a Phase 1 verification item.

### 7.5 Varbits and varps

| Symbol | Id | Scope | Use |
|---|---:|---|---|
| `varbit.wint_warmth` | 11434 | temp | 0..1000, HUD reads it directly |
| `varbit.wint_reward_pool` | 11435 | perm | owed reward cart rolls (0..8000) |
| `varbit.wint_needs_warning` | 11436 | perm | show the Oct-2024 rework mesbox once |
| `varbit.wint_area_attack` | 5354 | temp | attack warning camera/state toggle |
| `varbit.wint_transmit_respawndelay` | 7980 | temp | 100→0 respawn timer |
| `varp.wint_int` | 4487 | ? | verify semantics in Phase 0; likely points/high score |
| `varp.total_wintertodt_kills` | 1528 | perm | subdued count |
| `varbit.ca_task_wintertodt_*` | 12737–12744 | perm | combat achievements |
| `varbit.ca_total_tasks_completed_wintertodt` | 12919 | perm | CA total |

If `varp.wint_int` is not the per-round points varp, use a custom persistent
varp for points or the session field; do not guess. Phase 0 dumps the varp's
default/transmit metadata and the reward-cart/score behaviour from the cache.
Custom persistent state (personal best) may use a high id such as
`varp.wintertodt_personal_best = 654xx`, following Motherlode's 65400 range.

The log shows `wint_warmth` being set to 1000 at login and resetting on entry;
`wint_reward_pool` surviving login (7 owed at login, 25 after a round) and
`wint_needs_warning` being 1 at login then cleared on entry.

### 7.6 Visual/audio ids

| Symbol | Id | Use |
|---|---:|---|
| `projanim.wint_area_attack_spot` | 1310 | snow warning projectile |
| `projanim.wint_area_attack_impact` | 1311 | snow impact |
| `projanim.wint_light_brazier_bruma` | 7174 | brazier-to-storm magic bolt |
| `anim.wint_brazier_lit` | 7321 | brazier fire animation |
| `hitmark.wintertodt_drain_lit` | 75 | standard/area/brazier hitsplat |
| `hitmark.wintertodt_drain_tint` | 76 | tinted version |
| headbar 79 | 79 | overhead warmth/drain bar (unnamed in the text gamevals) |

Sound ids observed in the logs: `2531` (brazier ambience),
`2581`, `2596`, `520`/`518`, `2735` (herb pick), `2531` loop. Resolve their
names with `ServerCacheManager.getSynth(id)` in Phase 0 and use the named
symbols in code.

---

## 8. Player flow

### 8.1 Doors of Dinh

- Op1 `Enter`:
  - require Firemaking 50; if below, show the exact live message (verify; the
    wiki only states the requirement).
  - if `wint_needs_warning == 1`, show the Oct-2024 mesbox, set the varbit to 0,
    then continue.
  - teleport to `(1628, 3979, 0)` (observed), open the HUD, set warmth 1000,
    run `wint_inside_event`, play "Ice and Fire".
  - if fewer than four warm items are equipped, send "You feel the cold wind
    strike you as you enter; perhaps you should find some warmer clothes."
- Op2 `Peek`:
  - "The Wintertodt has X% energy left. There are Y players within the prison."
    (singular/plural: "There are no players within the prison." observed).
  - X = `ceil(energy * 100 / 3500)` or the same rounding the client uses; verify.
- From inside, clicking the door exits to `(1631, 3963, 0)` and closes the HUD.
- Leaving mid-round loses all current points (clear the points varp/session).

### 8.2 Area detection

Use `onArea`/`onAreaExit` if the cache/map area covering the prison is found.
Otherwise use `onPlayerCoordsChanged` plus a rectangle:

- prison rectangle around x 1615–1646, z 3968–4020;
- safe zone: the southern entrance strip (observed first cold hit at
  `(1638, 3988)`; the herb/root alcoves and entrance are safe). Exact bounds
  are a Phase 0/1 verification item against the wiki safe-area image and a live
  walk test.

The HUD must open/close on area entry/exit and on login (if already inside).

---

## 9. Actions

### 9.1 Chop bruma roots

- `onOpLoc1("loc.wint_roots")`.
- Require an axe (any axe; infernal axe does not use charges here).
- Walk adjacent, face the roots, repeat a **3-tick** skilling timer
  (`Bruma roots` wiki: 3 game ticks).
- Each attempt: play the chop animation/sound; if the success roll passes, add
  one `obj.wint_bruma_root`, grant `0.3 * Woodcutting level` XP, message
  "You get a bruma root." The log shows "You swing your axe at the roots."
  on attempts.
- If the inventory is full: "Your inventory is too full to hold any more roots."
  and stop.
- No Beaver pet chance.
- Reuse the existing woodcutting axe-speed/success formula if practical;
  otherwise implement the same level/axe curve in `WintertodtWoodcutting`.
- Roots never deplete.

### 9.2 Fletch roots into kindling

- `onItemOnItem("obj.knife", "obj.wint_bruma_root")` (and fletching-knife
  variants).
- Base **4 ticks** per root (log 2 messages every 4 ticks: 301, 305, 309, 313),
  **3 ticks** with a fletching knife (wiki: -1 tick).
- Message "You carefully fletch the root into a bundle of kindling."
- Consume exactly one root, add one kindling, grant `0.6 * Fletching level` XP.
- Fletching continues while running (wiki; observed while moving). Do not bind
  this to a stationary `ProtectedAccess` action that cancels on walk; use a
  per-player timed action/queue that survives movement, or explicitly allow
  movement. This is a required fidelity point.
- Stop if the roots run out; do not fletch in the safe area (wiki says no
  fletching there); if attempted, send the live refusal message (verify).

### 9.3 Feed braziers

- `onOpLoc1("loc.wint_brazier_lit")`.
- One root or one kindling per action.
- Points: root **10**, kindling **25**.
- Firemaking XP: root **3× level**, kindling **3.8× level**.
- If no roots/kindling: "You have run out of bruma roots." (observed; the same
  message is used when you have neither a root nor kindling).
- Feeding is observed at roughly a 2-tick cadence in log 2 (points step every
  2 ticks during a feed burst). Confirm the exact action length live; the wiki
  does not list it.
- Standard cold attacks interrupt feeding; implement the interruption lock.

### 9.4 Light and repair

- `onOpLoc1("loc.wint_brazier")` → Light; require tinderbox or bruma torch in
  inventory or equipped.
- `onOpLoc1("loc.wint_brazier_broken")` → Fix; require hammer or Imcando hammer.
- Both are **4-tick** actions (wiki); all players who begin the animation before
  the state changes receive full XP.
- Light: `6 * Firemaking level` XP, 25 points, morph to
  `loc.wint_brazier_lit`, message "You light the brazier."
- Fix: `4 * Construction level` XP (only if the player owns a POH; otherwise no
  Construction XP), 25 points, morph to `loc.wint_brazier`, message
  "You fix the brazier."
- If the associated pyromancer is down when trying to light:
  "Heal the Pyromancer before lighting the brazier."
- If the player lacks the tool, send the live tool message (verify from a
  packet log/live client).
- State races: if another player changes the brazier during the 4 ticks, still
  award XP/points only if the action was valid at start; morph only if the
  expected state still applies.

### 9.5 Herbs, potions and Brew'ma

- `onOpLoc1("loc.wint_herb_roots")` → Pick:
  - add `obj.wint_herb`, message "You pick a bruma herb."
  - no Farming XP (2024 change).
  - if the inventory is full: "The herb shatters as it hits the floor."
    (observed) and no item is added.
  - roots never deplete; the 2024 rework added two extra spawns in the lobby.
- Combine `obj.wint_herb` + `obj.wint_vial`:
  - instant (0 ticks, or 1 tick if the live client shows a pause);
  - consume both, add `obj.wint_potion4`, message "You combine the bruma herb
    into the unfinished potion.", grant `0.1 * Herblore level` XP;
  - outside the prison: "You can only do that within the influence of the
    Bruma tree."
- Drink (`onOpHeld2` / `Drink`):
  - restore **30% warmth** (300 units);
  - consume one dose, replace with the next-lower dose item;
  - messages "You drink some of your potion." and "You have N doses of potion
    left." (N=3,2,1); verify the empty-container behaviour for the last dose.
- Brew'ma (`npc.wint_brewma`):
  - dialogue from the live transcript;
  - combines every herb+vial pair in the inventory at once ("Brew'ma quickly
    mixes two Rejuvenation potions." observed), no need for Druidic Ritual;
  - transactions must be all-or-nothing for the number of pairs she agrees to
    mix; no item loss if the inventory is full.
- Heal a pyromancer:
  - `onOpNpc1("npc.wint_wizard")`/`wint_wizard_down` → Help, or use a potion on
    the NPC;
  - a single dose fully heals 14 HP and grants **75 points**;
  - consume the dose from the potion with the **fewest doses remaining** (wiki);
  - if the pyromancer is at full health: "Pyromancer|No need! I'm fine, thanks."
    and no dose is consumed;
  - if down: transform to `wint_wizard`, resume drain after a random 1–5 s;
  - if no potion: the live refusal message (verify).

### 9.6 Crates, bank, deposit box

- `loc.wint_chest_vial`:
  - options "Take-concoction", "Take-5 concoctions", "Take-10 concoctions"
    (wiki);
  - add `obj.wint_vial` 1/5/10; if the inventory cannot hold the requested
    count, take as many as fit and stop (no item creation if none fit);
  - messages: "You take an unfinished potion from the crate." /
    "You take the unfinished potions from the crate." (observed).
- Tool crates:
  - give exactly one hammer/knife/bronze axe/tinderbox **only if the player
    does not already have the regular item** (Imcando hammer and fletching knife
    do not count as the regular item, per wiki);
  - infinite supply; no depletion.
- `loc.wint_bankchest` → normal bank (`tryOpenBank`).
- `loc.wint_deposit_box` → normal deposit box if the object exists with that
  op.
- All crate/take flows need transaction tests for full inventory and repeated
  clicks.

### 9.7 Pillar jump

- `onOpLoc1("loc.wint_pillar_jump")`: require 60 Agility, play the jump
  animation, move to the far side, grant 18 Agility XP (wiki strategy page).
  This is the between-rounds training shortcut; implement after the core loop.

### 9.8 Food

- Any food that heals at least 4 HP restores **35% warmth per bite**; two-bite
  foods restore 70% total. Exclude triangle sandwich and bottle of wine.
- Add a small `FoodConsumed` event in `ConsumeFoodScript` after the heal and
  before/after delays; Wintertodt subscribes and applies 350 warmth if the
  player is in the prison and the actual heal >= 4.
- This is a generic, testable change; do not hardcode a food list in
  Wintertodt.

---

## 10. Warmth

- Range 0..1000 (1000 = 100%); HUD reads `varbit.wint_warmth` directly.
- Set to 1000 on entering the prison and on login if inside.
- **Standard cold damage** (percent, then ×10 for warmth units):

```
1 * floor((16 - W - min(2 * B, 6)) * FM / 100)
```

  `W` = equipped warm items capped at 4; `B` = lit braziers capped at 3;
  `FM` = base Firemaking level. The log shows 6% (60 warmth) at 99 FM with
  4 warm items and 4 lit braziers. The formula as written yields 5 at 99/4/4;
  calibrate rounding against the log (Phase 4) and use the live value, likely
  `round` or a +1 quirk.
- **Area attack damage**: `3 * floor((10 - W) * FM / 100)` percent.
- **Brazier shrapnel**: `2 * floor((10 - W) * FM / 100)` percent.
- Natural regeneration: **8% per minute + 1% per warm item** (max 4), i.e.
  `(80 + 10 * W) / 100` warmth units per tick. Add regen-bracelet, Hitpoints
  cape, Rapid Heal/Heal Other/Redemption, and Phoenix necklace/ring of life
  interactions only if the relevant systems already expose hooks; otherwise
  implement the base regen and note the missing effect as a follow-up.
- Food: 350 units per qualifying bite; potion: 300 units per dose.
- At 0: instant death. Use the normal death pipeline (`queueDeath`) with an
  unsafe death cause; do not apply a safe-death mark. Hardcore status is lost
  as normal.
- Visuals: `hitmark.wintertodt_drain_lit` (75) via `Player.showHitmark`, plus
  headbar 79 filled from warmth if the cache exposes the segments. The log
  shows `[hit] id=75` and `[headbar] id=79, fill=26` for a 6% hit.
- Interruptions: standard cold damage interrupts fletching and feeding, not
  chopping; apply the post-interruption cooldown (2024 change increased it).

---

## 11. Energy, braziers and pyromancers

### 11.1 Energy

- Max **3500**; 1% = 35 energy.
- Each lit brazier whose pyromancer is healthy drains **1% every 14 ticks**
  (35 energy / 14 ticks = 2.5 energy/tick per brazier; 10 energy/tick with 4
  lit). The mass-world log shows ~20 energy per 2 ticks with 4 braziers, which
  matches.
- A downed pyromancer stops that brazier's drain. Relighting a brazier or
  healing a pyromancer adds a random **1–5 s** delay before drain resumes.
- If **all four braziers are out**, energy regenerates **1% every 35 ticks**
  (1 energy/tick) after a random initial delay. Any lit brazier stops regen.
- At 0: round ends. Morph the storm to `wint_snow_storm_idle`, clear brazier
  states, stop attacks, award the round.

### 11.2 Braziers

- Four braziers, one per corner, each with an associated pyromancer.
- State transitions:
  - `wint_brazier` (unlit) --Light--> `wint_brazier_lit`
  - `wint_brazier_lit` --light attack--> `wint_brazier` (extinguished)
  - `wint_brazier_lit` --heavy attack--> `wint_brazier_broken`
  - `wint_brazier_broken` --Fix--> `wint_brazier` (then Light again)
- Morph with `LocRepository.change(from, target, Int.MAX_VALUE)`, preserving
  shape/angle; re-find the loc after each change.
- HUD state: 0 broken, 1 unlit, 2 lit (deduced from the log: break → 0,
  fix → 1, light → 2).
- At round start: all braziers unlit; at round end: all unlit/reset.

### 11.3 Pyromancers

- Four `npc.wint_wizard` at the brazier positions; 14 HP.
- A pyromancer attack deals **7** (two hits down). Transform to
  `npc.wint_wizard_down`; the HUD pyromancer param becomes 0.
- Help/heal: one potion dose, full heal, transform back, 75 points, resume
  drain after 1–5 s.
- Downed pyromancers cannot channel; their brazier cannot be lit until healed
  ("Heal the Pyromancer before lighting the brazier.").
- Speech: "Yemalo shi cardito!" while channelling, "Light this brazier!" when
  unlit, "Fix this brazier!" when broken, "We can rest for a time." on subdue,
  and the down lines from the transcript. A small per-pyromancer speech timer
  is enough.
- Spawn the pyromancers once (at world startup or first entry); they persist
  across rounds. Do not duplicate cache-map NPCs.

---

## 12. Attacks

Five attack types (wiki):

1. **Standard cold** — targeted at a player, 10-tick personal cooldown, no
   damage in the safe zone; interrupts fletching/feeding.
2. **Area snow** — warning over a 3×3 area centred on a random eligible player
   (not adjacent to an obstacle), then impact after 3 ticks; damage everyone in
   the 3×3.
3. **Brazier heavy** — warning over a brazier, then break it; damage players
   within 1 tile.
4. **Brazier light** — warning over a brazier, then extinguish it; no damage.
5. **Pyromancer** — warning over a pyromancer, then 7 damage; no danger to
   players.

Observed warning timing: warning locs/projanim at T, impact at **T+3**
(`log 1`: 233→236, 433→436, 713→716, 793→796, 913→916, 1193→1196, 1213→1216,
1333→1336, 1453→1456). Use exactly 3 ticks.

Attack tick (derived from the packet logs — see Appendix A.5):

- The boss acts on a **5-tick global lattice**. Every boss-caused brazier state
  change, every standard cold message/hit, and every area warning projectile
  falls on a 5-tick residue within a round:
  - log 2: 74/74 non-round-end brazier boss events at `tick % 5 == 4`; 33/33
    standard cold messages at `% 5 == 3`; 45/45 `hit id=75` at `% 5 == 4`;
  - log 1 round 1: 19/19 brazier boss events at `% 5 == 3`; round 2/3: 23/23
    at `% 5 == 2`; all 10 area `wint_area_attack_spot` projectiles at
    `% 5 == 3`.
- The phase is constant within a round but can change at a round boundary
  (log 1: 3 -> 2). The standard and area timers have their own phase relative
  to the brazier timer; implement them as separate 5-tick timers (or one
  heartbeat with per-type offsets), not as one shared phase.
- **A single target cannot be hit on consecutive 5-tick waves**: each brazier
  has a 10-tick cooldown. In log 2 only brazier 4 was lit; its 75 boss events
  have a minimum gap of 10 ticks (all gaps 10, 15, 20, ...), never 5. In log 1
  different braziers are hit on consecutive waves (gap 5), but the same brazier
  is never hit within 10 ticks. Assume the same 10-tick cooldown for
  pyromancers.
- **Brazier light:heavy weight ≈ 2:1.** In log 2 (single lit brazier) there
  are 48 light and 26 heavy boss events excluding the round-end cleanup:
  64.9% light (95% CI ~54–76%), consistent with 2:1 (66.7%).
- The visible special rate falls as energy rises: per-wave visible special
  rate by 350-energy bucket is 27.3% at 0–349, 25.4% at 350–699, 20% at
  700–1049, 18.5% at 1050–1399, 20% at 1400–1749, 10.8% at 1750–2099, 14.3%
  at 2100–2449, and 0% at 2450+. This is consistent with the wiki's
  energy-gated roll, but the absolute weights cannot be read directly because
  attacks on unlit braziers/downed pyromancers leave no state change.

What cannot be derived exactly:

- Full special-type weights (area vs light vs heavy vs pyro). In log 2 only
  brazier 4 was lit and only pyromancer 4 was healthy for most of the round, so
  3/4 of brazier/pyromancer targets were invisible. Log 1 has all pyromancers
  healthy but they are healed between the 2-tick HUD samples, so it records no
  pyro-down states at all.
- The standard-vs-special split: standard attacks on other players are not
  logged, and the local player's 10-tick cooldown hides repeats.
- Area weight: only 1 visible area warning in log 2 and 10 in log 1; warnings
  are only rendered near the player.

Working model for implementation:

```
every 5 ticks, per subsystem timer:
  standard: if roll(0..3500) < energy and player off 10-tick cooldown -> hit
  special:  if roll(0..3500) > energy -> pick a target whose 10-tick cooldown
            is ready; brazier type light:heavy = 2:1
```

Do not add a random `ATTACK_MIN_TICKS..ATTACK_MAX_TICKS` interval; the 5-tick
heartbeat and per-target `lastHitCycle` reproduce the observed spacing. The
remaining pyro/area weights need a cleaner log (see §19).

Other attack rules:

- Area attacks cannot target a player standing next to an obstacle; choose a
  valid centre with a free 3×3.
- Standard cold has a per-player 10-tick cooldown, so the same player cannot be
  hit more than once every 10 ticks.
- Damage to warmth uses §10 formulas; use `Player.showHitmark` (hitmark 75/76)
  and the headbar, not HP hits.
- Set `varbit.wint_area_attack` 0→1 at the warning and 1→0 at impact (camera
  shake/visual state).
- Use `projanim.wint_area_attack_spot` and the warning locs
  (`wint_area_attack_icicle` centre + four `wint_area_attack_snow` around it);
  use `projanim.wint_area_attack_impact` at impact.
- A standard hit while fletching/feeding cancels the action and applies the
  interruption lock; a hit while chopping does not cancel chopping.

---

## 13. HUD, audio and visual fidelity

### 13.1 HUD

- Open `interface.wint_status` as an overlay on
  `component.toplevel_osrs_stretch:overlay_hud` (or `pre_eoc` if required).
- On enter: `runClientScript("clientscript.wint_inside_event")`.
- On exit: `runClientScript("clientscript.wint_outside_event")`, close overlay.
- Every 2 ticks: `runClientScript("clientscript.wint_update", points, energy,
  pyro1, pyro2, pyro3, pyro4, brazier1, brazier2, brazier3, brazier4)`.
- `varbit.wint_warmth` and `varbit.wint_transmit_respawndelay` are read directly
  by the interface; set them as they change.
- The stock interface's onload scripts handle layout/timer; do not reimplement
  the HUD in a custom interface.

### 13.2 Music

- Camp: "The Doors of Dinh".
- Prison: "Ice and Fire".
- Use the repo's music system / `onArea` pattern, not a raw client script.

### 13.3 Visual streams

- `npc.wint_fire` streams from each lit brazier toward the storm; the log shows
  one spawn every 2 ticks from the brazier to the centre (e.g. (1639,3998) →
  (1627,4004)). This is optional polish; implement with a short-lived NPC that
  walks/teleports along the path, or use the `wint_light_brazier_bruma`
  projectile.
- `npc.wint_area_npc` appears at warning positions; use it only if the snow locs
  alone look wrong in the live client.
- Brazier fire animation `anim.wint_brazier_lit` (7321) should play on lit
  braziers; the loc morph may handle it automatically.
- The storm loc morphs `wint_snow_storm` ↔ `wint_snow_storm_idle` with energy
  and round state.
- Sounds: brazier ambience 2531, fletch/chop/light/heal sounds resolved from the
  log ids in Phase 0.

---

## 14. Points, XP, rewards

### 14.1 Points

| Action | Points |
|---|---:|
| Light a brazier | 25 |
| Add a bruma root | 10 |
| Add bruma kindling | 25 |
| Repair a broken brazier | 25 |
| Heal a pyromancer | 75 |

- At 500 points: "You have helped enough to earn a supply crate." (exact live
  message retained even though the crate is now a reward cart; observed).
- Points are per round, reset on entry, and lost on leaving/logout before the
  round ends.

### 14.2 XP

All multipliers are × the player's **base** level, unless noted:

| Action | Skill | Multiplier |
|---|---|---:|
| Light a brazier | Firemaking | 6× |
| Cut a bruma root | Woodcutting | 0.3× |
| Feed a bruma root | Firemaking | 3× |
| Fletch a root | Fletching | 0.6× |
| Feed bruma kindling | Firemaking | 3.8× |
| Fix a broken brazier | Construction | 4× (only with a POH) |
| Create a rejuvenation potion | Herblore | 0.1× |
| Subdue with 500+ points | Firemaking | 100× |

Log 2 at 99 FM: "You have gained 9900 Firemaking XP." (100 × 99).

### 14.3 Round end

- Only players inside the prison when the round ends receive rewards.
- If points >= 500:
  - grant `100 × Firemaking level` XP;
  - rewards `= 1 + floor(points / 500)` guaranteed, plus a
    `(points % 500) / 500` chance of one more;
  - add to `varbit.wint_reward_pool` (cap 8000);
  - message "You're owed an additional N rewards from the reward cart. You're
    now owed M rewards." (observed);
  - increment `varp.total_wintertodt_kills`, message "Your subdued Wintertodt
    count is: <col=ff0000>N</col>." (observed);
  - if points > personal best, update PB and message "You have a new high
    score! N" (observed).
- If points < 500: "You did not earn enough points to be worthy of a gift from
  the citizens of Kourend this time." (wiki).
- Clear points, warmth reset, close/reset brazier and pyromancer states, start
  the 60s timer.

### 14.4 Reward cart

- Object morphs based on `wint_reward_pool`: 0 → `wint_reward_pile_empty`,
  1–9 → `_1`, 10–24 → `_2`, 25–49 → `_3`, 50–99 → `_4`, 100+ → `_full`.
- Options: `Search`, `Check`, `Big-search`.
  - Search: one reward per **3 ticks** (wiki).
  - Big-search: five rewards per click (wiki).
  - Check: shows the current owed count (exact live text to verify).
- A search rolls the reward at claim time (not when the pool is earned) and
  uses the player's current levels.
- Roll order (per reward):

| Order | Table | Chance |
|---:|---|---:|
| 1 | Phoenix | 1/5,000 |
| 2 | Dragon axe | 1/10,000 |
| 3 | Tome of Fire (empty) | 1/1,000 |
| 4 | Warm gloves | 1/150 (or magic seed if 3+ pairs) |
| 5 | Bruma torch | 1/150 (or torstol seeds if 3+ torches) |
| 6 | Pyromancer outfit piece | 1/150 (piece with the fewest owned; tie order garb→hood→robe→boots) |
| 7 | Burnt page 7–29 | 1/45 |
| 8 | Resource table | remainder |

Resource table: coins 5/25, dynamite 1/25, saltpetre 1/25, and 3/25 each for
logs, gems, ores, herbs, seeds, fish. The exact per-item level interpolation is
the wiki module formula:

```
interpolate(level, lowRoll, highRoll) =
    clamp(floor(lowRoll + (highRoll - lowRoll) * (level - 1) / 98) + 1, 0, 256)

chance(item) = remaining * (interpolate / 256)
remaining -= chance(item)
```

with the ordered item lists and quantities from `Module:Wintertodt supply
crate`. Transcribe those rows into a server-only dbtable; do not hardcode a
`mapOf` in Kotlin.

- Transaction safety:
  - decrement the pool only after the item(s) are successfully added;
  - if the inventory is full, stop and leave the pool untouched;
  - handle stack overflow at `Int.MAX_VALUE` by rolling back the whole reward;
  - handle the item vanishing between check and give (same tick) by re-checking
    and rolling back;
  - Big-search must not partially consume a pool if none of the five can fit.
- Collection log: grant each unique on receipt; the Wintertodt category exists
  (`collection_log_category_wintertodt=60070`, category struct from cache). The
  10 items are the standard collection-log set (dragon axe, phoenix,
  pyromancer garb/robe/hood/boots, warm gloves, tome of fire, burnt page,
  bruma torch).
- Extra supply crate exchanges with Ignisia: implement the 2024-era exchanges
  (unique items → extra supply crate or 50 burnt pages; tome → 250 pages;
  3+ gloves/torches → magic/torstol seeds in the cart). Verify the exact
  dialogue and item ids in Phase 0.

### 14.5 Combat achievements

Wire the eight existing varbits:

| Varbit | Task |
|---|---|
| 12737 | Wintertodt Novice — subdue 5 times |
| 12738 | Wintertodt Champion — subdue 10 times |
| 12739 | Handyman — repair a destroyed brazier |
| 12740 | Mummy! — heal a downed pyromancer |
| 12741 | Leaving No One Behind — no pyromancer falls in a round |
| 12742 | Can We Fix It? — never all 4 braziers broken at once |
| 12743 | Cosy — subdue with four warm items |
| 12744 | Why Fletch? — subdue with 3000+ points |
| 12919 | total Wintertodt tasks completed |

Confirm which perfection varbit is which by reading the CA definitions; the
mapping above is inferred from the names and should be verified in Phase 0.

---

## 15. Camp NPCs and reward cart

- `npc.wint_master_pyromancer` (Ignisia) at (1634, 3948):
  - exchange uniques for extra supply crates or burnt pages (wiki);
  - full dialogue/transcript in Phase 5.
- Camp flavour NPCs: `wint_master_smith` (Undor?), `wint_wcguild_rep`,
  `wint_shay_guard`, `wint_pisc_navigator`, `wint_soldier`, `wint_troublecat`,
  `wint_toad` — dialogue polish, not required for the core loop.
- `wint_bankchest` at the camp: bank chest.
- Reward cart: sync the correct morph on login, after every pool change, and
  after a search.
- `wint_bonfire`, `wint_anvil`, `wint_tent` are scenery; only implement if the
  cache objects have ops we need.

---

## 16. Edge cases and failure modes

- **Leaving/logging out mid-round**: all current points are lost; do not grant
  rewards; HUD closes; pool unchanged.
- **Death at 0 warmth**: normal unsafe death; Hardcore loses status; items go
  to the grave per normal rules. Do not mark safe death.
- **Player enters mid-round**: join the existing state, set warmth 1000, show
  the current energy/players via peek if they ask; do not reset braziers.
- **Empty prison**: keep state as described in §6; verify.
- **Same-tick double clicks**: every action must be idempotent/guarded; two
  feeds must consume exactly two items and add exactly two points; two lights
  must not duplicate the morph or points.
- **Inventory full mid-action**: the action either completes without the item
  (with the live message) or is refused; never create items out of nowhere.
- **Brazier state race**: two players light/repair the same brazier; award each
  player who began a valid animation, but morph once.
- **Pyromancer state race**: two players heal the same downed pyromancer; one
  dose each is consumed; do not heal twice for one dose.
- **Reward cart full inventory / stack overflow**: roll back and keep the pool.
- **Round ends while a player is fletching/feeding**: cancel the action cleanly,
  award the points earned, no item loss.
- **Server restart**: reward pool, needs-warning, kills and PB are persistent
  varps/varbits; the round state resets to `WAITING` (energy full, braziers
  unlit).
- **World hop / logout**: same as leaving; no cross-world state.

---

## 17. Testing plan

### Unit tests (pure)

- Warmth damage formulas for FM 1/50/99, W 0..4, B 0..4; assert the observed
  6% case and document the rounding decision.
- Energy drain: 1/14 per brazier, 4 braziers = 10/tick; regen 1/35 with all out.
- Points→rewards: 500→2 guaranteed, 505→2 + 5%, 750→2 + 50%, 1000→3,
  1200→3 + 40%, 8530→18 + 6%.
- Reward interpolation and ordered-subtable rolls, including the "remaining"
  chain and the 256 clamp.
- Attack scheduler distribution over many seeds at 100%, 50%, 10% energy.
- Safe-zone rectangle and warm-item counting.

### Transaction tests (item movement)

- Crate take 1/5/10 with full inventory, exact space, and repeated clicks.
- Tool crate gives one only when the regular tool is absent; Imcando/fletching
  knife do not block it.
- Reward cart search with full inventory, stack overflow at `Int.MAX_VALUE`,
  item disappearing mid-check, and Big-search partial-space.
- Potion combine: herb/vial consumed exactly once; inventory full; outside
  prison refusal.
- Drink: 4→3→2→1→empty, 300 warmth per dose, no duplication.
- Heal pyromancer: lowest-dose potion chosen; full-health no-consume; downed
  transform; 75 points.
- Feed: root/kindling consumed exactly once; no roots message; points/XP.
- Fletch: root consumed exactly once; knife required; interruption.
- Reward pool cap at 8000; pool unchanged on failed search.

Use the `FoundryRewardTransactionsTest` fixture pattern (mock `GameRandom`,
`RecordingClient`, `ProtectedAccess`, `ServerCacheManager.init(241)`), adapted
for the Wintertodt tables.

### Integration/lifecycle tests

- Round start/end, 60s timer, energy 0, storm morph, brazier/pyro reset.
- Player joins mid-round; leaves; logs out; points state.
- HUD opens/closes on area entry/exit; `wint_update` params match the model.
- Persistence of reward pool/needs-warning/kills/PB.

### Live client (OpenRune Developer Tools MCP)

Use the dashboard at `http://127.0.0.1:7780/` while running the server.

1. `::tele` / `walk_to` to the camp; `interact_object` Doors of Dinh; verify
   Firemaking gate, warning mesbox, teleport, HUD (`screenshot`/`dump_interface`
   396), music.
2. `interact_object` roots → `wait_for` "You get a bruma root."; verify
   inventory and WC XP.
3. Knife on root → kindling; verify 4-tick/3-tick cadence with
   `get_effect_history`/timestamps.
4. Light/fix/feed a brazier; `get_var_history` for `wint_warmth`,
   `wint_transmit_respawndelay`, and points via the HUD; `screenshot` the
   brazier morph.
5. Force each attack with a debug command; `get_projectile_history` and
   `get_effect_history` for warning/impact; verify 3 ticks and damage values.
6. Eat food / drink potion; verify +350/+300 warmth.
7. Let warmth reach 0 in a safe test; verify death, grave, Hardcore status.
8. Complete a round; verify XP, points→rewards, count, high score, reward pool,
   cart morph, collection log.
9. Search/Big-search the cart; compare item counts before/after; full inventory
   and overflow tests.
10. Compare a solo round against `wintertodt (2).txt` event counts and the mass
    round against `wintertodt.txt` (Appendix A). Tune the attack scheduler.

### Log replay/regression

Add a small parsing script (or test-only Kotlin) that reads the two logs and
asserts the constants and messages used by the implementation. Do not add the
raw logs to the repo; keep a fixture summary in the test.

---

## 18. Implementation phases

### Phase 0 — verification and skeleton

- Dump the 240.2 cache for all Wintertodt locs/npcs/objs: ops, sizes, anims,
  params, varbit/varp metadata, interface 396 components, clientscript 1421
  params, headbar 79, hitmark 75/76, synth names.
- Resolve every `?` in §7 (warm gloves id, `wint_int` semantics, safe zone,
  last-dose container, Ignisia exchange ids).
- Create the module skeleton and `gamevals.toml` symbols.
- Acceptance: `gradlew :content:minigames:wintertodt:build` compiles; a test
  can resolve every symbol used by the plan.

### Phase 1 — camp, entry, HUD, warmth shell

- Doors of Dinh enter/peek/exit, FM 50 gate, warning mesbox, warm clothing
  message.
- `wint_status` overlay, `wint_inside_event`/`wint_outside_event`,
  `wint_update` every 2 ticks (static values first), music.
- Warmth varbit, HUD, login init, safe-zone detection.
- Debug `::wintertodt` command (enter, set energy, set warmth, force attack).
- Acceptance: enter/leave works in the live client; HUD matches the stock
  interface; warmth bar reads 100%; peek message matches.

### Phase 2 — braziers, pyromancers, energy, round loop

- Spawn pyromancers, storm loc, brazier state machine.
- Light/fix/feed actions, points/XP, messages.
- Energy drain/regen, 60s timer, round end (stub rewards).
- HUD params for energy/braziers/pyros.
- Acceptance: a round can be started, braziers lit, energy drains at the
  observed rate, storm goes idle at 0, 60s timer counts down, new round starts.

### Phase 3 — skills and potions

- Bruma roots chopping, fletching, crates, herbs, potion combine/drink,
  Brew'ma, pyromancer heal.
- Food warmth event.
- Acceptance: every action produces the exact item/XP/point/message; all
  transaction tests pass.

### Phase 4 — attacks and death

- Attack scheduler, five attacks, warnings, interruption, warmth damage,
  hitmarks/headbar, death at 0.
- Tune against both logs.
- Acceptance: forced attacks match warning 3 ticks and damage formulas; live
  cold/warmth/death works; event counts are within the log envelope.

### Phase 5 — rewards, cart, camp

- Points→pool→cart, unique/resource rolls, collection log, PB/count/high score.
- Reward cart morphs/search/check/big-search, Ignisia exchanges, bank chest.
- Acceptance: a full round grants the exact pool; cart search is transaction
  safe; collection log entries appear.

### Phase 6 — CAs, audio/visual polish

- Eight CA varbits, total completion.
- Brazier flame NPC stream, warning NPCs, proper sounds, storm/green bolts.
- Camp NPC dialogue polish.
- Acceptance: all eight CAs can be completed in a controlled test; the live
  scene matches the reference screenshots.

### Phase 7 — hardening and verification

- Full regression: unit + transaction + integration + live client.
- Compare against both packet logs; document deviations in `docs/quirks.md` if
  any are intentional.
- Performance sanity (4 pyros + attack effects + HUD at 50 players).
- Acceptance: definition of done below.

---

## 19. Open questions (resolve before/while coding)

1. Special weights beyond brazier light:heavy. The 5-tick global heartbeat and
   the 10-tick per-target cooldown are now derived from the logs (Appendix
   A.5); brazier light:heavy is ~2:1. Full special-type weights are still
   censored because most targets were unlit/downed or off-screen. Collect a
   solo log with all four braziers lit and all four pyromancers healthy for a
   full round, or use server-side/deob data, to pin the pyro/area weights.
2. Exact standard-damage rounding (wiki says floor; the log shows 6 at 99 FM
   with 4 warm/4 lit, where floor gives 5). Determine whether the live formula
   rounds or has an off-by-one.
3. `varp.wint_int` semantics (points vs. personal best). Dump its default and
   transmit metadata, and compare against the log's score messages.
4. Warm clothing source: cache item tag/param vs. a server dbtable. If the cache
   has a warm tag, use it; otherwise encode the wiki list in a dbtable.
5. Exact safe-zone rectangle (south strip + side alcoves).
6. Empty-prison behaviour (pause vs. continue; does energy regen when no one is
   inside?).
7. Last-dose potion container (vial vs. nothing) and the exact empty message.
8. Headbar 79 fill/segments and whether it should be shown on every cold hit.
9. Exact tool-refusal/no-potion/leave-round messages not present in the logs.
10. Reward cart "Check" text and the exact Big-search cadence.
11. Ignisia exchange ids/dialogue for the 2024-era unique exchanges.
12. Whether the first round after server start starts immediately on entry or
    waits for a 60s timer.

---

## 20. Definition of done

- [ ] Branch `wintertodt` builds cleanly and `gradlew test` passes.
- [ ] No hardcoded numeric content ids; every id is a gameval symbol.
- [ ] No hardcoded reward/level tables in Kotlin; data lives in a dbtable.
- [ ] Entering with <50 FM is refused; entering with <4 warm items warns.
- [ ] HUD 396 shows points/energy/warmth/4 braziers/4 pyromancers exactly as
      the stock interface does.
- [ ] Energy drains 1%/14 ticks per lit brazier with a healthy pyromancer;
      regen 1%/35 ticks when all are out; 60s respawn.
- [ ] All five attacks work, warn for 3 ticks, and apply the correct warmth
      damage; standard attacks interrupt fletching/feeding but not chopping.
- [ ] Warmth 0 kills; food/potion restore the correct amount; warm clothing
      reduces damage.
- [ ] Every action has the exact points/XP/message from the wiki/logs.
- [ ] Reward pool, cart morph, search/big-search, uniques, level-scaled
      resources and collection log are exact and transaction-safe.
- [ ] Eight CAs and the total-completed varbit wire correctly.
- [ ] Leaving/logout loses points; mid-round join and empty-prison behaviour
      match live.
- [ ] Live-client verification recorded (screenshots/dumps) for the HUD, each
      attack, a full round, and a cart search.
- [ ] `docs/wintertodt.md` updated from plan to implementation notes.

---

## Appendix A — facts extracted from the packet logs

### A.1 `wint_update` decode (from the stock CS2 `[clientscript,wint_update]`)

```text
int0 = points
int1 = Wintertodt energy (0..3500)
int2..int5 = pyromancer 1..4 (1 = healthy, else down icon)
int6..int9 = brazier 1..4 (0 = broken, 1 = unlit, 2 = lit)
```

Examples observed:

- Round start: `[0, 3500, 1,1,1,1, 1,1,1,1]` (all unlit).
- After lighting two: `[0, 3500, 1,1,1,1, 2,2,1,2]` then all 2.
- Broken brazier: `[350, 520, 1,1,1,1, 0,2,2,2]`.
- Solo round end: `[8530, 5, 0,0,0,1, 1,0,1,2]` then `[8530, 0, ...]`.

The script sends `wint_update` every 2 ticks.

### A.2 Energy and timing

- Max energy 3500; 1% = 35.
- 4 lit braziers: ~20 energy per 2 ticks = 10/tick; matches 1%/14 ticks each.
- Respawn timer 100→0, `-2` every 2 ticks = 60s (`log 2`, 2580–2680).
- `wint_warmth` set to 1000 on login; standard hits drop it by 60 (6%) at 99 FM
  with 4 warm items and 4 lit braziers.
- Area warning to impact: 3 ticks.
- Pyromancer down/up: `wint_wizard` (7371) ↔ `wint_wizard_down` (7372).
- Brazier states: 29312 unlit → 29314 lit → 29313 broken → 29312 fixed →
  29314 lit.

### A.3 Points/XP/messages observed

- "You light the brazier." (+25)
- "You fix the brazier." (+25)
- "You carefully fletch the root into a bundle of kindling."
- "You get a bruma root."
- "You swing your axe at the roots."
- "You pick a bruma herb."
- "You combine the bruma herb into the unfinished potion."
- "You take the unfinished potions from the crate."
- "You take an unfinished potion from the crate."
- "You drink some of your potion." / "You have N doses of potion left."
- "You have run out of bruma roots."
- "Your inventory is too full to hold any more roots."
- "The cold of the Wintertodt seeps into your bones."
- "The brazier is broken and shrapnel damages you."
- "Heal the Pyromancer before lighting the brazier."
- "The brazier has gone out."
- "You have helped enough to earn a supply crate."
- "You have run out of bruma roots."
- "The herb shatters as it hits the floor."
- "Pyromancer|No need! I'm fine, thanks."
- "You have gained 9900 Firemaking XP."
- "Your subdued Wintertodt count is: <col=ff0000>N</col>."
- "You have a new high score! N"
- "You're owed an additional N rewards from the reward cart. You're now owed M rewards."
- "The Wintertodt has 77% energy left. There are no players within the prison."
- "Please be aware that the Wintertodt has been modified since your last
  encounter; for full details, please visit our website for the news article
  dated October 2024, or consult the Old School Runescape Wiki."
- "You feel the cold wind strike you as you enter; perhaps you should find some
  warmer clothes." (wiki, not in the logs because the player wore four pieces)

### A.4 Map/coords

- Doors of Dinh: `(1627, 3964, 0)`.
- Prison entry teleport: `(1628, 3979, 0)`.
- Storm: `(1627, 4004, 0)`.
- Braziers: `(1620, 3997)`, `(1638, 3997)`, `(1620, 4015)`, `(1638, 4015)`.
- South pyromancers: `(1619, 3996)`, `(1641, 3996)`; north equivalents
  `(1619, 4014)`, `(1641, 4014)` (inferred by symmetry; verify).
- Brew'ma: `(1635, 3986, 0)`.
- Camp: NPCs around `(1623–1634, 3935–3953)`; reward cart/bank east of the
  camp around `(1632, 3942)`.

### A.5 Attack-grid derivation

The `wint_update` stream gives the global state of all four braziers and all
four pyromancers every 2 ticks. Cross-checking that against the 1-tick loc
changes gives a complete boss-action timeline.

**5-tick heartbeat.** Within each round every observed attack effect is
congruent modulo 5:

| Log / round | Event type | Count | `tick % 5` |
|---|---|---:|---:|
| log 2 (single round) | boss brazier events (excl. round-end) | 74 | 4 |
| log 2 | standard cold messages | 33 | 3 |
| log 2 | `hit id=75` (all) | 45 | 4 |
| log 1 round 1 | boss brazier events | 19 | 3 |
| log 1 round 2/3 | boss brazier events | 23 | 2 |
| log 1 all rounds | area `wint_area_attack_spot` projectiles | 10 | 3 |

The phase changes only at a round boundary (log 1: 3 -> 2), so this is a
per-round 5-tick scheduler, not a random interval. The standard and area
timers have their own phase relative to the brazier timer.

**10-tick per-target cooldown.** In log 2 only brazier 4 was lit. Its 75 boss
events have gaps `10, 15, 20, 25, ...`; the minimum is 10 ticks, never 5. In
log 1 different braziers can be hit on consecutive 5-tick waves (gap 5), but
the same brazier never takes two boss attacks within 10 ticks (excluding the
round-end cleanup where all braziers are extinguished together). Treat each
brazier (and, by extension, each pyromancer) as having a 10-tick
`lastHitCycle` gate.

**Brazier light:heavy.** Log 2, excluding the round-end cleanup: 48 light,
26 heavy = 64.9% light (95% CI ~54–76%), i.e. consistent with a **2:1**
light:heavy weight.

**Energy dependence.** Visible specials per 5-tick wave by 350-energy bucket:

| Energy | Waves | Visible special waves | Rate |
|---|---:|---:|---:|
| 0–349 | 99 | 27 | 27.3% |
| 350–699 | 81 | 16 | 19.8% |
| 700–1049 | 57 | 10 | 17.5% |
| 1050–1399 | 69 | 12 | 17.4% |
| 1400–1749 | 55 | 9 | 16.4% |
| 1750–2099 | 41 | 5 | 12.2% |
| 2100–2449 | 50 | 6 | 12.0% |
| 2450–2799 | 39 | 1 | 2.6% |
| 2800–3149 | 20 | 0 | 0% |

This is a **censored lower bound**: log 2 had only brazier 4 lit and only
pyromancer 4 healthy for most of the round, so attacks on the other six
targets leave no state change. It is consistent with the wiki's energy-gated
roll but does not pin the absolute weights.

**Why full weights are not derivable from these logs.** The boss can target
unlit braziers and downed pyromancers, and those attacks are invisible in the
client state stream. Log 1 had all pyromancers healthy but they were healed
between the 2-tick HUD samples, so it records **zero** pyro-down states; log 2
had only one healthy pyromancer. Area warnings are only rendered near the
player (1 visible in log 2, 10 in log 1). Standard attacks on other players are
not logged. A clean measurement needs a solo round with all four braziers lit,
all four pyromancers healthy, and the player outside the safe zone for the
whole round.

**Working model.** 5-tick heartbeat per subsystem, 10-tick per-target
cooldown, brazier light:heavy = 2:1. A 2:1:1 light:heavy:pyro split is
consistent with the low-energy data (22 brazier-4 attacks vs 7 pyro-4 attacks
at 0–349 energy ≈ 3:1, which is what 2:1:1 predicts when brazier light+heavy
are counted together), but that is an inference, not a proof. The area weight
remains unmeasured.

---

## Appendix B — reward cart data source

Use the wiki module raw source:

```
https://oldschool.runescape.wiki/w/Module:Wintertodt_supply_crate?action=raw
```

It contains the ordered resource lists for `crafting`, `herblore`, `farming`,
`woodcutting`, `mining`, `fishing`, `other`, plus the interpolation function and
the unique roll order. Transcribe it into a server-only dbtable with columns:

```
skill (string/enum), item (obj), minQty, maxQty, lowRoll, highRoll, order
```

and a small unique table:

```
order, item, denominator, condition
```

Do not copy the module's price/lookup logic; only the roll data.

The unique effective rates and the 3/25 resource-category chance are on
`https://oldschool.runescape.wiki/w/Reward_Cart`.

---

## Appendix C — references in this repo

- `content/minigames/gauntlet/src/main/kotlin/.../GauntletLifecycle.kt`
- `content/minigames/gauntlet/src/main/kotlin/.../GauntletRewards.kt`
- `content/minigames/giants-foundry/src/test/kotlin/.../FoundryRewardTransactionsTest.kt`
- `content/areas/misc/motherlode-mine/src/main/kotlin/.../MotherlodeMineScript.kt`
- `content/events/shooting-stars/src/main/kotlin/.../ShootingStarManager.kt`
- `content/events/shooting-stars/pack/src/main/kotlin/.../ShootingStarsTable.kt`
- `api/player/src/main/kotlin/org/rsmod/api/player/ui/PlayerInterfaceExtensions.kt`
- `api/player-output/src/main/kotlin/org/rsmod/api/player/output/ClientScripts.kt`
- `api/player/src/main/kotlin/org/rsmod/api/player/hit/PlayerExtensions.kt`
- `api/repo/src/main/kotlin/org/rsmod/api/repo/loc/LocRepository.kt`
- `api/game-process/src/main/kotlin/org/rsmod/api/game/process/GameLifecycle.kt`

---

## Appendix D — proposed first commits

1. `docs: add Wintertodt 1:1 implementation plan` (this document).
2. `feat(wintertodt): module skeleton and cache id constants`
3. `feat(wintertodt): doors, entry, HUD and warmth`
4. `feat(wintertodt): braziers, pyromancers, energy and round loop`
5. `feat(wintertodt): roots, fletching, potions and crates`
6. `feat(wintertodt): attacks, warmth damage and death`
7. `feat(wintertodt): rewards, reward cart and collection log`
8. `feat(wintertodt): combat achievements and polish`
9. `test(wintertodt): formulas, transactions and lifecycle`
