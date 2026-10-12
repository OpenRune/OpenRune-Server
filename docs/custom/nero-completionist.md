# Nero Completionist integration

The owner requested the v0.7 integration, then supplied the v0.11 update.
World shortcuts/containers/fairy rings are preserved separately at Forestry
checkpoint `0bb3fbfa0` on `feature/shortcuts-containers-fairy-rings`. This checkout starts at
accepted Revenant/Home commit `44f15a55f` and retains the fourteen accepted local
source changes separately. No parked container or shortcut rollout is implied.

## Handoff and target qualification

The original handoff ZIP SHA256 is
`8e0d03fa082994a4478c649dc090dba953afc67dd527a9830493200391729065`.
All seven outer-manifest hashes match. Documents describe the prototype and its
limits; the owner's direct request controls implementation. Sources/assets are
retained from the handoff; this is not a universal revision compatibility claim.
The v0.11 ZIP SHA256 is
`3906c37ac4f8c878bda87506a7a092cd27e7aa6f33a6cfce810e951050e5510f`.
All 468 supplied source fingerprints match. Only the reviewed feature assets
and client sources/artifacts were imported; the complete source archive was
not applied over the fork.

The handoff targets the older `workspace/OpenRune-Server` JAR (`fc1b3698...`).
The accepted launcher release uses `release/workspace/OpenRune-Server`, whose
JAR SHA256 is `80fb5e49ee0b910aae2feee58b4e5c0dbbcfeb54a68344ee1fdf75dfcb1b0ebc`.
The original build/checksum guards remain in the original bundle. Integration
requires new qualification against the accepted build, never bypassing guards.

A read-only preflight against that release found exactly one clash among 402
assets: varp 63020 is the existing special-staff protection timer. The 42 style
variables move to 63300-63341, checked against all native cache varps and source
varp declarations, including parked work. All wearable item IDs remain below
63487. Item/model/NPC/interface IDs are unchanged. The remapped 402-asset
preflight passes without modifying the release.

Upstream review: official tree `bbc78dfc259c988a77d5c840196946c72de862e9` has no
max-cape, quiver or completionist implementation. Open/draft and closed PR search
for cape-related work returned unrelated skill/quest/cape-merchant PRs. No
upstream branch was imported. The existing fork's max-cape behavior is the basis.

## Native integration approach

`content/other/nero-completionist` owns the Curator, chatbox purchase/fusion,
regular-only customiser, style persistence/migration and loopback appearance feed.
Its dedicated pack contains the reviewed raw models/widgets/servervars, native
symbol metadata and a cache task for both LIVE and SERVER passes. Each build
derives the fourteen bonuses from the corresponding native max variant (+2,
magic damage raw +20); hoods remain unchanged. Regeneration must be proven.

Curator startup uses the existing game lifecycle owner rather than an unbound
cycle subscription. Purchase/fusion/editor completion guard lifecycle, movement
and stale references. Only Done applies the draft; presets are independently
persistent. Style-feed bind must remain loopback, default normal port 46602.
The handed-off runtime's existing 3*percentage/100 scale and -12 model-Y hem
offset are retained; fixed styles and regular palettes remain isolated.

## Current validated chunk

The bounded native pack path preserves the exact contents and IDs of all
**538,211** existing config definitions after reopening the written caches. It
handles the revision-240 signed varp sentinel explicitly; a Displee rewrite
otherwise moves existing varp file IDs by 32768. The failed candidate was
replaced with an untouched accepted cache copy before qualification.

**26 Curator/cape integration tests + 15 existing max-cape tests pass**. The
actual NPC, held-item, pause-dialog and IF3 input paths cover purchases, all 22
recipes in both directions/full inventory, all 48 Wear options, editor
save/cancel/close/presets/migration, 24 worn-bonus calculations and Dizana
held/ammo/combat/persistence, actual PostgreSQL save/load, variant teleports
and native death/Perdu routes. Native visual acceptance is still separate.

Dizana reuses the reviewed shared ammo implementation from the parked branch;
only the necessary API changes and new-cape handlers are included. Full parked
container rollout remains separate. The extra native equipment UI handler must
be reconciled to one owner when that parked branch is resumed.

The full cache builder was attempted and fails in existing unrelated CS2
symbols. `:content:other:nero-completionist:nero-completionist-pack:packCompletionistAssets`
is the qualified bounded route for an existing accepted cache. Full fresh-cache
regeneration remains a separate open check; its failure is not hidden.

The opt-in client verifies all 34 supplied pinned local libraries, the supplied
runtime artifacts and Java 11. An actual runtime linkage check verifies the
bootstrap bridge and qualified player/NPC/model/attachment/scene hooks without
opening a client. Original revision/API/HD fingerprint guards, particle scale
and hem offset are retained.

## Variant behavior and protection

Held options come from each native max variant; regular/trimmed also retain
the native worn labels/submenus. Fixed forms keep their matching cape behavior.
Ardougne/Mythical teleports use shared teleport validation and collision checks.
Ava variants keep native recovery rates and Commune; regular forms have 72%
recovery. Guild entry and essence-pouch protection apply to all wearable max
variants; full smelting/coal/runecrafting/cooking perks apply to regular forms.
Dizana is blessed with shared extra ammunition and Ava upgrade.

Eight custom protection families have non-wearable broken, locked broken and
locked mangled forms at free IDs 63200-63223. They keep the custom inventory
model but clear bonuses/equipment slots. The revision-240 native cache already
has broken/mangled Infernal and Dizana max forms. These custom families use the
permanent lock/repair lifecycle reviewed in the parked quiver implementation;
this extends that policy to the custom families without replacing old items.
Low PvP loss returns a broken form and repair bounty. Deep unlocked loss yields
569,250 coins; locked deep loss returns a mangled form with 500k bounty/repair.
Broken items cannot consume protection slots or pay another repair bounty.
PvM locked items remain equipped; death previews stay pure. Dizana shared ammo
spills on PvP death even if its cape is protected.

Perdu revalidates selected references and pays atomically from inventory/bank.
Lock: parchment + 500k. Unlock: parchment + 475k refund. Repair keeps the family
and lock state. Locked fusion consumes a locked ingredient, so it creates no
free additional lock. Fees follow the matching max-family repair table:
Fire 150k, other non-Dizana families 99k, Dizana 400k.
References: [death table](https://oldschool.runescape.wiki/w/Death) and
[max exceptions](https://oldschool.runescape.wiki/w/Max_cape). The indexed wiki
is older than this cache and does not prove current mangled-state behavior.

## Validation and integration still to run

Exact-file ktlint hygiene passes, excluding all accepted baseline sources.
Repeated bounded regeneration passes. Native account save/load uses a fresh
temporary PostgreSQL database and the existing repository/applier: saved colors,
presets, Ava toggle and ammo survive; temporary drafts/timers do not. Touched
crafting/cooking/runecrafting modules compile.

The 217MB packaged JAR is built and preserves all nine accepted private Studio
pack entries byte-for-byte. Isolated boot loads 585 scripts, Central and a fresh
PostgreSQL database; the correct revision-240 JS5 handshake and appearance schema
1 both pass. Windows-assigned wildcard-tested TCP ports avoid reserved/busy fixed
ports; the test profile binds its own existing-key gamepack copy to those ports.
The actual Java 11 client initializes, side-loads the supplied particle plugin
and reports software particle/depth readiness. Only that owned client/server
were stopped afterward. No old profiles, RSProx, live cache/RSA/data were edited.

Installer rehearsal on an independent server copy passes Check, Install,
idempotent Install and exact Rollback of all 32 originals. An unknown target
change is refused before replacement. Process guards cover the selected server
and its game port, allowing unrelated clients to remain open. The original
live JAR and all fourteen accepted source hashes still match the baseline.

Package: `outputs/nero-completionist-v07-20261012` in the task workspace.
`TESTEN.cmd` prepares/starts the isolated test server and opt-in client;
`TESTEN.ps1 -Action Stop` stops the owned server. `VALIDATION.json` records the
actual evidence, and `LEESMIJ.md` contains the manual scenarios. The package
installer has not been run against the live installation.

Still open: native in-game visual acceptance of both genders, idle/walk/emote,
color/palette isolation and software/qualified-HD depth. The supplied renderer
still reports `sourceMaterial744=false` and `exact646Simulation=false`: it is
the v0.7 preview implementation, not a newly proven exact native particle engine.
Original render proof is reference evidence; startup does not prove appearance.
The older full-completion eligibility proposal is not part of v0.7 purchase
behavior and awaits an owner decision. Actual live installation/merge awaits
subsequent owner acceptance.

## v0.11 asset/client update

The native module remains `nero-completionist`; the supplied second module and
its unbound startup/feed callbacks are not added. Existing near-player/modal,
movement/death/logout/stale-input protections, permanent styles, native perks,
Dizana ammo and all repair/death routes remain the tested implementation.

The pack now owns 406 raw assets plus 48 derived repair representations.
Each of the 454 resulting assets is SHA-verified after cache reopening, with
538,211 unrelated config definitions preserved. The 42 style IDs remain
63300-63341, rather than adopting the archive's 63227 range. All item/model/
NPC/interface IDs remain unchanged; component metadata grows from 82 to 84.
New regular preview mode/particle controls are temporary, and the stale-editor
session counter moves from temporary style 37 to otherwise-unused style 38.
No permanent style or preset migration is needed for this UI change.

V0.11 provides double-sided, quarter-unit cape/hood geometry, gradient family
palettes and real male/female Dizana quiver meshes. Fire/Infernal use native
OSRS textures 40/59 on 60 original trim faces and their 60 reverse faces.
Cloth is not texture-replaced. Native depth bias separates four cloth/detail
layers; hidden hem emitters and ordinary-equipment bias are preserved.

`runtime-current.txt` selects `runtime-v011-final`; the test client follows it
and validates all runtime model-replacement files as well as JARs. All 34 local
client-library fingerprints are unchanged. Compilation with Java 21/release 11
produces 24 runtime and 14 plugin classes byte-identical to the supplied JARs.
The supplied ASM licence and API/revision/HD fingerprint guards are preserved.
`verify_client.py` exercises the actual qualified revision-240 decoder offline,
exact scaled coordinates for 96 assets, native emitter merge/copy/light/shared
sequence propagation, six trim-textured assets, four depth layers, twenty
stable upright preview frames and 48 native ItemStats conversions.

Only regular cape/hood follow freely edited colours. Each fixed/locked variant
keeps its matching hood and family palette. `::compcapes hoods` grants all 24
hoods atomically; it requires 24 empty slots and never clears inventory.
The two test commands use the native administrator guard. Actual held actions
round-trip all 48 capes/hoods through a full inventory without losing the old
item. The server extensions now use the native zero absence sentinel, preserving
normal native transforms instead of registering item -1 as a real output.

The separate test helper grants session-only administrator rights on native
PrepareLogin/Login events after checking its exact marked test root. It is
compiled outside the server JAR, is not installed live, and rejects unmarked
roots. Purchases and all normal-player runtime rules remain unchanged.

Qualified package: `outputs/nero-completionist-v011-final-20261012/TESTEN.cmd`.
Its 32 payload files preserve nine exact accepted private Studio JAR entries.
A separate server/database boots; rev240 JS5 handshake, appearance schema/feed,
client startup/particle readiness and installer Check/Install/Rollback on an
owned server copy are validated. Fourteen accepted source hashes and the live
JAR remain unchanged. All test processes are stopped after validation. Shutdown repeats the already
tracked database connection-close warning; no clean shutdown-warning-free result
is claimed. No user/player save was exercised by this boot-only test.

The delivered report's in-game screenshots are external evidence, not a new
local visual acceptance test. Final male/female idle/walk/emote/camera and HD
checks remain for the owner. Particles still use the provided soft-disc sprite
and approximate simulation; real trim textures do not establish original RS2
particle material 744 or exact engine parity. Full fresh-cache compilation still
has the previously recorded unrelated CS2 symbol failures. No live installation
or feature merge is performed by this update.
