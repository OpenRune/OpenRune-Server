# Zulrah

Native Kotlin encounter using OpenRune's existing Zulrah map and assets. The module
creates a private solo instance of regions 9007 and 9008. It does not import the
recording's terrain, walls or scenery, and does not require Nero Capture Studio
at runtime.

## Enter and play

Board the sacrificial boat in Zul-Andra. The existing boat's transformed quest
variants and Quick-board option are supported. Administrators can use `::zulrah`
to enter directly and `::zulrah leave` to leave. Use the existing teleport scroll
in the arena for the ordinary exit.

Entry fades to black for 1.2 seconds and reveals the arena over 2.4 seconds.
The first spawn waits five seconds from arrival, shown by the native countdown
overlay. Server actions run on 600 ms ticks, so this delay rounds up to 5.4 seconds.
The overlay is separate from the inventory and existing boss/instance HUDs.

Zulrah has 500 hitpoints shared across all
three forms. Four rotations control the emergence locations, attacks, venom
clouds, snakelings and alternating ranged/magic phase. Player combat uses the
server's native accuracy, prayers, hit processing and venom rules. Damage, death
and rewards are calculated from the current fight, not replayed from a capture.
When a phase's scripted clouds, snakelings and attacks have finished, Zulrah keeps
using normal attacks until the dive begins instead of waiting idle. A tail attack
only starts if its windup can finish before the dive.

Halberds, noxious halberds, scythes and other melee weapons with extended native
reach can attack Zulrah from the shore. These weapons receive a three-tile melee
reach against Zulrah only, measured from the boss's footprint. Normal collision,
line-of-sight, melee accuracy and the weapon's own hit behavior still apply.

After a kill, collect the drops on the walkable arrival tile and stay for the next
fight. The respawn countdown starts after the death animation and loot processing
finish. A new Zulrah spawns ten seconds later (10.2 seconds on server ticks), with
fresh health, combat state and a newly selected rotation. Existing ground loot
remains available. The two delay settings live in `ZulrahEncounterManager`.
Hazards and snakelings are removed when the fight ends; departure, death or logout
cancels pending spawns and releases the private map through the instance lifecycle.

Other instances with native respawning bosses also show the countdown overlay,
using each boss's existing respawn deadline. Their configured respawn times remain
unchanged. Pets, minions and bosses in the ordinary world do not create this HUD.

Private instance regions stay reserved until the instance manager destroys the
session. This prevents a second entry in the same tick from reusing the first
player's map before player activity has been recorded. Failed creation releases
the reserved map and refunds any entry fee.

Items selected for loss by the server's normal PvM death rules are held in a
persistent recovery inventory. Talk to the priest in Zul-Andra to reclaim them.
Make inventory space and repeat to claim any remainder. Reentry is blocked while
items remain unclaimed. Recovery is free under this server's current balance;
the OSRS fee after 50 kills is not implemented. Another unsafe death destroys
unclaimed items. Existing keep-item, Ultimate Ironman and pet policies still
apply.

## Loot

Administrators can use `::testloot Zulrah` (100 kills by default) or
`::testloot Zulrah 10`. The name is case-insensitive and resolves to Zulrah's
ordinary native death-kill hooks, using the same configured loot table as a kill.

The existing `content/drops` Zulrah table remains authoritative. One kill grants
one scales roll, two ordinary reward rolls, and the existing separate rare and
tertiary rolls. Collection log, loot tracker and pet hooks use the normal death
pipeline. The configured quantities and rare probabilities have been preserved;
they are not claimed to reproduce Alora's hidden drop rates.

Zulrah's duplicate entries in the supplemental pet table are removed: the
existing loot-table pet roll already goes through `PetDropHook`. This keeps one
1-in-4000 pet roll per kill and leaves pet following, pickup and insurance intact.

`NpcDeathKillContext.dropCoords` forwards the requested ground tile to drop-table
hooks. Its default remains the NPC's tile for existing callers. This lets an
over-water boss drop accessible loot without moving the NPC or bypassing death
hooks.

## Recording and sources

Reference recording: `recording-20260927-234358-Zulrah` (Alora), 118 ticks and 508
events. It shows one 500-HP kill through ranged, melee and magic forms at the
north anchor, plus animations, projectiles and three reward stacks. Its runtime
coordinates map to source coordinates with `x - 7480`, `z - 11792`.

The recording does not contain every rotation, venom clouds, snakelings, entry,
player death, recovery or drop probabilities. Those parts are implemented
separately using native server APIs and the references in [NOTICE.md](NOTICE.md).
The four rotation action tables come from an independently implemented OSRS
simulator; timing and hazard placement are implementation choices, not a claim
of exact Alora server reconstruction.

## Build and verify

Use Java 21. Rebuild the cache after installing this module, before starting the
server, so the NPC overlays, instance row and recovery inventory exist:

```powershell
.\gradlew.bat :or-cache:mergePluginGamevals
if ($LASTEXITCODE) { throw 'Gameval merge failed.' }
.\gradlew.bat :or-cache:buildCache
if ($LASTEXITCODE) { throw 'Cache build failed.' }
.\gradlew.bat :content:bosses:zulrah:test :api:drop-table-plugin:test assemble
if ($LASTEXITCODE) { throw 'Build or tests failed.' }
```

With Nero OpenRune Studio, select the checkout containing this module and run
Setup to rebuild and deploy its cache and server. Then use PLAY OPENRUNE.
Importing or previewing the capture scene is unnecessary.

Before enabling this encounter for a public world, play through entry, all forms,
the alternating phase, dodging, victory, accessible loot, exit, death/reclaim and
logout in the target client. Also check halberd/scythe attacks from the shore,
successive kills without leaving, and departure during both countdowns.
Automated cache and runtime tests do not establish
the final animation appearance or live-client timing.
