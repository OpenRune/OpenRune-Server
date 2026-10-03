# Native interfaces

Commands, monster drops, pets, skill guides and quest guides share the native steel
frame / orange labels / shadowed text design of Spawn and Collection Log.

- Commands: `content/other/commands` and its pack module.
- Monster guide: `content/interfaces/monster-info`, full-size NPC and reverse item
  search, inventory models, base drop probabilities, stats and registered locations.
- Pet gallery: `content/other/pets`, native item models and existing delivery service.
- Skills: `content/interfaces/skill-guides`, native `skill_guide_v2`, four initializer arguments.
- Quests: `content/quest`, steel frame over native requirements and scrollable real quest logs.

Commits: `921b43b95` for searchable guides/gallery and `444ead71b` for skill/quest
styling plus corrected cape submenu indices. [Guide regression notes](../guides-cape-regression.md).

Upstream risk: interface/CS2 signatures, gameval mapping, native model widgets and
packet op/subop semantics. Build the cache with matching server code. Preserve the
zero-based Max cape submenu index. Check fixed/resized layouts, long names, scrolling,
close buttons, search cancellation and rights before claiming visual completion.
