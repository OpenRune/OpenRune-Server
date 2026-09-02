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
- [x] Dragon hasta (height only - still missing an animation, see below)
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

## Height — reverted back to 96, confirmed correct

- [x] Dragon/Trailblazer/3rd age/Infernal/Crystal axe boost (`StatBoostSpecialAttacks.kt`)
- [x] Abyssal whip
- [x] Dragon dagger
- [x] Dragon longsword
- [x] Dragon mace
- [x] Dragon scimitar
- [x] Dragon sword
- [x] Saradomin sword (height only - anim still missing, see below)
- [x] Saradomin blessed sword (height only - anim still missing, see below)
- [x] Dragon thrownaxe
- [x] Magic shortbow (height only - anim/projectile still wrong, see below)
- [x] Magic bow (height only - anim still wrong, see below)
- [x] Dark bow (height only - projectile still wrong, see below)
- [x] Seercull (height only - projectile still wrong, see below)
- [x] Rune thrownaxe

## Height — still untested

- [ ] Brine sabre (needs underwater to test)
- [ ] Dogsword (height itself - the `statHeal` crash is now fixed, so this should be testable now)
- [ ] Noxious halberd (needs you poisoned to trigger)
- [ ] Vampyre flail

## Animation / effect content bugs (separate from height, not yet fixed)

- [ ] Saradomin sword - animation missing entirely
- [ ] Saradomin blessed sword - animation missing entirely
- [ ] Dragon crossbow - special attack animation missing entirely
- [ ] Magic bow (longbow) - animation messed up
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
- [ ] Dark bow - fires wrong projectile
- [ ] Seercull - fires wrong projectile
- [ ] Dragon hasta - missing an animation (something like Sunspear's thrust)
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
