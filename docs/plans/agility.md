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

Draynor is level **1**, not 10: Project Rebalance dropped the requirement in May 2024.

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

Both reference servers beside this repo were mined for the missing pieces on
2026-09-14. Neither has a rooftop course beyond the eight already ported, so Pollnivneach and
Prifddinas still need the client pass above.

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

**Kronos forks on GitHub have the two courses the local copy lacks.** `TuringProblem/Okronos` and
`tamerab1/Zelus-server-website-monorepo` both carry `PollnivneachCourse.java` and
`PrifddinasCourse.java` in the same `io.ruin` package the local Kronos uses, landings and mark
spawns included. That is where the tiles for those two courses came from, with the xp taken from
the wiki where the two disagreed. `Fludem/dylscape` is an RSMod fork with four rooftops in a DSL
close to this one, useful for cross-checking a landing but not for new coverage.

This cache is a better source than either server for the missing content: it carries the whole
`agility_pyramid_*` family (gaps, ledges, climbing rocks, jump hotspots, doors) and 237 symbols with
`shortcut` in the name. Level and xp for each of those come off the wiki; the landing tiles are the
only part no source has.

## Mechanics still missing

- **Failing an obstacle.** Live rooftops still fail below certain levels, dropping the player to the
  ground for damage; Project Rebalance removed the failure on some obstacles but not all. Nothing
  here can fail, so a lap is never interrupted. Each failable obstacle needs its own fall tile, so
  this is another client survey.
- **Multi-stage obstacles.** One obstacle is one animate, wait, land. Live chains several hops
  across a tightrope or a set of stepping stones. Cosmetic; the upgrade path is stages on
  `Obstacle`, not a different model.
- **Agility shortcuts.** 164 are wired: 157 from `agility-shortcuts.tsv` and seven more the table
  has no tiles for. Each row is an obstacle's op with the exact tile a player stands on and the
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

- **The special courses.** Agility Pyramid, Brimhaven Arena, Werewolf, Penguin, Colossal Wyrm,
  Hallowed Sepulchre, Ape Atoll (48), Shayzien and Dorgesh-Kaan are all absent. Each is a course
  with its own mechanic - tickets, moving blocks, traps, a timer - rather than another list of
  obstacles, and no source carries landings for them the way the rooftops turned out to.
- **Prifddinas portals.** One of six portals spawns per lap as a shortcut worth 82 xp; the course
  runs without them.

The giant squirrel does roll: every course carries its own base and a completed lap rolls
1 in `base - level * 25`, the same formula the heron uses. It lands in the inventory, because
nothing in the server spawns a pet as a follower yet.
