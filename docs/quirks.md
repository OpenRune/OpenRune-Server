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

## Farming

The full-skill continuation is WIP. Definitions are cache-backed; rebuild the cache
after changing the farming pack. Metadata coverage is not playable-content coverage:

| Coverage | Count |
|---|---:|
| Permanent crop definitions | 82 |
| Currently enabled crops | 33 |
| Normal-world patches | 99 |
| Patch sites | 62 |

Enabled crops are the original allotment/flower/herb set plus goutweed, grape vines
and Attas/Kronos. Other crop definitions remain addressable for saved state and consumers,
but planting is unavailable when disease, death, yield or other required rules are
held. The [Farming tracker](https://github.com/OpenRune/OpenRune-Server/issues/61#issuecomment-6390841276)
records cutoff-pinned wiki sources, normal-world recording evidence and unresolved
rules. Published models are not claims of observed OSRS packet parity.

Artwork uses named locs and native transform tables. Snape grass retains explicit
state banks for its scattered artwork; grape vines bind all twelve native click
zones. Duplicate artwork uses the first matching selector. Watered and dead bases
are cache columns, not runtime offset constants.

Crop declaration order is stored in `crop_index`, independently of numeric DBROW
IDs. Indices 1–27 retain their original crops; Huasca and white lily remain at
28 and 29. New crops append rather than renumbering existing saves. Original patch
varps and the saved clock are retained. New flags require the version marker;
legacy countdown bits are not reinterpreted as protection or special-patch flags.

The pre-merge WIP build `df7d211e6` used IDs now occupied on main. Additional
patches 40–42 and 61–65 now use varps 64490–64497; the herb-sack inventory is 64402.
These preview-save values require explicit migration before reuse. Legacy patch
IDs are unchanged. The matching herb enum and Tithe timer were also rebound;
rebuild with `OPENRUNE_FULL_REBUILD=true` after this reassignment. Incremental
cleanup can otherwise remove cache entries whose old IDs have been reused.

Growth uses globally aligned minute windows and a saved per-player offset in
0–31, including offline replay. Long stages no longer depend on the old narrow
per-patch countdown. Replay computes protection before each growth mutation and
publishes one final refresh rather than intermediate offline notifications.

Kronos uses an explicitly approximate **15%** skip rate from
[limited player observations](https://oldschool.runescape.wiki/w/Talk:Kronos_seed#Chance_to_skip_growth_stage),
not confirmed OSRS odds. The rate is cache-backed in `growth_skip_percent`.
Each window shares repeated skip rolls across patches on the same growth cycle,
checking disease at every extra stage and stopping at disease, maturity or full
produce capacity. Kronos never skips itself. Its effect and flower protection use
the state at the start of the window, including its final window before death;
subsequent windows receive no expired-anima effect. Offline replay uses this same
path. This enables Kronos, not the other held crop families.

A bounded live-client regression covered real seed planting (100 base XP), a
companion herb's two-stage advance, native growth/withering/dead states, clearing
without a second XP award, and logout/relog preserving both planted patches,
offset, inventory and skills. It used saved patch/clock fixtures and the inherited
150× XP rate, not natural acquisition, elapsed multi-day growth or OSRS packet parity.

Legacy allotments and herbs track harvest lives, not a fixed item count. Farming
level and eligible equipment affect whether a pick spends a life. Flowers give one
batch: woad yields three leaves, while limpwurt quantity is level/modifier-dependent.
Legacy planting XP is deferred until final harvest or dead clearing. Ranarr uses
26.5 planting XP and 30.5 per herb. Produce capacity is checked before reward RNG;
inventory, patch state and completion rewards must not partially commit.

Huasca requires level 65 and grows in four 20-minute stages. White lily requires
level 58 and grows in four five-minute stages. A mature, healthy white lily
protects neighbouring allotments, not herbs or itself, including during offline
catch-up. Its protection ends when picked. Cleared patches can be replanted
immediately; otherwise weeds can regrow.

Huasca also uses the existing Herblore production pipeline: level 58 to clean
([11.8 XP](https://oldschool.runescape.wiki/w/Huasca?oldid=15192709)) or make its
[unfinished potion](https://oldschool.runescape.wiki/w/Huasca_potion_%28unf%29?oldid=15192710)
(no XP), then one aldarium for a three-dose
[prayer regeneration potion](https://oldschool.runescape.wiki/w/Prayer_regeneration_potion?oldid=15324376)
(132 XP). Cleaning XP is packed in tenths; rebuild cache and deploy its consumers
together. Cleaning and unfinished mixing use atomic inventory transactions,
including when the input inventory is full.

The inherited disease model is still an unverified approximation: a 35% base
chance divided across growth stages, with compost/supercompost/ultracompost
reductions. Ultracompost does not confer immunity. The inherited watered-stage
bypass is unchanged WIP, not verified OSRS immunity. Do not extend this model to
held crop families or infer unpublished odds/timings from a successful local run.

Native Geomancy front/status panels use the real patch IDs, enums and clientscript.
Crop miniatures and compost-bin status are held rather than substituted with item
icons. Tool and vault transfers use the shared inventory transaction path.
Bag-to-bank actions run with protected access through the native bank deposit API,
preserving tab sizes, placeholders and final-stack withdrawal behavior.
The current account model has no bank-PIN authority; seed-vault use follows the
existing no-PIN bank model, not a fabricated successful PIN check. Protected PIN
access remains unavailable.

Jane's complete native contract pool is retained without filtering or reweighting.
Assignment is not offered until every crop in that pool is playable, preventing
uncancellable contracts for held crops. Guild information, seed exchanges and
ordinary seed-pack handling remain separate.

Other explicit holds include unpublished new-crop disease/yield/pruning
rules, contradictory compost-bin processing rules, empty-basket automatic Fill
priority, Tithe seed-pack white-lily weights, fresh Bologa unlock predicates,
Resurrect Crops spell eligibility/restored-stage details, the full Hespori fight
and death/reclaim flow, and missing raid/quest/kingdom contexts. Reagent-pouch bank
access covers the sourced supported-chest boundary, not a guessed universal bank
radius. These are unavailable features, not completed no-op handlers.

The reagent pouch's bankside action uses the native `if4` slot flags: `Fill` when
compatible carried ingredients are present, otherwise `Empty`. This check ignores
capacity; a full pouch can offer `Fill` without moving any items. Open/closed
variants and slot moves refresh the label without changing other bags' actions.

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

[magic-numbers]: https://en.wikipedia.org/wiki/Magic_number_(programming)
