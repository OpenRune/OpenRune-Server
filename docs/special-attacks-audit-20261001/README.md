# Current-cache weapon special attack inventory

Cache: revision 240. Before-change source baseline: `83c47b4e3fee910f87c8671cbdc4b5fddb856e4f`. Current registrations come from the postformat candidate test run on 2026-10-01, committed with this audit. Every entry in sa_energy_requirements is included; the CSV retains IDs, exact symbols, category, energy and the cache tooltip.

285 items; 29 explicitly registered before this task. 90 named tooltip families plus entries without a tooltip. Cosmetic, PvP, Leagues, charge and quest variants are not silently excluded.

Root cause: CombatTab looks up the equipped item in SpecialAttackRegistry. Cache metadata only exposes a button and energy cost; it does not create an attack. The base module bound only DragonLongswordSpecialAttack, DarkBowSpecialAttack and StatBoostSpecialAttacks. Unmapped items therefore display the misleading no-special message.

Shield note: Dragonfire shield, dragonfire ward and ancient wyvern shield are included as 0-energy cache entries. Their activation uses a separate shield path; that path contained TODO(). The candidate adds shield activation and equipped operations; limitations are stated below.

Current registration comes from the cache-backed test that reads the actual Guice SpecialAttackModule bindings and registers those implementations together in the real SpecialAttackRegistry. Its dependencies are mocked; server startup and gameplay effects are validated separately. The CSV includes exact handler class/kind, unresolved mechanics and qualification notes. Source references remain an auxiliary column only.

Registry rows measured: 285. Registered current-cache items: 137. Missing: 148. This does not mean every registered variant has complete OSRS parity.

Detailed mechanisms, unresolved families, special-boss exceptions, BH/BR limitations and evidence: [melee mechanisms and gaps](melee-specials-status.json) and [ranged/magic mechanisms and gaps](ranged-magic-specials-status.json). Shields implement basic charge/cooldown/operation behavior; ancient wyvern shield freeze chance/duration is not yet qualified or enabled.

## Evidence and reproduction

- [All 285 cache items and gap reasons](weapon-specials-coverage.csv).
- [All six equipped shield forms](weapon-shields-coverage.csv).
- [Unmodified cache metadata](special-attack-cache.tsv), [actual registry export](special-attack-coverage.tsv), and [shield registry export](shield-special-coverage.tsv).
- [Cache-backed registration and melee tests](../../content/other/special-attacks/src/test/kotlin/org/rsmod/content/other/special/attacks/melee/MeleeSpecialAttacksTest.kt).

The final run passed 32 special-attack tests: 15 melee/registry, 9 ranged/magic and 8 shield tests. The complete selected regression run passed 62 tests, and `:server:app:shadowJar` succeeded. This is cache-backed registration and handler-level evidence; no user-client combat demonstration is implied.

Run `./gradlew :content:other:special-attacks:test` from the repository root with the configured revision-240 test cache to regenerate `content/other/special-attacks/build/reports/special-attack-coverage.tsv` and `shield-special-coverage.tsv`. The CSV annotates those measured rows using the two JSON files; `candidate_source_reference` is only an auxiliary source-string check, not proof of registration. Baseline counts refer solely to baseline `83c47b4`, not the newly built server.

Current energy-enum registrations by kind: **61 Melee, 47 Instant, 21 Ranged, 5 Magic, 3 Shield**. The additional three uncharged shield forms are outside that enum.

| Family | Cache variants | Registered before | Registered now |
|---|---:|---:|---:|
| Abyssal Puncture | 8 | 0 | 8 |
| Annihilate | 3 | 0 | 0 |
| Armadyl Eye | 2 | 0 | 0 |
| Backstab | 4 | 0 | 0 |
| Bear Down | 1 | 0 | 0 |
| Behead | 2 | 0 | 0 |
| Binding Tentacle | 2 | 0 | 0 |
| Blood Sacrifice | 2 | 0 | 0 |
| Blood infusion | 2 | 0 | 0 |
| Brand | 1 | 0 | 0 |
| Break Shackles | 2 | 0 | 0 |
| Brutal Swing | 1 | 0 | 0 |
| Burning barrage | 2 | 0 | 0 |
| Celebrate | 1 | 0 | 0 |
| Chainhit | 1 | 0 | 0 |
| Cleave | 3 | 1 | 3 |
| Concentrated Shot | 5 | 0 | 5 |
| Condemn | 2 | 0 | 0 |
| Crystalline Severance | 1 | 0 | 0 |
| Descent of Darkness | 9 | 6 | 6 |
| Disrupt | 4 | 0 | 4 |
| Division | 2 | 0 | 0 |
| Duality | 5 | 0 | 5 |
| Echo slash | 1 | 0 | 0 |
| Eclipse | 2 | 0 | 0 |
| Energy Drain | 5 | 0 | 5 |
| Entice | 1 | 0 | 0 |
| Eviscerate | 3 | 0 | 2 |
| Evoke | 2 | 0 | 0 |
| Explosive Shatter | 1 | 0 | 0 |
| Favour of the War God | 1 | 0 | 0 |
| Feint | 4 | 0 | 0 |
| Ferocity | 1 | 0 | 0 |
| Fishstabber | 6 | 3 | 6 |
| Flames of Ralos | 1 | 0 | 0 |
| Hammer Blow | 1 | 0 | 1 |
| Hamstring | 2 | 0 | 0 |
| Healing Blade | 2 | 0 | 2 |
| Ice Cleave | 2 | 0 | 2 |
| Immolate | 4 | 0 | 4 |
| Impale | 1 | 0 | 0 |
| Invocate | 1 | 0 | 1 |
| Lightning Strike | 1 | 0 | 0 |
| Lingering Lightning | 1 | 0 | 0 |
| Liquify | 1 | 0 | 0 |
| Lumber Up | 11 | 4 | 11 |
| Momentum Throw | 2 | 0 | 0 |
| Penance | 1 | 0 | 1 |
| Phantom Strike | 3 | 0 | 0 |
| Power of Death | 8 | 0 | 0 |
| Power of the Gods | 1 | 0 | 0 |
| Powershot | 2 | 0 | 2 |
| Powerstab | 3 | 0 | 2 |
| Pulsate | 1 | 0 | 0 |
| Pulverize | 3 | 0 | 3 |
| Puncture | 9 | 0 | 9 |
| Quick Smash | 5 | 0 | 0 |
| Rampage | 2 | 0 | 2 |
| Rapid Burst | 1 | 0 | 0 |
| Retainer | 13 | 0 | 0 |
| Rock Knocker | 9 | 7 | 9 |
| Sanctuary | 1 | 0 | 1 |
| Saradomin's Lightning | 3 | 0 | 0 |
| Scatter ashes | 2 | 0 | 0 |
| Scorching shackles | 1 | 0 | 0 |
| Seeking Lunge | 1 | 0 | 0 |
| Sever | 4 | 0 | 0 |
| Shatter | 3 | 0 | 2 |
| Shield Bash | 2 | 0 | 0 |
| Shove | 11 | 0 | 0 |
| Slice and Dice | 5 | 0 | 0 |
| Smash | 7 | 0 | 4 |
| Snapshot | 3 | 0 | 2 |
| Snipe | 1 | 0 | 0 |
| Sol Slam | 1 | 0 | 0 |
| Soul Rend | 1 | 0 | 0 |
| Soulshot | 1 | 0 | 1 |
| Spear Wall | 2 | 0 | 0 |
| Sunder | 2 | 0 | 2 |
| Swarm | 1 | 0 | 0 |
| Sweep | 4 | 0 | 4 |
| The Judgement | 5 | 0 | 5 |
| Toxic Siphon | 2 | 0 | 0 |
| Tumeken's Light | 1 | 0 | 0 |
| Unleash | 5 | 0 | 0 |
| Virulence | 2 | 0 | 0 |
| Warstrike | 2 | 0 | 2 |
| Weaken | 4 | 0 | 0 |
| Wild Stab | 3 | 0 | 0 |
| Wrath of Amascut | 1 | 0 | 0 |
| null | 21 | 8 | 21 |

## Equipped shields

All six forms below are measured in the same real registry. The three charged forms are already part of the 285-row energy enum; the three uncharged forms are additional, for 288 distinct item IDs across both reports.

| Item ID | Symbol | Charged form | In energy enum | Registered |
|---:|---|---|---|---|
| 11283 | obj.dragonfire_shield | true | true | Shield |
| 11284 | obj.dragonfire_shield_uncharged | false | false | Shield |
| 22002 | obj.dragonfire_ward | true | true | Shield |
| 22003 | obj.dragonfire_ward_uncharged | false | false | Shield |
| 21633 | obj.wyvern_shield | true | true | Shield |
| 21634 | obj.wyvern_shield_uncharged | false | false | Shield |

## Qualification limits

- Area-target broadphase searches NPC southwest anchors before checking their full footprint. A large secondary NPC whose anchor is outside the search radius but whose footprint overlaps the area may be omitted. Primary targets and halberd large-primary double hits are unaffected.
- Halberd/dragon2h secondary area targets are NPC-only. Existing PvP hook Pass means not-vetoed, not positively eligible, so new area attacks do not use it to authorize damage to player bystanders. Primary PvP targets remain handled by the normal combat script.
- A registered handler and mapped cache symbol do not prove full live combat parity. Final registry report distinguishes registration from qualification.
- Battle Royale items decoded as Unarmed need separate normal-attack metadata/dispatch testing. Cosmetic model compatibility is not inferred from a name.
- BH/corrupted aliases have implemented special arithmetic, but a complete Bounty Hunter-only equipment restriction system is not present in this server.
- Special encounter exceptions, e.g. Tekton first-hit guarantee/failed-hit drain and boss stat floors, are not claimed implemented by these general weapon handlers.
- Delayed effects use source/target assignment and target UID validation; no live user-client combat was performed during unit qualification.
- The server's queued-hit API can modify NPC damage again at impact. Healing/stat drains currently use the queued hit's damage, matching other existing effects but not a guarantee of final post-script damage.
- Cache tooltip for MSB states reduced accuracy, while primary Wiki calculator implements 10/7 special accuracy multiplier.
- Ballista tooltip says extra 2.4 seconds, but current Wiki explicitly describes this tooltip as false. No extra delay was introduced.
- New handler unit tests use the actual installed 240 cache; they are not a live PvP/PvN client demonstration.

## Mechanics evidence

- Exact current cache sa_descriptions and sa_energy_requirements (all tooltip text and costs are in CSV).
- [Jagex Project Rebalance, 29 May 2024](https://secure.runescape.com/m=news/project-rebalance-combat-changes?oldschool=1): elder maul accuracy, current-defence reduction and energy cost.
- [Dragon longsword](https://oldschool.runescape.wiki/w/Dragon_longsword): standard Cleave has damage only; Bounty Hunter variant adds accuracy/speed. Current cache distinguishes these too.
- [Crystal halberd](https://oldschool.runescape.wiki/w/Crystal_halberd): Sweep, large-target second accuracy roll and 3x1 target area.
