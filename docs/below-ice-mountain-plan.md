# Below Ice Mountain — implementation plan

Status: work plan, not implemented.

Sources of truth, in order:

1. `.data/cache/LIVE` + `.data/cache/SERVER` (revision 240.2): quest row, varbits, NPCs,
   locs, items, music, interfaces.
2. `osrs-dumps/dump.npc` for NPC definitions/transforms.
3. The wiki walkthrough/transcript/reward pages.
4. Packet log `D:\Runescape\sniffs\f2p\20260817T183327-59bcb26.txt` (revision 240.1,
   F2P world 435, one full playthrough). The log is the reference for timing, dialogue
   branches, camera, NPC paths, sounds and music. It predates the 3 November 2026
   reward change.

Decisions already taken:

- Use **1,200 Mining XP** (current cache `stat_xp_awarded = mining 12000` in tenths,
  plus the 3 November 2026 wiki update). The old sniff scroll has no XP line.
- Reproduce the **full 1:1 cutscenes**: private-region workout montage, entrance
  sabotage and boss reveal.
- Scope stops at **quest completion + Ramarno's entrance/workshop intro**. Barronite
  mining, Sacred Forge, vault, golems and fishing are a separate task.
- Shared boss: when the Ancient Guardian dies, **every player in the chamber at stage
  35 advances**. Nobody soft-locks if someone else lands the kill.
- Replace/centralize the existing BIM stubs; keep unrelated Edgeville content.

---

## 1. Quest data from the cache

`QuestRow.getRow("dbrow.quest_belowicemountain")`:

| Field | Value |
|---|---|
| row id / quest id | 6 / 162 |
| display name | Below Ice Mountain |
| quest points | 1 |
| end state (`bim` max) | 120 |
| requirement | 16 quest points |
| start coord / npc | 3003, 3435, 0 / `npc.bim_willow_outside` (10638) |
| map element | 704 |
| stat XP awarded | mining 12000 (tenths) = **1,200 XP** |

Varbits (parent varp `varp.bim_main`, id 2951, except workout):

| Varbit | id | bits | Values used |
|---|---|---|---|
| `varbit.bim` | 12063 | 0–6 | 0, 5, 7, 10, 15, 20, 25, 30, 35, 40, 120 |
| `varbit.bim_marley` | 12064 | 7–12 | 0, 5, 10, 35, 40, 45, 50 |
| `varbit.bim_checkal` | 12065 | 13–18 | 0, 5, 10, 15, 20, 40, 45, 50 |
| `varbit.bim_burntof` | 12066 | 19–24 | 0, 5, 10, 15, 40, 45, 50 |
| `varbit.bim_claimed_reward` | 12067 | 25 | 0/1 (bag + completion) |
| `varbit.camdozaal_ramarno_intro` | 12068 | 26–27 | 0 entrance, 1 departed, 2 workshop |
| `varbit.bim_workout_counter` | 12131 | 0–15 on `varp.bim_extra` (2964) | paid repeats |
| `varbit.emote_flex` | 12062 | 20 on varp 313 | unlock |

Quest registration:

```kotlin
QuestScript(
    questKey = "quest_belowicemountain",
    questVarp = "varp.bim_main",
    questVarbit = "varbit.bim",
    completedQuestItemDisplay = ItemRewardDisplay("obj.bim_steak_sandwich", zoom = 400),
    ...
)
```

Add `check(quest.maxSteps == 120)` at startup, as other quests do.

### Stage transitions observed in the log

| Line(s) | Change | Trigger |
|---|---|---|
| 8274 | `bim` 0 → 5 | Player says Yes to Willow |
| 8405 | `bim` 5 → 7 | Player asks "What happened?" |
| 8690 | `bim` 7 → 10 | Willow finishes the crew explanation |
| 10602 | `bim_checkal` 0 → 5 | Checkal sends player to Atlas |
| 11395 | `bim_checkal` 5 → 10 | Player accepts Atlas' training |
| 13450 | `bim_checkal` 10 → 15 | Workout montage ends |
| 14648 | `bim_checkal` 15 → 20 | Checkal asks for the flex |
| 14940–14941 | `bim_checkal` 20 → 40, `bim` 10 → 15 | Player flexes; Checkal recruited |
| 16142 | `bim_marley` 0 → 5 | Marley asks for the sandwich |
| 18118 | `bim_marley` 5 → 10 | Cook explains the recipe |
| 21607 | `bim_marley` 10 → 35 | Sandwich handed over |
| 21819 | `bim_marley` 35 → 40 | Marley agrees to help |
| 24252 | `bim_burntof` 0 → 5 | Burntof asks for a drink |
| 24963 | `bim_burntof` 5 → 10 | Drink handed over |
| 25371 | `bim_burntof` 10 → 15 | Player accepts rock-paper-scissors |
| 26363–26364 | `bim_burntof` 15 → 40, `bim` 15 → 20 | Player wins RPS; all three recruited |
| 28709 | `bim` 20 → 25 | Entrance cutscene starts |
| 30365–30763 | `bim` 25 → 30; members 40 → 45 → 50 | Player enters the ruins |
| 32287 | `bim` 30 → 35; members 45 → 50 | Guardian awakens |
| 33536 | `bim` 35 → 40 | Guardian dies |
| 34266–34268 | `bim` 40 → 120; `claimed_reward` 0 → 1 | Ramarno appears / completion |
| 35068 | `camdozaal_ramarno_intro` 0 → 1 | Ramarno departs for workshop |
| 35640 | `camdozaal_ramarno_intro` 1 → 2 | Workshop conversation ends |

`bim` 15 is "first member recruited" (in the log it is Checkal). Use it when the
recruited count first reaches one. `bim` 20 when all three are 40.

---

## 2. Content inventory (all already in the cache)

### NPCs

| Purpose | Spawn/base | Visible/chathead |
|---|---|---|
| Willow outside | `npc.bim_willow_outside` 10638 | `npc.bim_willow` 10655 |
| Willow entrance | `npc.bim_willow_outside_entrace` 10639 | 10655 |
| Willow inside | `npc.bim_willow_inside` 10640 | 10655 |
| Willow post-quest (Nardah) | `npc.bim_willow_postquest` 10641 | 10655 |
| Checkal Barbarian Village | `npc.bim_checkal_barb` 10645 | `npc.bim_checkal` 10657 |
| Checkal entrance | `npc.bim_checkal_temple_outside` 10646 | 10657 |
| Checkal post-quest | `npc.bim_checkal_postquest` 10647 | 10657 |
| Burntof pub | `npc.bim_burntof_pub` 10648 | `npc.bim_burntof` 10659 / `_nodrink` 10660 |
| Burntof entrance | `npc.bim_burntof_temple_outside` 10649 | 10659/10660 |
| Burntof post-quest | `npc.bim_burntof_postquest` 10701 | 10659 |
| Marley Edgeville | `npc.bim_marley_edge` 10642 | `npc.bim_marley` 10656 |
| Marley entrance | `npc.bim_marley_temple_outside` 10643 | 10656 |
| Marley post-quest | `npc.bim_marley_postquest` 10644 | 10656 |
| Atlas | `npc.bim_atlas` 10658 | 10658; cutscene variant 10669 |
| Ramarno cutscene | `npc.bim_camzodaal_ramarno_cutscene` 10670 | 10670 |
| Ramarno entrance | 10702 (`camdozaal_ramarno_entrance_multi`) | 10685 |
| Ramarno workshop | 10703 (`camdozaal_ramarno_multi`) | 10685 then 10684 |
| Ancient Guardian | `npc.bim_golem_boss` 10654 | 10654 |
| Guardian cutscene | `npc.bim_golem_guardian_cutscene` 10665 | 10665 |
| Rubble cutscene | `npc.bim_golem_boss_inactive_cutscene` 10666 | 10666 |
| Invis fuse | `npc.bim_invisnpc` 10667 | 10667 |
| Tina cutscene | `npc.bim_cutscene_tina` 10668 | 10668 |
| Charlie the Tramp | `npc.tramppg` 5209 | 5209 |
| Blue Moon cook | `npc.fai_varrock_bluemoon_chef` 2895 | 2895 |
| Cutscene extras | Willow 10661, Marley 10662, Checkal 10663, Burntof 10664, Tramp 10671, Man 10672/10673, Woman 10674, Stray dog 10675, Barbarians 10676–10679, Aubury 10681, Rat 10682, Baraek 10683 |  |

The base spawns already exist in the world map with `multinpc` masks on the BIM
varbits; the client shows/hides them per player. Do not add world spawns for these.

### Locs

| Loc | id | Notes |
|---|---|---|
| Entrance multiloc | `loc.bim_entrance` 41357 | on `bim`: 0–29 rubble `loc.bim_rock` 41441, 30+ door `loc.bim_door` 41439 |
| Boss pillar | `loc.bim_boss_rock` 41458 | 2x1, Mine, 10 Mining, 10 XP |
| Broken pillar | `loc.bim_boss_rock_mined` 41459 | no ops |
| Cutscene pillar | `loc.bim_boss_rock_cutscene` 41460 | no ops |
| Big door closed / open | 41482 / 41483 / 41484 | 4x2 closed, two 1x3 leaves |
| Trap tripwire | `loc.bim_tripwire` 41442 | Marley cutscene only |
| Ruins exit | 41446, cutscene 41447 | Exit, generic transport already present |
| Willow's bag | 41448, cutscene 41449 | Investigate |
| Pressure plates | 41485 | 2x2 at 2952–2953, 5774–5775 |
| Entrance debris/rubble | 41445, 41451–41454 | cutscene dressing |

Static placements confirmed by `dumpLocs`: entrance at 2999, 3493; exit at 2951,
5761; bag at 2950, 5769; open door leaves at 2951/2954, 5780; pressure plates at
2952–2953, 5774–5775.

### Items

`obj.coins` 995, `obj.bread` 2309, `obj.knife` 946, `obj.cooked_meat` 2142,
`obj.beer` 1917, `obj.asgarnian_ale` 1905, `obj.dwarven_stout` 1913,
`obj.wizards_mind_bomb` 1907, `obj.bim_steak_sandwich` 25631.

### Music

| Track | MusicRow | Midi |
|---|---|---|
| Barbarian Workout | 2576 | 689 |
| The Ruins of Camdozaal | 2593 | 691 |

Resolve by `MusicRow.all().first { it.displayname == ... }`; do not hardcode row ids.

### Animations/sounds

Guardian idle/attack/death/awaken: `seq.ancient_guardian_idle` 8941,
`seq.ancient_guardian_attack_melee` 8942, `seq.ancient_guardian_death` 8943,
`seq.ancient_guardian_awaken` 8944. Steak sandwich: `seq.bim_sandwich_make` 8938.
Flex: `seq.emote_flex` 8917. Sounds: `synth.bim_ancient_guardian_*` (4971–4996
range), `synth.bim_dungeon_door_open` 4994, `synth.bim_dungeon_rumble` 4988, etc.

---

## 3. Files

New, under `content/quest/src/main/kotlin/org/rsmod/content/quest/area/asgarnia/icemountain/`:

| File | Responsibility |
|---|---|
| `BelowIceMountain.kt` | `QuestScript`: registration, rewards, journal, stage constants/helpers, login recovery, QP gate. |
| `BimNpcs.kt` | Willow, Checkal, Atlas, Burntof, Marley, Charlie, Cook, Ramarno handlers; delegates to dialogue files. |
| `BimWillowDialogue.kt` | Start/in-progress/entrance/post-quest Willow. |
| `BimCheckalDialogue.kt` | Recruitment, Atlas reminder, flex prompt, entrance, post-quest. |
| `BimAtlasDialogue.kt` | Quest training offer, paid repeats, first/second/subsequent variants. |
| `BimBurntofDialogue.kt` | Drink, RPS challenge, entrance, post-quest. |
| `BimMarleyDialogue.kt` | Sandwich request/hand-in, entrance, post-quest. |
| `BimCookDialogue.kt` | Blue Moon cook quest recipe + cabbage shop/dialogue. |
| `BimCharlieDialogue.kt` | Charlie standard dialogue + Marley option. |
| `BimRamarnoDialogue.kt` | Entrance intro, workshop intro, item-on-NPC. |
| `BimWorkoutCutscene.kt` | Private Varrock/Barbarian region montage, camera frames, NPCs, flex unlock. |
| `BimRpsCutscene.kt` | Upstairs rock-paper-scissors scene. |
| `BimEntranceCutscene.kt` | Sabotage outside + reveal inside + boss spawn. |
| `BimGuardian.kt` | Boss/pillar/door controller, headbar, death, reset, multi-player progress. |
| `BimItems.kt` | Steak sandwich, drink hand-in, bag coins. |
| `BimRegions.kt` | `RegionTemplate`s + coord assertions for Varrock and Barbarian Village. |

Modify:

- `content/quest/src/main/kotlin/org/rsmod/content/quest/manager/QuestModule.kt`:
  `bindInstance<BelowIceMountain>()` and any cutscene/guardian helpers that are
  injected.
- `content/areas/city/barbarian-village/.../Atlas.kt` and `Checkal.kt`: remove their
  BIM handlers (move the working post-quest text into the quest module).
- `content/areas/city/edgeville/.../EdgevilleLocals.kt`: remove the Marley stub;
  keep Doris.
- `content/quest/pack/src/main/resources/pack/configs/bim_npcs.toml` (new): Ancient
  Guardian server override — aggressive hunt mode, attack range 1, hunt range 5,
  max range 7, `param.headbar = headbar.health_30`, attack/defend/death anims,
  crush attack type, attack sound; suppress drops. Verify the exact `huntMode`
  value against a similar aggressive NPC.
- `content/travel/map-transports/src/main/resources/transports/transports.tsv`:
  keep or remove the two `loc.bim_door`/`loc.bim_exit` rows. If the quest module
  handles the door itself (recommended, so it can spawn/resume the boss), remove
  them to avoid a second server.
- `PROGRESS.md` is generated; do not edit.

No new cache IDs are needed for the core quest. Rebuild the cache after adding the
NPC pack config: `gradlew buildCache`.

---

## 4. Phase plan

### Phase 0 — scaffold and quest shell

1. Add `BelowIceMountain` with the real row key/varbit/varp, `maxSteps == 120`
   check, QP gate (16) and journal/reward wiring.
2. Rewards: `rewards { extra("2,000 Coins"); extra("Access to the Ruins of");
   extra("Camdozaal"); extra("1,200 Mining XP") }`; grant the 1,200 Mining XP in
   `onCompleted` with `statAdvance("stat.mining", 1200.0)`. Do **not** put 2,000
   coins in `rewards.items` — the coins come from the bag.
3. Completion scroll: model `obj.bim_steak_sandwich`, zoom 400. Line order:
   QP (automatic), 2,000 Coins, Access to the Ruins of, Camdozaal, 1,200 Mining XP.
   Verify against a current live scroll; the sniff only proves the first four.
4. Journal:
   - stage 0: none.
   - stages 5–19: the intro paragraph + three recruit objectives. Strike each
     member's objective when their varbit reaches 40, replacing it with
     "<str>I have recruited X.".
   - stage 20+: intro lines struck, new paragraph "I spoke with Burntof, Checkal
     and Marley..." (the exact packet text is below).
   - completion: the full struck epilogue and "QUEST COMPLETE!".
   Use the packet log's `<col=000080>` body and `<col=800000>` names for 1:1,
   not the generic `<red>`.
5. Login: `migrateLegacyStage`, then `recover(player)`:
   - mid-workout (`bim_checkal` 10–14): teleport to Atlas, reset checkal to 5.
   - `bim` 25: teleport to entrance (2996, 3496), reset `bim` to 20.
   - `bim` 30: teleport inside (2952, 5767), set 35, ensure boss.
   - `bim` 35/40: ensure boss/doors as appropriate.
   - any player in an instance region with no active cutscene: teleport to the
     nearest safe stage tile.
6. Register quest; confirm the quest appears in the quest list and the start icon.

### Phase 1 — recruitment loop (no cutscenes)

Willow (outside 3003, 3435):

- Use `access.isBodyTypeA()` for the transcript's `[man/woman]`, `[sir/miss]`,
  `[lad/lass]` and `[strongman/strongwoman]` variants.

- Stage 0: full transcript dialogue. QP < 16 → message and stop. `choice2` titled
  "Start the Below Ice Mountain quest?" (Yes/No). No → "Seems like you're in a bit
  of a pickle. Have fun with that."
- Stage 5/7: resume/re-explain; set 7 before "We had a falling out..." and 10 at
  the end.
- Stage 10–19: "Back again? What can I help you with?" menu: Marley, Checkal,
  Burntof, entrance, nothing. Set 10 on exit if still 5/7.
- Stage 20: entrance gathering scene (Phase 3).

Checkal (Barbarian Village 3087, 3415):

- Stage 0–4: full recruitment dialogue. 99 Strength branch is in the transcript.
  Set 5 before "prove your strength". Set 10 when Atlas training is accepted.
- Stage 5–9: "I seem to have forgotten where Atlas is. Can you remind me?"
- Stage 15–19: "So have you completed your training session with Atlas?" → flex
  prompt, `mesbox("Show your strength by flexing your muscles.")`, set 20.
- Stage 20: "What did you want me to do again?" → same prompt.
- Flex event: on `PlayEmote` with `seq.emote_flex`, if checkal 20 and within the
  Barbarian Village area, set 40, set `bim` 15 if it is 10, and play the
  post-flex Checkal dialogue. Do not double-play the animation.
- Entrance (10646) and post-quest (10647) dialogues from the transcript.

Atlas (3075, 3439):

- Before Checkal sends the player: "What do you want, little man/woman?" and
  "Not now, I'm busy."
- Checkal 5–9: full offer; Yes sets checkal 10 and starts Phase 2 cutscene; No
  ends with the transcript lines.
- Quest complete: the paid repeat dialogue. Counter 0 → first repeat text,
  1 → second, else subsequent; deduct 25,000 coins in an inventory transaction
  with rollback, then replay the workout cutscene and increment the counter.
  If no coins, the exact "Hold it there little one..." branch.

Marley (Edgeville 3088, 3471):

- Stage 0–4: full request dialogue; set 5.
- Stage 5–9: "Have you got the sandwich yet?" branch.
- Stage 10–34: same, but the cook has taught the recipe.
- With `obj.bim_steak_sandwich`: consume it, `objbox` "You give the steak sandwich
  to Marley.", set 35, play the hand-in dialogue, set 40, then `bim` 20 if all
  three are 40.
- Entrance (10643) and post-quest (10644) dialogues.

Burntof (Rising Sun Inn 2956, 3367):

- Stage 0–4: "Buy ush a drink."; no drink → set 5; with drink → consume and
  continue.
- Stage 5–9: "Where'sh my beer?"; no drink → "I'll get right on it."; with
  drink → consume, `objbox`, set 10, continue.
- Accepted drinks: beer, Asgarnian ale, Dwarven stout, Wizard's mind bomb, with
  the per-drink response lines.
- Stage 10–14: the Willow explanation and RPS challenge; set 15.
- Stage 15–39: start Phase 2 RPS cutscene (resume if interrupted).
- Stage 40: entrance dialogue; post-quest variant.

Charlie (Varrock, `npc.tramppg` 5209):

- Add the standard dialogue (Who are you? / Sorry / Go get a job / Here you go /
  alleyway) plus the BIM option "I'm looking for a man named Marley."
- The option is visible while `bim_marley` < 40; the response is the transcript's
  cousin/robbery/Edgeville text. Set nothing beyond marley 0 (talking to Marley
  sets 5).

Cook (Blue Moon Inn, `npc.fai_varrock_bluemoon_chef` 2895):

- "What do you want? I'm busy!" menu.
- Quest option when `bim_marley` 5–34: the full recipe explanation; set marley 10
  when the recipe is given. The "I forgot the recipe" variant at stage 10+.
- "I've managed to make myself a Steak sandwich" when the player has one.
- Cabbage shop: ∞ stock, 1 coin (the wiki's non-quest functionality).
- Steak sandwich recipe: `onOpHeldU("obj.knife", "obj.bread")`, requires
  `bim_marley >= 10`, `obj.cooked_meat` in the inventory, consumes bread + meat,
  adds `obj.bim_steak_sandwich`, `anim(seq.bim_sandwich_make)`, message
  "You combine the cooked meat and bread to make a Steak sandwich!".
  Do not grant Cooking XP (not in the log). Transaction rollback on full
  inventory/overflow.

### Phase 2 — workout and RPS cutscenes

**Workout montage** (log 10869–13560):

- Two private `RegionTemplate`s:
  - Varrock: copy region (50, 53) and (50, 54), which covers Varrock square,
    Aubury's, the west side and the palace steps.
  - Barbarian Village: copy region (48, 53), covering the Long Hall and Atlas.
- Use the `TheRestlessGhostCutscene` pattern: `regionRepo.add(template)` →
  `regionRepo.protect` → `SceneRegion` mapping → `lockedScene`/custom camera →
  `regionRepo.unprotect` in `finally`.
- Persist a return tile via a fixed stage rule (Atlas tile 3076, 3423). On login
  in an instance region with checkal 10–14, return to Atlas and reset to 5.
- Scenes, from the log:
  1. Varrock square / Aubury's: spawn Tramp, Rat, Aubury, Man; camera frames
     11580–12015; player runs; tomato map animation at 3234, 3426; unlock
     "Barbarian Workout".
  2. Barbarian Village: Atlas 10669 and barbarians; push-up/sit-up sequences
     8930/8931; camera 12138–12560; player runs north.
  3. Varrock palace steps: guards 11903–11905; camera 13027–13288; player runs
     up and cheers.
  4. Barbarian Village: Atlas final lines, `emote_flex = 1`, the flex unlock
     `mesbox`, return to (3076, 3423).
- Use `forcedWalk`/`telejump` for the player path, `walk` for NPCs, `facing`,
  `anim`, `spotanim`, `soundSynth`, `midi`, `camShake`, `mapAnim`.
- Start music: resolve `MusicRow` 2576 and `unlockAndPlay`.
- End: checkal 15; the player is back at Atlas. The flex unlock is part of the
  quest training even if the player later never uses the emote.

**RPS cutscene** (log 24088–26655):

- Player accepts at Burntof stage 10; set 15.
- Fade/hide HUD; spawn cutscene Burntof 10664 at (2956, 3370, 1); camera frames
  upstairs; play the rules dialogue.
- Three rounds: player choice Rock/Paper/Scissors; Burntof's sign is:
  - round 1: the sign that loses to the player (player "wins"); Burntof
    congratulates and confirms the wrong sign.
  - round 2: the sign that beats the player (Burntof wins); player claims
    victory, Burntof corrects.
  - round 3: the sign that beats the player (Burntof claims victory); player
    changes to the sign that beats Burntof; Burntof concedes.
- Spawn Tina 10668 at (2956, 3373, 1), the beer hand-off, then despawn.
- Set `bim_burntof` 40; set `bim` 20 if all three are 40; return the player to
  the pub floor.
- If the player logs out mid-scene, stage 15 remains; re-talking restarts.

### Phase 3 — entrance, reveal, boss, bag, Ramarno

**Entrance cutscene** (log 28522–30760):

- Willow entrance 10639 at (2996, 3495), stage 20. If not all recruited, a short
  "hurry up" branch.
- Full pre-cutscene dialogue and the Yes/No "Are you ready?" choice.
- Yes: set `bim` 25. Despawn the base crew spawns (or let the varbit hide them),
  spawn cutscene crew 10661–10664, hide HUD, camera, forced walk to the entrance.
- Willow/Burntof/Checkal/Marley/player lines in transcript order; Burntof places
  dynamite, fuse, explosion; loc swaps:
  - delete `bim_entrance` rubble 41357/41441 at 2999, 3493
  - add `bim_door_cutscene` 41440 and `bim_rubble_3` 41454
  - after Checkal, remove the rubble
  - add the cutscene exit 41447, tripwire 41442, cutscene bag 41449
- Follow the fade/camera beats in the log. At the end, teleport the player inside
  to (2952, 5767), set `bim` 30 and members 50. The normal exit stays replaced by
  the cutscene exit until the post-boss sequence.
- If the player logs out at 25: teleport outside to Willow, reset to 20.
- If at 30: teleport inside (2952, 5767), set 35, ensure the boss, skip the reveal.

**Reveal cutscene** (log 30760–32690):

- Camera on the crew; the long reveal dialogue (Willow is a grave robber; towns
  and crew reactions; Burntof steps on the pressure plate).
- Add the closed big door 41482 and the cutscene pillars 41460 during the reveal
  (log 30825–30831).
- Map animations 60, camera shake, rumble sound 4988; swap Rubble 10666 for the
  cutscene Guardian 10665 with `seq.ancient_guardian_awaken`, sound 4972.
- Crew flees; fade out; set `bim` 35; spawn the real Guardian 10654 at
  (2952, 5778, south), swap the cutscene pillars to the mineable 41458; play
  midi 691; fade in.
- If the player logs out at 35, ensure the same spawn/resume.

**Boss fight**:

- Guardian: use the cache stats (40 HP, combat 25, attack 25, strength 33,
  defence 25); server config override for aggression, attack range 1, hunt range
  5, max range 7, attack speed if needed, `headbar.health_30`, crush attack type,
  attack/death sounds/animations. Verify max hit 7 and attack speed 5 in a live
  client; adjust params if the cache defaults differ.
- Pillars: four at (2947, 5771), (2958, 5771), (2947, 5778), (2958, 5778),
  angle 3/3/1/1 (South/North mapping via `LocAngle`), shape 10. Each:
  - needs a pickaxe (reuse the mining tool helper; expose it from the mining
    module rather than duplicating);
  - needs Mining 10; message "You need a Mining level of 10 to mine this rock."
    (verify whether the pillar says "pillar");
  - plays the pickaxe animation, spam "You swing your pick at the pillar.";
  - 50% chance per swing to collapse (the log shows 2–3 swings per pillar);
  - on collapse: change to 41459, `spotanim` 60 (falling rocks), 10 Mining XP,
    10–12 damage to the Guardian capped at remaining HP, hitmark; if it dies,
    advance.
  - Guard against same-tick double clicks, loc already mined, boss already dead,
    and the player moving/cancelling.
- Death: `NpcDeathKillHook` on `npc.bim_golem_boss` plus a controller check.
  Pillar damage can kill without a player attack, so the pillar handler must call
  the same controller `onDeath(player)` path directly when HP reaches zero. Use a
  `dead` flag so the hook and the pillar path cannot advance twice.
  Advance every player in the chamber at stage 35 to 40 (not killer-only).
  Remove the boss, play the death/fade, and hand off to the post-boss sequence.
- Headbar: `param.headbar = headbar.health_30` on the NPC config; the engine
  updates it as HP changes. Verify the 30-unit fill matches the log.

**Post-boss, bag, completion** (log 33536–34360):

- Fade; remove the boss; leave the broken pillars for this player's scene; restore
  the normal exit 41446; open the big doors (41482 → 41483/41484), sound 4994;
  fade in. The boss death hook itself only removes the Guardian and sets stage
  40; the door/exit swaps belong to this sequence. Broken pillars are cleared
  when the room resets for other players.
- Position the player at (2951, 5769). Player lines about the monster and the
  bag; `mesbox("You rummage through Willow's bag...")`;
  `objbox("obj.coins", "You find a pile of coins!")`; grant exactly 2,000 coins
  in an inventory transaction; set `bim_claimed_reward = 1`; player "Those will
  do nicely!".
- Door-opening sound; player "What was that noise? Please don't be another rock
  monster..."; spawn cutscene Ramarno 10670 at (2952, 5778); player "What the!";
  Ramarno "Bless my beard..."; despawn 10670 and ensure the entrance Ramarno
  (10702) at (2951, 5779).
- Call `quest.completeQuest(...)`, which sends the start/complete message, adds
  the QP, plays the jingle and opens the scroll, then `afterQuestScroll { ... }`
  to start the Ramarno entrance dialogue after the player closes it.
- Entrance Ramarno dialogue from the transcript; end with "Ramarno departs for
  his workshop." and set `camdozaal_ramarno_intro = 1`.
- The workshop Ramarno (10703 at 2959, 5809) offers the second conversation;
  set `camdozaal_ramarno_intro = 2` at the end. Do not implement the forge/
  barronite/vault gameplay beyond the dialogue.
- Item-on-Ramarno: "Ramarno still looks shocked. Perhaps you should talk to him
  first."
- Post-quest Willow/Checkal/Burntof/Marley Nardah dialogues: add the handlers if
  the NPCs are present; the F2P quest does not depend on them.

### Phase 4 — music and polish

- `Barbarian Workout`: unlock/play at the montage start; check the area music
  system doesn't duplicate the message.
- `The Ruins of Camdozaal`: unlock/play on entering the ruins. If a music area
  for Camdozaal already exists, rely on it and do not send a second unlock.
- Quest scroll line order and colors; verify with `screenshot {interfaceId}` and
  `dump_interface` in the live client.
- Journal line wrapping/colors at each stage; compare with the packet log's
  interface 119 text.

### Phase 5 — recovery, multiplayer and tests

Recovery rules:

- Logout/death during the workout: return to Atlas, reset checkal to 5.
- Logout/death during the entrance: 25 → outside/reset 20; 30 → inside/35.
- Logout/death at 35: boss remains/revives; entering the room ensures it.
- Logout during the post-boss sequence: stage 40; on login open the doors, ensure
  Ramarno; if the bag is unlooted, allow it; talking to Ramarno completes.
- Boss death with several players in the chamber: set every eligible player to
  stage 40. Run the full bag/Ramarno sequence for the player who landed the kill;
  for the others, queue it (or let them trigger it by looting the bag / talking
  to Ramarno). Each player must still loot their own bag coins and see their own
  completion scroll exactly once.
- Boss reset: when no players at 35–40 remain in the chamber for ~100 ticks,
  despawn the boss/pillars/closed door and restore the open doors.
- Entrance/RPS shared-world scenes: a world-level cutscene lock so two players
  cannot drive the same shared NPCs at once. If locked, the NPC asks the player
  to wait. This is the pragmatic deviation needed because NPCs are shared.

Tests (JUnit 5, `GameTestExtension`; mirror `DaddysHomeInteractionTest`):

- `BimQuestStageTest`: start requires 16 QP; `bim` 0→5→7→10; each recruit's
  varbit transitions; `bim` 15/20/25/30/35/40/120; journal text at 0/10/20/120.
- `BimItemFlowTest`: bag gives exactly 2,000 coins once (repeat click, logout
  between loot and completion, full inventory, coin stack at max); steak
  sandwich consumes bread + cooked meat, not the knife; each accepted drink is
  consumed once; unaccepted drinks are not consumed; no dupe/loss totals.
- `BimGuardianTest`: pillar requires pickaxe/Mining 10; 10 XP per collapse;
  damage 10–12 capped; four pillars kill 40 HP; duplicate clicks don't double
  damage/XP; death advances every eligible player once; reset removes locs.
- `BimRewardTest`: 1 QP, 1,200 Mining XP exactly once, scroll lines/model.
- `BimRecoveryTest`: login at checkal 10, `bim` 25/30/35/40 lands on the right
  tile and doesn't repeat one-time rewards.
- `BimCutsceneRegionTest`: every cutscene coord resolves inside the copied
  template (`assert...Coords` pattern from TheRestlessGhost).

Live client verification with the OpenRune Developer Tools MCP:

1. `::item` beer, bread, knife, cooked meat, bronze pickaxe; start the quest at
   Willow; check the start message and journal.
2. Walk each recruit through every branch, including "No" answers and repeat
   dialogues; check `get_var_history` for the stage varbits.
3. Run the workout twice (quest + one paid repeat); verify coins deducted once,
   counter increments once, flex unlocked, camera/music/animation.
4. Run the RPS cutscene; verify all three rounds and Tina.
5. Trigger the entrance cutscene; check the loc swaps, fades, sounds, camera,
   and that the door becomes `loc.bim_door`.
6. Fight the Guardian with a weapon and with pillars; verify HP/headbar, max hit,
   pillar XP, death, stage 40, bag coins, completion scroll and Ramarno.
7. Close the scroll; verify the auto-Ramarno dialogue, the workshop dialogue,
   `camdozaal_ramarno_intro` 0→1→2, and re-entering Camdozaal.
8. Log out/death at each cutscene and stage; verify the recovery tile and no
   duplicated rewards.
9. Two clients in the chamber: both at stage 35, kill the boss, both advance.
10. `gradlew :or-cache:checkGamevals -PfailOnMissing`,
    `gradlew spotlessCheck` (or `spotlessApply` on changed files), and the
    relevant test modules.

---

## 5. Packet-log reference map

Use these line ranges when implementing:

| Section | Lines |
|---|---|
| Willow start / journal | 7222–8845 |
| Checkal recruitment | 9687–10794 |
| Atlas offer / workout | 10869–13560 |
| Checkal return / flex | 14371–15020 |
| Marley request | 15457–16393 |
| Cook / sandwich | 17600–18300 |
| Marley hand-in | 21488–21850 |
| Burntof / RPS | 24088–26655 |
| Willow at entrance | 28522–28900 |
| Sabotage / enter ruins | 28900–30760 |
| Reveal / guardian | 30760–32690 |
| Boss fight / pillars | 32660–33560 |
| Bag / doors / Ramarno / completion | 33560–34360 |
| Post-quest Ramarno | 34360–35881 |

Camera frames, NPC paths and sounds should be extracted from the log rather than
eyeballed. A one-off parser can emit a Kotlin table of `(tick, camera, npc, anim,
sound, loc)` beats; keep only the compact result in the repo, not the raw sniff.

---

## 6. Known deviations / items to verify live

1. **Completion scroll**: the sniff predates the XP line. Verify the current
   order and wording ("1,200 Mining XP" vs "1,200 Mining experience").
2. **Pillar collapse chance**: the log shows 2–3 swings per pillar; use 50% per
   swing. Verify whether OSRS uses a fixed two-swing state instead.
3. **Pillar damage**: log values 12/12/10/6 sum to 40; use random 10–12 capped
   at remaining HP. Verify the exact roll.
4. **Pillar low-level message**: "rock" vs "pillar".
5. **Quest start with <16 QP**: exact refusal text/UI is not in the transcript;
   the plan uses a message and ends the dialogue. Verify.
6. **Quest overview subtitle**: no cache column; use a short official-description
   subtitle. Verify if a client script provides the real one.
7. **Shared-world cutscene lock**: not OSRS-observable in a solo sniff; needed
   because NPCs are shared. Confirm behavior with two clients.
8. **Ramarno entrance spawn 10702**: verify whether it is a map spawn or must be
   spawned; the wiki spawn list says it is a fixed spawn at 2951, 5779.
9. **Boss drops**: verify the Ancient Guardian drops nothing (no bones), and add
   a suppression hook if the default death handling drops bones.
10. **Nardah post-quest crew**: members-only area; add the dialogues only if the
    NPCs exist in the current map data.
11. **Music area duplicate**: check the music area table before sending manual
    unlock messages for Camdozaal.
12. **Attack speed/max hit**: cache params vs wiki (5 ticks, max 7). Verify in a
    live client and adjust the NPC override if needed.

---

## 7. Suggested commit sequence

1. `quest: add Below Ice Mountain scaffold, stages, journal and rewards`
2. `quest: add BIM recruitment dialogues and item flows`
3. `quest: add BIM workout and RPS cutscenes`
4. `quest: add BIM entrance, guardian and completion sequence`
5. `quest: add BIM tests and recovery handling`
6. `quest: add BIM NPC overrides and live-verification fixes`

Run `gradlew buildCache` after adding `bim_npcs.toml` and any `gamevals.toml`
entries; run `checkGamevals` and the quest tests before each commit.
