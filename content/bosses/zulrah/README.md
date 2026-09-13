# Zulrah

Zul-andra travel, private islands, four standard rotation sequences, snakelings,
clouds, tail attacks, drops, kill counts and personal bests. Combat uses
`BossSpec`, `BossCombat` and the existing boss DSL. Instance-specific effects
use the existing extension registry, world queues and hit APIs.

## Layout

- `src/main/kotlin`: encounter, interaction and lifecycle scripts.
- `src/main/resources/zulrah/recorded-routine.json`: public OSRS recording fixture.
- `pack/`: native `PluginPack` module and inherited NPC configuration.
- `src/test/kotlin`: encounter, cache, lifecycle and tracking regression tests.

The pack follows `content/skills/fishing/pack`. It supplies
`param.attack_melee = 120` on `npc.snakeboss_minion_melee`; other NPC fields are
inherited. The magic snakeling is unchanged. Melee accuracy reads the definition
through `NvPMeleeAccuracy`. No custom NPC ids, maps, interfaces or clientscripts
are introduced.

## Integration

The encounter depends on the existing instance and boss APIs. Supporting changes
add optional loot coordinates/lifetime/remains suppression to the death API,
silent NPC attack denials, pending-instance protection, an NPC-attributed instant
hit overload, and an independent movement-delay clock for the tail stun.
Existing callers keep their defaults.

The Kotlin drop table in `content/drops` replaces the old Zulrah TOML table;
do not register both. Kill count uses the permanent `total_snakeboss_kills` varp.
Personal bests use persistent tick attributes and the Collection Log's native
transmit fields. Ring equipment, ring boss logs and mining changes are outside
this module's scope.

## Build and test

Use Java 21 and the repository's provisioned cache and gameval data:

```sh
./gradlew :or-cache:buildCache --max-workers=1
./gradlew :content:bosses:zulrah:zulrah-pack:test :content:bosses:zulrah:test :content:drops:test :api:death:test :content:interfaces:collection-log:compileKotlin --max-workers=1
```

Rebuild the cache before running the encounter so the packed melee bonus is
available. Cache-dependent tests must run, not skip.

For a gameplay check, use a Zul-andra teleport scroll and board the sacrificial
boat. Check island isolation, each rotation, hazard damage, snakeling deaths,
tail dodges/stuns, owner-only loot, a nearby Read exit and cleanup after death,
logout or teleport. Confirm one kill credit and persisted PB after relogging.
Check Collection Log refresh without relogging as well.

## Release limitations

Entry is currently administrator-only. Quest/first-visit access, death-item
retrieval and combat achievements are not complete. Pet and elite-clue awards
remain disabled pending their supporting systems. These are release gates, not
features implied by a successful build.

Some timings and hazard placements are provisional reconstructions. This is not
recovered Jagex source or fully verified normal-world OSRS parity. See
[sources and mechanics](EVIDENCE.md), [rotation coverage](ROTATION_EVIDENCE.md)
and [drop structure](DROP_EVIDENCE.md).
