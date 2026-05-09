# Door System

This document explains how the server determines which objects are doors and how opening and closing them works.

---

## Overview

The door system is **data-driven**. No script needs to know about specific door objects by name. Instead, each door object in the cache is tagged with a generic content category, and a single script handles all objects that share that category.

There are three layers:

1. **Content tags** — named categories for door states
2. **`loc.toml`** — assigns a content tag and parameters to each specific door object
3. **Scripts** — listen for interactions on a content tag, not on individual objects

---

## Layer 1 — Content tags (`content.rscm`)

Located at `.data/gamevals/content.rscm`, this file maps human-readable names to numeric IDs:

```
closed_single_door = 0
opened_single_door = 1
closed_left_door   = 2
closed_right_door  = 3
opened_left_door   = 4
opened_right_door  = 5
```

These are generic categories. They describe the *kind* of object, not which specific one it is.

---

## Layer 2 — Object configuration (`loc.toml`)

Located at `.data/raw-cache/server/loc.toml`, each door object gets:

- A `contentGroup` that places it in one of the categories above
- A `param.next_loc_stage` that points to the other state of the door (open ↔ closed)
- Optional `param.opensound` / `param.closesound` params

Example — a simple single door:

```toml
[[object]]
id = "loc.poordoor"
contentGroup = "content.closed_single_door"

[object.params]
"param.next_loc_stage" = "loc.poordooropen"
"param.opensound" = "synth.nicedoor_open"

[[object]]
id = "loc.poordooropen"
contentGroup = "content.opened_single_door"

[object.params]
"param.next_loc_stage" = "loc.poordoor"
"param.closesound" = "synth.nicedoor_close"
```

The two entries reference each other through `next_loc_stage`, forming a two-state machine. **Adding a new door to the game only requires adding these entries** — no script changes needed.

---

## Layer 3 — Scripts

### Single doors (`DoorScript.kt`)

```kotlin
onOpContentLoc1("content.closed_single_door") { openDoor(it.loc, it.type) }
onOpContentLoc1("content.opened_single_door") { closeDoor(it.loc, it.type) }
```

`onOpContentLoc1` fires for *any* loc with the matching content tag. The script reads parameters from the object type at runtime, so it works for every door without being modified.

### Double doors (`DoubleDoorScript.kt`)

```kotlin
onOpContentLoc1("content.closed_left_door")  { openLeftDoor(it.loc, it.type) }
onOpContentLoc1("content.closed_right_door") { openRightDoor(it.loc, it.type) }
onOpContentLoc1("content.opened_left_door")  { closeLeftDoor(it.loc, it.type) }
onOpContentLoc1("content.opened_right_door") { closeRightDoor(it.loc, it.type) }
```

When one panel is clicked, the script searches for its partner at the expected adjacent tile using `locRepo.findExact(...)` and moves both panels simultaneously.

---

## What happens when a door is opened

1. Read `param.opensound` from the object type and play it.
2. Read `param.next_loc_stage` to get the open variant of the door.
3. Calculate the new coordinates using `DoorTranslations` (based on the door's `LocShape` and `LocAngle`).
4. If the player is standing on the tile the open door would occupy (only relevant for diagonal doors), teleport them one step away.
5. Delete the closed door for `500` game cycles.
6. Spawn the open door at the new coordinates, rotated by one step, for the same duration.

After 500 cycles (~5 minutes) the temporary changes expire and the door returns to its original state automatically.

Closing a door is the exact reverse: read `param.closesound`, look up `next_loc_stage`, translate coordinates back, and swap the spawns.

---

## Coordinate translation (`DoorTranslations.kt`)

When a door opens it physically moves one tile. The direction depends on the door's shape and the angle it faces:

| Shape | Angle | Open direction |
|---|---|---|
| `WallStraight` | West | −X |
| `WallStraight` | North | +Z |
| `WallStraight` | East | +X |
| `WallStraight` | South | −Z |
| `WallDiagonal` | West | +Z |
| `WallDiagonal` | North | +X |
| `WallDiagonal` | East | −Z |
| `WallDiagonal` | South | −X |

The open door is also rotated by one step clockwise (`turnAngle(rotations = 1)`). Closing reverses both the position and the rotation.

Double-door panels use `translateOpenOpposite` / `translateCloseOpposite` so the two panels fold away from each other symmetrically.

---

## Adding a new door

To add a door that the engine handles automatically:

1. Add two entries to `.data/raw-cache/server/loc.toml` — one for the closed state and one for the open state.
2. Give each the correct `contentGroup` (`content.closed_single_door` / `content.opened_single_door`, or the left/right variants for double doors).
3. Set `param.next_loc_stage` on each to point at the other.
4. Optionally set `param.opensound` and `param.closesound`.

No script changes are required.
