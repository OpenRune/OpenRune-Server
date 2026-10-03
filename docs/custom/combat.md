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
