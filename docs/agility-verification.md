# Agility course verification

An in-client pass over the six courses added on 2026-09-14. Their obstacle tiles came out of the
cache and are exact; the landings were mostly reasoned from that geometry rather than observed, and
this checklist settled them.

## Result, run 2026-09-14

**Five of the six verified obstacle for obstacle. Every xp value is exactly right.**

| Course | Obstacles | Lap xp | Result |
|---|---|---|---|
| Colossal Wyrm Basic | 6/6 | 601.6 | every landing exact |
| Colossal Wyrm Advanced | 6/6 | 1,053.6 | every landing exact |
| Shayzien Basic | 7/7 | 153.5 | every landing exact |
| Shayzien Advanced | 8/8 | 508.0 | every landing exact |
| Werewolf | 5/5 (7 steps) | 350 | every obstacle fires |
| Ape Atoll | not run | - | see below |

**The shared-loc dispatch is proven, twice.** It is the part of the change no test can reach, and
both halves of it behave:

- The Shayzien start ladder paid **6.0** to a level 99 player wearing a grapple and **5.5** without,
  so one loc really does serve two courses at two different values.
- The Colossal Wyrm zipline paid **662** at the end of an advanced lap and **341.2** at the end of a
  basic one, so the lap in progress decides the payout rather than the player's level.

**Ape Atoll was not run in this pass.** The greegree gate works exactly as designed - the obstacles
refuse with *Only the stealthiest and most agile monkey can use this!* and pay nothing - but the
greegree could not be equipped: holding one answered *Nothing interesting happens*. The cause was
`ItemServerType.isEquipable` accepting only Wield or Wear, where greegrees say Hold; fixed since, so
the course is ready for its run. Holding a greegree still does not turn the player into a monkey.

**Two defects the first pass missed, both found by playing rather than probing** and both fixed on
the same day:

- **The Colossal Wyrm start ladder landed on the wrong platform.** The tower has two decks - the
  first at z 2931-2932 above the ladder, the second at z 2926-2928 where the tightrope starts, joined
  by a walkway at 1655, z 2929-2930. The ladder dropped the player straight onto the second, skipping
  the first and the walk between them. This is the one Colossal Wyrm landing that did *not* come off
  a termite marker - the first marker sits past the tightrope - so it was a guess described as
  evidence. Now 1653,2931.
- **The first rope had no travel.** The merged tightrope-and-ledge had no `slide`, so it played the
  balance animation on the spot for ten ticks and teleported. It was left off because a slide is a
  straight line and that crossing turns a corner. `Obstacle.via` now names the corner and the
  crossing runs as two legs: south along the rope to 1655,2919, then west along the ledge to
  1649,2910.

Neither was reachable by probing landings, because probing only confirms the player arrives where
the code says - it cannot tell you the code names the wrong tile, or that the journey looked wrong.

Two notes for anyone repeating this:

- **Relative landings resolve from where the engine parks the player, not from where you teleported
  them.** Clicking an obstacle walks the player to its approach tile first, so a `Landing(dx = -3)`
  measured from your assumed tile can come out a tile off. Both apparent mismatches in the run were
  this, not a wrong landing.
- **The two Shayzien Beams sit at x 1516 in the client scene and x 1512 in the cache map.** The code
  binds by loc id so this changes nothing about behaviour, but probe from 1515 rather than 1513 or
  the client will not raise a menu.

Driving the client, which cost more time than the verifying did:

- **Reset the camera after every teleport.** `interact_object` finds its target by projecting to the
  screen, so a rotated camera makes every obstacle "have no menu entry". Clicking the compass at
  roughly 607,38 puts yaw back to 0 and it works again. Most of the apparent failures in both passes
  were this.
- **`walk_to` is a no-op in this build.** Raw `click` on the viewport moves the player fine; do not
  read a stationary player as the game being stuck.
- **Thin models need `invoke_menu_action`.** Ropes and beams are too thin for the hover to land on,
  so drive them with `GAME_OBJECT_FIRST_OPTION` and the object's scene coords. Scene coords are world
  minus the scene base, and **the base is not always the player's region base** - it is at the
  Colossal Wyrm and is not at Shayzien, where the same arithmetic silently invokes empty tiles and
  every obstacle reports zero xp. Sanity-check one obstacle before trusting a whole lap to it.

## Playing it, which found what probing could not

The pass above confirmed every obstacle binds, lands where the code says and pays the right xp. None
of that could catch the way a crossing *looks*, and a second pass spent running laps found a series
of faults the measurements were blind to. Recording them because the pattern matters more than the
individual fixes:

| What it looked like | What it was |
|---|---|
| Player floats across instead of walking | Traverse paced by a fixed tick count rather than the distance |
| Hangs correctly, then drops off and slides | `anim` plays once; the animation ended six ticks before the ride did |
| Jumps, then drifts | Movement spread over the whole animation instead of the airborne tick |
| Skips weirdly across a gap | One straight slide cutting the corner off a zigzag of planks |
| Walks an invisible rope beside the real one | Crossing started from where the engine parked the player, not on the line |
| Hovers at the top of a ladder | `Landing(level = n)` landing on the tile above, which is open air |
| Drops into the hay bales | Landing tile guessed instead of using the course's own `Landing Spot` loc |
| Stranded mid-crossing, unable to move | An interrupted crossing leaving the player on an isolated plank |

**A probe that checks the player arrived where the code says is very close to tautological.** It
cannot tell you the code names the wrong tile, that the journey there looked wrong, or that the tile
is unreachable. Two measurement mistakes are worth knowing about before repeating this:

- Sampling position cannot tell a slide from a teleport, because `get_client_state` reports the
  server position and an exact move commits it up front. Timing can, but only if the walk to the
  obstacle is excluded - the engine walks the player to the loc first, and that movement is easily
  mistaken for the crossing.
- Staging a player by teleport puts them on tiles a real player could never stand on. One "the
  player starts somewhere unwalkable" report turned out to be the test harness's own staging tile
  (1652,2930, the mound beside the start ladder), not the course.

## What this checked, and what it did not

Every obstacle binds, resolves its op, executes its plane change, and pays exactly the wiki's xp.
The landings were confirmed to be real standing ground: the Shayzien plane drops both put the player
on the scaffolding decking, and the platform before the final gap carries the bucket-of-water spawn
the wiki describes, which independently confirms the geography.

What no amount of this proves is whether a landing matches **live**. The tile is ours; the client
only shows it is a sane place to stand. The Werewolf deathslide in particular still lands where it
was guessed - it works and closes the lap, but nothing here says live agrees.

## Setting up

The server needs no cache rebuild for these courses - they use cache symbols that already exist and
add no gamevals. `gradlew run`, then start the client with `--developer-mode` and enable
**OpenRune-DeveloperTools** once in the plugin sidebar.

```
::setlevel agility 99          # nothing here should be gated by level while checking landings
::item 4025                    # medium ninja monkey greegree, for Ape Atoll
::item 9419                    # mith grapple, for Shayzien advanced
::item 9174                    # bronze crossbow, same
```

Quest gates run through `QuestRequirements`, so if `gameplay.quest-requirements.mode` is
`assume-completed` the Werewolf and Colossal Wyrm gates pass without the quests.

## How to check one obstacle

```
walk_to {x, y}                                  -> stand where the obstacle is approached from
interact_object {nameFilter: "<name>", option: "<op>"}
get_client_state                                -> read the tile the player actually ended on
```

A landing is right when the player can see and click the next obstacle without walking through
scenery or falling. It is wrong when they land inside a wall, on the wrong plane, or out of reach of
the next obstacle. Record what `get_client_state` gives and correct `AgilityCourses.kt` to it.

## Shayzien, basic

Start at the ladder south-west of Kourend Castle. The basic and advanced courses share the first
three obstacles, so a failure here fails both.

| # | Obstacle | Loc | Stands at | Expected landing |
|---|---|---|---|---|
| 1 | Ladder | `shayzien_agility_both_start_ladder` | 1554,3631,0 | same tile, plane 3 |
| 2 | Monkeybars | `shayzien_agility_both_rope_climb` | 1552,3633,3 | 1537,3633,2 |
| 3 | Tightrope | `shayzien_agility_both_rope_walk` | 1536,3633,2 | 1526,3633,2 |
| 4 | Bar | `shayzien_agility_low_bar_climb` | 1523,3640,2 | same tile, plane 3 |
| 5 | Tightrope | `shayzien_agility_low_rope_walk_1` | 1525,3644,3 | 1540,3644,2 |
| 6 | Tightrope | `shayzien_agility_low_rope_walk_2` | 1541,3644,2 | 1553,3644,2 |
| 7 | Gap | `shayzien_agility_low_end_jump` | 1554,3641,2 | 1554,3637,0 |

**The plane changes are what to watch.** Steps 2 and 5 drop a plane and steps 1 and 4 climb one,
all inferred from which plane the *next* obstacle sits on rather than from anything observed. If
step 2 lands the player on plane 3 in live, the tightrope at step 3 is unreachable and the whole
course stalls there.

A lap pays 153.5 and there is no separate completion bonus: the 106 on the final gap is the
completion award, which is why the wiki calls it the obstacle that counts the lap.

## Shayzien, advanced

Needs a crossbow and a mith grapple in hand and quiver. Shares steps 1-3 above, then:

| # | Obstacle | Loc | Stands at | Expected landing |
|---|---|---|---|---|
| 4 | Beam | `shayzien_agility_up_swing_jump_1` | 1512,3637,2 | 1511,3635,2 |
| 5 | Edge | `shayzien_agility_up_jump_platform_1` | 1510,3634,2 | 1510,3630,2 |
| 6 | Edge | `shayzien_agility_up_jump_platform_2` | 1510,3627,2 | 1511,3622,2 |
| 7 | Beam | `shayzien_agility_up_swing_jump_2` | 1512,3619,2 | 1519,3621,2 |
| 8 | Zipline | `shayzien_agility_up_end_jump` | 1522,3621,2 | 1551,3630,0 |

**Also check the branch.** Take steps 1-3, then step onto the basic course's Bar instead of the
Beam: the lap should switch to basic and still complete for 153.5, not reset. Then the reverse, at
level 45 with a grapple. This is the one piece of logic in the change that no test can reach, since
it depends on player state.

The zipline landing is a guess at "somewhere near the start ladder on the ground". Anywhere that
closes the lap is fine; the tile itself does not matter much.

## Colossal Wyrm, basic

The one course whose landings are **not** guesses - each came off the termite spawn marker that sits
where live drops the player. Expect these to be right, and treat a mismatch as a sign the reasoning
behind the whole method is wrong.

| # | Obstacle | Loc | Stands at | Expected landing |
|---|---|---|---|---|
| 1 | Ladder | `varlamore_wyrm_agility_start_ladder_trigger` | 1652,2931,0 | 1654,2928,1 |
| 2 | Tightrope + ledge | `varlamore_wyrm_agility_balance_1_trigger` | 1655,2925,1 | 1649,2910,1 |
| 3 | Tightrope | `varlamore_wyrm_agility_basic_balance_1_trigger` | 1646,2910,1 | 1635,2910,1 |
| 4 | Rope + ledge | `varlamore_wyrm_agility_basic_monkeybars_1_trigger` | 1631,2910,1 | 1627,2931,1 |
| 5 | Ladder | `varlamore_wyrm_agility_basic_ladder_1_trigger` | 1626,2932,1 | same tile, plane 2 |
| 6 | Zipline | `varlamore_wyrm_agility_end_zipline_trigger` | 1626,2933,2 | 1650,2931,0 |

Steps 2 and 4 each swallow a ledge that has no op in the cache, so they cover more ground than one
obstacle normally would and pay double. **Check nothing is left un-run between them**: if live wants
the player to walk the ledge themselves, the long landing will look like a teleport through
scenery.

A lap pays 601.6. Termites and blessed bone shards are not modelled at all, so nothing should drop.

## Colossal Wyrm, advanced

Shares steps 1-2 above, then:

| # | Obstacle | Loc | Stands at | Expected landing |
|---|---|---|---|---|
| 3 | Ladder | `varlamore_wyrm_agility_advanced_ladder_1_trigger` | 1648,2909,1 | same tile, plane 2 |
| 4 | Edge | `varlamore_wyrm_agility_advanced_jump_1_trigger` | 1646,2907,2 | 1635,2907,2 |
| 5 | Tightrope + rope | `varlamore_wyrm_agility_advanced_balance_1_trigger` | 1633,2908,2 | 1624,2931,2 |
| 6 | Zipline | `varlamore_wyrm_agility_end_zipline_trigger` | 1626,2933,2 | 1650,2931,0 |

The zipline is shared with the basic course and pays 662 here against 341.2 there, so **run a lap of
each and confirm the zipline pays the right one both times.** That is the shared-loc dispatch doing
its job; getting it backwards is the most likely way this change is wrong in a way tests miss.

## Ape Atoll

Needs a small or medium ninja greegree or the Kruk greegree wielded. Landings here are relative, so
they follow the direction the player approaches from rather than fixing a tile.

| # | Obstacle | Loc | Stands at | Expected landing |
|---|---|---|---|---|
| 1 | Stepping stone | `100_ilm_stepping_stone` | 2754,2742,0 | 2 tiles west |
| 2 | Tropical tree | `100_ilm_climbable_tree` | 2753,2741,0 | same tile, plane 2 |
| 3 | Monkeybars | `100_ilm_monkeybars_start` | 2752,2741,2 | 6 tiles west, plane 0 |
| 4 | Skull slope | `100_ilm_cliff_climb_1` | 2746,2741,0 | 2 tiles west |
| 5 | Rope | `100_ilm_rope_swing` | 2752,2731,0 | 4 tiles east |
| 6 | Tropical tree | `100_ilm_agility_tree_base` | 2757,2734,0 | 5 tiles north |

**Step 3 is the doubtful one.** The monkeybars sit on plane 2 and the skull slope on plane 0 with
nothing between them in the cache, so the drop at the end of the bars is inferred from that gap
alone. Steps 4-6 are the weakest of the six courses: the slope, the rope and the final tree are far
enough apart that the distances are guesses at how far each obstacle carries you.

Also confirm the greegree gate: unequip it and the message should be *Only the stealthiest and most
agile monkey can use this!* rather than silence.

## Werewolf

Needs Creature of Fenkenstrain. Enter through the trapdoor south-east of Canifis.

| # | Obstacle | Loc | Stands at | Expected landing |
|---|---|---|---|---|
| 1 | Stepping stone | `werewolf_steping_stone` | 3538,9874,0 | 3540,9882,0 |
| 2 | Hurdle x3 | `werewolf_hurdle_mid` | 3539,9892,0 | 3 tiles north, each |
| 3 | Pipe | `waa_pipe` | 3538,9904,0 | 5 tiles north |
| 4 | Skull slope | `werewolf_skull_climb_1` | 3532,9910,0 | 3 tiles west |
| 5 | Zip line | `werewolf_slide_center` | 3528,9911,0 | 3543,9880,0 |

**Step 5 has nothing behind it.** The deathslide landing was picked to close the lap beside the
stepping stones and is the one tile in the whole change with no evidence at all. Whatever
`get_client_state` reports after riding it in live is the answer.

Step 2 is three separate jumps against one obstacle, so the lap needs all three before the pipe
counts. Any of the three lanes should work; the middle is what players use.

A lap pays 350, not the wiki's 730 - the missing 380 is the stick hand-in, which is not wired.
Confirm no stick spawns and the Agility Trainer says nothing, so the gap is visibly absent rather
than half-working.

## After the pass

Correct the landings in `AgilityCourses.kt`, then update the "Where the numbers and the tiles came
from" section of `docs/plans/agility.md`: it currently says the landings are reasoned, and once they
are observed that stops being true.
