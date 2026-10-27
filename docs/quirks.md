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

Explicit watering spends a dose from the clicked inventory slot. If that slot no
longer holds the expected can type after the wait, the action does not substitute
another matching can, water the patch or award watering XP. Tithe item-use follows
the same selected-source rule; its ordinary left-click still selects a usable
carried can automatically.

Huasca requires level 65 and grows in four 20-minute stages. White lily requires
level 58 and grows in four five-minute stages. A mature, healthy white lily
protects neighbouring allotments, not herbs or itself, including during offline
catch-up. Its protection ends when picked. Cleared patches can be replanted
immediately; otherwise weeds can regrow.

Mature, healthy, checked calquat trees can be cleared with a spade. Clearing
discards remaining fruit without another harvest or XP award and leaves reusable
weeded soil. This supports existing saved crops; calquat planting remains held.

Prifddinas allotments, flowers and crystal trees accept native Song of the Elves
progress or the configured quest policy. Summer Shore calquat access accepts the
sourced partial Troubled Tortugans stage, not only quest completion. Anglers'
Retreat hardwood plots require base Sailing 51; temporary boosts and the optional
Construction/rowboat route do not satisfy or add a plot requirement.

Huasca also uses the existing Herblore production pipeline: level 58 to clean
([11.8 XP](https://oldschool.runescape.wiki/w/Huasca?oldid=15192709)) or make its
[unfinished potion](https://oldschool.runescape.wiki/w/Huasca_potion_%28unf%29?oldid=15192710)
(no XP), then one aldarium for a three-dose
[prayer regeneration potion](https://oldschool.runescape.wiki/w/Prayer_regeneration_potion?oldid=15324376)
(132 XP). Cleaning XP is packed in tenths; rebuild cache and deploy its consumers
together. Cleaning and unfinished mixing use atomic inventory transactions,
including when the input inventory is full.

Scarecrows use native hay-source and sack-fill actions, followed by spear/watermelon
assembly. Placement and removal conserve the scarecrow; protection applies only to
sweetcorn at the same site. A bounded client regression exercised the Varlamore hay
source, assembly for 25 base XP, and native flower-patch placement/removal.

The Amulet of Nature persists its bound patch and reports growth, disease, death
and maturity. Final-state alerts include the native Farming jingle; offline replay
reports the final status rather than replaying intermediate alerts. The client
regression observed the hum and MIDI-jingle packet after maturation, no repeated
alert on Inspect, and the same binding after actual logout/relog.

Bottomless buckets use the filled item's native Fill/Check operations, retain one
compost type and double each input bucket into two uses, up to 10,000 uses. Patch
treatment spends one use only when committed. The bounded client check consumed
two buckets of each tier and returned exactly six empty buckets, rejected mixed
types, spent no charge on repeated treatment, and preserved three charges through
leprechaun storage and actual logout/relog. Empty intentionally discards remaining
uses and resets the type. Capacity/overflow and stale-source boundaries have
deterministic regression coverage, not a live 10,000-use fill.

Ordinary compost's native Empty discards one bucketful and returns an empty bucket
in the clicked slot, even with a full inventory, without XP. Reference:
normal rev231 `20250620T124602-163cd92@4612`. The current regression and actual-handler
smoke cover conservation and stale repeats; no new client run or bin parity is claimed.
Supercompost and ultracompost also expose native Empty, but their Op4 handlers remain
unimplemented pending verified discard output and messages. Their visible options
do not imply support for those tiers.

The inherited disease model is still an unverified approximation: a 35% base
chance divided across growth stages, with compost/supercompost/ultracompost
reductions. Ultracompost does not confer immunity. The inherited watered-stage
bypass is unchanged WIP, not verified OSRS immunity. Do not extend this model to
held crop families or infer unpublished odds/timings from a successful local run.

Native Geomancy front/status panels use the real patch IDs, enums and clientscript.
Crop miniatures use native item models through the cached picture components,
including cleared-patch and spirit-tree model selection. A seeded client check
covered growing, dead, diseased and mature crop previews, exact spell payment and
same-save reopen. This is not normal acquisition/training or packet-parity proof.
Paid casts use the recorded animation, delayed graphic and area sound, then open
the interface four game ticks later. Failed rune payment does not play the cast
or open the interface.
Compost-bin status remains held without authoritative per-site contents, type and
processing state; a shared nearby transmit cannot represent all bins independently.

Tool and vault transfers use the shared inventory transaction path.
Leprechaun Deposit-all reports all, some or none from actual eligible tool
transfers, not unrelated carried produce. Produce noting commits atomically
before showing the original produce in the native banknote-exchange objectbox.
Bag-to-bank actions run with protected access through the native bank deposit API,
preserving tab sizes, placeholders and final-stack withdrawal behavior.
The current account model has no bank-PIN authority; seed-vault use follows the
existing no-PIN bank model, not a fabricated successful PIN check. Protected PIN
access remains unavailable.

Seed-box withdrawals validate the clicked seed against the stored slot before
prompting or transferring, then recheck the stored type after a quantity dialog.
A stale click after a reorder cannot withdraw a different seed.

Ordinary-tree gardeners use the recorded animated sapling refusal for an empty
patch and wait for Continue. Other patch-family refusals are unchanged. This does
not enable held tree crops or change protection payments or removal rules.

Vannah's Trade opens the native 17-row Farming Stall with ordinary coin pricing.
Deepwater Mairin's Trade opens her native six-row market using Mermaid tears,
not coins. It refuses every buyback, including tradeable spores and tridents.
This supplies the existing shop consumer, not underwater training or tear
acquisition.

Shops use the native Value and 1/5/10/50 controls for both Buy and Sell defaults.
Forced Value does not transfer items; explicit quantity options keep their fixed
amounts. Selection preserves the other native preference bits. The obsolete
instruction-text argument was removed: the current shop interface has native
value/quantity controls, not the old instruction component.

Agelus' Trade opens the existing 24-row Farm Shop. It charges 100% of base value,
pays 70%, and uses the native 1% stock adjustment through the shared coin shop.

Agelus and Vannah also provide their standard Talk-to conversations. Their stock
choices open the same shops as direct Trade; Vannah's information branch returns
to the remaining stock/exit choices. Native-client verification covers both complete
trees, unchanged direct Trade stocks, closing and logout/relogin preservation.
Production-coroutine smoke also covers Continue suspension, cancellation, reopening
and stale-modal guards. Dialogue wording comes from the standard-world wiki
transcripts, not an OSRS packet-parity claim.

Plant-pot packs return 100 noted filled plant pots through the existing generic
queued pack opener, not empty pots. Deploy the rebuilt cache with this correction.
Actual pack/queue regressions cover full inventory, overflow rollback, stale input
and repeated requests without changing the generic opening engine.
Native verification consumed one approved pack and preserved all 100 filled notes,
empty worn inventory and unchanged skills through logout/save/relogin. The fresh
normal 1× actor used two approved merchant relocations and no skill presets; this
does not prove normal supply acquisition.

Bird nests now fall from successful non-bonus woodcutting products and can be
searched through native Search. Ordinary seed, Wyson seed, ring and coloured egg
nests atomically become one empty nest in the original slot plus their reward.
Full-inventory and stack-overflow failures leave the nest untouched. Discontinued
seed variants and clue nests are not handled.

Seed/Wyson/ring weights use the packed, cutoff-pinned wiki Drop Rate Project
estimates, not developer-exact odds. Woodcutting uses the published 1/256 per-log
chance, a 10% Woodcutting-cape increase and the strung-rabbit-foot type range.
Redwood and rosewood are excluded from these non-clue drops. The owned ground
nest lasts 200 ticks; red chat, sound 1516 and the native notification follow.
Shared ground-item reveal policy is unchanged: the repository schedules reveal
after 100 ticks, not the recorded never-public flag. This is not ownership parity.
Willow's recorded "a willow tree seed" noun is cache-backed: its generic cached
examine text cannot supply that wording.

Auto-queue reuses the generic pack opener's queue delay of 2 and lowest-slot-first
selection; the engine can decrement a newly queued entry during the same cycle.
Exact OSRS cadence/order/interruption and ring/egg wording remain unobserved.
Adjacent-tile placement excludes blocked tiles, also an unobserved selection rule.
These labelled shortcuts and Wyson nest-box exchange/Check/Extract need recording
request `58a3209d-d5a0-44f4-88bc-ee6638cf095e`. Wyson exchange and the wider seed-supply
tracker row remain incomplete.

The real-cache production smoke reproduces the recorded willow/acorn messages,
same-slot replacement, queued conservation, rollback and actual repository drops.
Native evidence is partial/unaccepted: decoded packets show same-slot replacements,
sound/notification and a forced-drop pickup/Search, but there is no screenshot,
immediate-walk proof or persistence check. The worker's second disposable actor
repeated supplies without authorization; that contamination is retained in local
run `harness-ff2f5b7f-0249-479f-908d-841561b65f2f`, not relabelled as an accepted pass.
Both runs' resources were released and the forced-drop fixture was removed.

Celastrus bark uses the existing knife-production engine: current Fletching 40,
one bark per battlestaff, 80 base Fletching XP and no Crafting XP. Native Fletch
and knife-on-bark share the quantity flow and commit checks. Ordinary knives take
four ticks per staff; a carried or offhand fletching knife takes four ticks for
the first and three thereafter. Tools and current skill requirements are checked
again before each conversion. This does not enable celastrus growing or harvesting.

Jane's complete native contract pool is retained without filtering or reweighting.
Assignment is not offered until every crop in that pool is playable, preventing
uncancellable contracts for held crops. Guild information, seed exchanges and
ordinary seed-pack handling remain separate.

Guild tier gates use current, boosted Farming at 45/65/85. Main-door diary credit
comes only from committed walking/running across the entrance, not validation,
teleport placement or login. Allanna and the Guild Garden supplier have separate
native trade bindings; the Guild Garden stock is separate from Falador's Garden
Centre. Felfiz provides anima information. Advanced-wing logout relocates collision
and zone membership along with the saved position before registry removal.
Gardener refusals and payment prompts use unanimated NPC chat with the same
page/Continue flow as animated dialogue.
Skills-necklace Guild transport remains held: native options and charge variants
are sourced, but exact arrival bounds/distribution and its base-versus-current
Farming threshold are not.

Otto can teach pot smashing after barehand planting is already learned and base
Farming is at least 15. A committed sapling planting advances the trial; report
back to finish it and enable the reversible automatic-smashing toggle. This does
not complete the global Barbarian miniquest. The earlier barehand-learning trial
remains held rather than inventing its failure odds.

Twenty-two sourced Farming diary task flags are cache-backed and follow committed
actions, including scarecrows, white-tree fruit, protection, Guild entry, Tithe
Logavano planting, Fertile Soil and Sarah's actual shop inventory. Repeats preserve
neighbouring flags. Task counters, tier rewards and completion authority remain
with the broader diary system; these handlers do not manufacture them. Root and
full-kingdom-approval tasks remain held with their missing producers. The
celastrus-battlestaff task remains held until Farming Guild bark provenance is
established; processing supplied bark does not award it. Occupied-slot leprechaun
tool replacement and kingdom weed regrowth/ownership rules also remain unresolved.

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
