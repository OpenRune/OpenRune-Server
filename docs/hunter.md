# Hunter

How hunter creatures are modelled: the shared data tables every technique reads,
the catch-rate model, and per-technique notes on where each number came from,
what is a guess, and what is deliberately not modelled.

Sources are the OSRS wiki (pages pinned by `oldid` where a number was
transcribed) and the decoded client cache. Where a value has no published
source, the technique's section says so and explains the guess.

## Module layout

`content/skills/hunter` holds the gameplay code; its `pack` submodule declares
the dbtables the creature data packs into. Techniques are deliberately
independent of each other — rules they share live as top-level declarations in
`HunterShared.kt`, not as members of any one technique.

## Creature tables

Each technique's creatures are rows of a dbtable declared in `HunterTables.kt`.
Columns 0–7 are shared verbatim by every creature table — npc, level, xp,
success_low, success_high, caught_items, caught_min, caught_max — so a creature
row means the same thing whichever table it came from, and per-technique
columns all start at 8.

- Every `npc` and `obj` is a cache symbol confirmed via `config/npc` /
  `config/obj` lookups, never a wiki name transcribed directly — the two
  frequently differ (the wiki's "Crimson swift" is `npc.hunting_bird_jungle`).
- XP is stored ×10 so the fractional values the wiki quotes survive an int
  column; the content side divides by ten once, at the point it awards.

### Column ids must form a dense 0..n-1 set, per table

The gameval encoder writes a table's columns sorted by id and never writes the
id itself, just a name per column; on read, each `dbcol` is assigned its
ordinal purely from read position — a counter starting at 0, incremented per
column. Leave a gap in the ids and the two numbering schemes desync: every
ordinal past the gap resolves one column too low, the highest id has no ordinal
left to reach it and is silently dropped, and the pack still reports `BUILD
SUCCESSFUL` with no diagnostic. Ids are per-table, so sharing 0–7 across tables
is safe; numbering per-technique columns from a common base above the shared
block is exactly how a gap would get introduced, which is why they are declared
nested per table in `HunterTables`.

## Catch rates

Creatures carry a `(success_low, success_high)` pair interpolated by
`SkillingSuccessRate` against the player's Hunter level, with `maxLevel = 99`.
That constant is not a "max hunter level" rule: it is the scale of the
published catch-rate charts every pair was read from or fitted to, which run
from level 1 to 99. Where a pair was fitted or guessed rather than published,
the technique's section below says which and why.

## Trap cap

`TrapLadder` transcribes the "Multiple traps" table on the wiki's *Pitfall*
page (oldid=15201220): one trap below level 20, then 2, 3, 4 and 5 at levels
20, 40, 60 and 80, read from the effective (boostable) level. A technique whose
published cap table disagrees keeps its own ladder rather than reusing this
one — crab trapping's starts at 2 and has no below-20 rung, because its lowest
site needs level 21; folding it in would grant a rung its source does not have.

## Randomness

A fixed reward quantity (`first == last` in `rollQuantity`) consumes no random
draw at all. This is load-bearing, not an optimisation: the unit tests script
the RNG as a fixed sequence of draws, so an unconditional draw for a flat
quantity would shift every subsequent roll and change what the next one
returns.
