# Special attack verification progress

Tier A = pure damage/accuracy specials, no engine dependency. Tier B = applies an effect to
something/someone else - originally thought to need a separate, risky engine-diff patch
(`HitImpactHandler` touching core `Npc`/`Player`/`PathingEntity` classes). That premise turned out
to be wrong for most of Tier B - see "Tier B breakthrough" below.

**Build status: 73 of 73 total weapons compile and run - the source set is now complete.** Since
the last count (70): Bone dagger, Dorgeshuun crossbow, and Vampyre flail (Ivandis/Blisterwood/
Hallowed) - see "Last three weapons unblocked" below. Nothing remains in `disabled-tier-b/`.

`compileKotlin`, `compileTestKotlin`, and `test` all exit 0 for everything currently in the source
set. Test files: written for the weapons with genuine custom math (see "Test files" section below)
- the rest (mostly flat accuracy/damage multipliers) don't have much to test beyond "is this
constant right," not yet done.

## Tier B breakthrough: most of it never needed the engine-diff patch at all

You asked "if you can do all 47, let's go" - traced every `HitImpactHandler` usage across all 47
Tier B files and found the real split was **22 easy** (just needed a mechanical rewrite) vs **26
hard** (need real new subsystems). Two things made the "easy" 22 possible without ever touching
`Npc.kt`/`Player.kt`/`PathingEntity.kt`:

1. **Real OSRS doesn't clamp damage after the roll** (established earlier tonight). Every
   `HitImpactHandler` used only to gate or scale an effect off "how much damage this hit dealt" can
   just use the already-known, pre-clamp `damage` value computed a few lines earlier, synchronously
   - no callback needed. This covered the large majority of cases (Abyssal whip's energy drain,
   Ancient mace/Dragon scimitar/Barrelchest anchor/Elder maul/Statius warhammer/Dogsword's stat
   drains, Saradomin sword/Voidwaker's bonus magic xp, the Armadyl-enchanted-bolt effect family
   shared by Armadyl crossbow and Zaryte crossbow, Seercull's magic drain, Morrigan's javelin's
   bleed, Tonalztics of Ralos's defence drain, Nightmare staff's prayer restore).
2. **For the one genuinely delayed case** (Ancient godsword's mark -> 8-tick-later heal), the
   damage was already a fixed, known-in-advance value (not a live roll) - so instead of a callback,
   the world-queue mechanism already used elsewhere in that same file just re-schedules the heal
   for the same delay the hit itself uses.

Three real (small) pieces of shared infrastructure came out of this, all outside the
`HitImpactHandler`/engine-diff family:
- **`BypassProtectionPrayerPlayerHitModifier`** (`api/player/hit/modifier/`) - Ancient mace and
  Dragon sword's Wild Stab both ignore Protect from Melee entirely; added as a sibling to the
  existing `StandardPlayerHitModifier`, and threaded an optional `modifier` param through
  `PlayerAttackManager.queueMeleeHit`/`SpecialAttackManager.queueMeleeHit` (default preserves every
  other caller's behavior).
- **`PowerOfDeathMeleeProtection`** (`api/player/hit/modifier/`) - Staff of the Dead's Power of
  Death (halve incoming melee damage for 60s while the staff stays equipped). Wired directly into
  `StandardPlayerHitModifier` since it must apply to *all* incoming melee, not just special-attack
  hits.
- **`ProtectionPrayerLockout`** + `Player.disableProtectionPrayers()` (`api/player/prayer/`) -
  Dragon scimitar's Sever (PvP): disables active protection prayers and blocks re-activating them
  for 8 ticks. Required one small hook in `PrayerTabScript.enablePrayer` (the prayer tab's own
  activation entry point) to check the lockout before allowing a protection prayer back on.
- **`defenceMultiplier` threaded through the melee accuracy formula chain** (`PvNMeleeAccuracy`,
  `PvPMeleeAccuracy`, `AccuracyFormulae`, `PlayerAttackManager`, `SpecialAttackManager`) - Vesta's
  longsword's Feint rolls against 25% of the target's defence rather than boosting its own attack
  roll (confirmed these aren't mathematically interchangeable - the real hit-chance formula isn't
  symmetric). Default `1.0` preserves every other caller.

All confirmed via wiki before writing (Dragon scimitar's exact "8 ticks (4.8s)" lockout, Staff of
the Dead's "one minute... lost immediately if the staff is unequipped", Vesta's longsword's
"rolled against 25% of the opponent's defence").

### Last three weapons unblocked (resolved - Bone dagger, Dorgeshuun crossbow, Vampyre flail)

Revisited both previously-deferred gaps and found each was smaller than the earlier note assumed
once actually traced end to end, rather than estimated from the surface.

**`lastDamagingPlayerUuid` (Bone dagger, Dorgeshuun crossbow)**: still a real core-engine touch, but
turned out to have an existing choke point to hang off rather than needing brand new plumbing.
`PathingEntity.recordDamage(source: Player, damage: Int)` already exists and is already called by
the *entire* shared hit-application pipeline for both NPC and player targets (`StandardNpcHitProcessor`
directly, `StandardPlayerHitProcessor` via `recordHitDamage` -> `Hit.recordDamageOn`) - it's the
same function that already powers `heroPoints`/`damageContributions` for loot/XP-sharing on group
kills, so it was already guaranteed to fire on every positive-damage hit from any weapon, special or
not. Added one field, `PathingEntity.lastDamagingPlayerUuid: Long?`, and one line inside that
existing function (`lastDamagingPlayerUuid = source.uuid`) - zero new call sites, zero behavior
change for anything else that reads `heroPoints`/`damageContributions`.

**Vampyre flail's Retainer**: all three "smaller pieces" the earlier note wanted verified
(`clearQueue`, `clearInteraction`, `npcChangeType(npc, into, duration)`) already existed with the
exact signatures the disabled file called - nothing to add there. The one apparently-missing piece,
`Npc.retainerUntil`, turned out to be dead state: it was only ever *written*, never read anywhere,
and `npcChangeType`'s own `duration` parameter already auto-reverts the NPC back to its original
type after the given cycle count - so the "trapped for 30 seconds" behavior was already fully
handled by the engine primitive, not by the field. Removed the two `target.retainerUntil = ...`
lines rather than inventing an unused field.

Both files also still referenced the pre-"Tier B breakthrough" `HitImpactHandler`/`impactHandler`
callback API (removed everywhere else in this project already, see above) - modernized both to the
same synchronous pre-clamp-`damage` pattern used by every other converted Tier B weapon. Also
carried the recurring `height = 96` target-hit-spotanim bug fix (see Osmumten fang/AGS above) into
all three files' target-facing impact spotanims while in there.

`obj.hallowed_flail` had no `items.toml` block at all (unlike its two siblings, `ivandis_flail`/
`blisterwood_flail`, which both already had full animation data) - added one mirroring the other
two exactly, since all three flail tiers share the identical `seq.ivandis_flail_*` animation set.

All three moved out of `disabled-tier-b/` into the live source set and registered in
`SpecialAttackModule.kt`. `BoneDaggerBackstabTest.kt` (pure-logic test for the "was I the target's
last damager" check, already written and waiting) moved with it and passes. Full project
`compileKotlin`/`test` passes. Cache rebuilt (the `hallowed_flail` toml addition needed it). Server
restarted clean, zero errors in the boot log - including zero `SpecialAttackRegistry` collisions,
worth checking explicitly given the Seercull/`daganoth_cave_magic_shortbow` id-collision crash
found earlier this same pass.

### BlowpipeAmmo built (resolved - Toxic blowpipe, Rosewood blowpipe converted)

Both special attack files (`ToxicBlowpipeSpecialAttack.kt`, `RosewoodBlowpipeSpecialAttack.kt`)
were already fully written, sitting in `disabled-tier-b/` and referencing a
`org.rsmod.api.player.ranged.BlowpipeAmmo` API that simply didn't exist in this repo yet. Checked
torka's Downloads contribution first (per the established habit this session) and found the
*complete* class already written there
(`OpenRune-Special-Attack-API-Engine-Diff/.../api/player/ranged/BlowpipeAmmo.kt`) - copied it in
directly and it compiled clean on the first try against this repo's current API surface. Real
mechanism: both blowpipe families pack their dart type/count (and, toxic only, scale count) into
the SAME per-item `vars` bitfield `ObjChargeManager` already uses for single-counter charge
weapons - `varobj.snakeboss_blowpipe_darttype`/`_dartcount`/`_flakes`, three independent bit
ranges on the one packed value, all real, pre-existing cache varobjs (confirmed via
`.data/gamevals/varobj.rscm`). `getBits`/`withBits` (same utils `ObjChargeManager` uses) read/write
each field independently.

**One real bug fixed in the imported file**: the wiki (current, 2025) says the rosewood blowpipe
"is able to shoot up to rune darts," but torka's file capped its `maxDartIndex` at adamant - fixed
the constant (`RUNE_DART_INDEX = 6`, was `ADAMANT_DART_INDEX = 5`) and its doc comment before
deploying, per the standing verify-against-wiki-before-trusting-any-source discipline.

**What was actually still missing, since torka's contribution only had the special attacks**:
- **Normal attacks**: both blowpipes store ammo *inside the weapon item itself*, not the quiver -
  the generic cache-driven ranged fallback (`PvNCombat`/`PvPCombat`) only knows about quiver ammo,
  so neither blowpipe could fire a normal attack at all without a dedicated `WeaponMap`. Built
  `BlowpipeWeapons.kt` (`content/other/special-weapons/.../ranged/`), mirroring each special
  file's own exact logic (dart/scale conservation rolls, 25%/100%-with-serpentine-helm venom
  chance for toxic, no venom at all for rosewood per the wiki) but with 1x multipliers instead of
  the special's boosted ones.
- **`EquipmentChecks.isSerpentineHelm`** - referenced by the special files but didn't exist; added
  (covers the base, charged, and both cyan/red-recolor variants).
- **`obj.rosewood_blowpipe`'s combat data** - the base cache import had no `weaponCategory` or any
  `attack_anim_stance*`/`bas_*` params at all (same missing-combat-data pattern as several other
  recently-added weapons this session) - added a full `items.toml` override using its own real
  named animation (`seq.rosewood_blowpipe_attack`,
  confirmed via `gameval_search`) plus the same generic `bas_*` wield-stance set toxic blowpipe
  already uses (no dedicated named aliases exist for rosewood's own idle/walk poses).
- **Loading darts/scales**: `BlowpipeAmmo.storeDarts`/`storeScales` existed as pure functions but
  nothing called them - a blowpipe could never actually get ammo into it. Built
  `BlowpipeCharging.kt` (`onOpHeldU` for every dart-type/blowpipe-variant pair, plus scales for the
  toxic family), following the exact same pattern as `WebweaverBowCharging.kt`. **Unload/Uncharge
  (returning stored ammo to the inventory) are not wired up yet** - `BlowpipeAmmo.unloadDarts`/
  `uncharge` exist and are ready, just need the actual `onOpHeld*` option hookup; a smaller,
  separate follow-up.

`compileKotlin`/`compileTestKotlin`/`test` all exit 0 project-wide; cache rebuilt (new
`rosewood_blowpipe` items.toml block); server restarted clean.

**Two follow-up bugs found live, both fixed**:
- **`BlowpipeWeapons` was never added to `RangedWeaponsModule.kt`'s bindings.** Unlike
  `PluginScript`s (auto-discovered), `WeaponMap` implementations need an explicit
  `addSetBinding<WeaponMap>(...)` or they're silently never invoked - rosewood blowpipe's normal
  attack fell through to the generic ranged fallback, which fails for a different reason (blowpipe
  ammo isn't in the quiver) but happens to print the exact same message text ("You are unable to
  fire your ammunition."), making it look identical to the dedicated handler's own failure branch.
  Fixed; both blowpipes' normal attacks confirmed working live.
- **`Toxic Siphon`'s spotanim was played on the player at `height = 96`** - the same blind-copy-
  from-Dragon-claws height pattern fixed on many other specials this session, but the deeper issue
  the user caught: `spotanim.toxic_blowpipe_specialattack` (id 1043) is the special's own flying
  projectile effect, not a player-cast effect at all - it was being played statically at the
  player's feet instead of as the projectile heading to the target. Fixed by passing it as the
  `spawnProjectile` spotanim (replacing the dart's own generic travel effect for this hit) instead
  of a separate `spotanim(...)` call on the player. The animation/spotanim aliases themselves
  (`seq.toxic_blowpipe_special_updated` id 876, the spotanim id 1043) were always correct - only
  how they were used was wrong.

**A real damage bug found live and fixed**: reported as "regular rune dart hits harder than the
same rune dart loaded in the blowpipe," which is backwards - the blowpipe should always hit at
least as hard (it adds its own +20 ranged strength on top). Root cause: the loaded dart's own
`ranged_strength` bonus (rune dart: +26, confirmed via cache) is never counted at all when firing
from the blowpipe. The standard equipment-bonus scan (`WornBonuses.calculate`) only sums params off
items actually worn in a `Wearpos` slot - a blowpipe's darts are packed inside the weapon's own
`vars` bitfield instead (see [[blowpipe-ammo-built-from-torkas-source]]), so they were invisible to
it entirely, even though `BlowpipeAmmo.rangedStrengthBonus(obj)` already existed (from torka's
original file) specifically for this purpose and was simply never wired in anywhere. Fixed with the
same one-line-per-formula pattern as the Eclipse atlatl max hit fix: added
`BlowpipeAmmo.rangedStrengthBonus(source.righthand)` to the `rangedBonus` term in both
`PvNRangedMaxHit`/`PvPRangedMaxHit.computeModifiedDamage` - a safe no-op (returns 0) for every
weapon that isn't a loaded blowpipe, since `BlowpipeAmmo.loadedDart` returns null otherwise. Covers
both the special and normal attacks in one fix, same shared-formula-chain reasoning as the atlatl.
Compiled/tested clean project-wide; no cache changes needed; server restarted clean.

**Check/Unload/Uncharge built**, confirmed against a real cache dump rather than guessed - the
live wiki cache (`osrs-wiki-cache-utils`) still has the un-league-ified real toxic/rosewood
blowpipe `config/obj` entries, showing the exact op layout: toxic loaded variants
(`iop2=Wield, iop3=Check, iop4=Unload, iop5=Uncharge`), rosewood loaded
(`iop2=Wield, iop3=Check, iop5=Unload` - no Uncharge, since it has no scales, and its removal op
sits at position 5 instead of 4). `param.wear_op1=Check` confirmed the same "Check" option is also
available while worn, replacing the item's default unequip-by-op1 (real Jagex behavior on this
specific weapon - it can still be removed via the equipment interface). Implemented with
`onOpHeld3`/`onOpHeld4`/`onOpHeld5`/`onOpWorn1`, reusing `BlowpipeAmmo.unloadDarts`/`uncharge`
(present in torka's original file, unused until now) for the actual state changes, and
`invAddOrDropType`/`choice2` (same pattern as `TumekensShadowCharging.uncharge`) for returning
ammo to the inventory with a confirmation prompt.

**A real bug from the first pass, caught live and fixed**: also wired up Check-while-worn via
`onOpWorn1`, since `param.wear_op1=Check` on the real item. `onOpWorn1`'s own doc explicitly warns
it "replaces the default unequip op handling" - and in this engine, op1 while worn is "Remove"
*regardless* of what `wear_op1` claims the real client would show, so registering it made the
Remove button silently run Check instead of unequipping (reported as "the remove option shows
scales and darts, not the check option"). Removed the `onOpWorn1` registration - held `Check` (op3) was
untouched. User confirmed a "Check" option genuinely exists on the equipment-tab screen too, so
this needed fixing properly rather than dropping: switched to `onOpWorn2`, the exact same slot
`TumekensShadowCharging` already uses for its own worn-Check - a real, free slot in this engine
that doesn't touch Remove.

**Player animation bug found live and fixed**: reported as "character just stands still while the
projectile fires" during Toxic Siphon. Root cause: `seq.toxic_blowpipe_special_updated` (used for
the spotanim fix earlier) was ALSO being played on the player via `anim(...)` - but a real cache
dump (`osrs-wiki-cache-utils`) showed it has none of the markers a real player-body seq carries
(`replaceheldright`, `walkmerge`, `heightoffset` - all present on e.g. Rosewood's own
`rosewood_blowpipe_special_attack` seq, all absent here). It's the flying dart's own model
animation, referenced by the spotanim's `anim=` field, not something to separately trigger on the
player - so calling `anim()` with it did nothing visible on a player skeleton. Fixed by reusing the
weapon's own real throwing animation instead (reads `param.attack_anim_stance1` directly off the
weapon, so it naturally picks the right variant for the ornament kit too), matching how Rosewood
blowpipe's own special already uses its own dedicated player seq.

**Projectile flew too high, fixed - and a real build-tooling bug found and fixed along the way**:
Toxic Siphon's flying dart used the shared `projanim.thrown` type (startHeight=163/endHeight=146,
same as every other thrown weapon), but this effect's own model looked far too high in the air at
those heights. Tried adding a brand-new dedicated `projanim.toxic_blowpipe_special` entry to
`projectiles.toml` with lower values (to avoid touching the shared type used by every other thrown
weapon) - this doesn't work the way `items.toml`/`param.rscm` custom aliases do: a raw-cache
`projectiles.toml` entry's `id` must already be a registered RSCM alias, not something you can
invent inline; hit `IllegalStateException: rsconfig constant mapping ... not found in table
'projanim'` during `buildCache`. Tried `projanim.dragonfire` (43/31) as an interim
workaround - reported too low. Went back and did it properly: found `.data/gamevals/projanim.rscm`
is a genuinely small, hand-editable alias table (18 lines, unlike `obj.rscm` which is effectively
empty and backed by a merged binary source elsewhere) - added `toxic_blowpipe_special=18` there,
then re-added the dedicated `projectiles.toml` entry (this time it built clean) with a middle-
ground height (95/70) between the too-high shared "thrown" type and the too-low dragonfire
experiment. Reported still slightly low - nudged both up
further (95/70 → 115/85). Still an unverified visual-tuning guess pending live confirmation.

**Osmumten's fang special: same recurring height=96 bug, only just now caught.** Reported "a bit
too high" - `spotanim.spotanim_weapon_sword_osmumten_special` was still at the blind `height = 96`
copied from Dragon claws early this session, never touched since it wasn't flagged before now.
Dropped to `0`, same fix as every other file that's hit this exact pattern.

While chasing that, also hit and fixed a **real, reproducible `:or-cache:buildCache` bug**,
unrelated to blowpipes specifically: `MinifyServerCache.kt`'s final step
(`temp.copyRecursively(loc, true)`) intermittently threw `FileAlreadyExistsException` overwriting
`loc`'s own cache files, 100% reproducible from a clean `.data/cache/SERVER/` deletion, a stopped
Gradle daemon, and no other process holding the cache open. Root cause: on Windows, closing the
`CacheLibrary` (line `cache.close()`) doesn't deterministically release its memory-mapped file
handles - JVM `MappedByteBuffer` unmapping isn't guaranteed until GC runs, and there's no explicit
unmap API available on this JDK. Added a `System.gc()` immediately before the overwrite copy;
rebuilt clean on the next attempt. A real, generally-applicable fix - not scoped to this session's
blowpipe work - worth remembering if `buildCache` ever throws this same exception again elsewhere.

**One real UX quirk found, not resolved**: loading ammo only works as "use blowpipe on scales/darts,"
not "use scales/darts on blowpipe" - the reverse direction does nothing, even though `onOpHeldU`'s
own doc claims click order shouldn't matter (the framework is meant to normalize which item is
`first`/`second` regardless of which the player clicks first). Spent a long diagnostic pass on this
(registration-time logging confirmed all handlers register with zero exceptions; interaction-time
logging showed the handler never firing for the "scale on blowpipe" direction) before the user found
the working direction empirically. Not chased further since it's unblocked, but a real, unexplained
discrepancy in this engine's click-order-independence claim - worth another look if it recurs
elsewhere.

### Poison/venom + shove-stun/bind unlock (resolved - 7 weapons converted)

Built three small, self-contained services this session, all following the same shape as
`BurnEffectService` above (in-memory, `WeakHashMap`-keyed, self-rescheduling via `WorldQueueList`,
no persistence across logout, no buff-bar/UI integration):

- **`NpcPoisonEffectService`** (`api/mechanics/toxins/`) - NPC-side poison and venom, reusing
  `PlayerPoison`/`PlayerVenom`'s own damage formulas directly (severity decaying 1/tick; venom
  escalating 6/8/10.../20 every 30 ticks) since NPCs get poisoned/envenomed identically to players
  mechanically - only the "where is this stored" part is new (NPCs have no varps). Unblocked
  **Webweaver bow**. Toxic blowpipe and Rosewood blowpipe also needed this, but are still blocked
  separately on the missing `BlowpipeAmmo` (see above) - this piece alone wasn't enough for them.
- **`BindEffectService`** (`api/combat/combat-commons/`) - a duration tracker for melee specials
  that immobilize a target and/or read how long it's already bound for (`Npc.movementLocked` for
  NPCs; a lazy "until" check, no engine flag exists for Player). Deliberately separate from the
  pre-existing magic freeze-spell mechanic (Snare/Entangle/Bind/Ice Barrage,
  `CombatEffects.freeze`/`Player.frozen`) - a target frozen by a spell won't show a remaining bind
  duration here, and vice versa. Unblocked **Zamorak godsword's Ice Cleave** (NPC side; players
  still freeze via the existing spell mechanic) and **Blue Moon spear's Break Shackles** (reads/
  breaks the same tracker).
- **`ShoveStunService`** (`api/combat/combat-commons/`) - the Shove special's stun (Dragon spear,
  Zamorakian spear/hasta). Verified against the wiki's own "Stun (status)" page while building this:
  torka's original file had an invented one-cycle post-stun immunity that doesn't exist in the real
  game ("There is no immunity and thus the stuns from Shove can be chained") - removed it entirely
  rather than porting the bug forward. Scope limit, documented in the file itself: a real stun
  blocks movement, attacking, eating, equipment-changing, and spellcasting, but this only drives
  `Npc.movementLocked` for NPCs and exposes a boolean for other code to check - nothing currently
  enforces the equivalent for player targets (no existing "block all actions" flag to hook into), so
  a stunned player can still act. The knockback itself also uses a plain teleport rather than the
  engine's `exactMove` primitive (a smoothed client-side slide) - `exactMove` has zero existing
  usages anywhere in this codebase to confirm its timing/direction parameters against, so this
  trades away that visual for something unambiguously correct instead of guessing.

You asked directly whether this batch had gotten test coverage - it hadn't, which broke this
session's own established pattern. Went back and extracted the pure logic out of all four new
services plus the two weapons with real inline math, and wrote tests for each: `BurnStacksTest`,
`ExpiringDurationMathTest` (shared by `BindEffectService`/`ShoveStunService`), `NpcPoisonOverrideTest`,
`UnleashMultipliersTest`, `ImpactMeleeSpecialAttacksTest`. Writing the last one caught a real,
previously-unnoticed bug: Dragon warhammer's NPC defence reduction was `current * 70 / 100`, but the
wiki's own worked example (75 -> 53 -> 38 Defence across two hits) only matches `current -
floor(current * 0.3)` - the two formulas floor at a different point and disagree whenever
`current * 30` isn't a clean multiple of 100. Fixed.

Also found and fixed two small pre-existing bugs while wiring these three specials up:
- Zamorak godsword's freeze duration was 33 ticks; the wiki is explicit it's 32 ("19.2 seconds").
- Saradomin godsword's heal/prayer-restore was gated on `impact.damage` (the post-mitigation hit).
  The wiki is explicit this is wrong: "calculated from the potential damage of a swing *before* some
  types of damage immunities are applied" and "before flat armour is applied" - switched to the
  already-known pre-mitigation roll, matching this session's established "real OSRS doesn't clamp
  damage after the roll" pattern (this isn't just an engine-diff workaround here - it's the
  wiki-documented *correct* behavior). Bandos godsword and Dragon warhammer were also switched from
  gating on `impact.damage` to gating on the accuracy roll itself, per the wiki's explicit contrast
  ("unlike... Bandos godsword... the dragon warhammer only needs to roll a successful hit").

### Missing normal-attack audit (Thunder khopesh's bug wasn't unique)

You asked whether other converted weapons might have the same "special works, normal attack is
broken/incomplete" gap Thunder khopesh had. Checked it directly rather than guessing: spot-checked
cache animation params (`attack_anim_stance*`) for every recent/limited-release item among the
converted weapons (Bounty Hunter, Leagues, quest-reward exclusives - the same category Thunder
khopesh belongs to). Found and fixed two more real, complete gaps (same as khopesh):
- **Crimson kisten** (June 2026 Maggot King drop) - zero animation params. Fixed with the item's own
  real named animations (`seq.human_weapons_crimson_kisten_attack`/`_attack_alt`), not a generic
  reuse - matches the wiki's own trivia about which styles kept which animation after a post-release
  hotfix.
- **Sunspear** (Blood Moon Rises quest reward) - zero animation params. Only its *special* attack has
  a named animation in gameval; its normal swing reuses the generic Spear-category fallback (same
  set the rune spear uses).

And confirmed **Fang of the Hound** has the exact same passive-and-animation gap as Thunder khopesh:
wiki - "every hit... has a 5% chance to cast Flames of Cerberus... without needing to pass a second
accuracy check" - never implemented, and its cache entry is also missing animation params entirely
(same Leagues-exclusive-item pattern). Fixed both pieces the same way as khopesh: a new
`FangOfTheHoundWeapons.kt`, hardcoded animation (no cache dependency), and a
`WeaponAttackManager.rollSpellMaxHit` wrapper (same story as the `SpecialAttackManager` one added
earlier - the underlying `PlayerAttackManager` function already existed).

Checked several more suspects and ruled them out with real evidence, not assumption: Morrigan's
throwing axe (ranged weapons only need one animation param, `attack_anim_stance1` - already present)
and Voidwaker (fully populated, matches Dragon scimitar's own values exactly) are both fine.
Statius's warhammer is missing its stance-3 slot, but warhammers only have 3 real combat styles
(Pound/Pummel/Block) - there's no 4th style to be missing an animation for, so that's not a bug.

**Update: did the full systematic sweep.** You found Dogsword had the same bug, which prompted
checking every recently-released weapon's cache data directly instead of waiting for more reports.
Confirmed and fixed **8 items total** with a complete `attack_anim_stance*` gap (all also missing
`weaponCategory`, so all showed "Unarmed"/Punch-Kick-Block before this): Thunder khopesh (both
League and Deadman variants), Fang of the Hound, Crimson kisten, Sunspear, Dogsword (both Echo and
Deadman variants), Burning claws, Arkan blade. Fixed each with the closest real reference: Dogsword
and Burning claws both cite real sounds/animations on their own wiki pages (Burning claws literally
lists "dragonclaws_normal" as its attack sound), so those reused Dragon claws'/a generic
TwoHandedSword's real set rather than a guess.

Checked and confirmed fine (fully populated cache data, no fix needed): Voidwaker, Morrigan's
throwing axe/javelin, Statius's warhammer, Vesta's longsword/spear, Zaryte crossbow, Blue Moon
spear, Tonalztics of Ralos, Osmumten's fang, Soulreaper axe, Eclipse atlatl, Dual macuahuitl,
Saradomin's blessed sword, Webweaver bow.

**This is now a real, completed sweep of every recently-released weapon among the tracked 68 - not
just the "obviously similar" ones** - if another one still turns up broken, it'd be a genuine
surprise rather than an expected gap.

**Correction after your screenshots**: Crimson kisten and Fang of the Hound were still showing
Punch/Kick/Block ("Category: Unarmed") even after the animation fix. Root cause: `weaponCategory`
itself was never set for either (I'd only fixed that for Thunder khopesh) - the combat-style tab is
rendered client-side straight from the item's cache definition, so this genuinely can't be worked
around in server code the way the animation was. Added `weaponCategory="Spiked"` (Crimson kisten)
and `weaponCategory="StabSword"` (Fang of the Hound) to their cache overrides and rebuilt - this
time killing the actual lock-holder (the osrs-mcp tool, not RSProx/the game server) proactively
first, and it went through clean on the first try. Also swapped Fang of the Hound's animation from a
generic sword swing to its own real one, `seq.human_karambit_attack` - you correctly guessed it's
the non-special counterpart to `seq.human_karambit_spec` (same swing, no swipe-up finish), and it
turned up in gameval exactly as described.

### `BurnEffectService` unlock (resolved - 3 weapons converted)

Built a real Burn status-effect subsystem from scratch (`api/mechanics/toxins/BurnEffectService.kt`)
since it never existed. Verified against the wiki's own "Burn" status-effect page, not just the
individual weapon pages: 1 damage every 4 ticks, a normal instance lasts 40 ticks (10 total
damage), up to 5 independent stacks active at once with the tick *frequency* staying fixed but the
*amount* per tick equal to however many stacks are currently active, and a 6th application while at
5 stacks is simply discarded. Coded as `HitType.Typeless` (not `Ranged`, despite the wiki calling it
"coded as ranged damage" for NPC-immunity purposes) specifically to match the wiki's explicit
carve-out that Protect from Missiles does **not** reduce it. Self-contained, in-memory (a
`WeakHashMap<PathingEntity, MutableList<Int>>` of remaining-tick counts per stack, self-rescheduling
via `WorldQueueList` every 4 ticks) - no buff-bar icon, no persistence across logout, and NPC
ranged-immunity/burn-severity-tier interactions aren't modeled. Same scope tradeoff as this
session's other timed-effect additions (`VestaSpearCombatImmunity`, `MorriganHamstring`).

Also added `consumeRemainingDamage(target)` - sums and clears every active stack's remaining
damage, ends the burn entirely - needed by Eclipse atlatl's own special attack, which (per the
wiki) converts unspent burn damage into a one-off max/min hit bonus.

This unblocked **Arkan blade, Burning claws, Eclipse atlatl** - moved out of `disabled-tier-b/`.
Arkan blade needed no other changes (`burns.apply(...)` was already correct). Eclipse atlatl also
had the fake `HitImpactHandler` bug (`queueMagicHit` doesn't have that parameter) and was missing
`SpecialAttackManager.rollMagicalRangedAccuracy` (same story as the melee equivalent earlier -
already fully implemented in `PlayerAttackManager`, just needed the wrapper).

### Morrigan's throwing axe (resolved)

The only missing piece was `Player.runEnergyDrainMultiplier[Until]`, which never existed anywhere in
the engine. Added `MorriganHamstring` (`api/player/hit/modifier/`) - an `AttributeKey`-based timed
flag, same shape as `VestaSpearCombatImmunity` - and hooked it into
`PlayerRunUpdateProcessor.decreaseRunEnergy()` (the one real per-tick run-energy-loss calculation)
to multiply the loss by 6x while active. Also had the fake `HitImpactHandler` bug in
`queueRangedHit`; the accuracy roll was already known synchronously, so the hamstring effect is
applied directly instead of waiting for an impact callback.

### `PvPAreaAttackManager` unlock (resolved - 8 weapons converted)

Turned out this needed far less new engine work than feared. `AreaMeleeTargetSelector.kt` (the
shared target-picker for every AOE melee/ranged special) was already fully written and
self-contained - it only needed a real `PvPAreaAttackManager` at
`api/combat/combat-scripts/.../org/rsmod/api/combat/player/PvPAreaAttackManager.kt` providing
`canAttack(source, target): Boolean` and `applySecondarySpecialAttack(source, target)`. Both turned
out to be thin wrappers around infrastructure that already exists for the game's *normal* PvP
combat path:
- `canAttack` reuses the existing `Set<PvPAttackValidateHook>` hooks (the same wilderness-level-
  range/Ferox-Enclave/would-skull-prevention checks `PvPCombatScript`'s own `attemptCombatOp`/`Ap`
  already runs for a normal attack) plus the existing `Player.isValidTarget()` check.
- `applySecondarySpecialAttack` replicates what `PvPCombat`'s `applyPkVars`/`applySpecialAttackHooks`
  already do for the *primary* target (skull hooks, `setPkVars`, special-attack hooks) - needed
  separately because secondary AOE targets never go through `PvPCombat.attack` at all; the special-
  attack file calls `manager.queueMeleeHit`/`queueRangedHit` on them directly.

Placed in the same package/module as `PvPCombat`/`PvPCombatScript` specifically so it could reuse
the module-internal `setPkVars(target: Player)` extension function directly instead of duplicating
its pk-var bookkeeping.

This unblocked all 8 files that only needed `PvPAreaAttackManager`: **Crystal halberd, Dinh's
bulwark, Dragon 2h sword, Dragon halberd, Dragon crossbow, Rune thrownaxe, Thunder khopesh, Vesta
spear** - moved out of `disabled-tier-b/` along with `AreaMeleeTargetSelector.kt` itself.

Three real bugs found and fixed while moving these in (none are `PvPAreaAttackManager`'s fault -
pre-existing issues in torka's original files that only a real compile could surface):
- **`HitImpactHandler` isn't a real type anywhere in this engine.** `Dinh's bulwark` and `Rune
  thrownaxe` both referenced it (`queueMeleeHit`/`queueRangedHit impactHandler = HitImpactHandler {
  ... }`), but neither function actually has an `impactHandler` parameter - grepping the whole repo
  found `HitImpactHandler` used only inside still-disabled torka files, never a real declaration.
  Same fix as the rest of tonight's Tier B work: the information the handler wanted (whether the
  hit actually did damage / whether the accuracy roll succeeded) is already known synchronously
  before the hit is queued, since real OSRS doesn't clamp damage after the roll - so both became
  plain synchronous checks (`if (damage > 0)` for the shield-bash drain, `if (roll.successful)` for
  continuing Rune thrownaxe's ricochet chain) instead of a callback. **Worth checking for the same
  pattern in the remaining 14 disabled files before assuming any of them "just needs
  `HitImpactHandler`" wired up** - it may not exist to wire up at all.
- **Vesta spear's Spear Wall anim/spotanim used raw cache ints** (`player.anim(8184)`,
  `spot = 1627`) against functions that only take `String` RSCM names. Same situation as Crimson
  kisten earlier - these two cache-native effects don't have named RSCM aliases in this revision -
  fixed with `RSCM.getReverseMapping(RSCMType.SEQ/SPOTANIM, id)`, the same reverse-mapping pattern
  already used throughout this file family for cache-only spotanims/projanims.
- **Vesta spear's melee-only immunity was wrong per the current wiki.** Built as
  `VestaSpearCombatImmunity` (not the `VestaSpearMeleeImmunity` name torka's file imported) because
  the wiki is explicit the effect blocks *both* melee and ranged damage ("the user becomes immune to
  melee and ranged attacks for 8 ticks" - ranged immunity was added in a later real-game update).
  Wired into `StandardPlayerHitModifier` (checked before the protection-prayer/Power-of-Death
  branches, same as `adminGodMode`) since it has to block *all* incoming melee/ranged, not just
  special-attack hits.

## Bugs found live-testing the newly-converted Tier B weapons

- **Ancient godsword's mark spotanim** - was `target.spotanim` (enemy-anchored), which you spotted
  as looking flat/static instead of following the sword like the real wiki screenshot. Same
  "combat-slot weapon effects are attacker-anchored" pattern as Dragon claws - switched to bare
  `spotanim`. Still looked slightly off after that (floating above/behind the blade); dropped
  `height` from 96 (copied blindly from Dragon claws) to 48. **You confirmed this looks good.**
  Flagged Armadyl godsword as needing the same careful re-check, since its "looks good" was a much
  quicker pass than this one got.
- **Every enchanted bolt effect (all 10, on Armadyl and Zaryte crossbow) was missing its own
  activation spotanim entirely** - Ruby's Blood Forfeit was the one you found first, but tracing it
  fully turned up the whole set (wiki's own effect names, cross-referenced to `spotanim.xbows_*`
  cache assets - Opal "Lucky Lightning", Jade "Earth's Fury", Pearl "Sea Curse", Topaz "Down to
  Earth", Sapphire "Clear Mind", Emerald "Magical Poison", Ruby "Blood Forfeit", Diamond "Armour
  Piercing", Dragonstone "Dragon's Breath", Onyx "Life Leech"). All target-anchored per their wiki
  descriptions ("strikes the target", "appears around the target", etc.) - including Ruby, which
  turned out to belong on the *target* (where the blood is drawn from), not the shooter, despite
  the "sacrifice" framing; you caught that live. Added `spotanim` as a field directly on the
  `ArmadylEnchantedBolt` enum and wired one shared call into `applyEffect`/`zaryteApplyEffect` so
  every bolt gets it without duplicating the call per-effect.
  - Also fixed: Ruby's self-damage used `player.statSub("stat.hitpoints", ...)`, which silently
    changes the stat with **no hitsplat at all** - switched to `takeInstantHit(...)`, matching Dual
    macuahuitl's already-correct pattern for the same kind of self-damage. Checked the rest of
    tonight's weapons for the same `statSub`-on-hitpoints anti-pattern - this was the only instance.
  - **Found, not fixed (same known gap as the deferred Tier B poison/burn work):** Jade (freeze)
    and Emerald (poison) currently do nothing against NPCs - `CombatEffects.freeze`/`poison` only
    accept `Player` targets, and NPC poison specifically needs the same `NpcPoisonEffectService`
    that Toxic blowpipe/Rosewood blowpipe/Webweaver bow are already blocked on. Not a new gap, just
    confirmation it extends here too.
  - Not yet live-tested.
- **Vesta's longsword's Feint - `::maxhit` bug, same category as Dragon claws/Dual macuahuitl.**
  The damage roll (`random.of(range)`) never checked `player.adminMaxHit`, so the cheat silently
  did nothing for this weapon's spec - same root cause as every other maxhit bug tonight. Extracted
  a `resolveDamage` function (same pattern as everywhere else) and added the check, with 3 new test
  cases in the existing `VestaLongswordFeintDamageTest.kt`. Not yet re-tested live.

## Fixed (real bugs found and corrected)

- **Armadyl godsword** - `spotanim` had no receiver; switched to `target.spotanim`. You live-tested
  this after the fix and confirmed it looks good - kept as target-targeted. **Live-confirmed, but
  see the Ancient godsword entry below - that "looks good" was a quick check, not a careful
  wiki-screenshot comparison, and AGS's own target-anchored spotanim turned out wrong under that
  closer look. Armadyl godsword deserves the same re-check before fully trusting it.**
- **Abyssal bludgeon** - same fix (`target.spotanim`), originally based on torka's own Discord bug
  report that the Penance visual played on the attacker instead of the enemy on his branch. You
  live-tested it after the Dragon-mace-class revert below and confirmed it's on the enemy, correct.
  **Live-confirmed.**
- **~~Dragon dagger, Dragon longsword, Dragon mace, Dual macuahuitl, Granite hammer, Osmumten's
  fang, Rune claws, Soulreaper axe~~ - REVERTED, this was a wrong bulk fix.** I generalized from
  torka's one specific Abyssal bludgeon complaint into a blanket "self-targeted spotanim =
  always a bug" rule and applied it across this whole class without checking each weapon
  individually. You live-tested it and the effect was landing on you (wrong) for this group even
  after the "fix." Real counter-evidence for the revert: our own verified, live damage-tested
  Dragon claws uses the exact same bare `spotanim(..., slot = constants.spotanim_slot_combat)`
  pattern and has never had an animation complaint; the standard (non-special) ranged combat code
  in `PvNCombat.kt` uses the identical bare pattern for its launch/muzzle-flash effect; and an
  earlier, already-committed version of Dragon mace (before torka's contribution) also used bare
  `spotanim` - which lines up with you not remembering an issue there before. Conclusion:
  `spotanim_slot_combat` without a receiver is a weapon-effect overlay on the *attacker's own*
  animation, not a target impact effect - reverted all 8 back to bare `spotanim(...)`. **You
  re-tested Abyssal bludgeon, Dragon dagger, Dragon mace, etc. after the revert and confirmed
  bludgeon lands on the enemy and the rest are back on the attacker/weapon as expected - all
  live-confirmed except Soulreaper axe, which isn't reachable in-game yet (Soul Stacks don't
  build up currently, unrelated to this fix) and Osmumten's fang/Rune claws which you didn't
  explicitly call out but should still be checked.**
- **Granite hammer height** - separately from the receiver revert above, you flagged the effect as
  "a little too high" compared to the real wiki reference screenshot (which shows it low, basically
  on the weapon) even with the correct (attacker) receiver. `height = 96` was blindly copied from
  Dragon claws when this file was written; the engine's own default is `height = 0` (no vertical
  offset). Dropped to `0`. This is a visual-tuning guess, not something verifiable from source like
  the receiver was - **needs your re-test to confirm**, not yet live-confirmed.
- **Magic shortbow** - a second issue on the same line as the animation bug: the "launch" spotanim
  (the muzzle-flash effect, which - unlike the others above - correctly belongs on the *shooter*,
  not the target) was wrongly given a `target.` receiver by my own first-pass fix, and also had a
  nullable/non-null type mismatch. Both corrected; matches the same pattern already proven correct
  in `PvNCombat.kt`'s standard ranged attack handling.
- **Magic bow** - was using the standard ranged max-hit formula, but the wiki documents Powershot
  as a custom formula that ignores gear ranged strength, prayers/void, and Slayer helmet(i) -
  only visible Ranged level and the ammo's own ranged strength matter. Reused the already-correct
  implementation from Magic shortbow's own Snapshot special (same formula, same constants).
- **Dark bow** - Descent of Darkness (regular arrows) had no upper damage cap at all
  (`5..Int.MAX_VALUE`). Wiki confirms both variants cap at 48 (`5..48` / `8..48`). Fixed.
- **Soulreaper axe (Behead)** - two real bugs:
  1. Accuracy and damage were both boosted by the same 6%-per-stack figure. The wiki is explicit:
     12% accuracy per stack, 6% damage per stack - different numbers. Fixed with separate
     multipliers.
  2. The wiki also documents a *minimum* damage floor (30% of the boosted max hit at 5 stacks),
     which the standard damage-rolling function has no way to express (it only rolls `1..maxHit`).
     Implemented a custom roll for this, following the same pattern as Dragon claws' `rollRange`
     (including respecting `::maxhit` for testability).
- **Dual macuahuitl (Blood Infusion) - `::maxhit` bug, same category as Dragon claws.** You
  reported the spec (19,10=29 total) looking weaker than a maxed regular attack (17,17=34 total)
  and asked if it might just be the display. Traced it fully: real Dual macuahuitl's *normal*
  attack already deals 2 hits, each independently rolled at HALF the weapon's full max hit
  (confirmed in the actual normal-attack file, `content/other/special-weapons/.../
  DualMacuahuitlWeapons.kt`, which calls `rollMeleeDamage` twice with `maxHitMultiplier = 0.5`) -
  so "17,17" is two half-hits of a full 34 max. The special's own `normalMax` variable is that same
  *full*, un-halved value, and with the existing 25%-total-boost math that gives valid per-hit
  ranges of roughly 4-21 and 5-21 - 19 and 10 both fall inside those, so **the formula itself was
  never wrong**; 29 total is just an unlucky legitimate roll, not a bug in the multiplier logic.
  The actual bug: the special's custom `rollDamage` lambda never checked `player.adminMaxHit`, so
  `::maxhit` silently did nothing here (identical root cause to the original Dragon claws bug
  fixed earlier this session). Fixed - now forces `range.last` when the cheat is active, so you
  should get a real maxed total noticeably above 34 on retest.
- **Soulreaper stack decay** (shared helper, not a weapon file itself) - two bugs found while
  cross-checking Soulreaper axe:
  1. It was healing the player 8 HP for every stack lost to *passive decay* (30s of not attacking
     with the axe). That heal belongs only to actually using Behead to consume stacks - decay
     should be a pure loss. Removed.
  2. The decay interval was 20 cycles (12s); the wiki says 50 ticks (30s). Fixed.

## Real feature gap found and fixed: Soul Stacks were never generated

You asked directly whether Soul Stacks were ever implemented correctly - traced it and confirmed:
no. What existed before tonight: Behead (consuming stacks) and decay (losing stacks after 30s of
inactivity), both of which I'd touched earlier this session, plus the Strength bonus from *held*
stacks (`MeleeMaxHitOperations.calculateEffectiveStrength` already read `varp.soulreaper_stacks`
and applied +6% Strength per stack). What was missing: the actual *gain*. No file anywhere
registered a normal-attack handler for the axe at all - it fell through to the generic melee
weapon path, which has no notion of stacks. So stacks could only ever go down, never up. Not
something I broke; a pure gap from before I started, presumably never in torka's patch either.

Wiki ("Soul stacks" section on the axe's own page) is explicit: a stack is generated on *every*
attack with the axe (even a miss) until five are held, applied *after* that swing's own damage is
calculated (so the swing that generates a stack doesn't benefit from the bonus it just earned).
Implemented as a new `content/other/special-weapons/.../melee/SoulreaperAxeWeapons.kt`, registered
in `MeleeWeaponsModule.kt`: an ordinary Crush attack that, after `queueMeleeHit`, increments the
stack (capped at 5 via a small pure `SoulreaperStackGain.nextStackCount` - tested in
`SoulreaperStackGainTest.kt`) and resets the decay timer. Added the `api.mechanics.toxins` gradle
dependency to `special-weapons/build.gradle.kts` for this. Compiles and tests clean - **not yet
live-tested**, since this is brand new code, not a refactor of something already confirmed working.

## Ancient godsword's full history (now fully converted, in the live source set)

- Earlier tonight: the "apply the mark" `HitImpactHandler` gate (on landing the initial hit)
  simplified to a synchronous check, since real OSRS doesn't clamp damage after the roll.
  - Also checked and **did not change**: I initially suspected `HitType.Typeless` was wrong here
    (thought Protect from Magic should reduce the delayed hit), but the primary, current wiki page
    is explicit that this damage is deliberately typeless and unaffected by Protect from Magic -
    a secondary source I glanced at first was outdated/beta info. Caught before making the change.
- Later tonight (Tier B batch): the *delayed heal* `HitImpactHandler` use (8 ticks later) converted
  to the world-queue re-schedule pattern - the file moved out of `disabled-tier-b/` entirely.
- **You reported the mark's spotanim looking wrong** - flat/static, planted on the ground, not
  following the sword like the real wiki screenshot shows. The code had `target.spotanim` (enemy-
  anchored), which is right for an *impact* effect but wrong for a *weapon-swing* effect like this
  one - same "combat slot spotanim should be attacker-anchored" pattern established earlier tonight
  for Dragon claws etc. Reverted to bare `spotanim` (attacker-anchored). **Not yet re-tested.**
    Heal caps (15%, capped 25 NPC / 15 player) were already correct.

## Verified correct, no changes needed

- **Dragon claws** - ours (already independently verified earlier), kept over torka's version
  (his used a different "roll a total, divide by 2" algorithm that doesn't look equivalent to the
  wiki's own worked example).
- **Abyssal dagger** - 25% accuracy, 15%/5% damage reduction (regular/imbued), single accuracy
  roll for both hits, slash defence - all match exactly.
- **Dragon battleaxe** - 10% drain to Attack/Defence/Ranged/Magic, Strength boost = 10 +
  (drained/4) - exact match.
- **StatBoostSpecialAttacks** (dragon/infernal/trailblazer/crystal axe, harpoon, pickaxe +
  Excalibur) - all the +3 skill boosts and Excalibur's +8 Defence match.
- **Dragon candle dagger** - trivial always-0-damage cosmetic special, correct as written.
- **Ballista (light/heavy)** - 25% accuracy + damage, matches your own live test.
- **Dragon knife** - two independent ordinary throws, explicitly no bonus per the wiki.
- **Dragon thrownaxe** - 25% accuracy only, guaranteed next-tick attack.
- **Granite maul (all variants incl. ornate handle)** - Quick Smash is a plain Crush attack with
  no accuracy or damage bonus at all ("unlike most special attacks, Quick Smash does not increase
  accuracy" - wiki); code uses `accuracyMultiplier = 1.0, maxHitMultiplier = 1.0`. The 60%/50%
  energy cost split between base/ornate-handle variants is read from cache data per-obj, not
  hardcoded. Matches.
- **Noxious halberd (Virulence)** - self-buff, no target at all (cures your own poison/venom).
  Its bare `spotanim` call was *already* correct - my first-pass bulk fix wrongly "corrected" this
  one too, caught by a real compile error (`target` doesn't exist in this function) and reverted.
- **HalberdSpecialVisuals, RangedSpecialAttackEffects** (shared helpers) - simple, correct
  utilities, nothing weapon-specific to verify.

## Newly converted from Tier B tonight (22 weapons, not yet live-tested)

All confirmed to compile and match their wiki mechanic, but this is brand-new code (not a refactor
of something already proven) - needs a first live test, same as the Soul Stack gain feature. Full
list: Ancient godsword (mark-gate simplified, delayed heal still uses the world-queue re-schedule
pattern), Ancient mace, Abyssal whip, Barrelchest anchor, Brine sabre, Demonbane (Darklight/
Arclight/Emberlight), Dogsword, Dragon scimitar, Dragon sword, Elder maul, Infernal tecpatl (reuses
Dragon claws' cascade math), Nightmare staff, Saradomin sword, Staff of the dead, Statius
warhammer, Vesta longsword, Voidwaker, Armadyl crossbow, Morrigan's javelin, Seercull, Tonalztics
of Ralos, Zaryte crossbow.

See "Tier B breakthrough" above for the still-deferred 26 (`disabled-tier-b/`).

## More converted since (12 weapons, not yet live-tested)

Four "hard" one-off weapons turned out to need far less than their original categorization implied
- in each case the actual roll/accuracy function already existed somewhere in `PlayerAttackManager`/
`AccuracyFormulae`, just missing a `SpecialAttackManager`-level wrapper (or, for Sunspear, missing
nothing at all - just a different way of expressing what already existed):
- **Crimson kisten** - the missing `dev.openrune.CrimsonKisten` constants object never existed;
  the item *does* have a real RSCM alias in this revision (`obj.crimson_kisten`) despite torka's
  comment claiming otherwise, but the cache genuinely lacks a `param_1564` special-energy param
  (hence the raw-`Int`-energy `registerMelee` overload). Fixed by inlining the real constants
  directly instead of the missing external object.
- **Sunspear** - "rolls with exactly 70% of its maximum accuracy" needed no new engine function at
  all: this engine's attack roll is already deterministic (no RNG) and scaled by `multiplier` before
  the single random hit/miss sample, so it's just the standard `rollMeleeAccuracy` with a
  conditional `0.70` multiplier instead of a separate "fixed roll" mechanism.
- **Saradomin's blessed sword** - only needed `SpecialAttackManager.rollMagicalMeleeAccuracy`
  wrapping the already-fully-implemented `PlayerAttackManager.rollMagicalMeleeAccuracy` (found this
  out the hard way - first added a duplicate implementation directly in `PlayerAttackManager` and
  got a "conflicting overloads" compile error).
- **Fang of the Hound** - same story for `SpecialAttackManager.rollSpellMaxHit`, wrapping the
  already-implemented `PlayerAttackManager.rollSpellMaxHit`/`calculateSpellMaxHit`. Confirmed
  against the wiki: Flames of Cerberus, base max hit 10, guaranteed cast on a landed special hit, no
  separate accuracy check.

Plus the 8 `PvPAreaAttackManager` weapons: **Crystal halberd, Dinh's bulwark, Dragon 2h sword,
Dragon halberd, Dragon crossbow, Rune thrownaxe, Thunder khopesh, Vesta spear** - see the
"`PvPAreaAttackManager` unlock" section above for what each needed and the bugs fixed while moving
them in.

## Test files

None of these files had testable logic separated from the live `SpecialAttackManager` (which has
no test harness anywhere in this codebase), except Dual macuahuitl (torka's own extraction). Same
pattern applied to the rest as it's used: pull the weapon-specific formula into a small pure
object/function that takes its inputs as plain params instead of a `ProtectedAccess` receiver, and
unit test that - the manager-wiring glue stays untested, same as everywhere else.

**Done (batch 1 - genuine custom math, not just a constant):**
- **Dragon claws** (`DragonClawsDamageTest.kt`) - the four-hit cascade tested against all four wiki
  worked examples (35-17-8-9, 0-30-15-16, 0-0-22-23, 0-0-0-46), all four hit-range boundaries, all
  four miss-cascade sympathy patterns, and `resolveRange`'s `::maxhit`/edge-case handling.
- **Soulreaper axe** (`SoulreaperAxeDamageTest.kt`) - accuracy/damage multiplier formulas, the 30%-
  at-5-stacks minimum floor, and the accuracy-fail/maxhit/random resolveDamage paths.
- **Soulreaper stack decay** (`SoulreaperStackDecayTest.kt`, in `api/mechanics/toxins` - this is
  where the class actually lives) - the 50-cycle interval and due/not-due boundary, with the timing
  math split out from the `Player`-attached state so it doesn't need a real player to test.
- **Magic bow** (`MagicBowDamageTest.kt`) - the Powershot formula checked against 3 hand-computed
  cases (confirming it truly ignores everything but ranged level and ammo strength) plus the
  maxhit/random resolveDamage paths.
- **Dark bow** (`DarkBowDamageTest.kt`) - both variants' range/multiplier constants, and the
  floor/cap clamping behavior (below floor, above cap, inside range, and a miss staying 0).
- **Dual macuahuitl** (existing `DualMacuahuitlDamageTest.kt`, extended) - added 3 cases covering
  tonight's `::maxhit` fix (was previously silently ignoring the cheat, same root cause as the
  original Dragon claws bug); the fix itself required extracting the inline `when` block into a
  testable `resolveDamage` function first, same pattern as everywhere else here.

**Batch 2 done.** Went through every remaining Tier A weapon (Abyssal dagger, Armadyl godsword,
Dragon mace, Dragon longsword, Granite hammer, StatBoost weapons, Ballista, Dragon knife, Dragon
thrownaxe, Rune claws, Granite maul, Dragon candle dagger, Noxious halberd - Abyssal bludgeon,
Dragon battleaxe, and Osmumten's fang already had tests from earlier). Most turned out to be pure
"pass a literal constant to the shared manager" with nothing to regression-test beyond what's
already wiki-verified above - extracted and tested only the ones with genuine derived logic:
- **Abyssal dagger** (`AbyssalDaggerTimingTest.kt`) - the second hit's delay differs by target type
  (1 tick vs players, 2 vs NPCs).
- **Ballista** (`BallistaAnimationTest.kt`) - the 4-way animation choice (ornamented x target type).
- **Dragon knife** (`DragonKnifeVariantTest.kt`) - poisoned-variant detection from the item name.
- **Granite maul** (`GraniteMaulStyleTest.kt`) - the combat-stance-to-attack-style mapping.

The rest (Armadyl godsword, Dragon mace, Dragon longsword, Granite hammer, StatBoost weapons,
Dragon thrownaxe, Rune claws, Granite maul's own damage roll, Dragon candle dagger) are flat
manager calls with literal multipliers - no test added, nothing there to catch a regression that
the wiki verification above didn't already confirm. Noxious halberd's toxin-priority branch
(venom > poison > neither) depends on live `PlayerVenom`/`PlayerPoison` state rather than pure
math, so it wasn't a good extraction candidate either.

## Live-test fixup round: Dogsword's real animation, height tuning, and a real energy crash

**Dogsword's animation was cache-correct but visually wrong.** The earlier `items.toml` fix (generic
`TwoHandedSword` category swing, `seq.human_dhsword_slash`/`_chop`) matched what a real plain 2h
sword uses and fixed the "Category: Unarmed"/Punch-Kick-Block bug, but looked like a plain sword
swing, not a godsword's. Real Bandos/Saradomin/Zamorak godswords use different raw animation IDs
(`7045`/`7054`/`7055`, sounds `3847`/`3846`, confirmed via `cache_search` on the real Bandos
godsword) that have **no named RSCM alias** in this cache revision (`cache_search type=seq
id=7045` returns an empty `debugName`). Since `items.toml` params need string names, built
`content/other/special-weapons/.../melee/DogswordWeapons.kt`: a normal-attack `WeaponMap` entry for
`obj.echo_godsword`/`obj.deadman_dogsword` that plays the real animation via
`RSCM.getReverseMapping(RSCMType.SEQ, rawId)` at runtime instead of a cache param - same technique
already used for Vesta spear/Crimson kisten's specials. `weaponCategory` in `items.toml` is still
what fixes the combat-tab UI and stays necessary; the code-side swing now overrides the visual on
top of it.

**Found and fixed two duplicate `[[item]]` blocks in `items.toml`** while working the Dogsword fix:
`obj.bone_claws` and `obj.echo_godsword` each had two separate override blocks (one pre-existing,
one added by an earlier fix pass tonight, without noticing the first). The loader apparently merges
duplicates by key rather than erroring, so nothing broke, but this needed cleaning up regardless -
merged each pair into one block (keeping `tradeable = false` and `param.attackrate` from
`echo_godsword`'s original, pre-existing entry) and deleted the redundant leftovers.

**Height tuning round 2** (same "blindly copied `height = 96` from Dragon claws" pattern as before,
still only fixed on files the user explicitly confirmed too high via live testing - see the earlier
"systemic `height = 96`" note; still not a blanket fix across all remaining files): Bandos godsword,
Saradomin godsword, Zamorak godsword, Dragon warhammer (all four in `ImpactMeleeSpecialAttacks.kt`),
Elder maul (both the launch and target-impact spotanim), Soulreaper axe - all `96` to `48`, each
with the same "unverified visual-tuning guess" disclaimer comment. Confirmed correct and
**deliberately left untouched**: Dragon dagger, and Dragon hasta's own Shove/Unleash spotanims
(user specifically called these out as the one exception that isn't too high, unlike everything
else) - plus whichever "mace" the user tested (ambiguous between Dragon mace and Ancient mace in
their message; left both alone rather than guess wrong on a confirmed-correct file).

**Real crash fixed: "Not enough special energy to take" disconnecting the player.** Reported live
against Dragon spear's Shove (`PvNCombat.attackMelee:77` → `activateMeleeSpecial` →
`SpecialAttackEnergy.takeSpecialEnergy` threw `IllegalArgumentException`, twice, ~16s apart, same
test player). Investigated and ruled out: Dragon spear's real registered cost is `250` (25%, via
cache enum 906/`sa_energy_requirements`, confirmed identical for `obj.dragon_spear` id=1249 and
both `obj.zamorak_spear`/`obj.zamorak_hasta`, all fed through the same `Shove` instance) - well
above the `< 10` "specialized/self-managing" threshold, so `activateMeleeSpecial`'s normal
`hasSpecialEnergy` pre-check does run for it; `Shove` doesn't self-manage energy at all and never
touches `specialEnergyVarp`; no duplicate registration or item-id collision exists for
`obj.dragon_spear` itself. Given the pre-check and the deduction are separated only by `Shove`'s own
body (which never spends energy), the only way this throws is if the player's special energy
changed between those two points - couldn't pin the exact interleaving (a genuine tick-overlap race
under rapid clicking is the most plausible candidate, given both crashes were immediately
reproducible), so rather than keep chasing an exact trigger, **hardened all three
`activateXSpecial` functions in `api/combat/combat-scripts/.../PlayerCommons.kt`** (melee, ranged,
magic - identical shape in all three) to re-validate `hasSpecialEnergy` immediately before calling
`takeSpecialEnergy`, instead of taking unconditionally whenever the special returned `true`. This
closes the entire crash class regardless of its root cause: worst case now is a special executing
for free once, never an uncaught exception mid-tick. `compileKotlin`/`compileTestKotlin`/`test` all
exit 0; cache rebuilt clean; server restarted with zero boot errors.

## Live-test fixup round 3: Dogsword's holding stance, height=0, Fang's proc effect

**Dogsword's swing animation was right but the idle/wield "holding" pose wasn't.** Root cause:
`weaponCategory="TwoHandedSword"` matches a generic 2h sword's combat-tab text ("2h sword") but
`WeaponCategory.kt` has a distinct `GodSword` entry (id 23) with its own note that equipping one
re-sets a client-side wield-stance varp (357) from `10` to `23` via the item's own op script - real
godswords use `GodSword`, not the generic category, even though both display identically in the
combat tab. Switched `obj.echo_godsword`/`obj.deadman_dogsword` to `weaponCategory="GodSword"`.

**Height=48 (this round's earlier guess) was still too high per live feedback** - dropped to `0`
(ground level) on every file touched in the two height-fixing rounds tonight: Arkan blade, Burning
claws, Morrigan's throwing axe, Bandos/Saradomin/Zamorak godswords, Dragon warhammer, Elder maul
(both spotanim), Soulreaper axe. Still the same discipline as before - only files already flagged
live, not a blanket sweep of the remaining ~30 files still at `height = 96`.

**Fang of the Hound's Flames of Cerberus passive** (the on-hit proc from its *normal* attack, not
its special) had the same blind `height = 96` on the spotanim it plays on the target - dropped to
`0` per the same live feedback.

Compiled clean, cache rebuilt, server restarted with zero boot errors.

## New feature: melee weapon poison ((p)/(p+)/(p++) daggers, spears, hastae) - was entirely unbuilt

Reported live: Dragon dagger(p)'s Puncture special appeared to poison the training dummy
*instantly*, unlike Webweaver bow's own poison (already fixed this session, correctly delayed 18s).
Investigation found something more basic than a second instant-hit bug: **no code anywhere applied
weapon poison for melee "(p)" weapons at all.** Grepped every poison call site in `api/` and
`content/` - the only callers of the poison system were the boss-effect DSL, Zaryte/Armadyl
crossbow specials, Webweaver bow's special, and Noxious halberd's normal attack. `items.toml` has
no poison-chance/severity param on any dagger/spear/hasta `(p)` variant, and
`DragonDaggerSpecialAttack.kt` itself has zero poison code. So the "instant splat" wasn't a second
instance of the already-fixed bug - it was something else entirely (unconfirmed; possibly a
misread of Puncture's own two close-together damage hits). Whatever it was, real weapon poison for
melee weapons was simply missing content, confirmed via the wiki (`Weapon poison`,
`Weapon poison(+)`, `Weapon poison(++)`): melee attacks apply poison at a 25% chance (1/4) on any
successful hit, dealing 4/5/6 initial damage per tier.

Built it: **`WeaponPoisonEffect`** (`api/mechanics/toxins/`) rolls the chance and, on success,
calls the already-existing `PlayerPoison.tryPoison`/`NpcPoisonEffectService.apply` - no new damage
math needed, since both were already fixed earlier this session to start the 30-tick recurring
timer instead of hitting instantly. Tier is read directly off the weapon's own RSCM alias suffix
(`_p`/`_p+`/`_p++`, exposed via `ItemServerType.internalName`) rather than a new `items.toml` param
per item - the real cache already carries this consistently across ~90 poisoned melee weapon
variants (bronze through dragon daggers/spears, keris, bone dagger, abyssal dagger, etc.), so
covering all of them needed zero toml edits. Wired into three call sites: `PvNCombat.attackMelee`
and `PvPCombat.attackMelee`'s generic fallback path (covers every dagger/spear/hasta `(p)` variant
that doesn't have its own dedicated `WeaponMap`, which today is all of them), plus
`DragonDaggerSpecialAttack.kt`'s two Puncture hits directly (specials aren't exempt from weapon
poison in real OSRS - each of Puncture's two hits rolls independently). Added
`implementation(projects.api.random)` to the toxins module's `build.gradle.kts` (previously
unused there). `compileKotlin`/`compileTestKotlin`/`test` all exit 0 project-wide; cache rebuilt
(no toml changes needed for this part); server restarted clean.

**Deliberately out of scope for this pass**: ranged poisoned ammo (arrows/bolts/darts/javelins/
knives `(p)`) - the tier lives on the *ammo* item, not the weapon, and consumption runs through
`RangedAmmoManager`'s separate ammo-detraction path instead of the melee fallback above. Same
25%/12.5% chance split and 4/5/6 vs 2/3/4 damage split per the wiki, just a different, larger hook
point - not built yet.

## Ranged weapon poison (arrows/bolts/darts/javelins/knives (p)) - built

The deferred half of the melee weapon poison work above. Added `WeaponPoisonEffect.
rollOnRangedHit(source, target, ammo, damage)` - same wiki-verified math (12.5% chance / 1-in-8,
initial damage 2/3/4 per tier), reusing the already-existing `WeaponPoisonTier.rangedDamage`/
`RANGED_CHANCE_DENOMINATOR` values that were defined but unused since the melee-only pass. Tier
resolution is identical to the melee path (RSCM alias suffix), just applied to whichever item was
actually consumed as ammo - the quiver item for bows/crossbows, or the wielded item itself for
thrown weapons (darts/knives/javelins), matching how `PvNCombat`/`PvPCombat`'s own generic ranged
fallback already resolves `weaponType` for ammo validation. Wired into that same fallback (covers
every standard bow/crossbow/thrown weapon using poisoned ammo automatically) plus
`DragonKnifeSpecialAttack.kt`'s Duality special specifically.

**A second "half-wired" file found**: Dragon knife's special already had real poison *detection*
(`DragonKnifeVariant.isPoisoned`, checking the item's display name for "(p" to pick a themed
travel spotanim/animation) but never actually called into the poison system at all - the visual
distinction existed with no mechanical backing. Added `poison.rollOnRangedHit(...)` after each of
Duality's two independently-thrown knives, same as Dragon dagger's Puncture got for melee. This is
the second time this exact pattern (a weapon "looks" poison-aware but the real poison call was
simply never added) has turned up this session - worth checking any other weapon with its own
poison-flavored visual branching for the same gap.

Added ranged-damage assertions to the existing `WeaponPoisonTierTest`. `compileKotlin`/
`compileTestKotlin`/`test` all exit 0 project-wide; no cache rebuild needed (pure Kotlin); server
restarted clean.

## Eclipse atlatl: real ammo-category bug found, deeper gaps documented (not fixed)

User reported the Eclipse atlatl "doesn't work at all" for normal attacks - the special
(`EclipseAtlatlSpecialAttack.kt`) was already fully built and registered, so this was specifically
about the weapon's ordinary ranged attack. Root cause, confirmed via `items.toml` + wiki + a real
category id lookup: `obj.eclipse_atlatl`/`obj.br_eclipse_atlatl` had `weaponCategory="Bow"` but no
`param.required_ammo` override, so ammo validation (`RangedAmmunition.validateArrows`) fell back to
`category.arrows` - meaning it demanded real arrows in the quiver instead of the atlatl's actual
ammo, `obj.atlatl_dart` (id 28991, real category `category.atlatl_dart` = 1915, confirmed via
`category.rscm`). Any player without arrows equipped (i.e. everyone actually using atlatl darts as
the wiki describes) would always get "There is no ammo left in your quiver." Fixed: added
`"param.required_ammo"="category.atlatl_dart"` to both item entries. Verified in the rebuilt cache
directly (`cache_search` on item 29000 shows `params={..., 65415=1915}`, and `65415` is confirmed
as `required_ammo` in `param.rscm`).

**Also needed a second fix after the ammo change**: the real ammo item, `obj.atlatl_dart`, had no
`param.proj_travel` at all (no flying-dart spotanim), so `PvNCombat`/`PvPCombat`'s generic ranged
fallback bailed with "You are unable to fire your ammunition." the moment ammo validation passed.
Added `"param.proj_travel"="spotanim.vfx_atlatl_projectile_01"` (the real atlatl dart travel
effect, found via `gameval_search table=spotanim query="atlatl"`). Confirmed live: normal attacks
and the special both fire correctly now.

**Max hit formula: fixed.** Per the wiki, the atlatl's max hit is uniquely based on the player's
**melee Strength level/strength bonus**, not ranged - "a unique mechanic among ranged weapons" -
while accuracy still rolls off ranged bonus/level as normal. Confirmed via source read that
`PvNRangedMaxHit`/`PvPRangedMaxHit.computeModifiedDamage` (the single shared formula both the
special's `manager.calculateRangedMaxHit` call and the normal-attack fallback's
`manager.rollRangedDamage` → `rollRangedMaxHit` → `calculateRangedMaxHit` chain both go through)
always pulled `bonuses.rangedStrengthBonus(source)` with no override for any weapon. Fixed with one
conditional branch per formula class (`EquipmentChecks.isEclipseAtlatl(source.righthand)`),
matching the exact style already used there for Twisted bow/Dragon hunter crossbow/etc. one-off
weapon behavior. New `EclipseAtlatlMaxHit.computeBaseDamage` (`api/combat/combat-formulas/.../
maxhit/ranged/`) reuses `PlayerMeleeMaxHit.calculateEffectiveStrength`/`calculateBaseDamage`
(literally identical formula shape to the ranged version, just different inputs) with the wiki's
other two atlatl-specific quirks folded in: melee strength prayers (Piety etc., not ranged ones)
and, per the wiki's own trivia, void bonus reads the *ranged* void helm rather than the melee one
even though the rest of the formula is melee-shaped. No melee combat style exists for this weapon
(only Accurate/Rapid/Longrange, all ranged-style), so the strength side uses the flat `+8` baseline
every style implicitly gets - no style grants a strength bonus here. Single fix point covers both
the special and any normal attack, since they share the same formula chain - no separate
`WeaponMap` needed. Not modeled (real but more niche per the wiki: "uses the melee bonuses from
the slayer helmet and salve amulets"): those two amulet/helm procs still apply their *ranged*
percentage in `RangedMaxHitOperations.modifyBaseDamage` for this weapon instead of their melee
one - a smaller, separate gap, not fixed this pass.

**Set effect (20% burn chance): fixed.** Correctly pointed out live: the special's own "consume
remaining burn damage" bonus can never do anything for a solo player without this - there was
simply nothing else applying Burn from the atlatl itself. Built `EclipseAtlatlBurnEffect`
(`api/mechanics/toxins/`), same shape as `WeaponPoisonEffect`: on a successful ranged hit, if the
full Eclipse Moon set (helm/chestplate/tassets/atlatl) is worn, roll 20% (`randomBoolean(5)`) and
call the already-existing `BurnEffectService.apply`. Wired into the same `PvNCombat`/`PvPCombat`
ranged-attack fallback as the max hit fix, right after `queueRangedHit` - deliberately *not* wired
into the special itself, since the special's whole design is to consume burn built up between
casts, not generate more (would be self-consuming and pointless). Also deduplicated the
full-set-worn check: it used to be a private extension in `EclipseAtlatlSpecialAttack.kt`; moved
to `EquipmentChecks.isEclipseMoonSet(helm, top, legs, weapon)`, matching the existing
`isDharokSet`/`isToragSet`-style pattern in that file, so both the special and the new passive
share one definition. Full project `compileKotlin`/`compileTestKotlin`/`test` all pass; no cache
rebuild needed (pure Kotlin); server restarted clean.

## Test coverage: two pure-math functions extracted and covered

Added `WeaponPoisonTierTest.kt` (suffix-detection logic for the new `WeaponPoisonEffect` above,
including a regression guard against a name that merely ends in a literal "p" without being a real
poison suffix, e.g. `iron_dagger_pouch`). Also extracted `AncientGodswordSpecialAttack.kt`'s Blood
Sacrifice heal-cap math (15% of target base HP, further capped by a flat player/NPC ceiling, never
exceeding the damage actually dealt) out of the `WorldQueueList`-coupled `heal()` method into a
standalone `BloodSacrificeHeal.healAmount(...)` object, then added `BloodSacrificeHealTest.kt`
covering all three cap interactions (damage-bound, percent-bound, flat-cap-bound) plus the zero
case. Broader batch test-writing across the remaining untested special-attack files (most of which
are flat accuracy/damage multipliers with little to extract, per the earlier note under "Test
files") is still open - this pass only covered what came up naturally while doing the poison work.

## Soulreaper axe (o) missing normal-attack combat data (resolved)

Same pattern as the earlier "missing normal-attack audit" and Leagues-exclusive-item findings:
`obj.soulreaper_axe_orn` (the ornament kit variant, id 33335) had no `weaponCategory` or
`attack_anim_stance*`/`bas_*` params in `items.toml` at all, so it fell through to the engine's
generic unarmed-shaped fallback for normal attacks despite `SoulreaperAxeSpecialAttack.kt` already
correctly handling the ornament variant for the special. Fixed by adding a second `[[item]]` block
for `obj.soulreaper_axe_orn`, copying the base `obj.soulreaper` block's params verbatim
(`weaponCategory="Axe"`, `seq.ancient_axe_crush`/`_slash` attack anims, `seq.ancient_axe_walk`/
`_idle` stance anims, `equipment_sound=2232`, `attackrate=5`, `defend_anim="seq.human_unarmedblock"`).
Required a full cache rebuild (`items.toml` change). Not yet live-tested by the user.

## Webweaver bow: added Check (charges) and Uncharge, neither existed before

User wasn't sure whether a partial "Unload" exists on this item at all ("we cant unload it i
think, well idk if u can do that gotta check wiki") and separately couldn't check remaining
charges anywhere. Settled both against a real cache dump
(`mcp__osrs-wiki-cache-utils__search_cache`, `config/obj`) rather than guessing:
`wild_cave_webweaver_charged` (27655) has `iop2=Wield, iop3=Check, iop5=Uncharge` - no partial
Unload exists, only an all-or-nothing Uncharge, matching the single pooled-charge model
`ObjChargeManager` already uses for this item (one `varobj.charges_16383` count, not a
dart/scale-style split like the blowpipe). The uncharged variant (`wild_cave_webweaver_uncharged`,
27652) only has `iop2=Wield, iop3=Dismantle` - no Check/Uncharge there, correctly.

Added to `WebweaverBowCharging.kt`: `onOpHeld3` (inventory Check), `onOpWorn2` (worn Check - see
below), and `onOpHeld5` (Uncharge). `checkCharges` just reports the current `ObjChargeManager`
count. `uncharge` mirrors `TumekensShadowCharging.uncharge`'s exact shape: bail if already at 0
charges, bail if inventory has no free space, `choice2` confirmation dialog, then
`charges.removeAllCharges(...)` and `invAddOrDrop(objRepo, ETHER_ITEM, removed)` to return the
revenant ether (`obj.wild_cave_shard`).

Used `onOpWorn2` rather than `onOpWorn1` for the worn Check, on purpose, even though the real
client shows Check at `wear_op1` for this item - same lesson learned the hard way on the toxic
blowpipe earlier this session (`onOpWorn1`'s own doc says it replaces this engine's default
unequip/Remove handling, so registering it for anything else silently breaks Remove). `onOpWorn2`
is the same safe slot `TumekensShadowCharging` already uses for its own worn-Check, so this was
applied preemptively instead of being found as a live bug again.

Full project `compileKotlin`/`test` passes. Cache rebuilt (for the Soulreaper axe items.toml
change - the Webweaver bow change is pure Kotlin and didn't need it). Server restarted clean, zero
errors in the boot log. Neither fix has been live-tested by the user yet.

## Systematic sweep: 29 more "special works, normal attack doesn't" weapons found and fixed

Prompted by the Soulreaper axe (o) fix above - user asked "is there no special attack we have on
unimplemented weapons anymore, cuz soulreaper (o) was like that." Answer: no, there were 29 more.
Found them precisely (not a guess-and-check sweep) by extracting every `obj.*` item alias
referenced anywhere in `content/other/special-attacks/src/main/kotlin` (i.e. every weapon that
already has a *working special attack registered*) and cross-referencing that list against every
`items.toml` block that has `weaponCategory` set but no `attack_anim_stance1` param at all - the
exact same shape of bug as Soulreaper axe (o). This is a stronger technique than the earlier
2026-08-29 sweep (see [[leagues-exclusive-items-missing-combat-data]]), which only checked base
item names and missed the "(o)"/corrupted/deadman sibling ids entirely.

**Fixed by mirroring the real base weapon's `items.toml` block** (same technique as Soulreaper axe
(o) - merge base weapon's animation params onto the variant, but *preserve* any of the variant's
own already-set overrides like a different `attackrate`, since several of these are deliberately
rebalanced for Deadman mode):

- **Bounty Hunter "corrupted" dragon weapons (17 items)**: dagger + (p)/(p+)/(p++), spear +
  (p)/(p+)/(p++), 2h sword, battleaxe, halberd, longsword, mace, scimitar, shortsword, warhammer,
  and the dragon crossbow (`obj.xbows_crossbow_dragon`). All had `weaponCategory` and often
  `equipment_sound`/`attackrate` already set (correctly, sometimes with BH-specific values) but
  zero animation params at all - would have fallen back to the punch/kick unarmed animation on
  every normal hit despite each one's dedicated special attack already working.
- **Deadman mode weapons (8 items)**: `deadman_ags`/`deadman_blighted_ags` (mirrors `obj.ags`,
  the real Armadyl godsword alias - not `armadyl_godsword`), `deadman_darkbow`/
  `deadman_blighted_dark_bow` (mirrors `obj.darkbow`), `deadman_voidwaker`/
  `deadman_blighted_voidwaker`, `deadman_nightmare_staff_volatile`/
  `deadman_blighted_volatile_staff` (mirrors `obj.nightmare_staff_volatile`).
- **`dinhs_bulwark_ornament`** and **`br_dual_macuahuitl`**: same pattern, mirrored from
  `obj.dinhs_bulwark`/`obj.dual_macuahuitl`.
- **`obj.toxic_sotd_deadman`/`obj.toxic_sotd_charged_deadman`**: these had *no items.toml block at
  all* (not caught by the weaponCategory-based sweep at all - found separately by checking every id
  `StaffOfTheDeadSpecialAttack.kt` registers). Added brand new blocks mirroring
  `obj.toxic_sotd`/`obj.toxic_sotd_charged`.
- **`obj.emberlight`**: real DT2 weapon (The Whisperer reward), genuinely has *no* unique
  normal-attack animation anywhere in the cache - confirmed via both `gameval_search` and
  `search_cache` kind=`config/seq`, only its special-attack animation
  (`seq.human_weapon_emberlight_01_spec`) and vfx exist. Wiki's own combat-style table (Chop/Slash
  Accurate, Slash Aggressive, Lunge Stab Controlled, Block Slash Defensive) matches the same
  slash-heavy/one-stab-style shape as Voidwaker and Dragon scimitar exactly, so it reuses their
  same generic `seq.human_sword_slash`/`seq.human_sword_stab`/`seq.human_sword_def` set - real OSRS
  itself doesn't always give a new weapon bespoke swing frames, only a special.
- **`obj.bh_dragon_claws_corrupted`**: this one wasn't just a missing-animation bug - its special
  attack was *never registered at all* in `DragonClawsSpecialAttack.kt` (only `obj.dragon_claws`
  was). Added the missing `registerMelee("obj.bh_dragon_claws_corrupted", DragonClaws(manager))`
  line plus the same animation mirror as the others.

**Found but deliberately not fixed this pass** (lower confidence, need their own look): 3 pairs of
skilling-tool "grip state" variants referenced by `StatBoostSpecialAttacks.kt`
(`trailblazer_reloaded_axe/_empty/_no_infernal`, and the analogous harpoon/pickaxe forms, plus
`crystal_axe_2h`/`dragon_axe_2h`/`3a_axe_2h`) - the `_2h` suffix's exact semantics are unconfirmed
(possibly a cosmetic two-handed grip render used only during the woodcutting animation, not a real
combat-equipped state), so mirroring combat data onto them without checking that first risks
animating something that's never actually the equipped fighting state. Also
`arclight_inactive`/`crystal_axe_2h_inactive` (pre-activation states, correctly out of scope - same
as barrows' "broken" items) and `osb10_dragon_candle` (holiday novelty, not a real combat weapon).

Full project `compileKotlin`/`test` passes. Cache rebuilt (all these are items.toml/new-block
changes). Server restarted clean, zero errors in the boot log. None of these 30 fixes (29 items +
the Dragon claws BH registration) have been live-tested by the user yet.

### AGS special height (resolved) + a proper family-based re-sweep (4 more registration gaps found)

**Armadyl godsword special height fixed** - same `height = 96` bug as Osmumten's fang, in
`ArmadylGodswordSpecialAttack.kt`'s single shared `smash()` function. Dropped to `height = 0`.
Covers all five registrations that share it (`ags`, `br_ags`, `deadman_ags`, `agsg`,
`deadman_blighted_ags`) in one fix. Pure Kotlin, no cache rebuild needed.

**User live-tested `bh_dragon_claws_corrupted` and confirmed the special still said "not
implemented"** despite the registration fix being verifiably deployed (checked the actual compiled
bytecode in both the jar and the classes dir the server loads from, not just the source - it was
there). Turned out the user was actually wielding a *different* item, `obj.deadman_blighted_dragon_claws`
(28534, "Dragon claws (cr)" for Deadman mode) - a sibling variant I hadn't touched at all, not the
Bounty Hunter one (28039) I'd fixed. Both display similarly enough to look like the same bug report.

**This prompted redoing the sweep properly** - the previous technique (cross-reference items
*already referenced somewhere* in special-attacks `.kt` files against items.toml animation gaps)
structurally cannot catch a weapon family where SOME siblings are registered but others were simply
forgotten entirely, since the missing ones never show up as "referenced" in the first place. New
technique: extract the exact `obj.*` id argument passed to every `registerMelee`/`registerRanged`/
`registerMagic`/`registerInstant` call, strip known variant prefixes (`bh_`, `deadman_`,
`deadman_blighted_`, `br_`, `cert_`) and suffixes (`_corrupted`, `_ornament`, `_orn`, `_imbue`,
`_inactive`) to get each weapon's "family root", group registered ids by root, then for every root
grep `items.toml` for every id containing that root and diff against the *global* registered set
(not just that root's own subset - needed to avoid false positives from ids like
`dragon_dagger_p`/`_p+`/`_p++` each becoming their own separate root under this stripping scheme).

**Real gaps found and fixed** (registration only - all four already had correct normal-attack
animation data in `items.toml`, so this was purely the special-attack side):
- `obj.br_dragon_claws`, `obj.dragon_claws_ornament`, `obj.deadman_blighted_dragon_claws` - the
  full Dragon claws family, added to `DragonClawsSpecialAttack.kt` alongside the two already there.
  `deadman_blighted_dragon_claws` additionally needed the items.toml animation mirror too (same
  gap pattern as the BH corrupted one, just never caught since it isn't referenced by the special
  file at all - the old sweep technique's blind spot exactly).

**False positives correctly ruled out before touching anything** (multi-line `registerX(...)` calls
where the id string is on a line after the opening paren defeat a naive single-line grep, but the
registration is real): `obj.bh_dragon_longsword_imbue`, `obj.morrigans_javelin_bh`,
`obj.statius_warhammer_bh`, `obj.vestas_spear_bh` were all already correctly registered.

**One genuinely interesting false positive, not a multi-line issue**: `obj.daganoth_cave_magic_shortbow`
looked like a real missing Magic shortbow variant (has full combat data, distinct RSCM alias) and
was added to `MagicShortbowSpecialAttack.kt` - which **crashed the server on boot**:
`IllegalStateException: Weapon already has a special attack mapped: 6724`. Turned out
`obj.daganoth_cave_magic_shortbow` and Seercull (registered by raw numeric id `6724`, since
Seercull itself has no RSCM alias in this revision - see the comment already in
`SeercullSpecialAttack.kt`) are literally **the same cache item under two different names** -
Seercull's real internal cache name apparently *is* `daganoth_cave_magic_shortbow` (Jagex dev
naming, presumably referencing where/how it was originally conceived). Confirmed via
`gameval_search`: both resolve to id 6724. Reverted the bad registration immediately. **Lesson**:
a distinct RSCM alias with full combat data is not proof of a distinct underlying item - two
aliases can share one numeric id, and `SpecialAttackRegistry.add` throwing on `AlreadyAdded` at
startup is a hard crash (not a soft failure), so this class of mistake is at least loud and
immediate rather than silently wrong.

**A second real issue hit during the crash-recovery restart**: after reverting and restarting, the
new server process failed with `BindException: Address already in use` on ports 8080/43594 - the
*previous, crashed* server process didn't fully release its listening sockets on the way down.
Had to find and kill the zombie PID directly (`netstat`, not just re-running the normal kill-by-port
step, since the first restart attempt's failure happened AFTER the port had already been bound but
BEFORE "Server ready", so the standard "kill whatever's on 43594 first" step from earlier in the
same restart cycle didn't apply to it).

**Deliberately not fixed, flagged for its own dedicated pass**: `crystal_halberd`'s huge charge-tier
family (`crystal_halberd_100` through `_1000`, `_new`, `_inactive`, plus a whole parallel
`nzone_crystal_halberd_*` set) - none of the ~20 sibling ids are registered anywhere. This is a
different, bigger problem than a forgotten `registerMelee` call: crystal halberd's charge/degrade
system was already pulled out to `disabled-tier-b/` earlier in this project's history (per git
status), meaning this was a deliberate scope cut, not an oversight, and deserves its own look at
the charge-tier damage scaling rather than a blind mirror-and-register pass. Also skipped:
`obj.dummy_dragon_knife_off`/`_p` (internal offhand-slot placeholders, not real player items) and
`obj.macro_broken_dragon_pickaxe` (anti-macro detection fixture, not a real item).

Full project `compileKotlin`/`test` passes. Cache rebuilt (the `deadman_blighted_dragon_claws`
toml addition needed it). Server restarted clean after resolving the crash and the port conflict,
zero errors in the boot log.

## Correction: the "trailblazer_*_no_infernal" family is NOT a cosmetic grip render

User reported "Dragon axe (or) can't be equipped." Looked it up in the real `SERVER` cache by
*name* rather than guessing an alias (`cache_search` with `type="item"`, not `type="obj"` - that
type value returns world objects/locs in this tool, not inventory items, which is why several
earlier `type="obj"` queries this session against the wrong table came back empty) and found "Dragon
axe (or)" is real cache item 25378, whose actual RSCM alias is `obj.trailblazer_axe_no_infernal` -
and the Trailblazer Reloaded League counterpart, `obj.trailblazer_reloaded_axe_no_infernal` (30352),
is also named "Dragon axe (or)" in-game.

This directly overturns the "lower confidence, deliberately not fixed" call from the earlier sweep
above, which speculated the whole `trailblazer_*_no_infernal`/`_empty` family might be "a cosmetic
two-handed grip render used only during the woodcutting animation, not a real combat-equipped
state." That was wrong - they're real, named, wearable weapons (`equipSlot=3` confirmed in the
built cache, no stat requirement blocking them), just following the "empty/no_infernal" naming
OSRS uses for a tool's *depleted infernal charge* state, not a rendering variant.

**Real gap found**: all 9 Trailblazer **Reloaded** League tool variants (axe/harpoon/pickaxe x
base/empty/no_infernal) had zero animation data in `items.toml`, while their original Trailblazer
League counterparts all had it in full - the second league's parallel item set was never converted,
same "forgot the sibling league" pattern as the Deadman/BH variants found earlier. Mirrored the
original league's combat data (`attack_anim_stance*`, sounds, `defend_anim`) onto all 9, skipping
`skill_anim`/`levelrequire` (real woodcutting-tool params that likely have their own distinct
Reloaded-League sequence names - not something to blind-copy from the original league without
checking, and out of scope for an equip/combat fix).

**Caveat turned out to be the real bug.** User confirmed they were actually testing id 25378 -
`trailblazer_axe_no_infernal`, "Dragon axe (or)" - which already had full animation data before the
toml fix above, so the animation gap genuinely wasn't why it couldn't be equipped. Real root cause,
found by checking the item's actual op layout via a live cache dump: this whole family's "Wield"
option sits at **`iop1`**, not `iop2` like almost every other weapon. This engine's *default* equip
handling is hardcoded to only fire on op2 (`HeldInteractions.opHeld2`, and `onOpHeld2`'s own doc
comment: "replaces the default wield/wear op handling") - clicking "Wield" on these items sends an
Op1 interaction, which routes to the generic op1 fallback, which does nothing since no script was
registered for it. Structurally the exact same class of bug as the `onOpWorn1`/Remove issue found
earlier this session on the toxic blowpipe (a real cache op label overriding what this engine
hardcodes for that action) - just on the *held* side instead of *worn*, and blocking Wield instead
of Remove.

Confirmed via a live cache dump that all 18 Trailblazer/Trailblazer Reloaded tool variants (axe/
harpoon/pickaxe x base/empty/no_infernal, both leagues) share this same `iop1=Wield` layout - not
just the one item reported. Fixed with a new `TrailblazerToolWieldFix.kt`
(`content/other/special-weapons/.../scripts/`): registers `onOpHeld1` for all 18 ids, manually
delegating to `HeldInteractions.equip(...)` - the same bypass entry point the engine's own op2
handler calls internally, exposed specifically for this kind of override.

This is very likely not exhaustive across the whole item database - any other real item whose
`iop1` happens to be its primary "Wield"/"Wear" action (rather than the more common `iop2`) would
hit the same bug, and there's no practical way to sweep all ~34k items for this ahead of time.
Fixed the confirmed, reported family; if another specific item turns up unequippable, check its
real op layout the same way before assuming it's the same missing-animation-data pattern as
everything else this session.

Full project `compileKotlin`/`test` passes. No cache rebuild needed for the wield fix itself (pure
Kotlin) - cache was already rebuilt for the animation-data toml fix. Server restarted clean, script
count went 281 -> 282 confirming the new script registered. Not yet live-tested by the user.
