# Gate System

This document explains how the server determines which objects are gates and how opening and closing them works. Gates follow the same content-tag pattern as [doors](doors.md) but differ in how the two panels move relative to each other.

---

## Overview

All picket gates are **two-panel** structures — a left panel and a right panel. Both panels always move together when either one is clicked. Like doors, the system is entirely data-driven: no script needs to know about a specific gate object by name.

---

## Content tags (`content.rscm`)

```
closed_left_picketgate  = 27
closed_right_picketgate = 28
opened_left_picketgate  = 29
opened_right_picketgate = 30
```

Each gate panel has its own content tag for each state, giving four tags in total.

---

## Object configuration (`loc.toml`)

Each gate is split into four entries — closed-left, closed-right, opened-left, opened-right — that reference each other via `next_loc_stage`:

```toml
[[object]]
id = "loc.fencegate_l"
contentGroup = "content.closed_left_picketgate"

[object.params]
"param.next_loc_stage" = "loc.openfencegate_l"
"param.opensound" = "synth.picketgate_open"

[[object]]
id = "loc.fencegate_r"
contentGroup = "content.closed_right_picketgate"

[object.params]
"param.next_loc_stage" = "loc.openfencegate_r"
"param.opensound" = "synth.picketgate_open"

[[object]]
id = "loc.openfencegate_l"
contentGroup = "content.opened_left_picketgate"

[object.params]
"param.next_loc_stage" = "loc.fencegate_l"
"param.closesound" = "synth.picketgate_close"

[[object]]
id = "loc.openfencegate_r"
contentGroup = "content.opened_right_picketgate"

[object.params]
"param.next_loc_stage" = "loc.fencegate_r"
"param.closesound" = "synth.picketgate_close"
```

---

## Script (`PicketGate.kt`)

```kotlin
onOpContentLoc1("content.closed_left_picketgate")  { openLeftGate(it.loc, it.type) }
onOpContentLoc1("content.closed_right_picketgate") { openRightGate(it.loc, it.type) }
onOpContentLoc1("content.opened_left_picketgate")  { closeLeftGate(it.loc, it.type) }
onOpContentLoc1("content.opened_right_picketgate") { closeRightGate(it.loc, it.type) }
```

Clicking either panel triggers the handler for that panel's content tag. The handler then locates the partner panel using `locRepo.findExact(...)` and moves both panels simultaneously.

---

## What happens when a gate is opened

When the **left panel** is clicked:

1. Read `param.opensound` and play it.
2. Find the right panel at `leftCoords + leftGateRightPair(shape, angle)` with content tag `closed_right_picketgate`.
3. Delete both panels for `500` game cycles.
4. Spawn the open left panel at `leftCoords + leftGateOpen(shape, angle)`, rotated 3 steps counterclockwise.
5. Spawn the open right panel at `rightCoords + rightGateOpen(shape, angle)`, also rotated 3 steps counterclockwise.

When the **right panel** is clicked the same steps happen, but the partner is found at `rightCoords - leftGateRightPair(shape, angle)` (subtracted instead of added, because the right panel is always one tile in the positive direction from the left panel).

Closing is the exact reverse: `param.closesound` is played, both open panels are deleted, and the closed variants are spawned back at their original positions rotated 3 steps clockwise (`rotations = -3`).

After 500 cycles (~5 minutes) the temporary state expires and the gate resets automatically.

---

## Coordinate translation (`GateTranslations.kt`)

Gates only support `WallStraight` shape. The translation table determines where each panel moves:

### Offset from left panel to right panel (`leftGateRightPair`)

| Angle | Right panel is at |
|---|---|
| West | left + (z+1) |
| North | left + (x+1) |
| East | left + (z−1) |
| South | left + (x−1) |

### Where the left panel moves when opening (`leftGateOpen`)

| Angle | Translation |
|---|---|
| West | x−1 |
| North | z+1 |
| East | x+1 |
| South | z−1 |

### Where the right panel moves when opening (`rightGateOpen`)

| Angle | Translation |
|---|---|
| West | x−2, z−1 |
| North | x−1, z+2 |
| East | x+2, z+1 |
| South | x+1, z−2 |

The right panel travels further than the left panel so both panels fold away behind the fence post on the same side.

Closing reuses the open translation functions but applies them to the angle rotated 3 steps, which is equivalent to reversing the direction.

---

## Key difference from doors

| | Doors | Gates |
|---|---|---|
| Panels | 1 (single) or 2 (double) | Always 2 |
| Shape support | `WallStraight` and `WallDiagonal` | `WallStraight` only |
| Panel rotation on open | 1 step clockwise | 3 steps counterclockwise |
| Right panel location | found by translating in the close direction | found by `leftGateRightPair` offset |

---

## Adding a new gate

Add four entries to `.data/raw-cache/server/loc.toml` — one per panel per state — with the correct `contentGroup` and `next_loc_stage` params pointing at each other. No script changes are needed.
