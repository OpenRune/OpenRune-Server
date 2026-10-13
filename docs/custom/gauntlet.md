# Gauntlet integration and source decision

The owner selected the Gauntlet after the completionist head correction, with
cape environment lighting explicitly parked. Revision 240 and the accepted
server/client/cache combination remain the baseline. This is a local test
candidate, not an installed or accepted 100% OSRS implementation.

## Upstream scope

Reviewed [OpenRune PR #302](https://github.com/OpenRune/OpenRune-Server/pull/302),
merged 2026-10-09. Source head:
`bedaf18fe06d345d84afd62632f7b69fcde3cec3`, base:
`83338ee70e815e4d6f0b09aba03cac819d33dfe2`.
The review includes code, PR files, available review metadata and upstream checks.
Only the Gauntlet module/pack and necessary shared consumers are ported. The
PR's unrelated quest, Barrows, God Wars, spade and resource changes are excluded.
Existing licensing remains in place.

The port follows this fork's Boss DSL, private instances, native player death,
inventory transactions, reward tables and injected Collection Log. Existing
consumable activity restrictions, Moons supplies, accepted StyleImmunity XP
handling, loot/news rules and owner-selected home catalogue remain intact.

## Implemented cycle

- Normal and Corrupted solo runs, random 7x7 rooms, twelve native template
  layouts, room revealing/rebuilds and 10-minute / 7:30 preparation limits.
- Tools, three-yield resources, paddlefish gathering/cooking, Egniol, singing
  recipes and tier 1/2/3 armour and all three weapon styles.
- Native Crystalline/Corrupted Hunllef: 600/1000 HP, ranged/magic, four-cast
  style cycle, stomp, six off-prayer attacks including misses, prayer disabling,
  tornadoes, floor patterns and escalating health stages.
- Damage uses current armour and player prayer on impact. The sixth player
  attack lands before NPC protection changes. Native immunity suppresses XP
  for the protected style. Pending attacks are rejected after leaving the run.
  Queued projectiles retain their armour/prayer calculation across a protection
  transmog and are cancelled when their owned actor disappears; native queue
  producer/consumer tests cover both cases.
- Native chest tables, mode completion counts, injected Collection Log and
  native pet delivery. Pet delivery owns its obtain log; no duplicate grant.
- Equipment escrow retains exact slots, counts and inventoryvars in a permanent
  native inventory. Leave/death/logout and login recovery restore original gear.
  Failed entry removes the created instance and recovers escrow. Other teleports
  cannot move an active run outside its owned lifecycle.

## Corrections and deliberate policy

Preparation kills/crafting no longer qualify a failed-run chest. Failed rewards
depend on actual Hunllef HP lost; zero damage does not improve the tier. This
fork uses 1-49 damage for junk and 50+ for incomplete rewards. The Wiki describes
[minimal and partial boss damage](https://oldschool.runescape.wiki/w/Corrupted_gauntlet_rewards)
but does not establish that numeric boundary, so 50 is a server policy, not a
claimed exact OSRS formula.

Recipe costs use the previous-tier item and atomic input/output. Failed output
awards no XP or points. Mining awards 10 base XP, matching the
[Corrupt Deposit reference](https://oldschool.runescape.wiki/w/Corrupt_Deposit),
instead of upstream's 100. The fork's separate XP modifiers still apply.
Woodcutting fixed-yield state is scoped to the current region and does not carry
the previous private run's partial tree count into another instance.

The pending reward tier/flag are permanent. The original native chest-display
varbit is mirrored at award, claim and login. Reward objects/counts/conditions
resolve before the pending flag is consumed, and claim rechecks ownership after
the opening delay. Prayer-disable cooldown persists through health phases while
ordinary boss cooldowns retain their previous reset behavior.

Upstream's first weak-monster frame guarantee, subsequent 1/4 weak rolls,
2/7 strong rolls with a third-kill guarantee, and unique demi-component sequence
are retained pending independent probability verification. Floor pattern assets
and cadence are ported from the reviewed implementation. Full visual/timing
parity, the personal scoreboard/time records and global Combat Achievements
remain review work; a module and successful build do not prove those features.
Entry follows this server's quest-free travel policy. No Song of the Elves quest
or originating quest progression is added by this module.

## Bounded cache and validation

`:or-cache:buildGauntletCache` overlays an existing accepted cache. It does not
download another revision or rebuild unrelated maps/CS2/packs. It writes only
the owned Gauntlet config IDs, native escrow scope and necessary food/potion/
cooking/mining table definitions. Original signed varp file -1 and unrelated
definitions are checked after reopening. Native DB gamevals are merged for
table/row groups only; enum and table codegen use the normal pipeline.

Executed validation: **154 tests**, including **42 Gauntlet**, 24 administrator
commands, 68 Boss DSL, 17 drops and 3 cooking tests. Producer/consumer cases use
native NPC hit queuing/processing, inventory item-on-item transactions and the
registered command dispatcher. Coverage includes sixth-hit order, misses,
four-cast/stomp counting, prayer penetration, phase cooldowns, full inventories,
rollback, exact escrow/save-load, pending chest save-load, duplicate claims,
reward source, and testloot state preservation. Instances/consumables/mining/
woodcutting `test` tasks have no registered unit tests; they are not counted.
The existing Woodcutting integration directory is not automatically executed.

The runtime JAR builds. Cache packing preserves over 538,000 unrelated config
definitions. All fourteen accepted checkout source hashes and the accepted live
JAR remain unchanged. The qualified cape runtime and original folded Dizana inventory shape are
included; v0.17 corrects its icon colours while retaining v0.16 bytecode.

## Owner test route

Package: `outputs/gauntlet-cape-v017-dizana-colours-20261013/TESTEN.cmd`. It uses a separate
database/server/client profile and retains the guarded install/rollback path.
The package's validation report records exact hashes and isolated boot evidence.
It is not installed automatically.

- `::testgauntlet` visits the Prifddinas entrance or leaves your own Gauntlet run.
- `::gauntletkit` / `::gauntletkithm` start an equipped normal/corrupted run.
- `::testloot gauntlet [1-1000]` / `::testloot cg [1-1000]` use the real chest
  tables and obtain-log route. Default is 100. They do not simulate completion
  count or consume a pending chest or an active run's escrow.
- Check a complete preparation run, all resources/recipes, timer and boss door;
  then win, claim, replay, die, leave and relog. Verify original gear and pending
  chest recovery. Confirm both modes' native interfaces, maps, animations and
  safe floor/tornado timing in the actual client before acceptance.
- Cape checks: `::compcapes hoods`, `::compcapes dizana`, `::compcapes dizana_locked`
  and the Fire/Infernal variants, both genders, movement, software and HD.

Current status and remaining work belong in [PROGRESS.md](../../PROGRESS.md).
