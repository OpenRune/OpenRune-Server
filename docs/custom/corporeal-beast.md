# Corporeal Beast

Status: test candidate, not accepted in-game. Branch: feature/corporeal-beast.
Accepted baseline: main 0dbd0110a (Kraken and fang crafting merged).

## Encounter

Native map NPCs in both public and ironman lairs use the boss DSL and a scoped
controller. Melee and three magic variants, delayed native hit processing,
dodgeable split splash, periodic stomp, Dark energy core hops/drain/healing and
poison slowdown are implemented. Boss death cancels pending effects and removes
the core; normal death rewards, pet, kill count and respawn services are retained.
The fork's 34-tick other-boss respawn policy remains unchanged.

Existing combat formula corpbane checks handle damage reduction once, including
stab-style checks; the encounter does not halve those hits again. Stat reductions
from specials feed the native accuracy calculations and melee maximum. Drained
stats recover one point each 20 ticks; empty lairs heal 25 HP each 7 ticks and
fully heal after 300 ticks without a damaging hit. Eight or more players trigger
additional crowd healing.

## Access and test commands

- `::testcorp`: administrator teleport to the pre-lair, with ironman routing at combat 90.
- Passage Go-through and Peek: normal entry/exit and player count.
- `::testloot corp [1-1000]`: native death-kill hooks, default 100 rolls. Aliases
  `corporeal` and `corporeal_beast` are accepted. Like existing NPC loot testing,
  this is real loot and may trigger kill hooks; use a test character.

## Loot and cache

The existing regular table is retained. A missing sigil pre-roll is added:
1/585, then elysian 1/7, spectral 3/7, arcane 3/7. Existing tertiary drops remain.
A dedicated pack corrects Corp's missing ranged defence bonuses and melee attack
bonus, poison immunity, elemental weakness and death animation. Other NPCs and
accepted encounters are preserved. A before/after audit of all 16,577 NPC definitions
changed only NPC 319; map and animation archives are unchanged.

## References and validation

Mechanics: https://oldschool.runescape.wiki/w/Corporeal_Beast/Strategies
Drops/stats: https://oldschool.runescape.wiki/w/Corporeal_Beast
Animations and locations come from this fork's revision-240 cache.

Collision probe confirms lobby (2966,4254,2), passage outside (2970,4254,2),
inside (2976,4254,2), and corresponding ironman tiles 128 squares north are clear.
Tests cover native incoming impacts, prayer reduction, leaving while a projectile
is airborne, stomp, core lifecycle, recovery and splash geometry.

Automated validation: 13 Corp tests and 16 commands tests pass, including actual
HP subtraction, core healing, poison/hop reset, pending-hit cancellation and loot
command aliases/counts. The cache builds successfully. The Wilderness exit
(3206,3683,0) is also collision-checked.

Still to validate in-game: animation timing and complete kill/re-entry cycles. Paid clan instances and Combat Achievement bookkeeping are
separate systems and are not claimed complete by this encounter implementation.

Implementation timing choices awaiting visual comparison: 10% chance of a
three-tick attack, a 100-tick poisoned-core drain interval, and a six-point splash
spread. Published mechanics establish the behaviours but not these exact values.

Full server JAR build passes. Isolated server startup, catalogue bridge request
and graceful shutdown pass against the candidate cache and JAR. Installer:
`outputs/corporeal-beast-update-20261004/INSTALLEREN.cmd`, based on the installed
`kraken-fix-20261004` package, with a software-only rollback checkpoint.
