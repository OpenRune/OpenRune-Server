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

This section records the superseded v0.11 checkpoint. Current visual corrections
are described below; the supplied v0.11 rendering was rejected in-game.

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

## v0.12 visual corrections (2026-10-12)

Owner evidence: `ScreenRecording_10-12-2026 13-17-29_1.mov` specifies the moving
Fire/Infernal panel coverage. `20261012-1508-32.4237241.mp4` demonstrates the
rejected v0.11 worn models and inventory icons. The owner requested the original
Completionist inventory shape in every variant, rather than upright worn meshes.

The actual dedicated-client log records `Unexpected Completionist reverse
topology` after changing capes in HD. That alpha-dependent face recount disabled
the entire model/particle observer. Exact SHA-bound per-face layers now propagate
through native decode, merge, shallow/shared-sequence copies and lighting. Layer
application does not infer topology from animated alpha. Hidden emitter faces
also use native hidden render mode, retaining the seven attachment coordinates.

The 785 added reverse faces previously used flat shading and shared front
vertices. Their vertices/normals are now independent, smooth, and displaced by
up to half an OSRS unit to prevent coincident surfaces. Face ordering, source
labels, palette bindings and the native Dizana quiver remain. A second reproduced
native failure after lighting was the use of `er.ba` (face-alpha groups) to
recover vertex skins. The qualified native vertex groups are `er.bu`; the merge
now restores those labels with explicit vertex bounds checks.

Fire/Infernal map native animated materials 40/59 to the two broad cloth channels
seen in the reference, using one continuous planar mapping. Each worn model has
508 textured front/back faces. Embroidery and raised edges retain their colours.
The original RS2 inventory meshes are restored: 16 unique regular/trimmed/family
models serve all 24 cape items, including locked aliases. Native projection is
zoom 1316, pitch 252, yaw 1020, offsets (-1,24). Hood icons are retained.

Particles retain a bounded 1.2-second model lease, reprojected at the actor's
current position. Empty/ordinary model samples, cape removal, despawn, logout
and expiry clear old state. This prevents cached frame gaps from starving an
emitter, without emitting indefinitely from removed equipment. Diagnostic counts
now report actual per-actor particles rather than the unused global simulator.

`runtime-v012-inventory-final` is the paired opt-in runtime. All 34 accepted client
library/API/HD fingerprints remain unchanged; 25 runtime and 14 plugin classes
are built from the reviewed sources. `fix_visual_assets.py` reads an immutable
asset baseline and `model_codec.py` rejects unsupported encodings. The original
owner mesh and layer inputs are retained under `tools/nero-completionist-client/assets`.

Executed native checks: all 96 assets; 32 smooth double-sided worn meshes;
16 folded icons; six native texture assets; exact coordinates and emitter
merge/copy/light/shared-sequence propagation; the HD animated-alpha regression;
movement/unequip/ordinary-model/expiry/despawn particle producer-consumer tests;
20 stable preview frames; 48 stats conversions. The real game `drawFrustum`
rasterizer renders 14 untextured inventory models offline using inert headless
client state. It does not start the applet, login, or network. Fire/Infernal
geometry/material mappings are verified separately; their animated scene
appearance still requires the owner's software/HD check.

The actual rev240 texture codec also verifies the retained native materials:
Fire 40 uses image 485, direction 1/speed 2; Infernal 59 uses image 318,
direction 1/speed 1. Both original indexed images exist in cache index 8, as
used by the qualified client loader. No substitute texture is injected.
SHA-bound runtime/pack inputs explicitly disable Git newline conversion.

The original inventory mesh contains pre-shaded channel-3 luminances. Every
one of those shades is included in the palette conversion/binding, preserving
the folded mesh but correcting each family colour and the regular customiser.
The 14 native untextured renders produce 12 distinct family palettes; Fire/
Infernal inventory meshes each have 140 genuinely textured panel faces.

The bounded cache pack again verifies all 454 writes and preserves 538,211
unrelated config definitions. All 26 native cape and 15 max-cape tests are rerun.
Test entry: `outputs/nero-completionist-visual-fix-v2-20261012/TESTEN.cmd`.
Existing accepted sources, live JAR/cache/RSA/playerdata, and earlier packages
are retained. Final rear/side, male/female, walking/emote, texture and HD visual
acceptance is still open; this update does not claim exact RS2 particle material
744/simulation parity or a live installation.

## v0.13 authored face correction (2026-10-12)

The owner rejected the remaining flicker in `20261012-1747-10.7358603.mp4`.
The v0.12 log continues emitting particles without observer failures. Its
technical checks established decoding and lifecycle, but did not establish
correct worn topology in an animated scene. The v0.12 visual result is superseded.

Read-only native references were resolved through the actual rev240 item codec:
max cape 13280 uses worn models 29616/29624; Fire cape 6570 uses 9638/9640;
Infernal cape 21295 uses 33103/33111. Native max cloth/trim uses priority groups
4/5 (its collar also uses 1). The owner-supplied source already contains both
outer cloth and an inner lining. Its original worn face priorities are retained
in `assets/original-worn-priorities.json` and are identical for both genders.

The conversion had appended 785 reversed faces to the 792 authored faces.
v0.12 separated their normals and displaced them slightly, still rendering a
second garment over existing cloth/ornament. The real gamepack rasterizer
reproduces the broken overlapping design. `original_shell` now keeps the authored
792 faces, including seven hidden particle anchors, plus any native quiver
faces after the generated block. It removes generated faces and unused vertices,
preserving positions, skin labels and source face IDs; palette references are
filtered/remapped. Original priorities 4/5/hidden 10 and native smooth shading
are restored. No layer bindings are emitted, so the selected runtime never
rewrites assembled-avatar priorities or synthesizes face bias. Ordinary body
and equipment rendering stays native.

Folded inventory models also retain the authored faces once, their native
priorities, complete shaded family palettes, and exact projection. Fire/Infernal
40/59 coverage is now 254 authored worn faces and 70 folded faces per asset.
Particle lifecycle, gameplay and persistence are unchanged.

Selected bundle: `runtime-v013-native-faces`. Its native runtime code is unchanged
from the qualified v0.12 bundle; asset bindings are new. All 34 client dependency
fingerprints remain unchanged. Native checks pass for 96 assets, 32 authored
worn models, seven anchors per model, merge/copy/light/shared-sequence inheritance,
animated alpha without ordering changes, the real model-to-particle lifecycle,
14 actual inventory renders (12 distinct palettes), and 1,536 actual worn
`drawFrustum` renders (16 models × eight angles × 12 shared model copies).
The worn render test checks visibility and repeatability, including lining and
decorated side. Fire/Infernal material mapping is checked separately; this
headless test does not implement an HD scene or a real animation sequence.

The bounded pack verifies 454 writes while preserving 538,211 existing config
definitions, and the server build succeeds. The new marked test server accepts
rev240 JS5 (00) and appearance schema 1 with zero player sessions. Installer
Check/Install/Rollback restores all 32 payload files on an owned copy.
The previous 26 cape and 15 max-cape gameplay tests remain applicable evidence;
no gameplay implementation changed in this visual fix.

Test entry: `outputs/nero-completionist-native-faces-v013-20261012/TESTEN.cmd`.
The package includes `VALIDATION.json` and `BOOT-PROOF.json`. Both genders,
actual idle/walk/emote, all family swaps, moving materials and HD still require
owner visual acceptance. Earlier packages, 14 accepted checkout source hashes,
the accepted live server JAR and live cache/RSA/playerdata remain preserved.
No live install, cape-code push or merge is performed by this fix.

## v0.14 complete-avatar occlusion correction (2026-10-12)

Owner evidence: `20261012-1846-41.4909015.mp4`, with chat annotations that the
quiver is barely visible and that the cape clips over the player. The owner
describes the current result as nearly perfect. The log continues rendering
particles, including HD submissions, without observer errors.

The v0.13 standalone cape check missed a complete-avatar interaction. A
read-only export of actual native male/female bodykits is merged using the
qualified gamepack's ModelData constructor. Its real `drawFrustum` reproduces
the lining drawn over the torso and the partially hidden quiver. Native quiver
item 28955 resolves to models 50822/51022. The custom quiver's 344 faces already
have the correct native scale/coordinates, so it is not enlarged or shifted.

`model.<sha>.opaqueDepth=true` is emitted only for the 32 worn custom cape
models. A weak, topology-bound flag propagates through native merge, copy,
lighting and shared sequence paths. Before drawing a flagged model, opaque
faces on that rendered avatar copy share depth group 0. The qualified
`fx.cb` priority field is verified by the real client's API/bytecode; an array
clone is installed only when changes are needed. Shared cached/native equipment
priority arrays are never mutated. Already-normalized copies allocate nothing.
Transparent/hidden priorities, source alpha and existing native body bias stay
unchanged. No layer bindings or synthetic bias are emitted, and no generated
reverse faces return. Unbound ordinary avatars, inventory models and native
max capes retain their original ordering.

The new `NativeAvatarCheck` exercises 28 actual body + cape merges across both
genders. It checks the front torso against an ordinary native body control,
preserves source arrays across shared sequence copies, and proves ordinary
avatars remain unchanged. Dizana's legitimate native shoulder strap is excluded
from the cape-only torso comparison. A separately decoded model with only its
344 quiver faces hidden supplies the rear visibility control: 2,175 male and
1,610 female pixels expose the native quiver after the correction. The 96-model,
1,536 worn-render, 14 inventory-render, source-alpha, emitter, producer-consumer
particle lifecycle and stats checks also pass.

`verification/cache/NativeAvatarReference.java` is a bounded read-only fixture
exporter; export paths inside the source cache are rejected. It handles native
kit model-ID opcodes 5 and 70..79, without changing the older server kit codec.
Compile against the existing server JAR with Java 21, export to a separate
fixture directory, then run `verify_client.py --avatar-fixtures <directory>`
alongside its existing Java/assets/output arguments. Ordinary native model
files are not bundled as new custom content or committed as fixtures.

Selected bundle: `runtime-v014-avatar-final`, 26 runtime and 14 plugin classes.
All 34 client/API/HD dependency fingerprints remain unchanged. This is a client
fix: the cape mesh/cache assets, gameplay source and server JAR are unchanged
from v0.13. Its 26 cape + 15 max-cape tests and 454-write cache preservation
proof remain prior evidence, not newly rerun gameplay claims. The new isolated
server accepts rev240 JS5 and appearance schema 1 with zero player sessions;
Check/Install/Rollback passes on an owned copy.

Test entry: `outputs/nero-completionist-avatar-fix-v014-20261012/TESTEN.cmd`.
Earlier packages and the live installation are retained. Real idle/walk/emote,
software/HD and all style swaps still require owner confirmation. No live
installation or cape-code push/merge is performed.

## v0.15 hood/head and quiver correction (2026-10-12)

Owner recordings `20261012-2037-19.6401824.mp4` and
`20261012-2039-04.7109490.mp4` report corrupted hood/head rendering in both
software and HD, black software map patches and a quiver sitting inside the
cape. The v0.14 cape-only avatar checks did not cover complete equipped heads.
Its hood rendering is not accepted. Existing cape cloth, inventory projection,
texture coverage, gameplay and particle scale are retained.

The immutable source's worn hood contains 564 male / 675 female authored
faces, followed by an exact reversed, flat-shaded copy of every face. The
427 cloth faces match the hood inventory topology; remaining source faces
provide its own head. `original_hood` removes the coincident copies while
retaining that original head, colours, bone groups 1/2/3 and native slots
0/8/11 masking hair and jaw. Removing the entire source head would leave
an empty face after the normal equipment masks. Inventory hoods are unchanged.
Only 32 worn hood assets and two quiver cape models change. SHA-bound hood
depth metadata also carries precision through copied/merged fixed variants.

`quiver_clearance` verifies the native 344-face quiver part has no shared cloth
vertices, then offsets only those vertices +32 quarter units on model Z
(eight OSRS units outward/back). Both genders retain native scale and bone
labels; locked Dizana items share the same corrected models. Native rear
visibility changes 2,804 male / 2,572 female pixels versus the same avatar with
only quiver faces hidden, compared with 2,175 / 1,610 at v0.14.

The software fallback previously selected depth rendering without explicitly
resetting its reused viewport depth at scene entry. Its hook now passes the
actual scene to the consumer. Before a logged-in main software scene with a
valid viewport, only viewport Z values are reset to the native empty value 0.
RGB remains owned by the game; GPU and nested WorldView depth are untouched.
This prevents prior camera occluders from participating in the new view. The
owner's actual map/shader symptom still requires an in-game retest; no full
world-scene reproduction or new HD acceptance is claimed.

`NativeDepthCheck` exercises the actual native buffer and scene consumer with
stale/NaN depths, repeated frames, viewport bounds, RGB ownership and GPU/nested
guards. `NativeHoodCheck` uses native bodykits without the masked hair/jaw,
merges each of 32 hoods with a cape, applies real native head-bone rotations,
and performs 768 software painter/depth renders across both genders. It
checks single authored triangles, finite bounded vertices/depth and subsequent
native geometry. These controlled bone poses are not the actual idle/walk
sequence or an HD GPU test. Existing 96 decodes, 1,536 cape renders, 14 native
icons/12 palettes, 28 complete cape avatars, emitter lifecycle, animated alpha
and 48 native stats checks pass with `-Xverify:all`.

Bundle `runtime-v015-hood-depth-final` builds 26 runtime / 14 plugin classes;
all 34 accepted client/API/HD libraries remain unchanged. The bounded pack
again verifies 454 writes and preserves 538,211 unrelated definitions after
reopening the cache. The final server JAR rebuild succeeds with the corrected
assets. The 26 cape + 15 max-cape gameplay tests remain earlier evidence;
gameplay Kotlin is unchanged. The packaged JAR preserves nine private Studio
entries. Own-copy installer Check/Install/Rollback succeeds; the new isolated
server accepts JS5 rev240 and appearance schema 1 with zero player sessions.

Test entry: `outputs/nero-completionist-hood-fix-v015-20261012/TESTEN.cmd`.
Retest hood on/off, head turns, walking and camera movement in software and
HD, plus Dizana's offset and both genders. Earlier packages, the live
installation and fourteen accepted source hashes are preserved. Cape code
remains local; no live installation or feature push/merge is performed.
