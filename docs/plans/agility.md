# Agility

What the module covers, what it does not, and the exact data the missing pieces need.

Everything numeric in `content/skills/agility` now comes from the wiki. Each course's obstacle xp
plus its lap bonus adds up to the wiki's total for a lap, and `AgilityCourseTest` asserts that, so a
mistyped number fails the build rather than quietly paying the wrong xp.

## Courses in

| Course | Level | Xp per lap | Marks | Squirrel base |
|---|---|---|---|---|
| Gnome Stronghold | 1 | 110.5 | none | 35,609 |
| Draynor Village Rooftop | 1 | 120 | 2/6 | 33,005 |
| Al Kharid Rooftop | 20 | 216 | 2/6 | 26,648 |
| Varrock Rooftop | 30 | 269.7 | 2/6 | 24,410 |
| Barbarian Outpost | 35 | 153.3 | none | 44,376 |
| Canifis Rooftop | 40 | 240 | 2/3, no level penalty | 36,842 |
| Falador Rooftop | 50 | 586 | 2/6 | 26,806 |
| Wilderness | 52 | 571.4 | none | 34,666 |
| Seers' Village Rooftop | 60 | 570 | 2/6 | 35,205 |
| Pollnivneach Rooftop | 70 | 1,016 | 2/6 | 33,422 |
| Prifddinas | 75 | 1,285.2 | none | 25,146 |
| Rellekka Rooftop | 80 | 920 | 2/5 | 31,063 |
| Ardougne Rooftop | 90 | 889 | 2/3 | 34,440 |
| Shayzien Basic | 1 | 153.5 | none | 31,804 |
| Shayzien Advanced | 45 | 508 | none | 29,738 |
| Colossal Wyrm Basic | 50 | 601.6 | none | none published |
| Colossal Wyrm Advanced | 62 | 1,053.6 | none | none published |
| Ape Atoll | 48 | 580 | none | 37,720 |
| Werewolf | 60 | 350 of 730 | none | 32,597 |

Draynor is level **1**, not 10: Project Rebalance dropped the requirement in May 2024.

The six courses below the rooftops were added on 2026-09-14. They carry no marks of grace because
nothing records the tiles a mark lands on at any of them; the rest of the mark machinery is course
data, so adding the spawns is all each would need.

**Werewolf pays 350 of its 730.** The missing 380 is the bonus for handing the stick to the Agility
Trainer, which wants the stick to spawn on the course and the trainer to take it. `AgilityCourseTest`
carries that shortfall as a named deduction rather than a wrong total, so wiring the stick is a
matter of deleting the entry.

**Neither Colossal Wyrm course rolls the squirrel.** Its rate changed on 19 August 2026 and the wiki
has published no new base, so both sit at zero rather than at an invented number.

### Basic and advanced pairs

Shayzien and Colossal Wyrm are each two courses that run the same obstacles and then split, so one
loc belongs to two courses. Obstacles are registered once per loc and the handler picks the course:
the lap already in progress wins, otherwise the hardest variant the player's level and gear allow.
Taking a branch moves the lap onto that branch, but only where the two courses really are the same
lap so far - every step before the split has to be the same loc - so a part-finished lap of one
course can never be cashed in on another.

### Where the numbers and the tiles came from

Levels, xp and obstacle order are the wiki's. **The tiles are the cache's**, read with
`gradlew :or-cache:dumpLocs -Plocs=<loc or id list, or x1:z1:x2:z2>`, which prints every tile a loc
is placed on with its plane, angle, name and ops. That closes the gap the earlier survey called out:
no other source records where an obstacle stands.

Colossal Wyrm is exact rather than reasoned. Its termite spawn markers (`*_termites`) sit on the tile
each obstacle drops the player on, because live scoops a termite up "upon completing the preceding
obstacle", so the landings after the first are read straight off the map. **The start ladder is the
exception**: the first marker sits past the tightrope, so the ladder had no evidence behind it and
was guessed wrong - it skipped the first platform and dropped the player on the second, which an
in-client pass caught. It now lands at 1653,2931 on the deck directly above the ladder. Elsewhere the obstacle
tiles are exact and the landings are derived from them: relative where the direction of travel is
unambiguous, absolute where the cache pins the far end. **The Werewolf deathslide is the one landing
with no evidence behind it** - it is placed back beside the stepping stones so the lap closes.

**All of it was then run in a client** - see `docs/agility-verification.md`. Every obstacle on
Shayzien, Colossal Wyrm and Werewolf binds, lands where it should and pays exactly the wiki's xp, and
the shared-loc dispatch was confirmed both ways. Ape Atoll could not be run: its greegree gate works,
but the greegree cannot be equipped on this server because the Monkey Madness transform content does
not exist.

### How a crossing is built

An obstacle is a list of [Stage]s, because a real one is three linked animations rather than one:
climb on, travel, drop off. The cache names them that way - `monkeybars_on` / `monkeybars_walk` /
`monkeybars_off`, `wyrm_agility_ledge_on` / `_walk` / `_off` - and every course here uses its own
set where the cache ships one. Four rules decide how a stage behaves, and all four came out of
watching laps rather than reading data:

- **A traverse lasts as long as its distance**, one tile per tick, not a fixed guess. `dumpSeqs`
  gives each animation's real length so the two stay in step.
- **`anim` plays a sequence once.** A crossing outlasts one pass of its animation, so the stage
  restarts it whenever it would run out. Without that the player hangs from a zipline for two ticks
  and then slides the remaining six with no animation at all, which reads as floating.
- **A jump travels in one tick and then holds still** (`moveTicks`) while the rest of its animation
  plays the landing. Spreading the movement over the whole animation makes the player drift after
  they have already come down.
- **A slide is a straight line, so every corner is a waypoint.** The scaffolding an obstacle crosses
  rarely runs straight, and a crossing that cuts the corner travels through open air.

### Where a crossing starts and ends

Two mistakes account for nearly every visual fault found in a client, and both are about tiles
rather than code:

- **A crossing starts wherever the engine parked the player**, which is beside the rope as often as
  on it, and the whole traverse then runs parallel to the real rope. Obstacles step onto their own
  line first (`mount`).
- **A landing has to be a tile that is actually built.** `Landing(level = n)` puts the player on the
  tile directly above, which on scaffolding is usually open air. Every landing here is a decking tile
  read out of the cache with `dumpLocs`, and the Colossal Wyrm zipline lands on the course's own
  `Landing Spot` loc (55173) rather than a tile that looked about right.

A crossing also can never end part-way. It passes over isolated planks with nothing walkable around
them, so the stage loop is wrapped: whatever interrupts it, the player finishes on the far side
instead of stranded in mid-air.

### What the engine cannot do

`EntityExactMove` carries x and z deltas, two delays and an angle - **no height**. Nothing can draw a
player rising, so a ladder plays its animation and goes up the way every other ladder in the game
does (see `LadderScript`), and a zipline has to ride on the plane it starts from and drop at the end,
or the client draws the whole ride along the ground. Giving that protocol a height component is an
engine change touching every ladder and staircase in the game, not an agility one.

Two obstacles on each Colossal Wyrm course have no op at all: live walks the player along the ledge
automatically. Each is folded into the obstacle before it, xp included, so a lap still totals what
the wiki says.

A merged obstacle turns a corner - south along a rope, then west along a ledge - and a slide is a
straight line, so `Obstacle.via` names the corner and the crossing is travelled as two legs. The
corners are the termite markers again: there is one per real obstacle, which is exactly the turn the
merge flattens. Without it the player stood still playing a walking animation and then teleported,
which is what the merge looked like before it was noticed in a client.

The ledges still are not separately clickable, because they genuinely have no op. Doing that
properly needs walktrigger support - the same thing the Penguin crushers want.

The wiki contradicts itself on the Colossal Wyrm basic total: the obstacle table adds to 601.6 and
says so, the prose says 633. The table is used, being internally consistent and current with the
12 August 2026 change.

### Marks of grace

A completed lap rolls for a mark only when three minutes have passed since the last one spawned,
which is why `varp.agility_mark_clock` stores the minute the last mark landed - it has to survive a
logout the way the live timer does. Twenty levels above the course requirement the roll is 80%
weaker, except at Canifis, where live never applies the penalty. Marks lie on the roof for ten
minutes and only the player who earned one can see it.

Not modelled: the diary rerolls (Kandarin at Seers', elite Ardougne) and the diary cooldown
reductions at Pollnivneach, Rellekka and Ardougne.

## Failing an obstacle

Project Rebalance took the failure out of most rooftop obstacles in 2024 - Draynor's tightropes and
wall-cross, the Pollnivneach banner and others all say so on their own pages. The **Pollnivneach
market stall** is the one course obstacle that still fails and still publishes a rate, so it is the
one that is wired: odds of 60 to 300 out of 256, and the live damage rule, `floor(hp / 17) + 2`,
which is a share of the hitpoints left rather than a flat hit. The Rellekka tightrope (10% of
current hitpoints) and the Falador hand holds both still fail in live but publish no rate, so they
do not here.

A failed obstacle costs the lap and the damage; live also drops the player off the roof, which this
does not, because no source records the tile each obstacle drops you onto.

## What the sibling servers hold

Both reference servers beside this repo were mined again on 2026-09-14 for the special courses.
**Neither carries one of them except the Agility Pyramid and the Brimhaven Arena**, both in
VIBESCAPE; Kronos has no course outside the rooftops, Barbarian Outpost and Wilderness. Ape Atoll,
Werewolf, Penguin and Dorgesh-Kaan are in neither, despite all four predating VIBESCAPE's own
revision, so the cache was the only source for them.

**Kronos (rev 184)** is the source most courses came from and the only one whose loc ids
still resolve: every id in its Barbarian Outpost and Wilderness files matches both this cache and
the wiki's own infoboxes, which is why those two courses could be added from it. What is left there
is not worth taking - its mark of grace roll is `levelReq / 200` with a donator-rank bonus, and its
pet roll is a flat 1/22,000, both of which the wiki's real rates have replaced.

**VIBESCAPE** is a 2009-era (rev 530) codebase, so it predates rooftops entirely. It does carry the
whole shortcut category, an Agility Pyramid course with every gap, ledge and crossing coordinate,
and a Brimhaven Arena - none of which exist here. Its coordinates are still good, because the map
has not moved. **Its loc ids are not.** Of 45 agility ids checked against this cache, 12 survive:

| Still the same obstacle | Now something else |
|---|---|
| 993, 3730, 7527, 12982, 19222, 22302 - stiles | 9300 "fence jump" is now `pinball_first_track` |
| 3931, 3932, 3933 - `regicide_logbalance*_start` | 2296 "log balance" is now `fai_varrock_fancy_sign` |
| 19849 - `ep_climbing_rocks01` | 2321 "monkey bars" is now a Varrock inn bar |
| 2231 - `zqclimbingrocks` | 11844 "Falador crumbling wall" is now a TzHaar door |
| 20210 - `agility_obstical_pipe_barbarian` | 29370, 29375 are now Yama league scenery |

So anything ported from VIBESCAPE has to have its ids re-resolved against `gamevals` by name first;
taking them at face value silently wires an obstacle to a signpost. The coordinates, levels and the
shape of each obstacle are what it is worth reading for.

**Its Agility Pyramid is the exception and was re-checked on 2026-09-14: every id and every tile in
`AgilityPyramidCourse.java` still resolves to the same obstacle in this cache.** That file is worth
porting rather than rewriting.

**Kronos forks on GitHub have the two courses the local copy lacks.** `TuringProblem/Okronos` and
`tamerab1/Zelus-server-website-monorepo` both carry `PollnivneachCourse.java` and
`PrifddinasCourse.java` in the same `io.ruin` package the local Kronos uses, landings and mark
spawns included. That is where the tiles for those two courses came from, with the xp taken from
the wiki where the two disagreed. `Fludem/dylscape` is an RSMod fork with four rooftops in a DSL
close to this one, useful for cross-checking a landing but not for new coverage.

This cache is a better source than either server for the missing content, and since
`gradlew :or-cache:dumpLocs` it is a better source for the landing tiles too - the one part the
earlier survey had written off as needing a client. It prints every tile a loc stands on, by symbol,
by id, or by box:

```
gradlew :or-cache:dumpLocs -Plocs=loc.werewolf_steping_stone,11644
gradlew :or-cache:dumpLocs -Plocs=1505:3615:1560:3650
```

Each row is the loc's tile, plane, angle, shape, cache name and ops, which is enough to lay out a
course without logging in. Level and xp still come off the wiki.

## Mechanics still missing

- **Failing an obstacle.** Live rooftops still fail below certain levels, dropping the player to the
  ground for damage; Project Rebalance removed the failure on some obstacles but not all. Nothing
  here can fail, so a lap is never interrupted. Each failable obstacle needs its own fall tile, so
  this is another client survey.
- **Multi-stage obstacles.** One obstacle is one animate, wait, land. Live chains several hops
  across a tightrope or a set of stepping stones. Cosmetic; the upgrade path is stages on
  `Obstacle`, not a different model.
- **Agility shortcuts.** 174 are wired: 166 from `dbtable.agility_shortcut` and eight more the
  table has no tiles for. Each row is an obstacle's op with the exact tile a player stands on and the
  tile it puts them on, so climbs, tunnels, stepping stone chains and the 23 crossings that change
  plane all work without a survey. Where a player uses an obstacle from a tile the table does not
  list, the landing falls back to the derived crossing: straight through the loc, first standable
  tile past its footprint.

  The tiles come from the shortest-path RuneLite plugin's own dataset (BSD 2-Clause), levels from
  the same, and xp from the wiki matched by loc id - 36 of the 157 award any. Every loc id was
  resolved to a gameval symbol at generation time, so no raw id is in the file, and 116 rows for
  locs the courses already bind were dropped rather than double-registering them.

  Requirements ride on the link rather than the shortcut, because one loc id can be two shortcuts:
  both Catacombs of Kourend cracks are `loc.zeah_cata_crack` and both Slayer Tower spiked chains are
  `loc.slayertower_sc_chain*`, at two levels each. The dataset already separates the chains by row;
  the cracks it gives at 17 apiece, so the northern one is corrected to the wiki's 34 by an override
  keyed on the tile.

  **Grapple crossings** check a mith grapple in the quiver and any crossbow in hand, plus the Ranged
  and Strength the wiki lists, and take the barehanded alternative into account: the same gap at a
  much higher Agility level with no gear. **Quest gates** run through `QuestRequirements`, so they
  follow the realm's quest requirement mode rather than reading the quest var directly. Everything
  else - diary flags, built bridges, rope tied - is the raw comparison the data carries.

  **Failing** is in for the eleven obstacles whose wiki page publishes a success chart: the stepping
  stones at Lumbridge Swamp Caves and Karamja, both log balances, the four Trollheim rock climbs,
  two strange floors, the Taverley pipe and the Fremennik chasm. The odds are the wiki's out of 256
  through the same skilling formula as everything else, a failed attempt still pays its fail xp, and
  the Ardougne log deals its 2-6. **A failed crossing leaves the player where they started** - live
  may well drop them in the water instead, and that is the one thing here that wants a client to
  settle.

  Still out: the Champions' Guild stepping stone and the Forthos strange floor can fail in live but
  publish no chart, so they cross every time; anything whose op is Enter, Open or Use, since those
  are cave mouths and gates other content owns; and the Revenant Caves pillar jumps, which the
  dataset does not carry.

- **The four courses still out.** Each needs something the course model does not have. The ids and
  tiles below are this cache's, dumped with `dumpLocs`, so none of them needs another survey.

  **Agility Pyramid** (30, 722 + a `300 + level * 8` bonus capped at 1,000, squirrel base 9,901) is
  not a list of obstacles at all: the same loc stands five times over, once per layer, and every
  crossing moves the player along whichever way they approached it rather than to a fixed tile. The
  ids are `10865` low wall, `10859` gap jump, `10860`/`10886`-`10889` ledge, `10867`/`10868` plank,
  `10861`-`10864` and `10882`-`10885` gap cross, `10857`/`10858` stairs, `10851` pyramid top,
  `10855`/`10856` doorway, `16535`/`16536` climbing rocks. The course sits at 3354-3372, 2831-2851
  with its upper layers mirrored to 3040-3048, 4693-4701. **VIBESCAPE's
  `content/global/skill/agility/pyramid/AgilityPyramidCourse.java` is a complete working
  implementation and every id and coordinate in it matches this cache**, which the ported rooftops
  never managed - so this one is a port rather than a survey. It wants its own `PluginScript`: a
  direction-derived move, a plane drop on failure, the moving blocks as npcs, and the pyramid top.

  **Penguin** (30, 540, squirrel base 9,779 - the best in the game) is linear apart from its first
  obstacle. Stepping stones `21126`-`21133` run 2631,4057 to 2635,4065 on plane 1, icicles `21134`
  stand in four pairs from 2644,4083 to 2662,4083, and the ice `21148`-`21156` covers 2664-2666,
  4069-4077. **The crushers are npcs**, so the 55 xp for passing them is not a loc op, and the ice
  and icicles are timed multi-click chains. Also needs the clockwork suit and penguin form.

  **Dorgesh-Kaan** (70, 2,750, squirrel base 10,561) has all its obstacles in the cache - `22569`
  cable, `22572` cable swing, `22564` monkeybar ladder, `22552` jutting wall, `22557` tunnel, and
  `22589` pylons for the grapple route - but 2,432 of the 2,750 is a delivery bonus: Turgall hands
  out a spanner, the player strips a part off a generator and carries it back by the route that
  matches it. That is a quest-shaped loop, not a lap, and the grapple route pays Ranged rather than
  Agility.

  **Brimhaven Arena, Hallowed Sepulchre, Werewolf Skullball and Gnome Ball** are minigames wearing a
  course's clothes and none fits this model: tickets and a dispenser that moves every minute, timed
  floors and loot, a ball with three kick strengths, a scored pitch. VIBESCAPE carries a whole
  Brimhaven Arena (`skill/agility/brimhaven/`) with its traps if that one is wanted first.
- **Prifddinas portals.** One of six portals spawns per lap as a shortcut worth 82 xp; the course
  runs without them.

The giant squirrel does roll: every course with a published base carries it and a completed lap rolls
1 in `base - level * 25`, the same formula the heron uses. It lands in the inventory, because
nothing in the server spawns a pet as a follower yet.
