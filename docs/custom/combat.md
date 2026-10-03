# Combat, shields and respawn policy

Origin: UPSTREAM + CUSTOM EXTENSIONS. `c46abc6af` adds explicit weapon/magic/shield
special dispatch; `9f38dbb1d` adds special reset. The accepted implementation is preserved.

The [special-attack audit](../special-attacks-audit-20261001/README.md) records
137 registered versus 148 still missing among 285 special-energy items, with six shield
forms separately covered. Do not label all specials complete just because the bar exists.
Shield charging, inspection and cooldowns are per-item/persistent as documented there.

Respawn policy: GWD 100 ticks = 60 seconds, other supported bosses 34 ticks = 20.4 seconds.
Encounter progression and non-respawning entities remain respected. Actual deadlines,
not guessed kill times, feed Nero. Changes to NPC death, instance cleanup or Boss DSL
need timer and duplicate-death tests as well as combat tests.

Upstream risks: target reach, autocast, shield dispatch, hit scheduling, NPC death hooks,
Boss DSL and stats/bonuses. Preserve current behaviour while evaluating any replacement.
