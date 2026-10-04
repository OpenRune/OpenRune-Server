# Commands

Origin: UPSTREAM + CUSTOM EXTENSIONS. Main module: `content/other/commands`.
Commands use the native `NativeCommandsInterface`; rights metadata is retained by
`api/cheat` and `engine/game`. The repaired UI preserves search/category/page state.

Custom administrator commands include ::allrunes (5000 each), ::maxmelee,
::maxrange, ::maxmage (inventory only), ::spres and ::teleport via the world map.
Inventory grants are atomic when space is insufficient. ::spres does not reset
independent shield charges/cooldowns. Zulrah debug helpers remain in AdminCommands.

Important commits: `5733d6939`, `5cf3e9f3e`, `83c47b4e3`, `c46abc6af`, `9f38dbb1d`.
Tests cover rights, menu paging/selection, dispatch and inventory preservation.

The old PersonalCommands class offered petspawn/allpets/petcall/petpickup helpers.
It is archived for review; it is not added as a second command-registration path.
Future changes must check command-name collisions, administrator checks and cancellation.

## Weapon acceptance sets

Administrator-only `::weptest` shows the available set range. `::weptest 1`,
`::weptest 2`, etc. grant up to 20 weapons/shields per set, in stable item-ID order.
The live special/weapon registries select the items; a special-energy item with
no special handler is excluded even if its normal attack is registered. Weapons
without special energy are included when they have a dedicated normal handler.
This covers the current combat implementation, not every generic cache weapon.
Only hand-slot items are granted, including registered ornament/poison variants.
Throwing weapons stack to 1,000; other weapons/shields are one each. Charged
dragonfire shield forms receive 50 charges; uncharged forms stay uncharged.
Other weapons retain their normal charging and ammunition requirements.
Inventory transactions are all-or-nothing and leave existing equipment untouched.
The command is automatically listed in `::commands` through native registration.

Special development is paused for user acceptance. Registration does not certify
complete mechanics or animations. Test normal attacks, specials after `::spres`,
impact positions, charge depletion, relog persistence and shield cooldowns.
PR #15 remains unmerged until the user gives explicit green light.

Validation (2026-10-04): 12 command/interface tests pass, full server JAR builds,
and isolated Nero-bridge startup succeeds. In-game acceptance remains pending.

## Barrows loot test

`::testloot barrows` generates 100 full-potential chest rewards on the ground.
`::testloot barrows 5` generates five. Accepted range: 1..1000; administrators only.
Each chest uses all six brothers and 1012 reward potential through the normal
Barrows reward calculation, including the player's diary rune bonus, equipment
drop multiplier and clue conversion. The test does not change killed-brother
flags, active crypt state, chest count, quest state or collection-log progress.
Normal Barrows chest play was accepted by the user on 2026-10-04.

## Max ranged necklace - 2026-10-04

`::maxrange` now supplies necklace of rupture instead of necklace of anguish.
Delivery remains inventory-only and atomic; worn items and existing inventory are preserved.
The command integration test verifies the actual native item identifier.
