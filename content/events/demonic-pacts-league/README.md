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
| Regions | Varlamore from the start, Karamja at 80 tasks, then one chosen region at each of 200, 300 and 450 tasks. Misthalin is closed. Zanaris and the Abyss are always open. Walking or teleporting into a locked region bounces you back. |
| Tasks | 210 starter tasks: region boss and monster kills, skill levels, total level, combat level, kill counts, and pact milestones. 10/30/80/200/400 points by difficulty. |
| Pact points | 2 to start, 1 per pact task, plus 1 every 10 tasks (configurable). 44 can be earned, 40 can be held at once. 6 full resets. |
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

### Relics that work

| Relic | On this server |
|---|---|
| Endless Harvest | Double logs, fish and ore and their XP; trees and rocks don't deplete; resources go to the bank (`::relic bank off`). |
| Barbarian Gathering | Failed gathering attempts succeed half the time; bonus Strength and Agility XP. Tools are still needed and the knapsack does nothing. |
| Abundance | +10 to non-combat skills, +2 XP per drop in them, 2 coins per XP gained in them. |
| Hotfoot | Caught fish are cooked and mined ore is smelted (level permitting); Cooking never burns; Agility always succeeds; running gives Agility XP. |
| Friendly Forager | 1 in 6 gathered resources also finds a grimy herb. Herblore perks aren't in. |
| Woodsman | Cut logs are burned for Firemaking XP (`::relic burn off`); Hunter always succeeds. |
| Bank Heist | The briefcase teleports to banks in unlocked regions (found from banker spawns). |
| Transmute | The ledger is an unlimited nature rune source. The new alchemy spells aren't in. |
| Larcenist | Thieving always succeeds; 10x loot, noted. |
| Grimoire | The grimoire switches spellbooks. |
| Eternal Sustenance | Food isn't used up. |
| Reloaded | One extra relic from any lower tier, picked in the relics window. |
| Executioner | Hits with the Sage's axe that leave a monster under 20% HP finish it (not Yama). |
| Flask of Fervour | Restores HP, Prayer and special attack; 3-minute recharge, shortened by damage dealt. |

Not in yet: Evil Eye, Map of Alacrity, Conniving Clues, Butler's Bell, Nature's Accord, Soul
Harvest, Culling Spree, Flow State and Minion. Their items are still handed out.

## Not included

- The real pact tree and leagues interfaces. Everything runs through chat commands for now.
- The relics listed as not in yet, echo bosses and echo orbs, the reward shop,
  boosted minigame points, and the official task list.
- Jagex's exact region borders; `demonic_pacts_regions.toml` uses hand-drawn boxes.

## Commands

| Command | Does |
|---|---|
| `::league` | Points, tier, rates, pacts, regions. |
| `::pacts` / `::pacts owned` / `::pacts all` | Pacts you can buy now, hold, or all of them. |
| `::pact <id>` | One pact's effect and connections. |
| `::pactbuy <id>` | Spend a pact point. |
| `::pactreset confirm` | Clear every pact (uses a reset). |
| `::tasks [todo\|done\|all\|<region>] [page]` | Task list. |
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
| `src/main/resources/demonic_pacts_tasks.toml` | Tasks. |
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
- Woodcutting now honours `SkillingProduct.depletes`, like mining.
- `PlayerCombatModifiers` (api/combat-manager): accuracy and max-hit multipliers for players
  attacking npcs.
- `NpcHitEvents.ModifyAny` (api/npc): a hit-modify event for every npc type.

Data sources: the [OSRS Wiki Demonic Pacts League page](https://oldschool.runescape.wiki/w/Demonic_Pacts_League)
and its [pact tree data module](https://oldschool.runescape.wiki/w/Module:Demonic_Pacts/data).
