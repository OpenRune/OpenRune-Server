# Zulrah rotation evidence

Source: [OSRS Wiki, Zulrah/Strategies, revision 15341713](https://oldschool.runescape.wiki/w/Zulrah/Strategies?oldid=15341713).
Revision timestamp: 2026-09-12T12:42:01Z; retrieved 2026-09-13 via MediaWiki API.
This is documented
behavior, not original server source or tick-accurate packet evidence.

## Phase order

G = green, M = crimson/melee, T = tanzanite. N = middle/north anchor;
E/W/S = east/west/south. Tokens are form plus position, in execution order.

| Rotation | Initial cycle |
| --- | --- |
| 1 | GN MN TN GS MN TW GS TS GW MN |
| 2 | GN MN TN GW TS MN GE TS GW MN |
| 3 | GN GE MN TW GS TE GN GW TN GE TN |
| 4 | GN TE GS TW MN GE GS TW GN TN GE TN |

Jad phases: rotations 1/2 phase 9 start ranged (10 attacks); rotation 3 phase 10
starts magic (10); rotation 4 phase 11 starts magic (8).

Initial GN uses four gas barrages. The recurring GN has five ranged attacks then
four gas barrages and belongs to the next cycle, not an extra standalone phase
before another opening GN.

## Recording comparison and limits

Submission 3276's first twelve observed forms/positions match rotation 4. Its
subsequent sequence matches the beginning of rotation 2; the second attempt is
another partial rotation 4. Source windows are ticks 8318-8847 and 9088-9388.

Attack counts require reconciliation: the long attempt's ninth phase has five
observed ranged projectiles, whereas this Wiki revision lists four. The third
phase has fewer observed ranged projectiles than the Wiki count. Check projectile
attribution, incomplete phases, attack stalls and special-mode effects before
treating either as an exact normal-world action schedule.

The sequence table does not establish per-action delays, targeting probabilities,
hazard geometry or lifetimes, or the transition selection algorithm.

## Provisional runtime

Missing timings and hazard placements are reconstructed as described below.
These reconstructions are not packet evidence or original Jagex source.

`ZulrahRotations.kt` contains the four Wiki sequences. `ZulrahSpec.kt` compiles
them into native boss DSL abilities and timed phases. The recorded JSON is unchanged.
No private-server code, new cache assets, maps, commands, or player messages were added.

### Coverage by phase

Numbers below refer to the 19 emergence phases in the retained recording, not
Wiki phase numbers. `C` means the matching captured action/timing fragment;
`P` means reconstructed with the indicated primary donor fragment. Normal tanzanite
shots use the provisional random-style rule even in otherwise captured fragments.

| Rotation | Phase 1 onward (same order as the table above) |
| --- | --- |
| 1 | C1 C14 C15 P17 C14 P8 P7 P17 P16 C14 |
| 2 | C1 C14 C15 C16 C17 C18 C19 P17 P16 C14 |
| 3 | C1 P19 P5 P8 P3 P2 P13 P16 P10 P11 C12 |
| 4 | C1 C2 P3 C4 C5 C6 C7 C8 P9 C10 C11 C12 |

Captured fragments reused in a different rotation are evidence for the fragment,
not proof that its hazard pattern/timing is identical in that rotation. Each
phase's `evidence` field records the actual donor tick intervals, with a
`Provisional reconstruction` prefix for generated action schedules.

### Reconstruction rules and known uncertainty

- Initial GN is C1 (29 ticks, four gas barrages). Recurring GN is C13 (35 ticks,
  five ranged attacks then four gas barrages). It replaces phase 1 in later cycles;
  it is never followed by another opening-only GN. All cycles can repeat indefinitely.
- Rotation selection uses equal weights with no repeat exclusion, through the native
  DSL selector. The Wiki says random selection and allows repeats, but does not
  verify equal probabilities. Equal weighting is an explicit provisional assumption.
- Normal tanzanite shots independently sample the magic/ranged frequency of the
  captured tanzanite shots, excluding green/Jad attacks (23 magic, 5 ranged).
  This is a small special-mode sample, **not** a verified 23/28 OSRS magic chance.
  Jad uses the Wiki's fixed first style and attack count, three ticks apart.
- Reconstructed phases start acting at +3 ticks. Most actions are three ticks
  apart; egg-to-direct-attack and gas-to-egg boundaries use four ticks. Consecutive
  tails use seven ticks; tail-to-other actions use eight ticks. Dive starts three
  ticks before the next phase. Final-action-to-next-emergence gaps are six ticks
  after direct attacks, seven after hazards, twelve after tails. These are borrowed
  conventions from the observed fragments, not exact evidence for missing phases.
- Hazards first reuse the primary donor's matching actions; if absent, they use
  another recorded phase at the same boss anchor, otherwise the first matching
  recorded phase. A donor's hazard waves cycle if the new sequence needs more.
  Gas barrages retain both projectiles. Source offsets are rebased into the new
  boss footprint, but **target tiles are never translated or mirrored**: every
  target is an observed island tile. Thus no synthetic off-island target is added.
  These reused patterns may not match the OSRS safe route for that phase; this
  requires client comparison. Repeated donor waves can refresh the same cloud tiles.
- Hazard projectile parameters, minion types, impact delays and 30-tick cloud
  lifetimes are inherited from donors. In particular, captured delayed impacts
  sometimes occur ten ticks later and cross emergence boundaries. Retaining those
  delays in a reconstructed phase is provisional, not proof of natural flight time.
- R1/R2 phase 8's ambiguous "alternating series of 5" is provisionally interpreted
  as five total actions: egg, gas, egg, gas, egg, after five direct attacks.
  R3 phase 3's "alternating series of 6" is six total actions: gas, egg, gas, egg,
  gas, egg, followed by two tails. Both interpretations need corroboration.
- R4 phase 3 now uses the Wiki's four ranged attacks, instead of the capture's
  three; phase 9 uses four instead of five. These two phases are explicitly
  reconstructed, not represented as unchanged capture playback.
- Irregular subsequent-cycle phase deviations mentioned by the Wiki are not
  implemented because their selection rules remain unknown. The standard four
  sequences repeat; this limitation is separate from making all four playable.

The fresh RSProx NPC/hazard search on 2026-09-13 still returned only submissions
3342, 820 and 3276. None supplied the missing complete rotations. Cache definitions
remain the existing OpenRune revision-240 assets; cache data does not prove timing.

### Verification

Automated tests cover every Wiki form/position sequence and action count, all four
Jad phases, observed-only hazard targets, mixed-style separation, first/repeat
opening distinction, and the native DSL selection graph. Native lifecycle tests
run each rotation twice, check every timed stage, preserve NPC identity/HP, and
exercise death cancellation during a reconstructed phase. The original finite
recording tests remain unchanged as an independent scheduling regression fixture.

Client comparison is still needed for provisional timing, safe routes, mixed-style
probabilities and repeated-cycle behavior. Use the existing teleport scroll and
boat; no diagnostic chat or rotation commands were introduced.
