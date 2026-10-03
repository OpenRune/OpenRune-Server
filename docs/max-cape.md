# Max cape

The original `obj.skillcape_max` now handles its inventory submenus. Wearing it uses
the existing inventory transaction and transforms it into `obj.skillcape_max_worn`.
The worn equipment tab and equipment statistics window route submenu packets to
their registered interface handler before attempting an inventory lookup.

Supported actions:

- Guild, skilling and house-portal **location** teleports. Chinchompa teleports share
  five daily uses; Black chinchompas asks for Wilderness confirmation. All teleports
  obey the server's teleport restrictions and look for a nearby walkable tile.
- Four spellbooks, five actual changes per day, and a remaining-use check. Selecting
  the current book consumes nothing; changing books clears autocast.
- Search: a bronze crossbow and attached mithril grapple, three times per day. Both
  items must fit; a failed transaction consumes neither space nor a daily use.
- Stamina Boost: full run energy and at least 100 ticks of stamina, once per day.
  It does not shorten a longer active stamina potion.
- Ring of Life: toggles the worn cape's escape after a nonlethal hit at or below 10%
  base hitpoints. It obeys level-30 teleport restrictions. Simply standing at low
  health does not repeatedly trigger it.
- Commune: toggles miscellaneous metal collection while worn. The local collection
  interval is 100 ticks, subject to metallic torso interference and inventory space.
  It uses steel arrows with rarer steel darts, knives, nails, bars and iron ore.

Daily counters reset at midnight UTC on the next action and persist with the player.
Preferences persist separately; collection scheduling and pending escape are temporary.

## Deliberate limits

Boat, boat naming, boat relocation and The Pandemonium remain unavailable pending
Sailing integration. These actions return an explicit message. Home goes to the
server home in Lumbridge because player-owned houses are not implemented. The other
house destinations lead outside their portals; they do not create or enter a house.
Combined combat capes (such as Infernal max cape) do not gain original Max cape perks.
This module does not claim to implement every passive skillcape perk.

## Validation

Run `gradlew :or-cache:buildCache` after changing the pack, then
`gradlew :content:other:max-cape:test :api:net:test :content:other:consumables:test`.
Use the existing revision-240 cache and matching CS2 project when building.
The local validated development project uses the 240.9 CS2 bundle; selecting 240.2
with a fresh CS2 directory fetched an incompatible 240.1 bundle during investigation.

In game, test the original Max cape in the inventory, then wear it and repeat in the
equipment tab and equipment statistics window. Check cancel and blocked teleports,
a nearly full inventory, relogged daily limits, low-health damage and the stamina
orb. Automated tests are not a substitute for visually checking the client menus.
