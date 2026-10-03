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
