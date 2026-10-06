# Scoping content to a world type

Declare `worldTypes` on a `PluginScript` and it becomes event content. Leave it unset - the
default - and the script applies everywhere.

```kotlin
class RagingEchoesRelics @Inject constructor(private val shops: Shops) : PluginScript() {
    override val worldTypes = listOf(WorldType.RAGING_ECHOES_LEAGUE)

    override fun ScriptContext.startup() {
        onOpNpc1("npc.relic_keeper") { openRelicInterface(it.npc) }
        onPlayerLogin { grantStartingRelic() }
    }
}
```

Two effects:

- **This world serves none of the listed modes** → the script is never registered. `startup()` is
  not called and no handler exists.
- **It serves one** → the script registers, but its player-facing handlers (ops, login/logout,
  commands) only fire for players on a listed mode. World-level events - startup, map clock,
  npc spawn - always fire.

`content/events/raging-echoes-league` is the smallest working example in the repo.

## Telling a denied player why

Npcs are world-global, so a league npc stands there for everyone on a mixed world. A denied click
gets a message rather than silence:

```kotlin
override val worldTypeDenyMessage =
    "The relic keeper looks straight through you. Switch with ::worldtype."
```

Unset, the default names the mode. Only deliberate interactions and commands send it — login,
movement and timers stay silent.

## Reacting to a switch

```kotlin
onWorldTypeChanged {
    player.mes("Moved from ${from.label} to ${to.label}.")
}
```

Fires after the save is swapped in place and the client resynced. Not on login.

A **scoped** script only sees arrivals — the gate matches `Player.worldType`, which is already `to`.
Cleanup on the way out needs an unscoped script:

```kotlin
class LeagueCleanup @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onWorldTypeChanged {
            if (from == WorldType.RAGING_ECHOES_LEAGUE) tidyUpRelics(player)
        }
    }
}
```

This is also where logic that must run **only** on a switch belongs: a switch re-fires the whole
login sequence, so a non-idempotent `onPlayerLogin` runs again every time.

## Branching instead of scoping

```kotlin
onOpNpc1("npc.bob") {
    if (player.worldType.isLeague) leagueGreeting() else normalGreeting()
}
```

`WorldType.isLeague` and `WorldType.isSeasonalEvent` cover the common groupings.

## Npc spawns for one mode

`.data/world-spawns/<key>/*.toml`, read at boot for each served mode:

```toml
[[spawn]]
npc = "npc.relic_keeper"
coords = "0_50_50_21_18"       # level_mapX_mapZ_localX_localZ
```

Deliberately not packed into the cache — one cache is shared by every mode a world serves, so a
packed npc would exist on all of them.

### The mixed-world limit

Npcs cannot be hidden per player: `NpcRepository.add` takes no observer, and
`NpcAvatar.setInaccessible` hides from everyone. A `main` player on a mixed world sees the
leagues npcs.

The clean answer is a dedicated world — `world-types: ["raging_echoes_league"]` serves no normal
play, which is how OSRS ships leagues. Otherwise separate them spatially, the way
`content/bosses/whisperer` does for its shadow realm.

See [world-types.md](world-types.md) for the storage model and how a player ends up on a mode.
