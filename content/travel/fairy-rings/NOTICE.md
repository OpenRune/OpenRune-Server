Fairy ring baseline adapted from Blade9766/OpenRune-Server, upstream PR #216
commit 375581c32314973d7de6a04e5e7f3713ef429f22. OpenRune MIT licence retained.
The fork adds native option validation, diary access, late teleport restrictions,
collision checks, animation cleanup and real native interaction tests.

Hideout route: OSRS Wiki Fairy Resistance Hideout and Fairytale II - Cure a
Queen, retrieved 2026-10-11; native rev240 ring placement verified in cache.
The house code reports the missing built house ring until the fork implements
Construction. Native tests exercise ring ops, IF3 rotation/confirmation/log
inputs, successful and interrupted travel, favourite persistence, island return
and the repeated hideout sequence.
