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
| Relic tiers | Reached at 0, 600, 1,200, 2,600, 5,200, 8,500, 16,500 and 28,000 league points. Only the tier passives above are implemented, not the relic choices. |
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

## Not included

- The real pact tree and leagues interfaces. Everything runs through chat commands for now.
- Relic choices (Endless Harvest, Hotfoot and the rest), echo bosses and echo orbs, the reward shop,
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
- `StatXpModifiers` (api/player): per-stat xp multiplier for naturally earned xp.
- `PlayerCombatModifiers` (api/combat-manager): accuracy and max-hit multipliers for players
  attacking npcs.
- `NpcHitEvents.ModifyAny` (api/npc): a hit-modify event for every npc type.

Data sources: the [OSRS Wiki Demonic Pacts League page](https://oldschool.runescape.wiki/w/Demonic_Pacts_League)
and its [pact tree data module](https://oldschool.runescape.wiki/w/Module:Demonic_Pacts/data).
