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

Latest registry: 162/285 registered special-energy items, 123 missing. Registration
is not visual or mechanical parity. Selected suites pass 122 tests: 42 weapons,
58 specials, 2 engine, 5 NPC, 2 Zulrah and 13 pets. The full server JAR builds.
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
floors and complete demon metadata coverage. The initial cache had no demon flags.
A parameter-only cache overlay now classifies 96 exact ID/name matches against
pinned Wiki DPS data and sets Duke resistance on two variants. A full comparison of
16,577 NPC definitions confirms zero unrelated field/parameter changes; an ordinary
NPC override was rejected because it reset existing fields. Tests exercise both
real packed Waterfiends and synthetic drain fixtures. This is not full coverage of
all transformed/NMZ demons or special vulnerabilities such as Yama. No local developer
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

Parameter-only NPC overlays use `[[npc_params]]`, `id` and `[npc_params.params]`.
They merge after existing server NPC overrides without changing client definitions,
combat stats, movement or other parameters. Unknown NPC IDs fail the pack.
Reference catalogue: [pinned Wiki DPS NPC data](https://github.com/weirdgloop/osrs-dps-calc/blob/89c3e25b344aea90d0189746e4b5f73dde0f0383/cdn/json/monsters.json).
Duke resistance: [Wiki strategy](https://oldschool.runescape.wiki/w/Duke_Sucellus/Strategies).

Silverlight/Darklight follow-up: both normal swords now receive their 60% demonbane
accuracy and damage bonuses (42% with Duke resistance). Dyed Silverlight retains
only its damage bonus; non-demons are unchanged. The actual equipped-item collector
and formula operations are regression-tested alongside Arclight/Emberlight controls.

Dragon hasta: all five poisoned/unpoisoned variants now implement Unleash. Damage
and accuracy scale by full 5%-energy steps, selected melee accuracy rolls against
stab defence, and PvP Protect from Melee is pierced via the native hit path. The
handler pays extra cost and leaves the minimum to the normal dispatcher; both
use native energy modifiers. Tests cover 5%, 25% and 100%, misses/hits, NPC/PvP,
all variants and reduced-cost mode. The caster uses the shared thrust animation
with the distinct post-2024 hasta spot effect at height zero. Live frame alignment
remains unverified. Reference: [Dragon hasta](https://oldschool.runescape.wiki/w/Dragon_hasta).
The special suite now passes 61 tests; registered coverage is 162/285.

Saradomin sword slice: ordinary Saradomin sword has its shared-accuracy melee hit
and separate 1-16 magic hit, with separate melee/Magic experience. PvP Protect from
Magic blocks the secondary hit entirely. Both blessed identities use melee offence
against magic defence, 25% increased maximum damage and 65% energy; their damage
still follows melee prayers and immunities. Caster sword effects and target lightning
use separate cache symbols. Tests cover NPC/PvP, hit/miss, protection prayers,
energy, experience routing and actual hybrid accuracy calculation. Blessed sword
ordinary degradation remains a separate unfinished weapon task. Visual frame/height
parity has not been verified in a live client.
References: [Saradomin sword](https://oldschool.runescape.wiki/w/Saradomin_sword),
[blessed sword](https://oldschool.runescape.wiki/w/Saradomin%27s_blessed_sword),
[magical melee](https://oldschool.runescape.wiki/w/Magical_melee).
Validation: 65 special tests, 129 selected tests overall, full server JAR build.
Registry: 165/285 registered, 120 missing; registration does not certify full mechanics.

Ancient warrior slice: four Vesta longsword identities use selected melee offence
against one-quarter of stab defence, with 20%-120% damage bounds. Three Statius
warhammers use 25%-125% damage bounds and drain current Defence only on positive
impact: 30% for ordinary/Last Man Standing, 75% for Bounty Hunter. Reductions apply
after rolling damage. Cancelled, blocked or stale-login hits do not drain. All 68
special tests pass, including exact accuracy boundaries and cumulative drains.
Registry: 172/285, 113 missing. Bounty Hunter/Deadman usage restrictions, degradation,
boss-specific drain floors and live effect alignment remain separate open work.
References: [Vesta's longsword](https://oldschool.runescape.wiki/w/Vesta%27s_longsword),
[Statius's warhammer](https://oldschool.runescape.wiki/w/Statius%27s_warhammer),
revision-240 per-item special descriptions. The packed Statius effect uses the
same sequence/model as the granite hammer effect with its own recolours; retained
the Statius-specific symbol rather than substituting the granite recolour.

Abyssal tentacle slice: both variants implement Binding Tentacle. Impact freezes
for eight ticks even on a miss and independently rolls 50% poison starting at four.
Native freeze/poison immunity, existing freeze duration, cancelled hits, dead targets
and stale source logins are covered. Charge degradation remains unfinished.
The whip previously used graphic sequence 1669 as its player animation; the packed
spot 341 confirms 1669 belongs to that graphic. Whip/tentacle now animate the player
with the native whip attack and send the graphic to the target. A cache-backed test
rejects graphics accidentally used as player animations throughout the melee table.
All 73 special tests pass; 174/285 registrations, 111 missing. Live timing/height
verification is still pending.
Reference: [Abyssal tentacle](https://oldschool.runescape.wiki/w/Abyssal_tentacle).

Staff protection slice: eight Dead/Light/Balance/toxic/Deadman identities now activate
their variant-specific player and cast effects for 100% special energy. A temporary
native varp expires at 100 ticks. Incoming melee damage halves at impact after
normal prayer reduction; magic/ranged/typeless damage is unchanged. Switching away
alone does not cancel protection, but receiving positive damage without a supported
staff does. Zero damage does not cancel; activation refreshes rather than stacks;
login/logout clears state. Native impact tests cover health, damage callbacks and
prayer-rounding order. Cache rebuild, 79 special / 143 selected tests, full JAR,
isolated startup/catalogue/clean shutdown pass. Registry: 182/285, 103 missing.
Legacy duel restrictions, passive rune-saving/charges and live visual qualification
remain open. There is no claim that cast effects alone reproduce every lingering
visual stage. Reference: [Staff of the dead](https://oldschool.runescape.wiki/w/Staff_of_the_dead).


Dark bow / Duality slice: nine Dark bow identities now dispatch, including LMS and
both Deadman identities. Ordinary Descent retains 5/8 minimum damage on accuracy
misses; its 0-to-maximum roll clamps low rolls, while BH uses a uniform 7/10-to-maximum
roll. Dragon damage caps at 48 before target reductions; native Corp and Elysian
reductions run after the roll. Each arrow uses its own projectile delay and processed
hit for XP. Missing double-launch metadata rejects before resource use, including
Seeking dragon arrows in the current server cache: this ammunition still needs its
native projectile metadata completed. No extra Seeking minimum is added.
Dragon knife Duality now uses the native two-knife player animation; poisoned knives
have their own player/projectile effects. Its explicit special projectile no longer
fails validation merely because normal projectile metadata is absent. Tests execute
all five knife variants and reject single-knife attacks before animation/consumption.
Registry: 185/285, 100 missing. Live frame/height qualification, event-world usage
restrictions and unimplemented target-specific reductions remain open.
References: [Dark bow](https://oldschool.runescape.wiki/w/Dark_bow),
[Dark bow (bh)](https://oldschool.runescape.wiki/w/Dark_bow_(bh)),
[Dragon knife](https://oldschool.runescape.wiki/w/Dragon_knife), revision-240 symbols
and packed item/projectile definitions. This is not a claim of 285/285 completion.

Validation: 87 special / 151 selected tests pass; server JAR and isolated
Nero-bridge startup, catalogue and clean shutdown pass. Zulrah source is unchanged.

BH Dragon mace: Shatter uses 15% native energy, 125% selected melee offence against
60% of crush defence and 150% melee maximum. Target levels remain unchanged.
Damage reductions follow the rolled damage; misses do not roll or apply modifiers.
Two new tests cover NPC/PvP accuracy boundaries, handler routing, FX, damage stages
and energy. All 89 special tests pass; registry 186/285, 99 missing. Live visuals
and BH world restrictions remain open. Primary reference:
[Dragon mace (bh)](https://oldschool.runescape.wiki/w/Dragon_mace_(bh)).

Dorgeshuun slice: four Bone daggers and the Dorgeshuun crossbow use the last positive
damage contributor to choose guaranteed versus normal accuracy. This is independent
of top damage and insertion order; misses and imported totals do not replace the
last attacker. Contributions clear resets the marker. Both specials respect
QuestRequirements for quest_deathtothedorgeshuun and cost 75% native energy.
Defence drains by actual damage only when Defence is not already lowered; cancelled
hits and stale source/target identities have no effect. Bone dagger uses its native
player stab plus a distinct graphic; Snipe uses the bone special projectile and
one native ammo-consumption attempt. Native target/prayer modifiers remain in the
hit queue. Boss-specific drain floors and Kephri's special team-case are not yet
qualified. Eight new tests pass; 97 special / 161 selected tests and full JAR pass.
Registry: 191/285, 94 missing. References:
[Bone dagger](https://oldschool.runescape.wiki/w/Bone_dagger),
[Dorgeshuun crossbow](https://oldschool.runescape.wiki/w/Dorgeshuun_crossbow).

Dorgeshuun candidate isolated startup/catalogue and clean shutdown passed; proof
in dorgeshuun-validation-20261003.json. Live installation remains untouched.

## Acceptance FX correction (2026-10-04)

Bludgeon Penance now places its miasma graphic on the NPC/player target, height 0,
with the existing 30-client-cycle impact delay. The wielder retains the attack
animation and no longer receives the target graphic.

Both Nightmare staff specials now animate the player with
`seq.nightmare_staff_special` (8532). Revision-240 cache decoding shows that
Volatile cast spot 1760 uses sequence 8546 and Eldritch cast spot 1762 uses
sequence 8548. The previous implementation incorrectly applied these effect-model
sequences to the player's body as well, producing disappearance/scale glitches.
The cast graphics retain their own sequences; target graphics, damage, energy,
attack speed and hit timing are unchanged. No cache/client patch is required.

Regression tests execute Bludgeon against NPC and player targets and distinguish
both Nightmare player animations from the decoded graphic sequences. Live visual
acceptance remains required. No new weapon family is added; special development
remains paused at 191/285 registrations pending user acceptance of PR #15.

Validation: 99 special-attack tests pass (0 failures/errors/skips); full server
JAR builds successfully. Visual acceptance is not claimed by these tests.
