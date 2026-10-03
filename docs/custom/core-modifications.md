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

### Once-only hit impact effects (2026-10-03)

- Paths: `engine/game/.../hit/{Hit,HitImpactEffects}.kt`,
  `api/npc/.../hit/processor/StandardNpcHitProcessor.kt`,
  `api/player/.../hit/processor/{StandardPlayerHitProcessor,DamageOnlyPlayerHitProcessor}.kt`.
- Reason: weapon healing/drain must follow a processed hit, not an independent timer
  that can outlive cancellation or use pre-impact HP. No attrs or persistence changes.
- Behaviour: optional callbacks survive Hit.copy, run once after native damage
  processing and receive actual damage. Hits without callbacks retain their effects.
- Introducing commit: `fix(combat): apply weapon effects on actual hit impact`.

## NPC weapon venom

- Paths: `api/mechanics/toxins/.../{NpcVenomTimerScript,impl/NpcVenom,impl/NpcPoison}.kt`
  and `api/combat/combat-commons/.../WeaponVenom.kt`.
- Reason: venom-capable weapons previously had no NPC venom lifecycle. Use native
  varn/timer state, immunity checks, poison fallback and actual hit callbacks.
- Dependency: special-weapons cache pack supplies the venom varn and timer gamevals.
- Verification: five venom regression tests; existing weapon and special suites pass.
- Zulrah's encounter implementation and player venom attacks are unchanged.
- Risk: new/custom hit processors must explicitly complete callbacks after applying
  damage. Compare any upstream impact callback API before merging it; migrate callers
  and remove this list if an upstream equivalent supplies the same cancellation semantics.
- Tests: copied/capped/zero damage, once-only execution, native NPC HP deduction,
  Siphon overkill healing, source relog and Soul Rend impact/cancellation.

## Prayer-piercing melee specials (2026-10-03)

- Paths: `api/combat/combat-manager/.../PlayerAttackManager.kt` and
  `api/specials/.../SpecialAttackManager.kt`.
- Adds an explicit prayer-piercing route for Dragon sword and Ancient mace.
  Only PvP protection penetration changes; normal modifiers, retaliation, source
  attribution and defend animation still execute. Ordinary melee calls are unchanged.
- Verified by real queued hits: 20 damage becomes 12 with normal Protect from Melee,
  remains 20 for a piercing hit, and remains zero against admin invulnerability.
- NPC style immunity is deliberately still processed. Boss-specific prayer mechanics
  require separate review; this route does not bypass every NPC immunity.
- Upstream adoption: replace the route with an equivalent per-hit penetration API
  only after the native queue regression and weapon tests pass.

## Parameter-only NPC cache overlays (2026-10-03)

- Files: `or-cache/.../tools/PackServerConfigOSRS.kt` and `codec/osrs/impl/NpcServerCodec.kt`.
- Reason: a full NPC override for a demon flag replaced earlier server overrides and reset client render priority.
- Change: `npc_params` overlays parse only ID and params, participate in NPC incremental inputs, and merge at the end of the existing server codec. No client NPC record is rewritten. Unknown base IDs fail validation.
- Related system: demonbane specials and existing combat attribute collectors.
- Evidence: 16,577 definitions compared; only 96 demon flags and two Duke resistance flags changed. Existing fields and kill-count parameters match. The codec regression test retains full custom fields and client render priority.
- Upstream conflict risk: cache overlay parser and codec constructor. Remove this extension if upstream supplies equivalent parameter-only patching or the native cache gains verified attributes. Introducing commit: the focused demon-metadata commit following `9b7daa8e4`.

## Silverlight/Darklight combat attributes (2026-10-03)

- Files: `CombatMeleeAttributes`, `CombatMeleeAttributeCollector`, `MeleeAccuracyOperations` in `api/combat/combat-formulas`.
- Reason: only dyed Silverlight received the damage flag; neither ordinary Silverlight nor Darklight received their demonbane accuracy/damage bonuses.
- Change: both swords receive the existing damage flag plus an explicit accuracy flag. Dyed Silverlight retains damage-only behavior. Against demons the bonus is 60%, or 42% with Duke resistance; other targets are unchanged.
- Evidence: equipped-item collector plus actual accuracy/damage operations tested for ordinary/dyed Silverlight, Darklight, Arclight, Emberlight and Dragon sword against ordinary/demon/resistant targets.
- Reference: [pinned Wiki DPS formulas](https://github.com/weirdgloop/osrs-dps-calc/blob/89c3e25b344aea90d0189746e4b5f73dde0f0383/src/lib/PlayerVsNPCCalc.ts).
- Upstream conflict risk: shared equipment attribute mappings and melee accuracy operations. Reconcile rather than stack bonuses when upstream adds equivalent coverage. Introducing commit: focused demonbane-formula follow-up to `61c605a8c`.

## Melee offence against magic defence (2026-10-03)

- File: `api/combat/combat-formulas/.../accuracy/melee/MeleeAgainstMagicAccuracy.kt`.
- Reason: blessed Saradomin sword contests magic defence while retaining melee offence and melee damage classification.
- Change: composes existing melee offence, NPC/player magic defence and native accuracy operations, including NPC magic-defence drain and raid scaling. Existing melee/magic formulas are unchanged.
- Evidence: helper routing tests verify selected melee stance/type, NPC defence/Magic inputs and player magic defence; special tests verify melee hit queues and protection behavior.
- Upstream conflict risk: formula helper signatures. Replace with an equivalent upstream mixed-accuracy route only after retaining these tests. Introducing commit: Saradomin sword slice after `6f300c4aa`.

## Reduced melee defence roll (2026-10-03)

- File: `api/combat/combat-formulas/.../accuracy/melee/ReducedMeleeDefenceAccuracy.kt`.
- Reason: Vesta's longsword reduces the target defence roll for that attempt; increasing attacker accuracy or temporarily editing target stats is not equivalent.
- Change: reuses existing offence and NPC/player defence formulas with an explicit defence percentage; no persistent stat mutation.
- Evidence: NPC/PvP probability-boundary regression verifies one-quarter defence and unchanged target level.
- Upstream conflict risk: formula helper signatures. Prefer an equivalent upstream per-attempt defence modifier when available. Introducing commit: ancient-warrior slice following `e74577500`.
