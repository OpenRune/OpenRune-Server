# Pets

Origin: UPSTREAM + CUSTOM EXTENSIONS. Implementation: `content/other/pets`.
The accepted version extends the native registry, reward/insurance and follower APIs.
`a53731b25` carries the Lil' Zik metamorphosis fix; `83c47b4e3`/`c46abc6af`
restore follower ownership synchronization on relog; `921b43b95` adds the item-model gallery.

Regression coverage: `PetMetamorphosisTest`, `PetFollowerSyncTest`, `PetMenuTest`.
Future checks: summon, right-click after relog, call/teleport, pickup with full inventory,
metamorphosis, loss/insurance and no duplicate follower/reward.

Upstream risks: player map clock/login ordering, follower varp, NPC lifecycle,
teleports, inventory transactions and persistence. Old pet branches contain earlier
implementations and tests; archive references preserve them without replacing this version.
