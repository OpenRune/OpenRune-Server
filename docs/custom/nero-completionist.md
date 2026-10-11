# Nero Completionist integration

The owner requested the v0.7 completionist-cape handoff integration after parking
world shortcuts/containers/fairy rings. The preserved Forestry checkpoint is
`84b002717` on `feature/shortcuts-containers-fairy-rings`. This checkout starts at
accepted Revenant/Home commit `44f15a55f` and retains the fourteen accepted local
source changes separately. No parked container or shortcut rollout is implied.

## Handoff and target qualification

The original handoff ZIP SHA256 is
`8e0d03fa082994a4478c649dc090dba953afc67dd527a9830493200391729065`.
All seven outer-manifest hashes match. Documents describe the prototype and its
limits; the owner's direct request controls implementation. Sources/assets are
retained from the handoff; this is not a universal revision compatibility claim.

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

**14 Curator/cape integration tests + 15 existing max-cape tests pass**. The
actual NPC, held-item, pause-dialog and IF3 input paths cover purchases, all 22
recipes in both directions/full inventory, all 48 Wear options, editor
save/cancel/close/presets/migration, 24 worn-bonus calculations and Dizana
held/ammo/combat/persistence scopes. These tests do not prove native visual
acceptance or the locked death/repair lifecycle.

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

## Validation and integration still to run

Repeated bounded pack/cache regeneration; broader shared-code regression;
actual account save/load; complete applicable cape perks and locked
death/Perdu/repair routes; software/qualified-HD depth and palette isolation;
isolated boot/login, guarded deployment with backups and client startup/rollback.
The original handoff proof is reference evidence, not a passed integration run.
