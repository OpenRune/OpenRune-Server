# EvolvedMind custom content

Runtime baseline: **OpenRune revision 240**, server `444ead71b`, Nero Studio
`0efcb039`. [Exact baseline and validation](docs/custom/baseline.md).
[Current progress](CUSTOM_PROGRESS.md) is the status authority; file existence is
not evidence that every mechanic is complete.

| System | Origin | Implementation | Details |
|---|---|---|---|
| Zulrah encounter and shoreline reach | CUSTOM using upstream APIs | `content/bosses/zulrah` | [Zulrah](docs/custom/zulrah.md) |
| Pet followers, relog and morph fixes, pet gallery | UPSTREAM + CUSTOM EXTENSIONS | `content/other/pets` | [Pets](docs/custom/pets.md) |
| Commands, inventory loadouts, special reset, map teleport | UPSTREAM + CUSTOM EXTENSIONS | `content/other/commands`, `content/interfaces/worldmap` | [Commands](docs/custom/commands.md) |
| Monster search, drop previews and stats | CUSTOM using upstream data | `content/interfaces/monster-info`, `api/drop-table-plugin` | [Interfaces](docs/custom/interfaces.md) |
| Skill/quest guide styling | UPSTREAM + CUSTOM EXTENSIONS | `content/interfaces/skill-guides`, `content/quest` | [Interfaces](docs/custom/interfaces.md) |
| Max cape menus and perks | CUSTOM using native item/cache menus | `content/other/max-cape` | [Max cape](docs/max-cape.md) |
| Weapon/shield special attacks | UPSTREAM + CUSTOM EXTENSIONS | `content/other/special-attacks`, `api/specials`, `api/combat` | [Combat](docs/custom/combat.md) |
| Boss respawn policy and Nero countdowns | HYBRID server/client integration | `api/npc`, `api/death`, `api/instances`; Nero plugins | [Architecture](docs/custom/architecture.md) |
| External plugin loading and interface mappings | UPSTREAM + CUSTOM EXTENSIONS | `engine/plugin`, `or-cache`, `server/app` | [Core modifications](docs/custom/core-modifications.md) |
| Object library, native loot value display | CUSTOM Nero integration | Separate Nero Studio repository | [Architecture](docs/custom/architecture.md) |
| Araxxor foundation | CUSTOM, no active encounter yet | `content/bosses/araxxor`, capture inspection | [Araxxor](docs/custom/araxxor.md) |
| Doom research | CUSTOM research, no active encounter | Archived branch history | [Research](docs/custom/research.md) |

Upstream: https://github.com/OpenRune/OpenRune-Server
Fork: https://github.com/EvolvedMind/OpenRune-Server
Paired launcher: https://github.com/EvolvedMind/Nero-OpenRune-Studio

Do not reactivate archived implementations alongside these handlers. Compare first.

Weapon charge and normal-attack work is tracked under
content/other/special-weapons on feature/weapon-completeness; see the combat document.
