# Instances

How the instance system works and how to add a new instanced boss.

An instance is a private copy of one or more map regions allocated at runtime for a specific group of players. Each instance has its own NPC spawns, tracks damage contributions separately, and is cleaned up automatically when it goes empty.

---

## Quick mental model

1. You define an **`InstanceSettingsRow`** in `InstanceSettingsTable.kt` — this is the database row that holds lobby settings (fee, max players, timer, enter/exit objects, etc.).
2. You register a `gamevals.toml` entry so the engine knows what numeric ID the row has.
3. You write a class that extends **`InstanceScript`** — this declares the map area, NPC spawns, and wires up the enter/exit object interactions.
4. The framework handles everything else: region allocation, NPC spawning, access control menus, kill timer, grace period, and cleanup.

---

## Step 1 — Add the settings row

Open `or-cache/src/main/kotlin/dev/openrune/tables/InstanceSettingsTable.kt` and add a new `row(...)` block inside `instanceSettings()`.

```kotlin
row("dbrow.instance_my_boss") {
    column(KEY, "my_boss")                          // unique string key for this instance type
    columnRSCM(ENTER_OBJECT, "loc.my_boss_entrance") // clickable entrance object
    columnRSCM(EXIT_OBJECT,  "loc.my_boss_exit")     // clickable exit object
    columnCoord(ENTER_COORD, CoordGrid(3000, 9000))  // where the player lands inside
    columnCoord(EXIT_COORD,  CoordGrid(3100, 3200))  // where they land on leave/death
    column(FEE, 0)                                  // coins charged on creation (0 = free)
    column(MAX_PLAYERS, 1)                          // max occupants (use 1 for solo, 5 for small group, etc.)
    column(TIME_LIMIT_MINUTES, 60)                  // 0 = no time limit
    column(GRACE_MINUTES, 10)                       // loot-collection window after boss dies
    columnRSCM(BOSS_NPC, "npc.my_boss")             // used for kill timer and damage tracking
    column(BOSS_NAME, "My Boss")                    // shown in the creation dialogue title
    column(RECOMMENDED_COMBAT, 80, 100)             // shown as "80-100+" in the dialogue
    column(TEAM_SIZE, 1)
    column(LOOT_MULTIPLIER, "x1.0")
    column(DESCRIPTION, "A very scary boss.")
}
```

All fields are required unless noted otherwise. `ENTER_COORD` / `EXIT_COORD` can also be set at runtime in the `InstanceArea` (see Step 3) — whichever is non-zero wins.

### Field reference

| Field | Type | Description |
|-------|------|-------------|
| `KEY` | String | Unique key used throughout the code to look up this instance type. Must match what you pass to `settingsRow()`. |
| `ENTER_OBJECT` | Loc | The location (object) players click to enter. `InstanceCreateScript` binds op-1 to the default create/join menu automatically. |
| `EXIT_OBJECT` | Loc | The location players click to leave. Op-1 is bound to `leaveFlow()` automatically. |
| `ENTER_COORD` | CoordGrid | Spawn point inside the instance. Can be overridden by `InstanceArea`. |
| `EXIT_COORD` | CoordGrid | Where the player is sent on leave or death. Can be overridden by `InstanceArea`. |
| `FEE` | Int | Coins deducted from the player's inventory on creation. 0 = free. |
| `MAX_PLAYERS` | Int | Hard cap on occupants. Join is rejected if the session is full. |
| `TIME_LIMIT_MINUTES` | Int | Minutes until the arena expires. 0 = no limit. Warnings are sent at 50 %, 25 %, 12.5 %, 1 min, and 30 sec remaining. |
| `GRACE_MINUTES` | Int | Minutes the instance stays alive after all bosses die (loot collection window). Default 10. |
| `BOSS_NPC` | Npc | One or more NPC types that count as the boss. Used for kill timer and to determine when all bosses are dead. |
| `BOSS_NAME` | String | Display name in creation/join dialogues. |
| `RECOMMENDED_COMBAT` | Int (×2) | Two integers forming a combat level range shown in the dialogue. |
| `TEAM_SIZE` | Int | Displayed in the dialogue. |
| `LOOT_MULTIPLIER` | String | Displayed in the dialogue (e.g. `"x1.0"`). |
| `DESCRIPTION` | String | Short description displayed in the dialogue. |

---

## Step 2 — Register the DB row ID

Open `api/instances/src/main/resources/gamevals.toml` and add your row alongside the existing entry:

```toml
[gamevals.dbrow]
instance_scurrius = 64681
instance_my_boss  = 64682   # pick the next available numeric ID
```

---

## Step 3 — Write the InstanceScript

Create a new file in the relevant content module, e.g. `content/.../MyBossInstance.kt`:

```kotlin
class MyBossInstance @Inject constructor(
    registry: BossInstanceRegistry,
) : InstanceScript(registry) {

    // Must match the dbrow key you defined in InstanceSettingsTable
    override fun settingsRow(): String = "dbrow.instance_my_boss"

    // The map area used for player-created (private) instances
    override fun area(): InstanceArea = PRIVATE_AREA

    override fun ScriptContext.configure() {
        // Op-1 on the enter/exit objects is wired automatically.
        // Override with onEnterObject / onExitObject if you need different behaviour.
        onEnterObject { enterPublicRoom(PUBLIC_AREA) }  // only needed if you have a public room
        onExitObject  { defaultLeaveFlow() }
    }

    private companion object {
        // Copy region 12345 and spawn the boss at the given coord inside that region
        private val PRIVATE_AREA = InstanceArea.copyRegions(
            centerRegionId = 12345,
            npcSpawns = listOf(InstanceNpc("npc.my_boss_instance", CoordGrid(3000, 9000))),
        )

        // Public room uses a different NPC variant so it isn't flagged as a personal kill
        private val PUBLIC_AREA = InstanceArea.copyRegions(
            centerRegionId = 12345,
            npcSpawns = listOf(InstanceNpc("npc.my_boss_normal", CoordGrid(3000, 9000))),
        )
    }
}
```

Don't forget to register the class with Guice in your module's binding file (or let the framework auto-discover it if your project uses classpath scanning).

---

## InstanceArea types

### `InstanceArea.copyRegions` — copy an existing map region

Use this for most bosses. The server copies the tiles from the live map into a freshly allocated region.

```kotlin
// Single region
InstanceArea.copyRegions(
    centerRegionId = 12345,
    npcSpawns = listOf(InstanceNpc("npc.my_boss", CoordGrid(3000, 9000))),
)

// 2×2 grid of regions around a centre (for large arenas)
InstanceArea.copyRegions(
    centerRegionId = 12345,
    gridSize = 2,
    npcSpawns = listOf(...),
)

// Explicit list of region IDs
InstanceArea.copyRegions(
    regionIds = listOf(12344, 12345, 12346),
    npcSpawns = listOf(...),
)
```

`enterCoord` and `exitCoord` can be omitted here and set via the DB row instead.

### `InstanceArea.template` — use a static region template

Use this when the instance area is a pre-built region that never changes (like a completely static dungeon floor). The template is referenced by a `RegionStaticTemplate` object rather than being copied from the live map.

```kotlin
InstanceArea.template(
    template = MY_STATIC_TEMPLATE,
    enterCoord = RegionLocal(0, 6, 8, 32, 32),
    exitCoord  = CoordGrid(3100, 3200),
    npcSpawns  = listOf(...),
)
```

---

## NPC spawns

`InstanceNpc` takes an NPC type string and a `CoordGrid` (the absolute coordinate of the spawn point in the source region before it is translated into the instance). The coord is converted to region-local coordinates internally.

```kotlin
InstanceNpc("npc.my_boss", CoordGrid(3000, 9000))
```

NPCs spawned this way are automatically attached to the instance session. When the instance is destroyed all attached NPCs are despawned.

If `spawnOnFirstJoin` is true (override `spawnOnFirstJoin(): Boolean = true` in your `InstanceScript`), NPCs are not spawned at creation time and instead spawn the first time a player joins. This is useful for public rooms where you don't want NPCs sitting idle before anyone enters.

---

## Lifecycle

```
create()  ──► Active  ──► [boss dies] ──► Grace (10 min default)
                  │                             │
                  │                         [time up] ──► destroyed
                  │
              [everyone leaves] ──► Reclaim (20 min default)
                                        │
                                    [still empty] ──► destroyed
                                    [someone joins] ──► Active again
```

- **Active**: The boss is alive, damage is tracked, the kill timer is running.
- **Grace**: The boss is dead, no new bosses spawn, players can collect loot. Time warnings fire at 50 %, 25 %, 12.5 %, 1 min, and 30 sec.
- **Reclaim**: The instance is empty (all players left mid-fight). It stays allocated for 20 minutes in case anyone rejoins. Destroyed if still empty at the deadline.

Kill timer is only tracked for instances with `maxPlayers ≤ 5`. Maximum recordable time is 60 minutes.

---

## Access modes

Players choose an access mode when creating an instance. It can be changed later via "Edit Instance Settings" on the enter object.

| Mode | Who can join |
|------|-------------|
| **Private** | Only the owner (always works with `forceAccess = true` on rejoin). |
| **Friends** | Anyone who knows the owner's name. |
| **Code** | Anyone who has the 4-character join code. Codes use characters `A-Z` (no I or O) and `2-9` to avoid ambiguity. |

Server-owned (public) instances always use `Friends` access and are auto-created/reused by `enterPublicRoom()`.

---

## Events

Register these inside `configure()` to hook into the instance lifecycle for your boss:

```kotlin
override fun ScriptContext.configure() {
    onInstanceStarted {
        // instance just became active (fires after create() or on first join with spawnOnFirstJoin)
    }

    onInstancePlayerJoin {
        // a player entered the instance; `player` is available
        mes("Welcome to the arena.")
    }

    onInstancePlayerLeave {
        // a player left the instance
    }

    onInstanceTimeTick {
        // fires at the standard warning intervals (50%, 25%, 12.5%, 1 min, 30 sec)
        // use `remainingTicks` on the session to decide what to say
    }

    onInstanceEnded {
        // the instance is being destroyed
    }
}
```

All events are scoped to this instance's `key`, so handlers only fire for your boss, not others.

---

## Public rooms

A public room is a server-owned instance — it is not tied to any player and is recycled between uses. Call `enterPublicRoom(area)` from `onEnterObject` to route players into the shared room. A new room is created automatically if none exists or if the existing one is full.

```kotlin
override fun ScriptContext.configure() {
    onEnterObject { enterPublicRoom(PUBLIC_AREA) }   // op-1 → public room
    // op-2 from InstanceCreateScript → private instance menu (automatic)
    onExitObject { defaultLeaveFlow() }
}
```

If you want to let players peek at public room occupancy before committing, add a custom op:

```kotlin
onOpLoc3(row.enterObject) {
    val session = manager.sessionsForKey(key).firstOrNull { it.isServerOwned }
    val count = session?.occupants?.size ?: 0
    mes("There are $count players in the public room.")
}
```
