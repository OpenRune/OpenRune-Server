# World types

A **world type** is a gameplay mode — `main`, the leagues, `gridmaster`. One account system, one
database, one set of queries; what changes per mode is the player's **save**.

For scoping content to a mode, see [world-type-plugins.md](world-type-plugins.md).

## The model in one table

An account holds up to three **characters** (named personas). A character spans every mode — same
name, same friends list, whichever mode it is playing. Only the save differs.

| | Scope | Examples |
|---|---|---|
| **Account** | one per login | password, email, 2FA, rights, punishments |
| **Character** | up to 3 per account, **spans all modes** | display name, friends, ignores, chat filters, membership |
| **Save** | one per character **per mode** | position, levels, xp, items, varps, attrs, run energy |

So `Mark1` has one friends list whether he is playing main or leagues, and other players see him
online either way. Logging into leagues loads a *different save* for the *same* character. A season
can be wiped without touching accounts, names or social.

> **World types are not realms.** A realm is a *world's* settings (xp rate, spawn coord, login
> message, dev mode) and a world has exactly one. A world type is which *save* a character is
> playing, and a world can serve several. Nothing here touches the `realms` table.

## The modes

`WorldType` (`engine/game`, `org.rsmod.game.world`) is the complete, closed set — a mode needs code
behind it, so there is no "add a row" path.

| `key` | Label |
|---|---|
| `main` | Main — normal, permanent play |
| `twisted_league` | Twisted League |
| `trailblazer_league` | Trailblazer League |
| `shattered_relics_league` | Shattered Relics League |
| `trailblazer_reloaded_league` | Trailblazer Reloaded League |
| `raging_echoes_league` | Raging Echoes League |
| `gridmaster` | Gridmaster |

`key` is the identifier everywhere: the `game.yml` entry, the Postgres schema name, and the value in
`accounts.active_world_type`. It is lower snake case because it has to work as a SQL identifier.

`WorldType.isLeague` and `WorldType.isSeasonalEvent` let content branch without matching strings.

## Configuring a world

`game.yml` lists **every** mode the world serves. Nothing is implied:

```yaml
world: 255
world-types: ["main", "raging_echoes_league"]
```

| Config | Result |
|---|---|
| omitted, or `["main"]` | a normal world |
| `["main", "raging_echoes_league"]` | main and leagues players side by side |
| `["raging_echoes_league"]` | dedicated leagues world — no normal play |
| `["gridmaster", "raging_echoes_league"]` | two events, no main |

**A login that cannot have the mode it asked for falls back to `main`** — whether the account has no
stored preference, or stored a mode this world has since stopped serving. Landing on their normal
save is predictable; landing on an event they never chose is not.

A world that does not serve `main` at all has no such option, so it uses its **first listed mode**
instead. That is what puts a main player somewhere sensible on a leagues-only world.

An unknown key fails startup rather than silently dropping a mode (`WorldType.supportedFrom`).

A world listing one mode is locked to it: `::worldtype` has nothing to offer. A world listing
several is a *mixed world* — see
[the mixed-world limit](world-type-plugins.md#the-mixed-world-limit).

## Switching mode

`::worldtype` opens a menu of the modes this world serves, current one marked. Picking one swaps the
save **in place** — the player stays connected.

The choice is also written to `accounts.active_world_type`, so the next login lands on the same mode
without asking. The client never expresses a mode on connect, so none of this needs a protocol or
client change.

### What happens

| Step | Thread | What |
|---|---|---|
| 1. Guard | game | Refused whenever a **logout** would be refused |
| 2. Save | saver service | Current mode written out |
| 3. Load | loader service | Target mode read back (pinned, no re-negotiation) |
| 4. Swap | game | State replaced, client resynced, login events re-fired |

**The guard is shared with logout.** Both call `Player.isLogoutBlocked()`, because a switch discards
exactly the state a logout would — so they cannot drift apart. It is `preventLogoutUntil` **or**
`isInCombat()`: the timer alone is only set when a player takes a hit or retaliates, so an aggressor
who is never hit back would otherwise slip through both. The player gets the same message.

The guard runs **twice** — at the command, and again when the swap lands, since steps 2–3 take
cycles and the player keeps playing. Aborting at the second check costs nothing: the outgoing save
already landed, and that is the mode they stay on.

### Step 4 in detail

In order, on the game thread (`WorldTypeSwitchService.applySwitch`):

1. Suspend autosave (`Player.persistenceSuspended`).
2. Clear the mode-scoped state.
3. Apply the loaded save.
4. Set `Player.worldType`, and mark the save new if this is a first visit.
5. Telejump through the engine, force a scene rebuild.
6. Publish `PrepareLogin` → `Initialize` → `Login` → `EngineLogin`.
7. Publish `WorldTypeChangedEvent(from, to)`.

Re-publishing the login events is what makes it seamless — their handlers already do a full client
resync (`VarpReset` and retransmit, inventory retransmit, stats, run energy, camera). Nothing new
had to be written for the client.

> Because the login events re-fire on every switch, **any login handler that is not idempotent will
> run again** — one-time grants, daily resets, welcome popups. Use
> [`onWorldTypeChanged`](world-type-plugins.md#reacting-to-a-switch) for logic that should only
> run on an actual switch.

### Why each of those steps exists

Each one is load-bearing; removing it reintroduces a bug that was fixed once already.

- **Suspend autosave.** Clearing and re-applying mutates attrs and inventories, and those mutations
  are themselves autosave triggers. `AccountSaveRequest` holds the player *live*, so an autosave
  queued mid-swap would persist the half-built state.
- **Clear before applying.** The appliers were written for a fresh `Player` and only write the rows
  they loaded — they merge rather than replace. Without `clearWorldTypeScopedState`, anything the
  incoming save has no row for survives from the mode being left; for inventories that means **items
  crossing between modes**. It clears `Perm`-scope inventories (exactly what the save pipeline
  persists), `statMap`, `vars` and `attr`.
- **Mark a first visit new.** A mode's save is brand new on first entry, so it needs the new-account
  initialisation — stats most visibly. Without it the character keeps an empty stat map, which reads
  as level 1 everywhere, **1 hitpoint included**.
- **Telejump, don't assign.** The appliers set `coords` outright, which is right for a login (the
  player is not registered yet) but skips zone bookkeeping for one who is.
- **Force the scene rebuild.** `RspCycle` only resends the scene when the build area changes, and a
  switch usually leaves the player on the same tile. Without `ClientCycle.forceSceneRebuild()` the
  client keeps showing the world of the mode being left.

If the swap throws, the player is force-disconnected rather than left holding a mix of two saves.
The outgoing mode was written out before the load began, so nothing is lost.

## Where data lives

**Shared, in `public`** — identity and anything describing the person:

| Table | Holds |
|---|---|
| `accounts` | name, password, email, 2FA, rights, discord id, `active_world_type` |
| `account_characters` | the characters: display name, rename history, membership, mute/ban |
| `character_friends`, `character_ignores`, `character_chat_filters` | social graph, per character |
| `sessions`, `punishments` | live sessions; bans/mutes/kicks |
| `activity_logs`, `activity_log_items`, `online_samples` | audit and analytics |
| `realms`, `worlds` | world definitions — unrelated to world types |

**Per mode, in a schema named after the `key`** — created from one migration (`db/world_type/V1`),
so every mode has an identical set:

```
<key>.character_progress   position, level, last login/logout, run energy, xp rate, online markers
<key>.character_varps
<key>.character_attrs
<key>.stats
<key>.inventories
<key>.inventory_objs
```

A character has one `character_progress` row per mode it has played, created on first entry. Every
per-mode table references `public.account_characters(id)`, so deleting a character still cascades
its saves away.

### The one rule

> **Any query touching a per-mode table must run through `withSchemaTransaction`, or the `schema`
> overload of `withTransactionBlocking`.**

The server issues `SET LOCAL search_path TO "<key>", public` per transaction, so one query can read
identity from `public` and the save from the mode's schema without being rewritten — see
`characters_select_metadata_by_login.sql`. There is one copy of each query and one save path for all
modes.

`SET LOCAL` is deliberate: it reverts on commit or rollback, so a scoped transaction cannot leak its
search path into the next one on the shared connection.

Plain `withTransaction` leaves the path on `public`, where the per-mode tables no longer exist.
`GameDatabase` verifies the effective schema and **throws** on a mismatch — a mis-scoped transaction
silently reads or writes another mode's save, which is far worse than a refused login. The two ways
it can fail:

| Symptom | Cause |
|---|---|
| `schema '<key>' does not exist` | schema dropped while the server was running — restart recreates it |
| `connection is in autocommit mode` | `SET LOCAL` did not stick |

The `GameDbManager` gateway is public-only. Every gateway request today touches `accounts` or the
social tables, so it is fine — a per-mode request would need its own scoped path.

Work that must span modes loops the served types and runs once per schema — see
`RealmConfigService.clearGhostOnlineSessions` and Central's
`WorldPresenceService.clearCharacterOnlineMarkers`.

### Saving and first entry

`CharacterAccountRepository.save` writes identity and the save as two statements in one
schema-scoped transaction. **The schema is resolved from `request.player.worldType` at execution
time**, not from the request — the request holds the player live, so taking both from the same place
is what stops a queued save writing one mode's state into another's tables.

The login query `LEFT JOIN`s `character_progress`, so a character with no save for a mode still
resolves. The repository creates the row and reports it as
`AccountLoadResponse.Ok.firstVisitToWorldType`, which starts the player at spawn on the base xp
rate.

### Cross-mode views

Only the *save* needs unioning; character rows are already in `public`.

- `public.progress_all` — online markers across modes, for Central's presence cross-check.
- `public.stats_all` — levels across modes, for the world login gate.

Both carry a `world_type` column and are rebuilt whenever `FlywayMigrator` runs. The schema set is
**discovered** (any schema holding a `character_progress` table) rather than passed in, because
Central must see every mode while only the game server knows which ones this world serves.

## Runbook

| Task | How |
|---|---|
| **Add a mode** | Add its `key` to `world-types`, restart. Schema created and migrated at boot. |
| **Close a mode** | Remove it from `world-types`. Saves are untouched; re-adding restores them. Players stored on it fall back to `main`. |
| **Wipe a season** | `DROP SCHEMA <key> CASCADE`, restart. Accounts, names, social, punishments and logs survive. |

Two cautions, both learned the hard way:

- **Drop the whole schema, not just its tables.** Flyway's `flyway_schema_history` lives inside it;
  if that survives, Flyway considers V1 applied and will not recreate the tables.
- **Restart after dropping.** Schemas are only created at boot. Dropping one on a running server
  leaves every query against it failing.

**Migrations.** `db/migration` runs against `public` and creates the `main` schema. `db/world_type`
runs once per mode schema, each with its own `flyway_schema_history`. Central migrates `public` and
`main`; the game server migrates the modes it serves, because Central never reads `game.yml`.

## Reference

**Identity.** The display name lives on the character row in `public.account_characters` — globally
unique, the same in every mode. A rename applies to that character everywhere at once and leaves the
account's other characters alone. Keeping identity in `public` is what makes the split work: a
mode's schema never needs a constraint or lookup spanning modes.

**Punishments.** Account-wide, as always: `punishments.account_id`, optionally narrowed to a
character. Nothing is mode-aware — a ban is a ban in every mode, which is the point of one shared
account system.

**Logs.** `activity_logs.world_type` records the mode (`world_id` cannot stand in — one world serves
several), filled by `CentralActivityLogWriter` from `player.worldType`. It also outlives a season
wipe: after `DROP SCHEMA ... CASCADE` the log rows remain and `world_type` is the only record of the
mode. `activity_log_items` inherits from its parent row.

**Social.** Friends, ignores and chat filters belong to the character and are shared by every mode,
because the row they key on is in `public`. Nothing about social is mode-aware:

- One friends list and one ignore list per character, identical whichever mode it plays.
- Private messages cross modes and account types — an ironman and a normal account can friend each
  other, and a main player can message someone in leagues.
- Presence is per character, so others see `Mark1` online whatever he is playing.
- An account's characters are separate people socially: `Mark1`'s friends are not `Mark2`'s.

**Characters per account.** Up to three (`CharacterAccountRepository.maxCharactersPerAccount`), each
with its own name and social graph. Login takes the lowest character id; a character-select screen
would choose here instead.

**One mode at a time.** Central deletes an account's other sessions on login, and the game's own
duplicate-login guard then checks the character's progress row inside its own schema.
