# Zulrah

Origin: CUSTOM encounter on upstream instances/combat/cache APIs. Current files:
`content/bosses/zulrah/src/main/kotlin/org/rsmod/content/bosses/zulrah/`, especially
`ZulrahEncounterController.kt` and `ZulrahBoatScript.kt`; existing drop hooks stay authoritative.

Current line entered main via `a53731b25`; debug commands via `29c4cbd0e`;
subsequent fixes are in `83c47b4e3` and `c46abc6af`. `ZulrahHalberdReachTest`
covers the accepted shoreline reach behaviour. Do not restore the older three-tile
reach or alternate encounter manager merely because an archived README describes it.

Dependencies: instances and abandonment recovery, NPC combat, collision, venom,
death/drop hooks, kill count/collection log and respawn timer/HUD integration.

The older `6a3590c24` line includes persistent item recovery, different rotations,
additional lifecycle tests and a different encounter architecture. Those are not
proven equivalent to this build. Their code is archived and remains NEEDS REVIEW.
One active implementation is retained; the archive is not loaded by the server.

Test future changes against entry, all forms/attacks, halberd reach, accessible loot,
repeat kills, exit during transitions, death, logout/relog, hazards and instance cleanup.
Boss DSL revision 241 changes require a dedicated compatibility review.
