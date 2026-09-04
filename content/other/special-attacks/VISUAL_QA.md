# Special attack visual QA

Live in-game verification pass (spec animation height, spotanim content, freeze/effect visuals).
Not the same as `PROGRESS.md` (that tracks whether the damage/effect logic is implemented at all -
this tracks whether it *looks* right). Check items off as you verify them; leave a note inline if
something's still wrong so we don't lose track between sessions.

## Height — confirmed correct, no further action needed

- [x] Abyssal bludgeon
- [x] Barrelchest anchor
- [x] Ancient mace
- [x] Dragon claws
- [x] Dragon hasta (height + animation - Unleash now uses Sunspear's thrust anim, confirmed live)
- [x] Dual macuahuitl
- [x] Rune claws
- [x] Statius warhammer
- [x] Vesta spear
- [x] Voidwaker
- [x] Armadyl crossbow
- [x] Zaryte crossbow
- [x] Ballista (both variants)
- [x] Dragon crossbow (height only - anim still missing, see below)
- [x] Morrigan's javelin
- [x] Eclipse atlatl
- [x] Webweaver bow (spec)
- [x] Saradomin sword (full fix, see Animation section below)
- [x] Saradomin blessed sword (full fix, see Animation section below)

## Height — reverted back to 96, confirmed correct

- [x] Dragon/Trailblazer/3rd age/Infernal/Crystal axe boost (`StatBoostSpecialAttacks.kt`)
- [x] Abyssal whip
- [x] Dragon dagger
- [x] Dragon longsword
- [x] Dragon mace
- [x] Dragon scimitar
- [x] Dragon sword
- [x] Dragon thrownaxe
- [x] Magic shortbow (height only - anim/projectile still wrong, see below)
- [x] Magic bow (height only - anim still wrong, see below)
- [x] Dark bow (height only - overlap bug still wrong, see below)
- [x] Seercull (height confirmed; visuals now fully fixed too, see below)
- [x] Rune thrownaxe

## Height — still untested

- [ ] Brine sabre (needs underwater to test)
- [ ] Dogsword (height itself - the `statHeal` crash is now fixed, so this should be testable now)
- [ ] Noxious halberd (needs you poisoned to trigger)
- [ ] Vampyre flail

## Animation / effect content bugs (separate from height, not yet fixed)

- [x] Saradomin sword - was only playing `saradomin_lightning` (76) on the target, nothing on the
      caster. Found a real reference implementation of this exact weapon's special (Zenyte-based
      `MouldyToast/Offline_Scape`, `SARADOMINS_LIGHTNING` in `SpecialAttack.java`) with the true
      graphics: caster gets `dh_sword_update_saradomin_god_special_spotanim` (1213, height 0) -
      purpose-built for this special, applied automatically by that engine's combat framework
      before the per-weapon handler runs, not reused from anywhere else; target gets
      `godwars_saradomin_magic_attack_spotanim` (1196, height 0, 30-cycle delay timed to the
      swing's impact) - confirmed live independently before this reference turned up. Animation
      (1132) was already correct. Confirmed live.
- [x] Saradomin blessed sword - same reference confirms it shares the caster glow (1213) and
      target lightning (1196) with the base sword exactly, plus a third effect the base sword
      doesn't have: a ground-location graphic at the target's own tile
      (`godwars_saradomin_light_attk_spot`, 1221, height 0, 30-cycle delay) sent to a coord rather
      than attached to the entity - needed adding `WorldRepository`/`spotanimMap` to this file,
      which didn't have location-based spotanim support before. Damage formula (1.25x max hit,
      single Magic-defence-rolled hit) was already correct - traced `rollMagicalMeleeAccuracy`'s
      own doc comment to confirm it always rolls the target's Magic defence regardless of the
      `attackType` param, matching the wiki exactly. **Needs your confirmation.**
- [ ] Dragon crossbow - special attack animation missing entirely
- [x] Magic bow (longbow) - animation confirmed fine as-is: Powershot has no unique animation in
      the real game either, it's just the weapon's plain normal-attack draw (`playRangedWeaponFx`).
      What was actually broken was the launch/travel colour - same bug as every other special in
      this pack, using the ammo's plain `proj_launch`/`proj_travel` instead of a dedicated effect.
      Every id in the project's custom `sp_attack_` graphic block (246-258) was already claimed by
      another special except 250 (`sp_attack_glow_arrow_launch`) - unused by elimination, and it's
      Powershot's own dedicated glow. Now overrides both launch and in-flight colour with it.
      Confirmed live.
- [x] Magic shortbow - rewritten from scratch off the cache data after a long run of guesswork
      iterations went nowhere. What the data says: `seq.snapshot` (1074, confirmed as the real spec
      anim by multiple RuneLite plugins) is two identical 27-client-cycle draw-and-release cycles
      back to back, so the second arrow looses 27 cycles (~0.9 tick) after the first.
      `sp_attack_snapshot_spotanim` (256) is Snapshot's only graphic - it's in the RS2 spec-graphic
      block (246-258) with `sp_attack_puncture`/`cleave`/`shatter`, all *attacker*-side, and its
      own anim is a single 21-cycle draw glow. So it's the player's per-draw launch glow, NOT a
      target-hit effect - the old code put it on the target, which was the "arrow appears on the
      enemy / weird thing on hit" you saw. The unnamed `glow_arrow_launch/travel` pair (249/250)
      belongs to Powershot/Soulshot by elimination, not Snapshot; arrows in flight are the worn
      ammo's normal `proj_travel`. Implementation: glow on player at delay 0 and again at delay 27
      (second in a different spotanim slot so it doesn't overwrite the first), second projectile is
      a `ProjAnim.copy` of the first with `startTime`/`endTime` +27 (same speed/arc; two
      byte-identical projectiles in one tick render as one). Dark bow's `doublearrow_one/two` was
      ruled out via projectiles.toml (different angle/stepMultiplier = intentional high/low arc).
      Live-tested and confirmed serviceable, **with one follow-up fix**: the arrows themselves were
      firing on the ammo's generic `projanim.arrow` delay (41 cycles, tuned for a normal single
      draw), so both fires leaked out after the compressed 27-cycle draws instead of matching them.
      Built the `ProjAnim`s by hand instead of via `spawnProjectile` so the first can launch at
      cycle 16 (held-draw frames end, snap/release begins) with `startTime`/`endTime` shifted
      together to keep the real flight duration unchanged. Confirmed live: arrows now fire
      alongside their own draw.
- [x] Dark bow - was spawning two overlapping projectiles per arrow: the correct special one
      (dragon-head/smoke, ammo-branched) plus a second, separate one using the ammo's own plain
      colour, both flying the same path at once - read as "wrong arrow in the air" regardless of
      ammo. Dropped the redundant plain-colour spawn entirely (confirmed via
      `ProjAnim.calculateEndTime` that dropping it doesn't touch timing, since that only depends on
      the projanim type, not the spotanim). Also fixed the two hits landing a tick apart at real
      range (`doublearrow_one/two`'s different `stepMultiplier` for the visual arc was leaking into
      damage timing) - both hits now resolve on the first arrow's tick, matching Magic shortbow's
      same fix. The "smoke instead of dragon head" report turned out to be non-dragon ammo, not a
      bug - `descentOfDragons()` only triggers for `category.dragon_arrow`, confirmed correctly
      tagged on the item. Confirmed live.
- [x] Seercull - was using the ammo's plain launch/travel colour with no target-hit effect at all
      (same "plain ammo colour" bug as every other special here). External wiki/RuneLite search for
      Soulshot's real graphic id turned up nothing, so found it live instead: added a raw-numeric-id
      mode to `::spot <id> [height]` (bypasses RSCM name lookup entirely) and tried a candidate
      cluster around Dagannoth Supreme's own arrow-shower graphics, since Seercull drops from it and
      the real special's burst-of-spikes look matches. Confirmed live: 474 (`dagannoth_arrow_
      spotanim_hit`, height 0) is the target-hit burst, 473 (`dagannoth_arrow_spotanim_travel`) is
      the in-flight arrow - both genuine, official Dagannoth Supreme graphics, reused here. 472
      (`sp_attack_glow_arrow_launch_white`) is the launch glow - turned out to be this project's own
      custom addition, not from the real game, which is why it never showed up in any external
      search. Animation itself confirmed fine as just the plain normal-attack draw, same as Magic
      longbow's Powershot.
- [x] Dragon hasta - the generic `seq.specialattack_unleash` placeholder didn't sync correctly
      live; swapped Unleash to Sunspear's own thrust animation (`seq.human_weapons_sunspear_spec`)
      instead - looks the same, confirmed working live.
- [x] Dogsword - `statHeal` crash fixed (`coerceIn(current, base)` could have `current > base` -
      already-boosted stat - and threw; now `coerceIn(current, maxOf(current, base))`, a shared fix
      in `PlayerStatExtensions.kt` that covers every heal-based special, not just Dogsword)
- [x] Dogsword - freeze now works on NPCs too, not just players (`BindEffectService`, matches the
      real Zamorak godsword's own `ImpactMeleeSpecialAttacks.kt`)
- [x] Dogsword + Zamorak godsword - both now play `spotanim.ice_barrage_impact` on freeze (wiki:
      "similar animation to Ice Barrage") - **needs your confirmation it actually shows now**

## Flagged, out of scope for this pass

- [ ] Blowpipe (normal attack, not special) - animation renders from the player instead of the pipe
- [x] Morrigan's javelin - forced to melee distance, fixed: `obj.morrigans_javelin` was missing
      `param.attack_range` entirely in `items.toml` (fell back to melee range=1). Both cache variants
      (`br_morrigans_javelin`, `morrigans_javelin_bh`) already had it set to 5 - just the base item
      never got it. Added `attack_range=5` to match. Checked Rune/Dragon thrownaxe too - both already
      had `attack_range` set correctly, so this wasn't a broader thrown-weapon pattern, just this one
      item. **Needs your confirmation it actually ranges properly now.**

## Before the PR

- [ ] Revert the raw-numeric-id addition to `::spot` in `AdminCommands.kt` (`content/other/
      commands`) - debug-only, added to test unnamed spotanim ids live for Seercull's fix. Not
      meant to ship; strip it back to name-only before this branch goes into the PR.
