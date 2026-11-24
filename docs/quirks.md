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

[magic-numbers]: https://en.wikipedia.org/wiki/Magic_number_(programming)

## Pirate's Treasure

Built from the OSRS wiki transcript and RSProx captures of Luthas's banana job (rev 239),
the Falador park gardener (rev 235) and Hector's chest (rev 232). Known differences:

- Karamjan rum only gets taken by the Musa Point customs officer. It does not break on a
  teleport: every move (stairs, ships, spells) goes through the same `telejump` validation,
  so there is no hook that fires only for magic teleports. The other Karamja exits the wiki
  lists (charter ships, Shilo cart, gnome glider, Lady of the Waves, Mor Ul Rek) do not check
  for rum either.
- The customs officer has no Ring of Charos(a) charm options and no Karamja gloves discount.
  Her "Pay-Fare" option with rum in the pack confiscates it and does not sail; how live OSRS
  handles that case is not documented.
- The quest journal text is written from the quest's steps; no capture of the live journal
  was available. The rum smuggling lines keep the phrases RuneLite's Quest Helper reads to
  sync ("I need to go to", "I have taken employment", "I have hidden my", "the crate has been
  shipped", "I have taken a job at", "I should take it to", "I seem to have lost it.").
- Hector's chest uses up the key when it opens. The live drop trick that yields extra
  pirate messages is not reproduced: once opened, "Open" says the chest is locked, and only
  using a key on it during the key stage opens it again.
- The gardener leaves once his digger is more than 12 tiles away or after 500 ticks, instead
  of exactly when the player leaves the park.
- Messages with no source, written to match nearby ones: "You don't have any bananas." when
  filling the crate with none, "You don't have enough inventory space to do that." when the
  casket, the dig or Wydin's crate needs a free slot, the "Do you want to take a banana?"
  prompt and "You take a banana." at Wydin's crate, and "Why would I want to do that?" for
  rum used on the crate before Luthas hires you. "There is also some rum stashed in here too."
  is sent as a game message like the other crate search lines (the wiki shows a box).
- Taking the white apron in Gerrant's shop is an ordinary ground item pickup, without the
  "freshly starched" message.
- Chathead expressions for Frank, Luthas, Wydin and the customs officer are chosen by hand.

## Mudfoot's Big Adventure

Built from an RSProx capture of the whole quest on a fresh account (rev 242.1), with the OSRS
wiki for branches the capture does not take. Known differences:

- The fights and the General Store scene run in instanced copies of the area, as in OSRS, but
  only the scene's own npcs are spawned there: the sheep, farmers and shop staff that OSRS
  copies in are left out.
- Mudfoot's attacks on the outlaws are scripted (every 4 ticks, stab or thrown rock, rolled with
  the npc-vs-npc formulas): the engine has no npc-vs-npc combat.
- The outlaws use the generic npc combat, with their launch, cast and impact spotanims added by
  an attack hook. Hit timings follow that combat code rather than the capture's exact delays.
- When the warrior goes 12 ticks without attacking the player (as in the capture, after the
  player runs off) while Mudfoot stands beside him, he turns on Mudfoot for the rest of the
  fight. His hits are capped so Mudfoot keeps at least 1 hitpoint; the capture never shows
  Mudfoot near death.
- Mudfoot shouts "Look! There she is!" and "Look! That him!" when the player confronts the
  outlaw, instead of a few ticks earlier as they come into view.
- The mage's "H.A.M. will get rid of people like you!" fires on her first damage rather than on
  the player's first attack.
- Npcs other than Captain Harlan do not react to Mudfoot following the player.
- Mudfoot waits for the player beside the outlaw in the woods and in the graveyard; the capture
  never shows those spots, so the tiles are chosen to match where he reappears after each fight.
- Lines with no capture source, written from the wiki's summary: Captain Harlan's combat advice,
  the guards answer, "I'm still looking into it." about the thieves, the weapon offer and the
  "Captain Harlan gives you..." handout box, the "already have" lines for the bow and staff, the
  full-inventory line before the reward, and Mudfoot's "Me meet you back..." waiting spot names.
- Captain Harlan's Defence cape: the wiki transcript has no lines for a purchase or a missing
  hood, so those use the lines other skillcape sellers give ("Excellent! Wear that cape with pride
  my friend.", "Well, come back and see me when you do.", the free-hood space line). "Of course!
  Here you go." for a spare hood is written to match. The option shows only at 99 Defence, as the
  captured level 3 account never saw it.
- Captain Harlan's one free handout per style during the quest uses the `bonus_*_giveaway`
  varbits as "already given"; after that every handout shares the 30 minute cooldown.
- Chathead expressions outside the captured dialogue are chosen by hand.
