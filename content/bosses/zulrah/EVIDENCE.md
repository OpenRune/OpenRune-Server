# Zulrah sources and verification boundaries

## Sources

- [OSRS Wiki strategies, revision 15341713](https://oldschool.runescape.wiki/w/Zulrah/Strategies?oldid=15341713):
  standard phase order and action counts, not a tick-accurate server trace.
- [RSProx 3276](https://rsprox.net/download/3276), revision 234.1, Grid Master:
  encounter fixture, projectiles, map layout and observed cleanup.
- [RSProx 820](https://rsprox.net/download/820) and
  [3342](https://rsprox.net/download/3342): additional map/death/message observations.
  These are special-mode recordings, not normal-world timing proof.
- [OpenRune revision-240 definitions](https://openrune.dev/diff/full): native
  assets, checked against the provisioned cache. Definitions do not establish
  server behaviour or prove every subrevision matches.
- [Snakeling revision 15298799](https://oldschool.runescape.wiki/w/Snakeling?oldid=15298799):
  melee attack bonus 120 for NPC 2045; magic NPC 2046 retains melee bonus zero.
- [Clientscript dumps at 1c258b4](https://github.com/Joshua-F/osrs-dumps/tree/1c258b475480e5363e3895e7acda05f41ba89432/script):
  native tick formatting and Collection Log fields.

The routine JSON retains its source URL, revision, mode and decompressed BIN
SHA-256. No raw recording, client or cache assets are part of this module.

## Map and travel

3276 rebuilds at ticks 8314 and 9085, and 47 rebuilds in 820, share a 9-by-8
chunk layout on each of planes 0 and 1. The central six chunks start at
`(2256,3064)`, `(2256,3072)`, `(2264,3064)`, `(2264,3072)`, `(2272,3064)` and
`(2272,3072)` with no rotation. Remaining chunks repeat `(2272,3056)` with
recorded rotations. The island preserves this 144-chunk map, not a contiguous
copy of the surrounding world region. Surrounding-rotation probabilities are
not established; the implementation uses the first recorded layout.

Arrival translates to source tile `(2268,3068,0)` and the first north boss anchor
to `(2266,3073,0)`. Subsequent boss anchors are instance-relative offsets:
north `(0,0)`, east `(10,-2)`, south `(0,-11)`, west `(-10,-2)`.

The teleport scroll uses the Wiki destination centre `(2196,3056,0)`.
Boat display forms 46241/46242 use existing interaction hooks. Cached maps,
animations, rowing text and camera APIs are retained; complete first-visit
dialogue, music and production quest access still need integration.

## Combat

All forms use the existing 500-HP NPC definitions. DSL abilities sequence native
animations and encounter extensions. Projectile hits use the standard accuracy
and hit modifiers, including launch-time protection checks. Incoming hits above
50 are capped to a random 45-50 result. Hazard ownership is instance-local.

- Clouds use object 11700, a 3-by-3 footprint, and one environmental damage roll
  per occupied game tick. The uniform 1-4 roll is provisional. The venom hitsplat
  does not apply venom status. Overlaps do not multiply damage. Matched cloud
  add/remove pairs in 3276 last 30 ticks unless boss death removes them early.
- Snakelings use native melee/magic forms, three-tick attack cadence, and native
  death sequences. A 67-tick natural expiry is provisional. Boss death kills
  remaining minions. Per-instance pursuit prevents generic overworld wandering.
- Crimson attacks capture one true tile at wind-up and resolve four ticks later
  for 20-30 typeless damage. The single-tile mask, impact delay, line-of-sight rule
  and five-tick movement/attack stun remain provisional. These are not proven
  by the Wiki's dodge-distance advice. Food and prayer handling are not locked.
- The stun uses native body sequence 848 and spot effect 80, with the generic
  red stun message. RSProx 1764, revision 227.1, tick 34715 establishes the message
  for Bloat, not this Zulrah-specific combination. Effect height 100, slot 2 and
  red presentation still require direct Zulrah verification.

Tail sequence/facing updates in 3276 at ticks 8441/8448, 8689/8696,
8798/8806 and 9207/9214 support fixed facing during a swing, not a verified
general left/right selection rule. No unambiguous tail damage/stun measurement
was established from those windows.

Detailed captured/reconstructed rotation coverage is in [ROTATION_EVIDENCE.md](ROTATION_EVIDENCE.md).

## Death and tracking

In 3276, death sequence 5804 begins at tick 9382; loot, exit and the kill message
appear at 9388. The implementation waits six ticks, then resolves native loot
and kill hooks before NPC deletion. Only the surviving instance owner can claim
completion, once. Interrupted/departed encounters cannot award delayed records.

Counter varp 1518 is permanent; Collection Log struct 505 refers to it. PBs are
stored as ticks using the existing attribute save layer. Observed timer rows:

| Recording | Spawn | Death animation | Message | Displayed seconds |
| --- | --- | --- | --- | --- |
| 3342 | 3805 | 3820 | 3826 | 7 |
| 3342 | 3851 | 3870 | 3876 | 10 |
| 3276 | 9088 | 9382 | 9388 | 175 |
| 820 | 10362 | 10404 | 10410 | 23 |

These fit rounding `(death - spawn - 3) * 0.6`; the implementation starts three
ticks after spawn and stops at death-animation start. This boundary is an
inference, not proof of internal server start/stop points. Native scripts
2401/2410/2411 guide rounding, precise timing and hour formatting.

Read object 11701 is separate from loot in 820 at ticks 10410 and 10473. Loot
stays on the owner's tile; Read selects a walk-clear neighbour, with a second
radius fallback. Uniform selection and that fallback are provisional. Exact
normal-world placement probabilities and immediate Collection Log PB refresh
still need live-client comparison.
