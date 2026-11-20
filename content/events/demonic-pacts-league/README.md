# Leagues VI: Demonic Pacts

An offline recreation of the Demonic Pacts League (15 April – 10 June 2026) as an OpenRune world
type. Turn it on with `world-types: ["demonic_pacts_league"]` in `game.yml`.

## What works

| System | Behaviour |
|---|---|
| Start | New league characters start in Civitas illa Fortis with level 3 Herblore and level 5 Runecraft. |
| Ironman | League characters are Ironmen (stored in the league save only; your main character is untouched). |
| XP | 5× at relic tier 1, 8× at tier 2, 12× at tier 4, 16× at tier 6. Combat stats get another 1.5× from tier 3. |
| Drops | Boosted drops 2×, rising to 5× at tier 4 (through the server's `boosted` drop flag). |
| Run energy | Never drains. |
| Relic tiers | Reached at 0, 600, 1,200, 2,600, 5,200, 8,500, 16,500 and 28,000 league points. Pick relics in Journal > Leagues > Relics. See "Relics that work" below. |
| Regions | Varlamore from the start, Karamja at 80 tasks, then one chosen region at each of 200, 300 and 450 tasks, picked in Journal > Leagues > Areas (which also teleports to unlocked areas). Misthalin is closed. Zanaris and the Abyss are always open. Walking or teleporting into a locked region bounces you back. |
| Tasks | The official 1,592-task list from the cache, shown in Journal > Leagues > Tasks. About 900 are detected automatically (levels, XP, kills and kill counts, equipping and owning items, gathering, cooking, smelting, smithing, crafting, runecrafting, herblore, thieving, firemaking, burying, implemented quests). The rest need `::taskdone <number>`. 10/30/80/200/400 points by difficulty. Tasks in locked areas can't be completed. |
| Pact points | 2 to start, plus 1 every 10 tasks (configurable). 44 can be earned, 40 can be held at once. 6 full resets. |
| Pact tree | All 132 nodes with their real connections. A node can be bought when it touches one you hold; the tree starts from `AA`. |

### Pact effects that work (52 of 132 nodes)

- **Accuracy**: +25% (small nodes) and +50% (large nodes) against npcs, all styles. Spells use a reroll-on-miss approximation because the spell formula takes no multiplier.
- **Damage**: +1% per small melee, ranged or magic node, applied to max hit.
- **J4**: max hit +4%, plus 4% for every 3 tiles of distance.
- **B3**: melee min hit +3, plus 3 per extra tile.
- **D1 Thorns** (shield): attackers take 3 damage. **M1** makes Thorns hit a second time for half.
- **G1** (shield): 0.1% per Defence level to reflect a whole hit back.
- **M2**: melee hits restore 2% special attack energy. **G4**: attacks from 2+ tiles restore 2%.
- **I6**: 1 Prayer every 15 ticks while no protection prayer is active.
- **Prayer penetration** nodes: apply against players' protection prayers (PvP).

The other 80 nodes (Regenerate, Ranged echoes, Blindbag, overheal, attack speed changes, elemental
spell effects, permanent Defence and so on) can be bought and saved, and are marked
`[unimplemented]` in game. Most need hooks the combat engine does not expose yet.

### Relics

The relic window shows the game's own description of each relic. On this server:

| Relic | On this server |
|---|---|
| Endless Harvest | Double logs, fish and ore and their XP; trees and rocks don't deplete; resources go to the bank (`::relic bank off`). |
| Barbarian Gathering | Chop and mine bare-handed (as crystal tools); failed gathering attempts succeed half the time; 10% bonus Strength and Agility XP. Fishing still needs tools, and the knapsack does nothing. |
| Abundance | +10 to non-combat skills, +2 XP per drop in them, 2 coins per XP gained in them. |
| Hotfoot | Agility and Cooking always succeed. With the searing boots on: fish are cooked, ore is smelted, running gives Agility XP. No Agility courses exist here. |
| Friendly Forager | With the pouch carried, gathering sometimes finds a grimy herb (Herblore level + 25). Potions are made with 4 doses; secondaries are saved 90% of the time. |
| Woodsman | Cut logs are burned for Firemaking XP (`::relic burn off`). No Fletching or Hunter here. |
| Bank Heist | The briefcase teleports to the official list of banks, deposit boxes and bank chests in unlocked areas. |
| Evil Eye | The necklace teleports to the official list of boss and raid entrances in unlocked areas. |
| Map of Alacrity | The map teleports to the official list of agility shortcuts in unlocked areas. |
| Transmute | The ledger is an unlimited nature rune source. Alchemic Divergence and Convergence aren't in. |
| Conniving Clues | Not in: this server has no clue scrolls. |
| Butler's Bell | The bell gives the demon butler a job (ores 8, fish 14, logs 10, flax 50 a minute; processed items at half rate with 10 XP each). It keeps working while you're logged out; ring again to collect, noted. |
| Nature's Accord | No Farming level requirements; 10x noted harvests with 10x XP; the fairy mushroom teleports to fairy rings and tool leprechauns in unlocked areas. Plants still die, and seeds aren't saved. |
| Larcenist | Thieving always succeeds; 10x loot, noted. No auto-repeat. |
| Soul Harvest | Bones and ashes that monsters drop become soul shards (by Prayer XP). Shards work as pure essence; `::releaseshards` gives 5.5 Prayer XP each (the Libation bowl isn't on this server). Farming doesn't give shards. |
| Grimoire | The grimoire switches spellbooks. Prayer and spell unlocks and Book of the Dead aren't in. |
| Eternal Sustenance | Food isn't used up. |
| Culling Spree | Slayer masters offer three tasks to choose from, and you pick 5-200 kills. The other perks aren't in. |
| Flow State | Woodcutting, Mining, Fishing, Cooking, smelting, anvil smithing and potion making repeat every 2 ticks. |
| Reloaded | One extra relic from any lower tier, picked in the relics window. |
| Executioner | Hits with the Sage's axe that leave a monster under 20% HP finish it (not Yama). |
| Minion | The whistle summons a minion for 30 minutes. It hits your target for 3-10 every 3 ticks and loots your kills into your inventory, noted (not bones or ashes if you have Soul Harvest). There's no visible minion. |
| Flask of Fervour | Restores HP, Prayer and special attack; three explosions of 60% of base Prayer to monsters within 3 tiles; no damage taken for 2.4 seconds; 3-minute recharge, 1 tick less per 10 damage in one hit. |

Relic items find their op (Teleport, Rub, Ring, and so on) by name at startup. If one does nothing, the
server log names the item and lists its ops.

## Not included

- The real pact tree and leagues interfaces. Everything runs through chat commands for now.
- The relics listed as not in yet, echo bosses and echo orbs, the reward shop,
  and boosted minigame points.
- Jagex's exact region borders; `demonic_pacts_regions.toml` uses hand-drawn boxes.

## Commands

| Command | Does |
|---|---|
| `::league` | Points, tier, rates, pacts, regions. |
| `::pacts` / `::pacts owned` / `::pacts all` | Pacts you can buy now, hold, or all of them. |
| `::pact <id>` | One pact's effect and connections. |
| `::pactbuy <id>` | Spend a pact point. |
| `::pactreset confirm` | Clear every pact (uses a reset). |
| `::tasks [todo\|done\|all\|manual\|<region>\|<words>] [page]` | Task list in chat, with each task's number. |
| `::taskdone <number>` | Admin: complete a task the server can't detect. |
| `::regions` | Region status and free choices. |
| `::unlockregion <key>` | Use a region choice. |
| `::dpregion` | Which region the tile you stand on belongs to. |
| `::relics` | Your relics and what they do here. |
| `::relic bank on\|off`, `::relic burn on\|off` | Endless Harvest banking, Woodsman log burning. |
| `::relicitems` | Get back lost relic items. |
| `::dppoints <n>`, `::dppactpoints <n>`, `::dpallregions` | Admin testing shortcuts. |

## Files

| File | Holds |
|---|---|
| `src/main/resources/demonic_pacts.toml` | Settings: start tile, thresholds, points, pact limits, region lock switch. |
| `src/main/resources/demonic_pacts_tree.toml` | The pact tree. |
| `src/main/resources/demonic_pacts_regions.toml` | Region boxes. |
| `src/main/resources/demonic_pacts_relic_teleports.toml` | Relic teleport destinations, from the [Shortest Path](https://github.com/Skretzo/shortest-path) plugin's `seasonal_transports.tsv` (BSD 2-Clause, as in the map-transports module). |
| `src/main/resources/demonic_pacts_butler.toml` | Butler's Bell jobs and rates. |
| `src/main/resources/demonic_pacts_league_tasks.toml` | The official task list with how each is tracked, generated from the cache (enum_5950, dbtable action). |
| `pack/src/main/resources/pack/configs/demonic_pacts.toml` | Save varps. |

Every node, region and task has a `bit`, its save slot. Add new entries with new bits; never
renumber existing ones, or saved progress will point at the wrong entries.

## Engine hooks this module added

- `WorldType.DEMONIC_PACTS_LEAGUE`.
- `StatXpModifiers` (api/player): per-stat xp multiplier, flat bonus and xp-gained callback for
  naturally earned xp.
- `SkillModifiers` (api/player): stat-roll success chance (also used by cooking burns), thieving
  loot multiplier, and food that isn't used up.
- `InventoryRuneSources` (api/spells-runes): carried objs that count as unlimited runes.
- `SkillModifiers` also has: ignored level requirements and harvest multipliers (farming), action
  tick overrides (gathering, cooking, smithing, herblore), bare-handed gathering (woodcutting,
  mining), extra secondary-save chance (herblore) and Slayer task choice.
- `SkillActivity` (api/player): a feed of finished skilling actions for the task list.
- Runecrafting accepts `league_soul_shard` as pure essence.
- Woodcutting now honours `SkillingProduct.depletes`, like mining.
- `PlayerCombatModifiers` (api/combat-manager): accuracy and max-hit multipliers for players
  attacking npcs.
- `NpcHitEvents.ModifyAny` (api/npc): a hit-modify event for every npc type.

Data sources: the [OSRS Wiki Demonic Pacts League page](https://oldschool.runescape.wiki/w/Demonic_Pacts_League)
and its [pact tree data module](https://oldschool.runescape.wiki/w/Module:Demonic_Pacts/data).
