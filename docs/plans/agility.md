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

## Courses out, and what they need

Both are ordinary rooftop courses, so the only thing stopping them is the **landing tile** of each
obstacle - where the player ends up after crossing. The loc symbols and xp below are settled; walk
each obstacle in a client, `::mypos` where you land, and the course drops straight into
`AgilityCourses.courses`.

### Pollnivneach Rooftop Course, level 70, 1016 xp a lap

| # | Obstacle | Loc | Xp | Obstacle tile |
|---|---|---|---|---|
| 1 | Basket | `loc.rooftops_pollnivneach_basket` | 10 | 3351, 2962 |
| 2 | Market stall | `loc.rooftops_pollnivneach_marketstall` | 45 | 3349, 2970 |
| 3 | Banner | `loc.rooftops_pollnivneach_hangingbanner` | 65 | 3356, 2978 |
| 4 | Gap | `loc.rooftops_pollnivneach_gap` | 35 | 3363, 2976 |
| 5 | Tree (jump-to) | `loc.rooftops_pollnivneach_tree` | 75 | |
| 6 | Rough wall | `loc.rooftops_pollnivneach_wallclimb` | 5 | 3365, 2982 |
| 7 | Monkeybars | `loc.rooftops_pollnivneach_monkeybars_start` | 55 | 3358, 2985 |
| 8 | Tree (jump-on) | `loc.rooftops_pollnivneach_treetop` | 60 | |
| - | Lap | | 666 | |

### Prifddinas Agility Course, level 75, 1315.2 xp a lap

Twelve obstacles, no marks of grace. Symbols are the `prif_agility_*` family: `start_ladder`
(36221), `tightrope1` (36234), `chimney_jump` (36227), `roof_jump` (36228), `dark_hole_active`
(36229), `rope_bridge1` (36233), `rope_bridge2` (36235), `tightrope2` (36236),
`balancing_rope` (36239) and `dark_hole_end` (36238). Which symbol is which step, and every landing,
needs the same client pass.

## What the sibling servers hold

Both reference servers under `D:\Documents RSPS SERVER` were mined for the missing pieces on
2026-09-14. Neither has a rooftop course beyond the eight already ported, so Pollnivneach and
Prifddinas still need the client pass above.

**Kronos (rev 184)** is the source the existing courses came from and the only one whose loc ids
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
- **Agility shortcuts.** None of the roughly fifty shortcuts across the map are wired. They are not
  courses - loc, level, landing on the far side, and they work in both directions - so they want
  their own table rather than a `Course` each.
- **Agility Pyramid, Brimhaven Arena, Werewolf, Penguin, Colossal Wyrm, Hallowed Sepulchre.** All
  absent. The first two are the ones players expect alongside the courses above. Ape Atoll (48),
  Shayzien and Dorgesh-Kaan are missing too.

The giant squirrel does roll: every course carries its own base and a completed lap rolls
1 in `base - level * 25`, the same formula the heron uses. It lands in the inventory, because
nothing in the server spawns a pet as a follower yet.
