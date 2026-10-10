# Edgeville home and teleport catalogue

The owner's home choice is **Edgeville**. `::home` arrives at `(3087,3496,0)`;
the Teleport wizard stands at `(3084,3499,0)` near the bank. Talk-to opens the
same public menu as `::teleports`. Existing Varrock content is retained.

## Catalogue and native interface

`content/other/home-teleporter` owns the runtime scripts, reviewed destination
table and matching pack. The table currently contains **145 entries**, with
**84 enabled routes** and **61 disabled future/review entries**. Categories:
Cities, Monsters, Bosses, Skilling, Dungeons, Minigames, Raids, PvP and Other.
Search spans all categories; six destinations are shown per page. Select a row
to see its description, then press Teleport. Future entries stay visible with
Coming soon and have no teleport output.

Availability means the travel route is enabled. It does not certify complete
gameplay at every training location. Only the owner-accepted encounters receive
enabled boss routes; other existing boss modules require review first. Boss
routes use the existing pre-lair, boat or entrance, preserving fees, Slayer
requirements and instance creation. The catalogue is intentionally curated;
finding a source directory does not enable unfinished content automatically.

The native 512x334 IF3 uses the fork's steel border, thin boxes, stone buttons,
item models and fonts. Fixed and resizable gameframes use the same main modal.
Free allocations were checked in the accepted rev240 cache and module mappings:

| Kind | Symbol | ID |
|---|---|---|
| NPC | `npc.home_teleport_wizard` | 65526 |
| Interface | `interface.home_teleports` | 1105 |
| Init script | `clientscript.home_teleports_init` | 45546 |
| Tick queue | `queue.home_teleport` | 61 |

The wizard inherits the native Watchtower Wizard definition, including its
models, Talk-to option and animation sequences. The CS2 symbol lives in the
pack's `cs2/symbols/clientscript.sym` so compilation is reproducible. No foreign
cache, NPC ID or animation ID is imported from the capture.

## Travel safety and lifecycle

Public travel uses `TeleportType.Standard` and the shared validator at cast,
after confirmation and before arrival. It respects teleport blocks, Wilderness
limits and the Revenant combat delay. Wilderness routes require confirmation.
No runes are consumed and no travel XP is granted. Death, logout, movement,
blocked landing and late denial cancel travel; outgoing graphics/animation are
cleared before arrival or cancellation. Normal standard teleport sequences are
reused from the accepted teleport fix.

Menu state belongs to the player and is removed on interface close/logout. The
wizard is owned by this plugin and removed on unload; unrelated NPCs and world
edits are retained.

## Edgeville Emblem trader

The pre-existing Edgeville trader previously redirected reward dialogue to
Daimon's Crater. A small `EmblemTraderExchangeHook` lets the Revenant module
supply the same artefact exchange used by the wandering revenant trader, without
a circular module dependency or duplicate Talk-to/skull handlers.

Talk-to with artefacts, the existing reward option and item-on-NPC interactions
can redeem all six ancient artefacts, including notes, at both traders. Values
are 500k, 1m, 2m, 4m, 8m and 16m coins per item. Confirmation snapshots are
rechecked before an atomic inventory transaction; cancel, movement, logout,
death, changed items and coin overflow preserve the artefacts. Skull dialogue
continues through the existing Wilderness script.

## Reference and upstream decision

The supplied `teleporter-recording.npack` and bridge were inspected as data:
Edgeville wizard, category sidebar, destination list and search. `tools.zip`
contains capture/bridge CLI libraries; foreign binaries were not executed or
used as server assets. This fork keeps its own native layout and cache.

Official upstream code searches at `97502542e388bc9c3b433361f3c63c3419738422`
found existing travel helpers and town NPC travel, rather than an equivalent
public category catalogue. Reviewed related work:

- [PR #216](https://github.com/OpenRune/OpenRune-Server/pull/216), closed without
  merge, head `375581c32314973d7de6a04e5e7f3713ef429f22`: admin teleport menu,
  jewellery safety and native travel code. Its admin exemption is inappropriate
  for this public menu; the fork's accepted validator/animation route is retained.
- [PR #228](https://github.com/OpenRune/OpenRune-Server/pull/228), head
  `b1b4e4237fb1512f7b0032cd9ed79c9ea638e876`: construction/portal interfaces and
  visibility/position helpers. It does not provide the requested public home
  catalogue. Native visibility handling is used; unrelated POH code is excluded.

## Verification and client acceptance

Tests exercise real NPC operation producers, incoming IF3 packets, public
commands, search, paging, disabled rows, Wilderness confirmation, safety checks,
animation cleanup, wizard ownership and actual native map collision. All enabled
landing tiles and the walking route between home and the wizard are checked.
Trader tests exercise native NPC operations, six artefacts/notes, replay
prevention and transaction rollback. Final counts and package evidence are
recorded in the test bundle.

Final validation: **110 selected tests**, including **11 teleporter tests** and
**4 Edgeville trader tests**, all pass. Native cache preservation, login-varp
checks (19,438 varbit bases and real new/existing-account writes), formatting,
server JAR and isolated rev240/Nero boot pass. The final boot probes all 85 tiles,
45 decoded menu components and the native Talk-to wizard definition. Existing
database shutdown warnings are also present in the baseline; the isolated run
exits normally with its processes cleaned up.

`tools/home-teleporter/PatchHomeTeleporter.java` adds only the wizard definition,
interface and init script to copies of LIVE and SERVER. It checks every existing
archive and NPC file. `VerifyHomeMappings.java` checks preservation of **220,164
existing mappings**, with **49 additions** (45 interface components plus four
allocated symbols). Existing config metadata, including native varp IDs, stays
intact. No RSA, playerdata, client or world-edit payload is installed.

Native gameval metadata for the added NPC and interface is included too; having
only the client IF3 bytes does not populate the server's component decoder.
The package is based on the verified installed Revenant entrance fix at
`44f27f5ba`; the feature remains separate for review and client acceptance.

Manual acceptance: use `::home`, Talk-to the wizard, test search/category/page
navigation in fixed and resizable mode, teleport to a city and an accepted boss,
cancel/accept a Revenant Caves destination, then exchange artefacts at Edgeville.
Visual animation timing and layout remain client acceptance checks; a headless
server build cannot certify them.
