# Cave krakens and Kraken

Implementation: `content/bosses/kraken`, using the existing revision-240 map spawns,
NPC models, named animations, Slayer task data and drop tables. No map, client or
cache replacement is required. Status: implementation candidate, awaiting in-game
user acceptance; this is not a declaration of exhaustive OSRS parity.

## Encounter

- Regular whirlpools surface a level-127 cave kraken with 125 HP. Its 6-tick magic
  attack has a maximum of 13 and can be protected against with Protect from Magic.
- The large whirlpool requires the four surrounding tentacles to be disturbed.
  Using one fishing explosive on the large whirlpool surfaces all five actors.
  Explosives do not surface ordinary cave krakens.
- Kraken has 255 HP and a 4-tick maximum-28 attack. Its four tentacles have 4-tick
  maximum-2 attacks. Accuracy uses ranged attack against magic defence; damage is
  typeless, so protection prayers do not block it.
- Ranged damage is divided by seven; melee cannot damage the underwater actors.
  The existing cache supplies elemental weaknesses and combat stats.
- Surfacing, attacks and deaths use native named model animations. Projectiles
  use Fire Wave for the boss and Water Wave for the smaller actors.
- An encounter belongs to its initiating player. Normal access requires base and
  current Slayer 87 and an active cave kraken/Kraken task. Duplicate clicks and
  other players cannot create extra actors. Player attacks resume after surfacing.
- Death uses the native kill/drop pipeline (including Slayer, kill count and pet
  hooks) with accessible loot on the player's land tile. Tentacles give no loot.
- The original whirlpools remain hidden while their actors exist and return after
  death. The boss retains the fork's accepted 34-tick respawn policy and native
  countdown; cave krakens reset after 25 ticks. Logout, death or leaving range removes actors and restores the pools;
  delayed projectiles and auto-attack callbacks check encounter identity first.

The existing public cave is used. Paid private instances, Lieve dialogue and
Kraken-specific Combat Achievements are not included in this change.

## Testing in game

At the existing Kraken Cove location, administrators can use `::krakentest` to
test without replacing their Slayer assignment. This temporary access expires on
leaving/logout; `::krakentest off` disables it immediately. Normal kill rewards
still apply, including task credit when the existing assignment matches.

## Validation

Real-cache registration and controller tests cover both activation routes,
ownership, no duplicate spawns, native HP/stats, Slayer restrictions, ranged/melee
damage rules, death/reset and logout cleanup. Command tests check that `::maxrange`
now supplies `obj.necklace_of_rupture` in inventory, with the existing atomic
delivery and equipment-preservation behaviour.

Validation on 2026-10-04: 11 Kraken tests, 9 command tests, formatting, full
server JAR build and isolated bridge startup/shutdown passed. Installer evidence records the exact tested JAR
and cache hashes. In-game acceptance should check model placement/animations,
ordinary cave kraken prayer protection, all five boss attacks and repeat kills.

Mechanic references:
[Kraken strategies](https://oldschool.runescape.wiki/w/User:Jeljo/Kraken/Strategies),
[Cave kraken](https://oldschool.runescape.wiki/w/Cave_kraken),
[Fishing explosive](https://oldschool.runescape.wiki/w/Fishing_explosive).
Asset identities and stats were resolved from this fork's installed cache, not
copied as numeric animation IDs from another revision.

This branch starts after the fang-crafting fix (PR #18). That fix remains intact;
Kraken is a separate review/test candidate and has not been approved for merge.
