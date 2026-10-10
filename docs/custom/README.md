# Fork technical documentation

Current status, active work and roadmap: [PROGRESS.md](../../PROGRESS.md).
This index contains implementation notes and evidence, not a second status list.

| Subject | Module / main source | Details |
|---|---|---|
| Runtime and recovery | Server packages, paired Nero Studio | [Checkpoints](baseline.md), [architecture](architecture.md) |
| Zulrah | `content/bosses/zulrah` | [Encounter](zulrah.md) |
| Araxxor | `content/bosses/araxxor` | [Encounter and fang recipes](araxxor.md) |
| Lizardman Shamans | `content/bosses/lizardman-shaman` and `pack` | [Combat, Shayzien protection, drops and test commands](lizardman-shamans.md) |
| Revenant Caves | `content/areas/revenant-caves` and `pack` | [Access, combat, ether, artefacts and Maledictus](revenant-caves.md) |
| Edgeville home teleporter | `content/other/home-teleporter` and `pack` | [Catalogue, native interface, travel safety and Emblem trader](home-teleporter.md) |
| Kraken | `content/bosses/kraken` | [Public-cave encounter](kraken.md) |
| Tormented Demons | `content/bosses/tormented-demon` and `pack` | [Encounter, Temple, rewards, crafting and acceptance evidence](tormented-demons.md) |
| Teleport animations and scrolls | `content/skills/magic/spell-teleports` | [Rev240 animation cleanup, all scroll routes and validation](teleport-scrolls.md) |
| Prayer scrolls, spellbook travel and gorillas | `content/interfaces/prayer-tab`, `content/skills/magic/spell-teleports`, `content/bosses/demonic-gorilla` | [Custom policy, native interactions, sources and tests](prayers-teleports-gorillas.md) |
| Corporeal Beast | `content/bosses/corporeal-beast` | [Encounter](corporeal-beast.md) |
| Doom of Mokhaiotl | `content/bosses/doom-of-mokhaiotl` and `pack` | [Encounter and rewards](doom-of-mokhaiotl.md) |
| Barrows reward testing | `content/bosses/barrows` | [Administrator commands](commands.md#barrows-loot-test) |
| Treasure Trails | `content/other/treasure-trails` | [Implementation](treasure-trails.md), [task coverage](clue-task-coverage.md), [test commands](clue-testing.md), [guardian fix](clue-guardians.md) |
| Pets and gallery | `content/other/pets` | [Followers, gallery, companion actions and morphs](pets.md) |
| Commands and loadouts | `content/other/commands`, `content/interfaces/worldmap` | [Commands](commands.md) |
| Monster, skill and quest interfaces | `content/interfaces/monster-info`, `content/interfaces/skill-guides`, `content/quest` | [Interfaces](interfaces.md) |
| Max cape | Custom native-menu handlers in `content/other/max-cape` | [Menus and perks](../max-cape.md) |
| Slayer menus and helmet crafting | `content/skills/slayer`, `content/skills/crafting`, native cache/shop data | [Source audit, menu references and conversion rules](slayer.md) |
| Completionist cape and hooded Slayer helmets | Reward requirements, equipment variants and client/cache visuals | [Design and visual reference](completion-rewards.md) |
| Combat, NPC animations, weapons and shields | `content/other/special-attacks`, `content/other/special-weapons`, `api/specials`, `api/combat` | [Combat and NPC animation audit](combat.md) |
| Boss-item assembly | `content/skills/crafting`, `content/other/special-weapons` | [Recipes and dismantling](boss-item-crafting.md) |
| Prices, Examine and notifications | `api/market`, `api/player-output`, `content/interfaces/collection-log` | [Four extensions](small-extensions.md) |
| Timers, NPC stat HUD and object library | `api/npc`, `api/death`, `api/instances`; paired Nero repository | [Architecture](architecture.md) |
| Shared APIs, plugin loading and cache mappings | `engine/plugin`, `or-cache`, `server/app` and affected APIs | [Core changes](core-modifications.md) |

## Upstream and recovery references

- [General upstream comparison](upstream-review.md) and [Doom/DSL review](upstream-review-20261006.md).
- [Test harness PR #282](upstream-pr-282-test-harness.md) and [level-up PR #286](upstream-level-up-779b81b.md).
- [Historical branch audit](branch-audit-20261003.md) and [research decisions](research.md).
- Repositories: [upstream](https://github.com/OpenRune/OpenRune-Server), [fork](https://github.com/EvolvedMind/OpenRune-Server), [Nero Studio](https://github.com/EvolvedMind/Nero-OpenRune-Studio).

Archived implementations remain review material. Do not load them alongside the active handlers.

Native clue action sources and test evidence: [clue completion](clue-completion.md)
and [watermelon/Farming scope](clue-farming.md). The additional eight native tasks
have [action, source and validation notes](clue-native-tasks.md).
Current status remains in PROGRESS.md.

The final ten skill-task routes have [native action and scope notes](clue-final-native-tasks.md).
