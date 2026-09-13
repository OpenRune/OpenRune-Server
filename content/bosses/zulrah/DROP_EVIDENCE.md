# Zulrah loot

Checked 2026-09-13. Uses the existing Kotlin drop-table DSL and native NPC death
kill hooks, ground-item repository, collection-log and loot-tracker calls.

## Sources

- [Zulrah, Wiki revision 15342915](https://oldschool.runescape.wiki/w/Zulrah?oldid=15342915),
  timestamp 2026-09-13T18:12:43Z. Retrieved directly through the Wiki API; search
  snippets still showed obsolete rune quantities and a /248 pool.
- [Shark drop table](https://oldschool.runescape.wiki/w/Shark_drop_table?oldid=14997272).
  The 2025 change is present in the current Zulrah table. Fractional outer weights
  are represented as a 12/249 table access followed by integer weights 3:3:2.
- [Rare drop table](https://oldschool.runescape.wiki/w/Rare_drop_table?oldid=15339986).
  Local Zulrah rare/gem/mega-rare variants correct the generated shared tables'
  key-half weights and account for the source map's nature talisman and all 12
  cached ring-of-wealth variants. Other monsters' tables are unchanged.
- [RSProx recording 3276](https://rsprox.net/download/3276), revision 234.1,
  Grid Master (not normal-world proof): tick 9388 contains three OBJ_ADD_SPECIFIC
  packets at the player's loot tile: 256 scales, 12 noted dragon bones and one
  snapdragon seed. Each has reveal=100, despawn=18000 and ownership=self.
  This establishes that recording's lifetime/placement, not its random drop odds.
- [Instance](https://oldschool.runescape.wiki/w/Instance?oldid=15315896) documents
  extended three-hour ground-item lifetimes in boss instances, while noting this
  is not universal. Zulrah's 2023 change extended its loot lifetime.

## Roll structure

- One guaranteed scale drop, quantity 100..299 inclusive.
- Two independent loot rolls. Each selects uniques at 1/256, otherwise the
  249-weight common table; the four unique items are equally weighted. A unique
  replaces that roll, rather than adding a third common/unique result.
- Common table weights and quantities match the current Wiki. Its printed common
  rates describe the common pool; runtime access also requires the unique miss.
  The Wiki does not expose original server code. The replacement interpretation
  follows its two-roll description rather than pretending the table is recovered code.
- Flax is a nested 5264-weight table: 5244 flax, 10 tanzanite mutagen, 10 magma
  mutagen. A mutagen replaces flax and is not an independent tertiary roll.
- Jar: one independent 1/3000 roll per kill. Brimstone key: one native conditional
  roll (1/50 at combat level 725), requiring the existing Konar task checks.
- Duplicate item outcomes use native ground-stack merging, with both quantities
  retained. A rare-table empty result can mean fewer visible loot stacks.

## Death and ownership

After the existing six-tick death animation, a valid surviving owner receives
loot on their current tile, beside the exit object. This is resolved before NPC
deletion while native hero attribution is still available. Finished state guards
against duplicate payouts. An abandoned/destroyed instance or dead owner cannot
receive a delayed reward.

`NpcDeath.spawnDrops` now accepts optional remains suppression and duration;
defaults retain ordinary NPC behavior. Zulrah suppresses the generic bones
fallback and passes 18000 ticks through the kill context to the normal drop hook.
Native OBJ_ADD rendering is retained; the engine does not yet reproduce all
OBJ_ADD_SPECIFIC client timer metadata. Server-side expiration is three hours.
Leftover reward-tile ground items are deleted on instance teardown.

## Deliberately unavailable

Pet award/following/duplicate protection and native elite-clue generation are not
implemented. Their documented rates (1/4000 pet; 1/75 clue, or 1/71 with claimed
elite combat rewards) remain recorded here but are not enabled as loose pet items,
a repeated fixed clue, or universally boosted rewards. The existing clue helper
unconditionally grants combat-tier bonuses; this table does not use that stub.
These supporting systems must be completed before those rewards can be enabled.

Quest gating on the gem-to-mega-rare branch uses the project's quest requirement
policy, including its configured assume-completed mode. Respect-progress depends
on the wider Legends' Quest implementation. No extra reward/debug chat or test
command was added. Ring equipment and death-item retrieval are outside this
integration's scope.

## Verification

Tests cover table pool sizes, nested branches, replacement semantics, two rolls
versus one tertiary stage, quantities, cache symbols/certificates, charged wealth
rings and registration. Encounter tests exercise the real death queue and reward
hook boundary with a deterministic marker reward: delayed payout, owner tile,
native attribution, no generic bones, no duplicate payout, cancellation, and
independent-island cleanup. In-client pickup still needs a completed kill test.
