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
  absent. The first two are the ones players expect alongside the courses above.
The giant squirrel does roll: every course carries its own base and a completed lap rolls
1 in `base - level * 25`, the same formula the heron uses. It lands in the inventory, because
nothing in the server spawns a pet as a follower yet.
