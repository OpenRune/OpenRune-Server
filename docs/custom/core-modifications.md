# Custom core modifications

Scope: runtime differences introduced by this fork up to accepted `444ead71b`,
relative to the upstream common ancestor. This does not label incoming revision 241
changes as custom. Test/build companion files are listed with their owning feature.
No additional core change remains from this organization pass.

## Combat/specials
- Reason/behaviour: Magic, melee, shield dispatch and bonus/protection handling.
- Upstream conflict risk: Combat target/hit/autocast/bonus APIs.
- Future removal path: Replace only with equivalent upstream dispatch while retaining regression tests.

| File | Introducing/relevant fork commit |
|---|---|
| `api/combat/combat-commons/src/main/kotlin/org/rsmod/api/combat/commons/DragonfireProtection.kt` | `c46abc6af` |
| `api/combat/combat-scripts/build.gradle.kts` | `c46abc6af` |
| `api/combat/combat-scripts/src/main/kotlin/org/rsmod/api/combat/PvNCombat.kt` | `c46abc6af` |
| `api/combat/combat-scripts/src/main/kotlin/org/rsmod/api/combat/PvPCombat.kt` | `c46abc6af` |
| `api/combat/combat-scripts/src/main/kotlin/org/rsmod/api/combat/player/PlayerCommons.kt` | `c46abc6af` |
| `api/combat/combat-scripts/src/main/kotlin/org/rsmod/api/combat/scripts/PvNCombatScript.kt` | `c46abc6af` |
| `api/combat/combat-scripts/src/main/kotlin/org/rsmod/api/combat/scripts/PvPCombatScript.kt` | `c46abc6af` |
| `api/combat/combat-scripts/src/test/kotlin/org/rsmod/api/combat/player/MagicSpecialDispatchTest.kt` | `c46abc6af` |
| `api/player/src/main/kotlin/org/rsmod/api/player/bonus/WornBonuses.kt` | `c46abc6af` |
| `api/player/src/main/kotlin/org/rsmod/api/player/worn/DragonfireShields.kt` | `c46abc6af` |
| `api/specials/src/main/kotlin/org/rsmod/api/specials/SpecialAttack.kt` | `c46abc6af` |
| `api/specials/src/main/kotlin/org/rsmod/api/specials/SpecialAttackRegistry.kt` | `c46abc6af` |
| `api/specials/src/main/kotlin/org/rsmod/api/specials/SpecialAttackRepository.kt` | `c46abc6af` |
| `api/specials/src/main/kotlin/org/rsmod/api/specials/combat/ShieldSpecialAttack.kt` | `c46abc6af` |

## Drops/previews
- Reason/behaviour: Preview without executing reward rolls; registry lookup and weighted inspection.
- Upstream conflict risk: Drop DSL, registry and death/reward hooks.
- Future removal path: Use an equivalent upstream read-only preview API.

| File | Introducing/relevant fork commit |
|---|---|
| `api/drop-table-plugin/src/main/kotlin/org/rsmod/api/droptable/DropRollItemRollable.kt` | `9f38dbb1d` |
| `api/drop-table-plugin/src/main/kotlin/org/rsmod/api/droptable/DropTablePreview.kt` | `9f38dbb1d` |
| `api/drop-table-plugin/src/main/kotlin/org/rsmod/api/droptable/DropTableRegistry.kt` | `9f38dbb1d` |
| `api/drop-table-plugin/src/test/kotlin/org/rsmod/api/droptable/DropTablePreviewTest.kt` | `9f38dbb1d` |
| `api/drop-table/src/main/kotlin/dtx/impl/weighted/WeightedRollable.kt` | `9f38dbb1d` |

## Boss respawns
- Reason/behaviour: Track actual scheduled deaths/respawns and cancel instance-owned deadlines.
- Upstream conflict risk: Boss DSL, NPC death and instance cleanup.
- Future removal path: Adopt upstream deadlines only if the Nero HUD contract and custom timings remain.

| File | Introducing/relevant fork commit |
|---|---|
| `api/bosses/src/main/kotlin/org/rsmod/api/bosses/runtime/EffectInterpreter.kt` | `c46abc6af` |
| `api/death/src/main/kotlin/org/rsmod/api/death/NpcDeath.kt` | `c46abc6af` |
| `api/instances/src/main/kotlin/org/rsmod/api/instances/InstanceManager.kt` | `c46abc6af` |
| `api/npc/src/main/kotlin/org/rsmod/api/npc/respawn/BossRespawnPolicy.kt` | `c46abc6af` |
| `api/npc/src/main/kotlin/org/rsmod/api/npc/respawn/BossRespawnTimers.kt` | `c46abc6af` |
| `api/npc/src/test/kotlin/org/rsmod/api/npc/respawn/BossRespawnTimersTest.kt` | `c46abc6af` |

## Commands/UI
- Reason/behaviour: Rights metadata, native submenu dispatch, NPC examine and administrator map navigation.
- Upstream conflict risk: Packet op/subop, permission metadata and UI ownership.
- Future removal path: Use native hooks that preserve rights, zero-based subops and existing menu behaviour.

| File | Introducing/relevant fork commit |
|---|---|
| `api/cheat/src/main/kotlin/org/rsmod/api/cheat/CheatHandlerBuilder.kt` | `5733d6939` |
| `api/net/src/main/kotlin/org/rsmod/api/net/rsprot/handlers/ClickWorldMapHandler.kt` | `83c47b4e3` |
| `api/net/src/main/kotlin/org/rsmod/api/net/rsprot/handlers/OpNpc6Handler.kt` | `9f38dbb1d` |
| `api/net/src/main/kotlin/org/rsmod/api/net/rsprot/handlers/SubOpHandler.kt` | `17fd92312` |
| `api/player/src/main/kotlin/org/rsmod/api/player/events/NpcExamineEvent.kt` | `9f38dbb1d` |
| `engine/game/src/main/kotlin/org/rsmod/game/cheat/CheatHandler.kt` | `5cf3e9f3e` |

## Cutscene/recovery
- Reason/behaviour: Ignore movement in cutscenes and recover abandoned instance sessions.
- Upstream conflict risk: Login placement, coordinate/instance representation and locks.
- Future removal path: Already equivalent to upstream contribution; preserve tests during next merge.

| File | Introducing/relevant fork commit |
|---|---|
| `api/net/build.gradle.kts` | `a53731b25` |
| `api/net/src/main/kotlin/org/rsmod/api/net/rsprot/handlers/MoveGameClickHandler.kt` | `a53731b25` |
| `api/net/src/main/kotlin/org/rsmod/api/net/rsprot/handlers/MoveMinimapClickHandler.kt` | `a53731b25` |
| `api/net/src/main/kotlin/org/rsmod/api/net/rsprot/player/AccountLoadResponseHook.kt` | `a53731b25` |
| `api/net/src/test/kotlin/org/rsmod/api/net/rsprot/handlers/CutsceneMoveClickTest.kt` | `a53731b25` |
| `api/net/src/test/kotlin/org/rsmod/api/net/rsprot/player/AbandonedInstanceRecoveryTest.kt` | `a53731b25` |

## Plugin runtime
- Reason/behaviour: Keep class loaders alive and use runtime shadow copies for deferred helper loads.
- Upstream conflict risk: Boot/shutdown, dependency injection and plugin JAR loading.
- Future removal path: Use equivalent upstream loader lifecycle without breaking deferred classes.

| File | Introducing/relevant fork commit |
|---|---|
| `engine/plugin/src/main/kotlin/org/rsmod/plugin/loader/ExternalPluginLoader.kt` | `4bf85522e` |
| `server/app/src/main/kotlin/org/rsmod/server/app/GameServer.kt` | `4bf85522e` |

## Cache/UI mappings
- Reason/behaviour: Dump and validate packed component symbols after full packing.
- Upstream conflict risk: FileStore packing, generated symbols and interface IDs.
- Future removal path: Retire only after upstream emits equivalent validated mappings.

| File | Introducing/relevant fork commit |
|---|---|
| `or-cache/src/main/kotlin/dev/openrune/CacheTools.kt` | `60be8c74f` |
| `or-cache/src/main/kotlin/dev/openrune/gamevals/GamevalDumper.kt` | `e10bcc4ea` |
| `or-cache/src/main/kotlin/dev/openrune/gamevals/InterfaceGamevalContracts.kt` | `60be8c74f` |
| `or-cache/src/test/kotlin/dev/openrune/gamevals/InterfaceGamevalContractsTest.kt` | `60be8c74f` |

## Installation/RSA
- Reason/behaviour: Correct private-key PEM labelling.
- Upstream conflict risk: Key generator/decoder compatibility.
- Future removal path: Use upstream equivalent while preserving existing installed keys.

| File | Introducing/relevant fork commit |
|---|---|
| `server/install/src/main/kotlin/org/rsmod/server/install/GameNetworkRsaGenerator.kt` | `80a8ddf3a` |

## Not adopted

The archived Central-startup, offline-login and installer alternatives are not part
of this baseline. The temporary lifecycle import was reverted; do not describe those
protections as active. See the branch audit and exact baseline for recovery references.

## Weapon completeness: blowpipe ammunition bonus

- Paths: api/player/src/main/kotlin/org/rsmod/api/player/worn/BlowpipeCharges.kt and
  api/player/src/main/kotlin/org/rsmod/api/player/bonus/WornBonuses.kt.
- Reason: ranged formulas and equipment UI previously ignored darts inside blowpipes.
- Behaviour: decode per-item native varobjs and add the actual loaded dart bonuses;
  other weapons and quiver-ignore rules retain their existing behaviour.
- Introducing commit: the focused feat(weapons) blowpipe chunk on feature/weapon-completeness.
- Risk: overlap with any future upstream blowpipe/bonus implementation; compare rather
  than double-applying bonuses. Removal path: retain state compatibility and tests when
  adopting an equivalent upstream implementation.
- Validation: nine dart tiers, both variants, max packed fields, unrelated quiver ammo,
  atomic loading/refunds and last-dart attack covered by cache-backed tests.

### Eye of Ayak charge state and per-spawn Magic defence (2026-10-03)

- Paths: `api/player/.../worn/AyakCharges.kt`, `api/npc/.../MagicDefenceDrain.kt`,
  `api/combat/combat-formulas/.../accuracy/magic/{PvNMagicAccuracy,NvNMagicAccuracy}.kt`.
- Reason: normal and special attacks must share item state; Soul Rend drains a bonus,
  not an NPC's Magic level or the shared type's params.
- State lives in native varobjs/varn defined by the special-weapons pack. No attrs.
- Behaviour: undrained NPCs have identical formulas; drained positive bonuses floor
  at zero. Negative original bonuses are preserved. Native respawn clears the varn.
- Introducing commit: focused `feat(weapons): implement Eye of Ayak charges and Soul Rend`.
- Risk: upstream per-NPC bonus modifiers may overlap; compare both magic accuracy
  call paths and reset semantics before adoption. Removal path: migrate the shared
  state/formula access to an upstream equivalent while preserving packed item values.
