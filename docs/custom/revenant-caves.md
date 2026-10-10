# Revenant Caves

## Sources and implementation decision

Reviewed 2026-10-10 against [Revenant Caves](https://oldschool.runescape.wiki/w/Revenant_Caves),
[Revenants](https://oldschool.runescape.wiki/w/Revenants), their individual monster
pages, [drop template](https://oldschool.runescape.wiki/w/Template:Revenants/Drops),
[strategies](https://oldschool.runescape.wiki/w/Revenants/Strategies), Maledictus,
entrance/exit/shortcut pages and the six ancient artefacts. Retrieved raw pages
and SHA-256 evidence are retained outside the repo in `work/revenant-wiki`.

Upstream main `97502542e388bc9c3b433361f3c63c3419738422` has the same partial
Wilderness infrastructure, formulas, cave map and skipped revenant drop entries.
All-state PR searches for Revenant, Maledictus, Forinthry and Ethereum returned
no matches. Preserve reviewed native
integrations, implement missing content in an area module, and make bounded
core changes only where existing hooks cannot express singles-plus/death rules.
Do not import an upstream branch.

## Required gameplay

Three one-way entrances (17/26/40 Wilderness), two stair exits, five boostable
65/75/89 Agility shortcuts. Admission is 100,000 coins from inventory/bank,
remembered until cave death or PvP Wilderness death; exit does not charge again.
Combat is singles-plus, with a PvP engagement taking priority over ordinary PvM.
Revenants adapt all three styles to prayers/defences, heal by 20 below half HP
with 15-tick cooldown and uniformly 8–25 total heals (updated Mod Ash source).
Ethereum reduces damage by 75%, consumes one ether per attack, suppresses
aggression, can absorb dropped ether, and supports charging/uncharge/dismantle.
All eleven normal species use the combat-level-scaled Wiki loot with skull/task
modifiers; supply rolls remain unnoted and avarice notes applicable main drops.
Ancient artefacts exchange for 500k/1m/2m/4m/8m/16m coins, or chisel into
1k/2k/4k/8k/16k/32k ether, without duplication or partial inventory mutations.

Maledictus requires accumulation-based world spawn/cooldown, all-player attacks,
ice/blood/air specials, MVP/participant rewards and Forinthry Surge. The Wiki
does not publish its exact flat spawn denominator, attack-selection timing or
shortcut failure probability; these must be explicitly documented as configured
server tuning, rather than described as verified OSRS constants.

The native scroll's three surface destinations from PR #35 remain intact.
Current full status and remaining validation belong in `PROGRESS.md`.

## Fork decisions and source limits

- Keep the accepted revision-240 client, model, animation and server cache.
  Apply only the selected SERVER definitions and six cave map chunks. All
  117,585 LIVE archives and unrelated SERVER files retain their bytes and IDs.
- Trader exchange is available on this fork's local world, rather than requiring
  an official Bounty Hunter world. His native Exchange, Skull and item-use
  operations remain the entry points.
- Maledictus spawn accumulation adds the killed revenant's combat level to a
  threshold out of 1,000,000. There is one world encounter, a 4,500-tick cooldown
  from spawn, and 1,000 ticks without combat before expiry. The full 5x5 model
  must fit walkable cave terrain. Hints and expiry share one bounded tick task.
- Configure three normal attacks before each rotating ice/blood/air special.
  Ice freezes for five ticks; air deals 25/17/9 damage at distance 0/1/2. These
  unpublished timing/falloff values and shortcut failure odds (5%) are server
  tuning. They are not claimed to be measured OSRS constants.
- The Wiki rare table lists weights totalling 39/40 (21/22 skulled). Reserve the
  unexplained remaining slot without an additional item; guaranteed ether still
  drops. This is an inference that preserves every published individual rate,
  rather than increasing all rates by renormalising the listed weights.
- Use native `saradomin_lightning` for the ordinary revenant heal visual, with
  the species' accepted attack/death sequences and native revenant projectiles.
  Final visual comparison remains an in-game acceptance check.
- Powered sceptres use the actual rev240 `spells_thammaron01_travel01` projectile;
  its definition lacks a generic weapon projectile param. Swapped sceptres retain
  the existing native autocast path. Webweaver consumes one ether per special,
  matching its documented one-charge update.
- Base weapons dismantle for 7,500 ether. Upgraded weapons return the original
  weapon and component. Level-85 item combinations preserve charges and yield
  no XP; native staff swaps preserve item vars. Tradeable equivalents rank
  charged weapons for Items Kept on Death. Craw's activation deposit burns;
  Webweaver's deposit drops along with its other ether.
- Reuse the existing native uncharged sceptre/skull Crafting row. Register only
  missing charged/recoloured variants, avoiding a duplicate handler at startup.
- A deferred teleport scroll remains in inventory until actual activation.
  Movement, death, logout, item replacement and late teleport denial cancel
  arrival without consuming it.

## Test commands and acceptance

`::testrevenants [1-3]` places the administrator outside the south, middle or
north entrance. `::revsitems` adds supplies transactionally; make inventory room.
It provides ether, bracelet, all three base weapons, avarice, teleport scrolls,
chisel, all six artefacts and entry coins. It does not clear existing items.

`::testloot revenants [1-1000]` uses the dragon table and native kill hooks;
`::testloot maledictus [1-1000]` uses the MVP table. Killcount, Collection Log
and loot tracking use their normal consumers. Fake kills do not spawn a world
boss, grant Surge or participant loot. `::testmaledictus` spawns one boss near
an administrator who is already in an open part of the caves.

Check all three scroll choices and entrances, both stair exits and each pillar
in both directions. Verify one entry fee, retention after exit, and loss on
cave/Wilderness PvP death. Wear charged ethereum, provoke a revenant and check
damage/charge reduction; toggle absorption. Test charged weapons inside and
outside Wilderness, sceptre Swap, trader exchange and artefact/chisel conversion.
For Maledictus, check back-symbol colours, tile dodges, blood healing, air area
damage, participant food, MVP rewards/Surge and multiplayer/PvP behaviour.

Automated evidence uses actual native object interactions, NPC combat producers
and hit queues, item transaction rollback, loot snapshot/delivery, charge gates,
death previews/completion and interrupted teleports. Owner acceptance is needed
for animation appearance, route feel and multiplayer behaviour in the client.

## Automated validation

44 new feature tests and 204 selected tests pass, including existing drops,
teleports, special weapons, Tormented Demons, shamans and gorillas. All 58
changed Kotlin files pass the scoped formatter. The final runtime JAR boots
with the accepted rev240 cache/Nero plugin in an isolated database and exposes
working health, catalogue, effect and respawn endpoints. All 15 entrance, exit
and shortcut landing tiles are walkable in the native runtime map.

Cache comparison preserves 117,585 LIVE archives, unrelated SERVER files and
220,158 existing mappings. Six new server-only mappings are added. Native
new-account initialization and all 19,438 varbit bases validate. The isolated
process exits normally and its PostgreSQL PID file is removed. It retains the
previous teleport baseline's database-close diagnostic during shutdown; this
does not establish a new Revenant regression.

The guarded package is `outputs/revenants-20261010`. It includes the candidate,
before/after hashes, a verified rollback checkpoint and test/cache/boot evidence.
No live installation is performed by preparing or checking this package.
