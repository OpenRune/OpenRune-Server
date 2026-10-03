# Combat, shields and respawn policy

Origin: UPSTREAM + CUSTOM EXTENSIONS. `c46abc6af` adds explicit weapon/magic/shield
special dispatch; `9f38dbb1d` adds special reset. The accepted implementation is preserved.

The [special-attack audit](../special-attacks-audit-20261001/README.md) records
137 registered versus 148 still missing among 285 special-energy items, with six shield
forms separately covered. Do not label all specials complete just because the bar exists.
Shield charging, inspection and cooldowns are per-item/persistent as documented there.

Respawn policy: GWD 100 ticks = 60 seconds, other supported bosses 34 ticks = 20.4 seconds.
Encounter progression and non-respawning entities remain respected. Actual deadlines,
not guessed kill times, feed Nero. Changes to NPC death, instance cleanup or Boss DSL
need timer and duplicate-death tests as well as combat tests.

Upstream risks: target reach, autocast, shield dispatch, hit scheduling, NPC death hooks,
Boss DSL and stats/bonuses. Preserve current behaviour while evaluating any replacement.

## Active weapon completeness work (2026-10-03)

Branch: feature/weapon-completeness, based on accepted runtime plus organization
commit 82d6c0203. User requests complete specials and normal weapon/charge behaviour.
Zulrah encounter files, rotations, reach policy and respawn policy remain unchanged.

Latest registry: 157/285 registered special-energy items, 128 missing. Registration
is not visual or mechanical parity. Selected suites pass 119 tests: 42 weapons,
55 specials, 2 engine, 5 NPC, 2 Zulrah and 13 pets. The full server JAR builds.
The earlier installable checkpoint passed 103 tests. Its isolated revision-240 server boot with the existing Nero server plugin passes
bridge health, catalogue search and continuously refreshed respawn snapshots, then
shuts down cleanly. This does not exercise a live game client or visually certify FX.
The following slices are a chronological implementation record; earlier counts below
refer to their respective checkpoints, not the current total.

Shadow/Venator charge follow-up: charging and full refunds now use a single native
inventory transaction, revalidate the exact item after dialogue and reject negative or
over-capacity amounts. Resource shortages or insufficient refund space roll back both
the weapon and every material. Existing recipes, native charge layouts and capacities
are retained. Three regression cases exercise both weapon families, including full
capacity, swapped items and a refund where only one of two material stacks fits.
This does not certify their remaining normal-attack mechanics or live effects.

Installable checkpoint: `weapons-update-20261003` is frozen at `880c6a9fa`, expects
the guides/cape baseline and includes rollback. Its 100 target files and prerequisites
pass read-only validation; scratch installer tests cover install, repeat install,
rollback, corrupt payloads, modified files, process guards and mid-copy recovery.

Subsequent melee impact slice (not in that installer): Warhammer, Elder Maul,
Bandos/Saradomin/Zamorak godswords, Whip and Anchor effects now attach to the native
hit instead of independent world timers. Drains use applied damage, zero damage
does not drain, and Healing Blade retains its pre-overkill heal basis. Callbacks are
once-only and reject replacement logins. Three new tests pass, including execution
through the registered Warhammer handler; all 42 special tests pass. No extra weapon
registrations are claimed and client FX placement remains unqualified.

Further melee families (not in the frozen installer): 17 additional item variants.
Dragon claws has four conditional accuracy branches, bounded split damage, the
all-miss chip outcome and paired hit delays. Post-special reductions apply separately
to each split hit, including independent Elysian rolls and integer rounding.
Dragon scimitar locks protection
prayers for eight ticks on positive impact without disabling Protect Item.
Darklight, Arclight (including inactive) and Emberlight drain Attack, Strength and
Defence additively from base levels after an accurate hit impacts; demon flags select
10%/15% rather than 5%, with the additional one level. Their special does not consume
Arclight charges; its ordinary charge/infusion lifecycle remains unfinished.
Dragon sword and Ancient mace use native melee hit processing with PvP prayer
penetration, retaining retaliation, hit modifiers and damage attribution. Ancient
mace drains actual PvP damage and restores prayer up to base plus that hit; NPC
restoration retains the rolled amount even when weapon immunity blocks damage.
Tests cover cancellation/relogin, zero hits, source/target FX ownership, exact
animation symbols, prayer expiry, damage bounds and a real prayer-piercing hit queue.
The subsequent full JAR also passed the isolated revision-240 boot, Nero catalogue,
live respawn snapshot refresh and graceful database/server shutdown checks. This
validation did not replace files in the accepted installation.

Remaining qualification: client animation height/frame alignment, variant cosmetics,
NPC prayer/immunity interactions, boss stat-drain
floors and demon metadata coverage. The cache-only fixture has no positive demon
params, so drain tests explicitly construct both demon and non-demon definitions;
this is not evidence that every live NPC is correctly classified. No local developer
client was listening on port 7780 during this validation. No "perfect FX" claim.
Formula references: [claw distributions](https://github.com/weirdgloop/osrs-dps-calc/blob/main/src/lib/dists/claws.ts),
[base-level demonbane drains](https://github.com/weirdgloop/osrs-dps-calc/blob/main/src/lib/scaling/DefenceReduction.ts),
[Ancient mace](https://oldschool.runescape.wiki/w/Ancient_mace).

Trident slice: eight normal/enhanced/ornament sea/swamp pairs plus full sea identities.
Item-local charge state uses the existing powered-staff varobj bit layout; inventory
transactions atomically pay/refund resources and preserve ornament and remaining charges.
Full tradable tridents derive 2500 charges from their identity. Coins are not refunded.
A cast spends one charge even on a splash; empty/PvP attempts do not create free hits.
Caster launch and target impact use distinct cache FX and projectile timing.

This is not yet full toxic-trident qualification: NPC venom and all encounter/PvP-area
exceptions remain to verify. Selected handler tests do not certify live client visuals.
Registration baseline remains 137/285; ordinary trident attacks add no special entries.

Mechanics reference: https://oldschool.runescape.wiki/w/Trident_of_seas_full

Scythe charge slice: normal, Holy and Sanguine forms now have persistent per-item
charges, atomic 200-blood-rune/one-vial payments for 100 charges, check/uncharge menus,
and one charge consumed for a damaging swing rather than per hit. Empty forms retain
the existing size-based normal attack with their weaker stats. No uncharge refund is
implied outside a well; confirmation explicitly warns that resources are lost.
Vyre-well storage/refunds, secondary area targets, variant FX qualification and
impact-modified damage accounting remain open. Corrupted and quest forms are not
silently aliased. Fifteen module tests pass (nine trident, five scythe, one cache export).
These slices do not change the 137/285 special registration count.

Blowpipe slice: normal and Blazing blowpipes store nine dart types and scales in the
existing three native varobjs. Load/unload/uncharge transactions are atomic. Ranged
bonuses use the loaded dart, not unrelated quiver arrows. Normal attacks and Toxic
Siphon consume stored ammunition with Ava conservation and two-thirds scale usage.
PvM/PvP rapid delays are two/three ticks. Siphon uses doubled accuracy, 1.5x maximum
and schedules half the queued damage as healing, guarded against replacement logins.
Venom, exact live projectile trajectory and impact-time cancellation remain unqualified.

Current registry: 139/285 (146 missing). Tests: 21 special-weapons + 35 special-attacks
passed. Full registry snapshot: [weapon registry](weapons-registry-20261003.tsv).
Reference: https://oldschool.runescape.wiki/w/Blowpibe

Eye of Ayak slice: server cache category corrected to PoweredStaff with range 6 and
3-tick ordinary casts. Native per-item varobjs preserve up to 50,000 charges and
whether the recipe used two death runes/one chaos rune or one demon tear. Recipes
cannot be mixed before uncharging; all remaining materials can be refunded atomically.
Soul Rend costs 50%, has 2x accuracy, scales the base maximum by 13/10 before gear,
and uses a 5-tick attack delay. Magic defence bonus drain is per NPC spawn, floors at
zero without altering Magic level or shared NPC definitions, and applies to subsequent
player and NPC magic accuracy. Respawn clears it. Ordinary and special PvP attempts
are rejected without spending resources. Doom passive recharge is deferred with Doom.

Validation: isolated revision-240 cache build passed; 27 ordinary-weapon and 39 special
attack tests passed. Registry: 140/285 registered, 145 missing. Tests cover refunds,
full inventories, final-charge splashes, cast/impact ownership, deferred drain, negative
base bonuses and respawn cleanup. The first cache build exposed stale shared CS2;
a fresh workspace-local LOCALAPPDATA resolved it. Live effect height/trajectory and
special impact timing still need client capture verification; tests confirm the intended
source/target wiring, not visual parity. No accepted installation files were replaced.
Mechanics/formula reference: https://github.com/weirdgloop/osrs-dps-calc/blob/main/src/lib/PlayerVsNPCCalc.ts

Impact completion follow-up: hits now share an opt-in, once-only effect list through
copies made by native processors. Callbacks run after damage is applied and receive
actual capped damage; discarded/cancelled hits never invoke them. Soul Rend drains
actual damage and Siphon heals its calculated pre-overkill amount only when that hit
impacts, retaining the source-login guard. Their previous independent world timers
are removed. Verified with two engine tests, a native NPC processor integration test,
and the full special suite (39 tests). No callback is registered by existing Zulrah code.

Sanguinesti slice: both ordinary and Holy staffs have native item-local 20,000-charge
storage, 2-blood-rune recharge/refund transactions and their cache check/charge/uncharge
menus. A four-tick cast consumes one charge even on splash. The current 2026 formula
uses floor(Magic/3), 1/5 leech chance and +8 damage on a leech proc; healing uses half
actual impact damage and cannot affect a replacement login. Cast, impact and heal use
the corresponding ordinary/Holy cache effects. Empty and PvP attempts do not cast.
Six Sanguinesti tests pass; combined selected suites now total 75 (34 weapons, 39
specials, 2 engine). PvP minigame exceptions and live effect trajectories remain open.
Reference: https://github.com/weirdgloop/osrs-dps-calc/blob/main/src/lib/PlayerVsNPCCalc.ts

Venom follow-up: blowpipe (normal and Toxic Siphon) and swamp-trident impacts now
roll their one-in-four venom chance only after positive damage. Charged serpent helm
identities guarantee NPC venom. NPC venom ticks every 30 ticks, increasing 6 through
20 damage, respects poison immunity and falls back to poison for venom-only immunity.
Ordinary poison cannot downgrade active venom; death/respawn clears the native state.
Helmet charge consumption itself remains outside this slice. Trident final-charge
consumption now follows hit construction so the equipped attack state is retained.
Five venom tests pass; selected suites total 80 (39 weapons, 39 specials, 2 engine).
The isolated cache build passed. Live client FX verification is still pending.
The accepted Zulrah source remains identical to the organization baseline.
