## TODO
- Explain types' computeIdentityHash.
- Nested EnumTypes in EnumProvider. Due to type-erasure, it is extremely tedious
to even begin to try and make the inner EnumTypes type-safe (checked during
boot-up). For now, we will just trust that the developers are careful (BAD!),
but solely because the other choice is to load them via reflection and try to
do comparisons that way, which may have a significant impact in future
scalability when it comes to boot-up performance.
- InvTransactions uses a cached singleton. It would become annoying for coders
to have to constantly get access to the required "lookup" maps in order to
perform an inv transaction. We willfully allow this antipattern for ease-of-use.
- Private type reference subclasses not allowed. Must be internal or public.
- .local in symbols directory being loaded by default and overrides any possible
colliding symbols from the root symbols.
- All game-related randoms should be generated through `GameRandom` functions.
- The great teleport/telejump tragedy of 24' where we had to choose between
anti-patterns that would blow a huge hit to testability or jeopardizing
user-friendliness by requiring the what-should-be simple teleport functions to
take a [CollisionFlagMap] instance arg. We went with the latter. The biggest
weakness of this approach is increasing the barrier-to-entry when a developer
wants to perform simple tasks. Teleporting _should_ be a simple task, but alas,
if we want to emulate core mechanics, teleporting needs to _instantly_ update
the collision flag map. We weighed our options from using singletons of the
collision map, to passing the collision map to each player/npc, to having a pub
sub system strictly for map changes - though at that point it'd be practically
the same thing as passing the game map pointer to the entities in question.
The solution of taking in the collision flag map as an arg is something that
will, *eventually*, solve itself by way of adding API-friendly wrapper classes
for systems such as "fade out" teleports, teleporting after interacting with a
loc, and so on. Though admittedly, it's a cumbersome ask for beginners to have
to deal with until those systems are in place.
- Decision to not make ClientScript arguments type-safe. Just not worth it,
especially if you consider that the arguments would have to be converted to
their primitive form dynamically during runtime.
- `delay` functions, AKA `p_delay`, do _not_ follow the "official" convention.
This means that calling `delay(1)` will delay the player by 1 tick, not 2.
`delay(0)` will not delay the player at all.
- Mention the purpose of each api submodule (in a README within api module).
- Inconsistency in OpLocHandler debug message vs. LocUInteractions debug
message. This comes down to the packet handler not having the detailed view of
the multiloc based on the player's vars. Meanwhile, OpLocU (as well as OpNpcU)
are handled at the "content-level" due to how they were changed from being
individual packets. The content-level scripts are exposed to the "visual" type
for these entities.
- Tests that involve obj transactions ([org.rsmod.objtx]) are not thread-safe
and the test class should be marked with `@Execution(ExecutionMode.SAME_THREAD)`
- `LocRegistry.findAll(zoneKey)` performed within a region's zone will not
return locs that did not originally belong to the copied normal zone. This is
intentional by design to prevent inconsistencies and unexpected behavior. This
scenario is rare but can occur under specific conditions - when a loc's angle,
dimensions, and the requested region rotation cause it to shift into an
unexpected region zone. This is an extremely niche case.
- Similarly, `findAll` performed in a region will skip any locs that have
shifted into a different zone.
- Any interaction on locs in region areas that are outside the region's working
area are discarded/ignored.
- If multiple zones in a region are copies of the same zone,
`Region.normal[...]` will resolve to the last one of these registered zones.
Since regions register zones column by column, from bottom to top, this means
the zone closest to the north-east boundary takes precedence.
- Zone update processing occurs in a fixed order, from the bottom-left build
area zone to the top-right zone. This means all `ZoneProt` updates are sent in
a predictable order, rather than in the order the updates occurred. For
example, if `zone(0, 1, 0)` receives a `soundarea` update first and
`zone(0, 0, 0)` receives one afterward, the latter will still be sent first due
to fixed iteration order; whereas the official game would send `zone(0, 1, 0)`
first since it was the earliest update.

## Level-up popups

`LevelUpScript` sets `varbit.busy` while `levelup_display` is open and clears
it through the interface close hook. Keep cleanup tied to the interface,
not the suspended dialogue: walking or a hit can cancel that dialogue.
Disabled popups do not set the flag.

## Types
```
engine
    └─ game
        └─ type
```
#### <u>_Internal `id` field_</u>
```kotlin
class ItemServerType(internal var id: Int?)
```
There was a conscious decision to allow for configuration types' "id" to be
mutable, and nullable.
#### What problem does this solve?
- Type-safety:
  - For years, frameworks used magic numbers for config types, whether it was
    for worn obj conditions, or to define a rewards table. This comes with
    obvious flaws (see [magic-numbers]). With time people moved onto static
    classes that held each individual id with a reasonably-named field. Again,
    this still comes with its problems. For example, you are still able to
    compare an obj to a constant that's not an obj:
    `if (player.righthand == Spotanims.iron_arrow_projectile)`.
- Consistency between references and configs in cache:
  - Another major flaw with the traditional approach, is that there was never a
    feasible way to make sure all references actually exist in the game cache.
    As the developer, you just had to trust that these constants were up-to-date
    and their naming stayed 100% true to what they originally meant to
    represent.
- Creating custom config types:
  - TODO:
#### What are the pros and cons?
- Cons
  - Can be abused and the `id` may not be accurate after runtime if a developer
    sees fit to modify it.
  - TODO:
- Pros
  - TODO:

## Account save snapshots

`AccountManager.save` requests capture at the end-of-tick gameplay boundary. Persistent
varps, serialized attributes, inventories, stats, identity and world type are detached
before background SQL runs. `CharacterDataStage.Pipeline.capture` must return a
`CharacterDataStage.Save` that consumes captured values, never the live player.

Mutations during an in-flight autosave remain dirty for a subsequent snapshot. Retries
consume the same snapshot before newer requests. Shutdown drains an interrupted batch
before queued requests, retaining each request until it succeeds or reports failure.

## Player timer phases

Soft timers normally run before interactions. `softTimer(..., afterInteractions = true)`
opts a timer into the pass immediately after player interactions. Foundry work uses this
phase so a tool re-click can affect the work consumed on that same tick. Rescheduling
moves a timer between phases; clearing it removes either registration.

Global effects tied to player work use `StartCycle`, before the map clock advances,
so their animations and sounds share the player's work tick.

## Foundry item safety

`PlayerDeathPreDropHook` runs before carried items are captured for death drops,
including administrator deaths that bypass drops. Foundry stores the preform here,
before a grave or ground stack can claim it. Equipment replacements store displaced
preforms after the equipment transaction commits. Teleport validation does not
store a preform; a successful coordinate change does.

Sword hand-in and ore-pack conversion use all-or-nothing inventory transactions.
Failed payment leaves the sword, commission and reward state unchanged. Coins go
to the bank first, then inventory if necessary; Ultimate Ironmen use inventory only.

## Teleport items

Destinations live in the `teleport_destination` dbtable and the items that use them in
`teleport_item` (`content/travel/teleports`). Teleport jewellery, Xeric's talisman, skill capes,
spirit trees and fairy rings are their own modules. Known differences from OSRS:

- Every destination is open: quest, diary and unlock requirements (Darkmeyer, Slepe, ...) are not
  checked.
- A destination whose map region is missing tells the player so instead of teleporting, and no
  charge is spent.
- All items use the standard jewellery teleport animation; the ectophial and seed pods do not play
  their own.
- Achievement diary rewards (Ardougne cloak, Karamja gloves, desert amulet, Morytania legs,
  Fremennik sea boots, Kandarin headgear, wilderness sword, western banner, Explorer's ring, Rada's
  blessing) teleport without their daily limits. Tiers that cannot teleport in OSRS (desert amulet
  1, western banner 1 and 2, and so on) still cannot.
- Kharedst's memoirs and the Book of the Dead spend `veos_memoir_charges`, but recharging at the
  Old Memorial and adding pages are not implemented.
- Memoir charges sit in a player varbit, so every copy a player owns shares one pool.
- The camulet has unlimited charges, as if Lazim had upgraded it, and both destinations are open.
  The grand seed pod is used up by `Squash`; its `Launch` op is not implemented.

Wilderness obelisks use the `wilderness_obelisk` rows. `Set Destination` and `Teleport to
Destination` need no Wilderness diary and do not remember a choice: both open the list and send
the pad straight to the picked obelisk.

Gnome gliders use the `glider` rows. Every glider, including Feldip Hills and Ape Atoll, is open
without The Grand Tree, One Small Favour or Monkey Madness II, and pilots skip their dialogue.
`varbit.pilot_multinpc_var` is set on login so the Feldip and Ape Atoll pilots show their glider
forms. Captain Errdo's `Talk-to` at the Digsite opens the map, so Lemanto Andra is two-way here
(it is arrival-only in OSRS).

Charter ships use the `charter` rows. They are free, open from a plain list rather than the
Sailing-era charter map, and land on the dock instead of the ship's deck (gangplanks are not
usable yet). Tempestus, Crandor and Barracuda HQ from Jagex's `chartering_destinations` are not
stops.

Quetzals read Jagex's own `dbtable.quetzal`. Every landing site is marked built on login (Twilight's
Promise and the Hunter landing-site builds are not required), and flights are a fade rather than
the flying cutscene.

Teleport scrolls land on the tiles in `enum.teleport_scroll_destinations` (the wiki's map squares,
all ground level) and need no unlocks; the Revenant cave scroll, which offers three Wilderness
entrances, is not handled yet.

Magic carpets use the `carpet_shantay` and `carpet_pollnivneach` rows (hub first). Rides are free,
fly a straight line at 3 tiles a tick with no banking turns or sound, and need no quests (Uzer,
Sophanem and Menaphos included).

Minecarts use the `lovakengj_minecart` rows (any station to any other, free without The Forsaken
Tower) and the `keldagrim_cart` rows (Keldagrim first; the Grand Exchange trapdoor and the Ice
Mountain and White Wolf Mountain carts lead back to it). Rides are a fade with no fare.

Boats, ferries and rowboats come from `dbtable.teleport_route`, generated from the shortest-path
plugin's ship and boat lists (91 routes). They are free, need no quests, and sail with a fade.
Routes whose op did not match the cache (some Rellekka boats, Waterbirth's Jarvald, and a few
others) are not bound, and the Entrana and Void Knight ferries keep their own scripts.

Minigame teleports (Grouping tab) use the `minigame` rows, matched by name to Jagex's
`minigame_teleport` rows. They play the home-teleport cast and share Jagex's 20-minute cooldown
(varp 888, which also greys the Grouping Teleport button), but need no minigame unlocks. The
cooldown message wording is a guess. The Minigame Teleport spell opens the scrolling `minigames`
list, where Select teleports; the Grouping tab's Teleport button works too. The channels tab
buttons open their panels.

[magic-numbers]: https://en.wikipedia.org/wiki/Magic_number_(programming)
