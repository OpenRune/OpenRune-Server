# Cooking implementation plan

## Goal
Implement Cooking to match wikidata (levels, XP, burn logic, station rules, prep steps, and outcome items) across cache + content.

## Current status (as of 2026-02-01)
- Heat-source cooking for core fish/meats is implemented with per-item chance profiles (added spider on stick/shaft, snail meats, fishcake, red/giant crab meat, hunter meats, antelope, jubbly, kebbits, and late-game fish). Sacred eel remains blocked by missing item key in cache.
- Bread and pitta bread baking implemented (range-only).
- Pies (redberry/meat/mud/apple/fish/botanical/mushroom/admiral/dragonfruit/wild/summer) baked on range with chance profiles.
- Stew and curry (fire/range) implemented.
- Pizzas (plain bake + topping steps) implemented.
- Chocolate cake topping step implemented.
- Wine (jug of wine, wine of zamorak) implemented with item-on-item success chances.
- Hot drinks (nettle tea), vegetable dishes, dairy, and ugthanki kebab prep implemented.
- Spit roasting and Camdozaal prep table are implemented (bird + beast + rabbit roast; guppy/cavefish/tetra/catfish prep).
- Multi-step prep + bake exists for garden pie and cake; other pies currently bake-only from uncooked items.
- Dough/base prep added for bread dough, pastry dough, pie shell, pizza base, incomplete pizza, and uncooked pizza.
- Burn success now uses chance profiles loaded from cache tables; stop-burn flags removed.
- Station/equipment modifiers implemented for Lumbridge range, Hosidius +5/+10, and cooking gauntlets.
- Cooking cape (no-burn) implemented; Hosidius +10 diary wiring still missing in runtime.
- Large recipe gaps remain per wikidata.

## Plan (phased)
### Phase 1 — Recipe coverage (core)
1) Add missing Meat/Fish recipes from wikidata.
   - Start with remaining meat/fish from meat-fish.md.
2) Add remaining prep steps (pitta, pie fillings, other bases) and topping flows (where missing).

### Phase 2 — Burn rules + chance data
1) Parse burn thresholds from burn_level.wikisrc.
2) Integrate skill_chances.json curves per item where available. (in progress; automated extract in tmp/)
3) Add station modifiers and equipment modifiers:
   - Lumbridge Castle range
   - Hosidius range (+5%, +10% with diary)
   - Cooking gauntlets
   - Cooking cape (no burn)

### Phase 3 — Special/side recipes
1) Hot drinks, vegetable dishes, dairy, kebabs.
2) Hunter foods: kebbits, snails, antelope, pyre fox.
3) Fishcake, giant crab meat, karambwan poison variants.

### Phase 4 — QA and balancing
1) Update cooking test presets to cover new recipes.
2) Add sanity checks for missing RSCM keys.
3) Verify outcomes/messages and inventory space handling.

## Primary files to change
- [cache/src/main/kotlin/org/alter/impl/skills/cooking/recipes/FishRecipes.kt](../../cache/src/main/kotlin/org/alter/impl/skills/cooking/recipes/FishRecipes.kt)
- [cache/src/main/kotlin/org/alter/impl/skills/cooking/recipes/MeatRecipes.kt](../../cache/src/main/kotlin/org/alter/impl/skills/cooking/recipes/MeatRecipes.kt)
- [cache/src/main/kotlin/org/alter/impl/skills/cooking/recipes/BakedGoodsRecipes.kt](../../cache/src/main/kotlin/org/alter/impl/skills/cooking/recipes/BakedGoodsRecipes.kt)
- [cache/src/main/kotlin/org/alter/impl/skills/cooking/CookingRecipeRegistry.kt](../../cache/src/main/kotlin/org/alter/impl/skills/cooking/CookingRecipeRegistry.kt)
- [content/src/main/kotlin/org/alter/skills/cooking/events/CookingUtils.kt](../../content/src/main/kotlin/org/alter/skills/cooking/events/CookingUtils.kt)
- [content/src/main/kotlin/org/alter/skills/cooking/events/CookingEvents.kt](../../content/src/main/kotlin/org/alter/skills/cooking/events/CookingEvents.kt)

## Reference data
- [wikidata/cooking.wikisrc](../../wikidata/cooking.wikisrc)
- [wikidata/burnt_food.wikisrc](../../wikidata/burnt_food.wikisrc)
- [wikidata/burn_level.wikisrc](../../wikidata/burn_level.wikisrc)
- [wikidata/skill_chances.json](../../wikidata/skill_chances.json)
- [wikidata/meat-fish.md](../../wikidata/meat-fish.md)
