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

## Marks of grace

A mark of grace lies for 1000 ticks from the moment it spawns, whether or not
its owner stays logged in; in OSRS its timer pauses while the owner is logged
out. The spawn timer is one Perm varp holding the real-time minute from which
marks may roll again (three minutes after a spawn, two when a course's diary
shortens it), so it survives logout and world changes, and it is shared by every
course: the wiki does not say whether the timer is per course.

## Agility transformations

Larry's Tuxedo-time puts a player carrying a clockwork suit into the penguin
suit, and only a player in it may run the Penguin course. No capture names the
npc the suit turns a player into, so it is the suit's own (`peng_suit_npc`). The
suit stays in the inventory, Larry's refusals are guessed, and the combat,
prayer, magic and emote tabs stay as they are where live swaps them for the
penguin's. Something wielded in the suit is equipped and handed straight back
with live's message, where live refuses it outright. The suit comes off with
live's animation and message once the player leaves the mapsquares of the zoo
pen, the iceberg, the course and the secret lair, which stand in for the wiki's
"immediate area of Larry or other penguins". The secret lair and the course's
door let a human through too; live may not.

A worn small or medium ninja greegree, or the Kruk greegree, turns its wearer
into the matching monkey on Ape Atoll's four surface mapsquares, with no
spotanim or sound, and taking it off turns them back. The Kruk greegree takes
Kruk's own npc, and the other greegrees change nobody yet.

## Penguin Agility Course

The crushers always hit for the 4 captured, a tick after the push where live
hits with it. Each click on the ice moves the player one tile, and only the turn
from the east column to the west can slip, on a guessed success curve, for the 2
captured. The sled jumps and the finish line fire when a player is on the tile
before them at the end of a tick, so a player running two tiles a tick can pass
one and must step back onto it. The swimming and sledding stances, and the tiles
they hold on, are guesses. The ice steps out of the pool do nothing yet.

## Agility shared course starts

Shayzien's and the Colossal Wyrm's basic and advanced courses share their first
obstacles. Those pay the basic course's xp, so the Shayzien ladder gives 5.5
where live gives 6 on the advanced course, and the lap counts for whichever
course the player splits off to.

## Agility falls

The Seers', Falador, Varrock and Canifis rooftop falls play as captured live.
Their success curves are fitted to the falls captured at Seers' levels 60-70,
Falador 54-59 and Varrock 30-36, so where each stops failing is extrapolated:
the Seers' gap at 71, its tightrope at the wiki's 79, the Falador hand holds at
the wiki's 66 and both Varrock obstacles at a guessed 40. Live only shows a
headbar, not hitpoints, so the hit's share of the hitpoints left is fitted
assuming the captured players had 99. The Canifis gap fell once in the captures,
which sets its landing but not its damage roll. No capture shows an Al Kharid,
Pollnivneach or Ardougne fall and only one shows a Rellekka fall, from 2024, so
those stay guesses.

The wiki says obstacles on the Barbarian Outpost, Ape Atoll, Penguin and
Shayzien advanced courses and the Werewolf zip line can fail, but not where a
fall lands, so none of them fails here but the Penguin course's ice, whose slip
a capture shows.

The Wilderness and Prifddinas success curves are guesses shaped to the wiki's
notes: Wilderness obstacles still fail at 99, Prifddinas ones stop failing at
91. Every Wilderness sample is at 99, where 2 of 46 crossings failed. The
Wilderness stepping stones' fall into the lava is captured, landing on the bank
with 20 damage at 99 hitpoints; other hitpoints take a share guessed from that
one sample. A slip on the ridge drops the player into the wolf pit unhurt; only
a northbound slip on one tile is captured, so a southbound one lands in the same
spot. The rope swing and log falls are the wiki's, with guessed pit tiles. A
failed obstacle can be retried without losing the lap.

## Colossal Wyrm timings

The Colossal Wyrm courses play as captured in April 2026, before their August
2026 update, and pay the current wiki's xp.

## Agility stepping stones

Live keeps a player on a Werewolf or Penguin stepping stone, or a Werewolf
hurdle, for the tick after the jump, and a click on the next one meanwhile jumps
a tick later. Here that click jumps on that tick: holding the player longer
makes the engine try to walk to the stone and drop the click. Live's Penguin
stones are three ticks apart; here they can be two.

## Werewolf stick

The trainer takes the stick when its holder leaves the Werewolf area, by the
ladder, a teleport or a death, and on logout. Live also took it on a teleport
that landed back inside the course; here that keeps it. A logout here leaves the
player where they were, where the wiki says live may put them back at the
entrance. The exit ladder lands on the tile the werewolf's trapdoor puts players
down from, as no climb out was captured. The Agility Boss's line to a player
still holding a stick and the trainer's message to a player without one are
worded from the wiki's descriptions.

## Werewolf trainers

The stick trainer shouts every 12 ticks, as captured; of 19 captured shouts
about half were "Don't forget to give me the stick when you're done!", so that
line is picked half the time. As the pipe, the skull slope or the deathslide
starts, the nearest Agility Trainer shouts: the deathslide lines are captured on
every one of 27 slides, but the pipe and slope share a pool built from only four
captured shouts, and no trainer shouted at the hurdles or stones. The wiki's
skull slope taunt for a forgotten stick is not done, and the trainers' and the
boss's occasional flourishes are not played.

## Werewolf Skullball

Skullball is built from the wiki and the cache with no capture. The ball rolls a
tile a tick and bounces off walls back half of what was left of the roll, a rule
fitted to the wiki's tip that a shot east into the wall before goal 10 stops
where three taps west do. It rolls through players and npcs, jumps onto a goal's
centre tile because the goal loc blocks walking, and is seen by everyone.
Show-Goal points the hint arrow at the next goal, which the wiki does not
describe. The wiki's 4 minutes and 3 seconds are counted as 400 and 5 ticks,
whole periods only, and the xp stops at 0. Refusals for a low Agility level, an
unfinished quest or someone else's ball are guessed, and there is no scoreboard
after the final goal or clue step for scoring a goal.

## Agility course extras

Ape Atoll and the Penguin course need only part of a quest on live; here they
check that the quest is started.

## Giant squirrel

Every course with a base chance on the wiki rolls the giant squirrel once per
finished lap, after the mark of grace roll. The Colossal Wyrm courses never roll
it: the wiki has had no base chance for them since their August 2026 change.

## Agility lap times

Only the Colossal Wyrm and Prifddinas courses send "Lap duration" after the lap
count, as captured. No rooftop or other course capture shows one. A lap runs
from the landing of its first obstacle to the landing of its last. Prifddinas
times are in whole seconds, which here are rounded down; live's rounding is
unknown. A lap exactly as fast as the personal best does not count as a new one.

## Colossal Wyrm termites

The wiki gives no spawn rate for termites, so a finished lap spawns them 35% of
the time, on one of that course's markers. That rate is worked out from the
wiki's 5.6 termites per advanced lap. Here some obstacles play two of live's
obstacles. Termites on the first marker of such an obstacle are scooped when it
ends, not partway along. The bone shard line's colour is a guess. Termites and
shards that do not fit in the inventory drop at the player's feet. The client's
lap loot tracker varbit is not set.

## Prifddinas portals

Every lap picks one of the six portals, and it opens when the obstacle before it
lands; live opens it when that obstacle starts. The wiki's 0.94 crystal shards a
lap is read as a portal on almost every lap. The exits of portals 3, 4 and 5 are
guesses, set where the obstacle each one skips lands. A portal closes once used,
so it cannot be taken twice in one lap as it can live after a fall.

## Wilderness Agility dispenser

Tag hands over the last finished lap's ticket and, once the fee is paid, its
loot, which goes into an open looting bag. A lap finished before the last one
was tagged replaces its ticket and loot, and the streak keeps counting. The loot
sits noted in the bag, which no capture confirms. The prompt before a lap starts
also asks when only a ticket is untagged, which no capture shows. Leaving
through the ridge gate loses the fee, the loot and the streak but keeps the
ticket, and a logout resets the streak to 0. The fee is not lost on death. The
clue is always the same medium clue scroll, never a scroll box, and its part of
the loot line is a guess. The xp for 11 or more tickets redeemed at once is the
wiki's.

## Agility reward shops

Grace's shop sells the plain graceful set and amylase packs for marks of grace.
Her lap count scroll lists Skullball at 0 laps, as a finished game counts no
lap, and turning her lap counter off silences the lap count message on every
course and the Agility Arena. Pirate Jackie's Brimhaven graceful recolour is not
built. Worm Tongue's shop cannot sell the graceful crafting kit, because the kit
is a dummy item the omnishop cannot hand over. His Stats option is missing, and
so is his call from the start ladder before the player first talks to him. A
returning player's talk options are a guess; only a first conversation is
captured. The map spawns his three-option form, not live's npc that switches
forms on the first talk, so his Shop option shows from the start.

## Brimhaven Agility Arena crossings

Each move of the arrow keeps one plank of each set whole and shows broken boards
on the other two, as captured; how many boards of a bad plank break is a guess.
Below 99 Agility a broken plank drops the player two tiles in, onto the bottom
level of the pillar they set out from, for the traps' damage, and the rope there
climbs back to the tile above it. No capture shows the fall, so its seqs, its
landing and the climb are guesses. At 99 a broken plank is crossed like a whole
one, where the wiki says it takes a tick longer. The pillars and hand holds
never fail, as no capture shows a fall off them. The sides of a crossing no
capture shows mirror the captured side: the south monkey bars, the north low
walls' seq delay, the east side's rope swings and the west hand holds. A rope
swing clicked from the wrong side starts the other rope's swing if it stands
close enough, where live says the rope can't be used from there. The obstacles
ask for no Agility level, Karamja gloves add nothing to their xp, and the rope
swing does not animate its rope.

## Brimhaven Agility Arena traps

Floor spikes and pressure pads never fail, as their fall is not captured and the
wiki gives no chance for it. A ducked dart flies once over the player, where
live can send more during the slow motion, and the dart dispensers do not fire
at empty corridors. The saw blades rise on the world's own seven-tick clock, so
their phase is not live's.

## Brimhaven Agility Arena entry and tickets

A tag pays one Agility Arena ticket and one Brimhaven voucher, as the wiki
gives; the captured tags paid eight vouchers. The arrow moves every 98 ticks to
any other dispenser at random, and moves on its own clock from server start.
Logging out in the arena puts the player in the entrance hut on login, world
hops included. Leaving the arena any way starts the minute's wait, which is
checked when the ladder is clicked rather than cleared by a timer. Cap'n Izzy
does not answer what the arena is or what to do in it, nor the skillcape
question below 99 Agility, and the parrot answers a solved challenge scroll
without handing over the trail's next clue. The wiki says only that tagging too
quickly after a world hop locks the dispensers for 60 seconds: here the first
tag after a login that comes within a minute of the last tag, on any world,
locks them for a minute, a relog included, with a guessed message. The cache's
overlay showing whether the last pillar was tagged is not opened, and the old
two-in-a-row rule is not built, as no capture since the 2024 rework opens the
one or needs the other. Pirate Jackie hands out only the easy Karamja diary
reward and lacks the rest of her diary menu.

## Looting bag

The looting bag only opens and closes. Check, Deposit and Settings, filling it
in the Wilderness, banking its contents and dropping them on death are not
built; only the Agility dispenser puts anything in it.

## Agility Pyramid timing

The pyramid's stairs, doorway and ledges start a tick later than live, which
moves the player the tick after they arrive; here, as on every course, an
obstacle starts two ticks after arrival. The stone blocks do match live: they
tilt the tick after the player stops on the row before them. The low wall is
climbed without the loc merging into the player's model as live sends it. Only
one direction of each obstacle is captured; low walls, gaps, ledges, planks and
hand-hold gaps cross back by mirroring it, a ledge or hand-hold gap with the
other side's seqs, and pay the same xp. The stairs down land where the flight
below is climbed from, which is a guess. `agility_pyramid_level` clears when the
player stands on the ground of the pyramid's map square, as captured once, so a
teleport off the pyramid leaves it set until then.

## Agility Pyramid climbing rocks

The climbing rocks down to Simon Templeton and on to the pyramid are climbed
west to east as captured; the climb back up mirrors the climb down with the same
seq, which no capture shows. The second rocks need the wiki's 30 Agility both
ways and pay the captured 5 xp each time; the first pay nothing. Their
placements away from the pyramid answer that they can't be used from there.

## Agility Pyramid falls

No pyramid obstacle fails, as no fall has been captured: the wiki gives fail
rates down to level 75 but no landing. Stepping onto a stone block from its far
side does nothing, where live always drops the player a floor for 1 damage.
Desert heat is not built, so the pyramid never makes the player thirsty.

## Pyramid blocks

The floor 1 block slides out every 15 ticks and the floor 3 block every 13, as
captured live; the wiki says 15 for both. Their phase follows the world clock
rather than their spawn. A block that moves onto a player pushes them two tiles
on and a floor down for 8 damage with the cache's push seqs and no message; the
wiki gives only the floor and the damage. A player in the middle of an obstacle
is not pushed.

## Pyramid top

The climbing rocks give the top whether or not the player is on a lap, once
until the doorway puts it back. A player with a full inventory is refused with a
guessed message and the top waits for them, and a second climb in a lap only
plays the climb. The climb completes the Desert diary's medium summit task with
the captured message, its task bit and count; nothing else of the diary is
built.

## Simon Templeton

Simon's introduction and his line before it are as captured; his other lines
follow the wiki's transcript with guessed mesanims. He buys Pyramid Plunder
artefacts at the wiki's prices, the lot through his chat (noted ones too after
the easy Desert diary) or one at a time when used on him, and turns down a
pharaoh's sceptre as the transcript reads. A player carrying both artefacts and
pyramid tops is offered the artefacts first, and a noted artefact used on him
gets "Nothing interesting happens.", both guesses. Which of his varbits picks
his greeting is a guess (the job one). A sale whose coins would not fit is
refused with a guessed message and takes nothing.

## Dorgesh-Kaan falls and broken parts

On live the cables and the fallen ladders can fail at any level, dropping the
player to the cavern floor for about ten damage and breaking any part they
carry. No fall has been captured, so these obstacles never fail here. A part
carried onto the route it cannot cross (a heavy part onto a pylon, a delicate
one onto the Agility route) makes the player fall on live; here it turns into
its broken part as the obstacle starts and the player crosses as usual, with no
message. The server has no hook after a teleport, so a jump of more than 16
tiles in one tick out of the caves' map square counts as one and breaks every
part the next tick, silently; a teleport from elsewhere after walking out with a
part leaves it whole. One pylon swing in 25 destroys the worn mithril grapple as
the player lands, with a guessed message; the wiki does not say when in the
swing it goes.

## Dorgesh-Kaan laps

A lap ends when Turgall takes one of the two parts he asked for. No capture
reaches the hand-in, so the course name in the lap count message is unverified,
and the client's lap timer scripts are never run, as on the captured lap. The
bonus is paid for each way across by the route taken, as the wiki's lap rates
show: the route out is the one the player last crossed before taking the part,
the route back is the part's own. Turgall's reply about the crossing follows the
route back. The old generator gives one part at a time, and a part only counts
once a new one has been taken since the last hand-in, so spare parts carried
back bring no extra laps. Turgall gives a spanner whenever the player agrees to
help without carrying one. Every obstacle needs a lit light source from the
firemaking table, carried or worn, and no follower; both refusals are guessed.

## Dorgesh-Kaan pylons

A pylon swing starts from up to ten tiles away, as live, and walks the player to
the swing tile a tile a tick in a straight line, where live paths around the
cave. The pylons need 70 Ranged and 70 Strength as the wiki says; the refusal's
wording is guessed. Only one way over each pylon south of the northern one is
captured; the other way, and the whole northbound Agility route, mirror the
captured lap.

## Agility shortcut timing

Each Agility shortcut plays its movement type's template. The captured ones play
as captured: the Troll Stronghold, Karamja, Death Plateau, Trollheim, Aldarin,
Ice Mountain, Shilo Village, Waterbirth, Morytania and Fossil Island rocks, the
pipes, the Varrock and Yanille wall tunnels, the Royal Titans and wyvern
tunnels, the Catacombs crack, the Zanaris jutting walls, the Canifis and
Mudskipper Point railings, the Fossil Island hole, the Dagannoth crevice, the
Rellekka, Barrows and Burgh de Rott hurdles, the Auburn Valley, Falador and
Burgh de Rott walls, the lava maze, Lumbridge Swamp Caves, Shilo river,
Necropolis, Rellekka and Nature Grotto stones, the Mort Myre bridge, the
Ardougne and Karamja logs, the Yanille dungeon ledge, bars and rubble, the
Slayer Tower and Chasm of Fire chains, the Chasm of Fire platform edge, the
Ectofuntus wall and the Karamja jungle grapple. Every other shortcut plays its
type's shape untimed, and the remaining hurdles, the Brimhaven and Kourend
tunnels, floor-to-floor climbs and drops borrow captured or course moves. Which
side of an uncaptured rock face is climbed up is guessed wherever the locs'
names do not say.

Live starts several shortcuts one tick after the player arrives, and walks the
Death Plateau rocks over before the player is even marked busy; here, as every
obstacle, they start two ticks after. The logs are walked two tiles a tick, as
captured with run on. Live merges the player into the pipe and wall models while
they cross; here the client draws only the glide. Live hides the minimap and
fades the screen in the Royal Titans and wyvern tunnels; here only the message
boxes show, and the player stands up four ticks after the Royal Titans tunnel
where live waits for them to click the last box. The Shilo Village rocks glide
the whole way down where live glides three tiles and steps the last, and the
Waterbirth rocks are climbed a tile a tick where live is seen two tiles a tick.
One click of a Lumbridge Swamp Caves stone leaps both stones, as every
normal-world capture shows.

## Agility shortcut xp

Shortcuts pay the wiki's xp on landing, or the captured xp where a capture shows
it: 1 for the Karamja and Troll Stronghold rocks, 10 for the Barbarian Outpost
pipe and the Dagannoth crevice, 7.5 for the Yanille dungeon pipe, 20 for its
bars, 22 for its ledge and 6 for climbing its rubble up, 8 for each Trollheim
rock, 3 for each Slayer Tower chain, 5 for the Chasm of Fire chain, 4 for the
Karamja log, 15 for the Mort Myre bridge and 0.5 for the Burgh de Rott inn wall.
Shortcuts sharing a row pay their own xp. The Chasm of Fire platform edge,
captured only on a tournament world, pays what its gaps pay.

## Agility shortcut failures

The shortcuts whose wiki page charts the success chance fail on curves fitted to
those charts: the Lumbridge Swamp Caves and Karamja stepping stones, the East
Ardougne and Sinclair Mansion logs, the Zanaris jutting walls and the Taverley
Dungeon pipe and strange floor. The Karamja rocks, the Trollheim rocks, the
Death Plateau rocks and the Mort Myre bridge fail as captured, on curves drawn
through the few captured attempts. A rock climbed up slides the player back to
its foot, a Death Plateau rock leaves them where they stood and the bridge drops
them in the swamp between its halves. Only the way up a rock fails, as only it
is captured failing. Every other failure stumbles on the near side, except at a
Zanaris wall, which the wiki says still lets the player through. Live also fails
the Karamja log, washing the player down the river, and the Death Plateau path's
top and bottom rocks; neither is built. The Forthos strange floor, which the
wiki says can fail without giving the chance, never fails here, and a failed
Lumbridge Swamp Caves jump leaves the player's light lit.

## Agility shortcut gates

Shortcuts check their Agility level with boosts counting, and the gates the
Shortest Path data names: quests through the quest requirement setting, the Lost
Tribe, Falador hard diary, Darkmeyer and In Aid of the Myreque varbits, Death
Plateau's climbing boots and the Karamja jungle tree's crossbow and mithril
grapple with its Ranged and Strength levels. The Legends' Quest vine and jagged
wall need the quest done, where live opens them part way through it, and the Al
Kharid palace wall needs Prince Ali Rescue as the wiki's table says. A level
refusal is the captured message box; every other refusal's wording is guessed,
and the wyvern tunnel's own refusal is not used. Each Brimhaven Dungeon pipe
needs its level one way only, as the wiki says; which way is guessed. The
Karamja log, the Mort Myre bridge and the Burgh de Rott inn wall have no
captured level, so they share rows of level 1 and 5. A shortcut loc placed where
the Shortest Path data has no crossing answers that it can't be used from there.
Karamja's middle stepping stone has no op in the cache, so the stones are
crossed one at a time from either bank. The Karamja jungle tree is grappled only
from the north shore to the island and from the island to the south shore, as
captured, and the Dagannoth crevice is one way, sliding a player back who tries
it from below.

## Agility shortcuts left to map transports

The swims to the Crossbow Tree and the Strong Trees, the Mourning's End Part II
blade traps and wall supports, the Mountain Daughter boulder, the Mausoleum
bridge, the Darkmeyer north-eastern wall, the Pollnivneach slope, the Death
Plateau climbing rocks, the Temple of the Eye rubble, the ropeswings by Moss
Giant Island, the Mos Le'Harmless stepping stone, the Revenant Caves pillar
jumps, the Lost Tribe's second hole and every shortcut another content script
already runs still teleport across with the level check and nothing else.

## Hallowed Sepulchre floors

Live gives each group of up to fifteen players its own copy of a floor, and
sells private copies for 200 marks. Here every player shares the one floor in
the static map, so the statues' cycles are shared too: a floor's traps start
three ticks after the first player arrives on an empty floor, as live's do for a
new copy, and a player arriving later joins them mid-cycle. Private access is
not sold. The lobby stairs keep out a player below 52 Agility and the floor
stairs one below the next floor's level with wording of our own. The south start
of floor 1, the north and south starts of floor 2 and the north start of floor 4
are worked out from where their return stairs face, as the captured starts are.
Floor 5's bounds are taken from the map.

## Hallowed Sepulchre timer

The timer warns at 2:30, 1:00, 0:30 and 0:10 on the ticks the capture does,
which are not evenly spaced: 0:10 comes at 20 ticks left. The best time over all
five floors is taken as the run's time spent on finishing floor 5, which no
capture shows. Leaving a floor by a teleport with time left sends the
closing-door message a tick later, as captured on earlier floors; floor 5 is
assumed to say the same.

## Hallowed Sepulchre obelisks

The cache has no god alignment, so the obelisk reads a list of our own: the
common god-aligned gear, with the hallowed tools counted as Saradomin's as the
wiki says. Items live counts and the list lacks are ignored. Another god's item
outweighs a Saradomin one worn with it, and it takes a tenth of full run energy;
the capture shows neither.

## Hallowed Sepulchre traps

Every wizard, knight, crossbowman and priest statue fires on the cycle the
captures show. Ten plane-2 wizard statues on floor 5 never attack in any capture
and have no fire beside them, so they stand idle. A sword hurts across its whole
three-by-three body wherever it passed that tick, and a bolt along the tiles it
crossed. Trap sounds go to every timed player within ten tiles, where live picks
them per player. Lightning strikes two ticks after its statue starts, as on
floor 4 and one floor 5 capture; the other floor 5 capture shows the strike and
its fade a tick later. A stun holds the player with the same freeze timer a
combat freeze uses.

## Hallowed Sepulchre strange tiles

Only 41 of the 393 strange tiles have a captured destination. The rest send the
player the floor's usual distance along their path, the median of its captured
tiles (five on floor 2, which has none), and land short of it when that tile is
blocked. A tile lights at a 1 in 25 chance each tick once six have passed since
it last did, close to the captured spread of gaps.

## Hallowed Sepulchre respawns

A trap hit sends the player back to the last checkpoint they stepped on: the
respawns the capture shows, plus each drop's and path stairs' landing. Before
any checkpoint they go back to their floor start. On the paths no capture walks
this is likely further back than live.

## Hallowed Sepulchre treasure

The floor 1 bridge, the floor 3 east grapple, the floor 3 and floor 5 portals
and the floor 5 grapple are captured; every other bridge, grapple swing, portal
and holy barrier crossing is laid out from the map. Each crossing pays its
floor's captured treasure xp, the holy barrier's too, though no capture passes
one. The portal pays the wiki's 200 Magic xp: the only captured casts paid twice
that, as the brazier and the steel nails did at the same rates where Agility and
Thieving paid the wiki's. It needs no runes and never breaks. A bridge pays by
its nails alone, from the wiki for all but the captured steel nails. The holy
barrier takes five ticks or four with the symbol, as the wiki says. Floor 4
always opens its brazier and bridge, as its one capture did, and its portal
stays shut.

## Hallowed Sepulchre coffins

A coffin's odds rise in a straight line from the wiki's 41.8% at 66 Thieving to
74.6% at 99, with the lockpicks' bonuses on top. A failed search poisons for 4
with wording of our own, and the sealed-floor refusal borrows the brazier's.
Which loot is noted is a guess, and live's ghostly knights and the Archpriest
are not built. The loot goes straight to the inventory, or the floor when it is
full, without live's drop broadcast.

## Mysterious Stranger

The Stranger's item retrieval, private access and his story are not built; his
greeting on later talks is the wiki's. The first talk is captured in full, and
gives the five crystal shards only once. His tool storage takes and gives only
what fits and stops quietly at each tool's limit; it has no Banknotes option,
and the wording of a full inventory is ours.

## Fossil Island underwater currents

Live keeps the current tiles server side; the cache only has the stream locs,
which do not match where players are pushed. The currents here push only from
the tiles a player was seen pushed from, so most of a stream lets a player swim
straight through. A push glides the player with the stream as live does, but it
turns them to face the stream and ends any walk in progress, where live leaves
both alone.

## Fossil Island underwater water and gear

The sea floor always shows the shallow water overlay; live switches to the deep
one over parts of the cave by a rule we don't know. Wearing the fishbowl helmet
with the diving apparatus keeps the breath full anywhere on the sea floor, so
the cracks, tunnel and plant door turn back anyone who carries the helmet, with
the line Mairin says about it. How live keeps the helmet out of the deeper water
is not captured.

## Fossil Island underwater searches

One chest or clam opens every 103 ticks for a hundred, picked at random from
every closed one on the sea floor; two others twitch as decoys. A failed search
costs breath two times in five, as the captures roughly show. Searching with no
room for the tears refuses with our own message and pays nothing. Mairin's
training uses the real levels, and the second op of its buttons exchanges five
tears; neither is captured. A drowned player who carries no tears is told
nothing about the payment.

[magic-numbers]: https://en.wikipedia.org/wiki/Magic_number_(programming)
