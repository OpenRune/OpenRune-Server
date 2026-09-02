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
- [ ] Magic shortbow - animation messed up + fires wrong projectile. Tried delaying the target-hit
      spotanim to the projectile's arrival (it was firing instantly at cast time, before the
      projectiles even existed) - tested live, felt worse, reverted. `proj_type` sourcing checked
      out fine (matches every other ranged file). No dedicated "green arrow" asset found in the
      cache for this weapon. You described it as: a green launch flash, then two plain-colored
      arrows - that sequence may be correct/intentional (Magic shortbow fires your real ammo, not a
      magic arrow; the green is presumably just the bow's own muzzle-flash), so the actual bug (if
      any) is still unidentified. Needs a fresh look, ideally with more specific detail on which part
      looks wrong (timing? color? something firing that shouldn't?).
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
