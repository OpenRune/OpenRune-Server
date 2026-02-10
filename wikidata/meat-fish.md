# Meat / fish (Cooking skill)

- Extracted from: `cooking.wikisrc`
- Section: Meat / fish
- Extract date (UTC): 2026-01-17
- Rows extracted: 65

`Done` is marked when a corresponding cooking action exists in `cache` (recipes), not just when an item exists in the cache.

| Done | Level | Item | XP | Heal |
|---|---:|---|---:|---:|
| [x] | 1 | Cooked meat | 30 | 3 |
| [ ] | 1 | Sinew | 3 | N/A |
| [x] | 1 | Shrimps | 30 | 3 |
| [x] | 1 | Cooked chicken | 30 | 3 |
| [x] | 1 | Cooked rabbit | 30 | 5 |
| [x] | 1 | Anchovies | 30 | 1 |
| [x] | 1 | Sardine | 40 | 4 |
| [ ] | 1 | Poison karambwan | 80 | -5 |
| [x] | 1 | Ugthanki meat | 40 | 3 |
| [x] | 5 | Herring | 50 | 5 |
| [ ] | 7 | Guppy | 12 | N/A |
| [x] | 10 | Mackerel | 60 | 6 |
| [x] | 11 | Roast bird meat | 60 | 6 |
| [ ] | 12 | Thin snail | 70 | 5-7 |
| [x] | 15 | Trout | 70 | 7 |
| [ ] | 16 | Spider on stick | 80 | 7-10 |
| [ ] | 16 | Spider on shaft | 80 | 7-10 |
| [ ] | 16 | Roast rabbit | 70 | 7 |
| [ ] | 17 | Lean snail | 80 | 5-8 |
| [x] | 18 | Cod | 75 | 7 |
| [x] | 20 | Pike | 80 | 8 |
| [ ] | 20 | Cavefish | 23 | N/A |
| [x] | 21 | Roast beast meat | 82.5 | 8 |
| [ ] | 21 | Red crab meat | 85 | 8 |
| [ ] | 21 | Cooked giant crab meat | 100 | 10 |
| [ ] | 22 | Fat snail | 95 | 7-9 |
| [ ] | 23 | Cooked wild kebbit | 73 | 4+4 (8) |
| [x] | 25 | Salmon | 90 | 9 |
| [x] | 28 | Slimy eel | 95 | 6-10 |
| [x] | 30 | Tuna | 100 | 10 |
| [x] | 30 | Cooked karambwan | 190 | 18 |
| [x] | 30 | Cooked chompy | 100 | 10 |
| [ ] | 31 | Cooked fishcake | 100 | 11 |
| [ ] | 31 | Cooked larupia | 92 | 6+5 (11) |
| [ ] | 32 | Cooked barb-tailed kebbit | 106 | 7+5 (12) |
| [ ] | 33 | Tetra | 31 | N/A |
| [x] | 35 | Rainbow fish | 110 | 11 |
| [x] | 38 | Cave eel | 115 | 7-11 |
| [x] | 40 | Lobster | 120 | 12 |
| [ ] | 41 | Cooked jubbly | 160 | 15 |
| [ ] | 41 | Cooked graahk | 124 | 8+6 (14) |
| [x] | 43 | Bass | 130 | 13 |
| [x] | 45 | Swordfish | 140 | 14 |
| [ ] | 46 | Catfish | 43 | N/A |
| [ ] | 51 | Cooked kyatt | 143 | 9+8 (17) |
| [ ] | 53 | Lava eel | 30 | 14 |
| [ ] | 56 | Swordtip squid | 150 | 15 |
| [ ] | 59 | Cooked pyre fox | 154 | 11+8 (19) |
| [x] | 62 | Monkfish | 150 | 16 |
| [ ] | 68 | Cooked sunlight antelope | 175 | 12+9 (21) |
| [ ] | 69 | Giant krill | 177.5 | 17 |
| [ ] | 71 | Jumbo squid | 180 | 17 |
| [ ] | 72 | Sacred eel | 109-124 | N/A |
| [ ] | 73 | Haddock | 180 | 18 |
| [ ] | 79 | Yellowfin | 200 | 19 |
| [x] | 80 | Shark | 210 | 20 |
| [x] | 82 | Sea turtle | 211.3 | 21 |
| [ ] | 82 | Cooked dashing kebbit | 215 | 13+10 (23) |
| [ ] | 83 | Halibut | 212.5 | 20 |
| [x] | 84 | Anglerfish | 230 | 3-22 |
| [ ] | 87 | Bluefin | 215 | 22 |
| [x] | 90 | Dark crab | 215 | 22 |
| [x] | 91 | Manta ray | 216.2 | 22 |
| [ ] | 91 | Marlin | 225 | 24 |
| [ ] | 92 | Cooked moonlight antelope | 220 | 14+12 (26) |

## Notes

- Burn thresholds (fire/range) are not included here yet; the saved `burn_level.wikisrc` needs additional parsing/naming normalization.
- Some entries (e.g., sinew, roasted kebbits, fishcake) may require different triggers/constraints than simple heat-source cooking and may be implemented separately.
- Guppy, cavefish, tetra, and catfish are prepared on the Preparation Table with a knife (runtime item-on-object), so they are not marked done under the "cache cooking action exists" rule.
