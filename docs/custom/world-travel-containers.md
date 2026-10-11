# Native world travel and container decisions

Current status belongs to [PROGRESS.md](../../PROGRESS.md).

## Accepted base

Revenant Caves PR36 merged as `7f0fdc01f8429822778927af944f1dc53b6c64fb`.
Edgeville home PR37 was retargeted to main and merged as
`98822c1ef901783bd9aa6d4062eb2cc1fa81bbde`. The resulting gameplay tree
`483b592943b29e5272bb4c9021cc94ca61006c8e` equals the tested home candidate
`40224c542acb44b79bf49281fb25ad9fe50115af`. New work retains revision 240.

## Reviewed upstream

- [Agility PR227](https://github.com/OpenRune/OpenRune-Server/pull/227), open
  draft at `98fe5d9a0554b202ccd63492c326dbe65793243a`: selective shortcut
  data reuse; rooftop courses are outside this request. Runtime data initially
  yields 353 candidate links and 160 loc/option bindings. Native comparison
  exposes wrong symbols, non-clickable intermediate props, mismatched options
  and blocked landings. The upstream list alone is not sufficient coverage.
- [Content PR216](https://github.com/OpenRune/OpenRune-Server/pull/216), closed
  unmerged at `375581c32314973d7de6a04e5e7f3713ef429f22`: selected fairy-ring
  and rune/looting-pouch modules adapted to existing fork APIs. The wider branch
  is not imported. Death uses the fork's pure preview DropHook with deferred
  completion; the old pre-drop mutation hook is not introduced. Ground pickup
  receives a small optional storage hook after ownership validation and repeats
  validation before deleting the obj after a pickup delay.
- [Varrock PR243](https://github.com/OpenRune/OpenRune-Server/pull/243), merged
  head `aa35c4e6c1cfd4ba66d7ecdb7482d4263e320a98`, and [Draynor PR245](https://github.com/OpenRune/OpenRune-Server/pull/245),
  merged head `20efad8edde22d9c750d67bc5146fb7240149900`: existing handlers
  retain ownership; no duplicate registration.
- Container searches also located [Crafting PR146](https://github.com/OpenRune/OpenRune-Server/pull/146):
  it explicitly defers pouch and silk sack storage/upgrade interactions.
  [Consumables PR140](https://github.com/OpenRune/OpenRune-Server/pull/140)
  provides the potion effect system already present in this fork. Raid PR271
  targets revision 241; it is outside this revision-240 scope.

## Primary data and verification

The [Shortcuts list](https://oldschool.runescape.wiki/w/Shortcuts) supplies
162 rows, including multiple levels sharing one object/page. The
[container list](https://oldschool.runescape.wiki/w/Container_items#List_of_containers)
contains 39 categories. The [fairy-ring table](https://oldschool.runescape.wiki/w/Fairy_ring)
is compared with the native `dbtable.fairyring` (all 64 dial combinations).
Wiki raw/API requests are batched after encountering rate limiting. Local
source manifests record URLs and SHA256 hashes.

Native audit reads the accepted isolated home cache: 34,606 item definitions,
62,525 loc definitions and 7,764 relevant placed objects. One pre-existing
malformed map archive group 25287 is recorded rather than silently counted as
valid; affected routes must be checked separately if the requested content uses it.

Spider Cave evidence and coordinates are documented in [Araxxor](araxxor.md).
The combined Araxxor/fairy test run passes 29 Araxxor tests and 5 fairy tests.
Fairy tests currently prove data/dials/registration, not every travel interaction.
No live installation has been performed.
